@file:Suppress("AssignedValueIsNeverRead")

package com.xenonware.mindcontrol

import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.automirrored.rounded.Shortcut
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.RemoveCircle
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
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowDown
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowRight
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import com.xenon.mylibrary.res.XenonDialog
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenon.mylibrary.values.MediumCornerRadius
import com.xenon.mylibrary.values.SmallestCornerRadius
import com.xenonware.mindcontrol.ui.AnalogAodStyle
import com.xenonware.mindcontrol.ui.ConcentricAodStyle
import com.xenonware.mindcontrol.ui.InlineAodStyle
import com.xenonware.mindcontrol.ui.InlineDigitalAodStyle
import com.xenonware.mindcontrol.ui.InlineDotAodStyle
import com.xenonware.mindcontrol.ui.StackedAodStyle
import com.xenonware.mindcontrol.ui.StackedDigitalAodStyle
import com.xenonware.mindcontrol.ui.StackedDotAodStyle
import com.xenonware.mindcontrol.ui.PlanetsAodStyle
import com.xenonware.mindcontrol.ui.SpinnerAodStyle
import com.xenonware.mindcontrol.ui.PixelStackedAodStyle
import com.xenonware.mindcontrol.ui.PixelInlineAodStyle
import com.xenonware.mindcontrol.ui.theme.BlueTheme
import com.xenonware.mindcontrol.ui.theme.GreenTheme
import com.xenonware.mindcontrol.ui.theme.Palette
import com.xenonware.mindcontrol.ui.theme.PaletteRow
import com.xenonware.mindcontrol.ui.theme.PaletteTheme
import com.xenonware.mindcontrol.ui.theme.RedTheme
import com.xenonware.mindcontrol.ui.theme.YellowTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val context = LocalContext.current
            var devicePalette by rememberSaveable { mutableStateOf(SettingsManager.getDevicePalette(context)) }
            var keyboardPalette by rememberSaveable {
                mutableStateOf(
                    SettingsManager.getKeyboardPalette(
                        context
                    )
                )
            }

            PaletteTheme(palette = devicePalette) {
                Surface(color = Color.Black) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        MindControlMainScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(WindowInsets.safeDrawing.asPaddingValues()),
                            devicePalette = devicePalette,
                            keyboardPalette = keyboardPalette,
                            onDevicePaletteChange = {
                                devicePalette = it
                                SettingsManager.setDevicePalette(context, it)
                            },
                            onKeyboardPaletteChange = {
                                keyboardPalette = it
                                SettingsManager.setKeyboardPalette(context, it)
                            },
                        )
                    }
                }
            }
        }
    }
}

data class ActionConfig(val keyCode: Int, val state: String, val type: String)

val ActionConfigSaver = listSaver<ActionConfig?, Any>(
    save = { if (it == null) emptyList() else listOf(it.keyCode, it.state, it.type) },
    restore = { if (it.isEmpty()) null else ActionConfig(it[0] as Int, it[1] as String, it[2] as String) }
)

val PairSaver = listSaver<Pair<Int, String>?, Any>(
    save = { if (it == null) emptyList() else listOf(it.first, it.second) },
    restore = { if (it.isEmpty()) null else (it[0] as Int) to (it[1] as String) }
)

// ---- Action availability helpers ----------------------------------------------------------------

private val SHIZUKU_REQUIRED_ACTIONS = setOf(
    SettingsManager.ACTION_WIFI_TOGGLE,
    SettingsManager.ACTION_BLUETOOTH_TOGGLE,
    SettingsManager.ACTION_DATA_TOGGLE,
    SettingsManager.ACTION_NFC_TOGGLE,
    SettingsManager.ACTION_LOCATION_TOGGLE,
    SettingsManager.ACTION_AUTO_BRIGHTNESS_TOGGLE,
    SettingsManager.ACTION_SHOW_MENU,
)


private fun isActionDisabled(action: String, shizukuReady: Boolean): Boolean {
    return !shizukuReady && action in SHIZUKU_REQUIRED_ACTIONS
}

private fun disabledReasonFor(action: String, shizukuReady: Boolean): Int? = when {
    !shizukuReady && action in SHIZUKU_REQUIRED_ACTIONS -> R.string.requires_shizuku
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

// -------------------------------------------------------------------------------------------------

@Composable
fun MindControlMainScreen(
    modifier: Modifier = Modifier,
    devicePalette: Palette,
    keyboardPalette: Palette,
    onDevicePaletteChange: (Palette) -> Unit,
    onKeyboardPaletteChange: (Palette) -> Unit,
) {
    val context = LocalContext.current
    var selectedButton by rememberSaveable(stateSaver = PairSaver) { mutableStateOf(null) }
    var showKeyboard by rememberSaveable { mutableStateOf(false) }
    var configFromKeyboard by rememberSaveable { mutableStateOf(false) }
    var actionSelectionConfig by rememberSaveable(stateSaver = ActionConfigSaver) { mutableStateOf(null) }
    var isScreenOff by rememberSaveable { mutableStateOf(false) }

    var shizukuPermission by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        while (true) {
            shizukuPermission = try {
                Shizuku.pingBinder() &&
                        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } catch (_: Exception) {
                false
            }
            kotlinx.coroutines.delay(2000)
        }
    }

    val state = when {
        actionSelectionConfig != null -> "action_selection"
        selectedButton != null -> "config"
        showKeyboard -> "keyboard"
        else -> "grid"
    }

    val view = LocalView.current
    val isDark = isSystemInDarkTheme()

    SideEffect {
        val window = (context as ComponentActivity).window
        val insetsController = WindowCompat.getInsetsController(window, view)

        if (state == "grid" || state == "keyboard") {
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        } else {
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
    }

    PredictiveBackHandler(enabled = state != "grid") { progress ->
        try {
            progress.collect { }
            when (state) {
                "action_selection" -> actionSelectionConfig = null
                "config" -> {
                    val cameFromKeyboard = configFromKeyboard
                    selectedButton = null
                    isScreenOff = false
                    configFromKeyboard = false
                    if (!cameFromKeyboard) showKeyboard = false
                }
                "keyboard" -> showKeyboard = false
            }
        } catch (_: Exception) {
            // Cancelled
        }
    }

    AnimatedContent(
        targetState = state, transitionSpec = {
            when {
                // Keyboard transitions (Vertical)
                targetState == "keyboard" && initialState == "grid" -> {
                    slideInVertically { it } + fadeIn() togetherWith slideOutVertically { -it / 3 } + fadeOut()
                }
                initialState == "keyboard" && targetState == "grid" -> {
                    slideInVertically { -it / 3 } + fadeIn() togetherWith slideOutVertically { it } + fadeOut()
                }

                // Horizontal Transitions (Config & Action Selection)
                // Forward: Grid -> Config, Config -> ActionSelection, Keyboard -> Config
                (targetState == "config" && (initialState == "grid" || initialState == "keyboard")) ||
                        (targetState == "action_selection" && initialState == "config") -> {
                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it / 3 } + fadeOut()
                }

                // Backward: ActionSelection -> Config, Config -> Grid, Config -> Keyboard
                (initialState == "action_selection" && targetState == "config") ||
                        (initialState == "config" && (targetState == "grid" || targetState == "keyboard")) -> {
                    slideInHorizontally { -it / 3 } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                }

                else -> fadeIn() togetherWith fadeOut()
            }
        }, label = "ScreenTransition"
    ) { s ->
        when (s) {
            "action_selection" -> {
                val config = actionSelectionConfig
                if (config != null) {
                    val themeWrapper: @Composable (@Composable () -> Unit) -> Unit = when {
                        configFromKeyboard || config.keyCode == 111 -> { content -> PaletteTheme(palette = keyboardPalette) { content() } }
                        config.keyCode == 131 -> { content -> RedTheme { content() } }
                        config.keyCode == 133 || config.keyCode == 132 -> { content -> YellowTheme { content() } }
                        config.keyCode == 24 || config.keyCode == 25 -> { content -> GreenTheme { content() } }
                        config.keyCode == 27 || config.keyCode == 134 -> { content -> BlueTheme { content() } }
                        else -> { content -> content() }
                    }
                    themeWrapper {
                        ActionSelectionScreen(
                            config = config,
                            onBack = { actionSelectionConfig = null },
                            onActionSelected = { _ ->
                                actionSelectionConfig = null
                            },
                            modifier = modifier
                        )
                    }
                }
            }

            "keyboard" -> PaletteTheme(palette = keyboardPalette) {
                CustomKeyboardScreen(
                    modifier = modifier,
                    devicePalette = devicePalette,
                    onBack = { showKeyboard = false },
                    onKeySelected = { code, name ->
                        configFromKeyboard = true
                        selectedButton = code to name
                    })
            }

            "config" -> {
                val button = selectedButton
                if (button != null) {
                    ButtonConfigScreen(
                        keyCode = button.first,
                        name = button.second,
                        modifier = modifier,
                        keyboardPalette = keyboardPalette,
                        isFromKeyboard = configFromKeyboard,
                        isScreenOff = isScreenOff,
                        shizukuPermission = shizukuPermission,
                        onScreenOffChange = { isScreenOff = it },
                        onBack = {
                            val cameFromKeyboard = configFromKeyboard
                            selectedButton = null
                            isScreenOff = false
                            configFromKeyboard = false
                            if (!cameFromKeyboard) showKeyboard = false
                        },
                        onSelectAction = { keyCode, stateStr, type ->
                            actionSelectionConfig = ActionConfig(keyCode, stateStr, type)
                        }
                    )
                } else {
                    Box(modifier.fillMaxSize())
                }
            }

            else -> GridScreen(
                modifier = modifier,
                devicePalette = devicePalette,
                keyboardPalette = keyboardPalette,
                onDevicePaletteChange = onDevicePaletteChange,
                onKeyboardPaletteChange = onKeyboardPaletteChange,
                onButtonSelected = { code, name ->
                    if (code == 111) showKeyboard = true
                    else selectedButton = code to name
                })
        }
    }
}

@Composable
fun GridScreen(
    modifier: Modifier = Modifier,
    devicePalette: Palette,
    keyboardPalette: Palette,
    onDevicePaletteChange: (Palette) -> Unit,
    onKeyboardPaletteChange: (Palette) -> Unit,
    onButtonSelected: (Int, String) -> Unit,
) {
    val pressedKeys by ButtonState.pressedKeys.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        // Top Part (Weight 2f)
        Row(
            modifier = Modifier
                .weight(2f)
                .fillMaxWidth()
        ) {
            // AI Button
            RedTheme {
                Surface(
                    onClick = { onButtonSelected(131, "AI Button") },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(end = 4.dp, bottom = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (pressedKeys.contains(131)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier.weight(0.333f), contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.stars),
                                contentDescription = null,
                                tint = if (pressedKeys.contains(131)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Box(
                            modifier = Modifier.weight(0.667f), contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.ai_button),
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                                fontFamily = QuicksandTitleVariable,
                                color = if (pressedKeys.contains(131)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Right Top Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                YellowTheme {
                    // Camera Up
                    Surface(
                        onClick = { onButtonSelected(133, "Camera Up") },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(start = 4.dp, bottom = 1.dp),
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = 4.dp,
                            bottomEnd = 4.dp
                        ),
                        color = if (pressedKeys.contains(133)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier.weight(0.333f),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowUp,
                                    contentDescription = null,
                                    tint = if (pressedKeys.contains(133)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Box(
                                modifier = Modifier.weight(0.667f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.camera_up),
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                    color = if (pressedKeys.contains(133)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontFamily = QuicksandTitleVariable
                                )
                            }
                        }
                    }
                    // Camera Down
                    Surface(
                        onClick = { onButtonSelected(132, "Camera Down") },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 1.dp, bottom = 4.dp),
                        shape = RoundedCornerShape(
                            topStart = 4.dp,
                            topEnd = 4.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp
                        ),
                        color = if (pressedKeys.contains(132)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier.weight(0.333f),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = if (pressedKeys.contains(132)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Box(
                                modifier = Modifier.weight(0.667f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.camera_down),
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                    color = if (pressedKeys.contains(132)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontFamily = QuicksandTitleVariable
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Part (Weight 4f)
        Row(
            modifier = Modifier
                .weight(4f)
                .fillMaxWidth()
        ) {
            val configuration = LocalConfiguration.current
            val hasKeyboard = configuration.keyboard != Configuration.KEYBOARD_NOKEYS

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                TogglesContainer(
                    modifier = Modifier
                        .weight(if (hasKeyboard) 3f else 4f)
                        .fillMaxWidth(),
                    hasKeyboard = hasKeyboard,
                    devicePalette = devicePalette,
                    keyboardPalette = keyboardPalette,
                    onDevicePaletteChange = onDevicePaletteChange,
                    onKeyboardPaletteChange = onKeyboardPaletteChange,
                )

                if (hasKeyboard) {
                    PaletteTheme(palette = keyboardPalette) {
                        Surface(
                            onClick = { onButtonSelected(111, "Keyboard Button") },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(end = 4.dp, top = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = if (pressedKeys.contains(111)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier.weight(0.333f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Keyboard,
                                        contentDescription = null,
                                        // Now uses the onPrimary color from keyboardPalette
                                        tint = if (pressedKeys.contains(111)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Box(
                                    modifier = Modifier.weight(0.667f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.keyboard),
                                        style = MaterialTheme.typography.titleMedium,
                                        textAlign = TextAlign.Center,
                                        color = if (pressedKeys.contains(111)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontFamily = QuicksandTitleVariable
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Right Bottom Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                GreenTheme {
                    // Volume Up
                    Surface(
                        onClick = { onButtonSelected(24, "Volume Up") },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 4.dp, bottom = 1.dp),
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = 4.dp,
                            bottomEnd = 4.dp
                        ),
                        color = if (pressedKeys.contains(24)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier.weight(0.333f),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AddCircle,
                                    contentDescription = null,
                                    tint = if (pressedKeys.contains(24)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Box(
                                modifier = Modifier.weight(0.667f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.volume_up),
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                    color = if (pressedKeys.contains(24)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontFamily = QuicksandTitleVariable
                                )
                            }
                        }
                    }
                    // Volume Down
                    Surface(
                        onClick = { onButtonSelected(25, "Volume Down") },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 1.dp, bottom = 4.dp),
                        shape = RoundedCornerShape(
                            topStart = 4.dp,
                            topEnd = 4.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp
                        ),
                        color = if (pressedKeys.contains(25)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier.weight(0.333f),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.RemoveCircle,
                                    contentDescription = null,
                                    tint = if (pressedKeys.contains(25)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Box(
                                modifier = Modifier.weight(0.667f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.volume_down),
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                    color = if (pressedKeys.contains(25)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontFamily = QuicksandTitleVariable
                                )
                            }
                        }
                    }
                }
                // Camera + Focus row
                Row(
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxWidth()
                ) {
                    BlueTheme {
                        // Camera Button
                        Surface(
                            onClick = { onButtonSelected(27, "Camera Button") },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(start = 4.dp, end = 1.dp, top = 4.dp),
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 4.dp,
                                bottomStart = 16.dp,
                                bottomEnd = 4.dp
                            ),
                            color = if (pressedKeys.contains(27)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CameraAlt,
                                    contentDescription = null,
                                    tint = if (pressedKeys.contains(27)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.camera_button),
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                    color = if (pressedKeys.contains(27)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontFamily = QuicksandTitleVariable
                                )
                            }
                        }
                        // Focus Button
                        Surface(
                            onClick = { onButtonSelected(134, "Focus Button") },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(start = 1.dp, top = 4.dp),
                            shape = RoundedCornerShape(
                                topStart = 4.dp,
                                topEnd = 16.dp,
                                bottomStart = 4.dp,
                                bottomEnd = 16.dp
                            ),
                            color = if (pressedKeys.contains(134)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FilterCenterFocus,
                                    contentDescription = null,
                                    tint = if (pressedKeys.contains(134)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.focus_button),
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                    color = if (pressedKeys.contains(134)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontFamily = QuicksandTitleVariable
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("BatteryLife")
@Composable
fun TogglesContainer(
    modifier: Modifier = Modifier,
    hasKeyboard: Boolean = false,
    devicePalette: Palette,
    keyboardPalette: Palette,
    onDevicePaletteChange: (Palette) -> Unit,
    onKeyboardPaletteChange: (Palette) -> Unit,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val accessibilityManager =
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    var isServiceEnabled by remember {
        mutableStateOf(
            accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
                .any { it.resolveInfo.serviceInfo.packageName == context.packageName })
    }

    var disableInCamera by remember { mutableStateOf(SettingsManager.isDisableInCamera(context)) }
    var defaultWhenVolumeVisible by remember {
        mutableStateOf(
            SettingsManager.isDefaultWhenVolumeVisible(
                context
            )
        )
    }
    var overrideScreenOff by remember {
        mutableStateOf(
            SettingsManager.isOverrideScreenOffEnabled(
                context
            )
        )
    }
    var volumeLongPressSkip by remember {
        mutableStateOf(
            SettingsManager.isVolumeLongPressSkipEnabled(
                context
            )
        )
    }

    var shizukuPermission by remember { mutableStateOf(false) }
    var shizukuAvailable by remember { mutableStateOf(false) }
    var shizukuInstalled by remember { mutableStateOf(false) }
    var isNotificationListenerEnabled by remember { mutableStateOf(false) }
    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
    var isBatteryOptimized by remember {
        mutableStateOf(!powerManager.isIgnoringBatteryOptimizations(context.packageName))
    }
    var showAccessibilityDisclosure by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            isServiceEnabled =
                accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
                    .any { it.resolveInfo.serviceInfo.packageName == context.packageName }

            isNotificationListenerEnabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                ?.contains(context.packageName) == true
            
            isBatteryOptimized = !powerManager.isIgnoringBatteryOptimizations(context.packageName)

            shizukuInstalled = try {
                context.packageManager.getPackageInfo("moe.shizuku.privileged.api", PackageManager.PackageInfoFlags.of(0))
                true
            } catch (_: Exception) {
                false
            }

            shizukuAvailable = try {
                Shizuku.pingBinder()
            } catch (_: Exception) {
                false
            }
            shizukuPermission = if (shizukuAvailable) {
                try {
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                } catch (_: Exception) {
                    false
                }
            } else {
                false
            }

            kotlinx.coroutines.delay(2000)
        }
    }

    Card(
        modifier = modifier
            .padding(
                top = 4.dp, end = 4.dp, bottom = if (hasKeyboard) 4.dp else 0.dp
            )
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
                .verticalScroll(scrollState)
        ) {
            val versionName = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } catch (_: Exception) {
                "Unknown"
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(0.5f),
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = QuicksandTitleVariable
                )
                Text(
                    text = stringResource(R.string.version_prefix) + versionName,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(0.25f),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = QuicksandTitleVariable
                )
            }

            // --- Accessibility Status (Always Visible) ---
            Card(
                onClick = {
                    if (!isServiceEnabled) showAccessibilityDisclosure = true
                    else {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isServiceEnabled) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ),
                border = BorderStroke(
                    1.dp, if (isServiceEnabled) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (isServiceEnabled) stringResource(R.string.accessibility_active) else stringResource(R.string.accessibility_inactive),
                        color = if (isServiceEnabled) Color(0xFF2E7D32) else Color(0xFFC62828),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // --- Shizuku Status (Always Visible) ---
            Card(
                onClick = {
                    when {
                        !shizukuInstalled -> {
                            val appId = "moe.shizuku.privileged.api"
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$appId".toUri()))
                            } catch (_: Exception) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$appId".toUri()))
                            }
                        }
                        !shizukuAvailable -> {
                            val intent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                            if (intent != null) context.startActivity(intent)
                        }
                        !shizukuPermission -> {
                            try { Shizuku.requestPermission(0) } catch (e: Exception) { Log.e("MainActivity", "Shizuku request error", e) }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (shizukuAvailable && shizukuPermission) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ),
                border = BorderStroke(
                    1.dp, if (shizukuAvailable && shizukuPermission) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val shizukuText = when {
                        shizukuAvailable && shizukuPermission -> stringResource(R.string.shizuku_authorized)
                        shizukuAvailable && !shizukuPermission -> stringResource(R.string.shizuku_unauthorized)
                        shizukuInstalled -> stringResource(R.string.shizuku_not_running)
                        else -> stringResource(R.string.shizuku_not_installed)
                    }
                    Text(
                        text = shizukuText,
                        color = if (shizukuAvailable && shizukuPermission) Color(0xFF2E7D32) else Color(0xFFC62828),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // --- Battery Optimization Status ---
            if (isBatteryOptimized) {
                Card(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = "package:${context.packageName}".toUri()
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Error opening battery optimization settings", e)
                            // Fallback to general settings if package-specific fails
                            val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            context.startActivity(fallbackIntent)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    border = BorderStroke(1.dp, Color(0xFFC62828))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.battery_opt_on),
                            color = Color(0xFFC62828),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(R.string.battery_opt_desc),
                            color = Color(0xFFC62828).copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            // --- Media Control / Notification Listener (Hide if Active) ---
            if (!isNotificationListenerEnabled) {
                Card(
                    onClick = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    border = BorderStroke(1.dp, Color(0xFFC62828))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.media_control_inactive),
                            color = Color(0xFFC62828),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // --- System Settings (Hide if Active) ---
            if (!Settings.System.canWrite(context)) {
                Card(
                    onClick = {
                        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                        intent.data = "package:${context.packageName}".toUri()
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    border = BorderStroke(1.dp, Color(0xFFC62828))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.system_settings_denied),
                            color = Color(0xFFC62828),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // --- Notification Policy / DND (Hide if Active) ---
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                Card(
                    onClick = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    border = BorderStroke(1.dp, Color(0xFFC62828))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.dnd_access_denied),
                            color = Color(0xFFC62828),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // --- POST_NOTIFICATIONS (Hide if Active) ---
            if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Card(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    border = BorderStroke(1.dp, Color(0xFFC62828))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.notifications_denied),
                            color = Color(0xFFC62828),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (showAccessibilityDisclosure) {
                XenonDialog(
                    properties = DialogProperties(usePlatformDefaultWidth = true),
                    onDismissRequest = { showAccessibilityDisclosure = false },
                    title = stringResource(R.string.accessibility_disclosure_title),
                    confirmButtonText = stringResource(R.string.grant_permission),
                    onConfirmButtonClick = {
                        showAccessibilityDisclosure = false
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    },
                    content = {
                        Text(
                            stringResource(R.string.accessibility_disclosure_content),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    stringResource(R.string.disable_in_camera),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = disableInCamera, onCheckedChange = {
                    disableInCamera = it
                    SettingsManager.setDisableInCamera(context, it)
                })
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    stringResource(R.string.default_volume_slider),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = defaultWhenVolumeVisible, onCheckedChange = {
                    defaultWhenVolumeVisible = it
                    SettingsManager.setDefaultWhenVolumeVisible(context, it)
                })
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    stringResource(R.string.override_screen_off),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = overrideScreenOff, onCheckedChange = {
                    overrideScreenOff = it
                    SettingsManager.setOverrideScreenOffEnabled(context, it)
                })
            }

            if (!(shizukuAvailable && shizukuPermission)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    Text(
                        stringResource(R.string.volume_skip_screen_off),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = volumeLongPressSkip, onCheckedChange = {
                        volumeLongPressSkip = it
                        SettingsManager.setVolumeLongPressSkipEnabled(context, it)
                    })
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            PaletteRow(
                label = stringResource(R.string.device_color),
                selected = devicePalette,
                onSelect = onDevicePaletteChange,
                options = listOf(Palette.Black, Palette.White, Palette.Pink, Palette.Blue),
            )

            Spacer(modifier = Modifier.height(8.dp))

            PaletteRow(
                label = stringResource(R.string.keyboard_color),
                selected = keyboardPalette,
                onSelect = onKeyboardPaletteChange,
                options = Palette.entries.toList(),
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun ButtonConfigScreen(
    keyCode: Int,
    name: String,
    modifier: Modifier = Modifier,
    keyboardPalette: Palette,
    isFromKeyboard: Boolean = false,
    isScreenOff: Boolean,
    shizukuPermission: Boolean,
    onScreenOffChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onSelectAction: (Int, String, String) -> Unit,
) {
    val context = LocalContext.current
    var showDisabledDialog by rememberSaveable { mutableStateOf(false) }
    var showFocusWarningDialog by rememberSaveable { mutableStateOf(false) }

    val overrideScreenOff = remember { SettingsManager.isOverrideScreenOffEnabled(context) }
    val isVolumeButton = keyCode == 24 || keyCode == 25

    val themeWrapper: @Composable (@Composable () -> Unit) -> Unit = when {
        isFromKeyboard || keyCode == 111 -> { content -> PaletteTheme(palette = keyboardPalette) { content() } }
        keyCode == 131 -> { content -> RedTheme { content() } }
        keyCode == 133 || keyCode == 132 -> { content -> YellowTheme { content() } }
        keyCode == 24 || keyCode == 25 -> { content -> GreenTheme { content() } }
        keyCode == 27 || keyCode == 134 -> { content -> BlueTheme { content() } }
        else -> { content -> content() }
    }

    themeWrapper {
        if (showDisabledDialog) {
            XenonDialog(
                properties = DialogProperties(usePlatformDefaultWidth = true),
                onDismissRequest = { showDisabledDialog = false },
                title = stringResource(R.string.notice),
                confirmButtonText = stringResource(R.string.ok),
                onConfirmButtonClick = { showDisabledDialog = false },
                content = {
                    val message = when {
                        !overrideScreenOff -> stringResource(R.string.override_disabled_msg)
                        keyCode == 27 -> stringResource(R.string.camera_limit_msg)
                        isVolumeButton && !shizukuPermission -> stringResource(R.string.volume_shizuku_msg)
                        !shizukuPermission -> stringResource(R.string.non_volume_shizuku_msg)
                        else -> stringResource(R.string.config_unavailable_msg)
                    }
                    Text(message)
                }
            )
        }
        if (showFocusWarningDialog) {
            XenonDialog(
                properties = DialogProperties(usePlatformDefaultWidth = true),
                onDismissRequest = { showFocusWarningDialog = false },
                title = stringResource(R.string.warning),
                confirmButtonText = stringResource(R.string.i_understand),
                onConfirmButtonClick = {
                    showFocusWarningDialog = false
                    onScreenOffChange(true)
                },
                content = {
                    Text(stringResource(R.string.focus_warning_msg))
                }
            )
        }
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            Column(modifier = modifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = stringResource(R.string.button_config_title, name),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { onScreenOffChange(false) },
                        border = if (isScreenOff) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                        colors = if (!isScreenOff) ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                        else ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) { Text(stringResource(R.string.screen_on)) }

                    Button(
                        onClick = {
                            if (!overrideScreenOff) {
                                showDisabledDialog = true // Will show the "Override Off" message
                            } else if (keyCode == 27) {
                                showDisabledDialog = true // Hardware limitation
                            } else if (isVolumeButton && !shizukuPermission) {
                                showDisabledDialog = true // Use toggle instead
                            } else if (!isVolumeButton && !shizukuPermission) {
                                showDisabledDialog = true // Requires Shizuku
                            } else if (keyCode == 134 && !isScreenOff) {
                                showFocusWarningDialog = true
                            } else {
                                onScreenOffChange(true)
                            }
                        },
                        border = if (!isScreenOff) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                        colors = when {
                            !overrideScreenOff || keyCode == 27 || (isVolumeButton && !shizukuPermission) || (!isVolumeButton && !shizukuPermission) -> ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                            isScreenOff -> ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                            else -> ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        }
                    ) { Text(stringResource(R.string.screen_off)) }
                }

                val stateStr = if (isScreenOff) "OFF" else "ON"

                Spacer(modifier = Modifier.height(16.dp))

                val pressTypes = if (keyCode == 132 || keyCode == 133) listOf("SINGLE_PRESS")
                else listOf("SINGLE_PRESS", "DOUBLE_PRESS", "TRIPLE_PRESS")
                val holdTypes = if (keyCode == 132 || keyCode == 133) emptyList()
                else listOf("HOLD", "PRESS_AND_HOLD")

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    pressTypes.forEachIndexed { index, type ->
                        val shape = when {
                            pressTypes.size == 1 -> RoundedCornerShape(MediumCornerRadius)
                            index == 0 -> RoundedCornerShape(
                                topStart = MediumCornerRadius,
                                topEnd = MediumCornerRadius,
                                bottomStart = SmallestCornerRadius,
                                bottomEnd = SmallestCornerRadius
                            )
                            index == pressTypes.size - 1 -> RoundedCornerShape(
                                topStart = SmallestCornerRadius,
                                topEnd = SmallestCornerRadius,
                                bottomStart = MediumCornerRadius,
                                bottomEnd = MediumCornerRadius
                            )
                            else -> RoundedCornerShape(SmallestCornerRadius)
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = shape,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            MindControlActionSelector(keyCode, stateStr, type, onSelectAction)
                        }
                    }

                    if (holdTypes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp)) // 14 + 2 (from verticalArrangement) = 16dp
                        holdTypes.forEachIndexed { index, type ->
                            val shape = when (index) {
                                0 -> RoundedCornerShape(
                                    topStart = MediumCornerRadius,
                                    topEnd = MediumCornerRadius,
                                    bottomStart = SmallestCornerRadius,
                                    bottomEnd = SmallestCornerRadius
                                )
                                holdTypes.size - 1 -> RoundedCornerShape(
                                    topStart = SmallestCornerRadius,
                                    topEnd = SmallestCornerRadius,
                                    bottomStart = MediumCornerRadius,
                                    bottomEnd = MediumCornerRadius
                                )
                                else -> RoundedCornerShape(SmallestCornerRadius)
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = shape,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                MindControlActionSelector(keyCode, stateStr, type, onSelectAction)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MindControlActionSelector(
    keyCode: Int,
    state: String,
    type: String,
    onSelectAction: (Int, String, String) -> Unit
) {
    val context = LocalContext.current
    val action = remember(keyCode, state, type) {
        SettingsManager.getAction(context, keyCode, state, type)
    }

    val shortcutLabel = stringResource(R.string.tab_shortcuts)
    val speedDialLabel = stringResource(R.string.speed_dial)
    val urlLabel = stringResource(R.string.url)
    val qrCodeLabel = stringResource(R.string.qr_code)
    val actionDisplayName = getActionDisplayName(action)

    val displayAction = remember(action, actionDisplayName, shortcutLabel, speedDialLabel, urlLabel, qrCodeLabel) {
        if (action.startsWith(SettingsManager.PREFIX_APP)) {
            val pkg = action.removePrefix(SettingsManager.PREFIX_APP)
            try {
                val pm = context.packageManager
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            } catch (_: Exception) {
                pkg
            }
        } else if (action.startsWith(SettingsManager.PREFIX_SHORTCUT)) {
            val parts = action.removePrefix(SettingsManager.PREFIX_SHORTCUT).split("||")
            parts.getOrNull(1) ?: shortcutLabel
        } else if (action.startsWith(SettingsManager.PREFIX_SPEED_DIAL)) {
            "$speedDialLabel: " + action.removePrefix(SettingsManager.PREFIX_SPEED_DIAL)
        } else if (action.startsWith(SettingsManager.PREFIX_URL)) {
            "$urlLabel: " + action.removePrefix(SettingsManager.PREFIX_URL)
        } else if (action.startsWith(SettingsManager.PREFIX_QR_CODE)) {
            "$qrCodeLabel: " + action.removePrefix(SettingsManager.PREFIX_QR_CODE)
        } else {
            actionDisplayName
        }
    }

    val displayType = getTypeDisplayName(type)

    val isContinuum = action == SettingsManager.ACTION_VOLUME_UP ||
            action == SettingsManager.ACTION_VOLUME_DOWN ||
            action == SettingsManager.ACTION_SCROLL_UP_SMOOTH ||
            action == SettingsManager.ACTION_SCROLL_DOWN_SMOOTH ||
            action == SettingsManager.ACTION_SCROLL_UP_SMOOTH_FAST ||
            action == SettingsManager.ACTION_SCROLL_DOWN_SMOOTH_FAST ||
            action == "TAP_SCROLL_UP_SMOOTH" ||
            action == "TAP_SCROLL_DOWN_SMOOTH" ||
            action == SettingsManager.ACTION_BRIGHTNESS_UP ||
            action == SettingsManager.ACTION_BRIGHTNESS_DOWN

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "$displayType: ",
            modifier = Modifier.width(165.dp),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start
        )
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedButton(
            onClick = { onSelectAction(keyCode, state, type) },
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = displayAction,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                ActionIcon(
                    action = action,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(start = 4.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                if (isContinuum) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
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

@Composable
fun ActionSelectionScreen(
    config: ActionConfig,
    onBack: () -> Unit,
    onActionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = listOf(
        stringResource(R.string.tab_actions),
        stringResource(R.string.tab_apps),
        stringResource(R.string.tab_shortcuts),
        stringResource(R.string.tab_system),
        stringResource(R.string.tab_media)
    )
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()
    val backgroundColor = MaterialTheme.colorScheme.background
    val isScreenOff = config.state == "OFF"

    var shizukuReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            shizukuReady = try {
                Shizuku.pingBinder() &&
                        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } catch (_: Exception) {
                false
            }
            kotlinx.coroutines.delay(2000)
        }
    }

    val displayType = getTypeDisplayName(config.type)

    Surface(color = backgroundColor, modifier = Modifier.fillMaxSize()) {
        Column(modifier = modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    stringResource(R.string.select_action_title, displayType),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            PrimaryScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 16.dp,
                indicator = {},
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                            .clip(RoundedCornerShape(30.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                            .then(
                                if (!selected) Modifier.border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(30.dp)
                                ) else Modifier
                            )
                            .clickable {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                beyondViewportPageCount = 1
            ) { page ->
                when (page) {
                    0 -> ActionsTab(config, onActionSelected, shizukuReady)
                    1 -> AppsTab(config, onActionSelected, isScreenOff)
                    2 -> ShortcutsTab(config, onActionSelected, isScreenOff)
                    3 -> SystemTab(config, onActionSelected, shizukuReady)
                    4 -> MediaTab(config, onActionSelected, shizukuReady)
                }
            }
        }
    }
}

@Composable
fun ActionsTab(
    config: ActionConfig,
    onActionSelected: (String) -> Unit,
    shizukuReady: Boolean,
) {
    val actions = listOf(
        SettingsManager.ACTION_NONE,
        SettingsManager.ACTION_DEFAULT,
        SettingsManager.ACTION_HOME,
        SettingsManager.ACTION_BACK,
        SettingsManager.ACTION_RECENTS,
        SettingsManager.ACTION_SHOW_MENU,
        SettingsManager.ACTION_LOCK,
        SettingsManager.ACTION_AOD,
        SettingsManager.ACTION_FLASHLIGHT,
        SettingsManager.ACTION_SCREENSHOT,
        SettingsManager.ACTION_QUICK_SETTINGS,
        SettingsManager.ACTION_LAST_APP,
        SettingsManager.ACTION_APP_INFO,
        SettingsManager.ACTION_POWER_DIALOG,
        SettingsManager.ACTION_GOOGLE_SEARCH,
        SettingsManager.ACTION_ASSISTANT,
        SettingsManager.ACTION_SCROLL_UP,
        SettingsManager.ACTION_SCROLL_DOWN,
        SettingsManager.ACTION_SCROLL_UP_SMOOTH,
        SettingsManager.ACTION_SCROLL_DOWN_SMOOTH,
        SettingsManager.ACTION_SCROLL_UP_SMOOTH_FAST,
        SettingsManager.ACTION_SCROLL_DOWN_SMOOTH_FAST,
        SettingsManager.ACTION_COPY,
        SettingsManager.ACTION_CUT,
        SettingsManager.ACTION_PASTE,
        SettingsManager.ACTION_SPEED_DIAL,
        SettingsManager.ACTION_URL,
        SettingsManager.ACTION_QR_CODE,
    )
    ActionList(actions, config, onActionSelected, shizukuReady)
}

@Composable
fun AppsTab(
    config: ActionConfig,
    onActionSelected: (String) -> Unit,
    isScreenOff: Boolean,
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val scope = rememberCoroutineScope()
    val apps = remember { mutableStateListOf<AppItem>() }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
            val appList = resolveInfos.map {
                AppItem(
                    it.loadLabel(pm).toString(),
                    it.activityInfo.packageName,
                    it.loadIcon(pm)
                )
            }.distinctBy { it.packageName }.sortedBy { it.name }
            withContext(Dispatchers.Main) {
                apps.clear()
                apps.addAll(appList)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (isScreenOff) {
            item {
                Text(
                    stringResource(R.string.wake_screen_apps_msg),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
        items(apps.size) { index ->
            val app = apps[index]
            val shape = when {
                apps.size == 1 -> RoundedCornerShape(30.dp)
                index == 0 -> RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                index == apps.size - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 30.dp, bottomEnd = 30.dp)
                else -> RoundedCornerShape(4.dp)
            }
            Surface(
                color = if (isScreenOff)
                    MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.surfaceContainer,
                shape = shape,
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    headlineContent = {
                        Text(
                            app.name,
                            color = if (isScreenOff)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    supportingContent = {
                        Text(
                            app.packageName,
                            color = if (isScreenOff)
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    leadingContent = {
                        val bitmap = remember(app.packageName) { app.icon.toBitmap().asImageBitmap() }
                        Image(
                            bitmap,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .then(if (isScreenOff) Modifier.alpha(0.38f) else Modifier)
                        )
                    },
                    modifier = Modifier
                        .clickable(enabled = !isScreenOff) {
                            SettingsManager.setAction(
                                context,
                                config.keyCode,
                                config.state,
                                config.type,
                                SettingsManager.PREFIX_APP + app.packageName
                            )
                            onActionSelected(app.name)
                        }
                        .padding(vertical = 4.dp),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
    }
}

data class AppItem(val name: String, val packageName: String, val icon: Drawable)

data class ShortcutItem(val name: String, val packageName: String, val className: String, val icon: Drawable)

@Composable
fun ShortcutsTab(
    config: ActionConfig,
    onActionSelected: (String) -> Unit,
    isScreenOff: Boolean,
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val scope = rememberCoroutineScope()
    val shortcutItems = remember { mutableStateListOf<ShortcutItem>() }

    @Suppress("DEPRECATION")
    val shortcutLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data ?: return@rememberLauncherForActivityResult

            // Log for debugging
            Log.d("ShortcutsTab", "Shortcut picker returned data: ${data.extras?.keySet()}")

            val shortcutIntent =
                data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT, Intent::class.java)

            val name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME) ?: "Shortcut"
            val uri = shortcutIntent?.toUri(Intent.URI_INTENT_SCHEME)

            if (uri != null) {
                Log.d("ShortcutsTab", "Saving shortcut: $name -> $uri")
                SettingsManager.setAction(
                    context,
                    config.keyCode,
                    config.state,
                    config.type,
                    SettingsManager.PREFIX_SHORTCUT + uri + "||" + name
                )
                onActionSelected(name)
            } else {
                Log.e("ShortcutsTab", "Failed to extract shortcut intent from result")
            }
        }
    }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            val intent = Intent(Intent.ACTION_CREATE_SHORTCUT)
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            val itemList = resolveInfos.map {
                ShortcutItem(
                    it.activityInfo.loadLabel(pm).toString().ifEmpty { it.loadLabel(pm).toString() },
                    it.activityInfo.packageName,
                    it.activityInfo.name,
                    it.loadIcon(pm)
                )
            }.sortedBy { it.name }
            withContext(Dispatchers.Main) {
                shortcutItems.clear()
                shortcutItems.addAll(itemList)
            }
        }
    }

    if (shortcutItems.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_shortcuts_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (isScreenOff) {
                item {
                    Text(
                        stringResource(R.string.wake_screen_shortcuts_msg),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            items(shortcutItems.size) { index ->
                val item = shortcutItems[index]
                val shape = when {
                    shortcutItems.size == 1 -> RoundedCornerShape(30.dp)
                    index == 0 -> RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                    index == shortcutItems.size - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 30.dp, bottomEnd = 30.dp)
                    else -> RoundedCornerShape(4.dp)
                }
                Surface(
                    color = if (isScreenOff)
                        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.surfaceContainer,
                    shape = shape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                item.name,
                                color = if (isScreenOff)
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        supportingContent = {
                            Text(
                                item.packageName,
                                color = if (isScreenOff)
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingContent = {
                            val bitmap = remember(item.packageName + item.className) { item.icon.toBitmap().asImageBitmap() }
                            Image(
                                bitmap,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .then(if (isScreenOff) Modifier.alpha(0.38f) else Modifier)
                            )
                        },
                        modifier = Modifier
                            .clickable(enabled = !isScreenOff) {
                                val intent = Intent(Intent.ACTION_CREATE_SHORTCUT).apply {
                                    component = ComponentName(item.packageName, item.className)
                                    addCategory(Intent.CATEGORY_DEFAULT)
                                }
                                try {
                                    shortcutLauncher.launch(intent)
                                } catch (e: Exception) {
                                    Log.e("ShortcutsTab", "Failed to launch shortcut picker", e)
                                }
                            }
                            .padding(vertical = 4.dp),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }
}

@Composable
fun SystemTab(
    config: ActionConfig,
    onActionSelected: (String) -> Unit,
    shizukuReady: Boolean,
) {
    val actions = listOf(
        SettingsManager.ACTION_VIBRATE_RINGER,
        SettingsManager.ACTION_CYCLE_SOUND_MODE,
        SettingsManager.ACTION_DND,
        SettingsManager.ACTION_NOTIFICATIONS,
        SettingsManager.ACTION_BRIGHTNESS_UP,
        SettingsManager.ACTION_BRIGHTNESS_DOWN,
        SettingsManager.ACTION_AUTO_BRIGHTNESS_TOGGLE,
        SettingsManager.ACTION_WIFI_TOGGLE,
        SettingsManager.ACTION_BLUETOOTH_TOGGLE,
        SettingsManager.ACTION_DATA_TOGGLE,
        SettingsManager.ACTION_NFC_TOGGLE,
        SettingsManager.ACTION_LOCATION_TOGGLE,
        SettingsManager.ACTION_ROTATE_TOGGLE,
        SettingsManager.ACTION_ROTATE_360,
        SettingsManager.ACTION_AUTOROTATE_TOGGLE
    )
    ActionList(actions, config, onActionSelected, shizukuReady)
}

@Composable
fun MediaTab(
    config: ActionConfig,
    onActionSelected: (String) -> Unit,
    shizukuReady: Boolean,
) {
    val actions = listOf(
        SettingsManager.ACTION_VOLUME_UP,
        SettingsManager.ACTION_VOLUME_DOWN,
        SettingsManager.ACTION_MUTE_VOL,
        SettingsManager.ACTION_VOLUME_DIALOG,
        SettingsManager.ACTION_MUTE_MIC_TOGGLE,
        SettingsManager.ACTION_PLAY_PAUSE,
        SettingsManager.ACTION_STOP,
        SettingsManager.ACTION_PREVIOUS,
        SettingsManager.ACTION_NEXT,
        SettingsManager.ACTION_FAST_FORWARD,
        SettingsManager.ACTION_REWIND,
        SettingsManager.ACTION_STEP_FORWARD,
        SettingsManager.ACTION_STEP_BACKWARD,
    )
    ActionList(actions, config, onActionSelected, shizukuReady)
}

@Composable
fun LiftToWakeWarningDialog(
    onDismissRequest: () -> Unit,
    onIgnore: (Boolean) -> Unit,
    onDisable: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var dontShowAgain by remember { mutableStateOf(false) }

    XenonDialog(
        properties = DialogProperties(usePlatformDefaultWidth = true),
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.disable_lift_to_wake_title),
        confirmButtonText = stringResource(R.string.disable),
        onConfirmButtonClick = {
            onDisable(dontShowAgain)
            try {
                // Open Display Settings where the user confirmed the setting is located
                val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)

                // If Shizuku is available, scroll to the bottom after a short delay
                if (ShizukuManager.isAvailable()) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        // 1. Slow swipe to collapse the header and move the list
                        // 2. sleep to let the animation finish
                        // 3. MOVE_END to jump focus to the bottom
                        // 4. Repeated DPAD_DOWN to settle on the absolute last item
                        val scrollCommand = buildString {
                            append("input swipe 500 1000 500 200 10")
                            append(" && sleep 0.8")
                            append(" && input keyevent 123")
                            repeat(25) { append(" && input keyevent 20") }
                        }
                        ShizukuManager.runShellCommand(scrollCommand)
                    }, 1500L)
                }
            } catch (_: Exception) {
                try {
                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                } catch (_: Exception) {
                    // Fallback failed
                }
            }
        },
        actionButton1Text = stringResource(R.string.ignore),
        onActionButton1Click = {
            onIgnore(dontShowAgain)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.lift_to_wake_msg),
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { dontShowAgain = !dontShowAgain }
            ) {
                Checkbox(
                    checked = dontShowAgain,
                    onCheckedChange = { dontShowAgain = it }
                )
                Text(
                    text = stringResource(R.string.dont_show_again),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AodStylePickerDialog(
    onDismissRequest: () -> Unit,
    onStyleSelected: (SettingsManager.AodStyle, Boolean) -> Unit
) {
    val context = LocalContext.current
    val styles = SettingsManager.getAvailableAodStyles()
    val carouselState = rememberCarouselState(itemCount = { styles.size })
    val selectedIndex = carouselState.currentItem
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val estimatedCenterItemWidth = (screenWidth - 220.dp).coerceIn(140.dp, 260.dp)
    val estimatedItemHeight = estimatedCenterItemWidth * 1.15f
    val carouselHeight = estimatedItemHeight + 16.dp

    var mediaControlsEnabled by remember { mutableStateOf(SettingsManager.isAodMediaEnabled(context)) }

    XenonDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = true),
        title = stringResource(R.string.select_aod_style_title),
        confirmButtonText = stringResource(R.string.select),
        onConfirmButtonClick = {
            onStyleSelected(styles[selectedIndex], mediaControlsEnabled)
        },
        contentManagesScrolling = true,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalCenteredHeroCarousel(
                    state = carouselState,
                    maxItemWidth = estimatedCenterItemWidth,
                    modifier = Modifier
                        .height(carouselHeight)
                        .fillMaxWidth(),
                    itemSpacing = 8.dp,
                    contentPadding = PaddingValues(horizontal = 24.dp)
                ) { index ->
                    val style = styles[index]
                    val isSelected = selectedIndex == index
                    
                    val borderStroke = BorderStroke(
                        if (isSelected) 4.dp else 2.dp, 
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    val itemShape = MaterialTheme.shapes.extraLarge

                    val styleName = when (style) {
                        SettingsManager.AodStyle.CONCENTRIC -> stringResource(R.string.style_concentric)
                        SettingsManager.AodStyle.STACKED -> stringResource(R.string.style_stacked)
                        SettingsManager.AodStyle.INLINE -> stringResource(R.string.style_inline)
                        SettingsManager.AodStyle.ANALOG -> stringResource(R.string.style_analog)
                        SettingsManager.AodStyle.STACKED_DOT -> stringResource(R.string.style_stacked_dot)
                        SettingsManager.AodStyle.STACKED_DIGITAL -> stringResource(R.string.style_stacked_digital)
                        SettingsManager.AodStyle.INLINE_DOT -> stringResource(R.string.style_inline_dot)
                        SettingsManager.AodStyle.INLINE_DIGITAL -> stringResource(R.string.style_inline_digital)
                        SettingsManager.AodStyle.PLANETS -> stringResource(R.string.style_planets)
                        SettingsManager.AodStyle.SPINNER -> stringResource(R.string.style_spinner)
                        SettingsManager.AodStyle.PIXEL_STACKED -> stringResource(R.string.style_pixel_stacked)
                        SettingsManager.AodStyle.PIXEL_INLINE -> stringResource(R.string.style_pixel_inline)
                    }

                    AodStyleOption(
                        name = styleName,
                        style = style,
                        onClick = {
                            scope.launch {
                                carouselState.animateScrollToItem(index)
                            }
                        },
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .aspectRatio(1f / 1.15f)
                            .maskClip(itemShape)
                            .maskBorder(borderStroke, itemShape)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.aod_media_controls),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = mediaControlsEnabled,
                        onCheckedChange = { mediaControlsEnabled = it }
                    )
                }
            }
        }
    )
}

@Composable
fun AodStyleOption(
    name: String,
    style: SettingsManager.AodStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        shape = androidx.compose.ui.graphics.RectangleShape
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Preview
            Box(modifier = Modifier.fillMaxSize().alpha(0.8f)) {
                when (style) {
                    SettingsManager.AodStyle.CONCENTRIC -> {
                        ConcentricAodStyle(
                            isActive = true,
                            notifications = emptyList(),
                            mediaInfo = null,
                            isCharging = false,
                            batteryLevel = 80,
                            animatedTextAlpha = 0f,
                            offsetY = 0f
                        )
                    }
                    SettingsManager.AodStyle.STACKED -> {
                        StackedAodStyle(
                            isActive = true,
                            notifications = emptyList(),
                            mediaInfo = null,
                            isCharging = false,
                            batteryLevel = 80,
                            animatedTextAlpha = 0f,
                            offsetY = 0f
                        )
                    }
                    SettingsManager.AodStyle.INLINE -> {
                        InlineAodStyle(
                            isActive = true,
                            notifications = emptyList(),
                            mediaInfo = null,
                            isCharging = false,
                            batteryLevel = 80,
                            animatedTextAlpha = 0f,
                            offsetY = 0f
                        )
                    }
                    SettingsManager.AodStyle.ANALOG -> {
                        AnalogAodStyle(
                            isActive = true,
                            notifications = emptyList(),
                            mediaInfo = null,
                            isCharging = false,
                            batteryLevel = 80,
                            animatedTextAlpha = 0f,
                            offsetY = 0f
                        )
                    }
                    SettingsManager.AodStyle.STACKED_DOT -> {
                        StackedDotAodStyle(
                            isActive = true,
                            notifications = emptyList(),
                            mediaInfo = null,
                            isCharging = false,
                            batteryLevel = 80,
                            animatedTextAlpha = 0f,
                            offsetY = 0f
                        )
                    }
                    SettingsManager.AodStyle.STACKED_DIGITAL -> {
                        StackedDigitalAodStyle(
                            isActive = true,
                            notifications = emptyList(),
                            mediaInfo = null,
                            isCharging = false,
                            batteryLevel = 80,
                            animatedTextAlpha = 0f,
                            offsetY = 0f
                        )
                    }
                    SettingsManager.AodStyle.INLINE_DOT -> {
                        InlineDotAodStyle(
                            isActive = true,
                            notifications = emptyList(),
                            mediaInfo = null,
                            isCharging = false,
                            batteryLevel = 80,
                            animatedTextAlpha = 0f,
                            offsetY = 0f
                        )
                    }
                    SettingsManager.AodStyle.INLINE_DIGITAL -> {
                        InlineDigitalAodStyle(
                            isActive = true,
                            notifications = emptyList(),
                            mediaInfo = null,
                            isCharging = false,
                            batteryLevel = 80,
                            animatedTextAlpha = 0f,
                            offsetY = 0f
                        )
                    }
                    SettingsManager.AodStyle.PLANETS -> {
                        PlanetsAodStyle(
                            isActive = true,
                            notifications = emptyList(),
                            mediaInfo = null,
                            isCharging = false,
                            batteryLevel = 80,
                            animatedTextAlpha = 0f,
                            offsetY = 0f
                        )
                    }
                    SettingsManager.AodStyle.SPINNER -> {
                        SpinnerAodStyle(
                            isActive = true,
                            notifications = emptyList(),
                            mediaInfo = null,
                            isCharging = false,
                            batteryLevel = 80,
                            animatedTextAlpha = 0f,
                            offsetY = 0f
                        )
                    }
                    SettingsManager.AodStyle.PIXEL_STACKED -> {
                        PixelStackedAodStyle(
                            isActive = true,
                            notifications = emptyList(),
                            mediaInfo = null,
                            isCharging = false,
                            batteryLevel = 80,
                            animatedTextAlpha = 0f,
                            offsetY = 0f
                        )
                    }
                    SettingsManager.AodStyle.PIXEL_INLINE -> {
                        PixelInlineAodStyle(
                            isActive = true,
                            notifications = emptyList(),
                            mediaInfo = null,
                            isCharging = false,
                            batteryLevel = 80,
                            animatedTextAlpha = 0f,
                            offsetY = 0f
                        )
                    }
                }
            }
            
            // Label overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(name, color = Color.White, fontWeight = FontWeight.Bold)
            }
            

        }
    }
}
@Composable
fun ActionList(
    actions: List<String>,
    config: ActionConfig,
    onActionSelected: (String) -> Unit,
    shizukuReady: Boolean = true,
) {
    val context = LocalContext.current
    var showInputDialog by rememberSaveable { mutableStateOf<String?>(null) }
    var showAodStyleDialog by rememberSaveable { mutableStateOf(false) }
    var showLiftToWakeWarning by rememberSaveable { mutableStateOf(false) }
    var inputValue by rememberSaveable { mutableStateOf("") }

    val aodActionName = stringResource(R.string.action_aod)
    val styleConcentric = stringResource(R.string.style_concentric)
    val styleStacked = stringResource(R.string.style_stacked)
    val styleInline = stringResource(R.string.style_inline)
    val styleAnalog = stringResource(R.string.style_analog)
    val styleStackedDot = stringResource(R.string.style_stacked_dot)
    val styleStackedDigital = stringResource(R.string.style_stacked_digital)
    val styleInlineDot = stringResource(R.string.style_inline_dot)
    val styleInlineDigital = stringResource(R.string.style_inline_digital)
    val stylePlanets = stringResource(R.string.style_planets)
    val styleSpinner = stringResource(R.string.style_spinner)
    val stylePixelStacked = stringResource(R.string.style_pixel_stacked)
    val stylePixelInline = stringResource(R.string.style_pixel_inline)
    val speedDialName = stringResource(R.string.speed_dial)
    val urlName = stringResource(R.string.url)
    val qrCodeName = stringResource(R.string.qr_code)

    if (showLiftToWakeWarning) {
        LiftToWakeWarningDialog(
            onDismissRequest = { showLiftToWakeWarning = false },
            onIgnore = { dontShowAgain ->
                if (dontShowAgain) SettingsManager.setShowLiftToWakeWarning(context, false)
                showLiftToWakeWarning = false
                showAodStyleDialog = true
            },
            onDisable = { dontShowAgain ->
                if (dontShowAgain) SettingsManager.setShowLiftToWakeWarning(context, false)
                showLiftToWakeWarning = false
                showAodStyleDialog = true
            }
        )
    }

    if (showAodStyleDialog) {
        AodStylePickerDialog(
            onDismissRequest = { showAodStyleDialog = false },
            onStyleSelected = { style, mediaEnabled ->
                SettingsManager.setAodStyle(context, style)
                SettingsManager.setAodMediaEnabled(context, mediaEnabled)
                SettingsManager.setAction(context, config.keyCode, config.state, config.type, SettingsManager.ACTION_AOD)
                val styleName = when (style) {
                    SettingsManager.AodStyle.CONCENTRIC -> styleConcentric
                    SettingsManager.AodStyle.STACKED -> styleStacked
                    SettingsManager.AodStyle.INLINE -> styleInline
                    SettingsManager.AodStyle.ANALOG -> styleAnalog
                    SettingsManager.AodStyle.STACKED_DOT -> styleStackedDot
                    SettingsManager.AodStyle.STACKED_DIGITAL -> styleStackedDigital
                    SettingsManager.AodStyle.INLINE_DOT -> styleInlineDot
                    SettingsManager.AodStyle.INLINE_DIGITAL -> styleInlineDigital
                    SettingsManager.AodStyle.PLANETS -> stylePlanets
                    SettingsManager.AodStyle.SPINNER -> styleSpinner
                    SettingsManager.AodStyle.PIXEL_STACKED -> stylePixelStacked
                    SettingsManager.AodStyle.PIXEL_INLINE -> stylePixelInline
                }
                onActionSelected("$aodActionName: $styleName")
                showAodStyleDialog = false
            }
        )
    }

    if (showInputDialog != null) {
        val title = when (showInputDialog) {
            SettingsManager.ACTION_SPEED_DIAL -> stringResource(R.string.enter_number)
            SettingsManager.ACTION_URL -> stringResource(R.string.enter_url)
            SettingsManager.ACTION_QR_CODE -> stringResource(R.string.enter_qr_content)
            else -> ""
        }
        val label = when (showInputDialog) {
            SettingsManager.ACTION_SPEED_DIAL -> stringResource(R.string.phone_number_hint)
            SettingsManager.ACTION_URL -> stringResource(R.string.link_hint)
            SettingsManager.ACTION_QR_CODE -> stringResource(R.string.text_hint)
            else -> ""
        }
        XenonDialog(
            onDismissRequest = { showInputDialog = null },
            properties = DialogProperties(usePlatformDefaultWidth = true),
            title = title,
            confirmButtonText = stringResource(R.string.ok),
            onConfirmButtonClick = {
                val prefix = when (showInputDialog) {
                    SettingsManager.ACTION_SPEED_DIAL -> SettingsManager.PREFIX_SPEED_DIAL
                    SettingsManager.ACTION_URL -> SettingsManager.PREFIX_URL
                    SettingsManager.ACTION_QR_CODE -> SettingsManager.PREFIX_QR_CODE
                    else -> ""
                }
                SettingsManager.setAction(context, config.keyCode, config.state, config.type, prefix + inputValue)
                onActionSelected(
                    when (showInputDialog) {
                        SettingsManager.ACTION_SPEED_DIAL -> speedDialName
                        SettingsManager.ACTION_URL -> urlName
                        SettingsManager.ACTION_QR_CODE -> qrCodeName
                        else -> ""
                    }
                )
                showInputDialog = null
            },
            content = {
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    singleLine = true,
                    label = { Text(label) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(actions.size) { index ->
            val action = actions[index]
            val disabled = isActionDisabled(action, shizukuReady)
            val disabledReasonRes = disabledReasonFor(action, shizukuReady)

            val displayName = getActionDisplayName(action)

            val isContinuum = action == SettingsManager.ACTION_VOLUME_UP ||
                    action == SettingsManager.ACTION_VOLUME_DOWN ||
                    action == SettingsManager.ACTION_SCROLL_UP_SMOOTH ||
                    action == SettingsManager.ACTION_SCROLL_DOWN_SMOOTH ||
                    action == SettingsManager.ACTION_SCROLL_UP_SMOOTH_FAST ||
                    action == SettingsManager.ACTION_SCROLL_DOWN_SMOOTH_FAST ||
                    action == "TAP_SCROLL_UP_SMOOTH" ||
                    action == "TAP_SCROLL_DOWN_SMOOTH" ||
                    action == SettingsManager.ACTION_BRIGHTNESS_UP ||
                    action == SettingsManager.ACTION_BRIGHTNESS_DOWN

            val shape = when {
                actions.size == 1 -> RoundedCornerShape(30.dp)
                index == 0 -> RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                index == actions.size - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 30.dp, bottomEnd = 30.dp)
                else -> RoundedCornerShape(4.dp)
            }

            Surface(
                color = if (disabled)
                    MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.surfaceContainer,
                shape = shape,
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    headlineContent = {
                        Text(
                            displayName,
                            color = if (disabled)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    supportingContent = disabledReasonRes?.let { resId ->
                        {
                            Text(
                                stringResource(resId),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    leadingContent = {
                        ActionIcon(
                            action = action,
                            modifier = Modifier.size(24.dp),
                            tint = if (disabled)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                            else MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingContent = if (isContinuum) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardDoubleArrowRight,
                                contentDescription = null,
                                tint = if (disabled)
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else null,
                    modifier = Modifier
                        .clickable(enabled = !disabled) {
                            if (action == SettingsManager.ACTION_AOD) {
                                if (SettingsManager.shouldShowLiftToWakeWarning(context)) {
                                    showLiftToWakeWarning = true
                                } else {
                                    showAodStyleDialog = true
                                }
                            } else if (action == SettingsManager.ACTION_SPEED_DIAL ||
                                action == SettingsManager.ACTION_URL ||
                                action == SettingsManager.ACTION_QR_CODE
                            ) {
                                showInputDialog = action
                                val currentSavedAction =
                                    SettingsManager.getAction(context, config.keyCode, config.state, config.type)
                                val prefix = when (action) {
                                    SettingsManager.ACTION_SPEED_DIAL -> SettingsManager.PREFIX_SPEED_DIAL
                                    SettingsManager.ACTION_URL -> SettingsManager.PREFIX_URL
                                    SettingsManager.ACTION_QR_CODE -> SettingsManager.PREFIX_QR_CODE
                                    else -> ""
                                }
                                inputValue =
                                    if (currentSavedAction.startsWith(prefix)) currentSavedAction.removePrefix(prefix) else ""
                            } else {
                                SettingsManager.setAction(context, config.keyCode, config.state, config.type, action)
                                onActionSelected(displayName)
                            }
                        }
                        .padding(vertical = 4.dp),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
    }
}