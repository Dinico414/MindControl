package com.xenonware.mindcontrol

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.core.content.edit
import com.xenonware.mindcontrol.ui.theme.Palette
import com.xenonware.mindcontrol.BuildConfig

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

    const val ACTION_MUTE_VOL = "MUTE_VOL"
    const val ACTION_MUTE_MIC_TOGGLE = "MUTE_MIC_TOGGLE"
    const val ACTION_VOLUME_DIALOG = "VOLUME_DIALOG"

    const val PREFIX_APP = "APP:"
    const val PREFIX_SHORTCUT = "SHORTCUT:"
    const val PREFIX_SPEED_DIAL = "SPEED_DIAL:"
    const val PREFIX_URL = "URL:"
    const val PREFIX_QR_CODE = "QR_CODE:"

    private const val KEY_DISABLE_IN_CAMERA = "disable_in_camera"
    private const val KEY_DEFAULT_WHEN_VOLUME_VISIBLE = "default_when_volume_visible"
    private const val KEY_OVERRIDE_SCREEN_OFF = "override_screen_off"
    private const val KEY_VOLUME_LONG_PRESS_SKIP = "volume_long_press_skip"
    private const val KEY_DEVICE_PALETTE = "device_palette"
    private const val KEY_KEYBOARD_PALETTE = "keyboard_palette"
    private const val KEY_AOD_STYLE = "aod_style"
    private const val KEY_AOD_MEDIA_ENABLED = "aod_media_enabled"
    private const val KEY_SHOW_LIFT_TO_WAKE_WARNING = "show_lift_to_wake_warning"

    private const val FD = BuildConfig.FEATURE_DROP
    private val fdInt = FD.toIntOrNull() ?: 0
    enum class AodStyle(val minFd: Int = 0) {
        CONCENTRIC,
        ANALOG,
        PLANETS(1),
        SPINNER(1),
        PIXEL_STACKED(1),
        PIXEL_INLINE(1),
        STACKED,
        INLINE,
        STACKED_DOT,
        INLINE_DOT,
        STACKED_DIGITAL,
        INLINE_DIGITAL
    }

    private fun prefs(context: Context): SharedPreferences {
        val deviceProtectedContext = context.createDeviceProtectedStorageContext()
        
        // Migrate existing preferences if necessary
        deviceProtectedContext.moveSharedPreferencesFrom(context, PREFS_NAME)
        
        return deviceProtectedContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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
                // Da wir im Manifest ${applicationId}.Alias nutzen, 
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

    fun getAvailableAodStyles(): List<AodStyle> =
        AodStyle.entries.filter { fdInt >= it.minFd }

    fun getAodStyle(context: Context): AodStyle {
        val styleName = prefs(context).getString(KEY_AOD_STYLE, AodStyle.CONCENTRIC.name) ?: ""
        val style = AodStyle.entries.find { it.name == styleName } ?: AodStyle.CONCENTRIC
        return if (fdInt >= style.minFd) style else AodStyle.CONCENTRIC
    }

    fun setAodStyle(context: Context, style: AodStyle) {
        prefs(context).edit { putString(KEY_AOD_STYLE, style.name) }
    }

    fun isAodMediaEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AOD_MEDIA_ENABLED, true)

    fun setAodMediaEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_AOD_MEDIA_ENABLED, enabled) }
    }

    fun shouldShowLiftToWakeWarning(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHOW_LIFT_TO_WAKE_WARNING, true)

    fun setShowLiftToWakeWarning(context: Context, show: Boolean) {
        prefs(context).edit { putBoolean(KEY_SHOW_LIFT_TO_WAKE_WARNING, show) }
    }
}
