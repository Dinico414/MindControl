package com.xenonware.mindcontrol

import android.accessibilityservice.AccessibilityServiceInfo
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
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.rounded.Assistant
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.FilterCenterFocus
import androidx.compose.material.icons.rounded.FlashlightOn
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
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import com.xenon.mylibrary.res.XenonDialog
import com.xenon.mylibrary.theme.QuicksandTitleVariable
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
            var devicePalette by remember { mutableStateOf(SettingsManager.getDevicePalette(context)) }
            var keyboardPalette by remember {
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

@Composable
fun MindControlMainScreen(
    modifier: Modifier = Modifier,
    devicePalette: Palette,
    keyboardPalette: Palette,
    onDevicePaletteChange: (Palette) -> Unit,
    onKeyboardPaletteChange: (Palette) -> Unit,
) {
    var selectedButton by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var showKeyboard by remember { mutableStateOf(false) }
    var configFromKeyboard by remember { mutableStateOf(false) }
    var actionSelectionConfig by remember { mutableStateOf<ActionConfig?>(null) }

    val state = when {
        actionSelectionConfig != null -> "action_selection"
        selectedButton != null -> "config"
        showKeyboard -> "keyboard"
        else -> "grid"
    }

    AnimatedContent(
        targetState = state, transitionSpec = {
            when {
                targetState == "keyboard" -> slideInVertically { it } + fadeIn() togetherWith slideOutVertically { -it } + fadeOut()
                initialState == "keyboard" && targetState == "grid" -> slideInVertically { -it } + fadeIn() togetherWith slideOutVertically { it } + fadeOut()
                targetState == "action_selection" -> slideInVertically { it } + fadeIn() togetherWith slideOutVertically { -it } + fadeOut()
                initialState == "action_selection" -> slideInVertically { -it } + fadeIn() togetherWith slideOutVertically { it } + fadeOut()
                else -> fadeIn() togetherWith fadeOut()
            }
        }, label = "ScreenTransition"
    ) { s ->
        when (s) {
            "action_selection" -> {
                val config = actionSelectionConfig
                if (config != null) {
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
                        onBack = {
                            val cameFromKeyboard = configFromKeyboard
                            selectedButton = null
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

    var shizukuPermission by remember { mutableStateOf(false) }
    var shizukuAvailable by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            isServiceEnabled =
                accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
                    .any { it.resolveInfo.serviceInfo.packageName == context.packageName }

            shizukuAvailable = try {
                Shizuku.pingBinder()
            } catch (_: Exception) {
                false
            }
            if (shizukuAvailable) {
                shizukuPermission = try {
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                } catch (_: Exception) {
                    false
                }
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
            } catch (e: Exception) {
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

            Card(
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
                        text = if (isServiceEnabled) "Accessibility: ACTIVE" else "Accessibility: INACTIVE",
                        color = if (isServiceEnabled) Color(0xFF2E7D32) else Color(0xFFC62828),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (shizukuAvailable && shizukuPermission) Color(0xFFE8F5E9) else Color(
                        0xFFFFEBEE
                    )
                ),
                border = BorderStroke(
                    1.dp, if (shizukuAvailable && shizukuPermission) Color(0xFF2E7D32) else Color(
                        0xFFC62828
                    )
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (shizukuAvailable) {
                            if (shizukuPermission) "Shizuku: AUTHORIZED" else "Shizuku: UNAUTHORIZED"
                        } else "Shizuku: NOT RUNNING",
                        color = if (shizukuAvailable && shizukuPermission) Color(0xFF2E7D32) else Color(
                            0xFFC62828
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (shizukuAvailable && !shizukuPermission) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(onClick = {
                            try {
                                Shizuku.requestPermission(0)
                            } catch (e: Exception) {
                                Log.e("MainActivity", "Shizuku request error", e)
                            }
                        }, contentPadding = PaddingValues(4.dp)) {
                            Text("Authorize", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            if (!isServiceEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(8.dp)
                ) {
                    Text("Accessibility Settings", style = MaterialTheme.typography.bodySmall)
                }
            }

            if (!Settings.System.canWrite(context)) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                        intent.data = ("package:" + context.packageName).toUri()
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Text("Allow Sys Settings", style = MaterialTheme.typography.bodySmall)
                }
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Text("Allow DND Settings", style = MaterialTheme.typography.bodySmall)
                }
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
    onBack: () -> Unit,
    onSelectAction: (Int, String, String) -> Unit,
) {
    var isScreenOff by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    Column(modifier = modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "$name Configuration",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (keyCode != 27 && keyCode != 134) {
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FilledTonalButton(
                    onClick = { isScreenOff = false },
                    colors = if (!isScreenOff) ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                    else ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent, contentColor = Color.White
                    )
                ) { Text("Screen On") }

                FilledTonalButton(
                    onClick = { isScreenOff = true },
                    colors = if (isScreenOff) ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                    else ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent, contentColor = Color.White
                    )
                ) { Text("Screen Off") }
            }
        } else {
            Text(
                "Screen On only",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(8.dp)
            )
        }

        val stateStr = if (isScreenOff) "OFF" else "ON"

        Spacer(modifier = Modifier.height(16.dp))

        val types = if (keyCode == 132 || keyCode == 133) listOf("SINGLE")
        else listOf("SINGLE", "DOUBLE", "TRIPLE", "LONG")

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            types.forEach { type ->
                MindControlActionSelector(keyCode, stateStr, type, onSelectAction)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
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
        } catch (e: Exception) {
            pkg
        }
    } else if (action.startsWith(SettingsManager.PREFIX_SHORTCUT)) {
        val parts = action.removePrefix(SettingsManager.PREFIX_SHORTCUT).split("|")
        parts.getOrNull(2) ?: action.removePrefix(SettingsManager.PREFIX_SHORTCUT)
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

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "$type: ", modifier = Modifier.weight(1f), color = Color.White
        )
        OutlinedButton(onClick = { onSelectAction(keyCode, state, type) }) {
            ActionIcon(
                action = action,
                modifier = Modifier.size(18.dp).padding(end = 4.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(displayAction, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun ActionIcon(action: String, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    val context = LocalContext.current
    val icon: Any? = when {
        action.startsWith(SettingsManager.PREFIX_APP) -> {
            val pkg = action.removePrefix(SettingsManager.PREFIX_APP)
            remember(pkg) {
                try {
                    context.packageManager.getApplicationIcon(pkg)
                } catch (e: Exception) {
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
        action == SettingsManager.ACTION_FLASHLIGHT -> Icons.Rounded.FlashlightOn
        action == SettingsManager.ACTION_SCREENSHOT -> Icons.Rounded.Screenshot
        action == SettingsManager.ACTION_QUICK_SETTINGS -> Icons.Rounded.Settings
        action == SettingsManager.ACTION_LAST_APP -> Icons.Rounded.Repeat
        action == SettingsManager.ACTION_APP_INFO -> Icons.Rounded.Info
        action == SettingsManager.ACTION_POWER_DIALOG -> Icons.Rounded.PowerSettingsNew
        action == SettingsManager.ACTION_GOOGLE_SEARCH -> Icons.Rounded.Search
        action == SettingsManager.ACTION_ASSISTANT -> Icons.Rounded.Assistant
        action == SettingsManager.ACTION_SCROLL_UP -> Icons.Rounded.KeyboardArrowUp
        action == SettingsManager.ACTION_SCROLL_DOWN -> Icons.Rounded.KeyboardArrowDown
        action == SettingsManager.ACTION_SCROLL_UP_SMOOTH -> Icons.Rounded.KeyboardDoubleArrowUp
        action == SettingsManager.ACTION_SCROLL_DOWN_SMOOTH -> Icons.Rounded.KeyboardDoubleArrowDown
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
        action == SettingsManager.ACTION_DATA_TOGGLE -> Icons.Rounded.DataUsage
        action == SettingsManager.ACTION_NFC_TOGGLE -> Icons.Rounded.Nfc
        action == SettingsManager.ACTION_LOCATION_TOGGLE -> Icons.Rounded.LocationOn
        action == SettingsManager.ACTION_ROTATE_TOGGLE -> Icons.Rounded.ScreenRotation
        action == SettingsManager.ACTION_ROTATE_360 -> Icons.AutoMirrored.Rounded.RotateRight
        action == SettingsManager.ACTION_AUTOROTATE_TOGGLE -> Icons.Rounded.ScreenRotation
        action == SettingsManager.ACTION_VOLUME_UP -> Icons.AutoMirrored.Rounded.VolumeUp
        action == SettingsManager.ACTION_VOLUME_DOWN -> Icons.AutoMirrored.Rounded.VolumeDown
        action == SettingsManager.ACTION_MUTE_VOL -> Icons.AutoMirrored.Rounded.VolumeOff
        action == SettingsManager.ACTION_MUTE_MIC_TOGGLE -> Icons.Rounded.MicOff
        action == SettingsManager.ACTION_PREVIOUS -> Icons.Rounded.SkipPrevious
        action == SettingsManager.ACTION_NEXT -> Icons.Rounded.SkipNext
        action == SettingsManager.ACTION_PLAY_PAUSE -> Icons.Rounded.PlayArrow
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
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Actions", "Apps", "Shortcuts", "System", "Media")

    Column(modifier = modifier
        .fillMaxSize()
        .background(Color.Black)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                "Select Action for ${config.type}",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
        }

        PrimaryScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Black,
            contentColor = Color.White,
            edgePadding = 16.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTabIndex) {
            0 -> ActionsTab(config, onActionSelected)
            1 -> AppsTab(config, onActionSelected)
            2 -> ShortcutsTab(config, onActionSelected)
            3 -> SystemTab(config, onActionSelected)
            4 -> MediaTab(config, onActionSelected)
        }
    }
}

@Composable
fun ActionsTab(config: ActionConfig, onActionSelected: (String) -> Unit) {
    val actions = listOf(
        SettingsManager.ACTION_NONE,
        SettingsManager.ACTION_DEFAULT,
        SettingsManager.ACTION_HOME,
        SettingsManager.ACTION_BACK,
        SettingsManager.ACTION_RECENTS,
        SettingsManager.ACTION_SHOW_MENU,
        SettingsManager.ACTION_LOCK,
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
        SettingsManager.ACTION_COPY,
        SettingsManager.ACTION_CUT,
        SettingsManager.ACTION_PASTE,
        SettingsManager.ACTION_SPEED_DIAL,
        SettingsManager.ACTION_URL,
        SettingsManager.ACTION_QR_CODE
    )
    ActionList(actions, config, onActionSelected)
}

@Composable
fun AppsTab(config: ActionConfig, onActionSelected: (String) -> Unit) {
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

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(apps) { app ->
            ListItem(
                headlineContent = { Text(app.name, color = Color.White) },
                supportingContent = { Text(app.packageName, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) },
                leadingContent = {
                    val bitmap = remember(app.packageName) { app.icon.toBitmap().asImageBitmap() }
                    Image(bitmap, contentDescription = null, modifier = Modifier.size(40.dp))
                },
                modifier = Modifier.clickable {
                    SettingsManager.setAction(context, config.keyCode, config.state, config.type, SettingsManager.PREFIX_APP + app.packageName)
                    onActionSelected(app.name)
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

data class AppItem(val name: String, val packageName: String, val icon: Drawable)

@Composable
fun ShortcutsTab(config: ActionConfig, onActionSelected: (String) -> Unit) {
    val context = LocalContext.current
    val pm = context.packageManager
    val scope = rememberCoroutineScope()
    val shortcutApps = remember { mutableStateListOf<AppItem>() }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            val intent = Intent(Intent.ACTION_CREATE_SHORTCUT)
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            val appList = resolveInfos.map {
                AppItem(
                    it.loadLabel(pm).toString(),
                    it.activityInfo.packageName,
                    it.loadIcon(pm)
                )
            }.distinctBy { it.packageName }.sortedBy { it.name }
            withContext(Dispatchers.Main) {
                shortcutApps.clear()
                shortcutApps.addAll(appList)
            }
        }
    }

    if (shortcutApps.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No shortcut-capable apps found", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(shortcutApps) { app ->
                ListItem(
                    headlineContent = { Text(app.name, color = Color.White) },
                    supportingContent = { Text(app.packageName, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) },
                    leadingContent = {
                        val bitmap = remember(app.packageName) { app.icon.toBitmap().asImageBitmap() }
                        Image(bitmap, contentDescription = null, modifier = Modifier.size(40.dp))
                    },
                    modifier = Modifier.clickable {
                        SettingsManager.setAction(context, config.keyCode, config.state, config.type, SettingsManager.PREFIX_SHORTCUT + app.packageName + "||" + app.name)
                        onActionSelected(app.name)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
fun SystemTab(config: ActionConfig, onActionSelected: (String) -> Unit) {
    val actions = listOf(
        SettingsManager.ACTION_VIBRATE_RINGER,
        SettingsManager.ACTION_DND,
        SettingsManager.ACTION_NOTIFICATIONS,
        SettingsManager.ACTION_BRIGHTNESS_UP,
        SettingsManager.ACTION_BRIGHTNESS_DOWN,
        SettingsManager.ACTION_AUTO_BRIGHTNESS_TOGGLE,
        SettingsManager.ACTION_WIFI_TOGGLE,
        SettingsManager.ACTION_DATA_TOGGLE,
        SettingsManager.ACTION_NFC_TOGGLE,
        SettingsManager.ACTION_LOCATION_TOGGLE,
        SettingsManager.ACTION_ROTATE_TOGGLE,
        SettingsManager.ACTION_ROTATE_360,
        SettingsManager.ACTION_AUTOROTATE_TOGGLE
    )
    ActionList(actions, config, onActionSelected)
}

@Composable
fun MediaTab(config: ActionConfig, onActionSelected: (String) -> Unit) {
    val actions = listOf(
        SettingsManager.ACTION_VOLUME_UP,
        SettingsManager.ACTION_VOLUME_DOWN,
        SettingsManager.ACTION_MUTE_VOL,
        SettingsManager.ACTION_MUTE_MIC_TOGGLE,
        SettingsManager.ACTION_PREVIOUS,
        SettingsManager.ACTION_NEXT,
        SettingsManager.ACTION_PLAY_PAUSE,
    )
    ActionList(actions, config, onActionSelected)
}

@Composable
fun ActionList(actions: List<String>, config: ActionConfig, onActionSelected: (String) -> Unit) {
    val context = LocalContext.current
    var showInputDialog by remember { mutableStateOf<String?>(null) }
    var inputValue by remember { mutableStateOf("") }

    if (showInputDialog != null) {
        val title = when(showInputDialog) {
            SettingsManager.ACTION_SPEED_DIAL -> "Enter Number"
            SettingsManager.ACTION_URL -> "Enter URL"
            SettingsManager.ACTION_QR_CODE -> "Enter QR Code Content"
            else -> ""
        }
        val label = when(showInputDialog) {
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
                val prefix = when(showInputDialog) {
                    SettingsManager.ACTION_SPEED_DIAL -> SettingsManager.PREFIX_SPEED_DIAL
                    SettingsManager.ACTION_URL -> SettingsManager.PREFIX_URL
                    SettingsManager.ACTION_QR_CODE -> SettingsManager.PREFIX_QR_CODE
                    else -> ""
                }
                SettingsManager.setAction(context, config.keyCode, config.state, config.type, prefix + inputValue)
                onActionSelected(when(showInputDialog) {
                    SettingsManager.ACTION_SPEED_DIAL -> "Speed Dial"
                    SettingsManager.ACTION_URL -> "URL"
                    SettingsManager.ACTION_QR_CODE -> "QR Code"
                    else -> ""
                })
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

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(actions) { action ->
            val displayName = action.split("_").joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
            
            ListItem(
                headlineContent = { Text(displayName, color = Color.White) },
                leadingContent = {
                    ActionIcon(action = action, modifier = Modifier.size(24.dp), tint = Color.White)
                },
                modifier = Modifier.clickable {
                    if (action == SettingsManager.ACTION_SPEED_DIAL || action == SettingsManager.ACTION_URL || action == SettingsManager.ACTION_QR_CODE) {
                        showInputDialog = action
                        val currentSavedAction = SettingsManager.getAction(context, config.keyCode, config.state, config.type)
                        val prefix = when(action) {
                            SettingsManager.ACTION_SPEED_DIAL -> SettingsManager.PREFIX_SPEED_DIAL
                            SettingsManager.ACTION_URL -> SettingsManager.PREFIX_URL
                            SettingsManager.ACTION_QR_CODE -> SettingsManager.PREFIX_QR_CODE
                            else -> ""
                        }
                        inputValue = if (currentSavedAction.startsWith(prefix)) currentSavedAction.removePrefix(prefix) else ""
                    } else {
                        SettingsManager.setAction(context, config.keyCode, config.state, config.type, action)
                        onActionSelected(displayName)
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}
