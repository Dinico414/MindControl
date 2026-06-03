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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

class MediaWatchActivity : ComponentActivity() {
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
                    
                    if (isEnabled && !isConnected) {
                        if (!toggleAttempted) {
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
                        
                        try {
                            NotificationListenerService.requestRebind(componentName)
                        } catch (e: Exception) {
                            Log.e("MediaWatchActivity", "Rebind failed", e)
                        }
                    } else if (isConnected) {
                        toggleAttempted = false
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
            var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }
            var totalDragX by remember { mutableFloatStateOf(0f) }
            var totalDragY by remember { mutableFloatStateOf(0f) }
            var offsetY by remember { mutableFloatStateOf(0f) }
            
            val mediaInfo by NotificationListener.activeMediaInfoFlow.collectAsState()

            val textAlphaTarget = if (isCharging) (if (isActive) 0.8f else 0.4f) else (if (isActive) 0.5f else 0f)

            val animatedTextAlpha by animateFloatAsState(
                targetValue = textAlphaTarget,
                label = "textAlpha",
                animationSpec = tween(durationMillis = 500)
            )

            LaunchedEffect(lastInteractionTime) {
                isActive = true
                delay(10000)
                isActive = false
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .pointerInput(mediaInfo) {
                        coroutineScope {
                            launch {
                                detectTapGestures(onPress = {
                                    lastInteractionTime = System.currentTimeMillis()
                                    tryAwaitRelease()
                                })
                            }
                            launch {
                                detectDragGestures(
                                    onDragStart = { 
                                        lastInteractionTime = System.currentTimeMillis()
                                        totalDragX = 0f
                                        totalDragY = 0f
                                    },
                                    onDragEnd = {
                                        val threshold = 50 * density
                                        when {
                                            totalDragY < -threshold -> { // Swipe UP to unlock
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
                MediaAodContent(
                    mediaInfo = mediaInfo,
                    isActive = isActive,
                    offsetY = offsetY,
                    animatedTextAlpha = animatedTextAlpha,
                    isCharging = isCharging,
                    batteryLevel = batteryLevel
                )
            }
        }
    }
}

@Composable
fun MediaAodContent(
    mediaInfo: MediaInfo?,
    isActive: Boolean,
    offsetY: Float,
    animatedTextAlpha: Float,
    isCharging: Boolean,
    batteryLevel: Int
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, offsetY.roundToInt()) }
    ) {
        // Background: Album Art darkened with vignette
        if (mediaInfo?.albumArt != null) {
            Image(
                bitmap = mediaInfo.albumArt.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (isActive) 0.5f else 0.3f),
                contentScale = ContentScale.Crop
            )
            // Darken overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            )
            // Vignette
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.Transparent, Color.Black),
                            radius = 1000f
                        )
                    )
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            
            Text(
                text = String.format(Locale.getDefault(), "%02d", hour),
                fontSize = 120.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.alpha(if (isActive) 0.9f else 0.7f)
            )
            Text(
                text = String.format(Locale.getDefault(), "%02d", minute),
                fontSize = 120.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.alpha(if (isActive) 0.9f else 0.7f)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (mediaInfo != null) {
                Text(
                    text = mediaInfo.title ?: "Unknown",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.alpha(if (isActive) 0.8f else 0.5f)
                )
                Text(
                    text = mediaInfo.artist ?: "Unknown Artist",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.alpha(if (isActive) 0.6f else 0.4f)
                )
            }
        }
        
        // Bottom battery/charging info
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.alpha(animatedTextAlpha)
            ) {
                if (isCharging) {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (batteryLevel >= 0) "$batteryLevel%" else "Charging",
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "Swipe up to unlock",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
