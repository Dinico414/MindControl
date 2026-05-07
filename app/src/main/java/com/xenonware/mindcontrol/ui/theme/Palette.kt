package com.xenonware.mindcontrol.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class Palette {
    Black,
    White,
    Pink,
    Blue,
    Yellow;

    companion object {
        fun fromKey(key: String?): Palette =
            entries.firstOrNull { it.name == key } ?: Black
    }
}

// ───────── BLACK ─────────
private val palBlackLight = lightColorScheme(
    primary = Color(0xFF171A1A), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF2C2F2F), onPrimaryContainer = Color(0xFF949696),
    secondary = Color(0xFF5E5E5E), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE3E2E1), onSecondaryContainer = Color(0xFF646464),
    tertiary = Color(0xFF1C1B1B), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF302E31), onTertiaryContainer = Color(0xFF999599),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFFCF8F8), onBackground = Color(0xFF1C1B1B),
    surface = Color(0xFFFCF8F8), onSurface = Color(0xFF1C1B1B),
    surfaceVariant = Color(0xFFE0E3E3), onSurfaceVariant = Color(0xFF434848),
    outline = Color(0xFF747878), outlineVariant = Color(0xFFC4C7C7),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF313030), inverseOnSurface = Color(0xFFF3F0EF),
    inversePrimary = Color(0xFFC5C7C6),
    surfaceDim = Color(0xFFDCD9D9), surfaceBright = Color(0xFFFCF8F8),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF6F3F2),
    surfaceContainer = Color(0xFFF1EDEC), surfaceContainerHigh = Color(0xFFEBE7E7),
    surfaceContainerHighest = Color(0xFFE5E2E1),
)
private val palBlackDark = darkColorScheme(
    primary = Color(0xFFC5C7C6), onPrimary = Color(0xFF2E3131),
    primaryContainer = Color(0xFF2C2F2F), onPrimaryContainer = Color(0xFF949696),
    secondary = Color(0xFFC7C6C6), onSecondary = Color(0xFF303030),
    secondaryContainer = Color(0xFF464747), onSecondaryContainer = Color(0xFFB6B5B4),
    tertiary = Color(0xFF1C1B1B), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF302E31), onTertiaryContainer = Color(0xFF999599),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF131313), onBackground = Color(0xFFE5E2E1),
    surface = Color(0xFF131313), onSurface = Color(0xFFE5E2E1),
    surfaceVariant = Color(0xFF434848), onSurfaceVariant = Color(0xFFC4C7C7),
    outline = Color(0xFF8E9191), outlineVariant = Color(0xFF434848),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE5E2E1), inverseOnSurface = Color(0xFF313030),
    inversePrimary = Color(0xFF5C5F5F),
    surfaceDim = Color(0xFF131313), surfaceBright = Color(0xFF3A3939),
    surfaceContainerLowest = Color(0xFF0E0E0E), surfaceContainerLow = Color(0xFF1C1B1B),
    surfaceContainer = Color(0xFF201F1F), surfaceContainerHigh = Color(0xFF2A2A2A),
    surfaceContainerHighest = Color(0xFF353534),
)

// ───────── WHITE ─────────
private val palWhiteLight = lightColorScheme(
    primary = Color(0xFF5D5F5F), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFFFFF), onPrimaryContainer = Color(0xFF747676),
    secondary = Color(0xFF5E5E5E), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE4E2E2), onSecondaryContainer = Color(0xFF646464),
    tertiary = Color(0xFFFFFFFF), onTertiary = Color(0xFF1C1B1B),
    tertiaryContainer = Color(0xFFFFFFFF), onTertiaryContainer = Color(0xFF747676),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFFCF8F8), onBackground = Color(0xFF1C1B1B),
    surface = Color(0xFFFCF8F8), onSurface = Color(0xFF1C1B1B),
    surfaceVariant = Color(0xFFE0E3E3), onSurfaceVariant = Color(0xFF444748),
    outline = Color(0xFF747878), outlineVariant = Color(0xFFC4C7C8),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF313030), inverseOnSurface = Color(0xFFF4F0EF),
    inversePrimary = Color(0xFFC6C6C7),
    surfaceDim = Color(0xFFDDD9D9), surfaceBright = Color(0xFFFCF8F8),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF6F3F2),
    surfaceContainer = Color(0xFFF1EDEC), surfaceContainerHigh = Color(0xFFEBE7E7),
    surfaceContainerHighest = Color(0xFFE5E2E1),
)
private val palWhiteDark = darkColorScheme(
    primary = Color(0xFFFFFFFF), onPrimary = Color(0xFF2F3131),
    primaryContainer = Color(0xFFE2E2E2), onPrimaryContainer = Color(0xFF636565),
    secondary = Color(0xFFC8C6C6), onSecondary = Color(0xFF303030),
    secondaryContainer = Color(0xFF494949), onSecondaryContainer = Color(0xFFB9B8B8),
    tertiary = Color(0xFFEE6398), onTertiary = Color(0xFF1C1B1B),
    tertiaryContainer = Color(0xFFFFDCC1), onTertiaryContainer = Color(0xFF623F20),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF141313), onBackground = Color(0xFFE5E2E1),
    surface = Color(0xFF141313), onSurface = Color(0xFFE5E2E1),
    surfaceVariant = Color(0xFF444748), onSurfaceVariant = Color(0xFFC4C7C8),
    outline = Color(0xFF8E9192), outlineVariant = Color(0xFF444748),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE5E2E1), inverseOnSurface = Color(0xFF313030),
    inversePrimary = Color(0xFF5D5F5F),
    surfaceDim = Color(0xFF141313), surfaceBright = Color(0xFF3A3939),
    surfaceContainerLowest = Color(0xFF0E0E0E), surfaceContainerLow = Color(0xFF1C1B1B),
    surfaceContainer = Color(0xFF201F1F), surfaceContainerHigh = Color(0xFF2A2A2A),
    surfaceContainerHighest = Color(0xFF353434),
)

// ───────── PINK ─────────
private val palPinkLight = lightColorScheme(
    primary = Color(0xFF8C4A60), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9E2), onPrimaryContainer = Color(0xFF703348),
    secondary = Color(0xFF74565F), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFD9E2), onSecondaryContainer = Color(0xFF5A3F47),
    tertiary = Color(0xFFEE6398), onTertiary = Color(0xFF1C1B1B),
    tertiaryContainer = Color(0xFFFFDCC1), onTertiaryContainer = Color(0xFF623F20),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFFFF8F8), onBackground = Color(0xFF22191C),
    surface = Color(0xFFFFF8F8), onSurface = Color(0xFF22191C),
    surfaceVariant = Color(0xFFF2DDE1), onSurfaceVariant = Color(0xFF514347),
    outline = Color(0xFF837377), outlineVariant = Color(0xFFD5C2C6),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF372E30), inverseOnSurface = Color(0xFFFDEDEF),
    inversePrimary = Color(0xFFFFB1C8),
    surfaceDim = Color(0xFFE6D6D9), surfaceBright = Color(0xFFFFF8F8),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFFFF0F2),
    surfaceContainer = Color(0xFFFAEAED), surfaceContainerHigh = Color(0xFFF5E4E7),
    surfaceContainerHighest = Color(0xFFEFDFE1),
)
private val palPinkDark = darkColorScheme(
    primary = Color(0xFFFFB1C8), onPrimary = Color(0xFF541D32),
    primaryContainer = Color(0xFF703348), onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = Color(0xFFE3BDC6), onSecondary = Color(0xFF422931),
    secondaryContainer = Color(0xFF5A3F47), onSecondaryContainer = Color(0xFFFFD9E2),
    tertiary = Color(0xFFEE6398), onTertiary = Color(0xFF1C1B1B),
    tertiaryContainer = Color(0xFF623F20), onTertiaryContainer = Color(0xFFFFDCC1),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF191113), onBackground = Color(0xFFEFDFE1),
    surface = Color(0xFF191113), onSurface = Color(0xFFEFDFE1),
    surfaceVariant = Color(0xFF514347), onSurfaceVariant = Color(0xFFD5C2C6),
    outline = Color(0xFF9E8C90), outlineVariant = Color(0xFF514347),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFEFDFE1), inverseOnSurface = Color(0xFF372E30),
    inversePrimary = Color(0xFF8C4A60),
    surfaceDim = Color(0xFF191113), surfaceBright = Color(0xFF413739),
    surfaceContainerLowest = Color(0xFF140C0E), surfaceContainerLow = Color(0xFF22191C),
    surfaceContainer = Color(0xFF261D20), surfaceContainerHigh = Color(0xFF31282A),
    surfaceContainerHighest = Color(0xFF3C3235),
)

// ───────── BLUE (palette) ─────────
private val palBlueLight = lightColorScheme(
    primary = Color(0xFF216487), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC6E7FF), onPrimaryContainer = Color(0xFF004C6C),
    secondary = Color(0xFF4F616E), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD2E5F4), onSecondaryContainer = Color(0xFF374955),
    tertiary = Color(0xFFABC7FF), onTertiary = Color(0xFF1C1B1B),
    tertiaryContainer = Color(0xFFE8DDFF), onTertiaryContainer = Color(0xFF4A4263),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFF6FAFE), onBackground = Color(0xFF181C20),
    surface = Color(0xFFF6FAFE), onSurface = Color(0xFF181C20),
    surfaceVariant = Color(0xFFDDE3EA), onSurfaceVariant = Color(0xFF41484D),
    outline = Color(0xFF71787E), outlineVariant = Color(0xFFC1C7CE),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF2D3135), inverseOnSurface = Color(0xFFEEF1F6),
    inversePrimary = Color(0xFF92CEF5),
    surfaceDim = Color(0xFFD7DADF), surfaceBright = Color(0xFFF6FAFE),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF0F4F8),
    surfaceContainer = Color(0xFFEBEEF3), surfaceContainerHigh = Color(0xFFE5E8ED),
    surfaceContainerHighest = Color(0xFFDFE3E7),
)
private val palBlueDark = darkColorScheme(
    primary = Color(0xFF92CEF5), onPrimary = Color(0xFF00344B),
    primaryContainer = Color(0xFF004C6C), onPrimaryContainer = Color(0xFFC6E7FF),
    secondary = Color(0xFFB6C9D8), onSecondary = Color(0xFF21323E),
    secondaryContainer = Color(0xFF374955), onSecondaryContainer = Color(0xFFD2E5F4),
    tertiary = Color(0xFFABC7FF), onTertiary = Color(0xFF1C1B1B),
    tertiaryContainer = Color(0xFF4A4263), onTertiaryContainer = Color(0xFFE8DDFF),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF101417), onBackground = Color(0xFFDFE3E7),
    surface = Color(0xFF101417), onSurface = Color(0xFFDFE3E7),
    surfaceVariant = Color(0xFF41484D), onSurfaceVariant = Color(0xFFC1C7CE),
    outline = Color(0xFF8B9198), outlineVariant = Color(0xFF41484D),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFDFE3E7), inverseOnSurface = Color(0xFF2D3135),
    inversePrimary = Color(0xFF216487),
    surfaceDim = Color(0xFF101417), surfaceBright = Color(0xFF353A3D),
    surfaceContainerLowest = Color(0xFF0A0F12), surfaceContainerLow = Color(0xFF181C20),
    surfaceContainer = Color(0xFF1C2024), surfaceContainerHigh = Color(0xFF262A2E),
    surfaceContainerHighest = Color(0xFF313539),
)

// ───────── YELLOW (palette, reuses Yellow* from Color.kt) ─────────
private val palYellowLight = lightColorScheme(
    primary = YellowPrimaryLight, onPrimary = YellowOnPrimaryLight,
    primaryContainer = YellowPrimaryContainerLight, onPrimaryContainer = YellowOnPrimaryContainerLight,
    secondary = YellowSecondaryLight, onSecondary = YellowOnSecondaryLight,
    secondaryContainer = YellowSecondaryContainerLight, onSecondaryContainer = YellowOnSecondaryContainerLight,
    tertiary = Color(0xFFFFDE3F), onTertiary = Color(0xFF1C1B1B),
    tertiaryContainer = Color(0xFFDEE9C1), onTertiaryContainer = Color(0xFF131F04),
    error = YellowErrorLight, onError = YellowOnErrorLight,
    errorContainer = YellowErrorContainerLight, onErrorContainer = YellowOnErrorContainerLight,
    background = YellowBackgroundLight, onBackground = YellowOnBackgroundLight,
    surface = YellowSurfaceLight, onSurface = YellowOnSurfaceLight,
    surfaceVariant = YellowSurfaceVariantLight, onSurfaceVariant = YellowOnSurfaceVariantLight,
    outline = YellowOutlineLight, outlineVariant = YellowOutlineVariantLight,
    scrim = YellowScrimLight,
    inverseSurface = YellowInverseSurfaceLight, inverseOnSurface = YellowInverseOnSurfaceLight,
    inversePrimary = YellowInversePrimaryLight,
    surfaceDim = YellowSurfaceDimLight, surfaceBright = YellowSurfaceBrightLight,
    surfaceContainerLowest = YellowSurfaceContainerLowestLight,
    surfaceContainerLow = YellowSurfaceContainerLowLight,
    surfaceContainer = YellowSurfaceContainerLight,
    surfaceContainerHigh = YellowSurfaceContainerHighLight,
    surfaceContainerHighest = YellowSurfaceContainerHighestLight,
)
private val palYellowDark = darkColorScheme(
    primary = YellowPrimaryDark, onPrimary = YellowOnPrimaryDark,
    primaryContainer = YellowPrimaryContainerDark, onPrimaryContainer = YellowOnPrimaryContainerDark,
    secondary = YellowSecondaryDark, onSecondary = YellowOnSecondaryDark,
    secondaryContainer = YellowSecondaryContainerDark, onSecondaryContainer = YellowOnSecondaryContainerDark,
    tertiary = Color(0xFFFFDE3F), onTertiary = Color(0xFF1C1B1B),
    tertiaryContainer = Color(0xFF424C2A), onTertiaryContainer = Color(0xFFDEE9C1),
    error = YellowErrorDark, onError = YellowOnErrorDark,
    errorContainer = YellowErrorContainerDark, onErrorContainer = YellowOnErrorContainerDark,
    background = YellowBackgroundDark, onBackground = YellowOnBackgroundDark,
    surface = YellowSurfaceDark, onSurface = YellowOnSurfaceDark,
    surfaceVariant = YellowSurfaceVariantDark, onSurfaceVariant = YellowOnSurfaceVariantDark,
    outline = YellowOutlineDark, outlineVariant = YellowOutlineVariantDark,
    scrim = YellowScrimDark,
    inverseSurface = YellowInverseSurfaceDark, inverseOnSurface = YellowInverseOnSurfaceDark,
    inversePrimary = YellowInversePrimaryDark,
    surfaceDim = YellowSurfaceDimDark, surfaceBright = YellowSurfaceBrightDark,
    surfaceContainerLowest = YellowSurfaceContainerLowestDark,
    surfaceContainerLow = YellowSurfaceContainerLowDark,
    surfaceContainer = YellowSurfaceContainerDark,
    surfaceContainerHigh = YellowSurfaceContainerHighDark,
    surfaceContainerHighest = YellowSurfaceContainerHighestDark,
)

@Composable
fun PaletteTheme(
    palette: Palette,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = when (palette) {
        Palette.Black  -> if (darkTheme) palBlackDark  else palBlackLight
        Palette.White  -> if (darkTheme) palWhiteDark  else palWhiteLight
        Palette.Pink   -> if (darkTheme) palPinkDark   else palPinkLight
        Palette.Blue   -> if (darkTheme) palBlueDark   else palBlueLight
        Palette.Yellow -> if (darkTheme) palYellowDark else palYellowLight
    }
    MaterialTheme(colorScheme = scheme, typography = Typography, content = content)
}

@Composable
fun PaletteSwatch(
    palette: Palette,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = MaterialTheme.colorScheme.onSurface
    PaletteTheme(palette) {
        val swatch = MaterialTheme.colorScheme
        Box(
            modifier = modifier
                .height(48.dp)
                .clip(RoundedCornerShape(50))
                .background(swatch.tertiary)
                .border(2.dp, borderColor, RoundedCornerShape(50))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Selected",
                    tint = swatch.onTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun PaletteRow(
    modifier: Modifier = Modifier,
    label: String,
    selected: Palette,
    onSelect: (Palette) -> Unit,
    options: List<Palette> = Palette.entries,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            options.forEach { p ->
                PaletteSwatch(
                    modifier = modifier.weight(1f),
                    palette = p,
                    selected = p == selected,
                    onClick = { onSelect(p) }
                )
            }
        }
    }
}