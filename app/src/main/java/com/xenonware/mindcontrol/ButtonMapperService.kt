package com.xenonware.mindcontrol

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.Path
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
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
    private var previousPackageName: String? = null
    private var isVolumePanelVisibleState = false
    private var volumePanelTimeoutRunnable: Runnable? = null
    private var lastKeyCode: Int = -1

    private var isShutterKeyPressed = false
    private var ignoreNextFocusUp = false
    private var pendingFocusDown: Runnable? = null

    private lateinit var audioManager: AudioManager
    private lateinit var cameraManager: CameraManager
    private lateinit var powerManager: PowerManager
    private var wakeLock: PowerManager.WakeLock? = null

    private val keyboardKeyCodes = setOf(
        29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,             // a..l
        41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54,     // m..z
        55, 56, 71,                                                 // , . [
        59, 62, 66, 67                                              // shift, space, enter, backspace
    )

    private fun hasCustomMapping(keyCode: Int, state: String): Boolean {
        val types = listOf("SINGLE", "DOUBLE", "TRIPLE", "LONG")
        return types.any {
            SettingsManager.getAction(this, keyCode, state, it) != SettingsManager.ACTION_DEFAULT
        }
    }

    private fun isTextFieldFocused(): Boolean {
        var focusedNode: android.view.accessibility.AccessibilityNodeInfo? = null
        try {
            val rootNode = rootInActiveWindow ?: return false
            // Find the node that currently has keyboard input focus
            focusedNode = rootNode.findFocus(android.view.accessibility.AccessibilityNodeInfo.FOCUS_INPUT)

            if (focusedNode != null) {
                // Check if the node is marked as editable or is an EditText class
                val isEditable = focusedNode.isEditable
                val isEditText = focusedNode.className?.toString()?.contains("EditText") == true
                return isEditable || isEditText
            }
        } catch (e: Exception) {
            Log.e(tag, "Error checking focused node", e)
        } finally {
            // Always recycle nodes to prevent memory leaks
            focusedNode?.recycle()
        }
        return false
    }

    private val cameraCallback = object : CameraManager.AvailabilityCallback() {
        override fun onCameraAvailable(cameraId: String) { isCameraInUse = false }
        override fun onCameraUnavailable(cameraId: String) { isCameraInUse = true }
    }

    override fun onServiceConnected() {
        Log.d(tag, "Service Connected")

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

        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MindControl:KeyCaptureLock")
        wakeLock?.acquire()

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
            
            if (pkg != null && pkg != packageName && pkg != "com.android.systemui") {
                if (pkg != lastPackageName) {
                    previousPackageName = lastPackageName
                    lastPackageName = pkg
                    Log.d(tag, "App Track: Current=$lastPackageName, Previous=$previousPackageName")
                }
            }

            if (pkg == "com.android.systemui") {
                val isVolumeDialog = className?.contains("Volume", ignoreCase = true) == true ||
                        className?.contains("Dialog", ignoreCase = true) == true
                if (isVolumeDialog) {
                    if (!isVolumePanelVisibleState) Log.d(tag, "Volume Panel Visible (detected via AccessibilityEvent)")
                    isVolumePanelVisibleState = true
                    volumePanelTimeoutRunnable?.let { handler.removeCallbacks(it) }
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

    private var continuousActionTask: Runnable? = null

    private val lastEventTimes = mutableMapOf<Pair<Int, Boolean>, Long>()
    private val lastEventSources = mutableMapOf<Pair<Int, Boolean>, Boolean>()

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.repeatCount > 0) return true
        val keyCode = event.keyCode
        val isDown = event.action == KeyEvent.ACTION_DOWN

        Log.v(tag, "ACCESSIBILITY RAW: keyCode=$keyCode action=${if (isDown) "DOWN" else "UP"}")

        val isSpecialKey = keyCode in setOf(134, 27, 25, 24, 131, 132, 133, 111)
        val isKeyboardKey = keyCode in keyboardKeyCodes
        if (!isSpecialKey && !isKeyboardKey) return false

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

            if (keyCode == 132 || keyCode == 133) return

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

        val currentPackage = rootInActiveWindow?.packageName?.toString() ?: lastPackageName
        val isCameraApp = currentPackage?.contains("camera", ignoreCase = true) == true

        val updateButtonState = {
            if (keyCode == 132 || keyCode == 133) {
                keyClearTasks[keyCode]?.let { handler.removeCallbacks(it) }
                ButtonState.setKeyPressed(keyCode, true)
                val task = Runnable { ButtonState.setKeyPressed(keyCode, false) }
                keyClearTasks[keyCode] = task
                handler.postDelayed(task, 500L)
            } else {
                ButtonState.setKeyPressed(keyCode, isDown)
            }
        }

        // ── Camera app pass-through ────────────────────────────────────────────

        if ((isCameraApp || isCameraInUse) && SettingsManager.isDisableInCamera(this)) {
            updateButtonState()
            return false
        }
        
        // ── Volume panel pass-through ──────────────────────────────────────────
        val isVolumeKey = keyCode == 24 || keyCode == 25
        if (isVolumeKey && isInteractive
            && SettingsManager.isDefaultWhenVolumeVisible(this)
            && isVolumePanelVisibleState
        ) {
            markVolumePanelVisible()
            updateButtonState()
            return false
        }

        // Keyboard-key handling
        if (keyCode in keyboardKeyCodes) {
            ButtonState.setKeyPressed(keyCode, isDown)

            if (isTextFieldFocused()) return false

            val isOurApp = currentPackage == packageName
            if (isOurApp) return true

            if (!hasCustomMapping(keyCode, state)) return false

            processKeyEvent(keyCode, isDown, state)
            return true
        }

        // Hardcoded behavior for Camera and Focus buttons when screen is OFF/Locked
        if (state == "OFF") {
            if (keyCode == 27) return false
            if (keyCode == 134) return true
        }

        if (fromShizuku) {
            Log.v(tag, "SHIZUKU KEY: $keyCode ${if (isDown) "DOWN" else "UP"} [Locked=$isLocked, State=$state]")
        } else {
            Log.v(tag, "ACCESSIBILITY KEY: $keyCode ${if (isDown) "DOWN" else "UP"} [Locked=$isLocked, State=$state]")
        }

        // Deduplicate events between Shizuku and AccessibilityService
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

        if (isDuplicate) {
            updateButtonState()
            return true
        }

        updateButtonState()

        // --- Hardware sequence logic for 134 (Focus) and 27 (Shutter) ---
        if (keyCode == 27) {
            if (isDown) {
                isShutterKeyPressed = true
                pendingFocusDown?.let {
                    handler.removeCallbacks(it)
                    pendingFocusDown = null
                }
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
            true
        } else {
            isContinuous
        }

        if (!actualIsContinuous) return

        val isScroll = action == SettingsManager.ACTION_SCROLL_UP_SMOOTH || action == SettingsManager.ACTION_SCROLL_DOWN_SMOOTH || action == "TAP_SCROLL_UP_SMOOTH" || action == "TAP_SCROLL_DOWN_SMOOTH"
        val delay = if (isScroll) 250L else 150L

        continuousActionTask = object : Runnable {
            override fun run() {
                performAction(keyCode, state, type)
                handler.postDelayed(this, delay)
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
            SettingsManager.ACTION_MUTE_VOL -> {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI)
                true
            }
            SettingsManager.ACTION_MUTE_MIC_TOGGLE -> {
                audioManager.isMicrophoneMute = !audioManager.isMicrophoneMute
                true
            }
            SettingsManager.ACTION_FLASHLIGHT -> { toggleFlashlight(); true }
            SettingsManager.ACTION_SCREENSHOT -> performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            SettingsManager.ACTION_LOCK -> performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            SettingsManager.ACTION_HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
            SettingsManager.ACTION_BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
            SettingsManager.ACTION_RECENTS -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            SettingsManager.ACTION_NOTIFICATIONS -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            SettingsManager.ACTION_QUICK_SETTINGS -> performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
            SettingsManager.ACTION_POWER_DIALOG -> performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
            SettingsManager.ACTION_LAST_APP -> { switchToLastApp(); true }
            SettingsManager.ACTION_APP_INFO -> { openAppInfo(); true }
            SettingsManager.ACTION_SHOW_MENU -> { ShizukuManager.injectKey(KeyEvent.KEYCODE_MENU); true }
            SettingsManager.ACTION_ASSISTANT -> { launchAssistant(); true }
            SettingsManager.ACTION_GOOGLE_SEARCH -> { launchGoogleSearch(); true }
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
            Log.d(tag, "ACTION_VOICE_COMMAND failed, falling back to Assist")
            // Fallback to the standard assist overlay if the voice intent is not handled
            if (!performGlobalAction(16)) { // GLOBAL_ACTION_ASSIST
                val assistIntent = Intent(Intent.ACTION_ASSIST)
                assistIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    startActivity(assistIntent)
                } catch (e2: Exception) {
                    Log.e(tag, "Voice Assistant error", e2)
                }
            }
        }
    }

    private fun launchGoogleSearch() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.setPackage("com.google.android.googlequicksearchbox")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            // Try to launch the package's main activity (the search shortcut)
            val launchIntent = packageManager.getLaunchIntentForPackage("com.google.android.googlequicksearchbox")
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
            } else {
                // Fallback to web search if package not found
                val webIntent = Intent(Intent.ACTION_WEB_SEARCH)
                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(webIntent)
            }
        } catch (e: Exception) {
            Log.e(tag, "Google search error", e)
        }
    }

    private fun switchToLastApp() {
        val targetPkg = previousPackageName ?: return
        Log.d(tag, "Switching to last app: $targetPkg")
        val intent = packageManager.getLaunchIntentForPackage(targetPkg)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(tag, "Error switching to last app", e)
            }
        }
    }

    private fun openAppInfo() {
        val currentPackage = rootInActiveWindow?.packageName?.toString() ?: lastPackageName
        if (currentPackage != null && currentPackage != packageName) {
            Log.d(tag, "Opening app info for: $currentPackage")
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = android.net.Uri.fromParts("package", currentPackage, null)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(tag, "Error opening app info", e)
            }
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
            Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
            val currentRotation = Settings.System.getInt(contentResolver, Settings.System.USER_ROTATION)
            val newRotation = if (currentRotation == 0) 1 else 0
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
        val distance = 112.5f
        if (up) {
            path.moveTo(centerX, centerY - distance)
            path.lineTo(centerX, centerY + distance)
        } else {
            path.moveTo(centerX, centerY + distance)
            path.lineTo(centerX, centerY - distance)
        }

        val stroke = GestureDescription.StrokeDescription(path, 0, 200)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager.unregisterAvailabilityCallback(cameraCallback)
        wakeLock?.let { if (it.isHeld) it.release() }
        Shizuku.removeBinderReceivedListener(binderListener)
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        ShizukuManager.stopMonitoring()
    }
}