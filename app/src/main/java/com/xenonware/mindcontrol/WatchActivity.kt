package com.xenonware.mindcontrol

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter.Companion.tint
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.xenonware.mindcontrol.ui.PixelWatchFace
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

            var isActive by remember { mutableStateOf(true) }
            var offsetY by remember { mutableFloatStateOf(0f) }
            
            val notifications by NotificationListener.activeNotificationsFlow.collectAsState()
            
            LaunchedEffect(notifications) {
                Log.d("WatchActivity", "UI received ${notifications.size} notifications")
            }

            val animatedTextAlpha by animateFloatAsState(
                targetValue = if (isActive) 0.5f else 0f,
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
                    .pointerInput(Unit) {
                        coroutineScope {
                            launch {
                                detectTapGestures(onPress = {
                                    isActive = true
                                    tryAwaitRelease()
                                })
                            }
                            launch {
                                detectVerticalDragGestures(
                                    onDragStart = { isActive = true },
                                    onDragEnd = {
                                        if (offsetY < -150 * density) {
                                            finish()
                                            overridePendingTransition(0, android.R.anim.fade_out)
                                        } else {
                                            offsetY = 0f
                                        }
                                    }
                                ) { _, dragAmount ->
                                    if (offsetY + dragAmount <= 0) {
                                        offsetY += dragAmount
                                    }
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset { IntOffset(0, offsetY.roundToInt()) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    PixelWatchFace(isActive = isActive)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Notification Icons
                     NotificationIconsRow(notifications = notifications, isActive = isActive)

                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Swipe up to unlock",
                        color = Color.White.copy(alpha = animatedTextAlpha)
                    )
                    Spacer(modifier = Modifier.weight(0.2f))
                }
            }
        }
    }
}

@Composable
fun NotificationIconsRow(notifications: List<StatusBarNotification>, isActive: Boolean) {
    val context = LocalContext.current
    val maxIcons = 5
    val displayList = notifications.take(maxIcons)
    val hasMore = notifications.size > maxIcons
    
    Log.d("WatchActivity", "Rendering NotificationIconsRow with ${displayList.size} icons")

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.4f,
        label = "iconAlpha",
        animationSpec = tween(durationMillis = 500)
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.alpha(animatedAlpha)
    ) {
        displayList.forEach { sbn ->
            val iconDrawable = remember(sbn.key) {
                try {
                    // Try to load small icon, fallback to legacy icon if needed
                    val icon = sbn.notification.smallIcon?.loadDrawable(context) ?: 
                    context.packageManager.getResourcesForApplication(sbn.packageName)
                        .getDrawable(sbn.notification.icon, null)
                    
                    if (icon == null) Log.w("WatchActivity", "Failed to load icon for ${sbn.packageName}")
                    icon
                } catch (e: Exception) {
                    Log.e("WatchActivity", "Error loading icon for ${sbn.packageName}", e)
                    null
                }
            }

            if (iconDrawable != null) {
                val bitmap = remember(sbn.key) {
                    try {
                        // Ensure we have a bitmap of a reasonable size (e.g. 96x96 px)
                        val b = iconDrawable.toBitmap(96, 96).asImageBitmap()
                        Log.d("WatchActivity", "Successfully created bitmap for ${sbn.packageName}")
                        b
                    } catch (e: Exception) {
                        Log.e("WatchActivity", "Error converting drawable to bitmap for ${sbn.packageName}", e)
                        null
                    }
                }
                
                if (bitmap != null) {
                    AnimatedVisibility(
                        visible = true, // It's in the displayList, so it should be visible
                        enter = fadeIn(tween(1000)) + scaleIn(initialScale = 0.7f),
                        exit = fadeOut(tween(500)) + scaleOut(targetScale = 0.7f)
                    ) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            colorFilter = tint(Color.White)
                        )
                    }
                }
            }
        }
        
        if (hasMore) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}
