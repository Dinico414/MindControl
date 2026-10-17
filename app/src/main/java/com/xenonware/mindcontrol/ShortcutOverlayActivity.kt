package com.xenonware.mindcontrol

import android.app.ActivityOptions
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import java.lang.ref.WeakReference

/**
 * Fires a shortcut for [ButtonMapperService].
 *
 * Unlocked device:  black overlay (if enabled) -> shortcut.
 *
 * Locked device (screen off or lock screen showing): this activity is deliberately NOT shown over
 * the keyguard. It wakes the screen, asks the system for the unlock prompt and then waits hidden
 * behind the lock screen. Waking up alone does nothing. Only once the keyguard is really gone
 * (onDismissSucceeded / USER_PRESENT / onResume, each confirmed against KeyguardManager) does the
 * black overlay appear, and the shortcut is launched after that black frame has been drawn.
 *
 * The black overlay stays on screen through the whole handoff: after launching the shortcut this
 * activity does NOT finish on a timer. It finishes in onStop, i.e. only once the shortcut's window
 * is fully covering ours, so there is never a frame of the launcher/lock screen showing through.
 */
class ShortcutOverlayActivity : ComponentActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var keyguardManager: KeyguardManager
    private lateinit var powerManager: PowerManager

    private var shortcutData: String? = null
    private var useOverlay = true
    private var isBlackSheetVisible by mutableStateOf(false)

    private var waitingForUnlock = false
    private var unlocked = false
    private var launched = false
    private var receiverRegistered = false
    private var dismissRequestedAt = 0L
    private var screenOnAt = 0L
    private var dismissRetries = 0
    private var confirmAttempts = 0

    private val unlockTimeout = Runnable {
        Log.w(TAG, "Device wasn't unlocked within ${UNLOCK_TIMEOUT_MS / 1000}s. Aborting shortcut launch.")
        abort()
    }

    private val screenOnFallback = Runnable { evaluate(screenWaitExpired = true) }

    /** Unlock signals can arrive a moment before KeyguardManager agrees, so confirm before launching. */
    private val confirmUnlock = object : Runnable {
        override fun run() {
            if (!isPending()) return
            when {
                !keyguardManager.isKeyguardLocked -> onDeviceUnlocked()
                ++confirmAttempts < CONFIRM_MAX_ATTEMPTS -> handler.postDelayed(this, CONFIRM_INTERVAL_MS)
                else -> Log.d(TAG, "Keyguard still locked, still waiting for the unlock.")
            }
        }
    }

    /** A cancel can race with a successful unlock, so give it a moment before treating it as "user canceled". */
    private val cancelCheck = Runnable {
        if (!isPending()) return@Runnable
        if (!keyguardManager.isKeyguardLocked) {
            onUnlockSignal("unlocked after cancel")
        } else {
            Log.d(TAG, "Unlock cancelled by user. Aborting shortcut launch.")
            abort()
        }
    }

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_USER_PRESENT -> onUnlockSignal("USER_PRESENT")
                Intent.ACTION_SCREEN_ON -> {
                    screenOnAt = SystemClock.elapsedRealtime()
                    if (!waitingForUnlock) evaluate()
                }
                Intent.ACTION_SCREEN_OFF -> if (waitingForUnlock && isPending()) {
                    Log.d(TAG, "Screen turned off before the device was unlocked. Aborting shortcut launch.")
                    abort()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Dismiss AOD if running
        sendBroadcast(
            Intent("com.xenonware.mindcontrol.ACTION_DISMISS_AOD").setPackage(packageName)
        )

        // A newer trigger replaces one that is still waiting, so a shortcut never fires twice.
        activeInstance?.get()?.takeIf { it !== this && !it.isFinishing }?.finish()
        activeInstance = WeakReference(this)

        keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        powerManager = getSystemService(POWER_SERVICE) as PowerManager

        // Never draw over the lock screen: while locked, this activity stays hidden behind the
        // keyguard, so the user only sees the normal lock screen and the unlock prompt.
        setShowWhenLocked(false)
        // Lets requestDismissKeyguard() turn the screen on if it is still off.
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isBlackSheetVisible) Color.Black else Color.Transparent)
            )
        }

        ContextCompat.registerReceiver(
            this,
            stateReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        receiverRegistered = true

        start(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Only reached with a single* launchMode: treat it as a fresh request.
        setIntent(intent)
        handler.removeCallbacksAndMessages(null)
        waitingForUnlock = false
        unlocked = false
        launched = false
        dismissRetries = 0
        isBlackSheetVisible = false
        start(intent)
    }

    override fun onResume() {
        super.onResume()
        // Being resumed while waiting usually means the keyguard is going away. The confirmation loop
        // makes sure we only continue once it is really gone.
        if (waitingForUnlock) onUnlockSignal("onResume")
    }

    override fun onStop() {
        super.onStop()
        // We are only stopped after launching once the shortcut's window fully covers ours, so the
        // black has done its job and can be removed now with no visible gap. (If we were stopped
        // for some other reason while still waiting, isPending() is true, and we leave it running.)
        if (launched) finishWithoutAnimation()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (receiverRegistered) {
            unregisterReceiver(stateReceiver)
            receiverRegistered = false
        }
        if (activeInstance?.get() === this) activeInstance = null
        super.onDestroy()
    }

    private fun start(intent: Intent) {
        shortcutData = intent.getStringExtra(EXTRA_SHORTCUT_DATA)
        useOverlay = intent.getBooleanExtra(EXTRA_USE_OVERLAY, true)
        if (shortcutData.isNullOrEmpty()) {
            finishAndRemoveTask()
            return
        }
        evaluate()
    }

    private fun evaluate(screenWaitExpired: Boolean = false) {
        if (!isPending() || waitingForUnlock) return
        handler.removeCallbacks(screenOnFallback)
        when {
            keyguardManager.isKeyguardLocked -> waitForUnlock()
            !powerManager.isInteractive && !screenWaitExpired -> {
                // Screen is off but the keyguard isn't up (yet). Decide once the screen is on,
                // so a lock that kicks in right now is still respected.
                Log.d(TAG, "Screen is off and not locked (yet). Waiting for the screen to turn on...")
                handler.postDelayed(screenOnFallback, SCREEN_ON_WAIT_MS)
            }
            else -> {
                Log.d(TAG, "Device is unlocked.")
                onDeviceUnlocked()
            }
        }
    }

    private fun waitForUnlock() {
        waitingForUnlock = true
        Log.d(TAG, "Device is locked. Requesting unlock prompt; the shortcut waits for the actual unlock.")
        handler.removeCallbacks(unlockTimeout)
        handler.postDelayed(unlockTimeout, UNLOCK_TIMEOUT_MS)
        requestUnlockPrompt()
    }

    private fun requestUnlockPrompt() {
        if (!isPending()) return
        dismissRequestedAt = SystemClock.elapsedRealtime()
        keyguardManager.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
            override fun onDismissSucceeded() = onUnlockSignal("onDismissSucceeded")
            override fun onDismissCancelled() = onUnlockPromptClosed(cancelled = true)
            override fun onDismissError() = onUnlockPromptClosed(cancelled = false)
        })
    }

    private fun onUnlockPromptClosed(cancelled: Boolean) {
        if (!isPending()) return
        if (!keyguardManager.isKeyguardLocked) {
            onUnlockSignal("prompt closed, keyguard gone")
            return
        }
        val sinceShown = SystemClock.elapsedRealtime() - maxOf(dismissRequestedAt, screenOnAt)
        // A cancel this quick comes from the system (e.g. the wake-up transition resetting the
        // bouncer), not from the user. Errors (e.g. the AOD activity still on top) are retried too.
        val causedBySystem = !cancelled || sinceShown < TRANSIENT_CANCEL_WINDOW_MS
        when {
            causedBySystem && dismissRetries < MAX_DISMISS_RETRIES -> {
                dismissRetries++
                Log.d(TAG, "Unlock prompt ${if (cancelled) "cancelled" else "failed"} by the system, retry $dismissRetries/$MAX_DISMISS_RETRIES")
                handler.postDelayed({ requestUnlockPrompt() }, DISMISS_RETRY_DELAY_MS)
            }
            cancelled -> {
                handler.removeCallbacks(cancelCheck)
                handler.postDelayed(cancelCheck, CANCEL_GRACE_MS)
            }
            else -> {
                // Last resort for ROMs that reject the request. The user can also just unlock
                // normally: USER_PRESENT still triggers the shortcut.
                Log.w(TAG, "Unlock prompt unavailable via KeyguardManager. Waiting for a manual unlock.")
                if (ShellManager.isAvailable()) {
                    Thread { ShellManager.runShellCommand("wm dismiss-keyguard") }.start()
                }
            }
        }
    }

    private fun onUnlockSignal(source: String) {
        if (!isPending()) return
        Log.d(TAG, "Unlock signal: $source")
        confirmAttempts = 0
        handler.removeCallbacks(confirmUnlock)
        handler.post(confirmUnlock)
    }

    private fun onDeviceUnlocked() {
        if (!isPending()) return
        unlocked = true
        waitingForUnlock = false
        handler.removeCallbacks(unlockTimeout)
        handler.removeCallbacks(screenOnFallback)
        handler.removeCallbacks(confirmUnlock)
        handler.removeCallbacks(cancelCheck)

        val data = shortcutData ?: run { abort(); return }
        if (useOverlay) {
            Log.d(TAG, "Unlocked. Showing black overlay, shortcut launches once it is drawn.")
            isBlackSheetVisible = true
            runAfterOverlayDrawn { triggerShortcutLaunch(data) }
        } else {
            Log.d(TAG, "Unlocked. Launching shortcut.")
            triggerShortcutLaunch(data)
        }
    }

    /** Runs [action] once the black frame has been submitted, so the overlay really comes first. */
    private fun runAfterOverlayDrawn(action: () -> Unit) {
        var done = false
        val runOnce = Runnable {
            if (!done) {
                done = true
                action()
            }
        }
        window.decorView.viewTreeObserver.registerFrameCommitCallback(runOnce)
        window.decorView.invalidate()
        handler.postDelayed(runOnce, OVERLAY_DRAW_FALLBACK_MS)
    }

    private fun triggerShortcutLaunch(shortcutData: String) {
        if (isFinishing || isDestroyed || launched) return
        launched = true
        launchShortcut(this, shortcutData)
        // Do NOT finish on a timer here: finishing before the shortcut covers us would reveal
        // whatever is behind the black. onStop does the finish once the shortcut is on top. This
        // is only a safety net for the case where the shortcut opens nothing at all, so we don't
        // sit on a black screen forever.
        handler.postDelayed({ if (!isFinishing) finishWithoutAnimation() }, LAUNCH_SAFETY_FINISH_MS)
    }

    private fun abort() {
        handler.removeCallbacksAndMessages(null)
        waitingForUnlock = false
        if (!isFinishing) finishAndRemoveTask()
    }

    private fun finishWithoutAnimation() {
        if (isFinishing) return
        handler.removeCallbacksAndMessages(null)
        finishAndRemoveTask()
        // No exit animation, so the black never fades out to reveal content behind it.
        overridePendingTransition(0, 0)
    }

    /** True while we may still move towards launching (not unlocked yet, not finishing). */
    private fun isPending() = !unlocked && !isFinishing && !isDestroyed

    companion object {
        private const val TAG = "ShortcutOverlayActivity"

        const val EXTRA_SHORTCUT_DATA = "SHORTCUT_DATA"
        const val EXTRA_USE_OVERLAY = "USE_OVERLAY"

        private const val UNLOCK_TIMEOUT_MS = 60_000L
        private const val SCREEN_ON_WAIT_MS = 1_500L
        private const val TRANSIENT_CANCEL_WINDOW_MS = 1_500L
        private const val CANCEL_GRACE_MS = 1_000L
        private const val MAX_DISMISS_RETRIES = 2
        private const val DISMISS_RETRY_DELAY_MS = 300L
        private const val CONFIRM_INTERVAL_MS = 50L
        private const val CONFIRM_MAX_ATTEMPTS = 60
        private const val OVERLAY_DRAW_FALLBACK_MS = 150L
        private const val LAUNCH_SAFETY_FINISH_MS = 6_000L

        private var activeInstance: WeakReference<ShortcutOverlayActivity>? = null

        fun launchShortcut(context: Context, shortcutData: String) {
            try {
                val parts = shortcutData.split("||")
                val data = parts[0]

                if (data.startsWith("intent:")) {
                    Log.d(TAG, "Launching shortcut via URI")
                    val intent = Intent.parseUri(data, 0)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                    val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    val pendingIntent = PendingIntent.getActivity(context, 0, intent, flags)

                    val startMode = if (Build.VERSION.SDK_INT >= 36) {
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
                    } else {
                        @Suppress("DEPRECATION")
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                    }
                    val bundle = ActivityOptions.makeBasic()
                        .setPendingIntentBackgroundActivityStartMode(startMode)
                        .toBundle()
                    pendingIntent.send(bundle)
                } else {
                    Log.d(TAG, "Launching shortcut via package name: $data")
                    val intent = context.packageManager.getLaunchIntentForPackage(data)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Shortcut launch error", e)
            }
        }
    }
}