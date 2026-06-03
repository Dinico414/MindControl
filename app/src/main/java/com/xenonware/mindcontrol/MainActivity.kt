@file:Suppress("AssignedValueIsNeverRead")

package com.xenonware.mindcontrol

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.os.Bundle
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowUp
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Watch
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
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

private fun disabledReasonFor(action: String, shizukuReady: Boolean): String? = when {
    !shizukuReady && action in SHIZUKU_REQUIRED_ACTIONS -> "Requires Shizuku"
    else -> null
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
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(end = 4.dp, bottom = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (pressedKeys.contains(131)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer)
                        .clickable { onButtonSelected(131, "AI Button") },
                    contentAlignment = Alignment.Center
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
                                text = "AI Button",
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
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(start = 4.dp, bottom = 1.dp)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = 4.dp,
                                    bottomEnd = 4.dp
                                )
                            )
                            .background(if (pressedKeys.contains(133)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer)
                            .clickable { onButtonSelected(133, "Camera Up") },
                        contentAlignment = Alignment.Center
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
                                    text = "Camera Up",
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                    color = if (pressedKeys.contains(133)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontFamily = QuicksandTitleVariable
                                )
                            }
                        }
                    }
                    // Camera Down
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 1.dp, bottom = 4.dp)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 4.dp,
                                    topEnd = 4.dp,
                                    bottomStart = 16.dp,
                                    bottomEnd = 16.dp
                                )
                            )
                            .background(if (pressedKeys.contains(132)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer)
                            .clickable { onButtonSelected(132, "Camera Down") },
                        contentAlignment = Alignment.Center
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
                                    text = "Camera Down",
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
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(end = 4.dp, top = 4.dp)
                                .clip(RoundedCornerShape(16.dp))
                                // Now uses the primary color from keyboardPalette
                                .background(if (pressedKeys.contains(111)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer)
                                .clickable { onButtonSelected(111, "Keyboard Button") },
                            contentAlignment = Alignment.Center
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
                                        text = "Keyboard",
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
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 4.dp, bottom = 1.dp)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = 4.dp,
                                    bottomEnd = 4.dp
                                )
                            )
                            .background(if (pressedKeys.contains(24)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer)
                            .clickable { onButtonSelected(24, "Volume Up") },
                        contentAlignment = Alignment.Center
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
                                    text = "Volume Up",
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                    color = if (pressedKeys.contains(24)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontFamily = QuicksandTitleVariable
                                )
                            }
                        }
                    }
                    // Volume Down
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 1.dp, bottom = 4.dp)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 4.dp,
                                    topEnd = 4.dp,
                                    bottomStart = 16.dp,
                                    bottomEnd = 16.dp
                                )
                            )
                            .background(if (pressedKeys.contains(25)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer)
                            .clickable { onButtonSelected(25, "Volume Down") },
                        contentAlignment = Alignment.Center
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
                                    text = "Volume Down",
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
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(start = 4.dp, end = 1.dp, top = 4.dp)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 4.dp,
                                        bottomStart = 16.dp,
                                        bottomEnd = 4.dp
                                    )
                                )
                                .background(if (pressedKeys.contains(27)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer)
                                .clickable { onButtonSelected(27, "Camera Button") },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CameraAlt,
                                    contentDescription = null,
                                    tint = if (pressedKeys.contains(27)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Camera\nButton",
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                    color = if (pressedKeys.contains(27)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontFamily = QuicksandTitleVariable
                                )
                            }
                        }
                        // Focus Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(start = 1.dp, top = 4.dp)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 4.dp,
                                        topEnd = 16.dp,
                                        bottomStart = 4.dp,
                                        bottomEnd = 16.dp
                                    )
                                )
                                .background(if (pressedKeys.contains(134)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer)
                                .clickable { onButtonSelected(134, "Focus Button") },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FilterCenterFocus,
                                    contentDescription = null,
                                    tint = if (pressedKeys.contains(134)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Focus\nButton",
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
    var showAccessibilityDisclosure by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            isServiceEnabled =
                accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
                    .any { it.resolveInfo.serviceInfo.packageName == context.packageName }

            isNotificationListenerEnabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                ?.contains(context.packageName) == true

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
                Spacer(modifier = Modifier.weight(0.25f))
                Text(
                    text = "Settings",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(0.5f),
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = QuicksandTitleVariable
                )
                Text(
                    text = "v$versionName",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(0.25f),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = QuicksandTitleVariable
                )
            }

            // --- Accessibility Status (Always Visible) ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable {
                        if (!isServiceEnabled) showAccessibilityDisclosure = true
                        else {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        }
                    },
                colors = CardDefaults.cardColors(
                    containerColor = if (isServiceEnabled) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ),
                border = BorderStroke(
                    1.dp, if (isServiceEnabled) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (isServiceEnabled) "Accessibility: ACTIVE" else "Accessibility: INACTIVE (Tap to Enable)",
                        color = if (isServiceEnabled) Color(0xFF2E7D32) else Color(0xFFC62828),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // --- Shizuku Status (Always Visible) ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable {
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
                colors = CardDefaults.cardColors(
                    containerColor = if (shizukuAvailable && shizukuPermission) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ),
                border = BorderStroke(
                    1.dp, if (shizukuAvailable && shizukuPermission) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val shizukuText = when {
                        shizukuAvailable && shizukuPermission -> "Shizuku: AUTHORIZED"
                        shizukuAvailable && !shizukuPermission -> "Shizuku: UNAUTHORIZED (Tap to Authorize)"
                        shizukuInstalled -> "Shizuku: NOT RUNNING (Tap to Open)"
                        else -> "Shizuku: NOT INSTALLED (Tap to Install)"
                    }
                    Text(
                        text = shizukuText,
                        color = if (shizukuAvailable && shizukuPermission) Color(0xFF2E7D32) else Color(0xFFC62828),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // --- Media Control / Notification Listener (Hide if Active) ---
            if (!isNotificationListenerEnabled) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    border = BorderStroke(1.dp, Color(0xFFC62828))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Media Control: INACTIVE (Tap to Enable)",
                            color = Color(0xFFC62828),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // --- System Settings (Hide if Active) ---
            if (!Settings.System.canWrite(context)) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                            intent.data = ("package:" + context.packageName).toUri()
                            context.startActivity(intent)
                        },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    border = BorderStroke(1.dp, Color(0xFFC62828))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Modify System Settings: DENIED (Tap to Allow)",
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                            context.startActivity(intent)
                        },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    border = BorderStroke(1.dp, Color(0xFFC62828))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "DND Access: DENIED (Tap to Allow)",
                            color = Color(0xFFC62828),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // --- POST_NOTIFICATIONS (Hide if Active) ---
            if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            context.startActivity(intent)
                        },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    border = BorderStroke(1.dp, Color(0xFFC62828))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Notifications: BLOCKED (Tap to Allow)",
                            color = Color(0xFFC62828),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (showAccessibilityDisclosure) {
                XenonDialog(
                    onDismissRequest = { showAccessibilityDisclosure = false },
                    title = "Accessibility Disclosure",
                    confirmButtonText = "Grant Permission",
                    onConfirmButtonClick = {
                        showAccessibilityDisclosure = false
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    },
                    content = {
                        Text(
                            "MindControl uses Accessibility Services to detect hardware button presses (such as volume and camera buttons) and map them to custom actions.\n\n" +
                                    "• This service is required for the app's core functionality.\n" +
                                    "• No personal data is collected or shared.\n" +
                                    "• Key presses are processed locally on your device.",
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
                    "Disable in camera",
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
                    "Default volume if slider visible",
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
                    "Override Screen Off",
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
                        "Volume Skip (Screen Off)",
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
                label = "Device color",
                selected = devicePalette,
                onSelect = onDevicePaletteChange,
                options = listOf(Palette.Black, Palette.White, Palette.Pink, Palette.Blue),
            )

            Spacer(modifier = Modifier.height(8.dp))

            PaletteRow(
                label = "Keyboard color",
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
                title = "Notice",
                confirmButtonText = "OK",
                onConfirmButtonClick = { showDisabledDialog = false },
                content = {
                    val message = when {
                        !overrideScreenOff -> "Override Screen Off is currently disabled in Settings."
                        keyCode == 27 -> "The Camera button turns the screen on automatically, so it cannot be used for Screen Off actions."
                        isVolumeButton && !shizukuPermission -> "To customize Volume buttons for Screen Off, please authorize Shizuku or use the 'Volume Skip' toggle in Settings."
                        !shizukuPermission -> "Configuring non-volume buttons for Screen Off requires Shizuku permission."
                        else -> "This configuration is currently unavailable."
                    }
                    Text(message)
                }
            )
        }
        if (showFocusWarningDialog) {
            XenonDialog(
                properties = DialogProperties(usePlatformDefaultWidth = true),
                onDismissRequest = { showFocusWarningDialog = false },
                title = "Warning",
                confirmButtonText = "I Understand",
                onConfirmButtonClick = {
                    showFocusWarningDialog = false
                    onScreenOffChange(true)
                },
                content = {
                    Text("Using the Focus button while the screen is off may lead to accidental actions while the device is in your pocket.")
                }
            )
        }
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            Column(modifier = modifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "$name Configuration",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
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
                    ) { Text("Screen On") }

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
                    ) { Text("Screen Off") }
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

    val displayAction = if (action.startsWith(SettingsManager.PREFIX_APP)) {
        val pkg = action.removePrefix(SettingsManager.PREFIX_APP)
        try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (_: Exception) {
            pkg
        }
    } else if (action.startsWith(SettingsManager.PREFIX_SHORTCUT)) {
        val parts = action.removePrefix(SettingsManager.PREFIX_SHORTCUT).split("||")
        parts.getOrNull(1) ?: "Shortcut"
    } else if (action.startsWith(SettingsManager.PREFIX_SPEED_DIAL)) {
        "Speed Dial: " + action.removePrefix(SettingsManager.PREFIX_SPEED_DIAL)
    } else if (action.startsWith(SettingsManager.PREFIX_URL)) {
        "URL: " + action.removePrefix(SettingsManager.PREFIX_URL)
    } else if (action.startsWith(SettingsManager.PREFIX_QR_CODE)) {
        "QR Code: " + action.removePrefix(SettingsManager.PREFIX_QR_CODE)
    } else {
        action.split("_").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    val displayType = type.split("_").joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "$displayType: ",
            modifier = Modifier.widthIn(min = 80.dp),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1
        )
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
        action == SettingsManager.ACTION_LOCK_AOD -> Icons.Rounded.Lock
        action == SettingsManager.ACTION_PIXEL_WATCH -> Icons.Rounded.Watch
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
    val tabs = listOf("Actions", "Apps", "Shortcuts", "System", "Media")
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

    val displayType = config.type.split("_").joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    Surface(color = backgroundColor, modifier = Modifier.fillMaxSize()) {
        Column(modifier = modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    "Select Action for $displayType",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
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
        SettingsManager.ACTION_LOCK_AOD,
        SettingsManager.ACTION_PIXEL_WATCH,
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
                    "Launching apps requires waking the screen, which is disabled in Screen Off mode.",
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
            Text("No shortcut-capable apps found", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        "Shortcuts launch activities that wake the screen and are disabled in Screen Off mode.",
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
fun ActionList(
    actions: List<String>,
    config: ActionConfig,
    onActionSelected: (String) -> Unit,
    shizukuReady: Boolean = true,
) {
    val context = LocalContext.current
    var showInputDialog by rememberSaveable { mutableStateOf<String?>(null) }
    var inputValue by rememberSaveable { mutableStateOf("") }

    if (showInputDialog != null) {
        val title = when (showInputDialog) {
            SettingsManager.ACTION_SPEED_DIAL -> "Enter Number"
            SettingsManager.ACTION_URL -> "Enter URL"
            SettingsManager.ACTION_QR_CODE -> "Enter QR Code Content"
            else -> ""
        }
        val label = when (showInputDialog) {
            SettingsManager.ACTION_SPEED_DIAL -> "Phone Number ..."
            SettingsManager.ACTION_URL -> "Link ..."
            SettingsManager.ACTION_QR_CODE -> "Text ..."
            else -> ""
        }
        XenonDialog(
            onDismissRequest = { showInputDialog = null },
            properties = DialogProperties(usePlatformDefaultWidth = true),
            title = title,
            confirmButtonText = "OK",
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
                        SettingsManager.ACTION_SPEED_DIAL -> "Speed Dial"
                        SettingsManager.ACTION_URL -> "URL"
                        SettingsManager.ACTION_QR_CODE -> "QR Code"
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
            val disabledReason = disabledReasonFor(action, shizukuReady)

            val displayName = action.split("_").joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }

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
                    supportingContent = disabledReason?.let {
                        {
                            Text(
                                it,
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
                    modifier = Modifier
                        .clickable(enabled = !disabled) {
                            if (action == SettingsManager.ACTION_SPEED_DIAL ||
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