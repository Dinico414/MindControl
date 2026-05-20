package com.xenonware.mindcontrol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.xenon.mylibrary.res.XenonDialog
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenonware.mindcontrol.ui.theme.PaletteTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class QrCodeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent.getStringExtra("EXTRA_QR_TEXT") ?: run {
            finish()
            return
        }

        setContent {
            val context = this@QrCodeActivity
            val devicePalette = remember { SettingsManager.getDevicePalette(context) }
            PaletteTheme(palette = devicePalette) {
                QrCodeContent(
                    text = text,
                    onDismiss = { finish() }
                )
            }
        }
    }
}

@Composable
fun QrCodeContent(text: String, onDismiss: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 2 })

    XenonDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(decorFitsSystemWindows = true),
        title = "QR-Code Share",
        containerColor = MaterialTheme.colorScheme.surface,
        confirmContainerColor = MaterialTheme.colorScheme.primary,
        confirmContentColor = MaterialTheme.colorScheme.onPrimary,
        contentManagesScrolling = true,
        content = {

            Box(
                modifier = Modifier
                    .size(240.dp)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(30.dp))
                ) { page ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (page == 0) {
                            ModernQrCode(text = text)
                        } else {
                            DefaultQrCode(text = text)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pager Indicator
            Row(
                Modifier
                    .height(8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(2) { iteration ->
                    val color = if (pagerState.currentPage == iteration)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outlineVariant
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontFamily = QuicksandTitleVariable,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    )
}

@Composable
fun ModernQrCode(
    text: String,
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.secondary
) {
    val bitMatrix = remember(text) {
        try {
            val hints = mutableMapOf<EncodeHintType, Any>()
            hints[EncodeHintType.MARGIN] = 0
            QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, hints)
        } catch (_: Exception) { null }
    } ?: return

    val matrixSize = bitMatrix.width
    val plan = remember(bitMatrix) { computeCellPlan(bitMatrix, matrixSize) }
    val infiniteTransition = rememberInfiniteTransition(label = "QrAnimation")

    val jitterTime by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing))
    )

    val flowerScale by infiniteTransition.animateFloat(
        initialValue = 0.985f, targetValue = 1.015f,
        animationSpec = infiniteRepeatable(tween(4500, easing = LinearEasing), RepeatMode.Reverse)
    )

    val finderRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing))
    )

    val flowerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
    val offDotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)

    Canvas(modifier = modifier.aspectRatio(1f)) {
        val canvasCenter = Offset(size.width / 2f, size.height / 2f)

        withTransform({ scale(flowerScale, flowerScale, canvasCenter) }) {
            val cloverPath = buildCookiePath(canvasCenter, size.width * 0.5f, 4, size.width * 0.08f, (PI / 4f).toFloat())
            drawPath(cloverPath, color = flowerColor)
        }

        val qrSize = size.width * 0.70f
        val qrInset = (size.width - qrSize) / 2f
        val moduleSize = qrSize / matrixSize
        val thickness = moduleSize * 0.78f

        withTransform({ translate(qrInset, qrInset) }) {
            val dotRandom = kotlin.random.Random(text.hashCode())
            val offRadius = moduleSize * 0.28f

            // Expanded loop to cover the 2-row corner zones
            for (y in -2..matrixSize + 1) {
                for (x in -2..matrixSize + 1) {
                    val isInsideGrid = x in 0 until matrixSize && y in 0 until matrixSize

                    // Determine if we are in a "Corner Zone" (The 2-row deep 7x7 cloud areas)
                    val isNearXCorner = x !in 0..<matrixSize
                    val isNearYCorner = y !in 0..<matrixSize
                    val isDeepOuter = x == -2 || x == matrixSize + 1 || y == -2 || y == matrixSize + 1

                    // The 8-Point Spawn Rule Logic:
                    // 1. Only allow the 2nd row (-2 or matrixSize + 1) if we are in a corner area
                    val isForbiddenSecondRow = isDeepOuter && !(isNearXCorner && isNearYCorner)
                    if (isForbiddenSecondRow) continue

                    val isOnBit = isInsideGrid && bitMatrix.get(x, y)
                    val isCornerGap = isInsideGrid && isAtFinderCorner(x, y, matrixSize)
                    val isStructural = isInsideGrid && plan.isStructural(x, y) && !isCornerGap

                    if (!isOnBit && !isStructural) {
                        val roll = dotRandom.nextFloat()

                        // Probability thresholds based on your rule
                        val threshold = when {
                            isInsideGrid -> 0.70f
                            isNearXCorner && isNearYCorner -> 0.52f // 1.5x of 0.35f (Corners)
                            else -> 0.35f // 1.0x (Sides)
                        }

                        if (roll < threshold) {
                            var centerX = x * moduleSize + moduleSize / 2f
                            var centerY = y * moduleSize + moduleSize / 2f

                            if (!isInsideGrid) {
                                // Jitter restricted to 1.5f of the dot size
                                val moveRange = offRadius * 1.2f
                                val phase = (x * 31 + y * 17).toFloat()
                                centerX += cos(jitterTime + phase) * moveRange
                                centerY += sin(jitterTime + phase * 1.3f) * moveRange
                            }

                            val shapeType = (dotRandom.nextInt(100)).mod(3)
                            drawOffDotShape(Offset(centerX, centerY), offRadius, shapeType, offDotColor, x, y)
                        }
                    }
                }
            }

            // Standard QR Components
            drawFinderPattern(0f, 0f, moduleSize, primaryColor, secondaryColor, finderRotation)
            drawFinderPattern((matrixSize - 7) * moduleSize, 0f, moduleSize, primaryColor, secondaryColor, finderRotation)
            drawFinderPattern(0f, (matrixSize - 7) * moduleSize, moduleSize, primaryColor, secondaryColor, finderRotation)

            plan.blocks.forEach { drawBlock2x2(it.x, it.y, moduleSize, pickColor(it.x, it.y, primaryColor, secondaryColor)) }
            plan.horizontalRuns.forEach { drawHorizontalRun(it, moduleSize, thickness, primaryColor, secondaryColor) }
            plan.verticalRuns.forEach { drawVerticalRun(it, moduleSize, thickness, primaryColor, secondaryColor) }
            plan.singles.forEach { drawSingle(it.first, it.second, moduleSize, thickness, primaryColor, secondaryColor) }
        }
    }
}
/**
 * Specifically returns true for the 4 extreme corners of the three 7x7 Finder squares.
 */
private fun isAtFinderCorner(x: Int, y: Int, size: Int): Boolean {
    val farStart = size - 7
    val farEnd = size - 1

    // Top-Left Finder: (0,0), (6,0), (0,6), (6,6)
    if ((x == 0 || x == 6) && (y == 0 || y == 6)) return true

    // Top-Right Finder: (farStart, 0), (farEnd, 0), (farStart, 6), (farEnd, 6)
    if ((x == farStart || x == farEnd) && (y == 0 || y == 6)) return true

    // Bottom-Left Finder: (0, farStart), (6, farStart), (0, farEnd), (6, farEnd)
    if ((x == 0 || x == 6) && (y == farStart || y == farEnd)) return true

    return false
}

private fun DrawScope.drawOffDotShape(
    center: Offset,
    radius: Float,
    type: Int,
    color: Color,
    x: Int,
    y: Int
) {
    when (type) {
        0 -> drawCircle(color, radius, center) // Circle
        1 -> { // Rounded Square
            val size = radius * 1.8f
            drawRoundRect(
                color = color,
                topLeft = Offset(center.x - size / 2f, center.y - size / 2f),
                size = Size(size, size),
                cornerRadius = CornerRadius(size * 0.3f)
            )
        }
        else -> { // Teardrop
            val path = Path().apply {
                radius * 2f
                val rect = Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius)
                val k = (x * 7 + y * 3).mod(4) // Randomize sharp corner direction
                addRoundRect(
                    RoundRect(
                        rect = rect,
                        topLeft = if (k == 0) CornerRadius.Zero else CornerRadius(radius),
                        topRight = if (k == 1) CornerRadius.Zero else CornerRadius(radius),
                        bottomRight = if (k == 2) CornerRadius.Zero else CornerRadius(radius),
                        bottomLeft = if (k == 3) CornerRadius.Zero else CornerRadius(radius)
                    )
                )
            }
            drawPath(path, color)
        }
    }
}


private data class Block(val x: Int, val y: Int)
private data class Run(val x: Int, val y: Int, val length: Int)

private class CellPlan(
    val matrixSize: Int,
    val structural: BooleanArray,      // cells "owned" by finder/alignment regions
    val blocks: List<Block>,           // top-left of every 2×2 block
    val horizontalRuns: List<Run>,
    val verticalRuns: List<Run>,
    val singles: List<Pair<Int, Int>>
) {
    fun isStructural(x: Int, y: Int) = structural[y * matrixSize + x]
}

private fun computeCellPlan(bitMatrix: BitMatrix, matrixSize: Int): CellPlan {
    val claimed = BooleanArray(matrixSize * matrixSize)
    val structural = BooleanArray(matrixSize * matrixSize)
    fun idx(x: Int, y: Int) = y * matrixSize + x

    fun inFinder(x: Int, y: Int) =
        (x in 0..6 && y in 0..6) ||
                (x in (matrixSize - 7) until matrixSize && y in 0..6) ||
                (x in 0..6 && y in (matrixSize - 7) until matrixSize)

    // Finders: structural and pre-claimed so run detection skips them.
    for (y in 0 until matrixSize) for (x in 0 until matrixSize) {
        if (inFinder(x, y)) {
            structural[idx(x, y)] = true
            claimed[idx(x, y)] = true
        }
    }

    // Alignment pattern (QR V2+ has one centered at (matrixSize - 7, matrixSize - 7)).
    // Removed special marking to "smoothly integrate" it as requested.

    val blocks = mutableListOf<Block>()

    // 3×3 solid: place a 2×2 at the top-left, leave the L for run detection.
    // This is the user's rule: "3×3 should be max 2×2 shape and then filled by lines."
    for (y in 0..matrixSize - 3) for (x in 0..matrixSize - 3) {
        if (claimed[idx(x, y)]) continue
        var solid = true
        outer@ for (dy in 0..2) for (dx in 0..2) {
            if (!bitMatrix.get(x + dx, y + dy) || claimed[idx(x + dx, y + dy)]) {
                solid = false; break@outer
            }
        }
        if (solid) {
            blocks += Block(x, y)
            for (dy in 0..1) for (dx in 0..1) claimed[idx(x + dx, y + dy)] = true
        }
    }

    // 2×2 solid blocks (greedy).
    for (y in 0..matrixSize - 2) for (x in 0..matrixSize - 2) {
        if (claimed[idx(x, y)]) continue
        var solid = true
        outer@ for (dy in 0..1) for (dx in 0..1) {
            if (!bitMatrix.get(x + dx, y + dy) || claimed[idx(x + dx, y + dy)]) {
                solid = false; break@outer
            }
        }
        if (solid) {
            blocks += Block(x, y)
            for (dy in 0..1) for (dx in 0..1) claimed[idx(x + dx, y + dy)] = true
        }
    }

    // Runs: each ON cell picks the LONGER of its horizontal/vertical extents
    // (ties → horizontal). This guarantees no L-shape: any one cell belongs to
    // exactly one straight run.
    val hRuns = mutableListOf<Run>()
    val vRuns = mutableListOf<Run>()
    val singles = mutableListOf<Pair<Int, Int>>()

    for (y in 0 until matrixSize) for (x in 0 until matrixSize) {
        if (claimed[idx(x, y)] || !bitMatrix.get(x, y)) continue

        var lenH = 1
        while (x + lenH < matrixSize &&
            bitMatrix.get(x + lenH, y) && !claimed[idx(x + lenH, y)]
        ) lenH++

        var lenV = 1
        while (y + lenV < matrixSize &&
            bitMatrix.get(x, y + lenV) && !claimed[idx(x, y + lenV)]
        ) lenV++

        when {
            lenH >= 2 && lenH >= lenV -> {
                hRuns += Run(x, y, lenH)
                for (i in 0 until lenH) claimed[idx(x + i, y)] = true
            }
            lenV >= 2 -> {
                vRuns += Run(x, y, lenV)
                for (i in 0 until lenV) claimed[idx(x, y + i)] = true
            }
            else -> {
                singles += x to y
                claimed[idx(x, y)] = true
            }
        }
    }

    return CellPlan(matrixSize, structural, blocks, hRuns, vRuns, singles)
}

private fun pickColor(x: Int, y: Int, p: Color, s: Color): Color = when {
    (x + y).mod(5) == 0 -> s
    else -> p
}

private fun DrawScope.drawFinderPattern(
    x: Float, y: Float, moduleSize: Float,
    primary: Color, secondary: Color, innerRotationDeg: Float
) {
    val finderSize = moduleSize * 7
    val outerStroke = moduleSize * 1.2f
    val center = Offset(x + finderSize / 2, y + finderSize / 2)

    drawCircle(
        color = primary,
        radius = (finderSize - outerStroke) / 2,
        center = center,
        style = Stroke(width = outerStroke)
    )

    // 6 spikes, way more rounded — was 9 bumps × 0.20 amplitude before.
    val innerPath = buildCookiePath(
        center = center,
        baseRadius = moduleSize * 1.35f,
        bumps = 6,
        amplitude = moduleSize * 0.10f,
        rotation = innerRotationDeg * (PI.toFloat() / 180f)
    )
    drawPath(innerPath, color = secondary)
}

private fun DrawScope.drawBlock2x2(bx: Int, by: Int, moduleSize: Float, color: Color) {
    val center = Offset(bx * moduleSize + moduleSize, by * moduleSize + moduleSize)
    when ((bx * 31 + by * 17).mod(3)) {
        0 -> {
            // Circle
            drawCircle(color = color, center = center, radius = moduleSize * 0.92f)
        }
        1 -> {
            // 4-petal mini cookie
            val path = buildCookiePath(
                center = center,
                baseRadius = moduleSize * 0.80f,
                bumps = 4,
                amplitude = moduleSize * 0.13f,
                rotation = (PI / 4f).toFloat()
            )
            drawPath(path, color = color)
        }
        else -> {
            // 8-petal mini cookie
            val path = buildCookiePath(
                center = center,
                baseRadius = moduleSize * 0.86f,
                bumps = 8,
                amplitude = moduleSize * 0.08f,
                rotation = 0f
            )
            drawPath(path, color = color)
        }
    }
}
private fun DrawScope.drawHorizontalRun(
    run: Run, moduleSize: Float, thickness: Float,
    primary: Color, secondary: Color
) {
    val (sx, sy, len) = run
    val color = pickColor(sx, sy, primary, secondary)
    val gap = moduleSize * 0.28f
    val r = thickness / 2f

    // Partition the run into segments with specific flat (grouped) or round (separate) gaps.
    val segments = partitionRun(len, sx + sy)
    var currentX = sx.toFloat()
    
    for (i in segments.indices) {
        val seg = segments[i]
        val isFirstOfRun = i == 0
        val isLastOfRun = i == segments.size - 1
        
        val left = currentX * moduleSize + (if (isFirstOfRun) 0f else gap / 2f)
        val right = (currentX + seg.len) * moduleSize - (if (isLastOfRun) 0f else gap / 2f)
        val top = sy * moduleSize + (moduleSize - thickness) / 2f
        
        if (right > left) {
            val path = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(left, top, right, top + thickness),
                        topLeft = if (seg.flatBefore) CornerRadius.Zero else CornerRadius(r),
                        bottomLeft = if (seg.flatBefore) CornerRadius.Zero else CornerRadius(r),
                        topRight = if (seg.flatAfter) CornerRadius.Zero else CornerRadius(r),
                        bottomRight = if (seg.flatAfter) CornerRadius.Zero else CornerRadius(r)
                    )
                )
            }
            drawPath(path, color = color)
        }
        currentX += seg.len
    }
}

private fun DrawScope.drawVerticalRun(
    run: Run, moduleSize: Float, thickness: Float,
    primary: Color, secondary: Color
) {
    val (sx, sy, len) = run
    val color = pickColor(sx, sy, primary, secondary)
    val gap = moduleSize * 0.28f
    val r = thickness / 2f

    val segments = partitionRun(len, sx + sy)
    var currentY = sy.toFloat()
    
    for (i in segments.indices) {
        val seg = segments[i]
        val isFirstOfRun = i == 0
        val isLastOfRun = i == segments.size - 1
        
        val top = currentY * moduleSize + (if (isFirstOfRun) 0f else gap / 2f)
        val bottom = (currentY + seg.len) * moduleSize - (if (isLastOfRun) 0f else gap / 2f)
        val left = sx * moduleSize + (moduleSize - thickness) / 2f
        
        if (bottom > top) {
            val path = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(left, top, left + thickness, bottom),
                        topLeft = if (seg.flatBefore) CornerRadius.Zero else CornerRadius(r),
                        topRight = if (seg.flatBefore) CornerRadius.Zero else CornerRadius(r),
                        bottomLeft = if (seg.flatAfter) CornerRadius.Zero else CornerRadius(r),
                        bottomRight = if (seg.flatAfter) CornerRadius.Zero else CornerRadius(r)
                    )
                )
            }
            drawPath(path, color = color)
        }
        currentY += seg.len
    }
}

private data class Segment(val len: Int, val flatBefore: Boolean, val flatAfter: Boolean)

private fun partitionRun(totalLen: Int, posKey: Int): List<Segment> {
    val lengths = mutableListOf<Int>()
    val flatAfter = mutableListOf<Boolean>()

    var rem = totalLen
    while (rem > 0) {
        when (rem) {
            1, 2 -> {
                lengths.add(rem)
                rem = 0
            }
            3 -> {
                // 1/2 or 2/1 (Grouped)
                if (posKey % 2 == 0) {
                    lengths.add(1); flatAfter.add(true); lengths.add(2)
                } else {
                    lengths.add(2); flatAfter.add(true); lengths.add(1)
                }
                rem = 0
            }
            4 -> {
                // 1/3 or 3/1 or 4
                if (posKey % 3 == 0) {
                    lengths.add(1); flatAfter.add(true); lengths.add(3)
                } else if (posKey % 3 == 1) {
                    lengths.add(3); flatAfter.add(true); lengths.add(1)
                } else {
                    lengths.add(4)
                }
                rem = 0
            }
            5 -> {
                // 1/4 or 4/1 (Grouped)
                if (posKey % 2 == 0) {
                    lengths.add(1); flatAfter.add(true); lengths.add(4)
                } else {
                    lengths.add(4); flatAfter.add(true); lengths.add(1)
                }
                rem = 0
            }
            6 -> {
                // 4 + 2 or 2 + 4 (Separate)
                if (posKey % 2 == 0) {
                    lengths.add(4); flatAfter.add(false); lengths.add(2)
                } else {
                    lengths.add(2); flatAfter.add(false); lengths.add(4)
                }
                rem = 0
            }
            7 -> {
                // 4 + 3 or 3 + 4 (Separate)
                if (posKey % 2 == 0) {
                    lengths.add(4); flatAfter.add(false); lengths.add(3)
                } else {
                    lengths.add(3); flatAfter.add(false); lengths.add(4)
                }
                rem = 0
            }
            else -> { // rem >= 8
                // 1/4 (Grouped) then separate break
                lengths.add(1); flatAfter.add(true); lengths.add(4); flatAfter.add(false)
                rem -= 5
            }
        }
    }

    val result = mutableListOf<Segment>()
    for (i in lengths.indices) {
        val hasFlatBefore = if (i == 0) false else flatAfter[i - 1]
        val hasFlatAfter = if (i == lengths.size - 1) false else flatAfter[i]
        result.add(Segment(lengths[i], hasFlatBefore, hasFlatAfter))
    }
    return result
}
private fun DrawScope.drawSingle(
    x: Int, y: Int, moduleSize: Float, thickness: Float,
    primary: Color, secondary: Color
) {
    val color = pickColor(x, y, primary, secondary)
    val center = Offset(x * moduleSize + moduleSize / 2f, y * moduleSize + moduleSize / 2f)
    val r = thickness / 2f

    when ((x * 7 + y * 13).mod(3)) {
        0 -> drawCircle(color = color, center = center, radius = r)
        1 -> drawRoundRect(
            color = color,
            topLeft = Offset(center.x - r, center.y - r),
            size = Size(thickness, thickness),
            cornerRadius = CornerRadius(thickness * 0.32f)
        )
        else -> {
            // 3 corners rounded like a circle, 1 corner sharp.
            val k = (x * 17 + y * 23).mod(4)
            val rect = Rect(center.x - r, center.y - r, center.x + r, center.y + r)
            val path = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = rect,
                        topLeft = if (k == 0) CornerRadius.Zero else CornerRadius(r),
                        topRight = if (k == 1) CornerRadius.Zero else CornerRadius(r),
                        bottomRight = if (k == 2) CornerRadius.Zero else CornerRadius(r),
                        bottomLeft = if (k == 3) CornerRadius.Zero else CornerRadius(r)
                    )
                )
            }
            drawPath(path, color = color)
        }
    }
}

@Composable
fun DefaultQrCode(
    text: String,
    modifier: Modifier = Modifier
) {
    val bitMatrix = remember(text) {
        try {
            QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0)
        } catch (_: Exception) {
            null
        }
    } ?: return

    val matrixSize = bitMatrix.width

    Canvas(modifier = modifier.aspectRatio(1f)) {
        val cornerRadius = 30.dp.toPx()

        // 1. Create a path for the rounded rectangle and clip the canvas
        // This ensures the black modules don't bleed past the rounded corners
        val path = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(Offset.Zero, size),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
            )
        }

        clipPath(path) {
            // 2. Draw the solid white background
            drawRect(color = Color.White, size = size)

            // 3. Draw the QR modules
            val moduleSize = size.width / matrixSize
            for (y in 0 until matrixSize) {
                for (x in 0 until matrixSize) {
                    if (bitMatrix.get(x, y)) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(x * moduleSize, y * moduleSize),
                            size = Size(moduleSize + 1f, moduleSize + 1f) // +1f prevents tiny gaps
                        )
                    }
                }
            }
        }
    }
}



/**
 * Builds a Material 3 Expressive "cookie" / lobed shape using polar coords:
 *   r(θ) = baseRadius + amplitude · cos(bumps · θ)
 *
 * @param center      shape center
 * @param baseRadius  average radius
 * @param bumps       number of outward lobes (4 = blob, 9 = finder cookie)
 * @param amplitude   how far lobes stick out (and indents go in)
 * @param rotation    rotation of the entire shape, in radians
 * @param steps       resolution; 240 is plenty smooth
 */
private fun buildCookiePath(
    center: Offset,
    baseRadius: Float,
    bumps: Int,
    amplitude: Float,
    rotation: Float = 0f,
    steps: Int = 240
): Path {
    val path = Path()
    val twoPi = (2.0 * PI).toFloat()
    for (i in 0..steps) {
        val t = i.toFloat() / steps
        val localAngle = t * twoPi
        // Lobes are fixed in the shape's local frame
        val r = baseRadius + amplitude * cos(bumps * localAngle)
        // Rotation is applied to the whole shape via the world angle
        val worldAngle = localAngle + rotation
        val px = center.x + r * cos(worldAngle)
        val py = center.y + r * sin(worldAngle)
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    return path
}