package com.xenonware.mindcontrol

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import moe.shizuku.server.IShizukuService
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

enum class PermissionStatus { UNAVAILABLE, AVAILABLE, DENIED, GRANTED }

object ShellManager {
    private const val TAG = "ShellManager"
    private const val PREFS = "shell_permissions"
    private const val KEY_ROOT_RESULT = "root_result" // "granted" | "denied" | absent = never asked
    private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
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
        prefs().edit().putString(KEY_ROOT_RESULT, if (granted) "granted" else "denied").apply()
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
        var remoteProcess: moe.shizuku.server.IRemoteProcess? = null
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
                val remoteProcess = service.newProcess(arrayOf("sh", "-c", command), null, null)
                val reader = BufferedReader(InputStreamReader(ParcelFileDescriptor.AutoCloseInputStream(remoteProcess.inputStream)))
                val output = reader.use { it.readText() }
                remoteProcess.waitFor()
                return output.trim()
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