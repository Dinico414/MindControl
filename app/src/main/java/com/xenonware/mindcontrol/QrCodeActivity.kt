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
    secondaryColor: Color = MaterialTheme.colorScheme.secondary,
    tertiaryColor: Color = MaterialTheme.colorScheme.tertiary
) {
    val bitMatrix = remember(text) {
        try {
            val hints = mutableMapOf<EncodeHintType, Any>()
            hints[EncodeHintType.MARGIN] = 0
            QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, hints)
        } catch (_: Exception) {
            null
        }
    } ?: return

    val matrixSize = bitMatrix.width
    val infiniteTransition = rememberInfiniteTransition(label = "QrAnimation")

    // Gentle breathing scale of the cookie blob background
    val blobScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BlobScale"
    )

    val dotScaleAnim by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DotScaleAnim"
    )

    // Continuous rotation of the inner finder cookie shapes
    val finderRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "FinderRotation"
    )

    val blobColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)

    Canvas(modifier = modifier.aspectRatio(1f)) {
        val moduleSize = size.width / matrixSize
        val canvasCenter = Offset(size.width / 2f, size.height / 2f)

        // 4-lobed "cookie" background blob (the wavy Material 3 Expressive shape)
        withTransform({
            scale(blobScale, blobScale, canvasCenter)
        }) {
            // 4 deep lobes; baseR + amp + blobScale(1.03) must stay <= 0.50
            // (0.40 + 0.085) * 1.03 ≈ 0.499 — fits exactly without clipping.
            val blobPath = buildCookiePath(
                center = canvasCenter,
                baseRadius = size.width * 0.40f,
                bumps = 4,
                amplitude = size.width * 0.085f,
                rotation = 0f
            )
            drawPath(blobPath, color = blobColor)
        }

        // Three finder patterns — outer ring + rotating cookie inner shape
        drawFinderPattern(
            0f, 0f, moduleSize, primaryColor, secondaryColor, finderRotation
        )
        drawFinderPattern(
            (matrixSize - 7) * moduleSize, 0f, moduleSize, primaryColor, secondaryColor, finderRotation
        )
        drawFinderPattern(
            0f, (matrixSize - 7) * moduleSize, moduleSize, primaryColor, secondaryColor, finderRotation
        )

        // Data modules — dots with pill bridges
        for (y in 0 until matrixSize) {
            for (x in 0 until matrixSize) {
                if (bitMatrix.get(x, y)) {
                    val isFinder = (x < 7 && y < 7) ||
                            (x >= matrixSize - 7 && y < 7) ||
                            (x < 7 && y >= matrixSize - 7)

                    if (!isFinder) {
                        val nextIsFinderX = (x + 1 < 7 && y < 7) ||
                                (x + 1 >= matrixSize - 7 && y < 7) ||
                                (x + 1 < 7 && y >= matrixSize - 7)
                        val nextIsFinderY = (x < 7 && y + 1 < 7) ||
                                (x >= matrixSize - 7 && y + 1 < 7) ||
                                (x < 7 && y + 1 >= matrixSize - 7)

                        val hasRight = x < matrixSize - 1 &&
                                bitMatrix.get(x + 1, y) && !nextIsFinderX
                        val hasBottom = y < matrixSize - 1 &&
                                bitMatrix.get(x, y + 1) && !nextIsFinderY

                        val dotSize = moduleSize * dotScaleAnim
                        val offset = (moduleSize - dotSize) / 2

                        // Sprinkle in some color variety like the reference
                        val dotColor = when {
                            (x * 3 + y * 7) % 11 == 0 -> tertiaryColor
                            (x + y) % 5 == 0 -> secondaryColor
                            else -> primaryColor
                        }

                        drawCircle(
                            color = dotColor,
                            center = Offset(
                                x * moduleSize + moduleSize / 2,
                                y * moduleSize + moduleSize / 2
                            ),
                            radius = dotSize / 2
                        )

                        // Pill connection bridges
                        if (hasRight) {
                            drawRoundRect(
                                color = dotColor,
                                topLeft = Offset(
                                    x * moduleSize + moduleSize / 2,
                                    y * moduleSize + offset
                                ),
                                size = Size(moduleSize, dotSize),
                                cornerRadius = CornerRadius(dotSize / 2)
                            )
                        }
                        if (hasBottom) {
                            drawRoundRect(
                                color = dotColor,
                                topLeft = Offset(
                                    x * moduleSize + offset,
                                    y * moduleSize + moduleSize / 2
                                ),
                                size = Size(dotSize, moduleSize),
                                cornerRadius = CornerRadius(dotSize / 2)
                            )
                        }
                    }
                }
            }
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

private fun DrawScope.drawFinderPattern(
    x: Float,
    y: Float,
    moduleSize: Float,
    primaryColor: Color,
    secondaryColor: Color,
    innerRotationDeg: Float = 0f
) {
    val finderSize = moduleSize * 7
    val outerStroke = moduleSize * 1.2f
    val center = Offset(x + finderSize / 2, y + finderSize / 2)

    // Outer circle ring
    drawCircle(
        color = primaryColor,
        radius = (finderSize - outerStroke) / 2,
        center = center,
        style = Stroke(width = outerStroke)
    )

    // Inner 9-lobed cookie shape — matches the reference and rotates
    val innerPath = buildCookiePath(
        center = center,
        baseRadius = moduleSize * 1.15f,
        bumps = 9,
        amplitude = moduleSize * 0.20f,
        rotation = innerRotationDeg * (PI.toFloat() / 180f)
    )
    drawPath(innerPath, color = secondaryColor)
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