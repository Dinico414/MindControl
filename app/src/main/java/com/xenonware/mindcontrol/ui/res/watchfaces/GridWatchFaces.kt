package com.xenonware.mindcontrol.ui.res.watchfaces

import android.service.notification.StatusBarNotification
import android.text.format.DateFormat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.xenonware.mindcontrol.MediaInfo
import com.xenonware.mindcontrol.ui.res.UnifiedAodStyle
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun StackedDotAodStyle(
    isActive: Boolean,
    notifications: List<StatusBarNotification>,
    mediaInfo: MediaInfo?,
    isCharging: Boolean,
    batteryLevel: Int,
    animatedTextAlpha: Float,
    offsetY: Float,
    isMediaEnabled: Boolean = true
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
        watchFace = { StackedDotWatchFace(isActive = isActive) }
    )
}

@Composable
fun StackedDigitalAodStyle(
    isActive: Boolean,
    notifications: List<StatusBarNotification>,
    mediaInfo: MediaInfo?,
    isCharging: Boolean,
    batteryLevel: Int,
    animatedTextAlpha: Float,
    offsetY: Float,
    isMediaEnabled: Boolean = true
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
        watchFace = { StackedDigitalWatchFace(isActive = isActive) }
    )
}

@Composable
fun InlineDotAodStyle(
    isActive: Boolean,
    notifications: List<StatusBarNotification>,
    mediaInfo: MediaInfo?,
    isCharging: Boolean,
    batteryLevel: Int,
    animatedTextAlpha: Float,
    offsetY: Float,
    isMediaEnabled: Boolean = true
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
        watchFace = { InlineDotWatchFace(isActive = isActive) }
    )
}

@Composable
fun InlineDigitalAodStyle(
    isActive: Boolean,
    notifications: List<StatusBarNotification>,
    mediaInfo: MediaInfo?,
    isCharging: Boolean,
    batteryLevel: Int,
    animatedTextAlpha: Float,
    offsetY: Float,
    isMediaEnabled: Boolean = true
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
        watchFace = { InlineDigitalWatchFace(isActive = isActive) }
    )
}

@Composable
fun StackedDotWatchFace(isActive: Boolean) {
    GenericStackedGridWatchFace(isActive = isActive, isNothingStyle = true)
}

@Composable
fun StackedDigitalWatchFace(isActive: Boolean) {
    GenericStackedGridWatchFace(isActive = isActive, isNothingStyle = false)
}

@Composable
fun InlineDotWatchFace(isActive: Boolean) {
    GenericInlineGridWatchFace(isActive = isActive, isNothingStyle = true)
}

@Composable
fun InlineDigitalWatchFace(isActive: Boolean) {
    GenericInlineGridWatchFace(isActive = isActive, isNothingStyle = false)
}

@Composable
internal fun GenericStackedGridWatchFace(isActive: Boolean, isNothingStyle: Boolean) {
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.7f,
        animationSpec = tween(500),
        label = "alpha"
    )

    var time by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(isActive) {
        while (true) {
            time = System.currentTimeMillis()
            if (isActive) delay(16.milliseconds) else delay(1000.milliseconds)
        }
    }

    val calendar = Calendar.getInstance().apply { timeInMillis = time }
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val displayHour = if (is24Hour) hour else (hour % 12).let { if (it == 0) 12 else it }
    val minute = calendar.get(Calendar.MINUTE)

    Canvas(
        modifier = Modifier.fillMaxWidth(0.4f).aspectRatio(1f).graphicsLayer(alpha = animatedAlpha)
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val digitWidth = w * 0.32f
        val digitHeight = h * 0.42f
        val spacing = w * 0.05f

        val h1 = displayHour / 10
        val h2 = displayHour % 10
        val m1 = minute / 10
        val m2 = minute % 10

        val topY = cy - digitHeight - spacing / 2f
        val bottomY = cy + spacing / 2f
        val leftX = cx - digitWidth - spacing / 2f
        val rightX = cx + spacing / 2f

        if (isNothingStyle) {
            drawNothingDigit(h1, Offset(leftX, topY), digitWidth, digitHeight, Color.White, 1f)
            drawNothingDigit(h2, Offset(rightX, topY), digitWidth, digitHeight, Color.White, 1f)
            drawNothingDigit(m1, Offset(leftX, bottomY), digitWidth, digitHeight, Color.White, 1f)
            drawNothingDigit(m2, Offset(rightX, bottomY), digitWidth, digitHeight, Color.White, 1f)
        } else {
            drawDigitalDigit(h1, Offset(leftX, topY), digitWidth, digitHeight, Color.White, 1f)
            drawDigitalDigit(h2, Offset(rightX, topY), digitWidth, digitHeight, Color.White, 1f)
            drawDigitalDigit(m1, Offset(leftX, bottomY), digitWidth, digitHeight, Color.White, 1f)
            drawDigitalDigit(m2, Offset(rightX, bottomY), digitWidth, digitHeight, Color.White, 1f)
        }
    }
}

@Composable
internal fun GenericInlineGridWatchFace(isActive: Boolean, isNothingStyle: Boolean) {
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.7f,
        animationSpec = tween(500),
        label = "alpha"
    )

    var time by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(isActive) {
        while (true) {
            time = System.currentTimeMillis()
            if (isActive) delay(16.milliseconds) else delay(1000.milliseconds)
        }
    }

    val calendar = Calendar.getInstance().apply { timeInMillis = time }
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val displayHour = if (is24Hour) hour else (hour % 12).let { if (it == 0) 12 else it }
    val minute = calendar.get(Calendar.MINUTE)

    Canvas(
        modifier = Modifier.fillMaxWidth(0.5f).aspectRatio(2.5f).graphicsLayer(alpha = animatedAlpha)
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val digitWidth = w * 0.17f
        val digitHeight = h * 0.85f
        val spacing = w * 0.02f
        val colonWidth = w * 0.05f

        val h1 = displayHour / 10
        val h2 = displayHour % 10
        val m1 = minute / 10
        val m2 = minute % 10

        val startX = cx - (digitWidth * 2 + colonWidth + digitWidth * 2 + spacing * 4) / 2f
        val y = cy - digitHeight / 2f

        var currentX = startX
        if (isNothingStyle) {
            drawNothingDigit(h1, Offset(currentX, y), digitWidth, digitHeight, Color.White, 1f)
            currentX += digitWidth + spacing
            drawNothingDigit(h2, Offset(currentX, y), digitWidth, digitHeight, Color.White, 1f)
            currentX += digitWidth + spacing
            
            // Colon
            val cellSize = digitWidth / 4f
            val dotRadius = cellSize * 0.38f
            drawCircle(Color.White, dotRadius, Offset(currentX + colonWidth / 2f, cy - digitHeight * 0.2f), 1f)
            drawCircle(Color.White, dotRadius, Offset(currentX + colonWidth / 2f, cy + digitHeight * 0.2f), 1f)
            
            currentX += colonWidth + spacing
            drawNothingDigit(m1, Offset(currentX, y), digitWidth, digitHeight, Color.White, 1f)
            currentX += digitWidth + spacing
            drawNothingDigit(m2, Offset(currentX, y), digitWidth, digitHeight, Color.White, 1f)
        } else {
            drawDigitalDigit(h1, Offset(currentX, y), digitWidth, digitHeight, Color.White, 1f)
            currentX += digitWidth + spacing
            drawDigitalDigit(h2, Offset(currentX, y), digitWidth, digitHeight, Color.White, 1f)
            currentX += digitWidth + spacing
            
            // Colon
            val t = digitWidth * 0.16f
            val sy = digitHeight * 0.2f
            drawRect(Color.White, Offset(currentX + colonWidth / 2f - t / 2f, cy - sy - t / 2f), Size(t, t), alpha = 1f)
            drawRect(Color.White, Offset(currentX + colonWidth / 2f - t / 2f, cy + sy - t / 2f), Size(t, t), alpha = 1f)

            currentX += colonWidth + spacing
            drawDigitalDigit(m1, Offset(currentX, y), digitWidth, digitHeight, Color.White, 1f)
            currentX += digitWidth + spacing
            drawDigitalDigit(h2, Offset(currentX, y), digitWidth, digitHeight, Color.White, 1f)
        }
    }
}
