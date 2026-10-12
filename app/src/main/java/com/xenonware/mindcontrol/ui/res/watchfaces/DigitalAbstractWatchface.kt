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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.xenonware.mindcontrol.MediaInfo
import com.xenonware.mindcontrol.ui.res.UnifiedAodStyle
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun DigitalAbstractAodStyle(
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
        watchFace = { DigitalAbstractWatchFace(isActive = isActive) }
    )
}

// --- Layout, in units of one digit's width ---
// Every digit sits on a grid of square modules: 5 wide x 7.5 tall (1 module = 0.2).
private const val M = 0.2f                               // one grid module
private const val CELL_H = 7.5f * M                      // digit height (1.5)
private const val GAP = M / 2f                           // gap between pieces inside a digit
private const val COL = (1f - GAP) / 2f                  // two equal (50/50) columns: 0.45 each
private const val LEFT_W = COL                           // left column: 0 .. 0.45
private const val RIGHT_X = COL + GAP                    // right column: 0.55 .. 1
private const val CIRCLE = 2f * M                        // every circle is 2 x 2 modules
private const val R = CIRCLE / 2f
private const val WIDE_X = CIRCLE + GAP                  // 2, 3, 5: shape next to the circles, 0.5 .. 1
private const val STROKE = 2f * M                        // every pill / rectangle is 2 modules thick

private const val GAP_X = 0.12f                          // gap between the two digit columns
private const val GAP_Y = 0.12f                          // gap between hour row and minute row
private const val SMALL_CORNER = 0.07f                   // radius of the "flat" corners
// Corner curves (superellipse exponent): 2 = circular, 3 = smooth, 4 = squircle
private const val PILL_N = 2f            // pills / rounded rectangles: fully circular
private const val CURVE_N = 2.5f         // half circles and the 0: between circular and smooth
private const val HALF_FLAT_CORNER = 0.11f // softer corners on the flat side of half circles
private const val BORDER_W = 0.035f                      // border thickness (x digit width)
private const val TOTAL_W = 2f + GAP_X
private const val TOTAL_H = CELL_H * 2f + GAP_Y


@Composable
fun DigitalAbstractWatchFace(isActive: Boolean) {
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)

    // Material You, always the dark scheme
    val scheme = remember(context) { dynamicDarkColorScheme(context) }
    val fillColor = scheme.primaryContainer   // solid body
    val borderColor = scheme.primary          // solid rim

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.7f, animationSpec = tween(500), label = "alpha"
    )
    // Screen-saver (AOD): the fill disappears completely, only the border stays
    val fillAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f, animationSpec = tween(700), label = "fill"
    )

    // Only minutes are shown, so one update per minute is enough
    var time by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            time = System.currentTimeMillis()
            val cal = Calendar.getInstance().apply { timeInMillis = time }
            val delayToNextMinute =
                60000L - (cal.get(Calendar.SECOND) * 1000L + cal.get(Calendar.MILLISECOND))
            delay(max(delayToNextMinute, 100L).milliseconds)
        }
    }

    val calendar = Calendar.getInstance().apply { timeInMillis = time }
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val displayHour = if (is24Hour) hour else (hour % 12).let { if (it == 0) 12 else it }
    val minute = calendar.get(Calendar.MINUTE)

    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.56f)
            .aspectRatio(TOTAL_W / TOTAL_H)
            .graphicsLayer(alpha = animatedAlpha)
    ) {
        // The border is centred on each outline, so half of it sits outside the shapes.
        // Reserve that half (plus 1px for antialiasing) on every side so the canvas /
        // graphicsLayer bounds never clip it.
        val unit = min(
            (size.width - 2f) / (TOTAL_W + BORDER_W),
            (size.height - 2f) / (TOTAL_H + BORDER_W)
        ) // one digit width in px
        val borderWidth = unit * BORDER_W
        val inset = borderWidth / 2f + 1f
        val cellH = unit * CELL_H
        val originX = inset + (size.width - 2f * inset - TOTAL_W * unit) / 2f
        val originY = inset + (size.height - 2f * inset - TOTAL_H * unit) / 2f

        val digits = intArrayOf(displayHour / 10, displayHour % 10, minute / 10, minute % 10)
        digits.forEachIndexed { index, digit ->
            val col = index % 2
            val row = index / 2
            val cellOrigin = Offset(
                originX + col * (unit + GAP_X * unit),
                originY + row * (cellH + GAP_Y * unit)
            )
            for (part in digitParts(digit)) {
                val path = part.toPath(cellOrigin, unit)
                if (fillAlpha > 0f) drawPath(path, color = fillColor, alpha = fillAlpha)
                drawPath(path, color = borderColor, style = Stroke(width = borderWidth))
            }
        }
    }
}

// ---------------------------------------------------------------------
// Digit shapes
// All coordinates are in units of the digit width: x 0..1, y 0..CELL_H.
// ---------------------------------------------------------------------

private sealed interface Part {
    fun toPath(origin: Offset, unit: Float): Path
    fun rotated180(): Part
}

private fun Offset.at(x: Float, y: Float, unit: Float) = Offset(this.x + x * unit, this.y + y * unit)

/**
 * Rectangle whose corners are superellipse quarters (|x|^n + |y|^n = 1) instead of circular
 * arcs. Each corner has its own horizontal/vertical radius. With n > 2 the curve flows into
 * the straight edges without a visible "kink", which reads much less circular.
 */
private fun Path.addSquircleRect(
    rect: Rect, tl: Size, tr: Size, br: Size, bl: Size, n: Float
) {
    squircleCorner(rect.left + tl.width, rect.top + tl.height, tl.width, tl.height, 180f, n, true)
    squircleCorner(rect.right - tr.width, rect.top + tr.height, tr.width, tr.height, 270f, n)
    squircleCorner(rect.right - br.width, rect.bottom - br.height, br.width, br.height, 0f, n)
    squircleCorner(rect.left + bl.width, rect.bottom - bl.height, bl.width, bl.height, 90f, n)
    close()
}

/** One superellipse quarter around (cx, cy), sweeping 90° clockwise from [startDeg]. */
private fun Path.squircleCorner(
    cx: Float, cy: Float, rx: Float, ry: Float, startDeg: Float, n: Float, first: Boolean = false
) {
    val steps = 24
    val exp = 2.0 / n
    for (i in 0..steps) {
        val a = Math.toRadians((startDeg + 90f * i / steps).toDouble())
        val c = cos(a)
        val sn = sin(a)
        val x = cx + rx * (sign(c) * abs(c).pow(exp)).toFloat()
        val y = cy + ry * (sign(sn) * abs(sn).pow(exp)).toFloat()
        if (first && i == 0) moveTo(x, y) else lineTo(x, y)
    }
}

/**
 * Rounded rectangle; each corner is fully round (`true`, half of the shorter side)
 * or a small soft corner (`false`).
 */
private data class Blob(
    val l: Float, val t: Float, val r: Float, val b: Float,
    val tl: Boolean, val tr: Boolean, val br: Boolean, val bl: Boolean
) : Part {
    override fun toPath(origin: Offset, unit: Float): Path {
        val rect = Rect(origin.at(l, t, unit), origin.at(r, b, unit))
        val full = min(rect.width, rect.height) / 2f
        val small = SMALL_CORNER * unit
        fun cr(round: Boolean) = if (round) Size(full, full) else Size(small, small)
        return Path().apply { addSquircleRect(rect, cr(tl), cr(tr), cr(br), cr(bl), PILL_N) }
    }

    override fun rotated180() = Blob(1 - r, CELL_H - b, 1 - l, CELL_H - t, br, bl, tl, tr)
}

/** Circle. */
private data class Dot(val x: Float, val y: Float, val radius: Float) : Part {
    override fun toPath(origin: Offset, unit: Float): Path {
        val c = origin.at(x, y, unit)
        val rPx = radius * unit
        return Path().apply { addOval(Rect(c.x - rPx, c.y - rPx, c.x + rPx, c.y + rPx)) }
    }

    override fun rotated180() = Dot(1 - x, CELL_H - y, radius)
}

/** Ellipse filling the box. */
private data class Oval(val l: Float, val t: Float, val r: Float, val b: Float) : Part {
    override fun toPath(origin: Offset, unit: Float): Path {
        val box = Rect(origin.at(l, t, unit), origin.at(r, b, unit))
        val q = Size(box.width / 2f, box.height / 2f)
        return Path().apply { addSquircleRect(box, q, q, q, q, CURVE_N) }
    }

    override fun rotated180() = Oval(1 - r, CELL_H - b, 1 - l, CELL_H - t)
}

/**
 * Half circle / half ellipse filling the box: flat on one side, round on the other.
 * [roundRight] = true → flat left edge, round right side ("D").
 */
private data class Half(
    val l: Float, val t: Float, val r: Float, val b: Float, val roundRight: Boolean
) : Part {
    override fun toPath(origin: Offset, unit: Float): Path {
        val box = Rect(origin.at(l, t, unit), origin.at(r, b, unit))
        val soft = HALF_FLAT_CORNER * unit
        val flat = Size(soft, soft)
        // The round side's two corners together span the whole height
        val round = Size(box.width - soft, box.height / 2f)
        return Path().apply {
            if (roundRight) addSquircleRect(box, flat, round, round, flat, CURVE_N)
            else addSquircleRect(box, round, flat, flat, round, CURVE_N)
        }
    }

    override fun rotated180() = Half(1 - r, CELL_H - b, 1 - l, CELL_H - t, !roundRight)
}

/** Pill between two points at any angle; [thickness] in digit-width units. */
private data class Capsule(
    val x1: Float, val y1: Float, val x2: Float, val y2: Float, val thickness: Float
) : Part {
    override fun toPath(origin: Offset, unit: Float): Path {
        val p1 = origin.at(x1, y1, unit)
        val p2 = origin.at(x2, y2, unit)
        val t = thickness * unit
        val length = (p2 - p1).getDistance() + t
        val mid = (p1 + p2) / 2f
        val angleDeg =
            Math.toDegrees(atan2((p2.y - p1.y).toDouble(), (p2.x - p1.x).toDouble())).toFloat()
        val matrix = Matrix().apply {
            translate(mid.x, mid.y)
            rotateZ(angleDeg)
        }
        val end = Size(t / 2f, t / 2f)
        return Path().apply {
            addSquircleRect(Rect(-length / 2f, -t / 2f, length / 2f, t / 2f), end, end, end, end, PILL_N)
            transform(matrix)
        }
    }

    override fun rotated180() = Capsule(1 - x1, CELL_H - y1, 1 - x2, CELL_H - y2, thickness)
}

/**
 * A rounded "nose" growing out of a vertical wall at x = [wall], centred at y = [mid].
 * The outline leaves the wall tangentially, swings out in a smooth S into a round peak
 * [reach] away from the wall and flows back in the same way, so merged with a body it
 * looks like one continuous shape instead of a separate oval clipped on.
 * [flow] = half the height over which it blends into the wall, [peakK] / [flowK] = how
 * round the peak / how soft the transition into the wall is. Points left if [left].
 */
private data class Bump(
    val wall: Float, val mid: Float, val reach: Float, val flow: Float,
    val peakK: Float, val flowK: Float, val left: Boolean = true
) : Part {
    override fun toPath(origin: Offset, unit: Float): Path {
        val d = if (left) 1f else -1f
        val peakX = wall - d * reach
        val inside = wall + d * 0.1f                 // closes the shape inside the body
        fun p(x: Float, y: Float) = origin.at(x, y, unit)
        return Path().apply {
            p(inside, mid - flow).let { moveTo(it.x, it.y) }
            p(wall, mid - flow).let { lineTo(it.x, it.y) }
            val a1 = p(wall, mid - flow + flowK); val a2 = p(peakX, mid - peakK); val a3 = p(peakX, mid)
            cubicTo(a1.x, a1.y, a2.x, a2.y, a3.x, a3.y)
            val b1 = p(peakX, mid + peakK); val b2 = p(wall, mid + flow - flowK); val b3 = p(wall, mid + flow)
            cubicTo(b1.x, b1.y, b2.x, b2.y, b3.x, b3.y)
            p(inside, mid + flow).let { lineTo(it.x, it.y) }
            close()
        }
    }

    override fun rotated180() = Bump(1 - wall, CELL_H - mid, reach, flow, peakK, flowK, !left)
}

/**
 * Tall body with a waist on its outer side (used by the 3 and both halves of the 8).
 * The outer side has a round lobe at the top and bottom. Between them two straight flanks
 * meet at [angleDeg] (150° by default), with a round tip of radius [filletR] at the
 * deepest point, [depth] in from the outer edge. Each flank eases tangentially into its
 * lobe, so the waist sits smoothly in the shape. The inner side is flat with soft corners.
 * [outerLeft] = which side carries the lobes; [lobeH] = height of each lobe's corner curve;
 * [flankLen] / [lobeEase] = handle lengths along the flank / into the lobe.
 */
private data class Waisted(
    val l: Float, val t: Float, val r: Float, val b: Float, val outerLeft: Boolean,
    val lobeH: Float = 0.34f, val depth: Float = 0.15f, val filletR: Float = 0.10f,
    val flankLen: Float = 0.12f, val lobeEase: Float = 0.26f, val angleDeg: Float = 150f
) : Part {
    override fun toPath(origin: Offset, unit: Float): Path {
        // Built with the lobes on the left, then mirrored if they belong on the right
        val box = Rect(origin.at(l, t, unit), origin.at(r, b, unit))
        val soft = HALF_FLAT_CORNER * unit
        val h1 = lobeH * unit
        val rk = filletR * unit
        val ease = lobeEase * unit
        val mid = box.center.y
        val rx = box.width - soft
        // Each flank's angle from vertical, so the two flanks meet at angleDeg
        val half = Math.toRadians(90.0 - angleDeg / 2.0)
        val sinA = sin(half).toFloat()
        val cosA = cos(half).toFloat()
        val flx = flankLen * unit * sinA                    // flank handle, split into x / y
        val fly = flankLen * unit * cosA
        val cxk = box.left + depth * unit - rk              // center of the round tip
        val tipX = cxk + rk * cosA                          // where the flanks touch the tip
        val tipDy = rk * sinA
        val path = Path().apply {
            squircleCorner(box.right - soft, box.top + soft, soft, soft, 270f, CURVE_N, true)
            squircleCorner(box.right - soft, box.bottom - soft, soft, soft, 0f, CURVE_N)
            squircleCorner(box.left + rx, box.bottom - h1, rx, h1, 90f, CURVE_N)
            // bottom lobe → lower flank
            cubicTo(
                box.left, box.bottom - h1 - ease,
                tipX - flx, mid + tipDy + fly,
                tipX, mid + tipDy
            )
            // round tip
            arcTo(
                Rect(cxk - rk, mid - rk, cxk + rk, mid + rk),
                90f - angleDeg / 2f, -(180f - angleDeg), false
            )
            // upper flank → top lobe
            cubicTo(
                tipX - flx, mid - tipDy - fly,
                box.left, box.top + h1 + ease,
                box.left, box.top + h1
            )
            squircleCorner(box.left + rx, box.top + h1, rx, h1, 180f, CURVE_N)
            close()
        }
        if (!outerLeft) {
            path.transform(Matrix().apply {
                translate(box.left + box.right, 0f)
                scale(-1f, 1f)
            })
        }
        return path
    }

    override fun rotated180() = Waisted(
        1 - r, CELL_H - b, 1 - l, CELL_H - t, !outerLeft, lobeH, depth, filletR, flankLen, lobeEase,
        angleDeg
    )
}

/** Several primitives merged into one shape with a single outer border (no seams). */
private data class Union(val parts: List<Part>) : Part {
    constructor(vararg parts: Part) : this(parts.toList())

    override fun toPath(origin: Offset, unit: Float): Path =
        parts.map { it.toPath(origin, unit) }
            .reduce { acc, p -> Path.combine(PathOperation.Union, acc, p) }

    override fun rotated180() = Union(parts.map { it.rotated180() })
}

private const val T = true

private val NINE = listOf(
    Half(0f, 0f, LEFT_W, CELL_H - CIRCLE - GAP, roundRight = false), // bowl above the circle
    Dot(COL / 2f, CELL_H - R, R),                              // circle, centred in the left column
    Half(RIGHT_X, 0f, 1f, CELL_H, roundRight = true)           // right half of a 0 (split 0)
)

private val DIGIT_PARTS: Array<List<Part>> = arrayOf(
    // 0 – one whole oval over the full grid
    listOf(Oval(0f, 0f, 1f, CELL_H)),
    // 1 – circle at the 1/3 mark + full-height pill on the right
    listOf(
        Dot(1f - STROKE - GAP - R, CELL_H / 3f, R),   // circle snug next to the pill (one gap)
        Blob(1f - STROKE, 0f, 1f, CELL_H, T, T, T, T)
    ),
    // 2 – circle centred at 1/3 of the half circle, half circle in the right column,
    //     full-width pill (2 modules tall) along the bottom
    listOf(
        Dot(R, (CELL_H - STROKE - GAP) / 3f, R),
        Half(WIDE_X, 0f, 1f, CELL_H - STROKE - GAP, roundRight = true),
        Blob(0f, CELL_H - STROKE, 1f, CELL_H, T, T, T, T)
    ),
    // 3 – circles top-left + bottom-left, body: two right lobes + a round nose flowing out to the left
    listOf(
        Dot(R, R, R),
        Dot(R, CELL_H - R, R),
        Union(
            Waisted(WIDE_X, 0f, 1f, CELL_H, outerLeft = false),
            Bump(WIDE_X, CELL_H / 2f, reach = 0.24f, flow = 0.40f, peakK = 0.18f, flowK = 0.20f)
        )
    ),
    // 4 – diagonal filling a 3.75 x 5.5 module box (top-left) + vertical pill bottom-right
    listOf(
        Capsule(
            0.75f - STROKE / 2f, STROKE / 2f,        // top-right end
            STROKE / 2f, 5.5f * M - STROKE / 2f,     // bottom-left end
            STROKE
        ),
        Blob(1f - STROKE, 3.5f * M, 1f, CELL_H, T, T, T, T)
    ),
    // 5 – left stem, circle top-right, half circle right (below the circle), circle bottom-left
    listOf(
        Blob(0f, 0f, STROKE, CELL_H - CIRCLE - GAP, T, T, T, T),
        Dot((WIDE_X + 1f) / 2f, R, R),
        Half(WIDE_X, CIRCLE + GAP, 1f, CELL_H, roundRight = true),
        Dot(R, CELL_H - R, R)
    ),
    // 6 – a 9 turned upside down
    NINE.map { it.rotated180() },
    // 7 – horizontal pill top-left (2.5 x 2 modules) + steep diagonal (right edge, top → center, bottom)
    listOf(
        Blob(0f, 0f, 2.5f * M, STROKE, T, T, T, T),
        Capsule(1f - STROKE / 2f, STROKE / 2f, 0.5f, CELL_H - STROKE / 2f, STROKE)
    ),
    // 8 – two full-height halves, each with two lobes on its outer side
    listOf(
        Waisted(0f, 0f, LEFT_W, CELL_H, outerLeft = true),
        Waisted(RIGHT_X, 0f, 1f, CELL_H, outerLeft = false)
    ),
    // 9
    NINE
)

private fun digitParts(digit: Int): List<Part> = DIGIT_PARTS[digit.coerceIn(0, 9)]