package com.xenonware.mindcontrol

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import moe.shizuku.server.IRemoteProcess
import moe.shizuku.server.IShizukuService
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipFile

enum class PermissionStatus { UNAVAILABLE, AVAILABLE, DENIED, GRANTED }

/**
 * Adapts a local [java.lang.Process] to [IRemoteProcess] so the root and Shizuku paths share one
 * type.
 *
 * Root runs the helper through `su -c`, which yields a plain local process; Shizuku hands back a
 * binder-backed remote one. [IRemoteProcess] is not [java.lang.Process]-shaped: its streams are
 * [ParcelFileDescriptor]s and liveness is [alive], so only the two streams actually used by the
 * virtual D-pad are adapted. The remaining methods exist to satisfy the interface.
 */
private class LocalRemoteProcess(private val process: Process) : IRemoteProcess {
    override fun destroy() = process.destroy()
    override fun waitFor(): Int = process.waitFor()
    override fun alive(): Boolean = process.isAlive

    /** Wraps a stream as a pipe-backed descriptor, the same shape Shizuku returns. */
    private fun descriptorFor(input: InputStream): ParcelFileDescriptor {
        val pipe = ParcelFileDescriptor.createPipe()
        Thread {
            runCatching {
                ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { out ->
                    input.copyTo(out)
                }
            }.also { runCatching { input.close() } }
        }.start()
        return pipe[0]
    }

    override fun getInputStream(): ParcelFileDescriptor = descriptorFor(process.inputStream)

    /** The helper writes nothing to stderr, so no pipe is needed. */
    override fun getErrorStream(): ParcelFileDescriptor? = null

    /** The process' stdin needs the opposite direction, so it is wrapped separately. */
    override fun getOutputStream(): ParcelFileDescriptor {
        val pipe = ParcelFileDescriptor.createPipe()
        Thread {
            runCatching {
                ParcelFileDescriptor.AutoCloseInputStream(pipe[0]).use { input ->
                    input.copyTo(process.outputStream)
                }
            }.also { runCatching { process.outputStream.close() } }
        }.start()
        return pipe[1]
    }

    override fun exitValue(): Int = 0
    override fun waitForTimeout(timeout: Long, unit: String): Boolean = false
    override fun asBinder(): android.os.IBinder? = null
}

object ShellManager {
    private const val TAG = "ShellManager"
    private const val PREFS = "shell_permissions"
    private const val KEY_ROOT_RESULT = "root_result" // "granted" | "denied" | absent = never asked
    private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

    /**
     * Where the native helper is staged when `nativeLibraryDir` did not get an extracted copy.
     *
     * `/data/local/tmp` is the one location the shell uid can both write to (to place the binary)
     * and execute from (to run it), which is why the staging copy is performed by the shell rather
     * than by this process. A name unique to the app avoids clashing with anything else on device.
     */
    private const val STAGED_HELPER_PATH = "/data/local/tmp/mindcontrol_virtual_dpad"
    const val SHIZUKU_REQUEST_CODE = 4711

    @Volatile private var isRunning = false
    private var monitoringThread: Thread? = null

    private lateinit var appContext: Context
    @Volatile private var initialized = false
    private val rootRequestInProgress = AtomicBoolean(false)
    @Volatile private var shizukuDeniedThisSession = false

    private val _rootStatus = MutableStateFlow(PermissionStatus.UNAVAILABLE)
    val rootStatus: StateFlow<PermissionStatus> = _rootStatus.asStateFlow()

    private val _shizukuStatus = MutableStateFlow(PermissionStatus.UNAVAILABLE)
    val shizukuStatus: StateFlow<PermissionStatus> = _shizukuStatus.asStateFlow()

    // Mapping from Linux Raw Scancodes (Hex) to Android KeyCodes
    private val scancodeMap = mapOf(
        "0072" to 25,  // VOL DOWN
        "0073" to 24,  // VOL UP
        "0074" to 26,  // POWER
        "00d4" to 27,  // CAMERA
        "020e" to 134, // FOCUS
        "003e" to 134, // FOCUS (User Device)
        "024b" to 131, // AI / ASSISTANT
        "02d0" to 131, // AI (Alt)
        "003b" to 131, // AI (User Device)
        "00a5" to 27,  // CAMERA (Alt)
        "00e2" to 26,  // POWER (Alt)
    )

    // ---------------------------------------------------------------------
    // Init & status
    // ---------------------------------------------------------------------

    private val shizukuResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode != SHIZUKU_REQUEST_CODE) return@OnRequestPermissionResultListener
            shizukuDeniedThisSession = grantResult != PackageManager.PERMISSION_GRANTED
            refreshShizukuStatus()
        }
    private val binderReceivedListener = Shizuku.OnBinderReceivedListener { refreshShizukuStatus() }
    private val binderDeadListener = Shizuku.OnBinderDeadListener { refreshShizukuStatus() }

    /** Called from MindControlApp.onCreate. Safe to call multiple times. */
    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        appContext = context.applicationContext
        initialized = true

        Shizuku.addRequestPermissionResultListener(shizukuResultListener)
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        refreshShizukuStatus()

        // Set the stored root state immediately so services starting at boot
        // already see GRANTED, then verify it in the background.
        _rootStatus.value = if (isDeviceRooted()) storedRootStatus() else PermissionStatus.UNAVAILABLE
        if (_rootStatus.value == PermissionStatus.GRANTED) {
            Thread { verifyStoredRoot() }.start()
        }
    }

    /** Cheap refresh, safe to poll. Never triggers a root prompt. */
    fun refresh() {
        refreshShizukuStatus()
        if (!initialized) return
        if (!isDeviceRooted()) {
            _rootStatus.value = PermissionStatus.UNAVAILABLE
        } else if (_rootStatus.value == PermissionStatus.UNAVAILABLE) {
            _rootStatus.value = storedRootStatus()
        }
    }

    /** Re-verify a previously granted root silently: still granted -> no prompt; revoked -> denied. */
    private fun verifyStoredRoot() {
        val ok = runSuCheck()
        saveRootResult(ok)
        _rootStatus.value = if (ok) PermissionStatus.GRANTED else PermissionStatus.DENIED
    }

    // Device-protected storage (like SettingsManager) so this also works before first unlock
    private fun prefs() = appContext.createDeviceProtectedStorageContext()
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun storedRootStatus(): PermissionStatus =
        when (prefs().getString(KEY_ROOT_RESULT, null)) {
            "granted" -> PermissionStatus.GRANTED
            "denied" -> PermissionStatus.DENIED
            else -> PermissionStatus.AVAILABLE
        }

    private fun saveRootResult(granted: Boolean) {
        prefs().edit { putString(KEY_ROOT_RESULT, if (granted) "granted" else "denied") }
    }

    /** Shows the Magisk/KernelSU prompt. Only call on user action. */
    fun requestRoot() {
        if (!initialized) return
        if (!rootRequestInProgress.compareAndSet(false, true)) return
        Thread {
            try {
                val granted = runSuCheck()
                saveRootResult(granted)
                _rootStatus.value = if (granted) PermissionStatus.GRANTED else PermissionStatus.DENIED
            } finally {
                rootRequestInProgress.set(false)
            }
        }.start()
    }

    private fun runSuCheck(): Boolean = try {
        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroy()
            false
        } else {
            val out = process.inputStream.bufferedReader().readText()
            process.exitValue() == 0 && out.contains("uid=0")
        }
    } catch (_: Exception) {
        false
    }

    private fun refreshShizukuStatus() {
        _shizukuStatus.value = try {
            when {
                !Shizuku.pingBinder() || Shizuku.isPreV11() -> PermissionStatus.UNAVAILABLE
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> {
                    shizukuDeniedThisSession = false
                    PermissionStatus.GRANTED
                }
                shizukuDeniedThisSession || Shizuku.shouldShowRequestPermissionRationale() ->
                    PermissionStatus.DENIED
                else -> PermissionStatus.AVAILABLE
            }
        } catch (_: Exception) {
            PermissionStatus.UNAVAILABLE
        }
    }

    /**
     * Requests Shizuku permission. If Shizuku isn't running, or the user chose
     * "deny and don't ask again", the dialog can't be shown, so open the Shizuku app instead.
     */
    fun requestShizuku(context: Context) {
        try {
            if (Shizuku.pingBinder() && !Shizuku.shouldShowRequestPermissionRationale()) {
                Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
                return
            }
        } catch (_: Exception) { }
        context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)?.let {
            context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    // ---------------------------------------------------------------------
    // Monitoring
    // ---------------------------------------------------------------------

    fun startMonitoring(onKeyEvent: (Int, Boolean) -> Unit) {
        if (isRunning) return
        isRunning = true

        monitoringThread = Thread {
            try {
                if (isRootAvailable()) {
                    startRootMonitoring(onKeyEvent)
                } else if (isShizukuAvailable()) {
                    startShizukuMonitoring(onKeyEvent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Monitoring error", e)
            } finally {
                isRunning = false
            }
        }
        monitoringThread?.start()
    }

    private fun startShizukuMonitoring(onKeyEvent: (Int, Boolean) -> Unit) {
        var remoteProcess: IRemoteProcess? = null
        try {
            val binder = Shizuku.getBinder() ?: return
            val service = IShizukuService.Stub.asInterface(binder)
            remoteProcess = service.newProcess(arrayOf("sh", "-c", "getevent"), null, null)
            val reader = BufferedReader(InputStreamReader(ParcelFileDescriptor.AutoCloseInputStream(remoteProcess.inputStream)))
            Log.i(TAG, ">>> SHIZUKU KERNEL MONITOR STARTED <<<")
            readEventLines(reader, onKeyEvent)
        } finally {
            remoteProcess?.destroy()
        }
    }

    private fun startRootMonitoring(onKeyEvent: (Int, Boolean) -> Unit) {
        var process: Process? = null
        try {
            process = Runtime.getRuntime().exec(arrayOf("su", "-c", "getevent"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            Log.i(TAG, ">>> ROOT KERNEL MONITOR STARTED <<<")
            readEventLines(reader, onKeyEvent)
        } finally {
            process?.destroy()
        }
    }

    private fun readEventLines(reader: BufferedReader, onKeyEvent: (Int, Boolean) -> Unit) {
        while (isRunning) {
            val line = reader.readLine() ?: break
            val parts = line.split("\\s+".toRegex()).filter { it.isNotBlank() }
            if (parts.size >= 4) {
                val type = parts[1]  // 0001 = EV_KEY
                val code = parts[2]  // The Scancode
                val value = parts[3] // 1 = Down, 0 = Up
                if (type == "0001") {
                    val androidKeyCode = scancodeMap[code] ?: -1
                    val isDown = value.endsWith("1")
                    if (androidKeyCode != -1) {
                        Log.i(TAG, "HARDWARE MATCH: $code -> $androidKeyCode (${if (isDown) "DOWN" else "UP"})")
                        onKeyEvent(androidKeyCode, isDown)
                    }
                }
            }
        }
    }

    fun stopMonitoring() {
        isRunning = false
        monitoringThread?.interrupt()
        monitoringThread = null
    }

    // ---------------------------------------------------------------------
    // Availability (means "granted", never prompts)
    // ---------------------------------------------------------------------

    fun injectKey(keyCode: Int) {
        runShellCommand("input keyevent $keyCode")
    }

    // ---------------------------------------------------------------------
    // Virtual gamepad D-pad
    // ---------------------------------------------------------------------

    /**
     * A long-lived `virtual_dpad` process with its stdin/stdout kept open.
     *
     * The device is created and destroyed on demand while this process stays alive, so pressing an
     * arrow never pays the cost of spawning a shell. The device itself is what Android sees as a
     * gamepad; this object is just the pipe to it.
     */
    private var dpadProcess: IRemoteProcess? = null
    private var dpadStdin: OutputStream? = null
    private var dpadStdout: BufferedReader? = null
    private var dpadDeviceCreated = false

    /**
     * Receives rumble requests forwarded by the helper.
     *
     * Called with `(magnitude, durationMs)` on the thread that is polling the helper, because
     * [dpadCommand] is the only reader of its stdout. Magnitude 0 means "stop now". The service
     * subscribes so it can play the request on the phone's real vibrator; when nothing is
     * subscribed, rumble requests are simply dropped.
     */
    @Volatile
    private var rumbleListener: ((Int, Long) -> Unit)? = null

    /** Registers (or clears, with null) the callback for forwarded rumble requests. */
    fun setRumbleListener(listener: ((Int, Long) -> Unit)?) {
        rumbleListener = listener
    }

    /**
     * Serialises access to [dpadStdout].
     *
     * Two readers share the helper's stdout: [dpadCommand] (while waiting for a reply) and
     * [dpadDrainThread] (so unsolicited rumble events are noticed promptly while no command is in
     * flight). Without this lock they could interleave `readLine` calls and steal each other's
     * lines, which would desync the reply protocol.
     */
    private val dpadReaderLock = Any()

    /** Background drain so rumble events are serviced even when no command is pending. */
    private var dpadDrainThread: Thread? = null
    @Volatile private var dpadDrainRunning = false

    /**
     * Resolves the on-disk path of the packaged native helper.
     *
     * The APK ships it as `lib/<abi>/virtual_dpad.so`. The first choice is
     * [android.content.pm.ApplicationInfo.nativeLibraryDir], which is where the installer extracts
     * it under legacy packaging -- and where a plain chmod works, because the directory is
     * app-owned.
     *
     * That directory is *not* always populated, however. The installer only extracts files it can
     * treat as loadable JNI libraries, and this helper is a PIE executable that is never passed to
     * [System.loadLibrary], so on some devices (and for some build variants) it stays compressed
     * inside the APK and `nativeLibraryDir` ends up with only the real `.so` files.
     *
     * When that happens the helper is staged from the APK into [STAGED_HELPER_PATH] first, so the
     * gamepad is not silently unavailable. The staging location matters: the helper is executed by
     * the *shell* uid (via Shizuku), which cannot read app-private storage, so [Context.getFilesDir]
     * is not usable -- it ends up with "Permission denied" and the process never answers. The shell
     * has to place the binary where that uid can execute it.
     */
    fun nativeHelperPath(): String? {
        val dir = appContext.applicationInfo.nativeLibraryDir ?: return null
        val extracted = File(dir, "virtual_dpad.so")
        if (extracted.exists()) {
            if (!extracted.canExecute()) {
                // The library directory is app-owned, so a plain chmod needs no privilege.
                runCatching { extracted.setExecutable(true, false) }
            }
            if (extracted.canExecute()) return extracted.absolutePath
        }

        // nativeLibraryDir did not have it, so stage a shell-executable copy from the APK instead.
        return stageHelperFromApk()
    }

    /**
     * Stages `lib/<abi>/virtual_dpad.so` where the shell uid can execute it.
     *
     * The APK entry is read by this process (it can read its own APK) and written to a private
     * cache file, then the *shell* copies that file to [STAGED_HELPER_PATH] and chmods it. Running
     * the copy through the shell is what makes the result reachable: the shell runs as its own uid,
     * so it can write to `/data/local/tmp` and later execute what it wrote there. A direct copy to
     * app-private storage would be unreadable to that uid and every command would time out.
     *
     * Cached, but tied to the helper's *size*: the staged copy is only reused while its size still
     * matches the one in this build's APK. Reusing purely on existence (the old behaviour) meant a
     * helper staged by an earlier build was never replaced, so a rebuilt binary -- and every fix in
     * it -- silently never reached the device. Returns null when privilege is missing, the APK has
     * no entry for the device's ABI, or the copy fails.
     */
    /**
     * When staging the helper was last attempted, to rate-limit repeated failures.
     *
     * Only consulted on the failure path; a successful or already-staged check returns before it.
     */
    @Volatile private var lastStageAttemptAt = 0L

    private fun stageHelperFromApk(): String? {
        if (!isRootAvailable() && !isShizukuAvailable()) {
            Log.w(TAG, "uinput: cannot stage helper without root or Shizuku")
            return null
        }

        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: return null
        val sourceDir = appContext.applicationInfo.sourceDir ?: return null

        // Read the current build's helper first, so both the version check and the staging use one
        // in-memory copy rather than reading the APK twice.
        val bytes = try {
            ZipFile(sourceDir).use { zip ->
                val entry = zip.getEntry("lib/$abi/virtual_dpad.so") ?: return@use null
                zip.getInputStream(entry).use { it.readBytes() }
            }
        } catch (e: Exception) {
            Log.w(TAG, "uinput: could not read the helper from the APK", e)
            null
        } ?: run {
            Log.w(TAG, "uinput: no lib/$abi/virtual_dpad.so entry in the APK")
            return null
        }

        // Reuse the staged copy only when it is present, executable and byte-identical to this
        // build's helper. `stat -c %s` is used rather than a checksum: it is one cheap shell round
        // trip, and a changed helper is what actually breaks the "stale binary" case.
        val stagedSize = runShellCommandBlocking(
            "[ -x $STAGED_HELPER_PATH ] && stat -c %s $STAGED_HELPER_PATH || echo none",
        ).trim()
        if (stagedSize == bytes.size.toString()) {
            return STAGED_HELPER_PATH
        }

        // A failed staging (e.g. /data/local/tmp not writable yet) would otherwise be retried on
        // every watchdog tick, piping the whole binary through the shell each time. Back off so a
        // persistent failure costs one attempt per window rather than a burst of traffic.
        if (SystemClock.uptimeMillis() - lastStageAttemptAt < 30_000L) {
            return null
        }
        lastStageAttemptAt = SystemClock.uptimeMillis()

        return try {
            // `bytes` was read above and already holds this build's helper.
            val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            val staged = runShellCommandBlocking(
                "echo '$b64' | base64 -d > $STAGED_HELPER_PATH && " +
                    "chmod 755 $STAGED_HELPER_PATH && " +
                    "[ -x $STAGED_HELPER_PATH ] && echo yes || echo no",
            )
            if (!staged.contains("yes")) {
                Log.w(TAG, "uinput: staging the helper failed: '${staged.trim()}'")
                return null
            }
            Log.i(TAG, "uinput: staged helper at $STAGED_HELPER_PATH")
            STAGED_HELPER_PATH
        } catch (e: Exception) {
            Log.w(TAG, "Could not stage virtual_dpad helper from the APK", e)
            null
        }
    }

    /**
     * True when the helper is present *and* the shell uid may open /dev/uinput.
     *
     * The node's owner varies by OEM and Android version. It is commonly `uhid:uhid` -- and the
     * shell uid is in the uhid group -- but e.g. on Pixel-class devices it is `system:net_bt_admin`
     * (`u:object_r:uhid_device:s0`), which the shell uid reaches via its `net_bt_admin` membership
     * instead. Because the group is not fixed, the result is probed rather than assumed.
     *
     * Only a *successful* probe is cached. A failure is deliberately not cached, because the
     * usual reason for one is that the check ran too early -- at boot, before Shizuku's binder has
     * been registered. Caching that failure meant the controller never started on its own for the
     * rest of the session, even once Shizuku was up and everything was in fact available.
     */
    private var uinputProbeResult: Boolean? = null

    /**
     * When the last probe failed, and how many have failed in a row.
     *
     * Probing costs a blocking shell round-trip, and callers poll this on a timer. Backing off
     * after a failure keeps a device without the capability from being probed forever.
     */
    @Volatile private var lastProbeFailureAt = 0L
    @Volatile private var consecutiveProbeFailures = 0

    fun isUinputAvailable(): Boolean {
        // A known-good result never changes, so it is safe to keep.
        if (uinputProbeResult == true) return true

        val helper = nativeHelperPath()
        if (helper == null) {
            Log.w(TAG, "uinput: helper missing for abi=${Build.SUPPORTED_ABIS.firstOrNull()}")
            return false
        }

        // Privilege is the one gate that can recover *without* a probe failing: Shizuku may still
        // be coming up, or its permission may not have been granted yet. Do not let those states
        // feed the failure backoff, or a permission granted a moment later would wait out the
        // whole window (up to 30 s) before the watchdog could start the controller. Return here
        // without touching the failure counters so the very next tick retries.
        if (!isRootAvailable() && !isShizukuAvailable()) {
            Log.w(
                TAG,
                "uinput: no privilege yet (root=${isRootAvailable()} shizuku=${isShizukuAvailable()})",
            )
            return false
        }

        // Back off after repeated failures: 1s, 2s, 4s … capped at 30s. The first call always
        // probes, so a device that does support this is never delayed.
        val backoff = (1000L shl consecutiveProbeFailures.coerceAtMost(5)).coerceAtMost(30_000L)
        if (consecutiveProbeFailures > 0 &&
            SystemClock.uptimeMillis() - lastProbeFailureAt < backoff
        ) {
            return false
        }

        val writable = runShellCommandBlocking("[ -w /dev/uinput ] && echo yes || echo no")
        val available = writable.contains("yes")
        Log.i(TAG, "uinput: probe reply='${writable.trim()}' available=$available")
        if (available) {
            uinputProbeResult = true
            consecutiveProbeFailures = 0
        } else {
            lastProbeFailureAt = SystemClock.uptimeMillis()
            consecutiveProbeFailures++
        }
        return available
    }

    /** Starts the helper if needed. Returns the running process, or null when it cannot start. */
    private fun ensureDpadProcess(): IRemoteProcess? {
        dpadProcess?.let { if (it.alive()) return it }

        val path = nativeHelperPath() ?: return null
        if (!isAvailable()) return null

        return try {
            // Spawned through `sh -c` rather than as a bare executable. Running the helper directly
            // leaves its stdin unusable in some Shizuku versions, so commands are never answered
            // and every request fails. The shell wrapper keeps the pipe usable, which is the same
            // shape that works when the helper is driven by hand.
            val process: IRemoteProcess = if (isRootAvailable()) {
                LocalRemoteProcess(Runtime.getRuntime().exec(arrayOf("su", "-c", path)))
            } else {
                val binder = Shizuku.getBinder() ?: return null
                if (!binder.pingBinder()) return null
                IShizukuService.Stub.asInterface(binder).newProcess(arrayOf("sh", "-c", path), null, null)
            }
            dpadProcess = process
            dpadStdin = ParcelFileDescriptor.AutoCloseOutputStream(process.outputStream)
            dpadStdout = BufferedReader(
                InputStreamReader(ParcelFileDescriptor.AutoCloseInputStream(process.inputStream))
            )
            dpadDeviceCreated = false
            // Unsolicited rumble events must be read even while no command is in flight, so the
            // drain loop runs for as long as the helper does.
            startDpadDrain()
            process
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start virtual D-pad helper", e)
            dpadProcess = null
            null
        }
    }

    /**
     * Writes one command and reads one reply line.
     *
     * Kept deliberately synchronous: the helper answers every command, so a reply means the kernel
     * accepted the event. [timeoutMs] guards against a wedged helper.
     *
     * Rumble requests arrive on the same stdout but are *unsolicited* -- the helper prints `FFEVT`
     * lines whenever the active app rumbles, without any command pending. They are therefore routed
     * to [rumbleListener] here and skipped, so they can never be mistaken for the reply to the
     * command being waited on (which would desync the request/reply protocol and make every later
     * command read a stale line).
     */
    private fun dpadCommand(command: String, timeoutMs: Long = 500): String? {
        val stdin = dpadStdin ?: return null
        val stdout = dpadStdout ?: return null
        return try {
            // The write and the read must be one critical section against the drain thread. If the
            // write happened first and the lock were only taken for the read, the drain could win
            // the race, read this command's reply and discard it as "stale" before we ever saw it.
            synchronized(dpadReaderLock) {
                stdin.write((command + "\n").toByteArray())
                stdin.flush()

                val deadline = System.currentTimeMillis() + timeoutMs
                while (System.currentTimeMillis() < deadline) {
                    if (stdout.ready()) {
                        val line = stdout.readLine() ?: break
                        if (handleRumbleLine(line)) continue
                        if (line.isNotBlank()) return line
                    } else {
                        Thread.sleep(2)
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "D-pad command '$command' failed", e)
            null
        }
    }

    /**
     * Reads any already-buffered helper output, routing rumble events to [rumbleListener].
     *
     * Used by the background drain thread. Only complete, *ready* lines are consumed, so this never
     * blocks: a command's reply is left in the buffer for [dpadCommand] to read. Non-rumble lines
     * that are ready here are a stale reply from an earlier timed-out command, and are discarded so
     * they cannot be mistaken for the next reply.
     */
    private fun drainRumbleEvents() {
        val stdout = dpadStdout ?: return
        synchronized(dpadReaderLock) {
            while (stdout.ready()) {
                val line = stdout.readLine() ?: return
                if (!handleRumbleLine(line) && line.isNotBlank()) {
                    Log.w(TAG, "D-pad drain discarded a stale line: $line")
                }
            }
        }
    }

    /**
     * Starts the low-frequency drain loop, if it is not already running.
     *
     * 50 ms is short enough that rumble feels instant when triggered by a button press that itself
     * produced a command reply, and cheap because the loop is a single `ready()` check per tick
     * when nothing is waiting.
     */
    private fun startDpadDrain() {
        if (dpadDrainRunning) return
        dpadDrainRunning = true
        val thread = Thread {
            while (dpadDrainRunning) {
                runCatching { drainRumbleEvents() }
                runCatching { Thread.sleep(50) }
            }
        }
        thread.isDaemon = true
        thread.name = "dpad-rumble-drain"
        dpadDrainThread = thread
        thread.start()
    }

    private fun stopDpadDrain() {
        dpadDrainRunning = false
        dpadDrainThread = null
    }

    /**
     * Dispatches an `FFEVT <magnitude> <durationMs>` line to [rumbleListener].
     *
     * Returns true when the line was a rumble event (and therefore consumed), false for an ordinary
     * command reply. Malformed lines are treated as consumed but ignored, so a garbled event cannot
     * be mistaken for a reply either.
     */
    private fun handleRumbleLine(line: String): Boolean {
        if (!line.startsWith("FFEVT")) return false

        val parts = line.split(' ')
        if (parts.size >= 3) {
            val magnitude = parts[1].toIntOrNull()
            val duration = parts[2].toLongOrNull()
            if (magnitude != null && duration != null) {
                rumbleListener?.invoke(magnitude, duration)
            }
        }
        return true
    }

    /**
     * Sends one D-pad press, creating the device on first use.
     *
     * Returns true only when the helper confirmed the event was written, so callers can fall back
     * to key injection when the virtual device is unavailable.
     */
    fun sendDpad(direction: String): Boolean {
        if (!isUinputAvailable()) return false
        ensureDpadProcess() ?: return false

        if (!dpadDeviceCreated) {
            val created = dpadCommand("CREATE", timeoutMs = 1500)
            if (created?.contains("CREATED") != true) {
                Log.w(TAG, "Virtual D-pad CREATE rejected: $created")
                return false
            }
            dpadDeviceCreated = true
            // Give InputReader a moment to register the new device before the first press, or the
            // very first event can be dropped because no device is reading it yet.
            Thread.sleep(60)
        }
        return dpadCommand(direction)?.contains("OK") == true
    }

    // ---------------------------------------------------------------------
    // Resident gamepad (gamepad mode)
    // ---------------------------------------------------------------------

    /**
     * Whether the resident gamepad device is currently live.
     *
     * Deliberately a separate flag from [dpadDeviceCreated]: the two devices are independent, and
     * the arrow path must never be affected by whether a controller is being advertised.
     */
    private var gamepadDeviceCreated = false

    /**
     * Reports a controller as connected, for as long as gamepad mode is active in a chosen app.
     *
     * This creates the *second*, axes-bearing device. The phone therefore believes an Xbox pad is
     * plugged in, which is what makes controller testers and games see it. The D-pad device used
     * for arrow navigation is a different device and is not touched here.
     *
     * Safe to call repeatedly: a live device is reused.
     */
    fun startGamepad(): Boolean {
        if (gamepadDeviceCreated) return true
        if (!isUinputAvailable()) {
            Log.w(TAG, "Gamepad: uinput not available (helper path or /dev/uinput denied)")
            return false
        }
        val helper = nativeHelperPath()
        Log.i(TAG, "Gamepad: helper=$helper")
        if (ensureDpadProcess() == null) {
            Log.w(TAG, "Gamepad: could not start the helper process")
            return false
        }

        val created = dpadCommand("PAD_CREATE", timeoutMs = 1500)
        Log.i(TAG, "Gamepad: PAD_CREATE reply=$created")
        if (created?.contains("CREATED") != true) {
            Log.w(TAG, "Gamepad PAD_CREATE rejected: $created")
            return false
        }
        gamepadDeviceCreated = true
        // Let InputReader register the device before anything is sent to it.
        Thread.sleep(60)
        // Report what the device actually advertises for force feedback. This is the same
        // EVIOCGBIT source Android's VibratorInputMapper consults, so it tells us whether the
        // phone will treat this controller as rumble-capable. Strictly best-effort: a slow or
        // silent helper must not affect whether the controller is considered started.
        runCatching { Log.i(TAG, "Gamepad FF caps: " + dpadCommand("PAD_FFINFO", timeoutMs = 300)) }
        Log.i(TAG, "Resident gamepad device created")
        return true
    }

    /**
     * Presses or releases a controller button on the resident pad.
     *
     * [hidCode] is the linux HID usage (BTN_A, BTN_TL …), converted by the caller from the
     * Android key code. Sent through the gamepad device rather than injected as a key event, so
     * the app receives it as genuine controller input.
     */
    fun sendPadButton(hidCode: Int, isDown: Boolean): Boolean {
        if (!gamepadDeviceCreated) return false
        val command = if (isDown) "PAD_DOWN $hidCode" else "PAD_UP $hidCode"
        return dpadCommand(command)?.contains("OK") == true
    }

    /**
     * Sets both sticks on the resident pad. Values are in the stick range from the native helper.
     */
    fun sendPadStick(leftX: Int, leftY: Int, rightX: Int, rightY: Int): Boolean {
        if (!gamepadDeviceCreated) return false
        return dpadCommand("PAD_STICK $leftX $leftY $rightX $rightY")?.contains("OK") == true
    }

    /**
     * Moves the D-pad. Each axis is -1, 0 or 1.
     *
     * The D-pad is carried by the hat axes rather than button codes, which is how the stock Xbox
     * keylayout reads it; sending key codes for it would be ignored.
     */
    fun sendPadDpad(x: Int, y: Int): Boolean {
        if (!gamepadDeviceCreated) return false
        return dpadCommand("PAD_DPAD $x $y")?.contains("OK") == true
    }

    /**
     * Sets both triggers. 0 = released, 255 = fully pressed.
     *
     * Triggers are analog axes on the Xbox layout, so they are set to an absolute value rather
     * than pressed like buttons. They are driven fully on or fully off.
     */
    fun sendPadTrigger(left: Int, right: Int): Boolean {
        if (!gamepadDeviceCreated) return false
        return dpadCommand("PAD_TRIGGER $left $right")?.contains("OK") == true
    }

    /**
     * True while the resident gamepad device exists, regardless of who created it.
     *
     * Used by the service and the settings screen so both agree on whether the controller is
     * currently being advertised.
     */
    fun isGamepadRunning(): Boolean = gamepadDeviceCreated

    /** Removes the resident gamepad so the phone no longer reports a connected controller. */
    fun stopGamepad() {
        if (!gamepadDeviceCreated) return
        gamepadDeviceCreated = false
        runCatching { dpadCommand("PAD_DESTROY", timeoutMs = 500) }
        Log.i(TAG, "Resident gamepad device destroyed")
    }

    /** Tears the virtual device down and stops the helper. Safe to call repeatedly. */
    fun stopVirtualDpad() {
        try {
            if (dpadDeviceCreated) dpadCommand("DESTROY", timeoutMs = 500)
            dpadCommand("QUIT", timeoutMs = 200)
        } catch (_: Exception) {
        } finally {
            // Stop the drain before closing the stream it reads from, so it can never observe a
            // half-closed pipe.
            stopDpadDrain()
            dpadDeviceCreated = false
            // The helper destroys both devices when it exits, so the local flag must match.
            gamepadDeviceCreated = false
            runCatching { dpadStdin?.close() }
            runCatching { dpadStdout?.close() }
            runCatching { dpadProcess?.destroy() }
            dpadStdin = null
            dpadStdout = null
            dpadProcess = null
        }
    }



    fun isAvailable(): Boolean = isRootAvailable() || isShizukuAvailable()

    fun isShizukuAvailable(): Boolean = try {
        Shizuku.pingBinder() &&
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Exception) {
        false
    }

    /** True only if root was actually granted to this app. */
    fun isRootAvailable(): Boolean = _rootStatus.value == PermissionStatus.GRANTED

    /** Checks if su exists on the device, regardless of whether it was granted. */
    fun isDeviceRooted(): Boolean {
        val paths = mutableListOf(
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
            "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su",
            "/debug_ramdisk/su", "/data/adb/magisk", "/data/adb/ksu"
        )
        System.getenv("PATH")?.split(":")?.forEach { paths.add("$it/su") }
        return paths.any { runCatching { File(it).exists() }.getOrDefault(false) }
    }

    // ---------------------------------------------------------------------
    // Commands
    // ---------------------------------------------------------------------

    fun runShellCommand(command: String) {
        Thread { runShellCommandBlocking(command) }.start()
    }

    fun runShellCommandBlocking(command: String): String = when {
        isRootAvailable() -> runRootCommandBlocking(command)
        isShizukuAvailable() -> runShizukuCommandBlocking(command)
        else -> ""
    }

    private fun runShizukuCommandBlocking(command: String): String {
        try {
            val binder = Shizuku.getBinder()
            if (binder != null && binder.pingBinder()) {
                val service = IShizukuService.Stub.asInterface(binder)
                Log.d(TAG, "Shizuku exec: $command")
                val remoteProcess = service.newProcess(arrayOf("sh", "-c", command), null, null)
                val reader = BufferedReader(InputStreamReader(ParcelFileDescriptor.AutoCloseInputStream(remoteProcess.inputStream)))
                val output = reader.use { it.readText() }
                val exit = remoteProcess.waitFor()
                Log.d(TAG, "Shizuku exec done: exit=$exit out='${output.trim()}'")
                return output.trim()
            } else {
                Log.w(TAG, "Shizuku binder unavailable for '$command'")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Shizuku command error: $command", e)
        }
        return ""
    }

    private fun runRootCommandBlocking(command: String): String {
        var process: Process? = null
        var os: DataOutputStream? = null
        var reader: BufferedReader? = null
        try {
            process = Runtime.getRuntime().exec("su")
            os = DataOutputStream(process.outputStream)
            reader = BufferedReader(InputStreamReader(process.inputStream))

            os.writeBytes("$command\n")
            os.writeBytes("exit\n")
            os.flush()

            val output = reader.readText()
            process.waitFor()
            return output.trim()
        } catch (e: Exception) {
            Log.e(TAG, "Root command error: $command", e)
        } finally {
            try { os?.close() } catch (_: Exception) {}
            try { reader?.close() } catch (_: Exception) {}
            process?.destroy()
        }
        return ""
    }
}