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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
            var offsetY by remember { mutableFloatStateOf(0f) }
            
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

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .pointerInput(mediaInfo) {
                        coroutineScope {
                            launch {
                                detectTapGestures(onPress = {
                                    isActive = true
                                    tryAwaitRelease()
                                })
                            }
                            launch {
                                detectDragGestures(
                                    onDragStart = { 
                                        isActive = true
                                        totalDragX = 0f
                                        totalDragY = 0f
                                    },
                                    onDragEnd = {
                                        val threshold = 50 * density
                                        when {
                                            totalDragY < -150 * density -> { // Swipe UP to unlock
                                                finish()
                                                overridePendingTransition(0, android.R.anim.fade_out)
                                            }
                                            totalDragY > threshold -> { // Swipe DOWN for pause/play
                                                if (mediaInfo?.isPlaying == true) {
                                                    mediaInfo?.controller?.transportControls?.pause()
                                                } else {
                                                    mediaInfo?.controller?.transportControls?.play()
                                                }
                                            }
                                            totalDragX < -threshold -> { // Swipe LEFT for next
                                                mediaInfo?.controller?.transportControls?.skipToNext()
                                            }
                                            totalDragX > threshold -> { // Swipe RIGHT for previous
                                                mediaInfo?.controller?.transportControls?.skipToPrevious()
                                            }
                                        }
                                        offsetY = 0f
                                        totalDragX = 0f
                                        totalDragY = 0f
                                    },
                                    onDragCancel = {
                                        offsetY = 0f
                                        totalDragX = 0f
                                        totalDragY = 0f
                                    }
                                ) { change, dragAmount ->
                                    change.consume()
                                    totalDragX += dragAmount.x
                                    totalDragY += dragAmount.y
                                    
                                    if (offsetY + dragAmount.y <= 0) {
                                        offsetY += dragAmount.y
                                    }
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
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
                            offsetY = offsetY
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
                            offsetY = offsetY
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
                            offsetY = offsetY
                        )
                    }
                }
            }
        }
    }
}
