package com.xenonware.mindcontrol

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Path
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.media.AudioManager
import android.media.session.MediaSessionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.net.toUri
import rikka.shizuku.Shizuku
import kotlin.math.max

@SuppressLint("AccessibilityPolicy")
class ButtonMapperService : AccessibilityService() {

    private val tag = "ButtonMapper"
    private var clickCount = 0
    private val clickDelay = 350L

    private var isCameraInUse = false
    private val isLongPress = mutableMapOf<Int, Boolean>()
    private var isFlashlightOn = false
    private var lastPackageName: String? = null
    private var previousPackageName: String? = null
    private var isVolumePanelVisibleState = false
    private var volumePanelTimeoutRunnable: Runnable? = null
    private var lastKeyCode: Int = -1
    private var lastKnownRingVolume: Int = -1

    private var isShutterKeyPressed = false
    private var ignoreNextFocusUp = false
    private var pendingFocusDown: Runnable? = null
    private var lastRingerToggleTime = 0L

    private lateinit var audioManager: AudioManager
    private lateinit var cameraManager: CameraManager
    private lateinit var powerManager: PowerManager
    private var wakeLock: PowerManager.WakeLock? = null

    private val keyboardKeyCodes = setOf(
        29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,             // a..l
        41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54,     // m..z
        55, 56,                                                     // , .
        59, 62, 66, 67                                              // shift, space, enter, backspace
    )

    private fun hasCustomMapping(keyCode: Int, state: String): Boolean {
        val types = listOf("SINGLE_PRESS", "DOUBLE_PRESS", "TRIPLE_PRESS", "HOLD", "PRESS_AND_HOLD")
        return types.any {
            SettingsManager.getAction(this, keyCode, state, it) != SettingsManager.ACTION_DEFAULT
        }
    }

    private fun isTextFieldFocused(): Boolean {
        try {
            val rootNode = rootInActiveWindow ?: return false
            // Find the node that currently has keyboard input focus
            val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
            // Check if the node is marked as editable or is an EditText class
            val isEditable = focusedNode.isEditable
            val isEditText = focusedNode.className?.toString()?.contains("EditText") == true
            return isEditable || isEditText
        } catch (e: Exception) {
            Log.e(tag, "Error checking focused node", e)
        }
        return false
    }

    private val cameraCallback = object : CameraManager.AvailabilityCallback() {
        override fun onCameraAvailable(cameraId: String) { isCameraInUse = false }
        override fun onCameraUnavailable(cameraId: String) { isCameraInUse = true }
    }

    override fun onServiceConnected() {
        Log.d(tag, "Service Connected")

        val info = serviceInfo
        info.flags = info.flags or 0x00000100 // FLAG_HANDLE_VOLUME_KEYS
        serviceInfo = info

        createNotificationChannel()
        val notification = Notification.Builder(this, "service_channel")
            .setContentTitle("MindControl Active")
            .setContentText("Monitoring hardware buttons...")
            .setSmallIcon(android.R.drawable.ic_menu_preferences)
            .build()
        startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)

        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        powerManager = getSystemService(POWER_SERVICE) as PowerManager
        cameraManager.registerAvailabilityCallback(cameraCallback, handler)

        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MindControl:KeyCaptureLock")
        wakeLock?.acquire(10*60*1000L /*10 minutes*/)

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
        if (result == PackageManager.PERMISSION_GRANTED) {
            Log.d(tag, "Shizuku Permission Granted")
            tryStartShizuku()
        }
    }

    private fun tryStartShizuku() {
        try {
            if (Shizuku.pingBinder() &&
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
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

    private val continuousActionTask = mutableMapOf<Int, Runnable>()

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
        val isVolumeKey = keyCode == 24 || keyCode == 25
        val isVolumeSkipActive = state == "OFF" && isVolumeKey && SettingsManager.isVolumeLongPressSkipEnabled(this)

        if (isDown) {
            isLongPress[keyCode] = false
            if (keyCode != lastKeyCode) {
                clickCount = 0
                if (lastKeyCode != -1) stopContinuousAction(lastKeyCode)
            }
            lastKeyCode = keyCode

            pendingMultiClick?.let { handler.removeCallbacks(it) }
            stopContinuousAction(keyCode)
            longPressRunnables.remove(keyCode)?.let { handler.removeCallbacks(it) }

            if (keyCode == 132 || keyCode == 133) return

            val capturedClickCount = clickCount
            val longPressRunnable = Runnable {
                isLongPress[keyCode] = true
                val type = if (capturedClickCount == 0) "HOLD" else "PRESS_AND_HOLD"
                Log.d(tag, "Long Press triggered: $type for $keyCode (state=$state)")
                performAction(keyCode, state, type)
                if (!isVolumeSkipActive) {
                    startContinuousAction(keyCode, state, type)
                }
            }
            longPressRunnables[keyCode] = longPressRunnable
            handler.postDelayed(longPressRunnable, 500L)
        } else {
            longPressRunnables.remove(keyCode)?.let { handler.removeCallbacks(it) }

            val wasLongPress = isLongPress.remove(keyCode) ?: false
            if (wasLongPress) {
                clickCount = 0
                stopContinuousAction(keyCode)
                return
            }

            stopContinuousAction(keyCode)

            if (keyCode == 132 || keyCode == 133) {
                Log.d(tag, "Single-click triggered for $keyCode (state=$state)")
                performAction(keyCode, state, "SINGLE_PRESS")
                return
            }

            if (isVolumeSkipActive) {
                performAction(keyCode, state, "SINGLE_PRESS")
                return
            }

            clickCount++

            val multiClickRunnable = Runnable {
                val type = when (clickCount) {
                    1 -> "SINGLE_PRESS"; 2 -> "DOUBLE_PRESS"; 3 -> "TRIPLE_PRESS"; else -> "MULTI"
                }
                Log.d(tag, "Multi-click triggered: $type for $keyCode (state=$state)")
                performAction(keyCode, state, type)
                clickCount = 0
                pendingMultiClick = null
            }
            pendingMultiClick = multiClickRunnable
            handler.postDelayed(multiClickRunnable, clickDelay)
        }
    }

//    // TOAST
//    private fun showKeyToast(keyCode: Int) {
//        val name = when (keyCode) {
//            131 -> "AI Button"
//            133 -> "Camera Up"
//            132 -> "Camera Down"
//            111 -> "Keyboard Button"
//            24 -> "Volume Up"
//            25 -> "Volume Down"
//            27 -> "Camera Button"
//            134 -> "Focus Button"
//            else -> {
//                val rawName = KeyEvent.keyCodeToString(keyCode)
//                if (rawName.startsWith("KEYCODE_")) {
//                    rawName.removePrefix("KEYCODE_")
//                        .lowercase()
//                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
//                } else {
//                    rawName
//                }
//            }
//        }
//        Toast.makeText(this, "$name, keycode: $keyCode", Toast.LENGTH_SHORT).show()
//    }

    private fun handleKeyEvent(keyCode: Int, isDown: Boolean, fromShizuku: Boolean): Boolean {
        val km = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        val isLocked = km.isKeyguardLocked
        val isInteractive = powerManager.isInteractive
        val state = if (isInteractive && !isLocked) "ON" else "OFF"

        val isOverrideEnabled = SettingsManager.isOverrideScreenOffEnabled(this)
        val shizukuAvailable = if (fromShizuku) true else ShizukuManager.isAvailable()
        val isVolumeKey = keyCode == 24 || keyCode == 25

        if (state == "OFF") {
            if (!isOverrideEnabled) return false
            
            if (isVolumeKey) {
                if (SettingsManager.isVolumeLongPressSkipEnabled(this)) {
                    // We need to handle this key to implement Long Press Skip
                    processKeyEvent(keyCode, isDown, state)
                    return true
                }
                // If Volume Skip is OFF, let the system handle volume buttons normally
                return false
            }

            if (!shizukuAvailable) {
                Log.d(tag, "Blocking non-volume key $keyCode in Screen Off because Shizuku is missing")
                return false
            }
        }

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

//        if (isDown) {
//            showKeyToast(keyCode) // TOAST
//        }
        
        // ── Camera app pass-through ────────────────────────────────────────────
        if ((isCameraApp || isCameraInUse) && SettingsManager.isDisableInCamera(this)) {
            updateButtonState()
            return false
        }
        
        // ── Volume panel pass-through ──────────────────────────────────────────
        if (isVolumeKey && SettingsManager.isDefaultWhenVolumeVisible(this) && isVolumePanelVisibleState
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
            if (!isDown) stopContinuousAction(keyCode)
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
                    stopContinuousAction(134)
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
        stopContinuousAction(keyCode)
        
        val action = SettingsManager.getAction(this, keyCode, state, type)
        val isContinuous = (action == SettingsManager.ACTION_DEFAULT && (keyCode == 24 || keyCode == 25)) ||
                action == SettingsManager.ACTION_VOLUME_UP ||
                action == SettingsManager.ACTION_VOLUME_DOWN ||
                action == SettingsManager.ACTION_SCROLL_UP_SMOOTH ||
                action == SettingsManager.ACTION_SCROLL_DOWN_SMOOTH ||
                action == SettingsManager.ACTION_SCROLL_UP_SMOOTH_FAST ||
                action == SettingsManager.ACTION_SCROLL_DOWN_SMOOTH_FAST ||
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

        val isScroll = action == SettingsManager.ACTION_SCROLL_UP_SMOOTH ||
                action == SettingsManager.ACTION_SCROLL_DOWN_SMOOTH ||
                action == SettingsManager.ACTION_SCROLL_UP_SMOOTH_FAST ||
                action == SettingsManager.ACTION_SCROLL_DOWN_SMOOTH_FAST ||
                action == "TAP_SCROLL_UP_SMOOTH" ||
                action == "TAP_SCROLL_DOWN_SMOOTH"
        val delay = if (isScroll) 250L else 150L

        val task = object : Runnable {
            override fun run() {
                if (continuousActionTask[keyCode] != this) return
                
                // Ensure the key is still physically pressed.
                // This is a failsafe against "zombie" repetition if the UP event was missed or ignored.
                if (!ButtonState.pressedKeys.value.contains(keyCode)) {
                    Log.w(tag, "Continuous action for $keyCode self-terminated (key no longer pressed)")
                    stopContinuousAction(keyCode)
                    return
                }

                performAction(keyCode, state, type)
                handler.postDelayed(this, delay)
            }
        }
        continuousActionTask[keyCode] = task
        handler.postDelayed(task, delay)
    }

    private fun stopContinuousAction(keyCode: Int) {
        continuousActionTask.remove(keyCode)?.let { handler.removeCallbacks(it) }
    }

    private fun performAction(keyCode: Int, state: String, type: String) {
        val action = SettingsManager.getAction(this, keyCode, state, type)
        Log.i(tag, ">>>> EXECUTING: $action [Key: $keyCode, State: $state, Type: $type, Locked: ${powerManager.isInteractive}] <<<<")

        var finalAction = if (SettingsManager.isDefaultWhenVolumeVisible(this) &&
            isVolumePanelVisibleState &&
            (keyCode == 24 || keyCode == 25)) {
            Log.d(tag, "Volume panel is visible, overriding action $action to DEFAULT")
            SettingsManager.ACTION_DEFAULT
        } else {
            action
        }

        // --- Forced Screen Off Volume behavior ---
        if (state == "OFF" && (keyCode == 24 || keyCode == 25)) {
            if (SettingsManager.isVolumeLongPressSkipEnabled(this)) {
                if (type == "LONG") {
                    skipMedia(keyCode == 24)
                    return
                } else {
                    finalAction = SettingsManager.ACTION_DEFAULT
                }
            }
        }

        val success = when (finalAction) {
            SettingsManager.ACTION_DEFAULT -> { simulateDefaultBehavior(keyCode); true }
            SettingsManager.ACTION_PLAY_PAUSE -> { dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE); true }
            SettingsManager.ACTION_NEXT -> { skipMedia(true); true }
            SettingsManager.ACTION_PREVIOUS -> { skipMedia(false); true }
            SettingsManager.ACTION_FAST_FORWARD -> { dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD); true }
            SettingsManager.ACTION_REWIND -> { dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_REWIND); true }
            SettingsManager.ACTION_STOP -> { dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_STOP); true }
            SettingsManager.ACTION_STEP_FORWARD -> { stepMedia(true); true }
            SettingsManager.ACTION_STEP_BACKWARD -> { stepMedia(false); true }
            SettingsManager.ACTION_VOLUME_UP -> { adjustVolume(AudioManager.ADJUST_RAISE); true }
            SettingsManager.ACTION_VOLUME_DOWN -> { adjustVolume(AudioManager.ADJUST_LOWER); true }
            SettingsManager.ACTION_MUTE_VOL -> {
                val isMuted = audioManager.isStreamMute(AudioManager.STREAM_MUSIC)
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    if (isMuted) AudioManager.ADJUST_UNMUTE else AudioManager.ADJUST_MUTE,
                    AudioManager.FLAG_SHOW_UI
                )
                true
            }
            SettingsManager.ACTION_VOLUME_DIALOG -> {
                audioManager.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
                true
            }
            SettingsManager.ACTION_MUTE_MIC_TOGGLE -> {
                audioManager.isMicrophoneMute = !audioManager.isMicrophoneMute
                true
            }
            SettingsManager.ACTION_FLASHLIGHT -> { toggleFlashlight(); true }
            SettingsManager.ACTION_SCREENSHOT -> performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            SettingsManager.ACTION_LOCK -> performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            SettingsManager.ACTION_LOCK_AOD -> {
                performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                handler.postDelayed({ startWatchActivity() }, 500)
                true
            }
            SettingsManager.ACTION_PIXEL_WATCH -> { startWatchActivity(); true }
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
            SettingsManager.ACTION_VIBRATE_RINGER -> { toggleVibrateRinger(); true }
            SettingsManager.ACTION_CYCLE_SOUND_MODE -> { cycleSoundMode(); true }
            SettingsManager.ACTION_DND -> { toggleDND(); true }
            SettingsManager.ACTION_COPY -> performClipboardAction(AccessibilityNodeInfo.ACTION_COPY)
            SettingsManager.ACTION_CUT -> performClipboardAction(AccessibilityNodeInfo.ACTION_CUT)
            SettingsManager.ACTION_PASTE -> performClipboardAction(AccessibilityNodeInfo.ACTION_PASTE)
            SettingsManager.ACTION_BRIGHTNESS_UP -> { adjustBrightness(20); true }
            SettingsManager.ACTION_BRIGHTNESS_DOWN -> { adjustBrightness(-20); true }
            SettingsManager.ACTION_AUTO_BRIGHTNESS_TOGGLE -> { toggleAutoBrightness(); true }
            SettingsManager.ACTION_ROTATE_TOGGLE -> { toggleRotation(); true }
            SettingsManager.ACTION_ROTATE_360 -> { toggleRotation360(); true }
            SettingsManager.ACTION_AUTOROTATE_TOGGLE -> { toggleAutoRotate(); true }
            SettingsManager.ACTION_WIFI_TOGGLE -> { toggleWifi(); true }
            SettingsManager.ACTION_BLUETOOTH_TOGGLE -> { toggleBluetooth(); true }
            SettingsManager.ACTION_DATA_TOGGLE -> { toggleData(); true }
            SettingsManager.ACTION_NFC_TOGGLE -> { toggleNfc(); true }
            SettingsManager.ACTION_LOCATION_TOGGLE -> { toggleLocation(); true }
            SettingsManager.ACTION_SCROLL_UP, "TAP_SCROLL_UP" -> { performScroll(true); true }
            SettingsManager.ACTION_SCROLL_DOWN, "TAP_SCROLL_DOWN" -> { performScroll(false); true }
            SettingsManager.ACTION_SCROLL_UP_SMOOTH, "TAP_SCROLL_UP_SMOOTH" -> { performScroll(true); true }
            SettingsManager.ACTION_SCROLL_DOWN_SMOOTH, "TAP_SCROLL_DOWN_SMOOTH" -> { performScroll(false); true }
            SettingsManager.ACTION_SCROLL_UP_SMOOTH_FAST -> { performScroll(true, multiplier = 2f); true }
            SettingsManager.ACTION_SCROLL_DOWN_SMOOTH_FAST -> { performScroll(false, multiplier = 2f); true }
            SettingsManager.ACTION_NONE -> { Log.d(tag, "Action is NONE, key blocked."); true }
            else -> {
                if (finalAction.startsWith(SettingsManager.PREFIX_APP)) {
                    launchApp(finalAction.removePrefix(SettingsManager.PREFIX_APP))
                } else if (finalAction.startsWith(SettingsManager.PREFIX_SHORTCUT)) {
                    launchShortcut(finalAction.removePrefix(SettingsManager.PREFIX_SHORTCUT))
                    true
                } else if (finalAction.startsWith(SettingsManager.PREFIX_SPEED_DIAL)) {
                    launchSpeedDial(finalAction.removePrefix(SettingsManager.PREFIX_SPEED_DIAL))
                    true
                } else if (finalAction.startsWith(SettingsManager.PREFIX_URL)) {
                    launchUrl(finalAction.removePrefix(SettingsManager.PREFIX_URL))
                    true
                } else if (finalAction.startsWith(SettingsManager.PREFIX_QR_CODE)) {
                    showQrCode(finalAction.removePrefix(SettingsManager.PREFIX_QR_CODE))
                    true
                } else {
                    false
                }
            }
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
        } catch (_: Exception) {
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

    private fun performClipboardAction(actionId: Int): Boolean {
        // Try the input-focused node first, then fall back to the accessibility-focused node
        val node = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
            ?: return false

        return node.performAction(actionId)
    }

    private fun launchApp(packageName: String): Boolean {
        Log.d(tag, "Launching app: $packageName")
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                startActivity(intent)
                return true
            } catch (e: Exception) {
                Log.e(tag, "Error launching app $packageName", e)
            }
        }
        return false
    }

    private fun launchShortcut(shortcutData: String) {
        try {
            val parts = shortcutData.split("||")
            val data = parts[0]
            
            if (data.startsWith("intent:")) {
                Log.d(tag, "Launching shortcut via URI")
                val intent = Intent.parseUri(data, 0)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                val pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)

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
                Log.d(tag, "Launching shortcut via package name: $data")
                val intent = packageManager.getLaunchIntentForPackage(data)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Shortcut launch error", e)
        }
    }

    private fun launchSpeedDial(number: String) {
        val intent = Intent(Intent.ACTION_CALL)
        intent.data = "tel:$number".toUri()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(tag, "Speed dial error", e)
            // Fallback to dialer if CALL permission is not granted
            val dialIntent = Intent(Intent.ACTION_DIAL)
            dialIntent.data = "tel:$number".toUri()
            dialIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                startActivity(dialIntent)
            } catch (e2: Exception) {
                Log.e(tag, "Dialer fallback error", e2)
            }
        }
    }

    private fun launchUrl(url: String) {
        var finalUrl = url
        if (!finalUrl.startsWith("http://") && !finalUrl.startsWith("https://")) {
            finalUrl = "https://$finalUrl"
        }
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = finalUrl.toUri()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(tag, "URL launch error", e)
        }
    }

    private fun showQrCode(text: String) {
        val intent = Intent(this, QrCodeActivity::class.java)
        intent.putExtra("EXTRA_QR_TEXT", text)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(tag, "QR show error", e)
        }
    }

    private fun startWatchActivity() {
        val intent = Intent(this, WatchActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(tag, "Watch show error", e)
        }
    }

    private fun toggleVibrateRinger() {
        val now = System.currentTimeMillis()
        if (now - lastRingerToggleTime < 500L) return
        lastRingerToggleTime = now

        val currentMode = audioManager.ringerMode
        val nextMode = if (currentMode == AudioManager.RINGER_MODE_NORMAL) {
            AudioManager.RINGER_MODE_VIBRATE
        } else {
            AudioManager.RINGER_MODE_NORMAL
        }
        
        try {
            audioManager.ringerMode = nextMode
            Log.d(tag, "Vibrate/Ringer Toggle: $currentMode -> $nextMode")
        } catch (e: Exception) {
            Log.e(tag, "Failed to toggle ringer mode", e)
        }
    }

    private fun cycleSoundMode() {
        val now = System.currentTimeMillis()
        if (now - lastRingerToggleTime < 500L) return
        lastRingerToggleTime = now

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val currentMode = audioManager.ringerMode
        
        // Target sequence: Silent (0) -> Vibrate (1) -> Normal (2) -> Silent (0)
        val nextMode = when (currentMode) {
            AudioManager.RINGER_MODE_SILENT -> 1 // To Vibrate
            AudioManager.RINGER_MODE_VIBRATE -> 2 // To Normal
            AudioManager.RINGER_MODE_NORMAL -> 0 // To Silent
            else -> 0
        }
        
        Log.d(tag, "Sound Mode Cycle: Current=$currentMode, NextModeTarget=$nextMode")
        
        try {
            if (!nm.isNotificationPolicyAccessGranted) {
                Log.w(tag, "cycleSoundMode: DND permission not granted")
                return
            }

            when (nextMode) {
                0 -> { // Silent
                    val vol = audioManager.getStreamVolume(AudioManager.STREAM_RING)
                    if (vol > 0) lastKnownRingVolume = vol

                    // FORCE BEHAVIOR: Jump to Normal first
                    if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
                        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    }
                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                }
                1 -> { // Vibrate
                    if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                        val vol = audioManager.getStreamVolume(AudioManager.STREAM_RING)
                        if (vol > 0) lastKnownRingVolume = vol
                    }
                    // FORCE BEHAVIOR: Jump to Normal first
                    if (audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT) {
                        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    }
                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                }
                2 -> { // Normal
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
                    val targetVol = if (lastKnownRingVolume > 0) lastKnownRingVolume
                    else (maxVol / 2).coerceAtLeast(1)
                    audioManager.setStreamVolume(AudioManager.STREAM_RING, targetVol, 0)
                    audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, targetVol, 0)
                }
            }

            // Force the system UI to show the change
            audioManager.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
            
            Handler(Looper.getMainLooper()).postDelayed({
                Log.d(tag, "Sound Mode Cycle Verify: Actual=${audioManager.ringerMode}")
            }, 200)
        } catch (e: Exception) {
            Log.e(tag, "Error cycling ringer mode", e)
        }
    }

    private fun toggleDND() {
        val now = System.currentTimeMillis()
        if (now - lastRingerToggleTime < 500L) return
        lastRingerToggleTime = now

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (nm.isNotificationPolicyAccessGranted) {
            val currentFilter = nm.currentInterruptionFilter
            // EXACT OLD BEHAVIOR: Use FILTER_NONE for ON
            val newFilter = if (currentFilter == NotificationManager.INTERRUPTION_FILTER_ALL) {
                NotificationManager.INTERRUPTION_FILTER_NONE
            } else {
                NotificationManager.INTERRUPTION_FILTER_ALL
            }
            try {
                nm.setInterruptionFilter(newFilter)
                Log.d(tag, "DND Toggle: $currentFilter -> $newFilter")
            } catch (e: Exception) {
                Log.e(tag, "Error toggling DND filter", e)
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
            // Force Auto-Rotate OFF
            Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
            
            val current = try {
                Settings.System.getInt(contentResolver, Settings.System.USER_ROTATION)
            } catch (_: Exception) { 0 }
            
            // Toggle specifically between Portrait (0) and Landscape (1)
            val next = if (current == 0) 1 else 0
            
            Settings.System.putInt(contentResolver, Settings.System.USER_ROTATION, next)
            Log.d(tag, "Rotation Toggle: $current -> $next")
        } catch (e: Exception) {
            Log.e(tag, "Rotation toggle error. Make sure 'Allow Sys Settings' is granted.", e)
        }
    }

    private fun toggleRotation360() {
        try {
            // Force Auto-Rotate OFF
            Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
            
            val current = try {
                Settings.System.getInt(contentResolver, Settings.System.USER_ROTATION)
            } catch (_: Exception) { 0 }
            
            // Cycle through all 4 orientations (0, 1, 2, 3)
            val next = (current + 1) % 4
            
            Settings.System.putInt(contentResolver, Settings.System.USER_ROTATION, next)
            Log.d(tag, "Rotation 360 Toggle: $current -> $next")
        } catch (e: Exception) {
            Log.e(tag, "Rotation 360 toggle error", e)
        }
    }

    private fun toggleAutoRotate() {
        try {
            val current = try {
                Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION)
            } catch (_: Exception) { 0 }
            
            val next = if (current == 1) 0 else 1
            Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, next)
            Log.d(tag, "Auto-Rotate Toggle: $current -> $next")
        } catch (e: Exception) {
            Log.e(tag, "Auto-Rotate toggle error", e)
        }
    }

    private fun toggleAutoBrightness() {
        try {
            val mode = try {
                Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE)
            } catch (_: Exception) { Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL }

            val next = if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            } else {
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            }

            if (ShizukuManager.isAvailable()) {
                ShizukuManager.runShellCommand("settings put system screen_brightness_mode $next")
                Log.d(tag, "Auto-Brightness Toggle (Shizuku): $mode -> $next")
            } else {
                val ok = Settings.System.putInt(
                    contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    next
                )
                Log.d(tag, "Auto-Brightness Toggle (System.putInt=$ok): $mode -> $next")
                if (!ok) {
                    Log.e(tag, "Auto-Brightness toggle was rejected. Authorize Shizuku for reliable toggling.")
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Auto-Brightness toggle error. Authorize Shizuku or grant 'Allow Sys Settings'.", e)
        }
    }

    private fun toggleWifi() {
        try {
            val current = Settings.Global.getInt(contentResolver, Settings.Global.WIFI_ON, 0)
            val next = if (current == 1) "disable" else "enable"
            if (ShizukuManager.isAvailable()) {
                ShizukuManager.runShellCommand("svc wifi $next")
                Log.d(tag, "Wifi Toggle (Shizuku): $current -> $next")
            } else {
                Log.e(tag, "Wifi toggle requires Shizuku or Root on this Android version")
            }
        } catch (e: Exception) {
            Log.e(tag, "Wifi toggle error", e)
        }
    }

    private fun toggleBluetooth() {
        try {
            val bm = getSystemService(BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
            val adapter = bm.adapter
            val isEnabled = adapter?.isEnabled == true
            val next = !isEnabled
            val cmd = if (next) "enable" else "disable"

            if (ShizukuManager.isAvailable()) {
                ShizukuManager.runShellCommand("svc bluetooth $cmd")
                Log.d(tag, "Bluetooth Toggle (Shizuku): $isEnabled -> $next")
            } else {
                Log.e(tag, "Bluetooth toggle requires Shizuku or Root")
            }
        } catch (e: Exception) {
            Log.e(tag, "Bluetooth toggle error", e)
        }
    }

    private fun toggleData() {
        try {
            val current = Settings.Global.getInt(contentResolver, "mobile_data", 0)
            val next = if (current == 1) "disable" else "enable"
            if (ShizukuManager.isAvailable()) {
                ShizukuManager.runShellCommand("svc data $next")
                Log.d(tag, "Data Toggle (Shizuku): $current -> $next")
            } else {
                Log.e(tag, "Mobile Data toggle requires Shizuku or Root")
            }
        } catch (e: Exception) {
            Log.e(tag, "Data toggle error", e)
        }
    }

    private fun toggleNfc() {
        try {
            val current = Settings.Global.getInt(contentResolver, "nfc_on", 0)
            val next = if (current == 1) "disable" else "enable"
            if (ShizukuManager.isAvailable()) {
                ShizukuManager.runShellCommand("svc nfc $next")
                Log.d(tag, "NFC Toggle (Shizuku): $current -> $next")
            } else {
                Log.e(tag, "NFC toggle requires Shizuku or Root")
            }
        } catch (e: Exception) {
            Log.e(tag, "NFC toggle error", e)
        }
    }

    private fun toggleLocation() {
        try {
            val lm = getSystemService(LOCATION_SERVICE) as LocationManager
            val isEnabled = lm.isLocationEnabled
            val nextMode = if (isEnabled) 0 else 3 // 0 = OFF, 3 = HIGH_ACCURACY
            
            if (ShizukuManager.isAvailable()) {
                ShizukuManager.runShellCommand("settings put secure location_mode $nextMode")
                Log.d(tag, "Location Toggle (Shizuku): $isEnabled -> $nextMode")
            } else {
                Log.e(tag, "Location toggle requires Shizuku or Root")
            }
        } catch (e: Exception) {
            Log.e(tag, "Location toggle error", e)
        }
    }

    private fun stepMedia(forward: Boolean) {
        try {
            val msm = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
            val sessions = msm.getActiveSessions(ComponentName(this, NotificationListener::class.java))
            val controller = sessions.firstOrNull()
            
            if (controller?.playbackState != null) {
                val delta = if (forward) 30000L else -30000L
                val newPos = max(0, controller.playbackState!!.position + delta)
                controller.transportControls.seekTo(newPos)
                Log.d(tag, "Stepped media ${if (forward) "forward" else "backward"} to $newPos")
            } else {
                dispatchMediaKey(if (forward) KeyEvent.KEYCODE_MEDIA_STEP_FORWARD else KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD)
            }
        } catch (e: Exception) {
            Log.e(tag, "Media step error", e)
            dispatchMediaKey(if (forward) KeyEvent.KEYCODE_MEDIA_STEP_FORWARD else KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD)
        }
    }

    private fun skipMedia(forward: Boolean) {
        val msm = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        try {
            val sessions = msm.getActiveSessions(ComponentName(this, NotificationListener::class.java))
            Log.d(tag, "Media Skip: Found ${sessions.size} active sessions")
            val controller = sessions.firstOrNull()
            
            if (controller != null) {
                Log.d(tag, "Media Skip: Controlling session ${controller.packageName}")
                if (forward) controller.transportControls.skipToNext()
                else controller.transportControls.skipToPrevious()
            } else {
                Log.d(tag, "Media Skip: No active media session, using fallback key events")
                dispatchMediaKey(if (forward) KeyEvent.KEYCODE_MEDIA_NEXT else KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            }
        } catch (_: SecurityException) {
            Log.e(tag, "Media Skip: Notification Access NOT granted!")
            dispatchMediaKey(if (forward) KeyEvent.KEYCODE_MEDIA_NEXT else KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        } catch (e: Exception) {
            Log.e(tag, "Media Skip error", e)
            dispatchMediaKey(if (forward) KeyEvent.KEYCODE_MEDIA_NEXT else KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        }
    }

    private fun performScroll(up: Boolean, multiplier: Float = 1f) {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val centerX = width / 2f
        val centerY = height / 2f

        val path = Path()
        // Standard Smooth Scroll (Pixel-based as requested)
        val distance = 112.5f * multiplier
        if (up) {
            path.moveTo(centerX, (centerY - distance).coerceAtLeast(0f))
            path.lineTo(centerX, (centerY + distance).coerceAtMost(height.toFloat()))
        } else {
            path.moveTo(centerX, (centerY + distance).coerceAtMost(height.toFloat()))
            path.lineTo(centerX, (centerY - distance).coerceAtLeast(0f))
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 200))
            .build()
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