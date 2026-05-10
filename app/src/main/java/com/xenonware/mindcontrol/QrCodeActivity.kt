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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenonware.mindcontrol.ui.theme.PaletteTheme

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
            Spacer(modifier = Modifier.height(24.dp))

            ModernQrCode(
                text = text,
                modifier = Modifier
                    .size(260.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(28.dp))
                    .padding(16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
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
        } catch (e: Exception) {
            null
        }
    } ?: return

    val matrixSize = bitMatrix.width
    val infiniteTransition = rememberInfiniteTransition(label = "QrAnimation")
    
    val cornerRadiusAnim by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CornerRadiusAnim"
    )

    val dotScaleAnim by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DotScaleAnim"
    )

    val rotationAnim by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RotationAnim"
    )

    Canvas(modifier = modifier.aspectRatio(1f)) {
        val moduleSize = size.width / matrixSize
        
        // Finder patterns with extra rotation
        drawFinderPattern(0f, 0f, moduleSize, primaryColor, secondaryColor, cornerRadiusAnim, rotationAnim)
        drawFinderPattern((matrixSize - 7) * moduleSize, 0f, moduleSize, primaryColor, secondaryColor, cornerRadiusAnim, -rotationAnim)
        drawFinderPattern(0f, (matrixSize - 7) * moduleSize, moduleSize, primaryColor, secondaryColor, cornerRadiusAnim, rotationAnim)

        for (y in 0 until matrixSize) {
            for (x in 0 until matrixSize) {
                if (bitMatrix.get(x, y)) {
                    val isFinder = (x < 7 && y < 7) || 
                                   (x >= matrixSize - 7 && y < 7) || 
                                   (x < 7 && y >= matrixSize - 7)

                    if (!isFinder) {
                        // Check neighbors for pill connections
                        val nextIsFinderX = (x + 1 < 7 && y < 7) || (x + 1 >= matrixSize - 7 && y < 7) || (x + 1 < 7 && y >= matrixSize - 7)
                        val nextIsFinderY = (x < 7 && y + 1 < 7) || (x >= matrixSize - 7 && y + 1 < 7) || (x < 7 && y + 1 >= matrixSize - 7)

                        val hasRight = x < matrixSize - 1 && bitMatrix.get(x + 1, y) && !nextIsFinderX
                        val hasBottom = y < matrixSize - 1 && bitMatrix.get(x, y + 1) && !nextIsFinderY

                        val dotSize = moduleSize * dotScaleAnim
                        val offset = (moduleSize - dotSize) / 2

                        // Base dot
                        drawCircle(
                            color = primaryColor,
                            center = Offset(x * moduleSize + moduleSize / 2, y * moduleSize + moduleSize / 2),
                            radius = dotSize / 2
                        )

                        // Pill connection bridges
                        if (hasRight) {
                            drawRect(
                                color = primaryColor,
                                topLeft = Offset(x * moduleSize + moduleSize / 2, y * moduleSize + offset),
                                size = Size(moduleSize, dotSize)
                            )
                        }
                        if (hasBottom) {
                            drawRect(
                                color = primaryColor,
                                topLeft = Offset(x * moduleSize + offset, y * moduleSize + moduleSize / 2),
                                size = Size(dotSize, moduleSize)
                            )
                        }
                    }
                }
            }
        }

        // Draw animated finder patterns
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFinderPattern(
    x: Float,
    y: Float,
    moduleSize: Float,
    primaryColor: Color,
    secondaryColor: Color,
    animProgress: Float,
    rotation: Float
) {
    val finderSize = moduleSize * 7
    val outerStroke = moduleSize
    val innerSize = moduleSize * 3
    
    // Animate corner radius for finders
    val cornerRadius = finderSize * (0.2f + 0.3f * animProgress)

    rotate(rotation, Offset(x + finderSize / 2, y + finderSize / 2)) {
        // Outer frame
        val outerPath = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(Offset(x + outerStroke/2, y + outerStroke/2), Size(finderSize - outerStroke, finderSize - outerStroke)),
                    cornerRadius = CornerRadius(cornerRadius)
                )
            )
        }
        drawPath(
            path = outerPath,
            color = primaryColor,
            style = Stroke(width = outerStroke)
        )

        // Inner dot
        val innerCornerRadius = innerSize * (0.3f + 0.2f * (1f - animProgress))
        drawRoundRect(
            color = secondaryColor,
            topLeft = Offset(x + moduleSize * 2, y + moduleSize * 2),
            size = Size(innerSize, innerSize),
            cornerRadius = CornerRadius(innerCornerRadius)
        )
    }
}
