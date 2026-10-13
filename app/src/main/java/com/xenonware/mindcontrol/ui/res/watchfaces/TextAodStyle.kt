package com.xenonware.mindcontrol.ui.res.watchfaces

import android.service.notification.StatusBarNotification
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenonware.mindcontrol.MediaInfo
import com.xenonware.mindcontrol.SettingsManager
import com.xenonware.mindcontrol.ui.res.UnifiedAodStyle

fun getComposeFontFamily(key: String): FontFamily {
    return when (key) {
        "QUICKSAND" -> QuicksandTitleVariable
        "SANS_SERIF" -> FontFamily.SansSerif
        "SERIF" -> FontFamily.Serif
        "MONOSPACE" -> FontFamily.Monospace
        "CURSIVE" -> FontFamily.Cursive
        else -> QuicksandTitleVariable
    }
}

@Composable
fun TextAodStyle(
    isActive: Boolean,
    notifications: List<StatusBarNotification>,
    mediaInfo: MediaInfo?,
    isCharging: Boolean,
    batteryLevel: Int,
    animatedTextAlpha: Float,
    offsetY: Float,
    isMediaEnabled: Boolean = true,
    overrideLayout: SettingsManager.TextClockLayout? = null,
    overrideSubstyle: SettingsManager.TextClockSubstyle? = null,
    overrideFontFamilyKey: String? = null,
    overrideFontWeightInt: Int? = null,
    overrideIsItalic: Boolean? = null
) {
    UnifiedAodStyle(
        isActive = isActive,
        notifications = notifications,
        mediaInfo = mediaInfo,
        isCharging = isCharging,
        batteryLevel = batteryLevel,
        animatedTextAlpha = animatedTextAlpha,
        offsetY = offsetY,
        isMediaEnabled = isMediaEnabled,
        watchFace = {
            TextWatchFace(
                isActive = isActive,
                overrideLayout = overrideLayout,
                overrideSubstyle = overrideSubstyle,
                overrideFontFamilyKey = overrideFontFamilyKey,
                overrideFontWeightInt = overrideFontWeightInt,
                overrideIsItalic = overrideIsItalic
            )
        }
    )
}

@Composable
fun TextWatchFace(
    isActive: Boolean,
    overrideLayout: SettingsManager.TextClockLayout? = null,
    overrideSubstyle: SettingsManager.TextClockSubstyle? = null,
    overrideFontFamilyKey: String? = null,
    overrideFontWeightInt: Int? = null,
    overrideIsItalic: Boolean? = null
) {
    val context = LocalContext.current
    val layout = overrideLayout ?: SettingsManager.getTextClockLayout(context)
    val substyle = overrideSubstyle ?: SettingsManager.getTextClockSubstyle(context)
    val familyKey = overrideFontFamilyKey ?: SettingsManager.getTextClockFontFamily(context)
    val weightInt = overrideFontWeightInt ?: SettingsManager.getTextClockFontWeight(context)
    val isItalic = overrideIsItalic ?: SettingsManager.isTextClockItalic(context)

    val fontFamily = getComposeFontFamily(familyKey)
    val fontWeight = FontWeight(weightInt)
    val fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal

    when (substyle) {
        SettingsManager.TextClockSubstyle.MODERN -> {
            when (layout) {
                SettingsManager.TextClockLayout.INLINE -> InlineWatchFace(
                    isActive = isActive,
                    fontFamily = fontFamily,
                    fontWeight = fontWeight,
                    fontStyle = fontStyle
                )
                SettingsManager.TextClockLayout.STACKED -> StackedWatchFace(
                    isActive = isActive,
                    fontFamily = fontFamily,
                    fontWeight = fontWeight,
                    fontStyle = fontStyle
                )
            }
        }
        SettingsManager.TextClockSubstyle.NOTHING -> {
            when (layout) {
                SettingsManager.TextClockLayout.INLINE -> InlineDotWatchFace(isActive = isActive)
                SettingsManager.TextClockLayout.STACKED -> StackedDotWatchFace(isActive = isActive)
            }
        }
        SettingsManager.TextClockSubstyle.DIGITAL -> {
            when (layout) {
                SettingsManager.TextClockLayout.INLINE -> InlineDigitalWatchFace(isActive = isActive)
                SettingsManager.TextClockLayout.STACKED -> StackedDigitalWatchFace(isActive = isActive)
            }
        }
        SettingsManager.TextClockSubstyle.PIXEL -> {
            when (layout) {
                SettingsManager.TextClockLayout.INLINE -> PixelInlineWatchFace(isActive = isActive)
                SettingsManager.TextClockLayout.STACKED -> PixelStackedWatchFace(isActive = isActive)
            }
        }
    }
}
