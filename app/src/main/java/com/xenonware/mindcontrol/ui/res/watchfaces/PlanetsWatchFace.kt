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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import com.xenonware.mindcontrol.MediaInfo
import com.xenonware.mindcontrol.ui.res.UnifiedAodStyle
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun PlanetsAodStyle(
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
        watchFace = { PlanetsWatchFace(isActive = isActive) }
    )
}

@Composable
fun PlanetsWatchFace(isActive: Boolean) {
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
            .fillMaxWidth(0.75f)
            .aspectRatio(1f)
            .graphicsLayer(alpha = animatedAlpha)
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = w / 2f

        // Let's define the radius for our rings:
        // Hour (center) ring: rHour = r * 0.35f
        // Minute (middle) ring: rMinute = r * 0.65f
        // Second (outside) ring: rSecond = r * 0.92f
        val rHour = r * 0.35f
        val rMinute = r * 0.65f
        val rSecond = r * 0.92f

        // Draw the rings themselves. The rings are drawn with low opacity so they are visible but subtle.
        drawCircle(
            color = Color.White.copy(alpha = 0.15f),
            radius = rHour,
            center = Offset(cx, cy),
            style = Stroke(width = r * 0.015f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.15f),
            radius = rMinute,
            center = Offset(cx, cy),
            style = Stroke(width = r * 0.015f)
        )
        // Only draw the second ring if AOD is active (optional, but consistent with second hand behavior)
        if (isActive) {
            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = rSecond,
                center = Offset(cx, cy),
                style = Stroke(width = r * 0.015f)
            )
        }

        // Now calculate angles for the "planets" running on the rings
        // In analog clock:
        // Hour angle (30 degrees per hour, plus 0.5 degrees per minute)
        val hourAngle = (hour + minute / 60f + second / 3600f) * 30f - 90f
        val hourRad = Math.toRadians(hourAngle.toDouble())

        // Minute angle (6 degrees per minute, plus 0.1 degrees per second)
        val minuteAngle = (minute + second / 60f) * 6f - 90f
        val minuteRad = Math.toRadians(minuteAngle.toDouble())

        // Draw Hour Planet (Center)
        val hPlanetX = cx + rHour * cos(hourRad).toFloat()
        val hPlanetY = cy + rHour * sin(hourRad).toFloat()
        drawCircle(
            color = Color.White,
            radius = r * 0.06f,
            center = Offset(hPlanetX, hPlanetY)
        )

        // Draw Minute Planet (Middle)
        val mPlanetX = cx + rMinute * cos(minuteRad).toFloat()
        val mPlanetY = cy + rMinute * sin(minuteRad).toFloat()
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = r * 0.045f,
            center = Offset(mPlanetX, mPlanetY)
        )

        // Draw Second Planet (Outside) - only if active
        if (isActive) {
            val secondAngle = (second + millis / 1000f) * 6f - 90f
            val secondRad = Math.toRadians(secondAngle.toDouble())
            val sPlanetX = cx + rSecond * cos(secondRad).toFloat()
            val sPlanetY = cy + rSecond * sin(secondRad).toFloat()
            drawCircle(
                color = Color.Red,
                radius = r * 0.03f,
                center = Offset(sPlanetX, sPlanetY)
            )
        }

        // Draw a tiny center dot (Sun / Core)
        drawCircle(
            color = Color.White.copy(alpha = 0.3f),
            radius = r * 0.05f,
            center = Offset(cx, cy)
        )
    }
}
