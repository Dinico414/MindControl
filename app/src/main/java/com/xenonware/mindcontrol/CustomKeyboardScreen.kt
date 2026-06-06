package com.xenonware.mindcontrol

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.rounded.KeyboardReturn
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenonware.mindcontrol.ui.theme.Palette
import com.xenonware.mindcontrol.ui.theme.PaletteTheme

private val letterKeyCodes: Map<Char, Int> = mapOf(
    'a' to 29, 'b' to 30, 'c' to 31, 'd' to 32, 'e' to 33, 'f' to 34,
    'g' to 35, 'h' to 36, 'i' to 37, 'j' to 38, 'k' to 39, 'l' to 40,
    'm' to 41, 'n' to 42, 'o' to 43, 'p' to 44, 'q' to 45, 'r' to 46,
    's' to 47, 't' to 48, 'u' to 49, 'v' to 50, 'w' to 51, 'x' to 52,
    'y' to 53, 'z' to 54, 'ö' to 74, '\'' to 75, 'ñ' to 120
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

@Composable
fun CustomKeyboardScreen(
    modifier: Modifier = Modifier,
    devicePalette: Palette,
    onBack: () -> Unit,
    onKeySelected: (Int, String) -> Unit
) {
    var currentLayout by remember { mutableStateOf(KeyboardLayout.QWERTY) }
    val pressedKeys by ButtonState.pressedKeys.collectAsState()

    val gap = 8.dp
    val columns = 10

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
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
                PaletteTheme(palette = devicePalette) {
                    KeyButton(
                        key = KeyInfo("Back to system buttons", weight = 7f, onClick = onBack),
                        modifier = Modifier.width(keyWidth(7f)).clip(RoundedCornerShape(16.dp)),
                        onKeyConfigure = onKeySelected
                    )
                }
                KeyButton(
                    key = KeyInfo(currentLayout.displayName, weight = 3f, onClick = {
                        currentLayout = currentLayout.next()
                    }),
                    modifier = Modifier.width(keyWidth(3f)).clip(RoundedCornerShape(16.dp)),
                    onKeyConfigure = onKeySelected
                )
            }

            getRowsForLayout(currentLayout).forEach { row ->
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    row.forEach { key ->
                        KeyButton(
                            key = key,
                            modifier = Modifier.width(keyWidth(key.weight)),
                            isHardwarePressed = key.keyCode != null && key.keyCode in pressedKeys,
                            onKeyConfigure = onKeySelected
                        )
                    }
                }
            }
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
    onKeyConfigure: (Int, String) -> Unit = { _, _ -> }
) {
    // Logic to visually dim disabled buttons
    val isDisabled = key.keyCode == null && key.onClick != null && key.label != "Back to system buttons" && !key.label.contains(KeyboardLayout.QWERTY.displayName, true) && !key.label.contains("QWERTZ", true) && !key.label.contains("AZERTY", true)

    val displayedLabel = remember(key.label) {
        if (key.label.length == 1) key.label.uppercase() else key.label
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(if (key.isPill) RoundedCornerShape(50) else RoundedCornerShape(4.dp))
            .background(
                when {
                    isHardwarePressed -> MaterialTheme.colorScheme.primary
                    isDisabled -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
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
        if (key.icon != null) {
            Icon(
                imageVector = key.icon,
                contentDescription = key.label,
                tint = if (isHardwarePressed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = if (isDisabled) 0.3f else 1f),
                modifier = Modifier.size(20.dp)
            )
        } else {
            Text(
                text = displayedLabel,
                color = if (isHardwarePressed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = if (isDisabled) 0.3f else 1f),
                fontSize = if (key.label.length > 1) 14.sp else 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                fontFamily = QuicksandTitleVariable
            )
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
    val comma     = KeyInfo(",", keyCode = 55)
    val period    = KeyInfo(".", keyCode = 56)

    // Helper to create keys that exist visually but are not clickable/mappable
    fun disabledKey(label: String, weight: Float = 1f, icon: ImageVector? = null, isPill: Boolean = false) =
        KeyInfo(label, weight, isPill, icon, keyCode = null, onClick = {})

    return when (layout) {
        KeyboardLayout.QWERTY -> listOf(
            "qwertyuiop".map { letter(it) },
            "asdfghjkl".map { letter(it) } + disabledKey("["),
            listOf(shift) + "zxcvbnm".map { letter(it) } + listOf(backspace),
            listOf(disabledKey("123", 1.5f, isPill = true), comma,
                disabledKey("Lan", icon = Icons.Rounded.Language),
                space, disabledKey("Fn"), period, enter)
        )
        KeyboardLayout.QWERTY_ES -> listOf(
            "qwertyuiop".map { letter(it) },
            "asdfghjkl".map { letter(it) } + letter('ñ'),
            listOf(shift) + "zxcvbnm".map { letter(it) } + listOf(backspace),
            listOf(disabledKey("123", 1.5f, isPill = true), comma,
                disabledKey("Lan", icon = Icons.Rounded.Language),
                space, disabledKey("Fn"), period, enter)
        )
        KeyboardLayout.QWERTZ -> listOf(
            "qwertzuiop".map { letter(it) },
            "asdfghjklö".map { letter(it) },
            listOf(shift) + "yxcvbnm".map { letter(it) } + listOf(backspace),
            listOf(disabledKey("123", 1.5f, isPill = true), comma,
                disabledKey("Lan", icon = Icons.Rounded.Language),
                space, disabledKey("Fn"), period, enter)
        )
        KeyboardLayout.AZERTY -> listOf(
            "azertyuiop".map { letter(it) },
            "qsdfghjklm".map { letter(it) } + disabledKey("["),
            listOf(shift) + "wxcvbn'".map { letter(it) } + listOf(backspace),
            listOf(disabledKey("123", 1.5f, isPill = true), comma,
                disabledKey("Lan", icon = Icons.Rounded.Language),
                space, disabledKey("Fn"), period, enter)
        )
    }
}