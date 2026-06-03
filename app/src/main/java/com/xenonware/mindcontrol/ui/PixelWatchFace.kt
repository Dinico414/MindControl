package com.xenonware.mindcontrol.ui

import android.text.format.DateFormat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun PixelWatchFace(isActive: Boolean) {
    val textMeasurer = rememberTextMeasurer()
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)

    val localDensity = LocalDensity.current

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.4f, animationSpec = tween(500), label = "alpha"
    )

    var isLongInactive by remember { mutableStateOf(false) }
    var isShortInactive by remember { mutableStateOf(false) }
    var isReappearing by remember { mutableStateOf(false) }

    LaunchedEffect(isActive) {
        if (isActive) {
            val wereMinutesHidden = isLongInactive
            isLongInactive = false
            if (wereMinutesHidden) {
                isReappearing = true
                delay(250L) // Delay seconds/pill expansion if minutes were hidden
                isReappearing = false
            }
            isShortInactive = false
        } else {
            isShortInactive = false
            isLongInactive = false
            isReappearing = false

            isShortInactive = true

            delay(300_000L)
            isLongInactive = true
        }
    }

    val minutesAlpha by animateFloatAsState(
        targetValue = if (isLongInactive) 0f else 1f,
        animationSpec = tween(1000),
        label = "minutesAlpha"
    )
    val secondsAlpha by animateFloatAsState(
        targetValue = if (isShortInactive || isReappearing) 0f else 1f,
        animationSpec = tween(500),
        label = "secondsAlpha"
    )
    val pillRightWeight by animateFloatAsState(
        targetValue = if (isShortInactive || isReappearing) 0f else 1f,
        animationSpec = tween(500),
        label = "pillRight"
    )

    var time by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val isFullyInactive = !isActive && pillRightWeight == 0f
    var burnInOffset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(isFullyInactive) {
        if (isFullyInactive) {
            while (true) {
                delay(20000L)
                val maxOffset = 80f
                burnInOffset = Offset(
                    x = (Random.nextFloat() * 2 - 1) * maxOffset,
                    y = (Random.nextFloat() * 2 - 1) * maxOffset
                )
            }
        } else {
            burnInOffset = Offset.Zero
        }
    }

    LaunchedEffect(isFullyInactive) {
        while (true) {
            time = System.currentTimeMillis()
            if (!isFullyInactive) {
                delay(16)
            } else {
                val calendar = Calendar.getInstance().apply { timeInMillis = time }
                val seconds = calendar.get(Calendar.SECOND)
                val millis = calendar.get(Calendar.MILLISECOND)
                val delayToNextMinute = 60000L - (seconds * 1000L + millis)
                delay(max(delayToNextMinute, 100L))
            }
        }
    }

    val calendar = Calendar.getInstance().apply { timeInMillis = time }
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val displayHour = if (is24Hour) {
        hour
    } else {
        val hour12 = hour % 12
        if (hour12 == 0) 12 else hour12
    }
    val minute = calendar.get(Calendar.MINUTE)
    val second = calendar.get(Calendar.SECOND)
    val millis = calendar.get(Calendar.MILLISECOND)

    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.75f)
            .aspectRatio(1f)
            .graphicsLayer(
                alpha = animatedAlpha,
                translationX = burnInOffset.x,
                translationY = burnInOffset.y
            )
    ) {
        val w = size.width
        val h = size.height

        val cx = w / 2f
        val cy = h / 2f
        val r = w / 2f

        val rInnerNum = r * 0.54f
        val rInnerTickIn = r * 0.62f
        val rInnerTickOut = r * 0.65f

        val rOuterNum = r * 0.82f
        val rOuterTickIn = r * 0.9f
        val rOuterTickOut = r * 0.95f

        val primaryColor = Color.White
        val secondaryColor = Color.Gray
        val pillOutlineColor = Color.LightGray

        fun pxToSp(px: Float): androidx.compose.ui.unit.TextUnit =
            (px / localDensity.density / localDensity.fontScale).sp

        val hourStyle = TextStyle(
            color = primaryColor,
            fontSize = pxToSp(r * 0.50f),
            fontWeight = FontWeight.Medium,
            fontFamily = QuicksandTitleVariable
        )
        val digMinStyle = TextStyle(
            color = primaryColor,
            fontSize = pxToSp(r * 0.20f),
            fontWeight = FontWeight.Medium,
            fontFamily = QuicksandTitleVariable
        )
        val dialNumStyle = TextStyle(
            color = secondaryColor,
            fontSize = pxToSp(r * 0.0875f),
            fontWeight = FontWeight.Normal,
            fontFamily = QuicksandTitleVariable
        )

        // Draw Hour
        val hourText = String.format(Locale.getDefault(), "%02d", displayHour)
        val hourLayout = textMeasurer.measure(hourText, hourStyle)
        drawText(
            textLayoutResult = hourLayout, topLeft = Offset(
                cx - hourLayout.size.width / 2f - r * 0.03f,
                cy - hourLayout.size.height / 2f - hourLayout.size.height * 0.02f
            )
        )

        val currentMinuteFloat = minute.toFloat()
        val currentSecondFloat = second + millis / 1000f

        fun drawDial(
            currentValue: Float,
            rNum: Float,
            rTickIn: Float,
            rTickOut: Float,
            alpha: Float = 1f,
            drawTicks: Boolean = true,
            drawNumbers: Boolean = true
        ) {
            if (alpha <= 0f) return

            for (i in 0 until 60) {
                val angleDeg = (currentValue - i) * 6f
                val rad = Math.toRadians(angleDeg.toDouble())
                val cosA = cos(rad).toFloat()
                val sinA = sin(rad).toFloat()

                // Ticks
                if (drawTicks) {
                    val isThick = i % 5 == 0
                    val strokeWidth = if (isThick) r * 0.012f else r * 0.005f
                    drawLine(
                        color = secondaryColor.copy(alpha = secondaryColor.alpha * alpha),
                        start = Offset(cx + rTickIn * cosA, cy + rTickIn * sinA),
                        end = Offset(cx + rTickOut * cosA, cy + rTickOut * sinA),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }

                // Numbers
                if (drawNumbers && i % 5 == 0) {
                    val displayNum = if (i == 0) 60 else i
                    val numText = String.format(Locale.getDefault(), "%02d", displayNum)
                    val layout = textMeasurer.measure(numText, dialNumStyle)
                    drawText(
                        textLayoutResult = layout, topLeft = Offset(
                            cx + rNum * cosA - layout.size.width / 2f,
                            cy + rNum * sinA - layout.size.height / 2f - layout.size.height * 0.02f
                        ), alpha = alpha
                    )
                }
            }
        }

        // --- DRAWING ORDER ---

        // 1. Calculate Pill Path
        val pillHeight = r * 0.36f
        val pillTop = cy - pillHeight / 2f
        val pillBottom = cy + pillHeight / 2f
        val inactivePillRight = cx + rInnerTickOut
        val pillLeft = inactivePillRight - pillHeight
        val pillRadius = pillHeight / 2f

        val activePillRight = cx + rOuterTickOut
        val currentPillRight =
            inactivePillRight + (activePillRight - inactivePillRight) * pillRightWeight

        val pillRect =
            Rect(left = pillLeft, top = pillTop, right = currentPillRight, bottom = pillBottom)
        val pillPath = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = pillRect, cornerRadius = CornerRadius(pillRadius, pillRadius)
                )
            )
        }

        // 2. Minute Numbers (clipped out where the pill is)
        clipPath(path = pillPath, clipOp = ClipOp.Difference) {
            drawDial(
                currentMinuteFloat,
                rInnerNum,
                rInnerTickIn,
                rInnerTickOut,
                alpha = minutesAlpha,
                drawTicks = false,
                drawNumbers = true
            )
        }

        // 3. Minute Ticks
        drawDial(
            currentMinuteFloat,
            rInnerNum,
            rInnerTickIn,
            rInnerTickOut,
            alpha = minutesAlpha,
            drawTicks = true,
            drawNumbers = false
        )

        // 4. Second Ring
        if (secondsAlpha > 0f) {
            drawDial(
                currentSecondFloat, rOuterNum, rOuterTickIn, rOuterTickOut, alpha = secondsAlpha
            )
        }

        // 5. Pill Outline (on top of everything)
        drawPath(pillPath, color = pillOutlineColor, style = Stroke(width = r * 0.008f))

        // 6. Digital Minute Inside Pill
        val minText = String.format(Locale.getDefault(), "%02d", minute)
        val minLayout = textMeasurer.measure(minText, digMinStyle)
        val circleCenterX = pillLeft + pillRadius
        drawText(
            textLayoutResult = minLayout, topLeft = Offset(
                circleCenterX - minLayout.size.width / 2f,
                cy - minLayout.size.height / 2f - minLayout.size.height * 0.02f
            )
        )
    }
}

// Static preview — no animation state, no tap logic, explicit dark background.
@Preview(
    widthDp = 300,
    heightDp = 300,
    showBackground = true,
    backgroundColor = 0xFF000000,
    apiLevel = 36
)
@Composable
fun PixelWatchFacePreview() {
    PixelWatchFace(isActive = true)
}

@Preview(
    widthDp = 300,
    heightDp = 300,
    showBackground = true,
    backgroundColor = 0xFF000000,
    apiLevel = 36
)
@Composable
fun PixelWatchFaceInactivePreview() {
    PixelWatchFace(isActive = false)
}

@Preview(
    widthDp = 300,
    heightDp = 300,
    apiLevel = 36
)
@Composable
fun PixelWatchFaceInteractivePreview() {
    var isActive by remember { mutableStateOf(true) }
    Box(modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectTapGestures { isActive = !isActive }
        }
    ) {
        PixelWatchFace(isActive = isActive)
    }
}
