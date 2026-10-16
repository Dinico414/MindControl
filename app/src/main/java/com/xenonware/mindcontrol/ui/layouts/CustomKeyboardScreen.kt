package com.xenonware.mindcontrol.ui.layouts

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.rounded.KeyboardReturn
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.VideogameAsset
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenonware.mindcontrol.ButtonState
import com.xenonware.mindcontrol.R
import com.xenonware.mindcontrol.SettingsManager
import com.xenonware.mindcontrol.ui.theme.GreenTheme
import com.xenonware.mindcontrol.ui.theme.Palette
import com.xenonware.mindcontrol.ui.theme.PaletteTheme

private val letterKeyCodes: Map<Char, Int> = mapOf(
    'a' to 29, 'b' to 30, 'c' to 31, 'd' to 32, 'e' to 33, 'f' to 34,
    'g' to 35, 'h' to 36, 'i' to 37, 'j' to 38, 'k' to 39, 'l' to 40,
    'm' to 41, 'n' to 42, 'o' to 43, 'p' to 44, 'q' to 45, 'r' to 46,
    's' to 47, 't' to 48, 'u' to 49, 'v' to 50, 'w' to 51, 'x' to 52,
    'y' to 53, 'z' to 54, '[' to 71, 'ö' to 74, '\'' to 75, 'ñ' to 120
)

private fun letter(c: Char) = KeyInfo(c.toString(), keyCode = letterKeyCodes[c])

private fun KeyInfo.configName(): String = when {
    label == "⌫" -> "Backspace"
    label == "," -> "Comma"
    label == "." -> "Period"
    label == "[" -> "Left_Bracket"
    label == "'" -> "Apostrophe"
    label.length == 1 -> label.uppercase()
    else -> label
}

/** Sentinel labels so the UI buttons can be recognised without string matching. */
private const val LAYOUT_BUTTON_LABEL = "__layout__"
private const val BACK_BUTTON_LABEL = "__back__"

/** Drawable for a binding's icon, shown on the key it is assigned to. */
@androidx.annotation.DrawableRes
private fun gamepadActionIconRes(action: SettingsManager.GamepadAction): Int? = action.iconRes



@Composable
fun CustomKeyboardScreen(
    modifier: Modifier = Modifier,
    devicePalette: Palette,
    shellReady: Boolean = false,
    gamepadMode: Boolean = false,
    onGamepadModeChange: (Boolean) -> Unit = {},
    gamepadProfile: Int = 1,
    onGamepadProfileChange: (Int) -> Unit = {},
    onOpenAppPicker: () -> Unit = {},
    onBack: () -> Unit,
    onKeySelected: (Int, String) -> Unit
) {
    val context = LocalContext.current
    var currentLayout by remember { mutableStateOf(KeyboardLayout.QWERTY) }
    var bindings by remember { mutableStateOf(emptyMap<Int, SettingsManager.GamepadAction>()) }
    val pressedKeys by ButtonState.pressedKeys.collectAsState()

    // Gamepad mode only works with Shizuku/root. If it is unavailable the mode falls back to the
    // keyboard, so the header and the key rows can never disagree with each other.
    val effectiveGamepadMode = gamepadMode && shellReady

    val gap = 8.dp
    val columns = 10

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            // This screen does not use ActivityScreen, so it applies the safe-drawing insets
            // itself. The caller passes a bare modifier, exactly as it does for the action and
            // button-config screens.
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(vertical = 12.dp)
    ) {
        val unitWidth = (maxWidth - gap * (columns - 1)) / columns
        fun keyWidth(weight: Float): Dp = unitWidth * weight + gap * (weight - 1)

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                // Without Shizuku/root the gamepad toggle cannot work, so the back button takes
                // the freed width instead and the row still fills the screen (7 + 3).
                val backWeight = if (shellReady) 5f else 7f
                PaletteTheme(palette = devicePalette) {
                    KeyButton(
                        key = KeyInfo(BACK_BUTTON_LABEL, weight = backWeight, onClick = onBack),
                        displayLabel = "Back to system buttons",
                        modifier = Modifier.width(keyWidth(backWeight)).clip(RoundedCornerShape(16.dp)),
                        onKeyConfigure = onKeySelected
                    )
                }

                // Rounded icon button that both indicates and switches the mode.
                // Keyboard mode -> controller icon, green.  Gamepad mode -> keyboard icon,
                // in the keyboard's own colour.
                if (shellReady) {
                    val iconTheme: @Composable (@Composable () -> Unit) -> Unit =
                        if (effectiveGamepadMode) ({ content -> content() })
                        else ({ content -> GreenTheme { content() } })
                    iconTheme {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .width(keyWidth(2f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(100f))
                                .clickable { onGamepadModeChange(!gamepadMode) }
                        ) {
                            Crossfade(
                                targetState = effectiveGamepadMode,
                                animationSpec = tween(durationMillis = 350),
                                label = "keyboardGamepadIcon"
                            ) { isGamepad ->
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isGamepad) Icons.Rounded.Keyboard else Icons.Rounded.VideogameAsset,
                                        contentDescription = stringResource(
                                            if (isGamepad) R.string.keyboard_mode else R.string.gamepad_mode
                                        ),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Profile button: only in gamepad mode, 1 wide, sits between the mode switch and
                // Apps. Shows the active profile as a numbered counter icon and cycles
                // 1 -> 2 -> 3 -> 1 on tap. The profile only affects controller bindings, never
                // the keyboard.
                if (effectiveGamepadMode) {
                    // Painted green like the rest of the gamepad-mode controls, so the whole
                    // header reads as one mode rather than mixing in the keyboard palette.
                    GreenTheme {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .width(keyWidth(1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp, topEnd = 4.dp, bottomEnd = 4.dp))
                                .clickable { onGamepadProfileChange((gamepadProfile % 3) + 1) }
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(
                                        SettingsManager.profileIconRes(gamepadProfile)
                                    ),
                                    contentDescription = stringResource(
                                        R.string.gamepad_profile_label
                                    ) + " " + gamepadProfile,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // One button, two roles: in gamepad mode it opens the app picker, otherwise it
                // cycles the typing layout. Two wide in gamepad mode (5 + 2 + 1 + 2), and the
                // full 3 wide otherwise so the row still fills the screen (7 + 3 without Shizuku).
                val appsWeight = if (effectiveGamepadMode) 2f else 3f
                val secondaryTheme: @Composable (@Composable () -> Unit) -> Unit =
                    if (effectiveGamepadMode) ({ content -> GreenTheme { content() } })
                    else ({ content -> content() })
                secondaryTheme {
                    KeyButton(
                        key = KeyInfo(LAYOUT_BUTTON_LABEL, weight = appsWeight, onClick = {
                            if (effectiveGamepadMode) onOpenAppPicker()
                            else currentLayout = currentLayout.next()
                        }),
                        displayLabel = if (effectiveGamepadMode) "Apps" else currentLayout.displayName,
                        modifier = Modifier.width(keyWidth(appsWeight)).clip(if (effectiveGamepadMode) RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp) else RoundedCornerShape(16.dp)),
                        onKeyConfigure = onKeySelected
                    )
                }
            }

            // In gamepad mode the whole keyboard is drawn green, overriding the chosen palette.
            val rowsTheme: @Composable (@Composable () -> Unit) -> Unit =
                if (effectiveGamepadMode) ({ content -> GreenTheme { content() } })
                else ({ content -> content() })

            rowsTheme {
                getRowsForLayout(currentLayout).forEach { row ->
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        row.forEach { key ->
                            val bound = key.keyCode?.let { bindings[it] }
                            KeyButton(
                                key = key,
                                modifier = Modifier.width(keyWidth(key.weight)),
                                isHardwarePressed = key.keyCode != null && key.keyCode in pressedKeys,
                                // The assigned controller action is shown as its icon under the
                                // letter, and unassigned keys are dimmed because they do nothing.
                                subIconRes = if (effectiveGamepadMode) bound?.let(::gamepadActionIconRes) else null,
                                dimmed = effectiveGamepadMode && bound == null,
                                onKeyConfigure = onKeySelected
                            )
                        }
                    }
                }
            }
        }

        // Re-read the bindings when the layout changes, so a value assigned in the picker shows up
        // as soon as the user comes back.
        LaunchedEffect(currentLayout, effectiveGamepadMode, gamepadProfile) {
            bindings = getRowsForLayout(currentLayout)
                .flatten()
                .mapNotNull { it.keyCode }
                .mapNotNull { code ->
                    SettingsManager.getGamepadBinding(context, gamepadProfile, code)?.let { code to it }
                }
                .toMap()
        }
    }
}

data class KeyInfo(
    val label: String,
    val weight: Float = 1f,
    val isPill: Boolean = false,
    val icon: ImageVector? = null,
    val keyCode: Int? = null,
    val onClick: (() -> Unit)? = null
)

@Composable
fun KeyButton(
    key: KeyInfo,
    modifier: Modifier = Modifier,
    isHardwarePressed: Boolean = false,
    displayLabel: String? = null,
    /** Icon of the controller action assigned to this key, shown under the glyph. */
    @androidx.annotation.DrawableRes subIconRes: Int? = null,
    dimmed: Boolean = false,
    onKeyConfigure: (Int, String) -> Unit = { _, _ -> }
) {
    // Visually dim only the decorative keys that exist for looks (123, Fn …).
    // The header buttons are real controls, so they are recognised by their sentinel labels
    // rather than by their visible text -- that text is passed in as [displayLabel] and changes
    // with the mode, which previously made these buttons look disabled.
    val isUiButton = key.label == BACK_BUTTON_LABEL || key.label == LAYOUT_BUTTON_LABEL
    val isDisabled = key.keyCode == null && key.onClick != null && !isUiButton

    val labelForDisplay = displayLabel ?: key.label
    val displayedLabel = remember(labelForDisplay) {
        if (labelForDisplay.length == 1) labelForDisplay.uppercase() else labelForDisplay
    }

    // [dimmed] is the gamepad "nothing assigned" state: the key does nothing and reads softer.
    val keyAlpha = when {
        dimmed -> 0.75f
        isDisabled -> 0.3f
        else -> 1f
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(if (key.isPill) RoundedCornerShape(50) else RoundedCornerShape(4.dp))
            .background(
                when {
                    isHardwarePressed -> MaterialTheme.colorScheme.primary
                    isDisabled -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    dimmed -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                    else -> MaterialTheme.colorScheme.primaryContainer
                }
            )
            .clickable(
                enabled = !isDisabled
            ) {
                when {
                    key.onClick != null -> key.onClick.invoke()
                    key.keyCode != null -> onKeyConfigure(key.keyCode, key.configName())
                    else -> {}
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // The column wraps both kinds of key so the binding sub-icon is shared: without this an
        // icon key (Backspace, Enter) could never show what it is mapped to. When a controller
        // action is bound the two swap emphasis: the glyph carries the softer tint and the bound
        // action's icon takes the full-strength one, so the mapping becomes the focal point.
        // Nudged down a touch when a binding is present, so the stacked icon+glyph pair sits
        // optically centred in the key.
        val hasBinding = subIconRes != null
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(2.dp).offset(y = if (hasBinding) 4.dp else 0.dp)
        ) {
            // The assigned controller action sits above the glyph, with a little breathing room
            // between the two. Drawing the action's own icon is clearer than repeating its label.
            if (subIconRes != null) {
                Icon(
                    painter = painterResource(subIconRes),
                    contentDescription = null,
                    tint = if (isHardwarePressed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = keyAlpha),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.height(6.dp))
            }

            val glyphColor = if (isHardwarePressed) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onPrimaryContainer.copy(
                    // A key with a binding lets its glyph recede so the action icon stands out.
                    alpha = if (hasBinding) 0.6f else keyAlpha
                )

            if (key.icon != null) {
                Icon(
                    imageVector = key.icon,
                    contentDescription = labelForDisplay,
                    tint = glyphColor,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                // A bound key shrinks its letter by 5sp to make room for the icon above it.
                val baseFontSize = if (labelForDisplay.length > 1) 14.sp else 18.sp
                Text(
                    text = displayedLabel,
                    color = glyphColor,
                    fontSize = if (hasBinding) (baseFontSize.value - 5).sp else baseFontSize,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    fontFamily = QuicksandTitleVariable
                )
            }
        }
    }
}

enum class KeyboardLayout(val displayName: String) {
    QWERTY("QWERTY"), QWERTY_ES("QWERTY 2"), QWERTZ("QWERTZ"), AZERTY("AZERTY");
    fun next(): KeyboardLayout = entries[(ordinal + 1) % entries.size]
}

fun getRowsForLayout(layout: KeyboardLayout): List<List<KeyInfo>> {
    val shift     = KeyInfo("Shift", 1.5f, keyCode = 59)
    val backspace = KeyInfo("⌫", 1.5f, icon = Icons.AutoMirrored.Outlined.Backspace, keyCode = 67)
    val space     = KeyInfo("Space", 3f, keyCode = 62)
    val enter     = KeyInfo("Enter", 1.5f, isPill = true,
        icon = Icons.AutoMirrored.Rounded.KeyboardReturn, keyCode = 66)
    val leftBracket = KeyInfo("[", keyCode = 71)
    val lan = KeyInfo("Lan", icon = Icons.Rounded.Language, keyCode = 113)
    val comma     = KeyInfo(",", keyCode = 55)
    val period    = KeyInfo(".", keyCode = 56)

    // Helper to create keys that exist visually but are not clickable/mappable
    fun disabledKey(label: String, weight: Float = 1f, icon: ImageVector? = null, isPill: Boolean = false) =
        KeyInfo(label, weight, isPill, icon, keyCode = null, onClick = {})

    return when (layout) {
        KeyboardLayout.QWERTY -> listOf(
            "qwertyuiop".map { letter(it) },
            "asdfghjkl".map { letter(it) } + listOf(leftBracket),
            listOf(shift) + "zxcvbnm".map { letter(it) } + listOf(backspace),
            listOf(disabledKey("123", 1.5f, isPill = true), comma, lan, space, disabledKey("Fn"), period, enter)
        )
        KeyboardLayout.QWERTY_ES -> listOf(
            "qwertyuiop".map { letter(it) },
            "asdfghjkl".map { letter(it) } + letter('ñ'),
            listOf(shift) + "zxcvbnm".map { letter(it) } + listOf(backspace),
            listOf(disabledKey("123", 1.5f, isPill = true), comma, lan, space, disabledKey("Fn"), period, enter)
        )
        KeyboardLayout.QWERTZ -> listOf(
            "qwertzuiop".map { letter(it) },
            "asdfghjklö".map { letter(it) },
            listOf(shift) + "yxcvbnm".map { letter(it) } + listOf(backspace),
            listOf(disabledKey("123", 1.5f, isPill = true), comma, lan, space, disabledKey("Fn"), period, enter)
        )
        KeyboardLayout.AZERTY -> listOf(
            "azertyuiop".map { letter(it) },
            "qsdfghjklm".map { letter(it) } + listOf(leftBracket),
            listOf(shift) + "wxcvbn'".map { letter(it) } + listOf(backspace),
            listOf(disabledKey("123", 1.5f, isPill = true), comma, lan, space, disabledKey("Fn"), period, enter)
        )
    }
}