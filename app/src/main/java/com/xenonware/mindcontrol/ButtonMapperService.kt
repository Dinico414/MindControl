package com.xenonware.mindcontrol

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
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.WindowManager
import rikka.shizuku.Shizuku

class ButtonMapperService : AccessibilityService() {

    private val tag = "ButtonMapper"
    private var clickCount = 0
    private val clickDelay = 350L

    private var isCameraInUse = false
    private var isLongPress = false
    private var isFlashlightOn = false
    private var lastPackageName: String? = null
    private var isVolumePanelVisibleState = false
    private var volumePanelTimeoutRunnable: Runnable? = null
    private var lastKeyCode: Int = -1
    
    // Track shutter button state to filter out 134 (focus) events
    private var isShutterKeyPressed = false
    private var ignoreNextFocusUp = false
    private var pendingFocusDown: Runnable? = null

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
            .setContentTitle("MindControl Active")
            .setContentText("Monitoring hardware buttons...")
            .setSmallIcon(android.R.drawable.ic_menu_preferences)
            .build()
        startForeground(1, notification)

        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        powerManager = getSystemService(POWER_SERVICE) as PowerManager
        cameraManager.registerAvailabilityCallback(cameraCallback, handler)

        // WakeLock to keep service alive when screen is off
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MindControl:KeyCaptureLock")
        wakeLock?.acquire()

        // Listen for Shizuku Binder
        Shizuku.addBinderReceivedListenerSticky(binderListener)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        
        tryStartShizuku()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel("service_channel", "MindControl Service", NotificationManager.IMPORTANCE_LOW)
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
        if (event == null) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString()
            val className = event.className?.toString()
            lastPackageName = pkg

            if (pkg == "com.android.systemui") {
                // Heuristic: Volume dialog in SystemUI often uses these class names or contains volume in view IDs
                // We'll mark it visible and set a timeout since we can't easily detect when it closes
                val isVolumeDialog = className?.contains("Volume", ignoreCase = true) == true || 
                                     className?.contains("Dialog", ignoreCase = true) == true

                if (isVolumeDialog) {
                    if (!isVolumePanelVisibleState) Log.d(tag, "Volume Panel Visible (detected via AccessibilityEvent)")
                    isVolumePanelVisibleState = true
                    volumePanelTimeoutRunnable?.let { handler.removeCallbacks(it) }
                    
                    // Volume panel typically auto-dismisses after 3 seconds
                    volumePanelTimeoutRunnable = Runnable { 
                        Log.d(tag, "Volume Panel Invisible (timeout)")
                        isVolumePanelVisibleState = false 
                    }
                    handler.postDelayed(volumePanelTimeoutRunnable!!, 3500L)
                }
            }
        }
    }
    override fun onInterrupt() {}

    private val handler = Handler(Looper.getMainLooper())
    private val longPressRunnables = mutableMapOf<Int, Runnable>()
    private val keyClearTasks = mutableMapOf<Int, Runnable>()
    private var pendingMultiClick: Runnable? = null
    
    // Continuous action state
    private var continuousActionTask: Runnable? = null
    
    // Track physical key state to deduplicate events from Shizuku and AccessibilityService
    private val lastEventTimes = mutableMapOf<Pair<Int, Boolean>, Long>()
    private val lastEventSources = mutableMapOf<Pair<Int, Boolean>, Boolean>() // true = Shizuku, false = Accessibility

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.repeatCount > 0) return true
        val keyCode = event.keyCode
        val isDown = event.action == KeyEvent.ACTION_DOWN
        
        // DEBUG: Log EVERY key to see if the service is alive
        Log.v(tag, "ACCESSIBILITY RAW: keyCode=$keyCode action=${if(isDown) "DOWN" else "UP"}")

        val isTargetKey = keyCode in setOf(134, 27, 25, 24, 131, 132, 133, 111)
        if (!isTargetKey) return false

        return handleKeyEvent(keyCode, isDown, fromShizuku = false)
    }

    private fun processKeyEvent(keyCode: Int, isDown: Boolean, state: String) {
        if (isDown) {
            isLongPress = false
            if (keyCode != lastKeyCode) {
                clickCount = 0
                pendingMultiClick?.let { handler.removeCallbacks(it) }
                stopContinuousAction()
            }
            lastKeyCode = keyCode

            longPressRunnables.remove(keyCode)?.let { handler.removeCallbacks(it) }

            if (keyCode == 132 || keyCode == 133) {
                return
            }

            val capturedState = state
            val longPressRunnable = Runnable {
                if (clickCount == 0) {
                    isLongPress = true
                    Log.d(tag, "Long Press triggered for $keyCode (state=$capturedState)")
                    performAction(keyCode, capturedState, "LONG")
                    startContinuousAction(keyCode, capturedState, "LONG")
                }
            }
            longPressRunnables[keyCode] = longPressRunnable
            handler.postDelayed(longPressRunnable, 500L)
        } else {
            longPressRunnables.remove(keyCode)?.let { handler.removeCallbacks(it) }

            if (isLongPress) {
                isLongPress = false
                clickCount = 0
                stopContinuousAction()
                return
            }

            if (keyCode == 132 || keyCode == 133) {
                Log.d(tag, "Single-click triggered for $keyCode (state=$state)")
                performAction(keyCode, state, "SINGLE")
                return
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
        }
    }

    private fun handleKeyEvent(keyCode: Int, isDown: Boolean, fromShizuku: Boolean): Boolean {
        val km = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        val isLocked = km.isKeyguardLocked
        val isInteractive = powerManager.isInteractive
        val state = if (isInteractive && !isLocked) "ON" else "OFF"

        // Check for Volume Panel
        val isVolumeKey = keyCode == 24 || keyCode == 25
        if (isVolumeKey && isInteractive && SettingsManager.isDefaultWhenVolumeVisible(this)) {
            if (isVolumePanelVisibleState) {
                // Determine what action we are dealing with. If any action is mapped to something
                // other than DEFAULT, we should probably still let the custom mapper handle it
                // and simulate the default behavior if needed.
                val singleAction = SettingsManager.getAction(this, keyCode, state, "SINGLE")
                val doubleAction = SettingsManager.getAction(this, keyCode, state, "DOUBLE")
                val multiAction = SettingsManager.getAction(this, keyCode, state, "MULTI")
                val longAction = SettingsManager.getAction(this, keyCode, state, "LONG")
                
                val hasCustomMapping = singleAction != SettingsManager.ACTION_DEFAULT ||
                                       doubleAction != SettingsManager.ACTION_DEFAULT ||
                                       multiAction != SettingsManager.ACTION_DEFAULT ||
                                       longAction != SettingsManager.ACTION_DEFAULT

                if (!hasCustomMapping) {
                    markVolumePanelVisible()
                    return false
                }
            }
        }

        // Hardcoded behavior for Camera and Focus buttons when screen is OFF/Locked
        if (state == "OFF") {
            if (keyCode == 27) {
                // Camera button: Let system handle it (Default behavior, wakes screen)
                return false 
            }
            if (keyCode == 134) {
                // Focus button: Block the key (None)
                return true 
            }
        }

        if (fromShizuku) {
            Log.v(tag, "SHIZUKU KEY: $keyCode ${if (isDown) "DOWN" else "UP"} [Locked=$isLocked, State=$state]")
        } else {
            Log.v(tag, "ACCESSIBILITY KEY: $keyCode ${if (isDown) "DOWN" else "UP"} [Locked=$isLocked, State=$state]")
        }

        // Deduplicate events since both Shizuku and Accessibility can trigger for the same physical press
        val eventKey = Pair(keyCode, isDown)
        val now = System.currentTimeMillis()
        val lastTime = lastEventTimes[eventKey] ?: 0L
        val lastSource = lastEventSources[eventKey]

        var isDuplicate = false
        if (now - lastTime < 300L && lastSource != null && lastSource != fromShizuku) {
            isDuplicate = true
            Log.v(tag, "Duplicate event detected and ignored: $keyCode ${if (isDown) "DOWN" else "UP"} fromShizuku=$fromShizuku")
        } else {
            lastEventTimes[eventKey] = now
            lastEventSources[eventKey] = fromShizuku
        }
        
        // Camera override check
        val currentPackage = rootInActiveWindow?.packageName?.toString() ?: lastPackageName
        val isCameraApp = currentPackage?.contains("camera", ignoreCase = true) == true

        val updateButtonState = {
            if (keyCode == 132 || keyCode == 133) {
                keyClearTasks[keyCode]?.let { handler.removeCallbacks(it) }
                ButtonState.setKeyPressed(keyCode, true)
                val task = Runnable { ButtonState.setKeyPressed(keyCode, false) }
                keyClearTasks[keyCode] = task
                handler.postDelayed(task, 200L)
            } else {
                ButtonState.setKeyPressed(keyCode, isDown)
            }
        }

        if (isCameraInUse && SettingsManager.isDisableInCamera(this) && isCameraApp) {
            updateButtonState()
            return false
        }

        if (isDuplicate) {
            updateButtonState()
            return true // It's blocked by the app so we return true to consume it, but we skip processing
        }

        updateButtonState()

        // --- Hardware button sequence logic for 134 (Focus) and 27 (Shutter) ---
        if (keyCode == 27) {
            if (isDown) {
                isShutterKeyPressed = true
                pendingFocusDown?.let {
                    handler.removeCallbacks(it)
                    pendingFocusDown = null
                }
                // Cancel any pending long press for 134 if we interrupted it
                longPressRunnables.remove(134)?.let { handler.removeCallbacks(it) }
                ignoreNextFocusUp = true
            } else {
                isShutterKeyPressed = false
            }
            processKeyEvent(keyCode, isDown, state)
            return true
        }

        if (keyCode == 134) {
            if (isDown) {
                if (isShutterKeyPressed) {
                    ignoreNextFocusUp = true
                    return true
                }
                
                // If a 27 click is pending, we might be in the middle of a double tap.
                // We should ignore the 134 down so we don't interrupt the 27 sequence.
                if (lastKeyCode == 27 && pendingMultiClick != null) {
                    ignoreNextFocusUp = true
                    return true
                }

                ignoreNextFocusUp = false
                val focusDownTask = Runnable {
                    processKeyEvent(134, true, state)
                    pendingFocusDown = null
                }
                pendingFocusDown = focusDownTask
                handler.postDelayed(focusDownTask, 75L)
            } else {
                if (ignoreNextFocusUp) {
                    ignoreNextFocusUp = false
                    longPressRunnables.remove(134)?.let { handler.removeCallbacks(it) }
                    return true
                }
                pendingFocusDown?.let {
                    // Fast tap of 134, process the down immediately before the up
                    handler.removeCallbacks(it)
                    it.run()
                }
                processKeyEvent(134, false, state)
            }
            return true
        }

        processKeyEvent(keyCode, isDown, state)
        return true
    }

    private fun startContinuousAction(keyCode: Int, state: String, type: String) {
        val action = SettingsManager.getAction(this, keyCode, state, type)
        val isContinuous = (action == SettingsManager.ACTION_DEFAULT && (keyCode == 24 || keyCode == 25)) || 
                           action == SettingsManager.ACTION_VOLUME_UP || 
                           action == SettingsManager.ACTION_VOLUME_DOWN ||
                           action == SettingsManager.ACTION_SCROLL_UP_SMOOTH ||
                           action == SettingsManager.ACTION_SCROLL_DOWN_SMOOTH ||
                           action == "TAP_SCROLL_UP_SMOOTH" ||
                           action == "TAP_SCROLL_DOWN_SMOOTH" ||
                           action == SettingsManager.ACTION_BRIGHTNESS_UP ||
                           action == SettingsManager.ACTION_BRIGHTNESS_DOWN
                           
        val actualIsContinuous = if (SettingsManager.isDefaultWhenVolumeVisible(this) && isVolumePanelVisibleState && (keyCode == 24 || keyCode == 25)) {
            true // Default behavior for volume keys is continuous
        } else {
            isContinuous
        }

        if (!actualIsContinuous) return

        val isScroll = action == SettingsManager.ACTION_SCROLL_UP_SMOOTH || action == SettingsManager.ACTION_SCROLL_DOWN_SMOOTH || action == "TAP_SCROLL_UP_SMOOTH" || action == "TAP_SCROLL_DOWN_SMOOTH"
        val delay = if (isScroll) 250L else 150L

        continuousActionTask = object : Runnable {
            override fun run() {
                // If it's a long press on a volume key with volume panel visible, the action might be overridden to DEFAULT.
                // We should pass the same parameters so it's overridden correctly
                performAction(keyCode, state, type)
                handler.postDelayed(this, delay) // Repeat
            }
        }
        handler.postDelayed(continuousActionTask!!, delay)
    }

    private fun stopContinuousAction() {
        continuousActionTask?.let { handler.removeCallbacks(it) }
        continuousActionTask = null
    }

    private fun performAction(keyCode: Int, state: String, type: String) {
        val action = SettingsManager.getAction(this, keyCode, state, type)
        Log.i(tag, ">>>> EXECUTING: $action [Key: $keyCode, State: $state, Type: $type, Locked: ${powerManager.isInteractive}] <<<<")
        
        // If we are executing an action and it's a volume key, and the user wants default behavior when the volume panel is visible,
        // we might want to bypass custom actions and execute DEFAULT instead if the volume panel is visible.
        val finalAction = if (SettingsManager.isDefaultWhenVolumeVisible(this) && 
                              isVolumePanelVisibleState && 
                              (keyCode == 24 || keyCode == 25)) {
            Log.d(tag, "Volume panel is visible, overriding action $action to DEFAULT")
            SettingsManager.ACTION_DEFAULT
        } else {
            action
        }

        val success = when (finalAction) {
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
            SettingsManager.ACTION_SCROLL_UP, "TAP_SCROLL_UP" -> { performScroll(true); true }
            SettingsManager.ACTION_SCROLL_DOWN, "TAP_SCROLL_DOWN" -> { performScroll(false); true }
            SettingsManager.ACTION_SCROLL_UP_SMOOTH, "TAP_SCROLL_UP_SMOOTH" -> { performScroll(true); true }
            SettingsManager.ACTION_SCROLL_DOWN_SMOOTH, "TAP_SCROLL_DOWN_SMOOTH" -> { performScroll(false); true }
            SettingsManager.ACTION_NONE -> { Log.d(tag, "Action is NONE, key blocked."); true }
            else -> false
        }
        
        if (!success) {
            Log.e(tag, "FAILED to execute action: $finalAction (This often happens on Lock Screens for Home/Recents/Screenshot)")
        }
    }

    private fun simulateDefaultBehavior(keyCode: Int) {
        Log.d(tag, "Simulating default for $keyCode")
        when (keyCode) {
            24 -> {
                audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                markVolumePanelVisible()
            }
            25 -> {
                audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                markVolumePanelVisible()
            }
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
        markVolumePanelVisible()
    }

    private fun markVolumePanelVisible() {
        if (!isVolumePanelVisibleState) Log.d(tag, "Volume Panel Visible (marked via key event)")
        isVolumePanelVisibleState = true
        volumePanelTimeoutRunnable?.let { handler.removeCallbacks(it) }
        volumePanelTimeoutRunnable = Runnable { 
            Log.d(tag, "Volume Panel Invisible (timeout)")
            isVolumePanelVisibleState = false 
        }
        handler.postDelayed(volumePanelTimeoutRunnable!!, 3500L)
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

    private fun performScroll(up: Boolean) {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val bounds = wm.currentWindowMetrics.bounds
        val centerX = bounds.width() / 2f
        val centerY = bounds.height() / 2f

        val path = Path()
        // 112.5f distance ensures 1.5x more scrolling (225px total) than before
        val distance = 112.5f
        if (up) {
            // Scroll up means finger moves down
            path.moveTo(centerX, centerY - distance)
            path.lineTo(centerX, centerY + distance)
        } else {
            // Scroll down means finger moves up
            path.moveTo(centerX, centerY + distance)
            path.lineTo(centerX, centerY - distance)
        }

        // 200ms duration for a slow, smooth swipe that won't trigger a fast fling
        val stroke = GestureDescription.StrokeDescription(path, 0, 200)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
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
