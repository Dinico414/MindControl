package com.xenonware.mindcontrol.ui.layouts

import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.xenonware.mindcontrol.ShellManager
import com.xenonware.mindcontrol.ui.theme.BlueTheme
import com.xenonware.mindcontrol.ui.theme.GreenTheme
import com.xenonware.mindcontrol.ui.theme.Palette
import com.xenonware.mindcontrol.ui.theme.PaletteTheme
import com.xenonware.mindcontrol.ui.theme.RedTheme
import com.xenonware.mindcontrol.ui.theme.YellowTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

val PairSaver = listSaver<Pair<Int, String>?, Any>(
    save = { if (it == null) emptyList() else listOf(it.first, it.second) },
    restore = { if (it.isEmpty()) null else (it[0] as Int) to (it[1] as String) }
)

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

    var shellPermission by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        while (true) {
            shellPermission = ShellManager.isAvailable()
            delay(2000.milliseconds)
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
                        shellPermission = shellPermission,
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