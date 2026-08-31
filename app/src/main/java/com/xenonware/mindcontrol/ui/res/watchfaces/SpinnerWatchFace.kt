package com.xenonware.mindcontrol.ui.res.watchfaces

import android.service.notification.StatusBarNotification
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import com.xenonware.mindcontrol.MediaInfo
import com.xenonware.mindcontrol.ui.res.UnifiedAodStyle
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SpinnerAodStyle(
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
        watchFace = { SpinnerWatchFace(isActive = isActive) }
    )
}

@Composable
fun SpinnerWatchFace(isActive: Boolean) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.7f,
        animationSpec = tween(500),
        label = "alpha"
    )

    var time by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(isActive) {
        while (true) {
            time = System.currentTimeMillis()
            if (isActive) delay(16.milliseconds) else delay(1000.milliseconds)
        }
    }

    val calendar = Calendar.getInstance().apply { timeInMillis = time }
    val hour = calendar.get(Calendar.HOUR)
    val minute = calendar.get(Calendar.MINUTE)

    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.6f)
            .aspectRatio(1f)
            .graphicsLayer(alpha = animatedAlpha)
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = w / 2f

        val tickCount = 12
        val hourIndex = hour % 12
        
        val minuteIndex = minute / 5
        val nextMinuteIndex = (minuteIndex + 1) % 12
        val minuteFraction = (minute % 5) / 5f

        // Ticks configuration
        // Scaled up lengths (aiming for ~1.5x where possible without crossing center)
        val tickLenShort = r * 0.48f 
        val tickLenHour = r * 0.88f // Kept long, nearly reaching center
        val maxR = r * 0.96f // Furthest out point
        
        val pillThickness = r * 0.08f // Slimmed down from 0.12r
        val borderThickness = r * 0.004f // Decreased from 0.007r
        val cornerRadius = CornerRadius(pillThickness / 2f)

        for (i in 0 until tickCount) {
            val angleDeg = i * 30f - 90f
            
            val isHour = i == hourIndex
            val currentLen = if (isHour) tickLenHour else tickLenShort
            
            // Minutes logic:
            // Every 5 minutes the "active" indicator moves to the next tick.
            // Transitions are linear: 100% -> 80% -> 60% -> 40% -> 20% -> 0%
            var fillAlpha = 0f 
            if (i == minuteIndex) {
                fillAlpha = 1f - minuteFraction
            } else if (i == nextMinuteIndex) {
                fillAlpha = minuteFraction
            }

            // All borders are 100% white as requested
            val borderAlpha = 1f

            // Draw a rounded rectangle rotated to the correct position
            rotate(degrees = angleDeg + 90f, pivot = Offset(cx, cy)) {
                // topLeft is calculated so the outer edge (top in local space) is always at maxR
                val topLeft = Offset(cx - pillThickness / 2f, cy - maxR)
                val size = Size(pillThickness, currentLen)
                
                // Filling: starts at 0% and reacts to the minute logic (0, 0.2, 0.4, 0.6, 0.8, 1.0)
                if (fillAlpha > 0f) {
                    drawRoundRect(
                        color = Color.White.copy(alpha = fillAlpha),
                        topLeft = topLeft,
                        size = size,
                        cornerRadius = cornerRadius
                    )
                }
                
                // Border: 100% white for all ticks
                drawRoundRect(
                    color = Color.White.copy(alpha = borderAlpha),
                    topLeft = topLeft,
                    size = size,
                    cornerRadius = cornerRadius,
                    style = Stroke(width = borderThickness)
                )
            }
        }
    }
}
