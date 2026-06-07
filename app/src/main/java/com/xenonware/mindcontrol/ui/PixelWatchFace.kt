package com.xenonware.mindcontrol.ui

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

@Composable
fun PixelWatchFace(isActive: Boolean) {
    val textMeasurer = rememberTextMeasurer()
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)

    val localDensity = LocalDensity.current

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.7f, animationSpec = tween(500), label = "alpha"
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
        targetValue = 1f,
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
                alpha = animatedAlpha
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

@Composable
fun StackedWatchFace(isActive: Boolean) {
    val textMeasurer = rememberTextMeasurer()
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)
    val locale = LocalConfiguration.current.locales[0]
    val localDensity = LocalDensity.current

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.7f,
        animationSpec = tween(500),
        label = "alpha"
    )

    var time by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val isFullyInactive = !isActive

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

    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.75f)
            .aspectRatio(1f)
            .graphicsLayer(
                alpha = animatedAlpha
            )
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = w / 2f

        fun pxToSp(px: Float): androidx.compose.ui.unit.TextUnit =
            (px / localDensity.density / localDensity.fontScale).sp

        val textStyle = TextStyle(
            color = Color.White,
            fontSize = pxToSp(r * 0.705f),
            fontWeight = FontWeight.Bold,
            fontFamily = QuicksandTitleVariable
        )

        val hourText = String.format(locale, "%02d", displayHour)
        val minuteText = String.format(locale, "%02d", minute)

        val hourLayout = textMeasurer.measure(hourText, textStyle)
        val minuteLayout = textMeasurer.measure(minuteText, textStyle)

        val hourY = cy - hourLayout.size.height + hourLayout.size.height * 0.20f
        val minuteY = cy - minuteLayout.size.height * 0.20f

        drawText(
            textLayoutResult = hourLayout,
            topLeft = Offset(cx - hourLayout.size.width / 2f, hourY)
        )
        drawText(
            textLayoutResult = minuteLayout,
            topLeft = Offset(cx - minuteLayout.size.width / 2f, minuteY),
            alpha = 1f
        )
    }
}

@Composable
fun InlineWatchFace(isActive: Boolean) {
    val textMeasurer = rememberTextMeasurer()
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)
    val locale = LocalConfiguration.current.locales[0]
    val localDensity = LocalDensity.current

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.7f,
        animationSpec = tween(500),
        label = "alpha"
    )

    var time by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val isFullyInactive = !isActive

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

    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.75f)
            .aspectRatio(1f)
            .graphicsLayer(
                alpha = animatedAlpha
            )
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = w / 2f

        fun pxToSp(px: Float): androidx.compose.ui.unit.TextUnit =
            (px / localDensity.density / localDensity.fontScale).sp

        val textStyle = TextStyle(
            color = Color.White,
            fontSize = pxToSp(r * 0.47f),
            fontWeight = FontWeight.Bold,
            fontFamily = QuicksandTitleVariable
        )

        val hourText = String.format(locale, "%02d", displayHour)
        val colonText = ":"
        val minuteText = String.format(locale, "%02d", minute)

        val hourLayout = textMeasurer.measure(hourText, textStyle)
        val colonLayout = textMeasurer.measure(colonText, textStyle)
        val minuteLayout = textMeasurer.measure(minuteText, textStyle)

        val totalWidth = hourLayout.size.width + colonLayout.size.width + minuteLayout.size.width
        val hourLeft = cx - totalWidth / 2f
        val colonLeft = hourLeft + hourLayout.size.width
        val minuteLeft = colonLeft + colonLayout.size.width

        drawText(
            textLayoutResult = hourLayout,
            topLeft = Offset(hourLeft, cy - hourLayout.size.height / 2f)
        )
        drawText(
            textLayoutResult = colonLayout,
            topLeft = Offset(colonLeft, cy - colonLayout.size.height / 2f),
            alpha = 1f
        )
        drawText(
            textLayoutResult = minuteLayout,
            topLeft = Offset(minuteLeft, cy - minuteLayout.size.height / 2f),
            alpha = 1f
        )
    }
}

@Composable
fun AnalogWatchFace(isActive: Boolean) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.7f,
        animationSpec = tween(500),
        label = "alpha"
    )

    var time by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(isActive) {
        while (true) {
            time = System.currentTimeMillis()
            if (isActive) delay(16) else delay(1000)
        }
    }

    val calendar = Calendar.getInstance().apply { timeInMillis = time }
    val hour = calendar.get(Calendar.HOUR)
    val minute = calendar.get(Calendar.MINUTE)
    val second = calendar.get(Calendar.SECOND)
    val millis = calendar.get(Calendar.MILLISECOND)

    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.7f)
            .aspectRatio(1f)
            .graphicsLayer(alpha = animatedAlpha)
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = w / 2f

        // Draw Ticks
        for (i in 0 until 60) {
            val angleDeg = i * 6f - 90f
            val rad = Math.toRadians(angleDeg.toDouble())
            val isMain = i % 5 == 0
            val tickLen = if (isMain) r * 0.15f else r * 0.05f
            val strokeWidth = if (isMain) r * 0.02f else r * 0.01f
            val alpha = if (isMain) 0.8f else 0.4f
            
            drawLine(
                color = Color.White.copy(alpha = alpha),
                start = Offset(cx + (r - tickLen) * cos(rad).toFloat(), cy + (r - tickLen) * sin(rad).toFloat()),
                end = Offset(cx + r * cos(rad).toFloat(), cy + r * sin(rad).toFloat()),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }

        // Hour Hand (ticks every minute)
        val hourAngle = (hour + minute / 60f) * 30f - 90f
        val hourRad = Math.toRadians(hourAngle.toDouble())
        drawLine(
            color = Color.White,
            start = Offset(cx, cy),
            end = Offset(cx + r * 0.5f * cos(hourRad).toFloat(), cy + r * 0.5f * sin(hourRad).toFloat()),
            strokeWidth = r * 0.04f,
            cap = StrokeCap.Round
        )

        // Minute Hand (ticks every minute)
        val minuteAngle = minute * 6f - 90f
        val minuteRad = Math.toRadians(minuteAngle.toDouble())
        drawLine(
            color = Color.White,
            start = Offset(cx, cy),
            end = Offset(cx + r * 0.75f * cos(minuteRad).toFloat(), cy + r * 0.75f * sin(minuteRad).toFloat()),
            strokeWidth = r * 0.025f,
            cap = StrokeCap.Round
        )

        // Second Hand (only if active)
        if (isActive) {
            val secondAngle = (second + millis / 1000f) * 6f - 90f
            val secondRad = Math.toRadians(secondAngle.toDouble())
            drawLine(
                color = Color.Red,
                start = Offset(cx, cy),
                end = Offset(cx + r * 0.85f * cos(secondRad).toFloat(), cy + r * 0.85f * sin(secondRad).toFloat()),
                strokeWidth = r * 0.01f,
                cap = StrokeCap.Round
            )
        }

        // Center dot
        drawCircle(color = Color.White, radius = r * 0.03f, center = Offset(cx, cy))
    }
}

@Composable
fun StackedDotWatchFace(isActive: Boolean) {
    GenericStackedGridWatchFace(isActive = isActive, isNothingStyle = true)
}

@Composable
fun StackedDigitalWatchFace(isActive: Boolean) {
    GenericStackedGridWatchFace(isActive = isActive, isNothingStyle = false)
}

@Composable
fun InlineDotWatchFace(isActive: Boolean) {
    GenericInlineGridWatchFace(isActive = isActive, isNothingStyle = true)
}

@Composable
fun InlineDigitalWatchFace(isActive: Boolean) {
    GenericInlineGridWatchFace(isActive = isActive, isNothingStyle = false)
}

private val NOTHING_DOT_PATTERNS = arrayOf(
    intArrayOf(0x6, 0x9, 0x9, 0x9, 0x9, 0x9, 0x6), // 0
    intArrayOf(0x4, 0x4, 0x4, 0x4, 0x4, 0x4, 0x4), // 1
    intArrayOf(0xE, 0x1, 0x1, 0x6, 0x8, 0x8, 0xF), // 2
    intArrayOf(0xE, 0x1, 0x1, 0x6, 0x1, 0x1, 0xE), // 3
    intArrayOf(0x9, 0x9, 0x9, 0xF, 0x1, 0x1, 0x1), // 4
    intArrayOf(0xF, 0x8, 0x8, 0xE, 0x1, 0x1, 0xE), // 5
    intArrayOf(0x6, 0x8, 0x8, 0xE, 0x9, 0x9, 0x6), // 6
    intArrayOf(0xF, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1), // 7
    intArrayOf(0x6, 0x9, 0x9, 0x6, 0x9, 0x9, 0x6), // 8
    intArrayOf(0x6, 0x9, 0x9, 0x7, 0x1, 0x1, 0x6)  // 9
)

private val DIGITAL_SEGMENTS = intArrayOf(
    0x3F, 0x06, 0x5B, 0x4F, 0x66, 0x6D, 0x7D, 0x07, 0x7F, 0x6F
)

private fun DrawScope.drawNothingDigit(digit: Int, offset: Offset, width: Float, height: Float, color: Color, alpha: Float) {
    val pattern = NOTHING_DOT_PATTERNS[digit.coerceIn(0, 9)]
    
    // Use a uniform cell size to ensure horizontal and vertical spacing are identical.
    val cellSize = kotlin.math.min(width / 4f, height / 7f)
    
    // Center the 4x7 grid within the allocated width/height.
    val startX = offset.x + (width - cellSize * 4f) / 2f
    val startY = offset.y + (height - cellSize * 7f) / 2f
    
    // Dot radius relative to the cell size.
    val dotRadius = cellSize * 0.38f
    
    for (row in 0 until 7) {
        val rowBits = pattern[row]
        for (col in 0 until 4) {
            val bit = (rowBits shr (3 - col)) and 1
            if (bit == 1) {
                val cx = startX + col * cellSize + cellSize / 2f
                val cy = startY + row * cellSize + cellSize / 2f
                drawCircle(color, dotRadius, Offset(cx, cy), alpha)
            }
        }
    }
}

private fun DrawScope.drawDigitalDigit(digit: Int, offset: Offset, width: Float, height: Float, color: Color, alpha: Float) {
    val segments = DIGITAL_SEGMENTS[digit.coerceIn(0, 9)]
    val t = width * 0.18f // thickness
    val r = t / 2f       // point depth
    val g = t / 4f       // Gap that ensures 45-degree alignment (r = 2g is the meeting distance)
    
    fun drawSeg(pts: List<Offset>) {
        val p = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
            close()
        }
        drawPath(p, color, alpha)
    }

    val x = offset.x
    val y = offset.y
    val w = width
    val h = height
    val mh = y + h / 2f

    // Standard meeting points for a 7-segment display
    val p1 = Offset(x + r, y + r)      // Top-Left
    val p2 = Offset(x + w - r, y + r)  // Top-Right
    val p3 = Offset(x + r, mh)         // Mid-Left
    val p4 = Offset(x + w - r, mh)     // Mid-Right
    val p5 = Offset(x + r, y + h - r)  // Bottom-Left
    val p6 = Offset(x + w - r, y + h - r) // Bottom-Right

    // Offset based on gap to make horizontal segments shorter for alignment
    val hg = g * 1.5f 

    // --- Horizontal Segments (A, G, D) ---

    // Segment A (Top)
    if ((segments and 0x01) != 0) {
        val ly = y + r
        drawSeg(listOf(
            Offset(p1.x + hg, ly),     // Left Point
            Offset(p1.x + hg + r, y),  // Top-Left
            Offset(p2.x - hg - r, y),  // Top-Right
            Offset(p2.x - hg, ly),     // Right Point
            Offset(p2.x - hg - r, y + t), // Bottom-Right
            Offset(p1.x + hg + r, y + t)  // Bottom-Left
        ))
    }

    // Segment G (Middle)
    if ((segments and 0x40) != 0) {
        drawSeg(listOf(
            Offset(p3.x + hg, mh),     // Left Point
            Offset(p3.x + hg + r, mh - r), // Top-Left
            Offset(p4.x - hg - r, mh - r), // Top-Right
            Offset(p4.x - hg, mh),     // Right Point
            Offset(p4.x - hg - r, mh + r), // Bottom-Right
            Offset(p3.x + hg + r, mh + r)  // Bottom-Left
        ))
    }

    // Segment D (Bottom)
    if ((segments and 0x08) != 0) {
        val ly = y + h - r
        drawSeg(listOf(
            Offset(p5.x + hg, ly),     // Left Point
            Offset(p5.x + hg + r, y + h - t), // Top-Left
            Offset(p6.x - hg - r, y + h - t), // Top-Right
            Offset(p6.x - hg, ly),     // Right Point
            Offset(p6.x - hg - r, y + h), // Bottom-Right
            Offset(p5.x + hg + r, y + h)  // Bottom-Left
        ))
    }

    // --- Vertical Segments (F, B, E, C) ---

    // Segment F (Top Left)
    if ((segments and 0x20) != 0) {
        val lx = x + r
        drawSeg(listOf(
            Offset(lx, p1.y + g),           // Top Point
            Offset(x + t, p1.y + g + r),    // Top-Right
            Offset(x + t, p3.y - g - r),    // Bottom-Right
            Offset(lx, p3.y - g),           // Bottom Point
            Offset(x, p3.y - g - r),        // Bottom-Left
            Offset(x, p1.y + g + r)         // Top-Left
        ))
    }

    // Segment B (Top Right)
    if ((segments and 0x02) != 0) {
        val lx = x + w - r
        drawSeg(listOf(
            Offset(lx, p2.y + g),           // Top Point
            Offset(x + w, p2.y + g + r),    // Top-Right
            Offset(x + w, p4.y - g - r),    // Bottom-Right
            Offset(lx, p4.y - g),           // Bottom Point
            Offset(x + w - t, p4.y - g - r), // Bottom-Left
            Offset(x + w - t, p2.y + g + r)  // Top-Left
        ))
    }

    // Segment E (Bottom Left)
    if ((segments and 0x10) != 0) {
        val lx = x + r
        drawSeg(listOf(
            Offset(lx, p3.y + g),           // Top Point
            Offset(x + t, p3.y + g + r),    // Top-Right
            Offset(x + t, p5.y - g - r),    // Bottom-Right
            Offset(lx, p5.y - g),           // Bottom Point
            Offset(x, p5.y - g - r),        // Bottom-Left
            Offset(x, p3.y + g + r)         // Top-Left
        ))
    }

    // Segment C (Bottom Right)
    if ((segments and 0x04) != 0) {
        val lx = x + w - r
        drawSeg(listOf(
            Offset(lx, p4.y + g),           // Top Point
            Offset(x + w, p4.y + g + r),    // Top-Right
            Offset(x + w, p6.y - g - r),    // Bottom-Right
            Offset(lx, p6.y - g),           // Bottom Point
            Offset(x + w - t, p6.y - g - r), // Bottom-Left
            Offset(x + w - t, p4.y + g + r)  // Top-Left
        ))
    }
}

@Composable
private fun GenericStackedGridWatchFace(isActive: Boolean, isNothingStyle: Boolean) {
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.7f,
        animationSpec = tween(500),
        label = "alpha"
    )

    var time by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(isActive) {
        while (true) {
            time = System.currentTimeMillis()
            if (isActive) delay(16) else delay(1000)
        }
    }

    val calendar = Calendar.getInstance().apply { timeInMillis = time }
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val displayHour = if (is24Hour) hour else (hour % 12).let { if (it == 0) 12 else it }
    val minute = calendar.get(Calendar.MINUTE)

    Canvas(
        modifier = Modifier.fillMaxWidth(0.4f).aspectRatio(1f).graphicsLayer(alpha = animatedAlpha)
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val digitWidth = w * 0.32f
        val digitHeight = h * 0.42f
        val spacing = w * 0.05f

        val h1 = displayHour / 10
        val h2 = displayHour % 10
        val m1 = minute / 10
        val m2 = minute % 10

        val topY = cy - digitHeight - spacing / 2f
        val bottomY = cy + spacing / 2f
        val leftX = cx - digitWidth - spacing / 2f
        val rightX = cx + spacing / 2f

        if (isNothingStyle) {
            drawNothingDigit(h1, Offset(leftX, topY), digitWidth, digitHeight, Color.White, 1f)
            drawNothingDigit(h2, Offset(rightX, topY), digitWidth, digitHeight, Color.White, 1f)
            drawNothingDigit(m1, Offset(leftX, bottomY), digitWidth, digitHeight, Color.White, 1f)
            drawNothingDigit(m2, Offset(rightX, bottomY), digitWidth, digitHeight, Color.White, 1f)
        } else {
            drawDigitalDigit(h1, Offset(leftX, topY), digitWidth, digitHeight, Color.White, 1f)
            drawDigitalDigit(h2, Offset(rightX, topY), digitWidth, digitHeight, Color.White, 1f)
            drawDigitalDigit(m1, Offset(leftX, bottomY), digitWidth, digitHeight, Color.White, 1f)
            drawDigitalDigit(m2, Offset(rightX, bottomY), digitWidth, digitHeight, Color.White, 1f)
        }
    }
}

@Composable
private fun GenericInlineGridWatchFace(isActive: Boolean, isNothingStyle: Boolean) {
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.7f,
        animationSpec = tween(500),
        label = "alpha"
    )

    var time by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(isActive) {
        while (true) {
            time = System.currentTimeMillis()
            if (isActive) delay(16) else delay(1000)
        }
    }

    val calendar = Calendar.getInstance().apply { timeInMillis = time }
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val displayHour = if (is24Hour) hour else (hour % 12).let { if (it == 0) 12 else it }
    val minute = calendar.get(Calendar.MINUTE)

    Canvas(
        modifier = Modifier.fillMaxWidth(0.5f).aspectRatio(2.5f).graphicsLayer(alpha = animatedAlpha)
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val digitWidth = w * 0.17f
        val digitHeight = h * 0.85f
        val spacing = w * 0.02f
        val colonWidth = w * 0.05f

        val h1 = displayHour / 10
        val h2 = displayHour % 10
        val m1 = minute / 10
        val m2 = minute % 10

        val startX = cx - (digitWidth * 2 + colonWidth + digitWidth * 2 + spacing * 4) / 2f
        val y = cy - digitHeight / 2f

        var currentX = startX
        if (isNothingStyle) {
            drawNothingDigit(h1, Offset(currentX, y), digitWidth, digitHeight, Color.White, 1f)
            currentX += digitWidth + spacing
            drawNothingDigit(h2, Offset(currentX, y), digitWidth, digitHeight, Color.White, 1f)
            currentX += digitWidth + spacing
            
            // Colon
            val cellSize = digitWidth / 4f
            val dotRadius = cellSize * 0.38f
            drawCircle(Color.White, dotRadius, Offset(currentX + colonWidth / 2f, cy - digitHeight * 0.2f), 1f)
            drawCircle(Color.White, dotRadius, Offset(currentX + colonWidth / 2f, cy + digitHeight * 0.2f), 1f)
            
            currentX += colonWidth + spacing
            drawNothingDigit(m1, Offset(currentX, y), digitWidth, digitHeight, Color.White, 1f)
            currentX += digitWidth + spacing
            drawNothingDigit(m2, Offset(currentX, y), digitWidth, digitHeight, Color.White, 1f)
        } else {
            drawDigitalDigit(h1, Offset(currentX, y), digitWidth, digitHeight, Color.White, 1f)
            currentX += digitWidth + spacing
            drawDigitalDigit(h2, Offset(currentX, y), digitWidth, digitHeight, Color.White, 1f)
            currentX += digitWidth + spacing
            
            // Colon
            val t = digitWidth * 0.16f
            val sy = digitHeight * 0.2f
            drawRect(Color.White, Offset(currentX + colonWidth / 2f - t / 2f, cy - sy - t / 2f), Size(t, t), alpha = 1f)
            drawRect(Color.White, Offset(currentX + colonWidth / 2f - t / 2f, cy + sy - t / 2f), Size(t, t), alpha = 1f)

            currentX += colonWidth + spacing
            drawDigitalDigit(m1, Offset(currentX, y), digitWidth, digitHeight, Color.White, 1f)
            currentX += digitWidth + spacing
            drawDigitalDigit(m2, Offset(currentX, y), digitWidth, digitHeight, Color.White, 1f)
        }
    }
}

@Composable
fun PlanetsWatchFace(isActive: Boolean) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.7f,
        animationSpec = tween(500),
        label = "alpha"
    )

    var time by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(isActive) {
        while (true) {
            time = System.currentTimeMillis()
            if (isActive) delay(16) else delay(1000)
        }
    }

    val calendar = Calendar.getInstance().apply { timeInMillis = time }
    val hour = calendar.get(Calendar.HOUR)
    val minute = calendar.get(Calendar.MINUTE)
    val second = calendar.get(Calendar.SECOND)
    val millis = calendar.get(Calendar.MILLISECOND)

    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.75f)
            .aspectRatio(1f)
            .graphicsLayer(alpha = animatedAlpha)
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = w / 2f

        // Let's define the radius for our rings:
        // Hour (center) ring: rHour = r * 0.35f
        // Minute (middle) ring: rMinute = r * 0.65f
        // Second (outside) ring: rSecond = r * 0.92f
        val rHour = r * 0.35f
        val rMinute = r * 0.65f
        val rSecond = r * 0.92f

        // Draw the rings themselves. The rings are drawn with low opacity so they are visible but subtle.
        drawCircle(
            color = Color.White.copy(alpha = 0.15f),
            radius = rHour,
            center = Offset(cx, cy),
            style = Stroke(width = r * 0.015f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.15f),
            radius = rMinute,
            center = Offset(cx, cy),
            style = Stroke(width = r * 0.015f)
        )
        // Only draw the second ring if AOD is active (optional, but consistent with second hand behavior)
        if (isActive) {
            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = rSecond,
                center = Offset(cx, cy),
                style = Stroke(width = r * 0.015f)
            )
        }

        // Now calculate angles for the "planets" running on the rings
        // In analog clock:
        // Hour angle (30 degrees per hour, plus 0.5 degrees per minute)
        val hourAngle = (hour + minute / 60f + second / 3600f) * 30f - 90f
        val hourRad = Math.toRadians(hourAngle.toDouble())

        // Minute angle (6 degrees per minute, plus 0.1 degrees per second)
        val minuteAngle = (minute + second / 60f) * 6f - 90f
        val minuteRad = Math.toRadians(minuteAngle.toDouble())

        // Draw Hour Planet (Center)
        val hPlanetX = cx + rHour * cos(hourRad).toFloat()
        val hPlanetY = cy + rHour * sin(hourRad).toFloat()
        drawCircle(
            color = Color.White,
            radius = r * 0.06f,
            center = Offset(hPlanetX, hPlanetY)
        )

        // Draw Minute Planet (Middle)
        val mPlanetX = cx + rMinute * cos(minuteRad).toFloat()
        val mPlanetY = cy + rMinute * sin(minuteRad).toFloat()
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = r * 0.045f,
            center = Offset(mPlanetX, mPlanetY)
        )

        // Draw Second Planet (Outside) - only if active
        if (isActive) {
            val secondAngle = (second + millis / 1000f) * 6f - 90f
            val secondRad = Math.toRadians(secondAngle.toDouble())
            val sPlanetX = cx + rSecond * cos(secondRad).toFloat()
            val sPlanetY = cy + rSecond * sin(secondRad).toFloat()
            drawCircle(
                color = Color.Red,
                radius = r * 0.03f,
                center = Offset(sPlanetX, sPlanetY)
            )
        }

        // Draw a tiny center dot (Sun / Core)
        drawCircle(
            color = Color.White.copy(alpha = 0.3f),
            radius = r * 0.05f,
            center = Offset(cx, cy)
        )
    }
}

