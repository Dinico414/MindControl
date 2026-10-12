package com.xenonware.mindcontrol.ui.res.watchfaces

import android.graphics.CornerPathEffect
import android.graphics.Paint
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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.xenonware.mindcontrol.MediaInfo
import com.xenonware.mindcontrol.ui.res.UnifiedAodStyle
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import android.graphics.Path as NativePath

@Composable
fun CubedAodStyle(
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
        watchFace = { CubedWatchFace(isActive = isActive) }
    )
}

// --- Layout, in units of one digit's width ---
// Each digit is a solid block on a 3 x 5 grid with non-uniform cells: thick outer strokes,
// thin slots. Removing cells cuts the slits and notches that form the digit.
private val COL_W = floatArrayOf(0.4f, 0.2f, 0.4f)                   // sums to 1
private val ROW_H = floatArrayOf(0.30f, 0.11f, 0.18f, 0.11f, 0.30f)  // sums to 1 (x DIGIT_H)
private const val DIGIT_H = 1.0f        // digit height
private const val CORNER = 0.14f        // corner radius (slits get fully round ends automatically)
private const val GAP = 0.08f           // gap between digits
private const val BORDER_W = 0.035f     // outline thickness in screen-saver mode
private const val TOTAL_W = 3f + 2f * GAP     // hours/minutes + seconds column
private const val TOTAL_H = 2f * DIGIT_H + GAP

/** 3 x 5 bitmaps, top row first; bit 2 = left column, bit 0 = right column. */
private val CUBED_PATTERNS = arrayOf(
    intArrayOf(0b111, 0b101, 0b101, 0b101, 0b111), // 0
    intArrayOf(0b110, 0b010, 0b010, 0b010, 0b111), // 1
    intArrayOf(0b111, 0b001, 0b111, 0b100, 0b111), // 2
    intArrayOf(0b111, 0b001, 0b111, 0b001, 0b111), // 3
    intArrayOf(0b101, 0b101, 0b111, 0b001, 0b001), // 4
    intArrayOf(0b111, 0b100, 0b111, 0b001, 0b111), // 5
    intArrayOf(0b111, 0b100, 0b111, 0b101, 0b111), // 6
    intArrayOf(0b111, 0b001, 0b001, 0b001, 0b001), // 7
    intArrayOf(0b111, 0b101, 0b111, 0b101, 0b111), // 8
    intArrayOf(0b111, 0b101, 0b111, 0b001, 0b111)  // 9
)

@Composable
fun CubedWatchFace(isActive: Boolean) {
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)

    // Material You (dark): hours primary, minutes tertiary, seconds secondary
    val scheme = remember(context) { dynamicDarkColorScheme(context) }
    val hourColor = scheme.primary.toArgb()
    val minuteColor = scheme.tertiary.toArgb()
    val secondColor = scheme.secondary.toArgb()

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.7f, animationSpec = tween(500), label = "alpha"
    )
    // Screen-saver (AOD): the fill disappears, only the outline stays
    val fillAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f, animationSpec = tween(700), label = "fill"
    )

    // Seconds column: disappears completely in the screen saver
    val secondsAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f, animationSpec = tween(500), label = "seconds"
    )

    // Tick every second while seconds are visible, otherwise once a minute
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
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val displayHour = if (is24Hour) hour else (hour % 12).let { if (it == 0) 12 else it }
    val minute = calendar.get(Calendar.MINUTE)
    val second = calendar.get(Calendar.SECOND)

    val fillPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL } }
    val strokePaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.78f)
            .aspectRatio(TOTAL_W / TOTAL_H)
            .graphicsLayer(alpha = animatedAlpha)
    ) {
        // Keep half the outline (+1px) inside the canvas so it is never clipped
        val unit = min(
            (size.width - 2f) / (TOTAL_W + BORDER_W),
            (size.height - 2f) / (TOTAL_H + BORDER_W)
        )
        val borderWidth = unit * BORDER_W
        val inset = borderWidth / 2f + 1f
        // While the seconds column fades out, hours/minutes slide back to the center
        val contentW = 2f + GAP + secondsAlpha * (1f + GAP)
        val originX = inset + (size.width - 2f * inset - contentW * unit) / 2f
        val originY = inset + (size.height - 2f * inset - TOTAL_H * unit) / 2f
        strokePaint.strokeWidth = borderWidth

        fun cell(col: Int, row: Int, digit: Int) = cubedDigitPath(
            digit,
            originX + col * (1f + GAP) * unit,
            originY + row * (DIGIT_H + GAP) * unit,
            unit, unit * DIGIT_H, CORNER * unit
        )

        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            fun draw(path: NativePath, color: Int, fill: Float, stroke: Float) {
                if (fill > 0f) {
                    fillPaint.color = color
                    fillPaint.alpha = (fill * 255).roundToInt()
                    native.drawPath(path, fillPaint)
                }
                if (stroke > 0f) {
                    strokePaint.color = color
                    strokePaint.alpha = (stroke * 255).roundToInt()
                    native.drawPath(path, strokePaint)
                }
            }

            // Hours (primary) and minutes (tertiary)
            draw(cell(0, 0, displayHour / 10), hourColor, fillAlpha, 1f)
            draw(cell(1, 0, displayHour % 10), hourColor, fillAlpha, 1f)
            draw(cell(0, 1, minute / 10), minuteColor, fillAlpha, 1f)
            draw(cell(1, 1, minute % 10), minuteColor, fillAlpha, 1f)

            // Seconds (secondary): tens next to the hours, ones next to the minutes.
            // Border 100 %, fill 50 %, both gone in the screen saver.
            if (secondsAlpha > 0f) {
                draw(cell(2, 0, second / 10), secondColor, 0.5f * secondsAlpha, secondsAlpha)
                draw(cell(2, 1, second % 10), secondColor, 0.5f * secondsAlpha, secondsAlpha)
            }
        }
    }
}

/**
 * Builds one digit: the filled cells are merged into a single outline (with holes),
 * then every corner – outer, inner and the ends of the slits – is rounded.
 */
private fun cubedDigitPath(
    digit: Int, left: Float, top: Float, w: Float, h: Float, radius: Float
): NativePath {
    val pattern = CUBED_PATTERNS[digit.coerceIn(0, 9)]
    val merged = NativePath()
    var y = top
    for (row in 0 until 5) {
        val rh = ROW_H[row] * h
        var x = left
        for (col in 0 until 3) {
            val cw = COL_W[col] * w
            if ((pattern[row] shr (2 - col)) and 1 == 1) {
                val cell = NativePath().apply { addRect(x, y, x + cw, y + rh, NativePath.Direction.CW) }
                merged.op(cell, NativePath.Op.UNION)
            }
            x += cw
        }
        y += rh
    }
    // CornerPathEffect rounds every corner; it caps the radius at half of each edge,
    // so thin slits automatically get fully round ends.
    val rounded = NativePath()
    Paint().apply { pathEffect = CornerPathEffect(radius) }.getFillPath(merged, rounded)
    rounded.fillType = NativePath.FillType.EVEN_ODD // keep the holes open
    return rounded
}