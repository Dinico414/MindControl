package com.xenonware.mindcontrol

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.xenonware.mindcontrol.ui.ConcentricAodStyle
import com.xenonware.mindcontrol.ui.StackedAodStyle
import com.xenonware.mindcontrol.ui.InlineAodStyle
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class WatchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Hide navigation and status bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        
        setContent {
            val context = LocalContext.current
            val density = LocalDensity.current.density
            
            // Check if service is enabled and monitor connection
            LaunchedEffect(Unit) {
                val componentName = ComponentName(context, NotificationListener::class.java)
                var toggleAttempted = false
                
                while(true) {
                    val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                    val isEnabled = enabledListeners?.contains(context.packageName) == true
                    val isConnected = NotificationListener.isServiceConnected
                    
                    Log.d("WatchActivity", "Service Status: Enabled=$isEnabled, Connected=$isConnected")
                    
                    if (isEnabled && !isConnected) {
                        if (!toggleAttempted) {
                            Log.d("WatchActivity", "Service stuck. Attempting Component Toggle trick...")
                            context.packageManager.setComponentEnabledSetting(
                                componentName,
                                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                PackageManager.DONT_KILL_APP
                            )
                            context.packageManager.setComponentEnabledSetting(
                                componentName,
                                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                                PackageManager.DONT_KILL_APP
                            )
                            toggleAttempted = true
                        }
                        
                        Log.d("WatchActivity", "Requesting service rebind...")
                        try {
                            NotificationListenerService.requestRebind(componentName)
                        } catch (e: Exception) {
                            Log.e("WatchActivity", "Rebind failed", e)
                        }
                    } else if (isConnected) {
                        toggleAttempted = false // Reset if it connects
                    }
                    delay(5000)
                }
            }

            var batteryLevel by remember { mutableIntStateOf(-1) }
            var isCharging by remember { mutableStateOf(false) }

            DisposableEffect(context) {
                fun updateBattery(intent: Intent?) {
                    intent?.let {
                        val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                        val plugged = it.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                        isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                     status == BatteryManager.BATTERY_STATUS_FULL ||
                                     plugged > 0
                        
                        val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                        val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                        if (level != -1 && scale != -1) {
                            batteryLevel = (level * 100 / scale.toFloat()).roundToInt()
                        }
                        Log.d("WatchActivity", "Battery Update: isCharging=$isCharging, level=$batteryLevel")
                    }
                }

                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        updateBattery(intent)
                    }
                }
                val intent = context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                updateBattery(intent)
                onDispose {
                    context.unregisterReceiver(receiver)
                }
            }

            var isActive by remember { mutableStateOf(true) }
            var totalDragX by remember { mutableFloatStateOf(0f) }
            var totalDragY by remember { mutableFloatStateOf(0f) }
            var offsetX by remember { mutableFloatStateOf(0f) }
            var offsetY by remember { mutableFloatStateOf(0f) }
            var dragRadius by remember { mutableFloatStateOf(0f) }
            var lockedDirection by remember { mutableStateOf<String?>(null) }
            
            val notifications by NotificationListener.activeNotificationsFlow.collectAsState()
            val mediaInfo by NotificationListener.activeMediaInfoFlow.collectAsState()
            
            val aodStyle = remember<SettingsManager.AodStyle> { SettingsManager.getAodStyle(context) }

            val textAlphaTarget = if (isCharging) (if (isActive) 0.8f else 0.4f) else (if (isActive) 0.5f else 0f)

            val animatedTextAlpha by animateFloatAsState(
                targetValue = textAlphaTarget,
                label = "textAlpha",
                animationSpec = tween(durationMillis = 500)
            )

            LaunchedEffect(isActive) {
                if (isActive) {
                    delay(10000)
                    isActive = false
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                val screenWidth = constraints.maxWidth.toFloat()
                val screenHeight = constraints.maxHeight.toFloat()
                val densityVal = LocalDensity.current.density

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            coroutineScope {
                                launch {
                                    detectTapGestures(onPress = {
                                        isActive = true
                                        tryAwaitRelease()
                                    })
                                }
                                launch {
                                    val actionThreshold = 160 * densityVal
                                    val deadzone = 25 * densityVal

                                    detectDragGestures(
                                        onDragStart = { 
                                            isActive = true
                                            totalDragX = 0f
                                            totalDragY = 0f
                                            offsetX = 0f
                                            offsetY = 0f
                                            dragRadius = 0f
                                            lockedDirection = null
                                        },
                                        onDragEnd = {
                                            val currentMedia = mediaInfo
                                            
                                            when (lockedDirection) {
                                                "V" -> {
                                                    if (totalDragY < -actionThreshold) {
                                                        // Swipe UP to unlock
                                                        finish()
                                                        overridePendingTransition(0, android.R.anim.fade_out)
                                                    } else if (totalDragY > actionThreshold && currentMedia != null) {
                                                        // Swipe DOWN for pause/play
                                                        if (currentMedia.isPlaying == true) {
                                                            currentMedia.controller?.transportControls?.pause()
                                                        } else {
                                                            currentMedia.controller?.transportControls?.play()
                                                        }
                                                    }
                                                }
                                                "H" -> {
                                                    if (currentMedia != null) {
                                                        if (totalDragX < -actionThreshold) {
                                                            // Swipe LEFT for next
                                                            currentMedia.controller?.transportControls?.skipToNext()
                                                        } else if (totalDragX > actionThreshold) {
                                                            // Swipe RIGHT for previous
                                                            currentMedia.controller?.transportControls?.skipToPrevious()
                                                        }
                                                    }
                                                }
                                            }
                                            
                                            offsetX = 0f
                                            offsetY = 0f
                                            totalDragX = 0f
                                            totalDragY = 0f
                                            dragRadius = 0f
                                            lockedDirection = null
                                        },
                                    ) { change, dragAmount ->
                                        change.consume()
                                        
                                        val currentMedia = mediaInfo
                                        val screenMin = kotlin.math.min(screenWidth, screenHeight)
                                        val maxVisualX = screenWidth * 0.2f
                                        val maxVisualY = screenHeight * 0.2f
                                        val maxRadius = screenMin * 0.08f

                                        // Only update drag amounts if not locked or if movement aligns with lock
                                        if (lockedDirection == null) {
                                            totalDragX += dragAmount.x
                                            totalDragY += dragAmount.y
                                            
                                            val absX = kotlin.math.abs(totalDragX)
                                            val absY = kotlin.math.abs(totalDragY)
                                            val maxDrag = kotlin.math.max(absX, absY)

                                            if (maxDrag > 15 * densityVal) {
                                                lockedDirection = if (currentMedia == null) "V" else (if (absY > absX) "V" else "H")
                                            }
                                        } else {
                                            if (lockedDirection == "V") totalDragY += dragAmount.y
                                            else totalDragX += dragAmount.x
                                        }

                                        val absX = kotlin.math.abs(totalDragX)
                                        val absY = kotlin.math.abs(totalDragY)
                                        val maxDrag = kotlin.math.max(absX, absY)

                                        if (currentMedia == null) {
                                            // STRICT MODE: No media player active
                                            offsetX = 0f
                                            offsetY = totalDragY.coerceIn(-maxVisualY, 0f)
                                            dragRadius = if (totalDragY < 0) {
                                                ((kotlin.math.abs(totalDragY) - deadzone) / (actionThreshold - deadzone)).coerceIn(0f, 1f) * maxRadius
                                            } else 0f
                                        } else {
                                            // NORMAL MODE: Media player active
                                            val progress = (maxDrag / actionThreshold).coerceIn(0f, 1f)
                                            val snap = (progress / 0.1f).coerceIn(0f, 1f)

                                            if (lockedDirection == "V") {
                                                offsetY = totalDragY.coerceIn(-maxVisualY, maxVisualY)
                                                offsetX = (totalDragX * (1f - snap)).coerceIn(-maxVisualX, maxVisualX)
                                            } else if (lockedDirection == "H") {
                                                offsetX = totalDragX.coerceIn(-maxVisualX, maxVisualX)
                                                offsetY = (totalDragY * (1f - snap)).coerceIn(-maxVisualY, maxVisualY)
                                            } else {
                                                offsetX = (totalDragX * (1f - snap)).coerceIn(-maxVisualX, maxVisualX)
                                                offsetY = (totalDragY * (1f - snap)).coerceIn(-maxVisualY, maxVisualY)
                                            }
                                            
                                            dragRadius = ((maxDrag - deadzone) / (actionThreshold - deadzone)).coerceIn(0f, 1f) * maxRadius
                                        }
                                    }
                                }
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                            .clip(RoundedCornerShape(with(LocalDensity.current) { dragRadius.toDp() }))
                    ) {
                        when (aodStyle) {
                            SettingsManager.AodStyle.CONCENTRIC -> {
                                ConcentricAodStyle(
                                    isActive = isActive,
                                    notifications = notifications,
                                    mediaInfo = mediaInfo,
                                    isCharging = isCharging,
                                    batteryLevel = batteryLevel,
                                    animatedTextAlpha = animatedTextAlpha,
                                    offsetY = 0f // We use the parent Box offset now
                                )
                            }
                            SettingsManager.AodStyle.STACKED -> {
                                StackedAodStyle(
                                    isActive = isActive,
                                    notifications = notifications,
                                    mediaInfo = mediaInfo,
                                    isCharging = isCharging,
                                    batteryLevel = batteryLevel,
                                    animatedTextAlpha = animatedTextAlpha,
                                    offsetY = 0f
                                )
                            }
                            SettingsManager.AodStyle.INLINE -> {
                                InlineAodStyle(
                                    isActive = isActive,
                                    notifications = notifications,
                                    mediaInfo = mediaInfo,
                                    isCharging = isCharging,
                                    batteryLevel = batteryLevel,
                                    animatedTextAlpha = animatedTextAlpha,
                                    offsetY = 0f
                                )
                            }
                        }
                    }
                    
                    // Swipe Visual Indicator
                    if (dragRadius > 0) {
                        val indicatorAlignment = when {
                            lockedDirection == "V" && totalDragY < 0 -> Alignment.BottomCenter
                            lockedDirection == "V" && totalDragY > 0 -> Alignment.TopCenter
                            lockedDirection == "H" && totalDragX < 0 -> Alignment.CenterEnd
                            lockedDirection == "H" && totalDragX > 0 -> Alignment.CenterStart
                            else -> Alignment.Center
                        }

                        val circleAlpha by animateFloatAsState(
                            targetValue = if (dragRadius > 10 * densityVal) 0.6f else 0f,
                            label = "circleAlpha",
                            animationSpec = tween(150)
                        )

                        Box(
                            modifier = Modifier
                                .align(indicatorAlignment)
                                .size(with(LocalDensity.current) { (dragRadius * 2).toDp() })
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = circleAlpha)),
                            contentAlignment = Alignment.Center
                        ) {
                            val iconAlpha by animateFloatAsState(
                                targetValue = if (dragRadius > 30 * densityVal) 0.7f else 0f,
                                label = "iconAlpha",
                                animationSpec = tween(300)
                            )

                            val icon = when {
                                lockedDirection == "V" && totalDragY < 0 -> Icons.Rounded.LockOpen
                                lockedDirection == "V" && totalDragY > 0 -> if (mediaInfo?.isPlaying == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow
                                lockedDirection == "H" && totalDragX < 0 -> Icons.Rounded.SkipNext
                                lockedDirection == "H" && totalDragX > 0 -> Icons.Rounded.SkipPrevious
                                else -> null
                            }
                            
                            icon?.let {
                                Icon(
                                    imageVector = it,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(with(LocalDensity.current) { dragRadius.toDp() })
                                        .alpha(iconAlpha)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
