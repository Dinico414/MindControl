package com.xenonware.mindcontrol.ui.res.watchfaces

import android.service.notification.StatusBarNotification
import android.text.format.DateFormat
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenonware.mindcontrol.MediaInfo
import com.xenonware.mindcontrol.ui.res.UnifiedAodStyle
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun TrackAodStyle(
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
        watchFace = { TrackWatchFace(isActive = isActive) }
    )
}

@Composable
fun TrackWatchFace(isActive: Boolean) {
    val textMeasurer = rememberTextMeasurer()
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)
    val localDensity = LocalDensity.current
    val dayFormat = remember { SimpleDateFormat("EEE", Locale.getDefault()) }

    val scheme =
        remember(context) { dynamicDarkColorScheme(context) }
    val accent = scheme.primary
    val handFill = scheme.primary.copy(alpha = 0.12f).compositeOver(Color.Black)
    val pillFill = scheme.primaryContainer
    val pillText = scheme.onPrimaryContainer
    val pillBorder = scheme.primaryContainer.copy(alpha = 0.7f).compositeOver(Color.Black)
    val dateFill = scheme.primary.copy(alpha = 0.05f).compositeOver(Color.Black)
    val dateOutline = scheme.outlineVariant.copy(alpha = 0.5f).compositeOver(Color.Black)
    val primaryText = scheme.onSurface
    val secondaryText = scheme.onSurfaceVariant

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.7f, animationSpec = tween(500), label = "alpha"
    )
    // Seconds track + seconds pill fade out in AOD
    val secondsAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f, animationSpec = tween(500), label = "secondsAlpha"
    )

    var time by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val isFullyInactive = !isActive && secondsAlpha == 0f
    LaunchedEffect(isFullyInactive) {
        while (true) {
            time = System.currentTimeMillis()
            if (!isFullyInactive) {
                delay(16.milliseconds)
            } else {
                val cal = Calendar.getInstance().apply { timeInMillis = time }
                val delayToNextMinute =
                    60000L - (cal.get(Calendar.SECOND) * 1000L + cal.get(Calendar.MILLISECOND))
                delay(max(delayToNextMinute, 100L).milliseconds)
            }
        }
    }

    val calendar = Calendar.getInstance().apply { timeInMillis = time }
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val displayHour = if (is24Hour) hour else (hour % 12).let { if (it == 0) 12 else it }
    val minute = calendar.get(Calendar.MINUTE)
    val second = calendar.get(Calendar.SECOND)
    val millis = calendar.get(Calendar.MILLISECOND)
    val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
    val dayName = dayFormat.format(calendar.time).uppercase(LocalLocale.current.platformLocale).trimEnd('.')

    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .aspectRatio(1f)
            .graphicsLayer(alpha = animatedAlpha)
    ) {
        val r = size.minDimension / 2f
        val cx = size.width / 2f
        val cy = size.height / 2f
        val center = Offset(cx, cy)

        fun pxToSp(px: Float): TextUnit =
            (px / localDensity.density / localDensity.fontScale).sp

        fun style(sizePx: Float, color: Color, weight: FontWeight = FontWeight.Medium) = TextStyle(
            color = color,
            fontSize = pxToSp(sizePx),
            fontWeight = weight,
            fontFamily = QuicksandTitleVariable
        )

        val secLabelStyle = style(r * 0.065f, secondaryText, FontWeight.Normal)
        val secPillStyle = style(r * 0.095f, accent, FontWeight.SemiBold)
        val minNumFontPx = r * 0.09f
        val minNumStyle = style(minNumFontPx, primaryText, FontWeight.SemiBold)
        val digitalStyle = style(r * 0.14f, pillText)
        val subHourStyle = style(r * 0.13f, primaryText)
        val dayStyle = style(r * 0.075f, primaryText)
        val dateStyle = style(r * 0.13f, accent, FontWeight.SemiBold)

        // Minute hand angle drives the whole cross layout.
        // Seconds fraction is always included so nothing jumps backwards when entering AOD.
        val minuteFloat = minute + (second + millis / 1000f) / 60f
        val minuteAngle = minuteFloat * 6f

        // Hour subdial opposite the hand, date at +75° (2:30), digital clock at -75° (9:30)
        val rOrbit = r * 0.42f
        val subCenter = polar(center, rOrbit, minuteAngle + 180f)
        val dateCenter = polar(center, rOrbit, minuteAngle + 75f)
        val digCenter = polar(center, rOrbit, minuteAngle - 75f)

        // ---------------------------------------------------------------
        // 1. Rotating seconds track (current second sits at 3 o'clock)
        // ---------------------------------------------------------------
        val rSec = r * 0.90f
        val pillAngle = 90f
        if (secondsAlpha > 0f) {
            val currentSec = second + millis / 1000f
            for (i in 0 until 60) {
                val angle = pillAngle - (i - currentSec) * 6f
                val distToPill = abs(((angle - pillAngle) % 360f + 540f) % 360f - 180f)
                val fade = ((distToPill - 10f) / 8f).coerceIn(0f, 1f) * secondsAlpha
                if (fade <= 0f) continue
                val pos = polar(center, rSec, angle)
                if (i % 2 == 0) {
                    val layout = textMeasurer.measure(
                        String.format(Locale.getDefault(), "%02d", i), secLabelStyle
                    )
                    drawCentered(layout, pos, fade)
                } else {
                    drawCircle(
                        color = secondaryText,
                        radius = r * 0.007f,
                        center = pos,
                        alpha = 0.6f * fade
                    )
                }
            }

            // Seconds pill
            val pillW = r * 0.21f
            val pillH = r * 0.15f
            // Keep the bigger pill inside the canvas edge
            val pillCenter = polar(center, r * 0.85f, pillAngle)
            drawRoundRect(
                color = accent,
                topLeft = Offset(pillCenter.x - pillW / 2f, pillCenter.y - pillH / 2f),
                size = Size(pillW, pillH),
                cornerRadius = CornerRadius(pillH / 2f),
                style = Stroke(width = r * 0.01f),
                alpha = secondsAlpha
            )
            val secLayout = textMeasurer.measure(
                String.format(Locale.getDefault(), "%02d", second), secPillStyle
            )
            drawCentered(secLayout, pillCenter, secondsAlpha)
        }

        // ---------------------------------------------------------------
        // 2. Fixed minute ring: dashes + radial numbers + 12 o'clock marker
        // ---------------------------------------------------------------
        // Ticks, numbers and the marker all share this outer edge
        val rRingOuter = r * 0.83f
        val shortTick = r * 0.032f
        val longTick = r * 0.052f
        // Half the visual height of the digits (cap height ≈ 0.72 × font size)
        val minNumHalfHeight = minNumFontPx * 0.36f
        val rMinNum = rRingOuter - minNumHalfHeight
        for (i in 1 until 60) {
            val angle = i * 6f
            if (i % 5 == 0) {
                val layout = textMeasurer.measure(
                    String.format(Locale.getDefault(), "%02d", i), minNumStyle
                )
                val pos = polar(center, rMinNum, angle)
                val rot = if (angle > 90f && angle < 270f) angle - 180f else angle
                // "15" sits under the seconds pill, so hide it while the seconds ring is visible
                val numAlpha = if (i == 15) 1f - secondsAlpha else 1f
                if (numAlpha > 0f) {
                    rotate(rot, pivot = pos) { drawCentered(layout, pos, numAlpha) }
                }
            } else {
                // 4 ticks between numbers: the 2 middle ones are longer
                val isMiddle = i % 5 == 2 || i % 5 == 3
                val len = if (isMiddle) longTick else shortTick
                // Ticks 14 and 16 sit beside the seconds pill: dim them partially while it's visible
                val tickAlpha = if (i == 14 || i == 16) 1f - 0.5f * secondsAlpha else 1f
                drawLine(
                    color = accent,
                    start = polar(center, rRingOuter - len, angle),
                    end = polar(center, rRingOuter, angle),
                    strokeWidth = r * 0.014f,
                    cap = StrokeCap.Round,
                    alpha = tickAlpha
                )
            }
        }

        // 12 o'clock marker (25% smaller), base on the shared outer edge
        val triBaseY = cy - r * 0.85f
        val triApex = Offset(cx, triBaseY + r * 0.105f)
        val triHalf = r * 0.06f
        val triCenter = Offset(cx, (triBaseY * 2f + triApex.y) / 3f)
        val holeRadius = r * 0.024f
        val triangle = roundedPolygonPath(
            points = listOf(
                Offset(cx - triHalf, triBaseY),
                Offset(cx + triHalf, triBaseY),
                triApex
            ),
            cornerRadius = r * 0.0286f
        ).apply {
            // Punch a real hole (transparent, so album art shows through correctly)
            addOval(
                Rect(
                    left = triCenter.x - holeRadius,
                    top = triCenter.y - holeRadius,
                    right = triCenter.x + holeRadius,
                    bottom = triCenter.y + holeRadius
                )
            )
            fillType = PathFillType.EvenOdd
        }
        drawPath(triangle, color = accent)
        // Dot in the middle of the hole
        drawCircle(color = accent, radius = r * 0.01f, center = triCenter)

        // ---------------------------------------------------------------
        // 3. Digital time pill (minute - 90°)
        // ---------------------------------------------------------------
        val digW = r * 0.43f
        val digH = r * 0.22f
        // Separate outline ring with a gap, so it never touches the container
        val borderGap = r * 0.022f
        val borderStroke = r * 0.012f
        val borderInset = borderGap + borderStroke / 2f
        drawRoundRect(
            color = pillBorder,
            topLeft = Offset(
                digCenter.x - digW / 2f - borderInset,
                digCenter.y - digH / 2f - borderInset
            ),
            size = Size(digW + borderInset * 2f, digH + borderInset * 2f),
            cornerRadius = CornerRadius(digH / 2f + borderInset),
            style = Stroke(width = borderStroke)
        )
        drawRoundRect(
            color = pillFill,
            topLeft = Offset(digCenter.x - digW / 2f, digCenter.y - digH / 2f),
            size = Size(digW, digH),
            cornerRadius = CornerRadius(digH / 2f)
        )
        val digitalText = if (is24Hour) {
            String.format(Locale.getDefault(), "%02d:%02d", displayHour, minute)
        } else {
            String.format(Locale.getDefault(), "%d:%02d", displayHour, minute)
        }
        drawCentered(textMeasurer.measure(digitalText, digitalStyle), digCenter)

        // ---------------------------------------------------------------
        // 4. Date complication (minute + 90°)
        // ---------------------------------------------------------------
        val dateRadius = r * 0.20f
        drawCircle(color = dateFill, radius = dateRadius, center = dateCenter)
        // Separate, subtle outline ring with a gap (same spacing as the digital clock)
        drawCircle(
            color = dateOutline,
            radius = dateRadius + borderInset,
            center = dateCenter,
            style = Stroke(width = borderStroke)
        )
        drawCentered(
            textMeasurer.measure(dayName, dayStyle),
            Offset(dateCenter.x, dateCenter.y - r * 0.068f)
        )
        drawCentered(
            textMeasurer.measure(String.format(Locale.getDefault(), "%02d", dayOfMonth), dateStyle),
            Offset(dateCenter.x, dateCenter.y + r * 0.058f)
        )

        // ---------------------------------------------------------------
        // 5. Hour subdial (opposite the minute hand, always upright)
        // ---------------------------------------------------------------
        val rs = r * 0.26f
        for (i in 0 until 12) {
            val angle = i * 30f
            val isQuarter = i % 3 == 0
            drawLine(
                color = accent,
                start = polar(subCenter, rs * if (isQuarter) 0.78f else 0.84f, angle),
                end = polar(subCenter, rs, angle),
                strokeWidth = r * if (isQuarter) 0.016f else 0.012f,
                cap = StrokeCap.Round
            )
            // Half-hour dot between the hour ticks
            drawCircle(
                color = accent,
                radius = r * 0.008f,
                center = polar(subCenter, rs * 0.92f, angle + 15f)
            )
        }
        val hourAngle = ((hour % 12) + minute / 60f) * 30f
        drawRadialCapsule(
            pivot = subCenter,
            angleDeg = hourAngle,
            inner = rs * 0.20f,
            outer = rs * 0.80f,
            thickness = r * 0.05f,
            fill = Color.Black,
            outline = primaryText,
            outlineWidth = r * 0.01f
        )
        drawCircle(color = accent, radius = r * 0.018f, center = subCenter)
        drawCentered(
            textMeasurer.measure(displayHour.toString(), subHourStyle),
            polar(subCenter, rs * 0.48f, hourAngle + 180f)
        )

        // ---------------------------------------------------------------
        // 6. Minute hand + center ring (on top)
        // ---------------------------------------------------------------
        drawRadialCapsule(
            pivot = center,
            angleDeg = minuteAngle,
            inner = r * 0.11f,
            outer = r * 0.73f,
            thickness = r * 0.065f,
            fill = handFill,
            outline = accent,
            outlineWidth = r * 0.014f
        )
        drawCircle(
            color = accent,
            radius = r * 0.024f,
            center = center,
            style = Stroke(width = r * 0.01f)
        )
    }
}

/** Closed polygon path with every corner rounded (corners cut back by [cornerRadius] along each edge). */
private fun roundedPolygonPath(points: List<Offset>, cornerRadius: Float): Path {
    val n = points.size
    fun towards(from: Offset, to: Offset, dist: Float): Offset {
        val d = to - from
        val len = d.getDistance()
        if (len == 0f) return from
        val t = (dist / len).coerceAtMost(0.5f)
        return Offset(from.x + d.x * t, from.y + d.y * t)
    }
    return Path().apply {
        for (i in 0 until n) {
            val prev = points[(i - 1 + n) % n]
            val curr = points[i]
            val next = points[(i + 1) % n]
            val start = towards(curr, prev, cornerRadius)
            val end = towards(curr, next, cornerRadius)
            if (i == 0) moveTo(start.x, start.y) else lineTo(start.x, start.y)
            quadraticTo(curr.x, curr.y, end.x, end.y)
        }
        close()
    }
}

/** Point on a circle; angle in degrees, clockwise from 12 o'clock. */
private fun polar(origin: Offset, radius: Float, angleDeg: Float): Offset {
    val rad = Math.toRadians(angleDeg.toDouble())
    return Offset(
        origin.x + radius * sin(rad).toFloat(),
        origin.y - radius * cos(rad).toFloat()
    )
}

private fun DrawScope.drawCentered(layout: TextLayoutResult, center: Offset, alpha: Float = 1f) {
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(
            center.x - layout.size.width / 2f,
            center.y - layout.size.height / 2f - layout.size.height * 0.02f
        ),
        alpha = alpha
    )
}

/** Outlined capsule pointing outward from [pivot] at [angleDeg] (clockwise from 12). */
private fun DrawScope.drawRadialCapsule(
    pivot: Offset,
    angleDeg: Float,
    inner: Float,
    outer: Float,
    thickness: Float,
    fill: Color,
    outline: Color,
    outlineWidth: Float,
    alpha: Float = 1f
) {
    rotate(angleDeg, pivot = pivot) {
        val topLeft = Offset(pivot.x - thickness / 2f, pivot.y - outer)
        val capsuleSize = Size(thickness, outer - inner)
        val corner = CornerRadius(thickness / 2f)
        drawRoundRect(
            color = fill, topLeft = topLeft, size = capsuleSize,
            cornerRadius = corner, alpha = alpha
        )
        drawRoundRect(
            color = outline, topLeft = topLeft, size = capsuleSize,
            cornerRadius = corner, style = Stroke(width = outlineWidth), alpha = alpha
        )
    }
}