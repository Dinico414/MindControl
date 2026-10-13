package com.xenonware.mindcontrol.ui.res.watchfaces

import android.service.notification.StatusBarNotification
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.xenonware.mindcontrol.MediaInfo
import com.xenonware.mindcontrol.ui.res.UnifiedAodStyle
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun AnalogAbstractAodStyle(
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
        watchFace = { AnalogAbstractWatchFace(isActive = isActive) }
    )
}

// --- Layout, relative to the dial radius ---
// Hour: a thick, short pill that starts just off the center
private const val HOUR_INNER = 0.10f
private const val HOUR_OUTER = 0.55f
private const val HOUR_THICKNESS = 0.16f
// Minute: a small dash sitting near the rim
private const val MINUTE_INNER = 0.70f
private const val MINUTE_OUTER = 0.84f
private const val MINUTE_THICKNESS = 0.055f
// Seconds: a dot on the rim
private const val SECOND_RADIUS = 0.80f
private const val SECOND_DOT = 0.055f
// Faint outline of the dial
private const val DIAL_STROKE = 0.008f
private const val DIAL_ALPHA = 0.10f

@Composable
fun AnalogAbstractWatchFace(isActive: Boolean) {
    val context = LocalContext.current

    // Material You, always the dark scheme
    val scheme = remember(context) { dynamicDarkColorScheme(context) }
    val hourColor = scheme.primary
    val minuteColor = scheme.tertiary
    val secondColor = scheme.secondary
    val dialColor = scheme.onSurface

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.7f, animationSpec = tween(500), label = "alpha"
    )
    // Seconds dot fades out in AOD
    val secondsAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f, animationSpec = tween(500), label = "seconds"
    )

    // Ticks once per second while the seconds dot is visible, otherwise once per minute
    var time by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val isFullyInactive = !isActive && secondsAlpha == 0f
    LaunchedEffect(isFullyInactive) {
        while (true) {
            time = System.currentTimeMillis()
            val cal = Calendar.getInstance().apply { timeInMillis = time }
            val ms = cal.get(Calendar.MILLISECOND)
            if (!isFullyInactive) {
                delay(max(1000L - ms, 16L).milliseconds)
            } else {
                val delayToNextMinute = 60000L - (cal.get(Calendar.SECOND) * 1000L + ms)
                delay(max(delayToNextMinute, 100L).milliseconds)
            }
        }
    }

    val calendar = Calendar.getInstance().apply { timeInMillis = time }
    val hour = calendar.get(Calendar.HOUR)
    val minute = calendar.get(Calendar.MINUTE)
    val second = calendar.get(Calendar.SECOND)

    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.7f)
            .aspectRatio(1f)
            .graphicsLayer(alpha = animatedAlpha)
    ) {
        val r = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        /** Point on the dial; angle in degrees, clockwise from 12 o'clock. */
        fun at(radius: Float, angleDeg: Float): Offset {
            val rad = Math.toRadians(angleDeg.toDouble())
            return Offset(
                center.x + radius * sin(rad).toFloat(),
                center.y - radius * cos(rad).toFloat()
            )
        }

        // Dial outline (barely visible)
        drawCircle(
            color = dialColor,
            radius = r - r * DIAL_STROKE / 2f,
            center = center,
            alpha = DIAL_ALPHA,
            style = Stroke(width = r * DIAL_STROKE)
        )

        // Hour: moves with the minutes (no seconds), so it creeps along like a real hour hand
        val hourAngle = (hour + minute / 60f) * 30f
        drawLine(
            color = hourColor,
            start = at(r * HOUR_INNER, hourAngle),
            end = at(r * HOUR_OUTER, hourAngle),
            strokeWidth = r * HOUR_THICKNESS,
            cap = StrokeCap.Round
        )

        // Minute: jumps once per minute
        val minuteAngle = minute * 6f
        drawLine(
            color = minuteColor,
            start = at(r * MINUTE_INNER, minuteAngle),
            end = at(r * MINUTE_OUTER, minuteAngle),
            strokeWidth = r * MINUTE_THICKNESS,
            cap = StrokeCap.Round
        )

        // Seconds: ticks once per second
        if (secondsAlpha > 0f) {
            drawCircle(
                color = secondColor,
                radius = r * SECOND_DOT,
                center = at(r * SECOND_RADIUS, second * 6f),
                alpha = secondsAlpha
            )
        }
    }
}
