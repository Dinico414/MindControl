package com.xenonware.mindcontroll

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import rikka.shizuku.Shizuku

class ButtonMapperService : AccessibilityService() {

    private val tag = "ButtonMapper"
    private var clickCount = 0
    private val clickDelay = 350L

    private var isCameraInUse = false
    private var isLongPress = false
    private var isFlashlightOn = false
    private var lastPackageName: String? = null
    private var lastKeyCode: Int = -1

    private lateinit var audioManager: AudioManager
    private lateinit var cameraManager: CameraManager
    private lateinit var powerManager: PowerManager
    private var wakeLock: PowerManager.WakeLock? = null

    private val cameraCallback = object : CameraManager.AvailabilityCallback() {
        override fun onCameraAvailable(cameraId: String) { isCameraInUse = false }
        override fun onCameraUnavailable(cameraId: String) { isCameraInUse = true }
    }

    override fun onServiceConnected() {
        Log.d(tag, "Service Connected")
        
        // Android 15 Foreground Requirement
        createNotificationChannel()
        val notification = Notification.Builder(this, "service_channel")
            .setContentTitle("MindControll Active")
            .setContentText("Monitoring hardware buttons...")
            .setSmallIcon(android.R.drawable.ic_menu_preferences)
            .build()
        startForeground(1, notification)

        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        powerManager = getSystemService(POWER_SERVICE) as PowerManager
        cameraManager.registerAvailabilityCallback(cameraCallback, handler)

        // WakeLock to keep service alive when screen is off
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MindControll:KeyCaptureLock")
        wakeLock?.acquire()

        // Listen for Shizuku Binder
        Shizuku.addBinderReceivedListenerSticky(binderListener)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        
        tryStartShizuku()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel("service_channel", "MindControll Service", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private val binderListener = Shizuku.OnBinderReceivedListener {
        Log.d(tag, "Shizuku Binder Received")
        tryStartShizuku()
    }

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, result ->
        if (result == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.d(tag, "Shizuku Permission Granted")
            tryStartShizuku()
        }
    }

    private fun tryStartShizuku() {
        try {
            if (Shizuku.pingBinder() && 
                Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                ShizukuManager.startMonitoring { keyCode, isDown ->
                    handler.post { handleKeyEvent(keyCode, isDown, fromShizuku = true) }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Shizuku start error", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            lastPackageName = event.packageName?.toString()
        }
    }
    override fun onInterrupt() {}

    private val handler = Handler(Looper.getMainLooper())
    private val longPressRunnables = mutableMapOf<Int, Runnable>()
    private var pendingMultiClick: Runnable? = null

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.repeatCount > 0) return true
        val keyCode = event.keyCode
        val isDown = event.action == KeyEvent.ACTION_DOWN
        
        // DEBUG: Log EVERY key to see if the service is alive
        Log.v(tag, "ACCESSIBILITY RAW: keyCode=$keyCode action=${if(isDown) "DOWN" else "UP"}")

        val isTargetKey = keyCode in setOf(134, 27, 25, 24, 131)
        if (!isTargetKey) return false

        return handleKeyEvent(keyCode, isDown, fromShizuku = false)
    }

    private fun handleKeyEvent(keyCode: Int, isDown: Boolean, fromShizuku: Boolean): Boolean {
        val km = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        val isLocked = km.isKeyguardLocked
        val isInteractive = powerManager.isInteractive
        val state = if (isInteractive && !isLocked) "ON" else "OFF"

        if (fromShizuku) {
            Log.v(tag, "SHIZUKU KEY: $keyCode ${if (isDown) "DOWN" else "UP"} [Locked=$isLocked, State=$state]")
        } else {
            Log.v(tag, "ACCESSIBILITY KEY: $keyCode ${if (isDown) "DOWN" else "UP"} [Locked=$isLocked, State=$state]")
        }

        // Camera override check
        val currentPackage = rootInActiveWindow?.packageName?.toString() ?: lastPackageName
        val isCameraApp = currentPackage?.contains("camera", ignoreCase = true) == true
        if (isCameraInUse && SettingsManager.isDisableInCamera(this) && isCameraApp) {
            return false
        }

        if (isDown) {
            isLongPress = false
            if (keyCode != lastKeyCode) {
                clickCount = 0
                pendingMultiClick?.let { handler.removeCallbacks(it) }
            }
            lastKeyCode = keyCode

            longPressRunnables.remove(keyCode)?.let { handler.removeCallbacks(it) }

            val capturedState = state
            val longPressRunnable = Runnable {
                if (clickCount == 0) {
                    isLongPress = true
                    Log.d(tag, "Long Press triggered for $keyCode (state=$capturedState)")
                    performAction(keyCode, capturedState, "LONG")
                }
            }
            longPressRunnables[keyCode] = longPressRunnable
            handler.postDelayed(longPressRunnable, 500L)
            return true

        } else {
            longPressRunnables.remove(keyCode)?.let { handler.removeCallbacks(it) }

            if (isLongPress) {
                isLongPress = false
                clickCount = 0
                return true
            }

            clickCount++
            val capturedKeyCode = keyCode
            val capturedState = state

            pendingMultiClick?.let { handler.removeCallbacks(it) }
            val multiClickRunnable = Runnable {
                val type = when (clickCount) {
                    1 -> "SINGLE"; 2 -> "DOUBLE"; 3 -> "TRIPLE"; else -> "MULTI"
                }
                Log.d(tag, "Multi-click triggered: $type for $capturedKeyCode (state=$capturedState)")
                performAction(capturedKeyCode, capturedState, type)
                clickCount = 0
                pendingMultiClick = null
            }
            pendingMultiClick = multiClickRunnable
            handler.postDelayed(multiClickRunnable, clickDelay)
            return true
        }
    }

    private fun performAction(keyCode: Int, state: String, type: String) {
        val action = SettingsManager.getAction(this, keyCode, state, type)
        Log.i(tag, ">>>> EXECUTING: $action [Key: $keyCode, State: $state, Type: $type, Locked: ${powerManager.isInteractive}] <<<<")

        val success = when (action) {
            SettingsManager.ACTION_DEFAULT -> { simulateDefaultBehavior(keyCode); true }
            SettingsManager.ACTION_PLAY_PAUSE -> { dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE); true }
            SettingsManager.ACTION_NEXT -> { dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT); true }
            SettingsManager.ACTION_PREVIOUS -> { dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS); true }
            SettingsManager.ACTION_VOLUME_UP -> { adjustVolume(AudioManager.ADJUST_RAISE); true }
            SettingsManager.ACTION_VOLUME_DOWN -> { adjustVolume(AudioManager.ADJUST_LOWER); true }
            SettingsManager.ACTION_FLASHLIGHT -> { toggleFlashlight(); true }
            SettingsManager.ACTION_SCREENSHOT -> performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            SettingsManager.ACTION_LOCK -> performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            SettingsManager.ACTION_HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
            SettingsManager.ACTION_BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
            SettingsManager.ACTION_RECENTS -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            SettingsManager.ACTION_NOTIFICATIONS -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            SettingsManager.ACTION_QUICK_SETTINGS -> performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
            SettingsManager.ACTION_ASSISTANT -> { launchAssistant(); true }
            SettingsManager.ACTION_BRIGHTNESS_UP -> { adjustBrightness(20); true }
            SettingsManager.ACTION_BRIGHTNESS_DOWN -> { adjustBrightness(-20); true }
            SettingsManager.ACTION_ROTATE_TOGGLE -> { toggleRotation(); true }
            SettingsManager.ACTION_NONE -> { Log.d(tag, "Action is NONE, key blocked."); true }
            else -> false
        }
        
        if (!success) {
            Log.e(tag, "FAILED to execute action: $action (This often happens on Lock Screens for Home/Recents/Screenshot)")
        }
    }

    private fun simulateDefaultBehavior(keyCode: Int) {
        Log.d(tag, "Simulating default for $keyCode")
        when (keyCode) {
            24 -> audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            25 -> audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            27 -> dispatchMediaKey(KeyEvent.KEYCODE_CAMERA)
            134 -> dispatchMediaKey(KeyEvent.KEYCODE_FOCUS)
            131 -> launchAssistant()
        }
    }

    private fun dispatchMediaKey(keyCode: Int) {
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    private fun adjustVolume(direction: Int) {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
    }

    private fun toggleFlashlight() {
        try {
            val cameraId = cameraManager.cameraIdList[0]
            isFlashlightOn = !isFlashlightOn
            cameraManager.setTorchMode(cameraId, isFlashlightOn)
        } catch (e: Exception) {
            Log.e(tag, "Flashlight error", e)
        }
    }

    private fun launchAssistant() {
        val intent = Intent(Intent.ACTION_VOICE_COMMAND)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(tag, "Assistant error", e)
        }
    }

    private fun adjustBrightness(delta: Int) {
        try {
            val currentBrightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            val newBrightness = (currentBrightness + delta).coerceIn(0, 255)
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, newBrightness)
        } catch (e: Exception) {
            Log.e(tag, "Brightness error. Needs WRITE_SETTINGS permission?", e)
        }
    }

    private fun toggleRotation() {
        try {
            // Disable autorotation first
            Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
            
            val currentRotation = Settings.System.getInt(contentResolver, Settings.System.USER_ROTATION)
            val newRotation = if (currentRotation == 0) 1 else 0 // Toggle between Portrait (0) and Landscape (1)
            
            Settings.System.putInt(contentResolver, Settings.System.USER_ROTATION, newRotation)
            Log.d(tag, "Rotation toggled to $newRotation")
        } catch (e: Exception) {
            Log.e(tag, "Rotation toggle error", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager.unregisterAvailabilityCallback(cameraCallback)
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        Shizuku.removeBinderReceivedListener(binderListener)
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        ShizukuManager.stopMonitoring()
    }
}
