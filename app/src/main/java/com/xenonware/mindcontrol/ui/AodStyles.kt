package com.xenonware.mindcontrol.ui

import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter.Companion.tint
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.xenonware.mindcontrol.MediaInfo
import com.xenonware.mindcontrol.R
import kotlin.math.roundToInt

@Composable
fun ConcentricAodStyle(
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
        watchFace = { ConcentricWatchFace(isActive = isActive) }
    )
}

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
fun PixelStackedAodStyle(
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
        watchFace = { PixelStackedWatchFace(isActive = isActive) }
    )
}

@Composable
fun PixelInlineAodStyle(
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
        watchFace = { PixelInlineWatchFace(isActive = isActive) }
    )
}

@Composable
fun StackedAodStyle(
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
        watchFace = { StackedWatchFace(isActive = isActive) }
    )
}

@Composable
fun InlineAodStyle(
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
        watchFace = { InlineWatchFace(isActive = isActive) }
    )
}

@Composable
fun AnalogAodStyle(
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
        watchFace = { AnalogWatchFace(isActive = isActive) }
    )
}

@Composable
fun StackedDotAodStyle(
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
        watchFace = { StackedDotWatchFace(isActive = isActive) }
    )
}

@Composable
fun StackedDigitalAodStyle(
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
        watchFace = { StackedDigitalWatchFace(isActive = isActive) }
    )
}

@Composable
fun InlineDotAodStyle(
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
        watchFace = { InlineDotWatchFace(isActive = isActive) }
    )
}

@Composable
fun InlineDigitalAodStyle(
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
        watchFace = { InlineDigitalWatchFace(isActive = isActive) }
    )
}

@Composable
fun PlanetsAodStyle(
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
        watchFace = { PlanetsWatchFace(isActive = isActive) }
    )
}


@Composable
fun UnifiedAodStyle(
    isActive: Boolean,
    notifications: List<StatusBarNotification>,
    mediaInfo: MediaInfo?,
    isCharging: Boolean,
    batteryLevel: Int,
    animatedTextAlpha: Float,
    offsetY: Float,
    isMediaEnabled: Boolean = true,
    watchFace: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, offsetY.roundToInt()) }
    ) {
        // Background: Album Art
        if (isMediaEnabled) {
            AodAlbumArtBackground(mediaInfo = mediaInfo, isActive = isActive)
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            watchFace()

            Spacer(modifier = Modifier.weight(0.5f))
            
            NotificationIconsRow(notifications = notifications, isActive = isActive)

            if (isMediaEnabled && mediaInfo != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = mediaInfo.title ?: stringResource(R.string.aod_unknown),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.alpha(if (isActive) 0.8f else 0.5f)
                )
                Text(
                    text = mediaInfo.artist ?: stringResource(R.string.aod_unknown_artist),
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.alpha(if (isActive) 0.6f else 0.4f)
                )
            }

            Spacer(modifier = Modifier.weight(0.5f))
            
            AodBottomInfo(
                isCharging = isCharging,
                batteryLevel = batteryLevel,
                animatedTextAlpha = animatedTextAlpha,
                mediaInfo = null,
                isActive = isActive
            )
            Spacer(modifier = Modifier.weight(0.2f))
        }
    }
}

@Composable
fun AodAlbumArtBackground(mediaInfo: MediaInfo?, isActive: Boolean) {
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
}

@Composable
fun AodBottomInfo(
    isCharging: Boolean,
    batteryLevel: Int,
    animatedTextAlpha: Float,
    mediaInfo: MediaInfo?,
    isActive: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.alpha(animatedTextAlpha)
    ) {
        AnimatedContent(
            targetState = isCharging,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "batteryTransition"
        ) { charging ->
            if (charging) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (batteryLevel >= 0) "$batteryLevel%" else stringResource(R.string.aod_charging),
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            } else if (mediaInfo != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = mediaInfo.title ?: stringResource(R.string.aod_unknown),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.alpha(if (isActive) 0.8f else 0.5f)
                    )
                    Text(
                        text = mediaInfo.artist ?: stringResource(R.string.aod_unknown_artist),
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.alpha(if (isActive) 0.6f else 0.4f)
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.swipe_up_to_unlock),
                    color = Color.White,
                    fontSize = 14.sp
                )
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
    
    Log.d("AodStyles", "Rendering NotificationIconsRow with ${displayList.size} icons")

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
                    
                    if (icon == null) Log.w("AodStyles", "Failed to load icon for ${sbn.packageName}")
                    icon
                } catch (e: Exception) {
                    Log.e("AodStyles", "Error loading icon for ${sbn.packageName}", e)
                    null
                }
            }

            if (iconDrawable != null) {
                val bitmap = remember(sbn.key) {
                    try {
                        // Ensure we have a bitmap of a reasonable size (e.g. 96x96 px)
                        val b = iconDrawable.toBitmap(96, 96).asImageBitmap()
                        Log.d("AodStyles", "Successfully created bitmap for ${sbn.packageName}")
                        b
                    } catch (e: Exception) {
                        Log.e("AodStyles", "Error converting drawable to bitmap for ${sbn.packageName}", e)
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
