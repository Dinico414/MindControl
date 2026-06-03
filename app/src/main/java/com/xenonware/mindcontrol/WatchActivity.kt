package com.xenonware.mindcontrol

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
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
            val density = LocalDensity.current.density
            var isActive by remember { mutableStateOf(true) }
            var offsetY by remember { mutableFloatStateOf(0f) }
            
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
