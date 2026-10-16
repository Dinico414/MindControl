package com.xenonware.mindcontrol

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.core.content.edit
import com.xenonware.mindcontrol.ui.theme.Palette

object SettingsManager {
    private const val PREFS_NAME = "MindControlPrefs"

    const val ACTION_DEFAULT = "DEFAULT"
    const val ACTION_NONE = "NONE"
    const val ACTION_PLAY_PAUSE = "PLAY_PAUSE"
    const val ACTION_NEXT = "NEXT"
    const val ACTION_PREVIOUS = "PREVIOUS"
    const val ACTION_VOLUME_UP = "VOLUME_UP"
    const val ACTION_VOLUME_DOWN = "VOLUME_DOWN"
    const val ACTION_FLASHLIGHT = "FLASHLIGHT"
    const val ACTION_SCREENSHOT = "SCREENSHOT"
    const val ACTION_LOCK = "LOCK"
    const val ACTION_AOD = "AOD"
    const val ACTION_LOCK_AOD = "LOCK_AOD"
    const val ACTION_LOCK_MEDIA_AOD = "LOCK_MEDIA_AOD"
    const val ACTION_BRIGHTNESS_UP = "BRIGHTNESS_UP"
    const val ACTION_BRIGHTNESS_DOWN = "BRIGHTNESS_DOWN"
    const val ACTION_HOME = "HOME"
    const val ACTION_BACK = "BACK"
    const val ACTION_RECENTS = "RECENTS"
    const val ACTION_NOTIFICATIONS = "NOTIFICATIONS"
    const val ACTION_QUICK_SETTINGS = "QUICK_SETTINGS"
    const val ACTION_ASSISTANT = "ASSISTANT"
    const val ACTION_ROTATE_TOGGLE = "ROTATE_TOGGLE"
    const val ACTION_SCROLL_UP = "SCROLL_UP"
    const val ACTION_SCROLL_DOWN = "SCROLL_DOWN"
    const val ACTION_SCROLL_UP_SMOOTH = "SCROLL_UP_SMOOTH"
    const val ACTION_SCROLL_DOWN_SMOOTH = "SCROLL_DOWN_SMOOTH"
    const val ACTION_SCROLL_UP_SMOOTH_FAST = "SCROLL_UP_SMOOTH_FAST"
    const val ACTION_SCROLL_DOWN_SMOOTH_FAST = "SCROLL_DOWN_SMOOTH_FAST"

    const val ACTION_SHOW_MENU = "SHOW_MENU"
    const val ACTION_LAST_APP = "LAST_APP"
    const val ACTION_APP_INFO = "APP_INFO"
    const val ACTION_POWER_DIALOG = "POWER_DIALOG"
    const val ACTION_GOOGLE_SEARCH = "GOOGLE_SEARCH"
    const val ACTION_COPY = "COPY"
    const val ACTION_CUT = "CUT"
    const val ACTION_PASTE = "PASTE"
    const val ACTION_SPEED_DIAL = "SPEED_DIAL"
    const val ACTION_URL = "URL"
    const val ACTION_QR_CODE = "QR_CODE"

    const val ACTION_FAST_FORWARD = "FAST_FORWARD"
    const val ACTION_REWIND = "REWIND"
    const val ACTION_STOP = "STOP"
    const val ACTION_STEP_FORWARD = "STEP_FORWARD"
    const val ACTION_STEP_BACKWARD = "STEP_BACKWARD"

    const val ACTION_VIBRATE_RINGER = "VIBRATE_RINGER"
    const val ACTION_CYCLE_SOUND_MODE = "CYCLE_SOUND_MODE"
    const val ACTION_DND = "DND"
    const val ACTION_AUTO_BRIGHTNESS_TOGGLE = "AUTO_BRIGHTNESS_TOGGLE"
    const val ACTION_WIFI_TOGGLE = "WIFI_TOGGLE"
    const val ACTION_BLUETOOTH_TOGGLE = "BLUETOOTH_TOGGLE"
    const val ACTION_DATA_TOGGLE = "DATA_TOGGLE"
    const val ACTION_NFC_TOGGLE = "NFC_TOGGLE"
    const val ACTION_LOCATION_TOGGLE = "LOCATION_TOGGLE"
    const val ACTION_AUTOROTATE_TOGGLE = "AUTOROTATE_TOGGLE"
    const val ACTION_ROTATE_360 = "ROTATE_360"
    const val ACTION_ASPECT_RATIO_FULL_4_3 = "ASPECT_RATIO_FULL_4_3"
    const val ACTION_ASPECT_RATIO_FULL_16_9 = "ASPECT_RATIO_FULL_16_9"
    const val ACTION_ASPECT_RATIO_CYCLE = "ASPECT_RATIO_CYCLE"
    const val ACTION_ASPECT_RATIO_FULL_4_3_16_9 = "ASPECT_RATIO_FULL_4_3_16_9"
    const val ACTION_ASPECT_RATIO_UP = "ASPECT_RATIO_UP"
    const val ACTION_ASPECT_RATIO_DOWN = "ASPECT_RATIO_DOWN"
    const val ACTION_SIZE_FULL = "SIZE_FULL"
    const val ACTION_SIZE_4_3 = "SIZE_4_3"
    const val ACTION_SIZE_16_9 = "SIZE_16_9"

    const val ACTION_DENSITY_CYCLE = "DENSITY_CYCLE"
    const val ACTION_DENSITY_300 = "DENSITY_300"
    const val ACTION_DENSITY_400 = "DENSITY_400"
    const val ACTION_CUSTOM_SIZE_TOGGLE = "CUSTOM_SIZE_TOGGLE"
    const val ACTION_CUSTOM_DENSITY_TOGGLE = "CUSTOM_DENSITY_TOGGLE"

    const val ACTION_MUTE_VOL = "MUTE_VOL"
    const val ACTION_MUTE_MIC_TOGGLE = "MUTE_MIC_TOGGLE"
    const val ACTION_VOLUME_DIALOG = "VOLUME_DIALOG"

    const val PREFIX_APP = "APP:"
    const val PREFIX_SHORTCUT = "SHORTCUT:"
    const val PREFIX_SPEED_DIAL = "SPEED_DIAL:"
    const val PREFIX_URL = "URL:"
    const val PREFIX_QR_CODE = "QR_CODE:"
    const val PREFIX_CUSTOM_SIZE = "CUSTOM_SIZE:"
    const val PREFIX_CUSTOM_DENSITY = "CUSTOM_DENSITY:"

    // ── Keyboard actions ──────────────────────────────────────────────────
    const val ACTION_KEY_INPUT = "KEY_INPUT"
    const val PREFIX_KEY_INPUT = "KEY_INPUT:"
    const val ACTION_ARROW_UP = "ARROW_UP"
    const val ACTION_ARROW_DOWN = "ARROW_DOWN"
    const val ACTION_ARROW_LEFT = "ARROW_LEFT"
    const val ACTION_ARROW_RIGHT = "ARROW_RIGHT"
    const val ACTION_SHIFT_HOLD = "SHIFT_HOLD"
    const val ACTION_SHIFT_LOCK = "SHIFT_LOCK"
    const val ACTION_OPEN_EMOJI_KEYBOARD = "OPEN_EMOJI_KEYBOARD"
    const val ACTION_TAB = "TAB"
    const val ACTION_REVERSE_TAB = "REVERSE_TAB"

    val ARROW_ACTIONS = setOf(ACTION_ARROW_UP, ACTION_ARROW_DOWN, ACTION_ARROW_LEFT, ACTION_ARROW_RIGHT)

    // ── Gamepad profiles ────────────────────────────────────────────────
    //
    // Switching the active controller profile is an ordinary, user-assignable action -- not a
    // hardcoded key shortcut. The three actions below appear in the action picker like any other,
    // so the user decides which button selects which profile. They only do anything while gamepad
    // mode is active, so keyboard mode is never affected.

    const val ACTION_PROFILE_1 = "PROFILE_1"
    const val ACTION_PROFILE_2 = "PROFILE_2"
    const val ACTION_PROFILE_3 = "PROFILE_3"

    /**
     * Profile number (1..3) an action selects, or null when it is not a profile action.
     *
     * The active profile is only ever changed through this mapping, so the action storage and the
     * binding storage stay decoupled.
     */
    fun profileForAction(action: String): Int? = when (action) {
        ACTION_PROFILE_1 -> 1
        ACTION_PROFILE_2 -> 2
        ACTION_PROFILE_3 -> 3
        else -> null
    }

    val GAMEPAD_PROFILES: IntRange = 1..3

    /** Key name (understood by [KeyInput.resolveKeyCode]) for an arrow action, or null. */
    fun arrowKeyName(action: String): String? = when (action) {
        ACTION_ARROW_UP -> "Up"
        ACTION_ARROW_DOWN -> "Down"
        ACTION_ARROW_LEFT -> "Left"
        ACTION_ARROW_RIGHT -> "Right"
        else -> null
    }

    // ── Gamepad mode ─────────────────────────────────────────────────────
    //
    // Entirely separate from the keyboard/arrow logic above. Gamepad mode only takes effect while
    // a user-chosen app is in the foreground, and it stores its own bindings under their own
    // prefix, so nothing here can affect arrow-key navigation.

    private const val KEY_GAMEPAD_APPS = "gamepad_apps"
    private const val KEY_GAMEPAD_BINDING_PREFIX = "gamepad_binding_"

    /**
     * The pre-profile binding key, from a build that had a single gamepad layout.
     *
     * Kept only to read an existing setup once; new writes always go through the per-profile key.
     */
    private const val KEY_GAMEPAD_BINDING_LEGACY_PREFIX = "gamepad_binding_"

    /**
     * Packages whose keyboard switches to the gamepad setup.
     *
     * The pad device is only reported as connected while one of these is in the foreground, so
     * the phone never claims a controller exists in apps the user did not choose.
     */
    fun getGamepadApps(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_GAMEPAD_APPS, emptySet()) ?: emptySet()

    fun setGamepadApps(context: Context, packages: Set<String>) {
        prefs(context).edit { putStringSet(KEY_GAMEPAD_APPS, packages) }
    }

    fun isGamepadApp(context: Context, packageName: String?): Boolean =
        packageName != null && packageName in getGamepadApps(context)

    private const val KEY_ACTIVE_GAMEPAD_PROFILE = "active_gamepad_profile"

    /**
     * The controller profile currently in use (1..3).
     *
     * Clamped on read, so a stored value from an older build can never point at a profile that no
     * longer exists.
     */
    fun getActiveGamepadProfile(context: Context): Int =
        prefs(context).getInt(KEY_ACTIVE_GAMEPAD_PROFILE, 1).coerceIn(GAMEPAD_PROFILES)

    fun setActiveGamepadProfile(context: Context, profile: Int) {
        prefs(context).edit { putInt(KEY_ACTIVE_GAMEPAD_PROFILE, profile.coerceIn(GAMEPAD_PROFILES)) }
    }

    /**
     * A controller action assignable to one keyboard key.
     *
     * A button carries a real [android.view.KeyEvent] key code. A stick direction carries axis
     * values instead, because a stick reports a position rather than pressing a button.
     */
    data class GamepadAction(
        val label: String,
        val keyCode: Int = 0,
        val stick: StickDirection? = null,
        // Set for a profile-switch binding: pressing the key activates this controller profile
        // instead of sending any controller input. Null for every ordinary controller action.
        val profile: Int? = null,
        // Optional drawable for the action. Drawn on the assigned keyboard key and next to the
        // row in the picker, so a bound key shows the same glyph as the control it maps to.
        @androidx.annotation.DrawableRes val iconRes: Int? = null
    )

    /**
     * One stick direction. Each axis is -1, 0 or 1; [isLeft] picks which stick it belongs to.
     */
    data class StickDirection(val isLeft: Boolean, val x: Int, val y: Int)

    /**
     * The three profile-switch bindings, shown in the gamepad picker next to the controller
     * actions so the user can assign "switch to profile N" to any key.
     */
    val GAMEPAD_PROFILE_ACTIONS: List<GamepadAction> = buildList {
        GAMEPAD_PROFILES.forEach { n ->
            add(
                GamepadAction(
                    label = "Profile $n",
                    profile = n,
                    iconRes = profileIconRes(n)
                )
            )
        }
    }

    /** Numbered counter drawable for a controller profile (1..3). */
    @androidx.annotation.DrawableRes
    fun profileIconRes(profile: Int): Int = when (profile) {
        2 -> R.drawable.counter_2_24px
        3 -> R.drawable.counter_3_24px
        else -> R.drawable.counter_1_24px
    }

    val GAMEPAD_STICK_ACTIONS: List<GamepadAction> = buildList {
        fun stick(label: String, isLeft: Boolean, x: Int, y: Int, iconRes: Int) =
            add(GamepadAction(label, stick = StickDirection(isLeft, x, y), iconRes = iconRes))

        stick("L-Up", isLeft = true, x = 0, y = -1, R.drawable.game_stick_l_up_24px)
        stick("L-Down", isLeft = true, x = 0, y = 1, R.drawable.game_stick_l_down_24px)
        stick("L-Left", isLeft = true, x = -1, y = 0, R.drawable.game_stick_l_left_24px)
        stick("L-Right", isLeft = true, x = 1, y = 0, R.drawable.game_stick_l_right_24px)
        stick("R-Up", isLeft = false, x = 0, y = -1, R.drawable.game_stick_r_up_24px)
        stick("R-Down", isLeft = false, x = 0, y = 1, R.drawable.game_stick_r_down_24px)
        stick("R-Left", isLeft = false, x = -1, y = 0, R.drawable.game_stick_r_left_24px)
        stick("R-Right", isLeft = false, x = 1, y = 0, R.drawable.game_stick_r_right_24px)
    }

    /**
     * Linux HID usage code for an Android controller key code, or 0 when there is none.
     *
     * The resident device is driven by HID usages (BTN_A, BTN_TL …) rather than Android key
     * codes, so bindings are translated on the way out. Kept here as a pure function so it can be
     * unit tested without a device.
     */
    fun hidCodeFor(androidKeyCode: Int): Int = when (androidKeyCode) {
        android.view.KeyEvent.KEYCODE_BUTTON_A -> 0x130
        android.view.KeyEvent.KEYCODE_BUTTON_B -> 0x131
        android.view.KeyEvent.KEYCODE_BUTTON_X -> 0x133
        android.view.KeyEvent.KEYCODE_BUTTON_Y -> 0x134
        android.view.KeyEvent.KEYCODE_BUTTON_L1 -> 0x136
        android.view.KeyEvent.KEYCODE_BUTTON_R1 -> 0x137
        // L2 / R2 are analog trigger axes, handled by sendPadTrigger, not as buttons.
        android.view.KeyEvent.KEYCODE_BUTTON_L2 -> 0
        android.view.KeyEvent.KEYCODE_BUTTON_R2 -> 0
        // SELECT is 158 (0x09e) on the stock Xbox keylayout, not the 0x13a a generic pad would
        // use. Sending the wrong code meant the tester never saw it.
        android.view.KeyEvent.KEYCODE_BUTTON_SELECT -> 0x09e
        android.view.KeyEvent.KEYCODE_BUTTON_START -> 0x13b
        android.view.KeyEvent.KEYCODE_BUTTON_MODE -> 0x13c
        android.view.KeyEvent.KEYCODE_BUTTON_THUMBL -> 0x13d
        android.view.KeyEvent.KEYCODE_BUTTON_THUMBR -> 0x13e
        // D-pad is NOT sent as button codes: the Xbox keylayout maps it to the hat axes, so the
        // direction is carried by ABS_HAT0X / ABS_HAT0Y instead (see sendPadDpad).
        else -> 0
    }

    /** Which trigger a key code refers to, or null when it is not a trigger. */
    fun isTriggerKey(androidKeyCode: Int): Boolean =
        androidKeyCode == android.view.KeyEvent.KEYCODE_BUTTON_L2 ||
            androidKeyCode == android.view.KeyEvent.KEYCODE_BUTTON_R2

    val GAMEPAD_BUTTON_ACTIONS: List<GamepadAction> = listOf(
        GamepadAction("A", android.view.KeyEvent.KEYCODE_BUTTON_A, iconRes = R.drawable.a_circle_24px),
        GamepadAction("B", android.view.KeyEvent.KEYCODE_BUTTON_B, iconRes = R.drawable.b_circle_24px),
        GamepadAction("X", android.view.KeyEvent.KEYCODE_BUTTON_X, iconRes = R.drawable.x_circle_24px),
        GamepadAction("Y", android.view.KeyEvent.KEYCODE_BUTTON_Y, iconRes = R.drawable.y_circle_24px),
        GamepadAction("D-Up", android.view.KeyEvent.KEYCODE_DPAD_UP, iconRes = R.drawable.gamepad_up_24px),
        GamepadAction("D-Down", android.view.KeyEvent.KEYCODE_DPAD_DOWN, iconRes = R.drawable.gamepad_down_24px),
        GamepadAction("D-Left", android.view.KeyEvent.KEYCODE_DPAD_LEFT, iconRes = R.drawable.gamepad_left_24px),
        GamepadAction("D-Right", android.view.KeyEvent.KEYCODE_DPAD_RIGHT, iconRes = R.drawable.gamepad_right_24px),
        GamepadAction("L-Bumper", android.view.KeyEvent.KEYCODE_BUTTON_L1, iconRes = R.drawable.game_bumper_left_24px),
        GamepadAction("R-Bumper", android.view.KeyEvent.KEYCODE_BUTTON_R1, iconRes = R.drawable.game_bumper_right_24px),
        GamepadAction("L-Trigger", android.view.KeyEvent.KEYCODE_BUTTON_L2, iconRes = R.drawable.game_trigger_left_24px),
        GamepadAction("R-Trigger", android.view.KeyEvent.KEYCODE_BUTTON_R2, iconRes = R.drawable.game_trigger_right_24px),
        GamepadAction("L3", android.view.KeyEvent.KEYCODE_BUTTON_THUMBL, iconRes = R.drawable.game_stick_l3_24px),
        GamepadAction("R3", android.view.KeyEvent.KEYCODE_BUTTON_THUMBR, iconRes = R.drawable.game_stick_r3_24px),
        GamepadAction("Start", android.view.KeyEvent.KEYCODE_BUTTON_START, iconRes = R.drawable.start_24px),
        GamepadAction("Menu", android.view.KeyEvent.KEYCODE_BUTTON_MODE, iconRes = R.drawable.home_24px),
        GamepadAction("Select", android.view.KeyEvent.KEYCODE_BUTTON_SELECT, iconRes = R.drawable.select_24px),
    )

    /**
     * Stick bindings live above every real key code so both kinds can share one stored int.
     */
    private const val STICK_BINDING_BASE = 1_000_000

    /**
     * Profile-switch bindings live above the stick range, so one int still carries all three
     * kinds of binding (controller button, stick direction, profile switch).
     */
    private const val PROFILE_BINDING_BASE = 2_000_000

    /**
     * "None", stored explicitly so it can override a preset.
     *
     * Without a stored value the presets below are applied, so simply removing the entry would
     * make "None" impossible to keep: the next read would put the preset back. This sentinel
     * occupies its own range above every real binding and decodes to null (no action).
     */
    private const val NONE_BINDING = 3_000_000

    /**
     * The binding assigned to [keyCode], or null when nothing is assigned.
     *
     * Decoding never depends on the action still being present in the picker's list. A binding is
     * reconstructed from the stored value itself, so an entry that was later renamed or removed
     * from the UI still resolves and keeps working. Relying on a list lookup here previously made
     * every binding silently stop working as soon as the list changed.
     */
    fun getGamepadBinding(context: Context, profile: Int, keyCode: Int): GamepadAction? {
        val stored = prefs(context).getInt(bindingKey(profile, keyCode), -1)
        if (stored == -1) {
            // Nothing stored for this profile+key, so the presets apply.
            //
            // The profile switcher (c/v/b) is preset in *every* profile, so the user can always
            // move between 1 and 2 and 3 no matter which one is active. Profile 1 additionally
            // ships the full controller layout; an older build's single-binding value is honoured
            // as a profile-1 fallback so an existing setup is never lost.
            if (profile == 1) {
                legacyGamepadBinding(context, keyCode)?.let { return it }
            }
            GAMEPAD_PROFILE_1_PRESET[keyCode]?.takeIf { profile == 1 }?.let { return it }
            return GAMEPAD_PROFILE_SWITCH_PRESET[keyCode]
        }

        // An explicitly stored "None": the key is unbound and no preset may re-apply.
        if (stored == NONE_BINDING) return null

        if (stored >= PROFILE_BINDING_BASE) {
            return GAMEPAD_PROFILE_ACTIONS.getOrNull(stored - PROFILE_BINDING_BASE)
        }

        if (stored >= STICK_BINDING_BASE) {
            val index = stored - STICK_BINDING_BASE
            return GAMEPAD_STICK_ACTIONS.getOrNull(index)
        }

        // A button: the stored int *is* the Android key code, so rebuild the action from it
        // rather than looking it up in the (mutable) picker list.
        return GamepadAction(labelForKeyCode(stored), stored, iconRes = iconForKeyCode(stored))
    }

    /** Display label for a controller key code, falling back to the raw number. */
    fun labelForKeyCode(androidKeyCode: Int): String =
        GAMEPAD_BUTTON_ACTIONS.firstOrNull { it.keyCode == androidKeyCode }?.label
            ?: "0x" + androidKeyCode.toString(16)

    /** Drawable for a controller key code, or null when the code has no icon. */
    private fun iconForKeyCode(androidKeyCode: Int): Int? =
        GAMEPAD_BUTTON_ACTIONS.firstOrNull { it.keyCode == androidKeyCode }?.iconRes

    /**
     * The pre-profile binding for a key, from a build that had only one gamepad layout.
     *
     * Read-only: it is never written back, so nothing here can resurrect an old value once the
     * user assigns a real binding for the profile.
     */
    private fun legacyGamepadBinding(context: Context, keyCode: Int): GamepadAction? {
        val stored = prefs(context).getInt(KEY_GAMEPAD_BINDING_LEGACY_PREFIX + keyCode, -1)
        if (stored == -1) return null
        if (stored >= STICK_BINDING_BASE) {
            return GAMEPAD_STICK_ACTIONS.getOrNull(stored - STICK_BINDING_BASE)
        }
        return GamepadAction(labelForKeyCode(stored), stored, iconRes = iconForKeyCode(stored))
    }

    fun setGamepadBinding(context: Context, profile: Int, keyCode: Int, action: GamepadAction?) {
        val stored = when {
            // "None" is written explicitly (not removed), so it overrides any preset on read.
            action == null -> NONE_BINDING
            action.profile != null -> {
                val index = GAMEPAD_PROFILE_ACTIONS.indexOf(action)
                if (index < 0) NONE_BINDING else PROFILE_BINDING_BASE + index
            }
            action.stick != null -> {
                val index = GAMEPAD_STICK_ACTIONS.indexOf(action)
                if (index < 0) NONE_BINDING else STICK_BINDING_BASE + index
            }
            else -> action.keyCode
        }
        prefs(context).edit {
            putInt(bindingKey(profile, keyCode), stored)
        }
    }

    /** Storage key for one profile's binding of one keyboard key. */
    private fun bindingKey(profile: Int, keyCode: Int): String =
        KEY_GAMEPAD_BINDING_PREFIX + profile.coerceIn(GAMEPAD_PROFILES) + "_" + keyCode

    /**
     * Profile 1's shipped default layout.
     *
     * Resolved by action *label* against the picker's own lists, so the preset never stores raw
     * magic numbers and keeps working if a code ever changes. Only profile 1 ships this full
     * controller layout; profiles 2 and 3 are empty apart from the shared switcher below.
     */
    val GAMEPAD_PROFILE_1_PRESET: Map<Int, GamepadAction> by lazy {
        val byLabel = (GAMEPAD_BUTTON_ACTIONS + GAMEPAD_STICK_ACTIONS).associateBy { it.label }

        mapOf(
            // Left hand: left stick on w/a/s/d, left trigger/bumper on q/a, D-pad on r/t/f/g.
            45 to "L-Bumper",   // q
            51 to "L-Up",       // w
            33 to "L-Right",    // e
            46 to "D-Up",       // r
            48 to "D-Right",    // t
            29 to "L-Trigger",  // a
            47 to "L-Left",     // s
            32 to "L-Down",     // d
            34 to "D-Left",     // f
            35 to "D-Down",     // g
            // Right hand: face buttons on i/o/k/l, right stick on y/u/h/j, bumper/trigger on p/[.
            53 to "R-Left",     // y
            49 to "R-Up",       // u
            37 to "X",          // i
            43 to "Y",          // o
            44 to "R-Bumper",   // p
            36 to "R-Down",     // h
            38 to "R-Right",    // j
            39 to "A",          // k
            40 to "B",          // l
            71 to "R-Trigger",  // [
            // Stick presses and system buttons around the bottom row.
            59 to "L3",         // Shift
            67 to "R3",         // Backspace
            55 to "Select",     // ,
            62 to "Menu",       // Space
            56 to "Start",      // .
        ).mapValues { (_, label) -> byLabel.getValue(label) }
    }

    /**
     * The manual profile switcher, preset in every profile.
     *
     * `c` -> Profile 1, `v` -> Profile 2, `b` -> Profile 3, on the home row of every profile, so
     * the user can always move between profiles no matter which one is active. These are ordinary,
     * changeable bindings, not a hardcoded shortcut.
     */
    val GAMEPAD_PROFILE_SWITCH_PRESET: Map<Int, GamepadAction> by lazy {
        val profileSwitch = GAMEPAD_PROFILE_ACTIONS.associateBy { it.profile }
        mapOf(
            31 to profileSwitch.getValue(1),  // c
            50 to profileSwitch.getValue(2),  // v
            30 to profileSwitch.getValue(3),  // b
        )
    }

    /**
     * The picker's sections, in display order.
     *
     * Grouped the way a controller is laid out, so related controls sit together. [titleRes] is a
     * string resource id resolved by the UI.
     */
    data class GamepadGroup(val titleRes: Int, val actions: List<GamepadAction>)

    val GAMEPAD_GROUPS: List<GamepadGroup>
        get() = listOf(
            GamepadGroup(R.string.gamepad_group_face, listOf(
                GamepadAction("A", android.view.KeyEvent.KEYCODE_BUTTON_A, iconRes = R.drawable.a_circle_24px),
                GamepadAction("B", android.view.KeyEvent.KEYCODE_BUTTON_B, iconRes = R.drawable.b_circle_24px),
                GamepadAction("X", android.view.KeyEvent.KEYCODE_BUTTON_X, iconRes = R.drawable.x_circle_24px),
                GamepadAction("Y", android.view.KeyEvent.KEYCODE_BUTTON_Y, iconRes = R.drawable.y_circle_24px),
            )),
            GamepadGroup(R.string.gamepad_group_dpad, listOf(
                GamepadAction("D-Up", android.view.KeyEvent.KEYCODE_DPAD_UP, iconRes = R.drawable.gamepad_up_24px),
                GamepadAction("D-Down", android.view.KeyEvent.KEYCODE_DPAD_DOWN, iconRes = R.drawable.gamepad_down_24px),
                GamepadAction("D-Left", android.view.KeyEvent.KEYCODE_DPAD_LEFT, iconRes = R.drawable.gamepad_left_24px),
                GamepadAction("D-Right", android.view.KeyEvent.KEYCODE_DPAD_RIGHT, iconRes = R.drawable.gamepad_right_24px),
            )),
            // Triggers are analog axes on the Xbox layout rather than buttons, so they do not
            // work yet. They are kept in the list, clearly marked, rather than hidden.
            GamepadGroup(R.string.gamepad_group_bumpers, listOf(
                GamepadAction("L-Bumper", android.view.KeyEvent.KEYCODE_BUTTON_L1, iconRes = R.drawable.game_bumper_left_24px),
                GamepadAction("R-Bumper", android.view.KeyEvent.KEYCODE_BUTTON_R1, iconRes = R.drawable.game_bumper_right_24px),
                GamepadAction("L-Trigger", android.view.KeyEvent.KEYCODE_BUTTON_L2, iconRes = R.drawable.game_trigger_left_24px),
                GamepadAction("R-Trigger", android.view.KeyEvent.KEYCODE_BUTTON_R2, iconRes = R.drawable.game_trigger_right_24px),
            )),
            // Each stick group holds its press (L3 / R3) followed by that stick's directions.
            GamepadGroup(R.string.gamepad_group_stick_left, buildList {
                add(GamepadAction("L3", android.view.KeyEvent.KEYCODE_BUTTON_THUMBL, iconRes = R.drawable.game_stick_l3_24px))
                addAll(GAMEPAD_STICK_ACTIONS.filter { it.stick?.isLeft == true })
            }),
            GamepadGroup(R.string.gamepad_group_stick_right, buildList {
                add(GamepadAction("R3", android.view.KeyEvent.KEYCODE_BUTTON_THUMBR, iconRes = R.drawable.game_stick_r3_24px))
                addAll(GAMEPAD_STICK_ACTIONS.filter { it.stick?.isLeft == false })
            }),
            GamepadGroup(R.string.gamepad_group_system, listOf(
                GamepadAction("Start", android.view.KeyEvent.KEYCODE_BUTTON_START, iconRes = R.drawable.start_24px),
                GamepadAction("Menu", android.view.KeyEvent.KEYCODE_BUTTON_MODE, iconRes = R.drawable.home_24px),
                GamepadAction("Select", android.view.KeyEvent.KEYCODE_BUTTON_SELECT, iconRes = R.drawable.select_24px),
            )),
            // Profile switches are ordinary assignable rows like any controller action, so a key
            // can be set to change the active profile instead of sending controller input.
            GamepadGroup(R.string.gamepad_group_profiles, GAMEPAD_PROFILE_ACTIONS),
        )

    private const val KEY_DISABLE_IN_CAMERA = "disable_in_camera"
    private const val KEY_DEFAULT_WHEN_VOLUME_VISIBLE = "default_when_volume_visible"
    private const val KEY_OVERRIDE_SCREEN_OFF = "override_screen_off"
    private const val KEY_VOLUME_LONG_PRESS_SKIP = "volume_long_press_skip"
    private const val KEY_SCREEN_OFF_SHORTCUT_OVERLAY = "screen_off_shortcut_overlay"
    private const val KEY_DEVICE_PALETTE = "device_palette"
    private const val KEY_KEYBOARD_PALETTE = "keyboard_palette"
    private const val KEY_AOD_STYLE = "aod_style"
    private const val KEY_AOD_MEDIA_ENABLED = "aod_media_enabled"
    private const val KEY_AOD_LOCK_ENABLED = "aod_lock_enabled"
    private const val KEY_SHOW_LIFT_TO_WAKE_WARNING = "show_lift_to_wake_warning"

    private const val KEY_TEXT_CLOCK_LAYOUT = "text_clock_layout"
    private const val KEY_TEXT_CLOCK_SUBSTYLE = "text_clock_substyle"
    private const val KEY_TEXT_CLOCK_FONT_FAMILY = "text_clock_font_family"
    private const val KEY_TEXT_CLOCK_FONT_WEIGHT = "text_clock_font_weight"
    private const val KEY_TEXT_CLOCK_FONT_ITALIC = "text_clock_font_italic"

    private const val KEY_ANALOG_CLOCK_SUBSTYLE = "analog_clock_substyle"

    enum class TextClockLayout {
        INLINE,
        STACKED
    }

    enum class TextClockSubstyle {
        MODERN,
        NOTHING,
        DIGITAL,
        PIXEL
    }

    enum class AnalogClockSubstyle {
        CLASSIC,
        PLANETS,
        ABSTRACT
    }

    private const val FD = BuildConfig.FEATURE_DROP
    private val fdInt = FD.toIntOrNull() ?: 0
    enum class AodStyle(val minFd: Int = 0) {
        TEXT,
        CONCENTRIC,
        ANALOG,
        PLANETS(1),
        SPINNER(1),
        BLOCKS(1),
        BARS(1),
        TRACK(2),
        DIGITAL_ABSTRACT(2),
        CUBED(2),

        // Legacy values kept for backward compatibility/migration
        STACKED,
        INLINE,
        STACKED_DOT,
        INLINE_DOT,
        STACKED_DIGITAL,
        INLINE_DIGITAL,
        PIXEL_STACKED,
        PIXEL_INLINE
    }

    @Volatile
    private var cachedPrefs: SharedPreferences? = null

    /**
     * Cached after the first successful migration. Previously every getAction() call re-ran
     * moveSharedPreferencesFrom() (file I/O), several times per key press.
     */
    private fun prefs(context: Context): SharedPreferences {
        cachedPrefs?.let { return it }
        synchronized(this) {
            cachedPrefs?.let { return it }
            val appContext = context.applicationContext ?: context
            val deviceProtectedContext = appContext.createDeviceProtectedStorageContext()
            // Returns true when moved or when there was nothing to move; false e.g. before first unlock.
            val migrated = deviceProtectedContext.moveSharedPreferencesFrom(appContext, PREFS_NAME)
            val prefs = deviceProtectedContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (migrated) cachedPrefs = prefs
            return prefs
        }
    }

    // ── Better Shift ──────────────────────────────────────────────────────
    private const val KEY_BETTER_SHIFT = "better_shift"

    fun isShiftKey(keyCode: Int): Boolean =
        keyCode == android.view.KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == android.view.KeyEvent.KEYCODE_SHIFT_RIGHT

    /** On by default: Shift keys only have Single press + Hold, react instantly, and always override the keyboard. */
    fun isBetterShiftEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BETTER_SHIFT, true)

    fun setBetterShiftEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_BETTER_SHIFT, enabled) }
    }

    fun isBetterShiftKey(context: Context, keyCode: Int): Boolean =
        isShiftKey(keyCode) && isBetterShiftEnabled(context)

    /** For the button screen: the "override while keyboard is active" switch is forced on and disabled. */
    fun isKeyboardOverrideLocked(context: Context, keyCode: Int): Boolean = isBetterShiftKey(context, keyCode)

    // ── Virtual gamepad D-pad ─────────────────────────────────────────────
    private const val KEY_VIRTUAL_DPAD = "virtual_dpad"

    /**
     * On by default. Arrow actions are sent through a virtual gamepad so Android's Focus Navigation
     * Engine moves focus, the way a physical Xbox controller does, instead of handing the key to the
     * app and letting it ignore it.
     *
     * Only takes effect when the helper and /dev/uinput are actually usable, so enabling it is
     * always safe: the service silently falls back to key injection when it cannot create a device.
     */
    fun isVirtualDpadEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_VIRTUAL_DPAD, true)

    fun setVirtualDpadEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_VIRTUAL_DPAD, enabled) }
    }

    // ── Zero-latency remapping ────────────────────────────────────────────
    private const val KEY_ZERO_LATENCY_PREFIX = "zero_latency_"

    /** The comma key is preconfigured: zero-latency on, override on, single press = "default" (types ","), hold = emoji. */
    private const val COMMA_KEY = android.view.KeyEvent.KEYCODE_COMMA

    /** Press types that can still be detected while zero-latency is on (no multi-press wait). */
    val ZERO_LATENCY_PRESS_TYPES = listOf("SINGLE_PRESS", "HOLD")

    /** Shift uses Better Shift instead; Camera Up/Down (132/133) already fire instantly. */
    fun supportsZeroLatency(keyCode: Int): Boolean =
        !isShiftKey(keyCode) && keyCode != 132 && keyCode != 133

    /**
     * Off by default, except for the comma key (on by default). When on, Single press fires the
     * moment the key is released, without waiting for further presses. Actions stay user-configurable;
     * stored multi-press actions are kept and come back when this is turned off again.
     */
    fun isZeroLatencyEnabled(context: Context, keyCode: Int): Boolean =
        supportsZeroLatency(keyCode) &&
                prefs(context).getBoolean("$KEY_ZERO_LATENCY_PREFIX$keyCode", keyCode == COMMA_KEY)

    fun setZeroLatencyEnabled(context: Context, keyCode: Int, enabled: Boolean) {
        prefs(context).edit { putBoolean("$KEY_ZERO_LATENCY_PREFIX$keyCode", enabled) }
    }

    /**
     * The action that actually runs. Better Shift keys are fixed: Single press = Shift, Hold = Shift Lock,
     * every other press type is blocked. Anything stored for Shift only applies when Better Shift is off.
     */
    fun getEffectiveAction(context: Context, keyCode: Int, state: String, type: String): String {
        if (!isBetterShiftKey(context, keyCode)) return getAction(context, keyCode, state, type)
        return when (type) {
            "SINGLE_PRESS" -> ACTION_SHIFT_HOLD
            "HOLD" -> ACTION_SHIFT_LOCK
            else -> ACTION_NONE
        }
    }

    fun getAction(context: Context, keyCode: Int, state: String, type: String): String {
        val prefs = prefs(context)
        val value = prefs.getString("btn_${keyCode}_${state}_${type}", null)
        if (value != null) return value

        // Fallback for old naming to preserve user settings
        val oldType = when (type) {
            "SINGLE_PRESS" -> "SINGLE"
            "DOUBLE_PRESS" -> "DOUBLE"
            "TRIPLE_PRESS" -> "TRIPLE"
            "HOLD" -> "LONG"
            else -> null
        }
        val def = defaultAction(keyCode, type)
        if (oldType != null) {
            return prefs.getString("btn_${keyCode}_${state}_${oldType}", def) ?: def
        }

        return def
    }

    /**
     * Per-key fallback actions used when nothing is stored yet. The comma key is preconfigured so
     * that Single press types the comma normally (DEFAULT) and Hold opens the emoji keyboard.
     */
    private fun defaultAction(keyCode: Int, type: String): String =
        if (keyCode == COMMA_KEY && type == "HOLD") ACTION_OPEN_EMOJI_KEYBOARD else ACTION_DEFAULT

    fun setAction(context: Context, keyCode: Int, state: String, type: String, action: String) {
        prefs(context).edit { putString("btn_${keyCode}_${state}_${type}", action) }
    }

    fun isDisableInCamera(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DISABLE_IN_CAMERA, true)

    fun setDisableInCamera(context: Context, disabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_DISABLE_IN_CAMERA, disabled) }
    }

    fun isDefaultWhenVolumeVisible(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DEFAULT_WHEN_VOLUME_VISIBLE, true)

    fun setDefaultWhenVolumeVisible(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_DEFAULT_WHEN_VOLUME_VISIBLE, enabled) }
    }

    fun isOverrideScreenOffEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_OVERRIDE_SCREEN_OFF, true)

    fun setOverrideScreenOffEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_OVERRIDE_SCREEN_OFF, enabled) }
    }

    fun isVolumeLongPressSkipEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_VOLUME_LONG_PRESS_SKIP, false)

    fun setVolumeLongPressSkipEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_VOLUME_LONG_PRESS_SKIP, enabled) }
    }

    fun isScreenOffShortcutOverlayEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SCREEN_OFF_SHORTCUT_OVERLAY, true)

    fun setScreenOffShortcutOverlayEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_SCREEN_OFF_SHORTCUT_OVERLAY, enabled) }
    }

    fun getDevicePalette(context: Context): Palette =
        Palette.fromKey(prefs(context).getString(KEY_DEVICE_PALETTE, Palette.Black.name))

    fun setDevicePalette(context: Context, palette: Palette) {
        prefs(context).edit { putString(KEY_DEVICE_PALETTE, palette.name) }
        updateAppIcon(context, palette)
    }

    private fun updateAppIcon(context: Context, palette: Palette) {
        val packageManager = context.packageManager
        val packageName = context.packageName

        val palettes = listOf(
            Palette.Black to "MainActivityAliasBlack",
            Palette.White to "MainActivityAliasWhite",
            Palette.Pink to "MainActivityAliasPink",
            Palette.Blue to "MainActivityAliasBlue",
        )

        palettes.forEach { (p, aliasSuffix) ->
            val state = if (p == palette) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }

            try {
                // Da wir im Manifest ${applicationId}. Alias nutzen,
                // bauen wir den Namen hier exakt so zusammen.
                val aliasClass = "$packageName.$aliasSuffix"
                packageManager.setComponentEnabledSetting(
                    ComponentName(packageName, aliasClass),
                    state,
                    PackageManager.DONT_KILL_APP
                )
            } catch (e: Exception) {
                android.util.Log.e("SettingsManager", "Failed to update icon for $p", e)
            }
        }
    }

    fun getKeyboardPalette(context: Context): Palette =
        Palette.fromKey(prefs(context).getString(KEY_KEYBOARD_PALETTE, Palette.Black.name))

    fun setKeyboardPalette(context: Context, palette: Palette) {
        prefs(context).edit { putString(KEY_KEYBOARD_PALETTE, palette.name) }
    }

    fun getAvailableAodStyles(): List<AodStyle> {
        val topLevelStyles = listOf(
            AodStyle.TEXT,
            AodStyle.ANALOG,
            AodStyle.CONCENTRIC,
            AodStyle.SPINNER,
            AodStyle.BLOCKS,
            AodStyle.BARS,
            AodStyle.TRACK,
            AodStyle.DIGITAL_ABSTRACT,
            AodStyle.CUBED
        )
        return topLevelStyles.filter { fdInt >= it.minFd }
    }

    fun getAodStyle(context: Context): AodStyle {
        val styleName = prefs(context).getString(KEY_AOD_STYLE, AodStyle.TEXT.name) ?: ""
        val style = AodStyle.entries.find { it.name == styleName } ?: AodStyle.TEXT
        return when (style) {
            AodStyle.STACKED -> {
                setTextClockLayout(context, TextClockLayout.STACKED)
                setTextClockSubstyle(context, TextClockSubstyle.MODERN)
                setAodStyle(context, AodStyle.TEXT)
                AodStyle.TEXT
            }
            AodStyle.INLINE -> {
                setTextClockLayout(context, TextClockLayout.INLINE)
                setTextClockSubstyle(context, TextClockSubstyle.MODERN)
                setAodStyle(context, AodStyle.TEXT)
                AodStyle.TEXT
            }
            AodStyle.STACKED_DOT -> {
                setTextClockLayout(context, TextClockLayout.STACKED)
                setTextClockSubstyle(context, TextClockSubstyle.NOTHING)
                setAodStyle(context, AodStyle.TEXT)
                AodStyle.TEXT
            }
            AodStyle.INLINE_DOT -> {
                setTextClockLayout(context, TextClockLayout.INLINE)
                setTextClockSubstyle(context, TextClockSubstyle.NOTHING)
                setAodStyle(context, AodStyle.TEXT)
                AodStyle.TEXT
            }
            AodStyle.STACKED_DIGITAL -> {
                setTextClockLayout(context, TextClockLayout.STACKED)
                setTextClockSubstyle(context, TextClockSubstyle.DIGITAL)
                setAodStyle(context, AodStyle.TEXT)
                AodStyle.TEXT
            }
            AodStyle.INLINE_DIGITAL -> {
                setTextClockLayout(context, TextClockLayout.INLINE)
                setTextClockSubstyle(context, TextClockSubstyle.DIGITAL)
                setAodStyle(context, AodStyle.TEXT)
                AodStyle.TEXT
            }
            AodStyle.PIXEL_STACKED -> {
                setTextClockLayout(context, TextClockLayout.STACKED)
                setTextClockSubstyle(context, TextClockSubstyle.PIXEL)
                setAodStyle(context, AodStyle.TEXT)
                AodStyle.TEXT
            }
            AodStyle.PIXEL_INLINE -> {
                setTextClockLayout(context, TextClockLayout.INLINE)
                setTextClockSubstyle(context, TextClockSubstyle.PIXEL)
                setAodStyle(context, AodStyle.TEXT)
                AodStyle.TEXT
            }
            AodStyle.PLANETS -> {
                setAnalogClockSubstyle(context, AnalogClockSubstyle.PLANETS)
                setAodStyle(context, AodStyle.ANALOG)
                AodStyle.ANALOG
            }
            else -> if (fdInt >= style.minFd) style else AodStyle.TEXT
        }
    }

    fun setAodStyle(context: Context, style: AodStyle) {
        prefs(context).edit { putString(KEY_AOD_STYLE, style.name) }
    }

    fun getAnalogClockSubstyle(context: Context): AnalogClockSubstyle {
        val name = prefs(context).getString(KEY_ANALOG_CLOCK_SUBSTYLE, AnalogClockSubstyle.CLASSIC.name)
        return AnalogClockSubstyle.entries.find { it.name == name } ?: AnalogClockSubstyle.CLASSIC
    }

    fun setAnalogClockSubstyle(context: Context, substyle: AnalogClockSubstyle) {
        prefs(context).edit { putString(KEY_ANALOG_CLOCK_SUBSTYLE, substyle.name) }
    }

    fun getTextClockLayout(context: Context): TextClockLayout {
        val name = prefs(context).getString(KEY_TEXT_CLOCK_LAYOUT, TextClockLayout.STACKED.name)
        return TextClockLayout.entries.find { it.name == name } ?: TextClockLayout.STACKED
    }

    fun setTextClockLayout(context: Context, layout: TextClockLayout) {
        prefs(context).edit { putString(KEY_TEXT_CLOCK_LAYOUT, layout.name) }
    }

    fun getTextClockSubstyle(context: Context): TextClockSubstyle {
        val name = prefs(context).getString(KEY_TEXT_CLOCK_SUBSTYLE, TextClockSubstyle.MODERN.name)
        return TextClockSubstyle.entries.find { it.name == name } ?: TextClockSubstyle.MODERN
    }

    fun setTextClockSubstyle(context: Context, substyle: TextClockSubstyle) {
        prefs(context).edit { putString(KEY_TEXT_CLOCK_SUBSTYLE, substyle.name) }
    }

    fun getTextClockFontFamily(context: Context): String =
        prefs(context).getString(KEY_TEXT_CLOCK_FONT_FAMILY, "QUICKSAND") ?: "QUICKSAND"

    fun setTextClockFontFamily(context: Context, family: String) {
        prefs(context).edit { putString(KEY_TEXT_CLOCK_FONT_FAMILY, family) }
    }

    fun getTextClockFontWeight(context: Context): Int =
        prefs(context).getInt(KEY_TEXT_CLOCK_FONT_WEIGHT, 700)

    fun setTextClockFontWeight(context: Context, weight: Int) {
        prefs(context).edit { putInt(KEY_TEXT_CLOCK_FONT_WEIGHT, weight) }
    }

    fun isTextClockItalic(context: Context): Boolean =
        prefs(context).getBoolean(KEY_TEXT_CLOCK_FONT_ITALIC, false)

    fun setTextClockItalic(context: Context, italic: Boolean) {
        prefs(context).edit { putBoolean(KEY_TEXT_CLOCK_FONT_ITALIC, italic) }
    }

    fun isAodMediaEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AOD_MEDIA_ENABLED, true)

    fun setAodMediaEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_AOD_MEDIA_ENABLED, enabled) }
    }

    fun isAodLockEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AOD_LOCK_ENABLED, true)

    fun setAodLockEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_AOD_LOCK_ENABLED, enabled) }
    }

    fun shouldShowLiftToWakeWarning(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHOW_LIFT_TO_WAKE_WARNING, true)

    fun setShowLiftToWakeWarning(context: Context, show: Boolean) {
        prefs(context).edit { putBoolean(KEY_SHOW_LIFT_TO_WAKE_WARNING, show) }
    }

    fun isButtonOverrideEnabled(context: Context, keyCode: Int): Boolean {
        if (isBetterShiftKey(context, keyCode)) return true
        if (keyCode == COMMA_KEY) return true
        val prefs = prefs(context)
        val key = "btn_override_$keyCode"
        if (prefs.contains(key)) {
            return prefs.getBoolean(key, false)
        }
        val states = listOf("ON", "OFF")
        val types = listOf(
            "SINGLE_PRESS", "DOUBLE_PRESS", "TRIPLE_PRESS", "QUADRUPLE_PRESS", "QUINTUPLE_PRESS",
            "HOLD", "PRESS_AND_HOLD", "DOUBLE_TAP_AND_HOLD",
            "SINGLE", "DOUBLE", "TRIPLE", "LONG"
        )
        for (state in states) {
            for (type in types) {
                val action = prefs.getString("btn_${keyCode}_${state}_${type}", ACTION_DEFAULT)
                if (action != null && action != ACTION_DEFAULT) {
                    return true
                }
            }
        }
        return false
    }

    fun setButtonOverrideEnabled(context: Context, keyCode: Int, enabled: Boolean) {
        prefs(context).edit { putBoolean("btn_override_$keyCode", enabled) }
    }

    val keyboardKeyCodes: Set<Int> = (7..16).toSet() +   // Numbers 0-9
            (29..56).toSet() +                               // Letters a-z, comma, period
            (57..60).toSet() +                               // Alt, Shift
            (61..76).toSet() +                               // Tab, Space, Enter, Backspace, punctuation
            (92..93).toSet() +                               // Page Up, Page Down
            (111..118).toSet() +                             // Escape, Delete, Ctrl, Caps/Scroll Lock, Meta Left/Right
            (123..124).toSet() +                             // End, Insert
            (19..23).toSet() +                               // Arrow keys / Dpad
            // F5-F12 only. F1-F4 (131-134) collide with the dedicated AI/Camera Up/Camera Down/Focus
            // hardware buttons on some devices, so they must NOT be treated as keyboard keys.
            (135..142).toSet()

    fun isKeyboardKey(keyCode: Int): Boolean = keyCode in keyboardKeyCodes

    fun isIgnoreKeyboardActive(context: Context, keyCode: Int): Boolean {
        return isKeyboardOverrideLocked(context, keyCode) ||
                prefs(context).getBoolean("ignore_kbd_active_$keyCode", keyCode == COMMA_KEY)
    }

    fun setIgnoreKeyboardActive(context: Context, keyCode: Int, enabled: Boolean) {
        prefs(context).edit { putBoolean("ignore_kbd_active_$keyCode", enabled) }
    }
}