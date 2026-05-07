package com.xenonware.mindcontrol.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

// Red Theme
private val RedDarkColorScheme = darkColorScheme(
    primary = RedPrimaryDark,
    onPrimary = RedOnPrimaryDark,
    primaryContainer = RedPrimaryContainerDark,
    onPrimaryContainer = RedOnPrimaryContainerDark,
    secondary = RedSecondaryDark,
    onSecondary = RedOnSecondaryDark,
    secondaryContainer = RedSecondaryContainerDark,
    onSecondaryContainer = RedOnSecondaryContainerDark,
    tertiary = RedTertiaryDark,
    onTertiary = RedOnTertiaryDark,
    tertiaryContainer = RedTertiaryContainerDark,
    onTertiaryContainer = RedOnTertiaryContainerDark,
    error = RedErrorDark,
    onError = RedOnErrorDark,
    errorContainer = RedErrorContainerDark,
    onErrorContainer = RedOnErrorContainerDark,
    background = RedBackgroundDark,
    onBackground = RedOnBackgroundDark,
    surface = RedSurfaceDark,
    onSurface = RedOnSurfaceDark,
    surfaceVariant = RedSurfaceVariantDark,
    onSurfaceVariant = RedOnSurfaceVariantDark,
    outline = RedOutlineDark,
    outlineVariant = RedOutlineVariantDark,
    scrim = RedScrimDark,
    inverseSurface = RedInverseSurfaceDark,
    inverseOnSurface = RedInverseOnSurfaceDark,
    inversePrimary = RedInversePrimaryDark,
    surfaceDim = RedSurfaceDimDark,
    surfaceBright = RedSurfaceBrightDark,
    surfaceContainerLowest = RedSurfaceContainerLowestDark,
    surfaceContainerLow = RedSurfaceContainerLowDark,
    surfaceContainer = RedSurfaceContainerDark,
    surfaceContainerHigh = RedSurfaceContainerHighDark,
    surfaceContainerHighest = RedSurfaceContainerHighestDark,
)

private val RedLightColorScheme = lightColorScheme(
    primary = RedPrimaryLight,
    onPrimary = RedOnPrimaryLight,
    primaryContainer = RedPrimaryContainerLight,
    onPrimaryContainer = RedOnPrimaryContainerLight,
    secondary = RedSecondaryLight,
    onSecondary = RedOnSecondaryLight,
    secondaryContainer = RedSecondaryContainerLight,
    onSecondaryContainer = RedOnSecondaryContainerLight,
    tertiary = RedTertiaryLight,
    onTertiary = RedOnTertiaryLight,
    tertiaryContainer = RedTertiaryContainerLight,
    onTertiaryContainer = RedOnTertiaryContainerLight,
    error = RedErrorLight,
    onError = RedOnErrorLight,
    errorContainer = RedErrorContainerLight,
    onErrorContainer = RedOnErrorContainerLight,
    background = RedBackgroundLight,
    onBackground = RedOnBackgroundLight,
    surface = RedSurfaceLight,
    onSurface = RedOnSurfaceLight,
    surfaceVariant = RedSurfaceVariantLight,
    onSurfaceVariant = RedOnSurfaceVariantLight,
    outline = RedOutlineLight,
    outlineVariant = RedOutlineVariantLight,
    scrim = RedScrimLight,
    inverseSurface = RedInverseSurfaceLight,
    inverseOnSurface = RedInverseOnSurfaceLight,
    inversePrimary = RedInversePrimaryLight,
    surfaceDim = RedSurfaceDimLight,
    surfaceBright = RedSurfaceBrightLight,
    surfaceContainerLowest = RedSurfaceContainerLowestLight,
    surfaceContainerLow = RedSurfaceContainerLowLight,
    surfaceContainer = RedSurfaceContainerLight,
    surfaceContainerHigh = RedSurfaceContainerHighLight,
    surfaceContainerHighest = RedSurfaceContainerHighestLight,
)

@Composable
fun RedTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) RedDarkColorScheme else RedLightColorScheme,
        typography = Typography,
        content = content
    )
}

// Green Theme
private val GreenDarkColorScheme = darkColorScheme(
    primary = GreenPrimaryDark,
    onPrimary = GreenOnPrimaryDark,
    primaryContainer = GreenPrimaryContainerDark,
    onPrimaryContainer = GreenOnPrimaryContainerDark,
    secondary = GreenSecondaryDark,
    onSecondary = GreenOnSecondaryDark,
    secondaryContainer = GreenSecondaryContainerDark,
    onSecondaryContainer = GreenOnSecondaryContainerDark,
    tertiary = GreenTertiaryDark,
    onTertiary = GreenOnTertiaryDark,
    tertiaryContainer = GreenTertiaryContainerDark,
    onTertiaryContainer = GreenOnTertiaryContainerDark,
    error = GreenErrorDark,
    onError = GreenOnErrorDark,
    errorContainer = GreenErrorContainerDark,
    onErrorContainer = GreenOnErrorContainerDark,
    background = GreenBackgroundDark,
    onBackground = GreenOnBackgroundDark,
    surface = GreenSurfaceDark,
    onSurface = GreenOnSurfaceDark,
    surfaceVariant = GreenSurfaceVariantDark,
    onSurfaceVariant = GreenOnSurfaceVariantDark,
    outline = GreenOutlineDark,
    outlineVariant = GreenOutlineVariantDark,
    scrim = GreenScrimDark,
    inverseSurface = GreenInverseSurfaceDark,
    inverseOnSurface = GreenInverseOnSurfaceDark,
    inversePrimary = GreenInversePrimaryDark,
    surfaceDim = GreenSurfaceDimDark,
    surfaceBright = GreenSurfaceBrightDark,
    surfaceContainerLowest = GreenSurfaceContainerLowestDark,
    surfaceContainerLow = GreenSurfaceContainerLowDark,
    surfaceContainer = GreenSurfaceContainerDark,
    surfaceContainerHigh = GreenSurfaceContainerHighDark,
    surfaceContainerHighest = GreenSurfaceContainerHighestDark,
)

private val GreenLightColorScheme = lightColorScheme(
    primary = GreenPrimaryLight,
    onPrimary = GreenOnPrimaryLight,
    primaryContainer = GreenPrimaryContainerLight,
    onPrimaryContainer = GreenOnPrimaryContainerLight,
    secondary = GreenSecondaryLight,
    onSecondary = GreenOnSecondaryLight,
    secondaryContainer = GreenSecondaryContainerLight,
    onSecondaryContainer = GreenOnSecondaryContainerLight,
    tertiary = GreenTertiaryLight,
    onTertiary = GreenOnTertiaryLight,
    tertiaryContainer = GreenTertiaryContainerLight,
    onTertiaryContainer = GreenOnTertiaryContainerLight,
    error = GreenErrorLight,
    onError = GreenOnErrorLight,
    errorContainer = GreenErrorContainerLight,
    onErrorContainer = GreenOnErrorContainerLight,
    background = GreenBackgroundLight,
    onBackground = GreenOnBackgroundLight,
    surface = GreenSurfaceLight,
    onSurface = GreenOnSurfaceLight,
    surfaceVariant = GreenSurfaceVariantLight,
    onSurfaceVariant = GreenOnSurfaceVariantLight,
    outline = GreenOutlineLight,
    outlineVariant = GreenOutlineVariantLight,
    scrim = GreenScrimLight,
    inverseSurface = GreenInverseSurfaceLight,
    inverseOnSurface = GreenInverseOnSurfaceLight,
    inversePrimary = GreenInversePrimaryLight,
    surfaceDim = GreenSurfaceDimLight,
    surfaceBright = GreenSurfaceBrightLight,
    surfaceContainerLowest = GreenSurfaceContainerLowestLight,
    surfaceContainerLow = GreenSurfaceContainerLowLight,
    surfaceContainer = GreenSurfaceContainerLight,
    surfaceContainerHigh = GreenSurfaceContainerHighLight,
    surfaceContainerHighest = GreenSurfaceContainerHighestLight,
)

@Composable
fun GreenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) GreenDarkColorScheme else GreenLightColorScheme,
        typography = Typography,
        content = content
    )
}

// Blue Theme
private val BlueDarkColorScheme = darkColorScheme(
    primary = BluePrimaryDark,
    onPrimary = BlueOnPrimaryDark,
    primaryContainer = BluePrimaryContainerDark,
    onPrimaryContainer = BlueOnPrimaryContainerDark,
    secondary = BlueSecondaryDark,
    onSecondary = BlueOnSecondaryDark,
    secondaryContainer = BlueSecondaryContainerDark,
    onSecondaryContainer = BlueOnSecondaryContainerDark,
    tertiary = BlueTertiaryDark,
    onTertiary = BlueOnTertiaryDark,
    tertiaryContainer = BlueTertiaryContainerDark,
    onTertiaryContainer = BlueOnTertiaryContainerDark,
    error = BlueErrorDark,
    onError = BlueOnErrorDark,
    errorContainer = BlueErrorContainerDark,
    onErrorContainer = BlueOnErrorContainerDark,
    background = BlueBackgroundDark,
    onBackground = BlueOnBackgroundDark,
    surface = BlueSurfaceDark,
    onSurface = BlueOnSurfaceDark,
    surfaceVariant = BlueSurfaceVariantDark,
    onSurfaceVariant = BlueOnSurfaceVariantDark,
    outline = BlueOutlineDark,
    outlineVariant = BlueOutlineVariantDark,
    scrim = BlueScrimDark,
    inverseSurface = BlueInverseSurfaceDark,
    inverseOnSurface = BlueInverseOnSurfaceDark,
    inversePrimary = BlueInversePrimaryDark,
    surfaceDim = BlueSurfaceDimDark,
    surfaceBright = BlueSurfaceBrightDark,
    surfaceContainerLowest = BlueSurfaceContainerLowestDark,
    surfaceContainerLow = BlueSurfaceContainerLowDark,
    surfaceContainer = BlueSurfaceContainerDark,
    surfaceContainerHigh = BlueSurfaceContainerHighDark,
    surfaceContainerHighest = BlueSurfaceContainerHighestDark,
)

private val BlueLightColorScheme = lightColorScheme(
    primary = BluePrimaryLight,
    onPrimary = BlueOnPrimaryLight,
    primaryContainer = BluePrimaryContainerLight,
    onPrimaryContainer = BlueOnPrimaryContainerLight,
    secondary = BlueSecondaryLight,
    onSecondary = BlueOnSecondaryLight,
    secondaryContainer = BlueSecondaryContainerLight,
    onSecondaryContainer = BlueOnSecondaryContainerLight,
    tertiary = BlueTertiaryLight,
    onTertiary = BlueOnTertiaryLight,
    tertiaryContainer = BlueTertiaryContainerLight,
    onTertiaryContainer = BlueOnTertiaryContainerLight,
    error = BlueErrorLight,
    onError = BlueOnErrorLight,
    errorContainer = BlueErrorContainerLight,
    onErrorContainer = BlueOnErrorContainerLight,
    background = BlueBackgroundLight,
    onBackground = BlueOnBackgroundLight,
    surface = BlueSurfaceLight,
    onSurface = BlueOnSurfaceLight,
    surfaceVariant = BlueSurfaceVariantLight,
    onSurfaceVariant = BlueOnSurfaceVariantLight,
    outline = BlueOutlineLight,
    outlineVariant = BlueOutlineVariantLight,
    scrim = BlueScrimLight,
    inverseSurface = BlueInverseSurfaceLight,
    inverseOnSurface = BlueInverseOnSurfaceLight,
    inversePrimary = BlueInversePrimaryLight,
    surfaceDim = BlueSurfaceDimLight,
    surfaceBright = BlueSurfaceBrightLight,
    surfaceContainerLowest = BlueSurfaceContainerLowestLight,
    surfaceContainerLow = BlueSurfaceContainerLowLight,
    surfaceContainer = BlueSurfaceContainerLight,
    surfaceContainerHigh = BlueSurfaceContainerHighLight,
    surfaceContainerHighest = BlueSurfaceContainerHighestLight,
)

@Composable
fun BlueTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) BlueDarkColorScheme else BlueLightColorScheme,
        typography = Typography,
        content = content
    )
}

// Yellow Theme
private val YellowDarkColorScheme = darkColorScheme(
    primary = YellowPrimaryDark,
    onPrimary = YellowOnPrimaryDark,
    primaryContainer = YellowPrimaryContainerDark,
    onPrimaryContainer = YellowOnPrimaryContainerDark,
    secondary = YellowSecondaryDark,
    onSecondary = YellowOnSecondaryDark,
    secondaryContainer = YellowSecondaryContainerDark,
    onSecondaryContainer = YellowOnSecondaryContainerDark,
    tertiary = YellowTertiaryDark,
    onTertiary = YellowOnTertiaryDark,
    tertiaryContainer = YellowTertiaryContainerDark,
    onTertiaryContainer = YellowOnTertiaryContainerDark,
    error = YellowErrorDark,
    onError = YellowOnErrorDark,
    errorContainer = YellowErrorContainerDark,
    onErrorContainer = YellowOnErrorContainerDark,
    background = YellowBackgroundDark,
    onBackground = YellowOnBackgroundDark,
    surface = YellowSurfaceDark,
    onSurface = YellowOnSurfaceDark,
    surfaceVariant = YellowSurfaceVariantDark,
    onSurfaceVariant = YellowOnSurfaceVariantDark,
    outline = YellowOutlineDark,
    outlineVariant = YellowOutlineVariantDark,
    scrim = YellowScrimDark,
    inverseSurface = YellowInverseSurfaceDark,
    inverseOnSurface = YellowInverseOnSurfaceDark,
    inversePrimary = YellowInversePrimaryDark,
    surfaceDim = YellowSurfaceDimDark,
    surfaceBright = YellowSurfaceBrightDark,
    surfaceContainerLowest = YellowSurfaceContainerLowestDark,
    surfaceContainerLow = YellowSurfaceContainerLowDark,
    surfaceContainer = YellowSurfaceContainerDark,
    surfaceContainerHigh = YellowSurfaceContainerHighDark,
    surfaceContainerHighest = YellowSurfaceContainerHighestDark,
)

private val YellowLightColorScheme = lightColorScheme(
    primary = YellowPrimaryLight,
    onPrimary = YellowOnPrimaryLight,
    primaryContainer = YellowPrimaryContainerLight,
    onPrimaryContainer = YellowOnPrimaryContainerLight,
    secondary = YellowSecondaryLight,
    onSecondary = YellowOnSecondaryLight,
    secondaryContainer = YellowSecondaryContainerLight,
    onSecondaryContainer = YellowOnSecondaryContainerLight,
    tertiary = YellowTertiaryLight,
    onTertiary = YellowOnTertiaryLight,
    tertiaryContainer = YellowTertiaryContainerLight,
    onTertiaryContainer = YellowOnTertiaryContainerLight,
    error = YellowErrorLight,
    onError = YellowOnErrorLight,
    errorContainer = YellowErrorContainerLight,
    onErrorContainer = YellowOnErrorContainerLight,
    background = YellowBackgroundLight,
    onBackground = YellowOnBackgroundLight,
    surface = YellowSurfaceLight,
    onSurface = YellowOnSurfaceLight,
    surfaceVariant = YellowSurfaceVariantLight,
    onSurfaceVariant = YellowOnSurfaceVariantLight,
    outline = YellowOutlineLight,
    outlineVariant = YellowOutlineVariantLight,
    scrim = YellowScrimLight,
    inverseSurface = YellowInverseSurfaceLight,
    inverseOnSurface = YellowInverseOnSurfaceLight,
    inversePrimary = YellowInversePrimaryLight,
    surfaceDim = YellowSurfaceDimLight,
    surfaceBright = YellowSurfaceBrightLight,
    surfaceContainerLowest = YellowSurfaceContainerLowestLight,
    surfaceContainerLow = YellowSurfaceContainerLowLight,
    surfaceContainer = YellowSurfaceContainerLight,
    surfaceContainerHigh = YellowSurfaceContainerHighLight,
    surfaceContainerHighest = YellowSurfaceContainerHighestLight,
)

@Composable
fun YellowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) YellowDarkColorScheme else YellowLightColorScheme,
        typography = Typography,
        content = content
    )
}
