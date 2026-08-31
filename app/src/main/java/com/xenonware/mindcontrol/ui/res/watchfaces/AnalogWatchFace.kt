package com.xenonware.mindcontrol.ui.res.watchfaces

import android.service.notification.StatusBarNotification
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import com.xenonware.mindcontrol.MediaInfo
import com.xenonware.mindcontrol.ui.res.UnifiedAodStyle
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun AnalogAodStyle(
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
        watchFace = { AnalogWatchFace(isActive = isActive) }
    )
}

@Composable
fun AnalogWatchFace(isActive: Boolean) {
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
    val hour = calendar.get(Calendar.HOUR)
    val minute = calendar.get(Calendar.MINUTE)
    val second = calendar.get(Calendar.SECOND)
    val millis = calendar.get(Calendar.MILLISECOND)

    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.7f)
            .aspectRatio(1f)
            .graphicsLayer(alpha = animatedAlpha)
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = w / 2f

        // Draw Ticks
        for (i in 0 until 60) {
            val angleDeg = i * 6f - 90f
            val rad = Math.toRadians(angleDeg.toDouble())
            val isMain = i % 5 == 0
            val tickLen = if (isMain) r * 0.15f else r * 0.05f
            val strokeWidth = if (isMain) r * 0.02f else r * 0.01f
            val alpha = if (isMain) 0.8f else 0.4f
            
            drawLine(
                color = Color.White.copy(alpha = alpha),
                start = Offset(cx + (r - tickLen) * cos(rad).toFloat(), cy + (r - tickLen) * sin(rad).toFloat()),
                end = Offset(cx + r * cos(rad).toFloat(), cy + r * sin(rad).toFloat()),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }

        // Hour Hand (ticks every minute)
        val hourAngle = (hour + minute / 60f) * 30f - 90f
        val hourRad = Math.toRadians(hourAngle.toDouble())
        drawLine(
            color = Color.White,
            start = Offset(cx, cy),
            end = Offset(cx + r * 0.5f * cos(hourRad).toFloat(), cy + r * 0.5f * sin(hourRad).toFloat()),
            strokeWidth = r * 0.04f,
            cap = StrokeCap.Round
        )

        // Minute Hand (ticks every minute)
        val minuteAngle = minute * 6f - 90f
        val minuteRad = Math.toRadians(minuteAngle.toDouble())
        drawLine(
            color = Color.White,
            start = Offset(cx, cy),
            end = Offset(cx + r * 0.75f * cos(minuteRad).toFloat(), cy + r * 0.75f * sin(minuteRad).toFloat()),
            strokeWidth = r * 0.025f,
            cap = StrokeCap.Round
        )

        // Second Hand (only if active)
        if (isActive) {
            val secondAngle = (second + millis / 1000f) * 6f - 90f
            val secondRad = Math.toRadians(secondAngle.toDouble())
            drawLine(
                color = Color.Red,
                start = Offset(cx, cy),
                end = Offset(cx + r * 0.85f * cos(secondRad).toFloat(), cy + r * 0.85f * sin(secondRad).toFloat()),
                strokeWidth = r * 0.01f,
                cap = StrokeCap.Round
            )
        }

        // Center dot
        drawCircle(color = Color.White, radius = r * 0.03f, center = Offset(cx, cy))
    }
}
