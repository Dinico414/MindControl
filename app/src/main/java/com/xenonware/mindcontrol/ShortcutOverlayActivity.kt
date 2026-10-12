package com.xenonware.mindcontrol

import android.app.ActivityOptions
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

class ShortcutOverlayActivity : ComponentActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var launched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Dismiss AOD if running
        sendBroadcast(Intent("com.xenonware.mindcontrol.ACTION_DISMISS_AOD"))

        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val useOverlay = intent.getBooleanExtra("USE_OVERLAY", true)

        setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (useOverlay) Color.Black else Color.Transparent)
            )
        }

        val shortcutData = intent.getStringExtra("SHORTCUT_DATA")
        if (!shortcutData.isNullOrEmpty()) {
            val km = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            if (km.isKeyguardLocked) {
                Log.d(TAG, "Device is locked. Requesting keyguard unlock engagement...")
                km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                    override fun onDismissSucceeded() {
                        Log.d(TAG, "Keyguard dismiss succeeded! Launching shortcut...")
                        triggerShortcutLaunch(shortcutData)
                    }

                    override fun onDismissCancelled() {
                        Log.d(TAG, "Keyguard dismiss cancelled by user")
                        finishAndRemoveTask()
                    }

                    override fun onDismissError() {
                        Log.e(TAG, "Keyguard dismiss error, attempting fallback launch")
                        triggerShortcutLaunch(shortcutData)
                    }
                })
            } else {
                Log.d(TAG, "Device is not locked. Launching shortcut directly...")
                triggerShortcutLaunch(shortcutData)
            }
        } else {
            finishAndRemoveTask()
        }
    }

    private fun triggerShortcutLaunch(shortcutData: String) {
        if (!isFinishing && !isDestroyed && !launched) {
            launched = true
            launchShortcut(this, shortcutData)
            handler.postDelayed({
                if (!isFinishing) {
                    finishAndRemoveTask()
                }
            }, 600)
        }
    }

    override fun onPause() {
        super.onPause()
        if (launched) {
            handler.postDelayed({
                if (!isFinishing) {
                    finishAndRemoveTask()
                }
            }, 300)
        }
    }

    companion object {
        private const val TAG = "ShortcutOverlayActivity"

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
