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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.xenonware.mindcontrol.MediaInfo
import com.xenonware.mindcontrol.ui.res.UnifiedAodStyle
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun PixelStackedAodStyle(
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
        watchFace = { PixelStackedWatchFace(isActive = isActive) }
    )
}

@Composable
fun PixelInlineAodStyle(
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
        watchFace = { PixelInlineWatchFace(isActive = isActive) }
    )
}

@Composable
fun PixelStackedWatchFace(isActive: Boolean) {
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
        val digitWidth = w * 0.38f
        val digitHeight = h * 0.45f
        val hSpacing = w * 0.02f
        val vSpacing = h * 0.08f

        val h1 = displayHour / 10
        val h2 = displayHour % 10
        val m1 = minute / 10
        val m2 = minute % 10

        val topY = cy - digitHeight - vSpacing / 2f
        val bottomY = cy + vSpacing / 2f
        val leftX = cx - digitWidth - hSpacing / 2f
        val rightX = cx + hSpacing / 2f

        drawPixelDigit(h1, Offset(leftX, topY), digitWidth, digitHeight, Color.White, 1f)
        drawPixelDigit(h2, Offset(rightX, topY), digitWidth, digitHeight, Color.White, 1f)
        drawPixelDigit(m1, Offset(leftX, bottomY), digitWidth, digitHeight, Color.White, 1f)
        drawPixelDigit(m2, Offset(rightX, bottomY), digitWidth, digitHeight, Color.White, 1f)
    }
}

@Composable
fun PixelInlineWatchFace(isActive: Boolean) {
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
        modifier = Modifier.fillMaxWidth(0.6f).aspectRatio(2f).graphicsLayer(alpha = animatedAlpha)
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        
        val digitWidth = w * 0.18f
        val digitHeight = h * 0.8f
        val spacing = w * 0.02f
        val colonWidth = w * 0.08f

        val h1 = displayHour / 10
        val h2 = displayHour % 10
        val m1 = minute / 10
        val m2 = minute % 10

        val totalWidth = digitWidth * 4 + spacing * 3 + colonWidth
        val startX = cx - totalWidth / 2f
        val y = cy - digitHeight / 2f

        var currentX = startX
        drawPixelDigit(h1, Offset(currentX, y), digitWidth, digitHeight, Color.White, 1f)
        currentX += digitWidth + spacing
        drawPixelDigit(h2, Offset(currentX, y), digitWidth, digitHeight, Color.White, 1f)
        currentX += digitWidth + spacing
        
        // Colon
        val dotRadius = digitWidth * 0.12f
        drawCircle(Color.White, dotRadius, Offset(currentX + colonWidth / 2f, cy - digitHeight * 0.15f), 1f)
        drawCircle(Color.White, dotRadius, Offset(currentX + colonWidth / 2f, cy + digitHeight * 0.15f), 1f)
        
        currentX += colonWidth + spacing
        drawPixelDigit(m1, Offset(currentX, y), digitWidth, digitHeight, Color.White, 1f)
        currentX += digitWidth + spacing
        drawPixelDigit(m2, Offset(currentX, y), digitWidth, digitHeight, Color.White, 1f)
    }
}
