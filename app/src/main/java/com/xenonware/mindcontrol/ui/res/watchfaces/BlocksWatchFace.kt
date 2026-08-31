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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenonware.mindcontrol.MediaInfo
import com.xenonware.mindcontrol.ui.res.UnifiedAodStyle
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Locale
import kotlin.math.max
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun BlocksAodStyle(
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
        watchFace = { QuicksandBlocksWatchFace(isActive = isActive) }
    )
}

@Composable
fun QuicksandBlocksWatchFace(isActive: Boolean) {
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
    val isFullyInactive = !isActive

    LaunchedEffect(isFullyInactive) {
        while (true) {
            time = System.currentTimeMillis()
            if (!isFullyInactive) {
                delay(16.milliseconds)
            } else {
                val calendar = Calendar.getInstance().apply { timeInMillis = time }
                val seconds = calendar.get(Calendar.SECOND)
                val millis = calendar.get(Calendar.MILLISECOND)
                val delayToNextMinute = 60000L - (seconds * 1000L + millis)
                delay(max(delayToNextMinute, 100L).milliseconds)
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

        fun pxToSp(px: Float): TextUnit =
            (px / localDensity.density / localDensity.fontScale).sp

        // Lower area for 6 big blocks in 3x2 grid
        val blockAreaTop = h * 0.35f
        val blockAreaHeight = h * 0.60f
        
        // Calculate block size S and spacing g
        // We want 3 columns and 2 rows of squares of size S
        val g = w * 0.04f
        
        // Solve for S. If gridWidth is at most w * 0.85f:
        // 3 * S + 2 * g = w * 0.85f => 3 * S = 0.85 * w - 2 * g
        // S = (w * 0.85f - 2 * g) / 3f
        val s = (w * 0.85f - 2 * g) / 3f
        
        val gridWidth = 3 * s + 2 * g
        val gridHeight = 2 * s + g
        
        val startX = cx - gridWidth / 2f
        val startY = blockAreaTop + (blockAreaHeight - gridHeight) / 2f

        // Hour on top of the left first square (first block col 0, row 0)
        // Let's center it horizontally with the first square
        val hourCenterX = startX + s / 2f

        val hourStyle = TextStyle(
            color = Color.White,
            fontSize = pxToSp(w * 0.30f), // Sized beautifully
            fontWeight = FontWeight.Bold,
            fontFamily = QuicksandTitleVariable
        )

        val hourText = String.format(Locale.getDefault(), "%02d", displayHour)
        val hourLayout = textMeasurer.measure(hourText, hourStyle)
        
        val hourY = h * 0.02f
        drawText(
            textLayoutResult = hourLayout,
            topLeft = Offset(hourCenterX - hourLayout.size.width / 2f, hourY)
        )

        val completedIntervals = minute / 10
        val currentIntervalMinute = minute % 10

        for (row in 0 until 2) {
            for (col in 0 until 3) {
                val blockIndex = row * 3 + col
                val bx = startX + col * (s + g)
                val by = startY + row * (s + g)

                // Each of the 6 big blocks is divided into a 3x3 grid of cells
                val c = s / 3f

                // State of the block
                when {
                    blockIndex < completedIntervals -> {
                        // Completed block: "filled for full block" - 9 squares fill the entire area
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(bx, by),
                            size = Size(s, s)
                        )
                    }
                    blockIndex == completedIntervals -> {
                        // Current block
                        for (rSub in 0 until 3) {
                            for (cSub in 0 until 3) {
                                val subIndex = rSub * 3 + cSub // 0 to 8
                                val x = bx + cSub * c
                                val y = by + rSub * c

                                val isPast = subIndex < currentIntervalMinute

                                if (isPast) {
                                    // "middle used"
                                    val size = c * 0.65f
                                    val offset = (c - size) / 2f
                                    drawRect(
                                        color = Color.White,
                                        topLeft = Offset(x + offset, y + offset),
                                        size = Size(size, size)
                                    )
                                } else {
                                    // "small not used"
                                    val size = c * 0.25f
                                    val offset = (c - size) / 2f
                                    drawRect(
                                        color = Color.White.copy(alpha = 0.35f),
                                        topLeft = Offset(x + offset, y + offset),
                                        size = Size(size, size)
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        // Future block - all squares are "small not used"
                        for (rSub in 0 until 3) {
                            for (cSub in 0 until 3) {
                                val x = bx + cSub * c
                                val y = by + rSub * c
                                val size = c * 0.25f
                                val offset = (c - size) / 2f
                                drawRect(
                                    color = Color.White.copy(alpha = 0.35f),
                                    topLeft = Offset(x + offset, y + offset),
                                    size = Size(size, size)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
