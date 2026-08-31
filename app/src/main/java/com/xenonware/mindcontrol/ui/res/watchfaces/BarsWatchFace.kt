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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenonware.mindcontrol.MediaInfo
import com.xenonware.mindcontrol.ui.res.UnifiedAodStyle
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun BarsAodStyle(
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
        watchFace = { BarsWatchFace(isActive = isActive) }
    )
}

@Composable
fun BarsWatchFace(isActive: Boolean) {
    val textMeasurer = rememberTextMeasurer()
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)
    val localDensity = LocalDensity.current

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

    val totalHourSegments = if (is24Hour) 24 else 12
    val activeHourSegments = if (is24Hour) hour else (hour % 12).let { if (it == 0) 12 else it }

    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.65f)
            .aspectRatio(1.7f)
            .graphicsLayer(alpha = animatedAlpha)
    ) {
        val w = size.width
        val h = size.height

        // Define bar widths and horizontal spacing
        val barWidth = w * 0.42f
        val gap = w * 0.08f
        val leftBarX = (w - (barWidth * 2f + gap)) / 2f
        val rightBarX = leftBarX + barWidth + gap

        // --- Measure the text FIRST, so the bars can reserve room for it ---
        // Font size scaled to fit nicely (about 0.28x of the total height)
        val fontSize = (h * 0.28f / localDensity.density / localDensity.fontScale).sp
        val textStyle = TextStyle(
            color = Color.White,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            fontFamily = QuicksandTitleVariable
        )

        val paddingAboveBar = 4f // Constant gap between the topmost segment and the number

        val hourText = String.format(Locale.getDefault(), "%02d", displayHour)
        val minuteText = String.format(Locale.getDefault(), "%02d", minute)
        val hourLayout = textMeasurer.measure(hourText, textStyle)
        val minuteLayout = textMeasurer.measure(minuteText, textStyle)

        // Both numbers share the same font/size, so one baseline applies to both.
        val numberBaseline = hourLayout.getLineBaseline(0)

        // Reserve a strip at the top = number height (baseline) + the gap.
        // Sizing the segments against this reduced height means a FULL bar tops
        // out exactly at the reserved line, so the number can never be covered
        // and the spacing above the topmost segment is always `paddingAboveBar`.
        val reservedTop = numberBaseline + paddingAboveBar
        val drawableHeight = h - reservedTop

        // Draw Hour Bar (Left) - split into 12 or 24 segments
        val hourSpacing = if (totalHourSegments == 12) 4f else 2f
        val hourSegmentHeight = (drawableHeight - (totalHourSegments - 1) * hourSpacing) / totalHourSegments

        for (i in 0 until activeHourSegments) {
            // Drawn from bottom to top
            val y = h - (i * (hourSegmentHeight + hourSpacing)) - hourSegmentHeight

            drawRoundRect(
                color = Color.White,
                topLeft = Offset(leftBarX, y),
                size = Size(barWidth, hourSegmentHeight),
                cornerRadius = CornerRadius(hourSegmentHeight * 0.3f)
            )
        }

        // Draw Minute Bar (Right) - split into 60 segments
        val minuteSpacing = 1.2f
        val minuteSegmentHeight = (drawableHeight - (60 - 1) * minuteSpacing) / 60

        for (i in 0 until minute) {
            // Drawn from bottom to top
            val y = h - (i * (minuteSegmentHeight + minuteSpacing)) - minuteSegmentHeight

            drawRoundRect(
                color = Color.White,
                topLeft = Offset(rightBarX, y),
                size = Size(barWidth, minuteSegmentHeight),
                cornerRadius = CornerRadius(minuteSegmentHeight * 0.3f)
            )
        }

        // Draw Hour Number
        val hourWaterLevelY = if (activeHourSegments > 0) {
            h - activeHourSegments * hourSegmentHeight - (activeHourSegments - 1) * hourSpacing
        } else {
            h
        }
        // At fulfill, hourWaterLevelY == reservedTop, so hourTextY == 0 (just fits).
        val hourTextY = (hourWaterLevelY - numberBaseline - paddingAboveBar).coerceAtLeast(0f)

        drawText(
            textLayoutResult = hourLayout,
            topLeft = Offset(leftBarX, hourTextY)
        )

        // Draw Minute Number
        val minuteWaterLevelY = if (minute > 0) {
            h - minute * minuteSegmentHeight - (minute - 1) * minuteSpacing
        } else {
            h
        }
        val minuteTextY = (minuteWaterLevelY - numberBaseline - paddingAboveBar).coerceAtLeast(0f)

        drawText(
            textLayoutResult = minuteLayout,
            topLeft = Offset(rightBarX, minuteTextY)
        )
    }
}
