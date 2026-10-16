package com.xenonware.mindcontrol

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Path
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.media.AudioManager
import android.media.session.MediaSessionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import androidx.core.net.toUri
import rikka.shizuku.Shizuku
import kotlin.math.max
import kotlin.math.min

@Suppress("DEPRECATION")
@SuppressLint("AccessibilityPolicy")
class ButtonMapperService : AccessibilityService() {

    companion object {
        /** Easily enable or disable key toasts from within the code. Set too false to disable toasts. */
        private const val SHOW_KEY_TOASTS = false

        /** Vibration when holding Shift triggers Shift Lock (Better Shift). */
        private const val SHIFT_LOCK_VIBRATION_MS = 80L

        /**
         * The running service instance, so the settings screen can start or stop the controller
         * without a broadcast or bound service. Null when the service is not connected.
         */
        @Volatile
        private var instance: ButtonMapperService? = null

        /** True when the accessibility service is connected and can act on requests. */
        fun isConnected(): Boolean = instance != null

        /**
         * Starts advertising a controller, regardless of which app is in front.
         *
         * Returns false when the service is not running, so the caller can tell the user to enable
         * accessibility first rather than leaving them with a button that does nothing.
         */
        fun startControllerManually(): Boolean {
            val service = instance ?: return false
            service.manualGamepadRequested = true
            service.handler.post { service.syncGamepadForForegroundApp() }
            return true
        }

        /** Stops a manually started controller and returns to automatic per-app behaviour. */
        fun stopControllerManually(): Boolean {
            val service = instance ?: return false
            service.manualGamepadRequested = false
            service.handler.post { service.syncGamepadForForegroundApp() }
            return true
        }

        /** True while a controller is being advertised, however it was started. */
        fun isControllerRunning(): Boolean = ShellManager.isGamepadRunning()
    }

    private val tag = "ButtonMapper"
    private var clickCount = 0
    private val clickDelay = 350L

    private var isCameraInUse = false
    private val isLongPress = mutableMapOf<Int, Boolean>()
    private var isFlashlightOn = false
    private var lastPackageName: String? = null
    private var previousPackageName: String? = null
    private val launcherPackages = mutableSetOf<String>()
    private var isVolumePanelVisibleState = false
    private var volumePanelTimeoutRunnable: Runnable? = null
    private var lastKeyCode: Int = -1
    private var lastKnownRingVolume: Int = -1
    private var isTextFocusActive = false

    private var isShutterKeyPressed = false
    private var ignoreNextFocusUp = false
    private var pendingFocusDown: Runnable? = null
    private var lastRingerToggleTime = 0L

    // ── Keyboard modifier state ────────────────────────────────────────────
    /** One-shot shift: the next typed character / key input is shifted, then it turns off. */
    private var isShiftHoldActive = false
    /** Caps-lock style: letters stay uppercase until toggled off. */
    private var isShiftLockActive = false
    /** Physical keys we swallowed and re-sent shifted (so their UP / repeats are swallowed too). */
    private val shiftConsumedKeys = mutableSetOf<Int>()
    /** Keys we injected through the shell; they come back through onKeyEvent and must not be remapped. */
    private val recentlyInjected = mutableMapOf<Int, Long>()
    private val injectedEchoWindow = 2000L

    // Managed (locked) Shift key: tap = Shift Hold, hold = Shift Lock, held + key = real Shift chord
    /** Which Shift key is physically held right now (null = none). */
    private var shiftDownKeyCode: Int? = null
    private val physicalShiftDown: Boolean get() = shiftDownKeyCode != null
    private var shiftChordUsed = false
    private var shiftLongPressFired = false
    private var shiftLongPressRunnable: Runnable? = null

    // TOAST: single reusable toast + de-dup between Shizuku and Accessibility sources
    private var keyToast: Toast? = null
    private var lastToastKeyCode: Int = -1
    private var lastToastTime = 0L

    // Phantom SPACE suppression after CTRL_LEFT (113)
    private var ctrlLeftDownTime = 0L
    private val swallowSpaceUp = mutableMapOf(false to false, true to false) // keyed by fromShizuku
    private val phantomSpaceWindow = 150L // ms

    private lateinit var audioManager: AudioManager
    private lateinit var cameraManager: CameraManager
    private lateinit var powerManager: PowerManager
    private var wakeLock: PowerManager.WakeLock? = null

    private val keyboardKeyCodes = SettingsManager.keyboardKeyCodes

    /**
     * The only actions that repeat while a button is held. Everything else fires once per hold:
     * toggles would flip on/off, Paste would spam, emoji would open and close again, etc.
     */
    private val repeatableActions = setOf(
        SettingsManager.ACTION_VOLUME_UP,
        SettingsManager.ACTION_VOLUME_DOWN,
        SettingsManager.ACTION_BRIGHTNESS_UP,
        SettingsManager.ACTION_BRIGHTNESS_DOWN,
        SettingsManager.ACTION_SCROLL_UP,
        SettingsManager.ACTION_SCROLL_DOWN,
        "TAP_SCROLL_UP",
        "TAP_SCROLL_DOWN",
        SettingsManager.ACTION_SCROLL_UP_SMOOTH,
        SettingsManager.ACTION_SCROLL_DOWN_SMOOTH,
        SettingsManager.ACTION_SCROLL_UP_SMOOTH_FAST,
        SettingsManager.ACTION_SCROLL_DOWN_SMOOTH_FAST,
        "TAP_SCROLL_UP_SMOOTH",
        "TAP_SCROLL_DOWN_SMOOTH",
        SettingsManager.ACTION_ARROW_UP,
        SettingsManager.ACTION_ARROW_DOWN,
        SettingsManager.ACTION_ARROW_LEFT,
        SettingsManager.ACTION_ARROW_RIGHT,
        SettingsManager.ACTION_FAST_FORWARD,
        SettingsManager.ACTION_REWIND,
        SettingsManager.ACTION_STEP_FORWARD,
        SettingsManager.ACTION_STEP_BACKWARD,
    )

    private fun isRepeatableAction(keyCode: Int, action: String): Boolean = when {
        // DEFAULT repeats only for volume keys; for keyboard keys it is re-sent once
        action == SettingsManager.ACTION_DEFAULT -> keyCode == 24 || keyCode == 25
        action in repeatableActions -> true
        // Plain keys (letters, arrows, Backspace …) auto-repeat; shortcuts like Ctrl+V fire once
        action.startsWith(SettingsManager.PREFIX_KEY_INPUT) ->
            KeyInput.decode(action)?.let { !it.ctrl && !it.alt } ?: false
        else -> false
    }

    private val smoothScrollActions = setOf(
        SettingsManager.ACTION_SCROLL_UP_SMOOTH,
        SettingsManager.ACTION_SCROLL_DOWN_SMOOTH,
        SettingsManager.ACTION_SCROLL_UP_SMOOTH_FAST,
        SettingsManager.ACTION_SCROLL_DOWN_SMOOTH_FAST,
        "TAP_SCROLL_UP_SMOOTH",
        "TAP_SCROLL_DOWN_SMOOTH",
    )

    private val allPressTypes = listOf(
        "SINGLE_PRESS", "DOUBLE_PRESS", "TRIPLE_PRESS", "QUADRUPLE_PRESS", "QUINTUPLE_PRESS",
        "HOLD", "PRESS_AND_HOLD", "DOUBLE_TAP_AND_HOLD"
    )

    private fun hasCustomMapping(keyCode: Int, state: String): Boolean {
        // Zero-latency: multi-press types can never fire, so they must not make the key "mapped"
        val types = if (SettingsManager.isZeroLatencyEnabled(this, keyCode)) {
            SettingsManager.ZERO_LATENCY_PRESS_TYPES
        } else {
            allPressTypes
        }
        return types.any {
            SettingsManager.getAction(this, keyCode, state, it) != SettingsManager.ACTION_DEFAULT
        }
    }

    private fun currentState(): String {
        val km = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        return if (powerManager.isInteractive && !km.isKeyguardLocked) "ON" else "OFF"
    }

    private fun isEditableClass(className: CharSequence?): Boolean {
        val name = className?.toString() ?: return false
        return name.contains("EditText", ignoreCase = true) ||
            name.contains("AutoComplete", ignoreCase = true) ||
            name.contains("SearchView", ignoreCase = true) ||
            name.contains("WebView", ignoreCase = true) ||
            name.contains("TextField", ignoreCase = true) ||
            name.contains("TextInput", ignoreCase = true)
    }

    private fun isTextFieldFocused(): Boolean {

        val fromFocusTree = try {
            val rootNode = rootInActiveWindow
            val focusedNode = rootNode?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            if (focusedNode != null) {
                val editable = focusedNode.isEditable ||
                    (focusedNode.isFocused && focusedNode.isFocusable && isEditableClass(focusedNode.className))
                if (editable) {
                    isTextFocusActive = true
                    true
                } else {
                    false
                }
            } else {
                null // inconclusive - findFocus could not resolve a node
            }
        } catch (e: Exception) {
            Log.e(tag, "Error checking focused node", e)
            null
        }

        if (fromFocusTree == false) {
            isTextFocusActive = false
            return false
        }

        return fromFocusTree == true || isTextFocusActive
    }

    /** Fresh (un-throttled) lookup of the focused editable field, or null. */
    private fun focusedEditableNode(): AccessibilityNodeInfo? = try {
        rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.takeIf { it.isEditable }
    } catch (_: Exception) {
        null
    }

    private val cameraCallback = object : CameraManager.AvailabilityCallback() {
        override fun onCameraAvailable(cameraId: String) { isCameraInUse = false }
        override fun onCameraUnavailable(cameraId: String) { isCameraInUse = true }
    }

    @SuppressLint("WakelockTimeout")
    override fun onServiceConnected() {
        Log.d(tag, "Service Connected")
        instance = this

        // Publish the stored profile so the UI starts in step with the service.
        ButtonState.setGamepadProfile(SettingsManager.getActiveGamepadProfile(this))

        updateLauncherPackages()

        val info = serviceInfo
        // FLAG_HANDLE_VOLUME_KEYS + FLAG_INPUT_METHOD_EDITOR (type into the focused field directly,
        // no Shizuku needed). Also declare flagInputMethodEditor in the accessibility service XML.
        info.flags = info.flags or 0x00000100 or AccessibilityServiceInfo.FLAG_INPUT_METHOD_EDITOR
        serviceInfo = info

        createNotificationChannel()
        startForeground(
            1,
            buildServiceNotification(getString(R.string.notification_text)),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )

        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        powerManager = getSystemService(POWER_SERVICE) as PowerManager
        cameraManager.registerAvailabilityCallback(cameraCallback, handler)

        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MindControl:KeyCaptureLock")
        wakeLock?.acquire()

        Shizuku.addBinderReceivedListenerSticky(binderListener)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        tryStartMonitoring()

        // Gamepad mode is also re-checked on a timer rather than only on window changes. Sync used
        // to run solely when the foreground package changed, so it never happened when the service
        // started while a chosen app was already open -- after a reboot, for example -- and the
        // controller was then never advertised until the user switched apps twice.
        startGamepadWatchdog()
    }

    /**
     * Keeps the controller device in step with the foreground app, independently of window events.
     *
     * Cheap by design: it compares the current package against the last one it acted on, so the
     * work happens only when the state actually differs.
     */
    private val gamepadWatchdog = object : Runnable {
        override fun run() {
            try {
                syncGamepadForForegroundApp()
            } catch (e: Exception) {
                Log.w(tag, "Gamepad watchdog failed", e)
            }
            handler.postDelayed(this, GAMEPAD_WATCHDOG_INTERVAL_MS)
        }
    }

    private fun startGamepadWatchdog() {
        handler.removeCallbacks(gamepadWatchdog)
        handler.post(gamepadWatchdog)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel("service_channel", getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun buildServiceNotification(text: String): Notification =
        Notification.Builder(this, "service_channel")
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_preferences)
            .setOnlyAlertOnce(true)
            .build()

    // ---------------------------------------------------------------------
    // Gamepad notifications
    // ---------------------------------------------------------------------
    //
    // Uses its own channel and id so it never disturbs the foreground-service notification, and
    // is silent: the user already knows they turned gamepad mode on.

    private val gamepadChannelId = "gamepad_channel"
    private val gamepadNotificationId = 2

    private fun ensureGamepadChannel() {
        val channel = NotificationChannel(
            gamepadChannelId,
            getString(R.string.gamepad_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }

    /** Tells the user a controller is being reported while a chosen app is in front. */
    private fun showGamepadNotification(packageName: String?) {
        ensureGamepadChannel()
        val label = runCatching {
            val info = packageManager.getApplicationInfo(packageName ?: "", 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrNull() ?: packageName.orEmpty()

        val notification = Notification.Builder(this, gamepadChannelId)
            .setContentTitle(getString(R.string.gamepad_notification_title))
            .setContentText(getString(R.string.gamepad_notification_text, label))
            .setSmallIcon(android.R.drawable.ic_menu_preferences)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()

        runCatching {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .notify(gamepadNotificationId, notification)
        }.onFailure { Log.w(tag, "Could not show gamepad notification", it) }
    }

    private fun hideGamepadNotification() {
        runCatching {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .cancel(gamepadNotificationId)
        }
    }

    /** Shows the current Shift state in the persistent notification so it's never "invisible". */
    private fun updateModifierIndicator() {
        val text = when {
            isShiftLockActive -> "⇪ Shift Lock on"
            isShiftHoldActive -> "⇧ Shift – next key"
            else -> getString(R.string.notification_text)
        }
        try {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(1, buildServiceNotification(text))
        } catch (e: Exception) {
            Log.w(tag, "Could not update modifier notification", e)
        }
    }

    private val binderListener = Shizuku.OnBinderReceivedListener {
        Log.d(tag, "Shizuku Binder Received")
        tryStartMonitoring()
    }

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, result ->
        if (result == PackageManager.PERMISSION_GRANTED) {
            Log.d(tag, "Shizuku Permission Granted")
            tryStartMonitoring()
        }
    }

    private fun tryStartMonitoring() {
        try {
            if (ShellManager.isAvailable()) {
                ShellManager.startMonitoring { keyCode, isDown ->
                    handler.post { handleKeyEvent(keyCode, isDown, fromShizuku = true) }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Monitoring start error", e)
        }
    }

    @SuppressLint("SwitchIntDef")
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                isTextFocusActive = false // Reset focus on window change to force re-check
                dpadFocusCursor = null // the focused node belongs to the old window
                val pkg = event.packageName?.toString()
                val className = event.className?.toString()

                if (pkg != null && pkg != packageName && pkg != "com.android.systemui" && !launcherPackages.contains(pkg)) {
                    if (pkg != lastPackageName) {
                        previousPackageName = lastPackageName
                        lastPackageName = pkg
                        Log.d(tag, "App Track: Current=$lastPackageName, Previous=$previousPackageName")
                        // Enter or leave gamepad mode for the app that just came to the front.
                        syncGamepadForForegroundApp()
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
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                val source = event.source
                if (source != null) {
                    // Broadened to cover Compose/WebView/search fields, not just *EditText*.
                    isTextFocusActive = source.isEditable || isEditableClass(source.className)
                    source.recycle()
                }
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                // Strong signal that a text field is the one receiving input right now.
                val source = event.source
                if (source != null) {
                    if (source.isEditable || isEditableClass(source.className)) {
                        isTextFocusActive = true
                    }
                    source.recycle()
                }
            }
        }
    }

    override fun onInterrupt() {}

    private val handler = Handler(Looper.getMainLooper())
    private val longPressRunnables = mutableMapOf<Int, Runnable>()
    private val downClickCounts = mutableMapOf<Int, Int>()
    private val keyClearTasks = mutableMapOf<Int, Runnable>()
    private var pendingMultiClick: Runnable? = null

    private val continuousActionTask = mutableMapOf<Int, Runnable>()


    override fun onKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val isDown = event.action == KeyEvent.ACTION_DOWN

        // Our own shell-injected keys echo back here: pass them through untouched (prevents loops).
        if (isOwnInjectedKey(event)) return false

        // Original behavior: auto-repeats are always swallowed (hold logic is handled by our own timers).
        if (event.repeatCount > 0) return true

        Log.v(tag, "ACCESSIBILITY RAW: keyCode=$keyCode action=${if (isDown) "DOWN" else "UP"} source=${event.source}")

        // Block the SPACE that some hardware fires right after CTRL_LEFT
        if (shouldSwallowPhantomSpace(keyCode, isDown, fromShizuku = false)) return true

        // TOAST: show for EVERY key the service receives, before any filtering
        if (isDown) showKeyToast(keyCode)

        // Gamepad mode runs before the Shift machinery below. Shift consumes keys to re-send them
        // shifted, so a key bound to a controller action would be swallowed and never reach the
        // gamepad branch -- which is why binding Shift itself had no effect in gamepad mode.
        if (gamepadActive &&
            SettingsManager.getGamepadBinding(this, activeGamepadProfile(), keyCode) != null
        ) {
            return handleKeyEvent(keyCode, isDown, fromShizuku = false)
        }

        // Shift key itself (managed by default, reacts instantly – no multi-press wait)
        if (handleManagedShiftKey(event)) return true

        // Shift Hold / Shift Lock / held Shift applied to physical keyboard typing
        if (handleShiftedTyping(event)) return true

        // Emoji soft keyboard physical shortcut: Ctrl + . or Alt + . or Cmd/Meta + .
        // (only real keyboards – our own injected events must never re-trigger it)
        if (keyCode == KeyEvent.KEYCODE_PERIOD && event.deviceId != KeyCharacterMap.VIRTUAL_KEYBOARD &&
            (event.isCtrlPressed || event.isAltPressed || event.isMetaPressed)
        ) {
            if (isDown) openEmojiKeyboard()
            return true
        }

        val isSpecialKey = keyCode in setOf(134, 27, 25, 24, 131, 132, 133, 111)
        val isKeyboardKey = keyCode in keyboardKeyCodes || event.isFromSource(android.view.InputDevice.SOURCE_KEYBOARD)
        return !(!isSpecialKey && !isKeyboardKey) && handleKeyEvent(keyCode, isDown, fromShizuku = false)
    }

    // Per-key press state shared by both event sources (Shizuku + Accessibility)
    private val logicalDownTime = mutableMapOf<Int, Long>()
    private val lastUnpairedUpTime = mutableMapOf<Int, Long>()

    /**
     * true if this event is the second copy of a press we already counted.
     * A DOWN while the key is already down, or a UP while it is already up, is a copy.
     * Only affects counting – the caller still returns the same consume/pass-through value as before.
     */
    private class CountedEvent(val time: Long, val fromShizuku: Boolean, var copySeen: Boolean = false)
    private val lastCountedEvent = mutableMapOf<Pair<Int, Boolean>, CountedEvent>()

    private fun isDuplicatePressEvent(keyCode: Int, isDown: Boolean, fromShizuku: Boolean): Boolean {
        val now = SystemClock.uptimeMillis()
        // The other source's copy of an event we already counted (also catches a lagging copy that
        // only arrives after the whole press is done). Each counted event absorbs in most ONE copy,
        // so a real quick second tap is never swallowed.
        val last = lastCountedEvent[keyCode to isDown]
        if (last != null && !last.copySeen && last.fromShizuku != fromShizuku && now - last.time < 300L) {
            last.copySeen = true
            return true
        }
        val dup = isDuplicateByState(keyCode, isDown, now)
        if (!dup) lastCountedEvent[keyCode to isDown] = CountedEvent(now, fromShizuku)
        return dup
    }

    private fun isDuplicateByState(keyCode: Int, isDown: Boolean, now: Long): Boolean {
        if (isDown) {
            val since = logicalDownTime[keyCode]
            // 3 s safety net in case a UP was ever lost, so a key can never get stuck
            if (since != null && now - since < 3000L) return true
            logicalDownTime[keyCode] = now
            return false
        }
        if (logicalDownTime.remove(keyCode) != null) return false
        // UP without a DOWN: camera up/down (132/133) may only report UPs – accept those once per 300 ms
        if (keyCode == 132 || keyCode == 133) {
            val last = lastUnpairedUpTime[keyCode] ?: 0L
            if (now - last < 300L) return true
            lastUnpairedUpTime[keyCode] = now
            return false
        }
        return true
    }

    private fun isOwnInjectedKey(event: KeyEvent): Boolean {
        // Shell-injected keys come from the virtual keyboard device. A real key (e.g. the physical
        // Shift) is NEVER treated as ours – before, a real Shift pressed within 2 s of an injected
        // Shift+key was passed through, which made Better Shift randomly stop working.
        if (event.deviceId != KeyCharacterMap.VIRTUAL_KEYBOARD) return false
        val keyCode = event.keyCode
        val isDown = event.action == KeyEvent.ACTION_DOWN
        val time = recentlyInjected[keyCode] ?: return false
        if (SystemClock.uptimeMillis() - time > injectedEchoWindow) {
            recentlyInjected.remove(keyCode)
            return false
        }
        if (!isDown) recentlyInjected.remove(keyCode)
        return true
    }

    private fun markInjected(keyCodes: Collection<Int>) {
        val now = SystemClock.uptimeMillis()
        keyCodes.forEach { recentlyInjected[it] = now }
    }

    /**
     * Better Shift (on by default), applied everywhere incl. text fields:
     *  - Single press → its action (default: Shift), fires the instant the key is released
     *  - Hold 500 ms  → short vibration + its action (default: Shift Lock)
     *  - Shift held + another key → normal Shift chord (Shift+A, Shift+arrows …), nothing else fires
     * All other press types are blocked. With Better Shift off, Shift is mapped like any other button.
     */
    private fun handleManagedShiftKey(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        if (!SettingsManager.isBetterShiftKey(this, keyCode)) return false
        if (!powerManager.isInteractive) return false
        val state = currentState()

        val isDown = event.action == KeyEvent.ACTION_DOWN
        ButtonState.setKeyPressed(keyCode, isDown)

        if (isDown) {
            val held = shiftDownKeyCode
            if (held != null && held != keyCode) return true // the other Shift key is already held
            //  == keyCode means its UP got lost – start fresh instead of staying stuck
            shiftDownKeyCode = keyCode
            shiftChordUsed = false
            shiftLongPressFired = false
            shiftLongPressRunnable?.let { handler.removeCallbacks(it) }
            val runnable = Runnable {
                if (physicalShiftDown && !shiftChordUsed) {
                    shiftLongPressFired = true
                    vibrate(VibrationEffect.EFFECT_HEAVY_CLICK, SHIFT_LOCK_VIBRATION_MS)
                    performAction(keyCode, state, "HOLD")
                }
            }
            shiftLongPressRunnable = runnable
            handler.postDelayed(runnable, 500L)
        } else {
            if (shiftDownKeyCode != keyCode) return true // UP of the ignored second Shift key
            shiftDownKeyCode = null
            shiftLongPressRunnable?.let { handler.removeCallbacks(it) }
            shiftLongPressRunnable = null
            if (!shiftLongPressFired && !shiftChordUsed) {
                performAction(keyCode, state, "SINGLE_PRESS")
            }
        }
        return true
    }

    /**
     * Re-sends physical keyboard keys shifted when Shift Hold / Shift Lock is active, or when the
     * managed Shift key is physically held (we consumed its DOWN, so the system doesn't know it's held).
     */
    private fun handleShiftedTyping(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        if (event.action == KeyEvent.ACTION_UP) {
            if (shiftConsumedKeys.remove(keyCode)) {
                ButtonState.setKeyPressed(keyCode, false)
                return true
            }
            return false
        }
        if (event.action != KeyEvent.ACTION_DOWN) return false

        val chord = physicalShiftDown
        if (!chord && !isShiftHoldActive && !isShiftLockActive) return false
        if (KeyEvent.isModifierKey(keyCode) || event.isMetaPressed) return false
        // Real modifiers held without our managed Shift: user shortcut or already shifting – leave it alone.
        if (!chord && (event.isShiftPressed || event.isCtrlPressed || event.isAltPressed)) return false

        val applies = when {
            chord -> true // Shift held: every key gets Shift (letters, arrows for selection, Tab …)
            isShiftHoldActive -> KeyInput.isShiftable(keyCode)
            else -> KeyInput.isLetter(keyCode)
        }
        if (!applies) return false

        if (chord) {
            // Need somewhere to deliver it: a text field, or the shell for everything else
            if (focusedEditableNode() == null && !ShellManager.isAvailable()) return false
            shiftChordUsed = true
        } else {
            if (rootInActiveWindow?.packageName == packageName) return false // don't interfere with our own remap UI
            if (focusedEditableNode() == null) return false
            // A user mapping only wins while typing if it is set to also work while the keyboard is
            // active (same rule as handleKeyEvent). Mappings meant for "keyboard not in use" must not
            // block Shift – otherwise this letter stays lowercase and the NEXT letter gets capitalized.
            if (SettingsManager.isButtonOverrideEnabled(this, keyCode) &&
                SettingsManager.isIgnoreKeyboardActive(this, keyCode) &&
                hasCustomMapping(keyCode, currentState())
            ) return false
        }

        shiftConsumedKeys += keyCode
        ButtonState.setKeyPressed(keyCode, true)
        if (isShiftHoldActive) {
            isShiftHoldActive = false
            updateModifierIndicator()
        }
        injectKey(keyCode, shift = true, ctrl = event.isCtrlPressed, alt = event.isAltPressed, typing = true)
        return true
    }

    private fun processKeyEvent(keyCode: Int, isDown: Boolean, state: String) {
        val isVolumeKey = keyCode == 24 || keyCode == 25
        val isVolumeSkipActive = state == "OFF" && isVolumeKey && SettingsManager.isVolumeLongPressSkipEnabled(this)

        Log.d(tag, "processKeyEvent: keyCode=$keyCode, isDown=$isDown, state=$state, clickCount=$clickCount")

        if (isDown) {
            if (longPressRunnables.containsKey(keyCode)) return

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

            downClickCounts[keyCode] = clickCount

            val longPressRunnable = Runnable {
                isLongPress[keyCode] = true
                val clickCountAtDown = downClickCounts[keyCode] ?: 0
                clickCount = 0
                pendingMultiClick?.let { handler.removeCallbacks(it); pendingMultiClick = null }

                val type = when {
                    clickCountAtDown == 2 -> "DOUBLE_TAP_AND_HOLD"
                    clickCountAtDown > 0 -> "PRESS_AND_HOLD"
                    else -> "HOLD"
                }
                Log.d(tag, "LONG PRESS FIRED: type=$type for keyCode=$keyCode (state=$state, clickCountAtDown=$clickCountAtDown)")
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
            Log.d(tag, "processKeyEvent UP: keyCode=$keyCode, wasLongPress=$wasLongPress, clickCountBeforeIncrement=$clickCount")
            if (wasLongPress) {
                clickCount = 0
                downClickCounts.remove(keyCode)
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

            // Zero-latency: fire Single press right away, skip the 350 ms multi-click window.
            // clickCount stays 0, so a following hold is always detected as plain HOLD.
            if (SettingsManager.isZeroLatencyEnabled(this, keyCode)) {
                pendingMultiClick?.let { handler.removeCallbacks(it) }
                pendingMultiClick = null
                clickCount = 0
                Log.d(tag, "ZERO-LATENCY single press for keyCode=$keyCode (state=$state)")
                performAction(keyCode, state, "SINGLE_PRESS")
                return
            }

            clickCount++
            Log.d(tag, "clickCount incremented to: $clickCount for keyCode=$keyCode")

            pendingMultiClick?.let { handler.removeCallbacks(it) }

            val multiClickRunnable = Runnable {
                val type = when (clickCount) {
                    1 -> "SINGLE_PRESS"
                    2 -> "DOUBLE_PRESS"
                    3 -> "TRIPLE_PRESS"
                    4 -> "QUADRUPLE_PRESS"
                    5 -> "QUINTUPLE_PRESS"
                    else -> "MULTI"
                }
                Log.d(tag, "MULTI-CLICK FIRED: type=$type for keyCode=$keyCode (clickCount=$clickCount)")
                performAction(keyCode, state, type)
                clickCount = 0
                pendingMultiClick = null
            }
            pendingMultiClick = multiClickRunnable
            handler.postDelayed(multiClickRunnable, clickDelay)
        }
    }

    // TOAST
    private fun showKeyToast(keyCode: Int) {
        if (!SHOW_KEY_TOASTS) return
        val now = System.currentTimeMillis()
        if (keyCode == lastToastKeyCode && now - lastToastTime < 300L) return
        lastToastKeyCode = keyCode
        lastToastTime = now

        val name = when (keyCode) {
            131 -> "AI Button"
            133 -> "Camera Up"
            132 -> "Camera Down"
            111 -> "Keyboard Button"
            24 -> "Volume Up"
            25 -> "Volume Down"
            27 -> "Camera Button"
            134 -> "Focus Button"
            else -> {
                val rawName = KeyEvent.keyCodeToString(keyCode)
                if (rawName.startsWith("KEYCODE_")) {
                    rawName.removePrefix("KEYCODE_")
                        .lowercase()
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                } else {
                    rawName
                }
            }
        }

        handler.post {
            keyToast?.cancel()
            keyToast = Toast.makeText(applicationContext, "$name, keycode: $keyCode", Toast.LENGTH_SHORT).also { it.show() }
        }
    }

    private fun shouldSwallowPhantomSpace(keyCode: Int, isDown: Boolean, fromShizuku: Boolean): Boolean {
        val now = SystemClock.uptimeMillis()
        when (keyCode) {
            KeyEvent.KEYCODE_CTRL_LEFT if isDown -> {
                ctrlLeftDownTime = now
            }

            KeyEvent.KEYCODE_SPACE if isDown &&
                    now - ctrlLeftDownTime < phantomSpaceWindow -> {
                swallowSpaceUp[fromShizuku] = true
                Log.d(tag, "Blocked phantom SPACE after CTRL_LEFT (${now - ctrlLeftDownTime}ms, fromShizuku=$fromShizuku)")
                return true
            }

            KeyEvent.KEYCODE_SPACE if !isDown && swallowSpaceUp[fromShizuku] == true -> {
                swallowSpaceUp[fromShizuku] = false
                return true
            }
        }
        return false
    }

    private fun handleKeyEvent(keyCode: Int, isDown: Boolean, fromShizuku: Boolean): Boolean {
        // Shizuku path: ignore the phantom SPACE too (Shizuku only observes, so this just skips mapping)
        if (fromShizuku && shouldSwallowPhantomSpace(keyCode, isDown, fromShizuku = true)) return true

        // Key already handled by Shift Hold / Shift Lock typing on the accessibility path
        if (fromShizuku && keyCode in shiftConsumedKeys) return true

        // Managed Shift is handled entirely in onKeyEvent; don't let the Shizuku copy run the mapping too
        if (fromShizuku && SettingsManager.isBetterShiftKey(this, keyCode)) return true

        // TOAST: Shizuku events don't pass through onKeyEvent, so toast them here, before any early return
        if (isDown && fromShizuku) showKeyToast(keyCode)

        // Every press arrives twice (Shizuku + Accessibility). Track it per key so a press is only
        // ever counted once – otherwise single press becomes double press, double becomes quadruple.
        val dup = isDuplicatePressEvent(keyCode, isDown, fromShizuku)

        val km = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        val isLocked = km.isKeyguardLocked
        val isInteractive = powerManager.isInteractive
        val state = if (isInteractive && !isLocked) "ON" else "OFF"

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

        // Gamepad mode runs before the Button Override gate below. Button Override exists to decide
        // whether a *typing* key should be intercepted; a key the user explicitly bound on the
        // gamepad picker is meant to be intercepted, so requiring that unrelated flag made every
        // binding except the hardcoded-on comma key silently do nothing.
        if (gamepadActive && keyCode in keyboardKeyCodes) {
            val gamepadAction =
                SettingsManager.getGamepadBinding(this, activeGamepadProfile(), keyCode)
            Log.d(tag, "Gamepad probe: key=$keyCode binding=${gamepadAction?.label} down=$isDown")
            if (gamepadAction != null && !dup) {
                updateButtonState()
                val stick = gamepadAction.stick
                when {
                    // A profile switch sends no controller input: it only changes which profile's
                    // bindings are in force, and does so on the press (nothing to release).
                    gamepadAction.profile != null -> {
                        if (isDown) {
                            SettingsManager.setActiveGamepadProfile(this, gamepadAction.profile)
                            // Mirror it to the UI so the header and the key sub-labels follow the
                            // switch immediately, rather than only on the next screen re-entry.
                            ButtonState.setGamepadProfile(gamepadAction.profile)
                            showProfileToast(gamepadAction.profile)
                            Log.i(tag, "Gamepad profile switched to ${gamepadAction.profile} (key $keyCode)")
                        }
                    }
                    stick != null && isDown -> pressStickDirection(gamepadAction, stick)
                    stick != null -> releaseStickDirection(gamepadAction, stick)
                    // Triggers are analog axes, driven fully on or fully off.
                    SettingsManager.isTriggerKey(gamepadAction.keyCode) ->
                        setGamepadTrigger(gamepadAction.keyCode, isDown)
                    // The D-pad rides the hat axes; press moves it, release re-centres.
                    isDpadAction(gamepadAction.keyCode) ->
                        setGamepadDpad(gamepadAction.keyCode, isDown)
                    isDown -> triggerGamepadAction(gamepadAction)
                    else -> releaseGamepadAction(gamepadAction)
                }
                return true
            }
        }

        if (!SettingsManager.isButtonOverrideEnabled(this, keyCode)) {
            updateButtonState()
            return false
        }

        val isOverrideEnabled = SettingsManager.isOverrideScreenOffEnabled(this)
        val shizukuAvailable = fromShizuku || ShellManager.isAvailable()
        val isVolumeKey = keyCode == 24 || keyCode == 25

        if (state == "OFF") {
            if (!isOverrideEnabled) return false

            if (isVolumeKey) {
                if (SettingsManager.isVolumeLongPressSkipEnabled(this)) {
                    if (!dup) processKeyEvent(keyCode, isDown, state)
                    return true
                }
                return false
            }

            if (!shizukuAvailable && !isInteractive) {
                return false
            }
        }

        val currentPackage = lastPackageName
        val isCameraApp = currentPackage?.contains("camera", ignoreCase = true) == true

        // ── Camera app pass-through ────────────────────────────────────────────
        if ((isCameraApp || isCameraInUse) && SettingsManager.isDisableInCamera(this)) {
            updateButtonState()
            return false
        }

        // ── Volume panel pass-through ──────────────────────────────────────────
        if (isVolumeKey && SettingsManager.isDefaultWhenVolumeVisible(this) && isVolumePanelVisibleState) {
            markVolumePanelVisible()
            updateButtonState()
            return false
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

        // Deduplicate events between Shizuku and AccessibilityService (decided by isDuplicatePressEvent)
        val isDuplicate = dup
        if (isDuplicate) {
            Log.v(tag, "Duplicate event detected and ignored: $keyCode ${if (isDown) "DOWN" else "UP"} fromShizuku=$fromShizuku")
            updateButtonState()
            if (!isDown) stopContinuousAction(keyCode)
            return true
        }

        updateButtonState()

        // --- Hardware sequence logic for 134 (Focus) and 27 (Shutter) ---
        // Must run BEFORE the generic keyboard-key handling below: on some devices the dedicated
        // focus key (134) collides with KEYCODE_F3, and the camera button (27) emits a paired focus
        // event that has to be suppressed here – otherwise it would fire the user's 134 mapping and
        // break double-tap on the camera button.
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

        // Keyboard-key handling
        if (keyCode in keyboardKeyCodes) {
            ButtonState.setKeyPressed(keyCode, isDown)

            // Respect the "Ignore keyboard active" setting for every key, including arrow-mapped
            // ones: without it, a key must not override typing / a focused text field.
            if (!SettingsManager.isIgnoreKeyboardActive(this, keyCode) && isTextFieldFocused()) return false

            val isOurApp = currentPackage == packageName
            if (isOurApp) return true

            if (!hasCustomMapping(keyCode, state)) return false

            // Reaching here means the event was already deduplicated above.
            processKeyEvent(keyCode, isDown, state)
            return true
        }

        processKeyEvent(keyCode, isDown, state)
        return true
    }

    private fun startContinuousAction(keyCode: Int, state: String, type: String) {
        stopContinuousAction(keyCode)

        val action = SettingsManager.getEffectiveAction(this, keyCode, state, type)

        // Only actions where repeating makes sense repeat while held; everything else
        // (toggles, clipboard, emoji, app launches, screenshots …) fires exactly once per hold.
        val volumePanelDefault = SettingsManager.isDefaultWhenVolumeVisible(this) &&
                isVolumePanelVisibleState && (keyCode == 24 || keyCode == 25)
        if (!volumePanelDefault && !isRepeatableAction(keyCode, action)) return

        val isKeyRepeat = action.startsWith(SettingsManager.PREFIX_KEY_INPUT) ||
                action in SettingsManager.ARROW_ACTIONS
        val isScroll = action in smoothScrollActions
        val isVolume = action == SettingsManager.ACTION_VOLUME_UP ||
                action == SettingsManager.ACTION_VOLUME_DOWN ||
                action == SettingsManager.ACTION_DEFAULT

        val delay = when {
            isKeyRepeat -> 100L // like a keyboard's auto-repeat
            isScroll -> 250L
            isVolume -> 150L
            else -> 600L
        }

        val task = object : Runnable {
            override fun run() {
                if (continuousActionTask[keyCode] != this) return

                // Failsafe against "zombie" repetition if the UP event was missed or ignored.
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
        val action = SettingsManager.getEffectiveAction(this, keyCode, state, type)
        Log.i(tag, ">>>> EXECUTING: $action [Key: $keyCode, State: $state, Type: $type, Interactive: ${powerManager.isInteractive}] <<<<")

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
                if (type == "HOLD" || type == "LONG") {
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
            SettingsManager.ACTION_MUTE_MIC_TOGGLE -> { toggleMicPrivacy(); true }
            SettingsManager.ACTION_FLASHLIGHT -> { toggleFlashlight(); true }
            SettingsManager.ACTION_SCREENSHOT -> performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            SettingsManager.ACTION_LOCK -> performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            SettingsManager.ACTION_AOD,
            SettingsManager.ACTION_LOCK_AOD,
            SettingsManager.ACTION_LOCK_MEDIA_AOD -> {
                if (SettingsManager.isAodLockEnabled(this)) {
                    performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                    handler.postDelayed({ startWatchActivity() }, 500)
                } else {
                    startWatchActivity()
                }
                true
            }
            SettingsManager.ACTION_HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
            SettingsManager.ACTION_BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
            SettingsManager.ACTION_RECENTS -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            SettingsManager.ACTION_NOTIFICATIONS -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            SettingsManager.ACTION_QUICK_SETTINGS -> performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
            SettingsManager.ACTION_POWER_DIALOG -> performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
            SettingsManager.ACTION_LAST_APP -> { switchToLastApp(); true }
            SettingsManager.ACTION_APP_INFO -> { openAppInfo(); true }
            SettingsManager.ACTION_SHOW_MENU -> { ShellManager.injectKey(KeyEvent.KEYCODE_MENU); true }
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
            SettingsManager.ACTION_ASPECT_RATIO_FULL_4_3 -> {
                toggleAspectRatio(listOf("RESET", "4:3"), direction = 1)
                true
            }
            SettingsManager.ACTION_ASPECT_RATIO_FULL_16_9 -> {
                toggleAspectRatio(listOf("RESET", "16:9"), direction = 1)
                true
            }
            SettingsManager.ACTION_ASPECT_RATIO_CYCLE, SettingsManager.ACTION_ASPECT_RATIO_FULL_4_3_16_9 -> {
                toggleAspectRatio(listOf("RESET", "4:3", "16:9"), direction = 1)
                true
            }
            SettingsManager.ACTION_ASPECT_RATIO_UP -> {
                toggleAspectRatio(listOf("RESET", "4:3", "16:9"), direction = 1)
                true
            }
            SettingsManager.ACTION_ASPECT_RATIO_DOWN -> {
                toggleAspectRatio(listOf("RESET", "4:3", "16:9"), direction = -1)
                true
            }
            SettingsManager.ACTION_SIZE_FULL -> { setScreenSize("RESET"); true }
            SettingsManager.ACTION_SIZE_4_3 -> { setScreenSize("4:3"); true }
            SettingsManager.ACTION_SIZE_16_9 -> { setScreenSize("16:9"); true }
            SettingsManager.ACTION_DENSITY_CYCLE -> {
                toggleDensity(listOf("300", "400"), direction = 1)
                true
            }
            SettingsManager.ACTION_DENSITY_300 -> {
                ShellManager.runShellCommand("wm density 300")
                true
            }
            SettingsManager.ACTION_DENSITY_400 -> {
                ShellManager.runShellCommand("wm density 400")
                true
            }
            SettingsManager.ACTION_SCROLL_UP, "TAP_SCROLL_UP" -> { performScroll(true); true }
            SettingsManager.ACTION_SCROLL_DOWN, "TAP_SCROLL_DOWN" -> { performScroll(false); true }
            SettingsManager.ACTION_SCROLL_UP_SMOOTH, "TAP_SCROLL_UP_SMOOTH" -> { performScroll(true); true }
            SettingsManager.ACTION_SCROLL_DOWN_SMOOTH, "TAP_SCROLL_DOWN_SMOOTH" -> { performScroll(false); true }
            SettingsManager.ACTION_SCROLL_UP_SMOOTH_FAST -> { performScroll(true, multiplier = 2f); true }
            SettingsManager.ACTION_SCROLL_DOWN_SMOOTH_FAST -> { performScroll(false, multiplier = 2f); true }

            // ── Keyboard ──────────────────────────────────────────────────
            SettingsManager.ACTION_ARROW_UP,
            SettingsManager.ACTION_ARROW_DOWN,
            SettingsManager.ACTION_ARROW_LEFT,
            SettingsManager.ACTION_ARROW_RIGHT -> {
                // Explicit Arrow action: send a plain keyboard arrow, never virtual controller input.
                val name = SettingsManager.arrowKeyName(finalAction)!!
                executeKeyInputAction(
                    KeyInput.Spec(shift = false, ctrl = false, alt = false, key = name),
                    asKeyboard = true
                )
                true
            }
            SettingsManager.ACTION_SHIFT_HOLD -> {
                // Like a phone keyboard: tapping Shift while Shift Lock is on turns the lock off.
                if (isShiftLockActive) toggleShiftLock() else toggleShiftHold()
                true
            }
            SettingsManager.ACTION_SHIFT_LOCK -> { toggleShiftLock(); true }
            SettingsManager.ACTION_TAB -> {
                executeKeyInputAction(KeyInput.Spec(shift = false, ctrl = false, alt = false, key = "Tab"))
                true
            }
            SettingsManager.ACTION_REVERSE_TAB -> {
                executeKeyInputAction(KeyInput.Spec(shift = true, ctrl = false, alt = false, key = "Tab"))
                true
            }
            SettingsManager.ACTION_OPEN_EMOJI_KEYBOARD -> { openEmojiKeyboard(); true }

            // Switch the active controller profile. Deliberately inert outside gamepad mode:
            // the profiles are a gamepad-only concept, so a key mapped to this action keeps doing
            // nothing in normal keyboard mode rather than silently changing layout state.
            SettingsManager.ACTION_PROFILE_1,
            SettingsManager.ACTION_PROFILE_2,
            SettingsManager.ACTION_PROFILE_3 -> {
                val profile = SettingsManager.profileForAction(finalAction)
                if (profile != null && gamepadActive) {
                    SettingsManager.setActiveGamepadProfile(this, profile)
                    ButtonState.setGamepadProfile(profile)
                    Log.i(tag, "Gamepad profile switched to $profile")
                } else if (profile != null) {
                    Log.d(tag, "Profile action ignored: gamepad mode is not active")
                }
                true
            }

            SettingsManager.ACTION_NONE -> { Log.d(tag, "Action is NONE, key blocked."); true }
            else -> {
                if (finalAction.startsWith(SettingsManager.PREFIX_APP)) {
                    launchApp(finalAction.removePrefix(SettingsManager.PREFIX_APP))
                } else if (finalAction.startsWith(SettingsManager.PREFIX_SHORTCUT)) {
                    val shortcutData = finalAction.removePrefix(SettingsManager.PREFIX_SHORTCUT)
                    wakeUpScreenAndDismissAod()
                    val useOverlay = SettingsManager.isScreenOffShortcutOverlayEnabled(this)
                    launchShortcutWithOverlay(shortcutData, useOverlay)
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
                } else if (finalAction.startsWith(SettingsManager.PREFIX_CUSTOM_SIZE)) {
                    val customSize = finalAction.removePrefix(SettingsManager.PREFIX_CUSTOM_SIZE)
                    toggleAspectRatio(listOf("RESET", customSize), direction = 1)
                    true
                } else if (finalAction.startsWith(SettingsManager.PREFIX_CUSTOM_DENSITY)) {
                    val customDensity = finalAction.removePrefix(SettingsManager.PREFIX_CUSTOM_DENSITY)
                    toggleDensity(listOf("300", customDensity), direction = 1)
                    true
                } else if (finalAction.startsWith(SettingsManager.PREFIX_KEY_INPUT)) {
                    KeyInput.decode(finalAction)?.let { executeKeyInputAction(it) }
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

    private fun showToast(msg: String) {
        if (!SHOW_KEY_TOASTS) return
        handler.post {
            keyToast?.cancel()
            keyToast = Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).also { it.show() }
        }
    }

    // Reusable toast for profile switches. Kept apart from [keyToast] (which the debug key toasts
    // own) and shown regardless of SHOW_KEY_TOASTS: this is user feedback, not a debug aid, so it
    // must appear even in a release build.
    private var profileToast: Toast? = null

    /** Tells the user which controller profile is now active, e.g. "Profile 2". */
    private fun showProfileToast(profile: Int) {
        handler.post {
            profileToast?.cancel()
            profileToast = Toast.makeText(
                applicationContext,
                getString(R.string.gamepad_profile_switched, profile),
                Toast.LENGTH_SHORT
            ).also { it.show() }
        }
    }

    // ── Keyboard: Shift Hold / Shift Lock ──────────────────────────────────

    private fun vibrate(
        effectType: Int = VibrationEffect.EFFECT_CLICK,
        fallbackDurationMs: Long = 50L
    ) {
        try {
            val vibrator = (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            if (vibrator.hasVibrator()) {
                val effect = try {
                    VibrationEffect.createPredefined(effectType)
                } catch (_: Exception) {
                    VibrationEffect.createOneShot(fallbackDurationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                }
                vibrator.vibrate(effect)
            }
        } catch (e: Exception) {
            Log.w(tag, "Vibration failed (VIBRATE permission in the manifest?)", e)
        }
    }

    private fun toggleShiftHold() {
        isShiftHoldActive = !isShiftHoldActive
        vibrate(VibrationEffect.EFFECT_CLICK)
        showToast(if (isShiftHoldActive) "Shift: next key" else "Shift off")
        updateModifierIndicator()
    }

    private fun toggleShiftLock() {
        isShiftLockActive = !isShiftLockActive
        isShiftHoldActive = false
        vibrate(VibrationEffect.EFFECT_HEAVY_CLICK)
        // Note: we deliberately don't inject KEYCODE_CAPS_LOCK anymore – combined with our own
        // shifting it double-applied and produced lowercase letters.
        showToast(if (isShiftLockActive) "Shift Lock on" else "Shift Lock off")
        updateModifierIndicator()
    }

    // ── Keyboard: emoji ───────────────────────────────────────────────────

    /** Last time the emoji picker was triggered – every trigger TOGGLES it, so never fire twice. */
    private var lastEmojiTriggerTime = 0L

    /**
     * Opens the emoji picker with exactly ONE signal: KEYCODE_EMOJI_PICKER (317) through the shell,
     * so it passes the normal input pipeline and reaches the keyboard app.
     *
     * Previously several signals were sent (317, Meta+., Ctrl+., Alt+., toggleSoftInput). Each one
     * toggles the picker, and the injected Ctrl+. came back through onKeyEvent and re-triggered this
     * function – so the picker opened and closed again depending on timing.
     * Also: never call performGlobalAction(18) – that is GLOBAL_ACTION_DPAD_LEFT (cursor jump).
     */
    private fun openEmojiKeyboard() {
        val now = SystemClock.uptimeMillis()
        if (now - lastEmojiTriggerTime < 1000L) {
            Log.d(tag, "openEmojiKeyboard ignored (already triggered ${now - lastEmojiTriggerTime} ms ago)")
            return
        }
        lastEmojiTriggerTime = now

        val keycodeEmojiPicker = 317 // KeyEvent.KEYCODE_EMOJI_PICKER (API 34+)
        if (!ShellManager.isAvailable()) {
            // Keys sent through the accessibility input connection go to the app, not to the keyboard
            // app, so they can't open its emoji picker – the shell is the only route that works.
            Log.w(tag, "openEmojiKeyboard needs Shizuku/root")
            showToast("Emoji keyboard needs Shizuku")
            return
        }
        Log.d(tag, "openEmojiKeyboard: sending keyevent $keycodeEmojiPicker")
        markInjected(listOf(keycodeEmojiPicker))
        Thread {
            try {
                ShellManager.runShellCommand("input keyevent $keycodeEmojiPicker")
            } catch (e: Exception) {
                Log.e(tag, "Shell emoji injection error", e)
            }
        }.start()
    }

    // ── Keyboard: key input / arrows ──────────────────────────────────────

    /**
     * [asKeyboard] marks a key the user set as an explicit *action* (e.g. the Arrow actions in the
     * action picker). Such a key must behave as a plain keyboard key and never be re-emitted as
     * virtual controller input, so it stays completely separate from the gamepad/controller mode.
     */
    private fun executeKeyInputAction(spec: KeyInput.Spec, asKeyboard: Boolean = false) {
        val keyCode = KeyInput.resolveKeyCode(spec.key)
        if (keyCode == KeyEvent.KEYCODE_UNKNOWN) {
            Log.w(tag, "Key input: unknown key '${spec.key}'")
            return
        }

        // Shift Lock behaves like Caps Lock (letters only); Shift Hold shifts any next key once.
        val lockApplies = isShiftLockActive && KeyInput.isLetter(keyCode)
        val shift = spec.shift || isShiftHoldActive || lockApplies
        if (isShiftHoldActive) {
            isShiftHoldActive = false
            updateModifierIndicator()
        }

        Log.d(tag, "Key input: key=${spec.key} ($keyCode) shift=$shift ctrl=${spec.ctrl} alt=${spec.alt}")
        injectKey(keyCode, shift, spec.ctrl, spec.alt, asKeyboard = asKeyboard)
    }

    private fun metaStateFor(shift: Boolean, ctrl: Boolean, alt: Boolean): Int =
        (if (shift) KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON else 0) or
                (if (ctrl) KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON else 0) or
                (if (alt) KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON else 0)

    /**
     * Delivers a key with modifiers, trying the best available route:
     * 1. Accessibility input connection (Android 13+) – instant, works in any focused text field, no Shizuku.
     * 2. Shell (`input keycombination` / `input keyevent`) – works everywhere, but each call is slow.
     * 3. Accessibility node actions – text fields only (type, delete, move cursor, select, copy/paste).
     *
     * [typing] = re-sending physical keyboard input; prefers the fast, ordered routes over the shell
     * so fast typing doesn't arrive out of order.
     */
    private fun injectKey(
        keyCode: Int,
        shift: Boolean,
        ctrl: Boolean,
        alt: Boolean,
        typing: Boolean = false,
        asKeyboard: Boolean = false
    ) {
        val meta = metaStateFor(shift, ctrl, alt)

        // Arrow keys double as D-pad navigation. Inside a text field they move the caret; outside
        // they move focus like a controller, which the IME connection can't do, so route those to
        // the dedicated path instead of the generic IME-first flow below.
        if (keyCode in KeyInput.arrowKeyCodes && !typing) {
            if (injectViaDpadNavigation(keyCode, meta, asKeyboard)) {
                Log.d(tag, "injectKey $keyCode -> dpad navigation")
                return
            }
        }

        if (injectViaAccessibilityIme(keyCode, meta)) {
            Log.d(tag, "injectKey $keyCode -> accessibility IME connection")
            return
        }

        val shellReady = ShellManager.isAvailable()
        if ((typing || !shellReady) && injectViaNodeActions(keyCode, meta)) {
            Log.d(tag, "injectKey $keyCode -> node actions")
            return
        }

        if (shellReady) {
            Log.d(tag, "injectKey $keyCode -> shell")
            injectViaShell(keyCode, shift, ctrl, alt)
            return
        }
        Log.w(tag, "Key $keyCode not delivered: no focused text field and no Shizuku/root")
        showToast("Key input needs a focused text field or Shizuku")
    }

    /**
     * D-pad navigation for arrow keys.
     *
     * The preferred route is a virtual gamepad: the event enters the system tagged SOURCE_GAMEPAD,
     * so Android's Focus Navigation Engine moves focus before the app is consulted. That is what
     * makes arrows behave like a physical Xbox controller and removes the "only works in some
     * apps" problem, because the app never gets the chance to ignore the key.
     *
     * If the gamepad device cannot be created (no Shizuku/root, or an OEM blocks /dev/uinput) the
     * original chain still runs: caret movement in text fields, shell D-pad injection, the raw key
     * to the focused app, a geometric focus search, then a scroll fallback.
     */
    private fun injectViaDpadNavigation(keyCode: Int, meta: Int, asKeyboard: Boolean = false): Boolean {
        val editable = focusedEditableNode()
        if (editable != null) {
            // Text field: move the caret at character/line granularity like a keyboard arrow.
            if (injectViaNodeActions(keyCode, meta)) return true
        }

        // 0) Controller-style navigation through a virtual gamepad, when enabled and available.
        //
        // Skipped for a user-set Arrow *action* (asKeyboard): that action must be delivered as a
        // real keyboard arrow, so it never enters the virtual controller device. This keeps the
        // settable Arrow actions fully separate from the gamepad/controller mode.
        if (!asKeyboard && SettingsManager.isVirtualDpadEnabled(this)) {
            val direction = dpadDirectionFor(keyCode)
            if (direction != null && ShellManager.sendDpad(direction)) {
                Log.d(tag, "injectKey $keyCode -> virtual gamepad ($direction)")
                return true
            }
        }

        // 1) Real system focus navigation via shell when available (exact, works everywhere).
        if (ShellManager.isAvailable()) {
            injectViaShell(keyCode, shift = false, ctrl = false, alt = false)
            return true
        }

        // 2) No shell: hand the raw D-pad key to the focused app. Apps with their own D-pad/focus
        //    handling (TV UIs, launchers, lists) move focus from this. This must happen BEFORE the
        //    generic IME path so it isn't swallowed only when a text field is focused.
        if (editable == null && injectViaAccessibilityIme(keyCode, meta)) {
            return true
        }

        // 3) Focus-search fallback: move focus to the nearest focusable node in the arrow's direction.
        if (moveFocusByDirection(keyCode)) return true

        // 4) Last resort: scroll the nearest scrollable container in the arrow's direction.
        return performDpadScroll(keyCode)
    }

    // ---------------------------------------------------------------------
    // Gamepad mode
    // ---------------------------------------------------------------------
    //
    // Kept separate from the keyboard path on purpose. Nothing in this section writes to the
    // arrow/action storage, and the arrow path never reads anything here, so enabling gamepad
    // mode cannot change how arrow keys behave.

    /**
     * Set when the user starts the controller by hand from settings.
     *
     * While true the controller stays advertised whatever app is in front, and the watchdog leaves
     * it alone instead of tearing it down on its next pass.
     */
    @Volatile
    private var manualGamepadRequested = false

    /**
     * Whether the resident gamepad device is currently advertised.
     *
     * Written by the watchdog and read on the key-event path, so it is volatile: the key path
     * must see the latest value without waiting for the next sync pass.
     */
    @Volatile
    private var gamepadActive = false

    /** How often the gamepad state is re-checked, independent of window-change events. */
    private val GAMEPAD_WATCHDOG_INTERVAL_MS = 1500L

    /**
     * The controller profile whose bindings are currently in force.
     *
     * Read on every gamepad key event, so switching profile takes effect immediately without a
     * restart. Profiles are independent of the keyboard path -- this is only consulted inside the
     * gamepad branch below.
     */
    private fun activeGamepadProfile(): Int = SettingsManager.getActiveGamepadProfile(this)

    /**
     * Creates or removes the gamepad device so it matches the app that is now in front.
     *
     * The controller is advertised only inside the apps the user chose, so the rest of the system
     * never sees a controller that is not being used.
     */
    private fun syncGamepadForForegroundApp() {
        // Falls back to the window's own package when no window change has been seen yet, which is
        // the case right after a service start. Without this the lookup used a null package and
        // gamepad mode never engaged until the user happened to switch apps.
        val foreground = lastPackageName
            ?: rootInActiveWindow?.packageName?.toString()

        // A manual start from settings holds the controller open regardless of the foreground app,
        // until the user stops it. Without this the watchdog would tear it down on its next pass.
        if (manualGamepadRequested) {
            if (!gamepadActive) {
                if (ShellManager.startGamepad()) {
                    gamepadActive = true
                    showGamepadNotification(foreground)
                    Log.i(tag, "Gamepad started manually")
                } else {
                    Log.w(tag, "Manual gamepad start failed")
                }
            }
            return
        }

        val shouldBeActive = SettingsManager.isGamepadApp(this, foreground)

        // Only act (and log) when something actually differs. The watchdog runs every 1.5 s, so
        // logging each pass would flood the log and hide the entries that matter.
        if (shouldBeActive == gamepadActive) {
            // Still re-post the notification while active, so it survives being dismissed.
            if (gamepadActive) showGamepadNotification(foreground)
            return
        }

        Log.d(
            tag,
            "Gamepad sync: foreground=$foreground shouldBeActive=$shouldBeActive " +
                "active=$gamepadActive"
        )
        when {
            shouldBeActive && !gamepadActive -> {
                if (ShellManager.startGamepad()) {
                    gamepadActive = true
                    showGamepadNotification(foreground)
                    Log.i(tag, "Gamepad mode active for $foreground")
                } else {
                    Log.w(tag, "Gamepad mode requested but the device could not be created")
                }
            }
            !shouldBeActive && gamepadActive -> {
                ShellManager.stopGamepad()
                gamepadActive = false
                hideGamepadNotification()
                Log.i(tag, "Gamepad mode left, controller removed")
            }
        }
    }

    /**
     * Sends one gamepad binding: a controller button, or a stick direction.
     *
     * Buttons go through the normal key path; stick directions are accumulated per stick so two
     * can be held at once (e.g. L-Up + L-Right) and each contributes to its own axis.
     */
    private fun triggerGamepadAction(action: SettingsManager.GamepadAction) {
        val stick = action.stick
        if (stick != null) {
            pressStickDirection(action, stick)
            return
        }
        // Sent through the gamepad device as a real controller button, not injected as a key
        // event: an app reading controller input would ignore a synthetic keyevent.
        val hid = SettingsManager.hidCodeFor(action.keyCode)
        if (hid == 0) {
            Log.w(tag, "Gamepad: no HID code for ${action.label} (${action.keyCode})")
            return
        }
        ShellManager.sendPadButton(hid, isDown = true)
    }

    /** True when a binding is one of the four D-pad directions. */
    private fun isDpadAction(androidKeyCode: Int): Boolean = when (androidKeyCode) {
        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> true
        else -> false
    }

    /**
     * Currently held D-pad directions.
     *
     * Tracked as a set so two can be held at once (up + left gives a diagonal) and releasing one
     * leaves the other applied, exactly like a real hat switch.
     */
    private val heldDpadDirections = mutableSetOf<Int>()

    /** Presses or releases one D-pad direction and re-sends the resulting hat position. */
    private fun setGamepadDpad(androidKeyCode: Int, isDown: Boolean) {
        if (isDown) heldDpadDirections.add(androidKeyCode) else heldDpadDirections.remove(androidKeyCode)

        val x = (if (KeyEvent.KEYCODE_DPAD_RIGHT in heldDpadDirections) 1 else 0) -
            (if (KeyEvent.KEYCODE_DPAD_LEFT in heldDpadDirections) 1 else 0)
        val y = (if (KeyEvent.KEYCODE_DPAD_DOWN in heldDpadDirections) 1 else 0) -
            (if (KeyEvent.KEYCODE_DPAD_UP in heldDpadDirections) 1 else 0)

        Log.d(tag, "Gamepad dpad: x=$x y=$y")
        ShellManager.sendPadDpad(x, y)
    }

    /**
     * Sets one trigger fully pressed or released.
     *
     * Triggers are 0..255 axes, not buttons: pressing sets 255, releasing returns to 0. The other
     * trigger keeps whatever value it already had.
     */
    private fun setGamepadTrigger(androidKeyCode: Int, isDown: Boolean) {
        val left = if (androidKeyCode == KeyEvent.KEYCODE_BUTTON_L2) isDown else heldTriggers.contains(KeyEvent.KEYCODE_BUTTON_L2)
        val right = if (androidKeyCode == KeyEvent.KEYCODE_BUTTON_R2) isDown else heldTriggers.contains(KeyEvent.KEYCODE_BUTTON_R2)

        if (isDown) heldTriggers.add(androidKeyCode) else heldTriggers.remove(androidKeyCode)

        val leftValue = if (left) TRIGGER_FULL else 0
        val rightValue = if (right) TRIGGER_FULL else 0
        Log.d(tag, "Gamepad trigger: left=$leftValue right=$rightValue")
        ShellManager.sendPadTrigger(leftValue, rightValue)
    }

    /** Which triggers are currently held, so each axis is independent of the other. */
    private val heldTriggers = mutableSetOf<Int>()

    /** A trigger is fully on or fully off, so there are only these two values. */
    private val TRIGGER_FULL = 255

    /** Releases the controller button bound to a key, so it does not stay latched down. */
    private fun releaseGamepadAction(action: SettingsManager.GamepadAction) {
        val hid = SettingsManager.hidCodeFor(action.keyCode)
        if (hid == 0) return
        ShellManager.sendPadButton(hid, isDown = false)
    }

    /** Currently held stick directions, per stick. */
    private val heldStickDirections =
        mutableMapOf<Boolean, MutableSet<SettingsManager.StickDirection>>()

    private fun pressStickDirection(
        action: SettingsManager.GamepadAction,
        stick: SettingsManager.StickDirection
    ) {
        heldStickDirections.getOrPut(stick.isLeft) { mutableSetOf() }.add(stick)
        Log.d(tag, "Gamepad stick direction held: ${action.label}")
        applySticks()
    }

    private fun releaseStickDirection(
        action: SettingsManager.GamepadAction,
        stick: SettingsManager.StickDirection
    ) {
        heldStickDirections[stick.isLeft]?.remove(stick)
        Log.d(tag, "Gamepad stick direction released: ${action.label}")
        // Recomputes both sticks; a stick with nothing held sums to zero, i.e. centred.
        applySticks()
    }

    /**
     * One stick's axis value.
     *
     * Held directions are summed and clamped to full deflection, so up + right gives a diagonal
     * at full value on both axes, while opposing directions cancel back to centre.
     */
    private fun stickAxis(isLeft: Boolean, axis: Int): Int {
        val held = heldStickDirections[isLeft].orEmpty()
        val sum = held.sumOf { if (axis == 0) it.x else it.y }.coerceIn(-1, 1)
        return sum * STICK_FULL_DEFLECTION
    }

    /** Sends both sticks from everything currently held. */
    private fun applySticks() {
        ShellManager.sendPadStick(
            leftX = stickAxis(isLeft = true, axis = 0),
            leftY = stickAxis(isLeft = true, axis = 1),
            rightX = stickAxis(isLeft = false, axis = 0),
            rightY = stickAxis(isLeft = false, axis = 1)
        )
    }

    /** Full deflection an analog stick reports at its limit; matches the native device range. */
    private val STICK_FULL_DEFLECTION = 32767

    /**
     * Maps an arrow key code to the helper's direction name, or null if it is not an arrow.
     * The names match the protocol implemented by `virtual_dpad.c`.
     */
    private fun dpadDirectionFor(keyCode: Int): String? = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_UP -> "UP"
        KeyEvent.KEYCODE_DPAD_DOWN -> "DOWN"
        KeyEvent.KEYCODE_DPAD_LEFT -> "LEFT"
        KeyEvent.KEYCODE_DPAD_RIGHT -> "RIGHT"
        else -> null
    }

    /**
     * No-root D-pad navigation: finds the nearest focusable node in the arrow's direction (geometric
     * search over the accessibility tree) and focuses it, imitating a controller D-pad.
     *
     * ACTION_FOCUS does not report back through findFocus(FOCUS_ACCESSIBILITY), so we keep our own
     * cursor of the last node we focused. Otherwise, the next press would recompute from a null/stale
     * reference and keep re-selecting the same first item.
     */
    private var dpadFocusCursor: AccessibilityNodeInfo? = null

    private fun moveFocusByDirection(keyCode: Int): Boolean {
        return try {
            val root = rootInActiveWindow ?: return false

            val current = dpadFocusCursor
                ?: root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
                ?: root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)

            val currentRect = android.graphics.Rect()
            current?.getBoundsInScreen(currentRect)
            val cx = currentRect.exactCenterX()
            val cy = currentRect.exactCenterY()
            val hasCurrent = current != null && currentRect.width() > 0 && currentRect.height() > 0

            var best: AccessibilityNodeInfo? = null
            var bestScore = Float.MAX_VALUE
            val candidateRect = android.graphics.Rect()

            val all = ArrayList<AccessibilityNodeInfo>()
            collectFocusableNodes(root, all)
            for (candidate in all) {
                // Skip the node we already focused (compare bounds, since node identity is not stable).
                candidate.getBoundsInScreen(candidateRect)
                val w = candidateRect.width()
                val h = candidateRect.height()
                if (w <= 0 || h <= 0) continue
                if (hasCurrent && candidateRect.left == currentRect.left &&
                    candidateRect.top == currentRect.top && candidateRect.right == currentRect.right &&
                    candidateRect.bottom == currentRect.bottom
                ) continue

                val dx = candidateRect.exactCenterX() - cx
                val dy = candidateRect.exactCenterY() - cy

                val (primary, secondary) = when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_RIGHT -> dx to kotlin.math.abs(dy)
                    KeyEvent.KEYCODE_DPAD_LEFT -> -dx to kotlin.math.abs(dy)
                    KeyEvent.KEYCODE_DPAD_DOWN -> dy to kotlin.math.abs(dx)
                    else -> -dy to kotlin.math.abs(dx) // DPAD_UP
                }
                // Require the candidate to be in the requested direction when we have a cursor.
                if (hasCurrent && primary <= 0f) continue

                // Prefer aligned, close nodes: primary distance dominates, cross-axis penalized.
                val score = if (hasCurrent) primary + secondary * 2f else secondary
                if (score < bestScore) {
                    bestScore = score
                    best = candidate
                }
            }

            val target = best ?: return false
            val focused = target.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            if (focused) dpadFocusCursor = target
            return focused
        } catch (e: Exception) {
            Log.w(tag, "D-pad focus search failed", e)
            false
        }
    }

    /** Collects every visible, focusable node in the tree (bounded depth to stay cheap). */
    private fun collectFocusableNodes(
        node: AccessibilityNodeInfo?,
        out: MutableList<AccessibilityNodeInfo>,
        depth: Int = 0
    ) {
        if (node == null || depth > 40) return
        if (node.isFocusable && node.isVisibleToUser) out.add(node)
        for (i in 0 until node.childCount) {
            collectFocusableNodes(node.getChild(i), out, depth + 1)
        }
    }

    /**
     * Last-resort fallback for arrow keys when there is no shell and no focusable target.
     * Moves the nearest scrollable ancestor the way a controller would.
     */
    private fun performDpadScroll(keyCode: Int): Boolean {
        val forward = keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
        return try {
            val focused = rootInActiveWindow ?: return false
            var node: AccessibilityNodeInfo? = focused.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                ?: focused.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
            var scrollable: AccessibilityNodeInfo? = null
            while (node != null) {
                if (node.isScrollable) {
                    scrollable = node
                    break
                }
                node = node.parent
            }
            shellSafeScroll(scrollable, forward)
        } catch (e: Exception) {
            Log.w(tag, "D-pad scroll fallback failed", e)
            false
        }
    }

    private fun shellSafeScroll(node: AccessibilityNodeInfo?, forward: Boolean): Boolean {
        return node != null && node.performAction(
            if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        )
    }

    private fun injectViaAccessibilityIme(keyCode: Int, meta: Int): Boolean {
        return try {
            val im = inputMethod ?: return false
            if (!im.currentInputStarted) return false
            val connection = im.currentInputConnection ?: return false
            val flags = KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE
            val down = SystemClock.uptimeMillis()
            connection.sendKeyEvent(
                KeyEvent(down, down, KeyEvent.ACTION_DOWN, keyCode, 0, meta, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, flags)
            )
            connection.sendKeyEvent(
                KeyEvent(down, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, keyCode, 0, meta, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, flags)
            )
            true
        } catch (e: Exception) {
            Log.w(tag, "Accessibility input connection failed", e)
            false
        }
    }

    private fun injectViaShell(keyCode: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        val modifiers = buildList {
            if (ctrl) add(KeyEvent.KEYCODE_CTRL_LEFT)
            if (alt) add(KeyEvent.KEYCODE_ALT_LEFT)
            if (shift) add(KeyEvent.KEYCODE_SHIFT_LEFT)
        }
        markInjected(modifiers + keyCode)

        // "input keyevent 59 29" presses Shift and A one after another, it does NOT hold Shift.
        // keycombination actually holds the modifiers down.
        val command = if (modifiers.isEmpty()) {
            "input keyevent $keyCode"
        } else {
            "input keycombination ${(modifiers + keyCode).joinToString(" ")}"
        }
        ShellManager.runShellCommand(command)
    }

    private fun injectViaNodeActions(keyCode: Int, meta: Int): Boolean {
        val node = focusedEditableNode() ?: return false
        val shift = (meta and KeyEvent.META_SHIFT_ON) != 0
        val ctrl = (meta and KeyEvent.META_CTRL_ON) != 0
        val alt = (meta and KeyEvent.META_ALT_ON) != 0

        if (ctrl) {
            return when (keyCode) {
                KeyEvent.KEYCODE_C -> node.performAction(AccessibilityNodeInfo.ACTION_COPY)
                KeyEvent.KEYCODE_X -> node.performAction(AccessibilityNodeInfo.ACTION_CUT)
                KeyEvent.KEYCODE_V -> node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
                KeyEvent.KEYCODE_A -> setSelection(node, 0, currentText(node).length)
                else -> false
            }
        }
        if (alt) return false

        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                val horizontal = keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                val forward = keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                val args = Bundle().apply {
                    putInt(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                        if (horizontal) AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER
                        else AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE
                    )
                    putBoolean(AccessibilityNodeInfo.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN, shift)
                }
                node.performAction(
                    if (forward) AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                    else AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY,
                    args
                )
            }
            KeyEvent.KEYCODE_MOVE_HOME -> setSelection(node, 0, 0)
            KeyEvent.KEYCODE_MOVE_END -> currentText(node).length.let { setSelection(node, it, it) }
            KeyEvent.KEYCODE_DEL -> replaceSelection(node, "", deleteBefore = 1)
            KeyEvent.KEYCODE_FORWARD_DEL -> replaceSelection(node, "", deleteAfter = 1)
            KeyEvent.KEYCODE_ENTER -> {
                if (!node.isMultiLine) {
                    node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id)
                } else {
                    replaceSelection(node, "\n")
                }
            }
            else -> {
                val text = KeyInput.charFor(keyCode, if (shift) KeyEvent.META_SHIFT_ON else 0) ?: return false
                replaceSelection(node, text)
            }
        }
    }

    private fun currentText(node: AccessibilityNodeInfo): String =
        if (node.isShowingHintText) "" else node.text?.toString().orEmpty()

    private fun setSelection(node: AccessibilityNodeInfo, start: Int, end: Int): Boolean {
        val args = Bundle().apply {
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, start)
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, end)
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args)
    }

    /** Replaces the selection (or deletes around the cursor) via SET_TEXT, keeping the cursor in place. */
    private fun replaceSelection(
        node: AccessibilityNodeInfo,
        insert: String,
        deleteBefore: Int = 0,
        deleteAfter: Int = 0
    ): Boolean {
        val text = currentText(node)
        var start = node.textSelectionStart
        var end = node.textSelectionEnd
        if (start < 0 || end < 0 || start > text.length || end > text.length) {
            start = text.length
            end = text.length
        }
        if (start > end) start = end.also { end = start }
        if (start == end) {
            start = max(0, start - deleteBefore)
            end = min(text.length, end + deleteAfter)
        }
        if (insert.isEmpty() && start == end) return true // nothing to delete

        val newText = text.substring(0, start) + insert + text.substring(end)
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
        }
        if (!node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) return false
        val cursor = start + insert.length
        setSelection(node, cursor, cursor)
        return true
    }

    // ── Everything below is unchanged ─────────────────────────────────────

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
            else -> if (keyCode in keyboardKeyCodes) injectDefaultKeyboardKey(keyCode)
        }
    }

    /**
     * DEFAULT for a keyboard key: we consumed the real key (because another press type of it is
     * mapped), so re-send it ourselves. Shift Hold / Shift Lock apply exactly like normal typing.
     */
    private fun injectDefaultKeyboardKey(keyCode: Int) {
        val holdApplies = isShiftHoldActive && KeyInput.isShiftable(keyCode)
        val lockApplies = isShiftLockActive && KeyInput.isLetter(keyCode)
        if (holdApplies) {
            isShiftHoldActive = false
            updateModifierIndicator()
        }
        Log.d(tag, "DEFAULT keyboard key: re-injecting $keyCode (shift=${holdApplies || lockApplies})")
        injectKey(keyCode, shift = holdApplies || lockApplies, ctrl = false, alt = false, typing = true)
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
            Log.d(tag, "ACTION_VOICE_COMMAND failed, falling back to ACTION_ASSIST")
            // (performGlobalAction(16) was removed: 16 is GLOBAL_ACTION_DPAD_UP, not "assist")
            val assistIntent = Intent(Intent.ACTION_ASSIST)
            assistIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                startActivity(assistIntent)
            } catch (e2: Exception) {
                Log.e(tag, "Voice Assistant error", e2)
            }
        }
    }

    private fun launchGoogleSearch() {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage("com.google.android.googlequicksearchbox")
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
            } else {
                val webIntent = Intent(Intent.ACTION_WEB_SEARCH)
                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(webIntent)
            }
        } catch (e: Exception) {
            Log.e(tag, "Google search error", e)
        }
    }

    private fun switchToLastApp() {
        performGlobalAction(GLOBAL_ACTION_RECENTS)
        handler.postDelayed({
            performGlobalAction(GLOBAL_ACTION_RECENTS)
        }, 150)
    }

    private fun toggleMicPrivacy() {
        try {
            if (ShellManager.isAvailable()) {
                Thread {
                    ShellManager.runShellCommandBlocking("cmd sensor_privacy help")

                    var output = ShellManager.runShellCommandBlocking("cmd sensor_privacy get-state 0 1")
                    if (output.contains("Unknown", ignoreCase = true) || output.contains("Invalid", ignoreCase = true)) {
                        output = ShellManager.runShellCommandBlocking("cmd sensor_privacy get-state 0 microphone")
                    }

                    val isPrivacyEnabled = if (output.contains("Unknown", ignoreCase = true) || output.contains("Invalid", ignoreCase = true) || output.isEmpty()) {
                        audioManager.isMicrophoneMute
                    } else {
                        output.contains("enabled", ignoreCase = true) || output.contains("true", ignoreCase = true)
                    }

                    val nextState = !isPrivacyEnabled
                    val subCmd = if (nextState) "enable" else "disable"
                    val stateStr = if (nextState) "true" else "false"

                    val commands = listOf(
                        "cmd sensor_privacy set-state 0 1 $stateStr",
                        "cmd sensor_privacy set-state 0 microphone $stateStr",
                        "cmd sensor_privacy $subCmd 0 1",
                        "cmd sensor_privacy $subCmd 0 microphone",
                        "cmd sensor_privacy $subCmd microphone"
                    )

                    var success = false
                    for (cmd in commands) {
                        val res = ShellManager.runShellCommandBlocking(cmd)
                        if (res.isEmpty() || (!res.contains("Unknown", ignoreCase = true) && !res.contains("Invalid", ignoreCase = true))) {
                            Log.d(tag, "Mic Privacy Toggle Success with command: $cmd")
                            success = true
                            break
                        }
                    }

                    audioManager.isMicrophoneMute = nextState
                    Log.d(tag, "Mic Privacy Toggle: TargetState=$nextState, Success=$success")
                }.start()
            } else {
                audioManager.isMicrophoneMute = !audioManager.isMicrophoneMute
            }
        } catch (e: Exception) {
            Log.e(tag, "Error toggling mic privacy", e)
            audioManager.isMicrophoneMute = !audioManager.isMicrophoneMute
        }
    }

    private fun updateLauncherPackages() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfos = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        launcherPackages.clear()
        for (ri in resolveInfos) {
            ri.activityInfo.packageName?.let { launcherPackages.add(it) }
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

    private fun wakeUpScreenAndDismissAod() {
        try {
            sendBroadcast(Intent("com.xenonware.mindcontrol.ACTION_DISMISS_AOD"))

            if (!powerManager.isInteractive && ShellManager.isAvailable()) {
                Thread { ShellManager.runShellCommand("input keyevent ${KeyEvent.KEYCODE_WAKEUP}") }.start()
            }

            @Suppress("DEPRECATION")
            val screenWakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                "MindControl:ShortcutWakeLock"
            )
            screenWakeLock.acquire(3000)
        } catch (e: Exception) {
            Log.e(tag, "Error waking up screen", e)
        }
    }

    private fun launchShortcutWithOverlay(shortcutData: String, useOverlay: Boolean = true) {
        try {
            val intent = Intent(this, ShortcutOverlayActivity::class.java).apply {
                putExtra(ShortcutOverlayActivity.EXTRA_SHORTCUT_DATA, shortcutData)
                putExtra(ShortcutOverlayActivity.EXTRA_USE_OVERLAY, useOverlay)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(tag, "Error launching shortcut overlay", e)
            val km = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            if (!km.isKeyguardLocked) launchShortcut(shortcutData)
        }
    }

    private fun launchShortcut(shortcutData: String) {
        ShortcutOverlayActivity.launchShortcut(this, shortcutData)
    }

    private fun launchSpeedDial(number: String) {
        val intent = Intent(Intent.ACTION_CALL)
        intent.data = "tel:$number".toUri()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(tag, "Speed dial error", e)
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
            AudioManager.RINGER_MODE_SILENT -> 1
            AudioManager.RINGER_MODE_VIBRATE -> 2
            AudioManager.RINGER_MODE_NORMAL -> 0
            else -> 0
        }

        Log.d(tag, "Sound Mode Cycle: Current=$currentMode, NextModeTarget=$nextMode")

        try {
            if (!nm.isNotificationPolicyAccessGranted) {
                Log.w(tag, "cycleSoundMode: DND permission not granted")
                return
            }

            when (nextMode) {
                0 -> {
                    val vol = audioManager.getStreamVolume(AudioManager.STREAM_RING)
                    if (vol > 0) lastKnownRingVolume = vol
                    if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
                        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    }
                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                }
                1 -> {
                    if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                        val vol = audioManager.getStreamVolume(AudioManager.STREAM_RING)
                        if (vol > 0) lastKnownRingVolume = vol
                    }
                    if (audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT) {
                        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    }
                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                }
                2 -> {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
                    val targetVol = if (lastKnownRingVolume > 0) lastKnownRingVolume
                    else (maxVol / 2).coerceAtLeast(1)
                    audioManager.setStreamVolume(AudioManager.STREAM_RING, targetVol, 0)
                    audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, targetVol, 0)
                }
            }

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
            Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
            val current = try {
                Settings.System.getInt(contentResolver, Settings.System.USER_ROTATION)
            } catch (_: Exception) { 0 }
            val next = if (current == 0) 1 else 0
            Settings.System.putInt(contentResolver, Settings.System.USER_ROTATION, next)
            Log.d(tag, "Rotation Toggle: $current -> $next")
        } catch (e: Exception) {
            Log.e(tag, "Rotation toggle error. Make sure 'Allow Sys Settings' is granted.", e)
        }
    }

    private fun toggleRotation360() {
        try {
            Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
            val current = try {
                Settings.System.getInt(contentResolver, Settings.System.USER_ROTATION)
            } catch (_: Exception) { 0 }
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

            if (ShellManager.isShizukuAvailable()) {
                ShellManager.runShellCommand("settings put system screen_brightness_mode $next")
                Log.d(tag, "Auto-Brightness Toggle (Shizuku): $mode -> $next")
            } else if (ShellManager.isRootAvailable()) {
                ShellManager.runShellCommand("settings put system screen_brightness_mode $next")
                Log.d(tag, "Auto-Brightness Toggle (Root): $mode -> $next")
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
            if (ShellManager.isAvailable()) {
                ShellManager.runShellCommand("svc wifi $next")
                Log.d(tag, "Wifi Toggle: $current -> $next")
            } else {
                Log.e(tag, "Wifi toggle requires Shell access (Shizuku or Root)")
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

            if (ShellManager.isAvailable()) {
                ShellManager.runShellCommand("svc bluetooth $cmd")
                Log.d(tag, "Bluetooth Toggle: $isEnabled -> $next")
            } else {
                Log.e(tag, "Bluetooth toggle requires Shell access")
            }
        } catch (e: Exception) {
            Log.e(tag, "Bluetooth toggle error", e)
        }
    }

    private fun toggleData() {
        try {
            val current = Settings.Global.getInt(contentResolver, "mobile_data", 0)
            val next = if (current == 1) "disable" else "enable"
            if (ShellManager.isAvailable()) {
                ShellManager.runShellCommand("svc data $next")
                Log.d(tag, "Data Toggle: $current -> $next")
            } else {
                Log.e(tag, "Mobile Data toggle requires Shell access")
            }
        } catch (e: Exception) {
            Log.e(tag, "Data toggle error", e)
        }
    }

    private fun toggleNfc() {
        try {
            val current = Settings.Global.getInt(contentResolver, "nfc_on", 0)
            val next = if (current == 1) "disable" else "enable"
            if (ShellManager.isAvailable()) {
                ShellManager.runShellCommand("svc nfc $next")
                Log.d(tag, "NFC Toggle: $current -> $next")
            } else {
                Log.e(tag, "NFC toggle requires Shell access")
            }
        } catch (e: Exception) {
            Log.e(tag, "NFC toggle error", e)
        }
    }

    private fun toggleLocation() {
        try {
            val lm = getSystemService(LOCATION_SERVICE) as LocationManager
            val isEnabled = lm.isLocationEnabled
            val nextMode = if (isEnabled) 0 else 3

            if (ShellManager.isAvailable()) {
                ShellManager.runShellCommand("settings put secure location_mode $nextMode")
                Log.d(tag, "Location Toggle: $isEnabled -> $nextMode")
            } else {
                Log.e(tag, "Location toggle requires Shell access")
            }
        } catch (e: Exception) {
            Log.e(tag, "Location toggle error", e)
        }
    }

    private fun calculateHeight43(width: Int): Int {
        val h = kotlin.math.round(width * 4.0 / 3.0).toInt()
        return if (h % 2 != 0) h + 1 else h
    }

    private fun calculateHeight169(width: Int): Int {
        val h = kotlin.math.round(width * 16.0 / 9.0).toInt()
        return if (h % 2 != 0) h + 1 else h
    }

    private fun extractPhysicalDimensions(output: String): Pair<Int, Int> {
        val match = Regex("""Physical size:\s*(\d+)x(\d+)""", RegexOption.IGNORE_CASE).find(output)
        val w = match?.groupValues?.get(1)?.toIntOrNull()
        val h = match?.groupValues?.get(2)?.toIntOrNull()
        if (w != null && h != null) {
            return Pair(w, h)
        }
        return try {
            val dm = resources.displayMetrics
            Pair(dm.widthPixels, dm.heightPixels)
        } catch (_: Exception) {
            Pair(1080, 1920)
        }
    }

    private fun getPhysicalDimensions(): Pair<Int, Int> {
        return try {
            if (ShellManager.isAvailable()) {
                val output = ShellManager.runShellCommandBlocking("wm size")
                extractPhysicalDimensions(output)
            } else {
                val dm = resources.displayMetrics
                Pair(dm.widthPixels, dm.heightPixels)
            }
        } catch (_: Exception) {
            Pair(1080, 1920)
        }
    }

    private fun setScreenSize(target: String) {
        try {
            if (ShellManager.isAvailable()) {
                when (target.uppercase()) {
                    "RESET", "FULL" -> {
                        ShellManager.runShellCommand("wm size reset")
                        Log.d(tag, "Screen size set to full (wm size reset)")
                    }
                    "4:3" -> {
                        val (width, _) = getPhysicalDimensions()
                        val height = calculateHeight43(width)
                        ShellManager.runShellCommand("wm size ${width}x$height")
                        Log.d(tag, "Screen size set to 4:3 (${width}x$height)")
                    }
                    "16:9" -> {
                        val (width, _) = getPhysicalDimensions()
                        val height = calculateHeight169(width)
                        ShellManager.runShellCommand("wm size ${width}x$height")
                        Log.d(tag, "Screen size set to 16:9 (${width}x$height)")
                    }
                    else -> {
                        ShellManager.runShellCommand("wm size $target")
                        Log.d(tag, "Screen size set to $target")
                    }
                }
            } else {
                Log.e(tag, "Setting screen size requires Shell access (Shizuku or Root)")
            }
        } catch (e: Exception) {
            Log.e(tag, "Set screen size error", e)
        }
    }

    private fun toggleAspectRatio(targets: List<String>, direction: Int = 1) {
        try {
            if (ShellManager.isAvailable()) {
                val output = ShellManager.runShellCommandBlocking("wm size")
                val (physicalWidth, physicalHeight) = extractPhysicalDimensions(output)
                val physicalSizeString = "${physicalWidth}x${physicalHeight}"
                val target43 = "${physicalWidth}x${calculateHeight43(physicalWidth)}"
                val target169 = "${physicalWidth}x${calculateHeight169(physicalWidth)}"

                val resolvedSizes = targets.map { target ->
                    when (target.uppercase()) {
                        "RESET", "FULL", "1080X1240" -> "RESET"
                        "4:3" -> target43
                        "16:9" -> target169
                        else -> target
                    }
                }

                val overrideMatch = Regex("""Override size:\s*(\d+)x(\d+)""", RegexOption.IGNORE_CASE).find(output)
                val currentOverride = overrideMatch?.let { "${it.groupValues[1]}x${it.groupValues[2]}" }

                val currentIndex = if (currentOverride == null || currentOverride.equals(physicalSizeString, ignoreCase = true)) {
                    val resetIdx = resolvedSizes.indexOf("RESET")
                    if (resetIdx != -1) resetIdx else resolvedSizes.indexOf(physicalSizeString)
                } else {
                    resolvedSizes.indexOf(currentOverride)
                }

                val nextIndex = if (currentIndex != -1) {
                    (currentIndex + direction + resolvedSizes.size) % resolvedSizes.size
                } else {
                    if (direction >= 0) 0 else resolvedSizes.lastIndex
                }

                val nextSize = resolvedSizes[nextIndex]
                if (nextSize == "RESET") {
                    ShellManager.runShellCommand("wm size reset")
                    Log.d(tag, "Aspect Ratio Toggle: $currentIndex -> $nextIndex (wm size reset)")
                } else {
                    ShellManager.runShellCommand("wm size $nextSize")
                    Log.d(tag, "Aspect Ratio Toggle: $currentIndex -> $nextIndex ($nextSize)")
                }
            } else {
                Log.e(tag, "Aspect Ratio toggle requires Shell access (Shizuku or Root)")
            }
        } catch (e: Exception) {
            Log.e(tag, "Aspect Ratio toggle error", e)
        }
    }

    private fun toggleDensity(densities: List<String>, direction: Int = 1) {
        try {
            if (ShellManager.isAvailable()) {
                val output = ShellManager.runShellCommandBlocking("wm density")
                val lines = output.lines()
                val overrideLine = lines.find { it.contains("Override density:", ignoreCase = true) }
                val activeLine = overrideLine ?: lines.find { it.contains("Physical density:", ignoreCase = true) } ?: output

                val currentIndex = densities.indexOfFirst { activeLine.contains(it) }
                val nextIndex = if (currentIndex != -1) {
                    (currentIndex + direction + densities.size) % densities.size
                } else {
                    0
                }
                val nextDensity = densities[nextIndex]
                ShellManager.runShellCommand("wm density $nextDensity")
                Log.d(tag, "Density Toggle: $currentIndex -> $nextIndex ($nextDensity)")
            } else {
                Log.e(tag, "Density toggle requires Shell access (Shizuku or Root)")
            }
        } catch (e: Exception) {
            Log.e(tag, "Density toggle error", e)
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
        keyToast?.cancel()
        cameraManager.unregisterAvailabilityCallback(cameraCallback)
        wakeLock?.let { if (it.isHeld) it.release() }
        Shizuku.removeBinderReceivedListener(binderListener)
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        if (instance === this) instance = null
        ShellManager.stopMonitoring()
        handler.removeCallbacks(gamepadWatchdog)
        // Drop the resident controller first, then the helper: otherwise the phone would keep
        // reporting a connected gamepad after the service is gone.
        ShellManager.stopGamepad()
        gamepadActive = false
        hideGamepadNotification()
        ShellManager.stopVirtualDpad()
    }
}