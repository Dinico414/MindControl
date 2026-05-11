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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { finish() },
                    contentAlignment = Alignment.Center
                ) {
                    QrCodeContent(
                        text = text,
                        onDismiss = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun QrCodeContent(text: String, onDismiss: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 2 })

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Quick Share",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = QuicksandTitleVariable
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Scan this QR code with another device to join",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontFamily = QuicksandTitleVariable,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(280.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                        RoundedCornerShape(32.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
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

            Spacer(modifier = Modifier.height(16.dp))

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
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontFamily = QuicksandTitleVariable
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Dismiss")
            }
        }
    }
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

    val flowerScale by infiniteTransition.animateFloat(
        initialValue = 0.985f, targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FlowerScale"
    )

    // Only the FINDER inner cookies rotate — nothing else does.
    val finderRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "FinderRotation"
    )

    val flowerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    val offDotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)

    Canvas(modifier = modifier.aspectRatio(1f)) {
        val canvasCenter = Offset(size.width / 2f, size.height / 2f)

        // 4-petal flower with petals pointing into the CORNERS.
        // Rotation of π/4 shifts the cos(4θ) lobes by 45°.
        withTransform({ scale(flowerScale, flowerScale, canvasCenter) }) {
            // Was: baseRadius 0.48, amplitude 0.12 → tall petals, lots of empty canvas
            val flowerPath = buildCookiePath(
                center = canvasCenter,
                baseRadius = size.width * 0.55f,    // bigger flower
                bumps = 4,
                amplitude = size.width * 0.09f,     // shorter petals (was 0.12)
                rotation = (PI / 4f).toFloat()
            )
            drawPath(flowerPath, color = flowerColor)
        }

        // QR data inset so it sits inside the flower's indents.
        // 14% inset on each side → QR fills 72% of the canvas;
        // flower indent (0.36) just clears the QR side midpoint (0.36).
        // Was: qrInset = 0.14 → QR filled 72%
        val qrInset = size.width * 0.07f        // QR now fills 86%
        val qrSize = size.width - 2f * qrInset
        val moduleSize = qrSize / matrixSize
        val thickness = moduleSize * 0.78f

        withTransform({ translate(qrInset, qrInset) }) {

            // 1. Faded OFF dots — only inside the QR data area, only as small
            //    circles at ~60% of the cell. Skipped in structural regions.
            val offRadius = moduleSize * 0.30f
            for (y in 0 until matrixSize) for (x in 0 until matrixSize) {
                if (plan.isStructural(x, y)) continue
                if (bitMatrix.get(x, y)) continue
                drawCircle(
                    color = offDotColor,
                    center = Offset(
                        x * moduleSize + moduleSize / 2f,
                        y * moduleSize + moduleSize / 2f
                    ),
                    radius = offRadius
                )
            }

            // 2. Finder patterns (the only rotating element).
            drawFinderPattern(0f, 0f, moduleSize, primaryColor, secondaryColor, finderRotation)
            drawFinderPattern((matrixSize - 7) * moduleSize, 0f, moduleSize, primaryColor, secondaryColor, finderRotation)
            drawFinderPattern(0f, (matrixSize - 7) * moduleSize, moduleSize, primaryColor, secondaryColor, finderRotation)

            // 3. 2×2 blocks — static 4-petal mini cookies, petals to corners.
            plan.blocks.forEach { (bx, by) ->
                drawBlock2x2(bx, by, moduleSize, pickColor(bx, by, primaryColor, secondaryColor))
            }

            // 5. Horizontal runs (length 2 pill, 3-4 flat-join split, 5+ gap split).
            plan.horizontalRuns.forEach { run ->
                drawHorizontalRun(run, moduleSize, thickness, primaryColor, secondaryColor)
            }

            // 6. Vertical runs (symmetric).
            plan.verticalRuns.forEach { run ->
                drawVerticalRun(run, moduleSize, thickness, primaryColor, secondaryColor)
            }

            // 7. Singles — rounded square / circle / teardrop, deterministic per position.
            plan.singles.forEach { (x, y) ->
                drawSingle(x, y, moduleSize, thickness, primaryColor, secondaryColor)
            }
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
        when {
            rem == 1 || rem == 2 -> {
                lengths.add(rem)
                rem = 0
            }
            rem == 3 -> {
                // 1/2 or 2/1 (Grouped)
                if (posKey % 2 == 0) {
                    lengths.add(1); flatAfter.add(true); lengths.add(2)
                } else {
                    lengths.add(2); flatAfter.add(true); lengths.add(1)
                }
                rem = 0
            }
            rem == 4 -> {
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
            rem == 5 -> {
                // 1/4 or 4/1 (Grouped)
                if (posKey % 2 == 0) {
                    lengths.add(1); flatAfter.add(true); lengths.add(4)
                } else {
                    lengths.add(4); flatAfter.add(true); lengths.add(1)
                }
                rem = 0
            }
            rem == 6 -> {
                // 4 + 2 or 2 + 4 (Separate)
                if (posKey % 2 == 0) {
                    lengths.add(4); flatAfter.add(false); lengths.add(2)
                } else {
                    lengths.add(2); flatAfter.add(false); lengths.add(4)
                }
                rem = 0
            }
            rem == 7 -> {
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
            // Keep ZXing's default quiet-zone margin so this is a properly
            // scannable, "boring" black-on-white QR code.
            QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0)
        } catch (_: Exception) {
            null
        }
    } ?: return

    val matrixSize = bitMatrix.width

    Canvas(modifier = modifier.aspectRatio(1f)) {
        // Solid white background — makes this a real normal QR code,
        // not a tinted-on-card one.
        drawRect(color = Color.White, size = size)

        val moduleSize = size.width / matrixSize
        for (y in 0 until matrixSize) {
            for (x in 0 until matrixSize) {
                if (bitMatrix.get(x, y)) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(x * moduleSize, y * moduleSize),
                        size = Size(moduleSize, moduleSize)
                    )
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