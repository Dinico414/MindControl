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

    val ARROW_ACTIONS = setOf(ACTION_ARROW_UP, ACTION_ARROW_DOWN, ACTION_ARROW_LEFT, ACTION_ARROW_RIGHT)

    /** Key name (understood by [KeyInput.resolveKeyCode]) for an arrow action, or null. */
    fun arrowKeyName(action: String): String? = when (action) {
        ACTION_ARROW_UP -> "Up"
        ACTION_ARROW_DOWN -> "Down"
        ACTION_ARROW_LEFT -> "Left"
        ACTION_ARROW_RIGHT -> "Right"
        else -> null
    }

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

    /** Press types a Shift key offers while Better Shift is on. */
    val BETTER_SHIFT_PRESS_TYPES = listOf("SINGLE_PRESS", "HOLD")

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

    /** For the button screen: which press types to list for this key. */
    fun pressTypesFor(context: Context, keyCode: Int, allTypes: List<String>): List<String> =
        if (isBetterShiftKey(context, keyCode)) allTypes.filter { it in BETTER_SHIFT_PRESS_TYPES } else allTypes

    /** For the button screen: the "override while keyboard is active" switch is forced on and disabled. */
    fun isKeyboardOverrideLocked(context: Context, keyCode: Int): Boolean = isBetterShiftKey(context, keyCode)

    /** For the button screen: with Better Shift on, the Shift press types are shown but can't be changed. */
    fun isPressTypeLocked(context: Context, keyCode: Int): Boolean = isBetterShiftKey(context, keyCode)

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
        if (oldType != null) {
            return prefs.getString("btn_${keyCode}_${state}_${oldType}", ACTION_DEFAULT) ?: ACTION_DEFAULT
        }

        return ACTION_DEFAULT
    }

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
            (111..116).toSet() +                             // Escape, Delete, Ctrl, Caps/Scroll Lock
            (123..124).toSet() +                             // End, Insert
            (19..23).toSet() +                               // Arrow keys / Dpad
            (131..142).toSet()                               // F1-F12

    fun isKeyboardKey(keyCode: Int): Boolean = keyCode in keyboardKeyCodes

    fun isIgnoreKeyboardActive(context: Context, keyCode: Int): Boolean {
        if (isKeyboardOverrideLocked(context, keyCode)) return true
        return prefs(context).getBoolean("ignore_kbd_active_$keyCode", false)
    }

    fun setIgnoreKeyboardActive(context: Context, keyCode: Int, enabled: Boolean) {
        prefs(context).edit { putBoolean("ignore_kbd_active_$keyCode", enabled) }
    }
}