package com.xenonware.mindcontrol

import android.app.KeyguardManager
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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.xenonware.mindcontrol.ui.res.AnalogAodStyle
import com.xenonware.mindcontrol.ui.res.BarsAodStyle
import com.xenonware.mindcontrol.ui.res.BlocksAodStyle
import com.xenonware.mindcontrol.ui.res.ConcentricAodStyle
import com.xenonware.mindcontrol.ui.res.InlineAodStyle
import com.xenonware.mindcontrol.ui.res.InlineDigitalAodStyle
import com.xenonware.mindcontrol.ui.res.InlineDotAodStyle
import com.xenonware.mindcontrol.ui.res.PixelInlineAodStyle
import com.xenonware.mindcontrol.ui.res.PixelStackedAodStyle
import com.xenonware.mindcontrol.ui.res.PlanetsAodStyle
import com.xenonware.mindcontrol.ui.res.SpinnerAodStyle
import com.xenonware.mindcontrol.ui.res.StackedAodStyle
import com.xenonware.mindcontrol.ui.res.StackedDigitalAodStyle
import com.xenonware.mindcontrol.ui.res.StackedDotAodStyle
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@Suppress("DEPRECATION")
class WatchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val km = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        if (!km.isKeyguardLocked) {
            finish()
            overridePendingTransition(0, 0)
            return
        }
        
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
                    delay(5000.milliseconds)
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

            DisposableEffect(context) {
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        if (intent?.action == Intent.ACTION_USER_PRESENT) {
                            Log.d("WatchActivity", "Device unlocked, finishing AOD")
                            finish()
                        }
                    }
                }
                context.registerReceiver(receiver, IntentFilter(Intent.ACTION_USER_PRESENT))
                onDispose {
                    try {
                        context.unregisterReceiver(receiver)
                    } catch (_: Exception) {}
                }
            }

            var isActive by remember { mutableStateOf(true) }
            var isFinishing by remember { mutableStateOf(false) }
            var totalDragX by remember { mutableFloatStateOf(0f) }
            var totalDragY by remember { mutableFloatStateOf(0f) }
            val animOffsetX = remember { Animatable(0f) }
            val animOffsetY = remember { Animatable(0f) }
            val animDragRadius = remember { Animatable(0f) }
            var lockedDirection by remember { mutableStateOf<String?>(null) }
            
            val scope = rememberCoroutineScope()
            
            val notifications by NotificationListener.activeNotificationsFlow.collectAsState()
            val rawMediaInfo by NotificationListener.activeMediaInfoFlow.collectAsState()
            val isMediaEnabled = remember { SettingsManager.isAodMediaEnabled(context) }
            val mediaInfo = if (isMediaEnabled) rawMediaInfo else null
            
            val aodStyle = remember { SettingsManager.getAodStyle(context) }

            val textAlphaTarget = if (isCharging) (if (isActive) 0.8f else 0.4f) else (if (isActive) 0.5f else 0f)

            val animatedTextAlpha by animateFloatAsState(
                targetValue = textAlphaTarget,
                label = "textAlpha",
                animationSpec = tween(durationMillis = 500)
            )

            LaunchedEffect(isActive) {
                if (isActive) {
                    delay(10000.milliseconds)
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
                                    val unlockThreshold = 150 * densityVal
                                    val mediaThreshold = 60 * densityVal
                                    val deadzone = 20 * densityVal

                                    detectDragGestures(
                                        onDragStart = { 
                                            Log.d("WatchActivity", "Drag started")
                                            isActive = true
                                            totalDragX = 0f
                                            totalDragY = 0f
                                            scope.launch { animOffsetX.snapTo(0f) }
                                            scope.launch { animOffsetY.snapTo(0f) }
                                            scope.launch { animDragRadius.snapTo(0f) }
                                            lockedDirection = null
                                        },
                                        onDragEnd = {
                                            Log.d("WatchActivity", "Drag ended: dir=$lockedDirection, dx=$totalDragX, dy=$totalDragY")
                                            val bounceSpec = spring<Float>(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                            
                                            if (lockedDirection == "V" && totalDragY < -unlockThreshold && !isFinishing) {
                                                isFinishing = true
                                                Log.d("WatchActivity", "Triggering UNLOCK")
                                                // Swipe UP to unlock - Bounce off-screen
                                                scope.launch {
                                                    animOffsetY.animateTo(
                                                        targetValue = -screenHeight,
                                                        animationSpec = spring(
                                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                                            stiffness = Spring.StiffnessLow
                                                        )
                                                    )
                                                    finish()
                                                    overridePendingTransition(0, android.R.anim.fade_out)
                                                }
                                            } else {
                                                // Media control or canceled unlock - Bounce back to center
                                                try {
                                                    if (lockedDirection == "V" && totalDragY > mediaThreshold && mediaInfo != null) {
                                                        Log.d("WatchActivity", "Triggering Play/Pause")
                                                        if (mediaInfo.isPlaying) {
                                                            mediaInfo.controller?.transportControls?.pause()
                                                        } else {
                                                            mediaInfo.controller?.transportControls?.play()
                                                        }
                                                    } else if (lockedDirection == "H" && mediaInfo != null) {
                                                        if (totalDragX < -mediaThreshold) {
                                                            Log.d("WatchActivity", "Triggering Skip Next")
                                                            mediaInfo.controller?.transportControls?.skipToNext()
                                                        } else if (totalDragX > mediaThreshold) {
                                                            Log.d("WatchActivity", "Triggering Skip Previous")
                                                            mediaInfo.controller?.transportControls?.skipToPrevious()
                                                        }
                                                    } else {
                                                        Log.d("WatchActivity", "No action triggered, bouncing back")
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e("WatchActivity", "Media action failed", e)
                                                }

                                                scope.launch { animOffsetX.animateTo(0f, bounceSpec) }
                                                scope.launch { animOffsetY.animateTo(0f, bounceSpec) }
                                                scope.launch { animDragRadius.animateTo(0f, bounceSpec) }
                                            }
                                            
                                            totalDragX = 0f
                                            totalDragY = 0f
                                            lockedDirection = null
                                        },
                                        onDragCancel = {
                                            Log.d("WatchActivity", "Drag canceled")
                                            val bounceSpec = spring<Float>(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                            scope.launch { animOffsetX.animateTo(0f, bounceSpec) }
                                            scope.launch { animOffsetY.animateTo(0f, bounceSpec) }
                                            scope.launch { animDragRadius.animateTo(0f, bounceSpec) }
                                            totalDragX = 0f
                                            totalDragY = 0f
                                            lockedDirection = null
                                        }
                                    ) { change, dragAmount ->
                                        change.consume()

                                        val screenMin = min(screenWidth, screenHeight)
                                        val maxVisualX = screenWidth * 0.2f
                                        val maxVisualY = screenHeight * 0.2f
                                        val maxRadius = screenMin * 0.08f

                                        // Only update drag amounts if not locked or if movement aligns with lock
                                        if (lockedDirection == null) {
                                            totalDragX += dragAmount.x
                                            totalDragY += dragAmount.y
                                            
                                            val absX = abs(totalDragX)
                                            val absY = abs(totalDragY)
                                            val maxDrag = max(absX, absY)

                                            if (maxDrag > 15 * densityVal) {
                                                lockedDirection = if (absY > absX || mediaInfo == null) "V" else "H"
                                                Log.d("WatchActivity", "Locking direction to: $lockedDirection")
                                            }
                                        } else {
                                            if (lockedDirection == "V") totalDragY += dragAmount.y
                                            else totalDragX += dragAmount.x
                                        }

                                        val absX = abs(totalDragX)
                                        val absY = abs(totalDragY)
                                        val maxDrag = max(absX, absY)

                                        var targetX: Float
                                        var targetY: Float
                                        var targetRadius: Float

                                        if (mediaInfo == null) {
                                            // STRICT MODE: No media player active
                                            targetX = 0f
                                            targetY = totalDragY.coerceIn(-maxVisualY, 0f)
                                            targetRadius = if (totalDragY < 0) {
                                                ((abs(totalDragY) - deadzone) / (unlockThreshold - deadzone)).coerceIn(0f, 1f) * maxRadius
                                            } else 0f
                                        } else {
                                            // NORMAL MODE: Media player active
                                            val activeThreshold = if (lockedDirection == "V" && totalDragY < 0) unlockThreshold else mediaThreshold
                                            val progress = (maxDrag / activeThreshold).coerceIn(0f, 1f)
                                            val snap = (progress / 0.1f).coerceIn(0f, 1f)

                                            when (lockedDirection) {
                                                "V" -> {
                                                    targetY = totalDragY.coerceIn(-maxVisualY, maxVisualY)
                                                    targetX = (totalDragX * (1f - snap)).coerceIn(-maxVisualX, maxVisualX)
                                                }
                                                "H" -> {
                                                    targetX = totalDragX.coerceIn(-maxVisualX, maxVisualX)
                                                    targetY = (totalDragY * (1f - snap)).coerceIn(-maxVisualY, maxVisualY)
                                                }
                                                else -> {
                                                    targetX = (totalDragX * (1f - snap)).coerceIn(-maxVisualX, maxVisualX)
                                                    targetY = (totalDragY * (1f - snap)).coerceIn(-maxVisualY, maxVisualY)
                                                }
                                            }
                                            
                                            targetRadius = ((maxDrag - deadzone) / (activeThreshold - deadzone)).coerceIn(0f, 1f) * maxRadius
                                        }

                                        scope.launch {
                                            animOffsetX.snapTo(targetX)
                                            animOffsetY.snapTo(targetY)
                                            animDragRadius.snapTo(targetRadius)
                                        }
                                    }
                                }
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset { IntOffset(animOffsetX.value.roundToInt(), animOffsetY.value.roundToInt()) }
                            .clip(RoundedCornerShape(with(LocalDensity.current) { 
                                // Coerce to 0 because bouncy springs can produce negative values during overshoot,
                                // which causes RoundedCornerShape to throw an IllegalArgumentException.
                                animDragRadius.value.coerceAtLeast(0f).toDp() 
                            }))
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
                                    offsetY = 0f, // We use the parent Box offset now
                                    isMediaEnabled = isMediaEnabled
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
                                    offsetY = 0f,
                                    isMediaEnabled = isMediaEnabled
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
                                    offsetY = 0f,
                                    isMediaEnabled = isMediaEnabled
                                )
                            }
                            SettingsManager.AodStyle.ANALOG -> {
                                AnalogAodStyle(
                                    isActive = isActive,
                                    notifications = notifications,
                                    mediaInfo = mediaInfo,
                                    isCharging = isCharging,
                                    batteryLevel = batteryLevel,
                                    animatedTextAlpha = animatedTextAlpha,
                                    offsetY = 0f,
                                    isMediaEnabled = isMediaEnabled
                                )
                            }
                            SettingsManager.AodStyle.STACKED_DOT -> {
                                StackedDotAodStyle(
                                    isActive = isActive,
                                    notifications = notifications,
                                    mediaInfo = mediaInfo,
                                    isCharging = isCharging,
                                    batteryLevel = batteryLevel,
                                    animatedTextAlpha = animatedTextAlpha,
                                    offsetY = 0f,
                                    isMediaEnabled = isMediaEnabled
                                )
                            }
                            SettingsManager.AodStyle.STACKED_DIGITAL -> {
                                StackedDigitalAodStyle(
                                    isActive = isActive,
                                    notifications = notifications,
                                    mediaInfo = mediaInfo,
                                    isCharging = isCharging,
                                    batteryLevel = batteryLevel,
                                    animatedTextAlpha = animatedTextAlpha,
                                    offsetY = 0f,
                                    isMediaEnabled = isMediaEnabled
                                )
                            }
                            SettingsManager.AodStyle.INLINE_DOT -> {
                                InlineDotAodStyle(
                                    isActive = isActive,
                                    notifications = notifications,
                                    mediaInfo = mediaInfo,
                                    isCharging = isCharging,
                                    batteryLevel = batteryLevel,
                                    animatedTextAlpha = animatedTextAlpha,
                                    offsetY = 0f,
                                    isMediaEnabled = isMediaEnabled
                                )
                            }
                            SettingsManager.AodStyle.INLINE_DIGITAL -> {
                                InlineDigitalAodStyle(
                                    isActive = isActive,
                                    notifications = notifications,
                                    mediaInfo = mediaInfo,
                                    isCharging = isCharging,
                                    batteryLevel = batteryLevel,
                                    animatedTextAlpha = animatedTextAlpha,
                                    offsetY = 0f,
                                    isMediaEnabled = isMediaEnabled
                                )
                            }
                            SettingsManager.AodStyle.PLANETS -> {
                                PlanetsAodStyle(
                                    isActive = isActive,
                                    notifications = notifications,
                                    mediaInfo = mediaInfo,
                                    isCharging = isCharging,
                                    batteryLevel = batteryLevel,
                                    animatedTextAlpha = animatedTextAlpha,
                                    offsetY = 0f,
                                    isMediaEnabled = isMediaEnabled
                                )
                            }
                            SettingsManager.AodStyle.PIXEL_STACKED -> {
                                PixelStackedAodStyle(
                                    isActive = isActive,
                                    notifications = notifications,
                                    mediaInfo = mediaInfo,
                                    isCharging = isCharging,
                                    batteryLevel = batteryLevel,
                                    animatedTextAlpha = animatedTextAlpha,
                                    offsetY = 0f,
                                    isMediaEnabled = isMediaEnabled
                                )
                            }
                            SettingsManager.AodStyle.PIXEL_INLINE -> {
                                PixelInlineAodStyle(
                                    isActive = isActive,
                                    notifications = notifications,
                                    mediaInfo = mediaInfo,
                                    isCharging = isCharging,
                                    batteryLevel = batteryLevel,
                                    animatedTextAlpha = animatedTextAlpha,
                                    offsetY = 0f,
                                    isMediaEnabled = isMediaEnabled
                                )
                            }
                            SettingsManager.AodStyle.BLOCKS -> {
                                BlocksAodStyle(
                                    isActive = isActive,
                                    notifications = notifications,
                                    mediaInfo = mediaInfo,
                                    isCharging = isCharging,
                                    batteryLevel = batteryLevel,
                                    animatedTextAlpha = animatedTextAlpha,
                                    offsetY = 0f,
                                    isMediaEnabled = isMediaEnabled
                                )
                            }
                            SettingsManager.AodStyle.BARS -> {
                                BarsAodStyle(
                                    isActive = isActive,
                                    notifications = notifications,
                                    mediaInfo = mediaInfo,
                                    isCharging = isCharging,
                                    batteryLevel = batteryLevel,
                                    animatedTextAlpha = animatedTextAlpha,
                                    offsetY = 0f,
                                    isMediaEnabled = isMediaEnabled
                                )
                            }
                            SettingsManager.AodStyle.SPINNER -> {
                                SpinnerAodStyle(
                                    isActive = isActive,
                                    notifications = notifications,
                                    mediaInfo = mediaInfo,
                                    isCharging = isCharging,
                                    batteryLevel = batteryLevel,
                                    animatedTextAlpha = animatedTextAlpha,
                                    offsetY = 0f,
                                    isMediaEnabled = isMediaEnabled
                                )
                            }
                        }
                    }
                    
                    // Swipe Visual Indicator
                    if (animDragRadius.value > 0) {
                        val indicatorAlignment = when (lockedDirection) {
                            "V" if totalDragY < 0 -> Alignment.BottomCenter
                            "V" if totalDragY > 0 -> Alignment.TopCenter
                            "H" if totalDragX < 0 -> Alignment.CenterEnd
                            "H" if totalDragX > 0 -> Alignment.CenterStart
                            else -> Alignment.Center
                        }


                        Box(
                            modifier = Modifier
                                .align(indicatorAlignment)
                                .size(with(LocalDensity.current) { (animDragRadius.value * 2).toDp() })
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0f)),
                            contentAlignment = Alignment.Center
                        ) {
                            val iconAlpha by animateFloatAsState(
                                targetValue = if (animDragRadius.value > 30 * densityVal) 0.7f else 0f,
                                label = "iconAlpha",
                                animationSpec = tween(300)
                            )

                            val icon = when (lockedDirection) {
                                "V" if totalDragY < 0 -> Icons.Rounded.LockOpen
                                "V" if totalDragY > 0 -> if (mediaInfo?.isPlaying == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow
                                "H" if totalDragX < 0 -> Icons.Rounded.SkipNext
                                "H" if totalDragX > 0 -> Icons.Rounded.SkipPrevious
                                else -> null
                            }

                            icon?.let {
                                Icon(
                                    imageVector = it,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(with(LocalDensity.current) { animDragRadius.value.toDp() })
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
