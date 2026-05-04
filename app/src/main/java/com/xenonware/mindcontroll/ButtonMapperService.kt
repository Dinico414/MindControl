package com.xenonware.mindcontroll

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.Context
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

class ButtonMapperService : AccessibilityService() {

    private val TAG = "ButtonMapper"
    private val handler = Handler(Looper.getMainLooper())
    private var clickCount = 0
    private var lastKeyCode = -1
    private val CLICK_DELAY = 350L

    private var isCameraInUse = false
    private var isLongPress = false
    private var isFlashlightOn = false

    private lateinit var audioManager: AudioManager
    private lateinit var cameraManager: CameraManager
    private lateinit var powerManager: PowerManager

    private val cameraCallback = object : CameraManager.AvailabilityCallback() {
        override fun onCameraAvailable(cameraId: String) { isCameraInUse = false }
        override fun onCameraUnavailable(cameraId: String) { isCameraInUse = true }
    }

    override fun onServiceConnected() {
        Log.d(TAG, "Service Connected")
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        cameraManager.registerAvailabilityCallback(cameraCallback, handler)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val action = event.action

        // Only handle specific keys requested by user
        val isTargetKey = when (keyCode) {
            134, 27, 25, 24, 131 -> true
            else -> false
        }
        if (!isTargetKey) return false

        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val isLocked = km.isKeyguardLocked
        val isInteractive = powerManager.isInteractive
        
        // Use "OFF" mapping if locked or screen is off
        val state = if (isInteractive && !isLocked) "ON" else "OFF"

        // Handle camera-in-use override
        if (isCameraInUse && SettingsManager.isDisableInCamera(this)) {
            Log.d(TAG, "Camera is in use and override enabled. Passing through key $keyCode.")
            return false // Let system handle the key (e.g., shutter button)
        }

        if (action == KeyEvent.ACTION_DOWN) {
            Log.d(TAG, "onKeyDown: $keyCode (State: $state, Locked: $isLocked)")
            isLongPress = false
            handler.removeCallbacksAndMessages("longPress")
            
            handler.postAtTime({
                if (clickCount == 0) {
                    isLongPress = true
                    performAction(keyCode, state, "LONG")
                }
            }, "longPress", android.os.SystemClock.uptimeMillis() + 500L)
            return true
        } else if (action == KeyEvent.ACTION_UP) {
            Log.d(TAG, "onKeyUp: $keyCode")
            handler.removeCallbacksAndMessages("longPress")
            
            if (isLongPress) {
                isLongPress = false
                return true
            }

            clickCount++
            lastKeyCode = keyCode
            handler.removeCallbacksAndMessages("multiClick")
            handler.postAtTime({
                val type = when (clickCount) {
                    1 -> "SINGLE"
                    2 -> "DOUBLE"
                    3 -> "TRIPLE"
                    else -> "MULTI"
                }
                performAction(lastKeyCode, state, type)
                clickCount = 0
            }, "multiClick", android.os.SystemClock.uptimeMillis() + CLICK_DELAY)
            return true
        }
        return false
    }

    private fun performAction(keyCode: Int, state: String, type: String) {
        val action = SettingsManager.getAction(this, keyCode, state, type)
        Log.i(TAG, "Executing Action: $action for Key: $keyCode, State: $state, Type: $type")

        when (action) {
            SettingsManager.ACTION_DEFAULT -> simulateDefaultBehavior(keyCode)
            SettingsManager.ACTION_PLAY_PAUSE -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            SettingsManager.ACTION_NEXT -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
            SettingsManager.ACTION_PREVIOUS -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            SettingsManager.ACTION_VOLUME_UP -> adjustVolume(AudioManager.ADJUST_RAISE)
            SettingsManager.ACTION_VOLUME_DOWN -> adjustVolume(AudioManager.ADJUST_LOWER)
            SettingsManager.ACTION_FLASHLIGHT -> toggleFlashlight()
            SettingsManager.ACTION_SCREENSHOT -> performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            SettingsManager.ACTION_LOCK -> performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            SettingsManager.ACTION_HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
            SettingsManager.ACTION_BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
            SettingsManager.ACTION_RECENTS -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            SettingsManager.ACTION_NOTIFICATIONS -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            SettingsManager.ACTION_QUICK_SETTINGS -> performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
            SettingsManager.ACTION_ASSISTANT -> launchAssistant()
            SettingsManager.ACTION_BRIGHTNESS_UP -> adjustBrightness(20)
            SettingsManager.ACTION_BRIGHTNESS_DOWN -> adjustBrightness(-20)
            SettingsManager.ACTION_NONE -> Log.d(TAG, "Action is NONE, blocking button.")
        }
    }

    private fun simulateDefaultBehavior(keyCode: Int) {
        Log.d(TAG, "Simulating default behavior for $keyCode")
        when (keyCode) {
            24 -> adjustVolume(AudioManager.ADJUST_RAISE)
            25 -> adjustVolume(AudioManager.ADJUST_LOWER)
            27 -> {
                // Usually camera shutter
                dispatchMediaKey(KeyEvent.KEYCODE_CAMERA)
            }
            134 -> {
                // Focus button - might be hard to simulate perfectly
                dispatchMediaKey(KeyEvent.KEYCODE_FOCUS)
            }
            131 -> {
                // AI button - usually assistant
                launchAssistant()
            }
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
            Log.e(TAG, "Flashlight error", e)
        }
    }

    private fun launchAssistant() {
        val intent = Intent(Intent.ACTION_VOICE_COMMAND)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Assistant error", e)
        }
    }

    private fun adjustBrightness(delta: Int) {
        try {
            val currentBrightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            val newBrightness = (currentBrightness + delta).coerceIn(0, 255)
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, newBrightness)
        } catch (e: Exception) {
            Log.e(TAG, "Brightness error. Needs WRITE_SETTINGS permission?", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager.unregisterAvailabilityCallback(cameraCallback)
    }
}
