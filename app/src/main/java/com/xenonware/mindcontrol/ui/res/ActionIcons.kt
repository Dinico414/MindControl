package com.xenonware.mindcontrol.ui.res

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.automirrored.rounded.Shortcut
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ArrowCircleDown
import androidx.compose.material.icons.rounded.ArrowCircleUp
import androidx.compose.material.icons.rounded.Assistant
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.FilterCenterFocus
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowDown
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowUp
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.WatchLater
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.xenonware.mindcontrol.R
import com.xenonware.mindcontrol.SettingsManager

val SHELL_REQUIRED_ACTIONS = setOf(
    SettingsManager.ACTION_WIFI_TOGGLE,
    SettingsManager.ACTION_BLUETOOTH_TOGGLE,
    SettingsManager.ACTION_DATA_TOGGLE,
    SettingsManager.ACTION_NFC_TOGGLE,
    SettingsManager.ACTION_LOCATION_TOGGLE,
    SettingsManager.ACTION_AUTO_BRIGHTNESS_TOGGLE,
    SettingsManager.ACTION_SHOW_MENU,
)

fun isActionDisabled(action: String, shellReady: Boolean): Boolean {
    return !shellReady && action in SHELL_REQUIRED_ACTIONS
}

fun disabledReasonFor(action: String, shellReady: Boolean): Int? = when {
    !shellReady && action in SHELL_REQUIRED_ACTIONS -> R.string.requires_shell
    else -> null
}

@Composable
fun getActionDisplayName(action: String): String {
    val resId = when (action) {
        SettingsManager.ACTION_NONE -> R.string.action_none
        SettingsManager.ACTION_DEFAULT -> R.string.action_default
        SettingsManager.ACTION_PLAY_PAUSE -> R.string.action_play_pause
        SettingsManager.ACTION_NEXT -> R.string.action_next
        SettingsManager.ACTION_PREVIOUS -> R.string.action_previous
        SettingsManager.ACTION_VOLUME_UP -> R.string.action_volume_up
        SettingsManager.ACTION_VOLUME_DOWN -> R.string.action_volume_down
        SettingsManager.ACTION_FLASHLIGHT -> R.string.action_flashlight
        SettingsManager.ACTION_SCREENSHOT -> R.string.action_screenshot
        SettingsManager.ACTION_LOCK -> R.string.action_lock
        SettingsManager.ACTION_AOD -> R.string.action_aod
        SettingsManager.ACTION_BRIGHTNESS_UP -> R.string.action_brightness_up
        SettingsManager.ACTION_BRIGHTNESS_DOWN -> R.string.action_brightness_down
        SettingsManager.ACTION_HOME -> R.string.action_home
        SettingsManager.ACTION_BACK -> R.string.action_back
        SettingsManager.ACTION_RECENTS -> R.string.action_recents
        SettingsManager.ACTION_NOTIFICATIONS -> R.string.action_notifications
        SettingsManager.ACTION_QUICK_SETTINGS -> R.string.action_quick_settings
        SettingsManager.ACTION_ASSISTANT -> R.string.action_assistant
        SettingsManager.ACTION_ROTATE_TOGGLE -> R.string.action_rotate_toggle
        SettingsManager.ACTION_SCROLL_UP -> R.string.action_scroll_up
        SettingsManager.ACTION_SCROLL_DOWN -> R.string.action_scroll_down
        SettingsManager.ACTION_SCROLL_UP_SMOOTH -> R.string.action_scroll_up_smooth
        SettingsManager.ACTION_SCROLL_DOWN_SMOOTH -> R.string.action_scroll_down_smooth
        SettingsManager.ACTION_SCROLL_UP_SMOOTH_FAST -> R.string.action_scroll_up_smooth_fast
        SettingsManager.ACTION_SCROLL_DOWN_SMOOTH_FAST -> R.string.action_scroll_down_smooth_fast
        SettingsManager.ACTION_SHOW_MENU -> R.string.action_show_menu
        SettingsManager.ACTION_LAST_APP -> R.string.action_last_app
        SettingsManager.ACTION_APP_INFO -> R.string.action_app_info
        SettingsManager.ACTION_POWER_DIALOG -> R.string.action_power_dialog
        SettingsManager.ACTION_GOOGLE_SEARCH -> R.string.action_google_search
        SettingsManager.ACTION_COPY -> R.string.action_copy
        SettingsManager.ACTION_CUT -> R.string.action_cut
        SettingsManager.ACTION_PASTE -> R.string.action_paste
        SettingsManager.ACTION_FAST_FORWARD -> R.string.action_fast_forward
        SettingsManager.ACTION_REWIND -> R.string.action_rewind
        SettingsManager.ACTION_STOP -> R.string.action_stop
        SettingsManager.ACTION_STEP_FORWARD -> R.string.action_step_forward
        SettingsManager.ACTION_STEP_BACKWARD -> R.string.action_step_backward
        SettingsManager.ACTION_VIBRATE_RINGER -> R.string.action_vibrate_ringer
        SettingsManager.ACTION_CYCLE_SOUND_MODE -> R.string.action_cycle_sound_mode
        SettingsManager.ACTION_DND -> R.string.action_dnd
        SettingsManager.ACTION_AUTO_BRIGHTNESS_TOGGLE -> R.string.action_auto_brightness_toggle
        SettingsManager.ACTION_WIFI_TOGGLE -> R.string.action_wifi_toggle
        SettingsManager.ACTION_BLUETOOTH_TOGGLE -> R.string.action_bluetooth_toggle
        SettingsManager.ACTION_DATA_TOGGLE -> R.string.action_data_toggle
        SettingsManager.ACTION_NFC_TOGGLE -> R.string.action_nfc_toggle
        SettingsManager.ACTION_LOCATION_TOGGLE -> R.string.action_location_toggle
        SettingsManager.ACTION_AUTOROTATE_TOGGLE -> R.string.action_autorotate_toggle
        SettingsManager.ACTION_ROTATE_360 -> R.string.action_rotate_360
        SettingsManager.ACTION_MUTE_VOL -> R.string.action_mute_vol
        SettingsManager.ACTION_MUTE_MIC_TOGGLE -> R.string.action_mute_mic_toggle
        SettingsManager.ACTION_VOLUME_DIALOG -> R.string.action_volume_dialog
        else -> null
    }
    return if (resId != null) stringResource(resId) else action.split("_").joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

@Composable
fun getTypeDisplayName(type: String): String {
    val resId = when (type) {
        "SINGLE_PRESS" -> R.string.press_single
        "DOUBLE_PRESS" -> R.string.press_double
        "TRIPLE_PRESS" -> R.string.press_triple
        "HOLD" -> R.string.press_hold
        "PRESS_AND_HOLD" -> R.string.press_and_hold
        else -> null
    }
    return if (resId != null) stringResource(resId) else type.split("_").joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

@Composable
fun ActionIcon(action: String, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    val context = LocalContext.current
    val icon: Any = when {
        action.startsWith(SettingsManager.PREFIX_APP) -> {
            val pkg = action.removePrefix(SettingsManager.PREFIX_APP)
            remember(pkg) {
                try {
                    context.packageManager.getApplicationIcon(pkg)
                } catch (_: Exception) {
                    Icons.Rounded.Apps
                }
            }
        }
        action.startsWith(SettingsManager.PREFIX_SHORTCUT) -> Icons.AutoMirrored.Rounded.Shortcut
        action.startsWith(SettingsManager.PREFIX_SPEED_DIAL) || action == SettingsManager.ACTION_SPEED_DIAL -> Icons.Rounded.Phone
        action.startsWith(SettingsManager.PREFIX_URL) || action == SettingsManager.ACTION_URL -> Icons.Rounded.Language
        action.startsWith(SettingsManager.PREFIX_QR_CODE) || action == SettingsManager.ACTION_QR_CODE -> Icons.Rounded.QrCode
        action == SettingsManager.ACTION_NONE -> Icons.Rounded.Block
        action == SettingsManager.ACTION_DEFAULT -> Icons.Rounded.SettingsBackupRestore
        action == SettingsManager.ACTION_HOME -> Icons.Rounded.Home
        action == SettingsManager.ACTION_BACK -> Icons.AutoMirrored.Rounded.ArrowBack
        action == SettingsManager.ACTION_RECENTS -> Icons.Rounded.History
        action == SettingsManager.ACTION_SHOW_MENU -> Icons.Rounded.Menu
        action == SettingsManager.ACTION_LOCK -> Icons.Rounded.Lock
        action == SettingsManager.ACTION_AOD -> Icons.Rounded.WatchLater
        action == SettingsManager.ACTION_LOCK_AOD -> Icons.Rounded.Lock
        action == SettingsManager.ACTION_LOCK_MEDIA_AOD -> Icons.Rounded.WatchLater
        action == SettingsManager.ACTION_FLASHLIGHT -> Icons.Rounded.FlashlightOn
        action == SettingsManager.ACTION_SCREENSHOT -> Icons.Rounded.Screenshot
        action == SettingsManager.ACTION_QUICK_SETTINGS -> Icons.Rounded.Settings
        action == SettingsManager.ACTION_LAST_APP -> Icons.Rounded.Repeat
        action == SettingsManager.ACTION_APP_INFO -> Icons.Rounded.Info
        action == SettingsManager.ACTION_POWER_DIALOG -> Icons.Rounded.PowerSettingsNew
        action == SettingsManager.ACTION_GOOGLE_SEARCH -> Icons.Rounded.Search
        action == SettingsManager.ACTION_ASSISTANT -> Icons.Rounded.Assistant
        action == SettingsManager.ACTION_SCROLL_UP -> Icons.Rounded.ArrowCircleUp
        action == SettingsManager.ACTION_SCROLL_DOWN -> Icons.Rounded.ArrowCircleDown
        action == SettingsManager.ACTION_SCROLL_UP_SMOOTH -> Icons.Rounded.KeyboardArrowUp
        action == SettingsManager.ACTION_SCROLL_DOWN_SMOOTH -> Icons.Rounded.KeyboardArrowDown
        action == SettingsManager.ACTION_SCROLL_UP_SMOOTH_FAST -> Icons.Rounded.KeyboardDoubleArrowUp
        action == SettingsManager.ACTION_SCROLL_DOWN_SMOOTH_FAST -> Icons.Rounded.KeyboardDoubleArrowDown
        action == SettingsManager.ACTION_COPY -> Icons.Rounded.ContentCopy
        action == SettingsManager.ACTION_CUT -> Icons.Rounded.ContentCut
        action == SettingsManager.ACTION_PASTE -> Icons.Rounded.ContentPaste
        action == SettingsManager.ACTION_VIBRATE_RINGER -> Icons.Rounded.Vibration
        action == SettingsManager.ACTION_DND -> Icons.Rounded.DoNotDisturbOn
        action == SettingsManager.ACTION_NOTIFICATIONS -> Icons.Rounded.Notifications
        action == SettingsManager.ACTION_BRIGHTNESS_UP -> Icons.Rounded.BrightnessHigh
        action == SettingsManager.ACTION_BRIGHTNESS_DOWN -> Icons.Rounded.BrightnessLow
        action == SettingsManager.ACTION_AUTO_BRIGHTNESS_TOGGLE -> Icons.Rounded.BrightnessAuto
        action == SettingsManager.ACTION_WIFI_TOGGLE -> Icons.Rounded.Wifi
        action == SettingsManager.ACTION_BLUETOOTH_TOGGLE -> Icons.Rounded.Bluetooth
        action == SettingsManager.ACTION_DATA_TOGGLE -> Icons.Rounded.DataUsage
        action == SettingsManager.ACTION_NFC_TOGGLE -> Icons.Rounded.Nfc
        action == SettingsManager.ACTION_LOCATION_TOGGLE -> Icons.Rounded.LocationOn
        action == SettingsManager.ACTION_ROTATE_TOGGLE -> Icons.Rounded.ScreenRotation
        action == SettingsManager.ACTION_ROTATE_360 -> Icons.AutoMirrored.Rounded.RotateRight
        action == SettingsManager.ACTION_AUTOROTATE_TOGGLE -> Icons.Rounded.ScreenRotation
        action == SettingsManager.ACTION_VOLUME_UP -> Icons.AutoMirrored.Rounded.VolumeUp
        action == SettingsManager.ACTION_VOLUME_DOWN -> Icons.AutoMirrored.Rounded.VolumeDown
        action == SettingsManager.ACTION_MUTE_VOL -> Icons.AutoMirrored.Rounded.VolumeOff
        action == SettingsManager.ACTION_VOLUME_DIALOG -> Icons.Rounded.Tune
        action == SettingsManager.ACTION_MUTE_MIC_TOGGLE -> Icons.Rounded.MicOff
        action == SettingsManager.ACTION_PREVIOUS -> Icons.Rounded.SkipPrevious
        action == SettingsManager.ACTION_NEXT -> Icons.Rounded.SkipNext
        action == SettingsManager.ACTION_PLAY_PAUSE -> Icons.Rounded.PlayArrow
        action == SettingsManager.ACTION_STOP -> Icons.Rounded.Stop
        action == SettingsManager.ACTION_FAST_FORWARD -> Icons.Rounded.FastForward
        action == SettingsManager.ACTION_REWIND -> Icons.Rounded.FastRewind
        action == SettingsManager.ACTION_STEP_FORWARD -> Icons.Rounded.Forward10
        action == SettingsManager.ACTION_STEP_BACKWARD -> Icons.Rounded.Replay10
        action == SettingsManager.ACTION_CYCLE_SOUND_MODE -> Icons.Rounded.Vibration
        else -> Icons.Rounded.QuestionMark
    }

    when (icon) {
        is ImageVector -> Icon(imageVector = icon, contentDescription = null, modifier = modifier, tint = tint)
        is Drawable -> {
            val bitmap = remember(icon) { icon.toBitmap().asImageBitmap() }
            Image(bitmap = bitmap, contentDescription = null, modifier = modifier)
        }
    }
}