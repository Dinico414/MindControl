@file:Suppress("AssignedValueIsNeverRead")

package com.xenonware.mindcontrol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import com.xenonware.mindcontrol.ui.layouts.MindControlMainScreen
import com.xenonware.mindcontrol.ui.theme.PaletteTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val context = LocalContext.current
            var devicePalette by rememberSaveable { mutableStateOf(SettingsManager.getDevicePalette(context)) }
            var keyboardPalette by rememberSaveable {
                mutableStateOf(
                    SettingsManager.getKeyboardPalette(
                        context
                    )
                )
            }

            PaletteTheme(palette = devicePalette) {
                Surface(color = Color.Black) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        MindControlMainScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(WindowInsets.safeDrawing.asPaddingValues()),
                            devicePalette = devicePalette,
                            keyboardPalette = keyboardPalette,
                            onDevicePaletteChange = {
                                devicePalette = it
                                SettingsManager.setDevicePalette(context, it)
                            },
                            onKeyboardPaletteChange = {
                                keyboardPalette = it
                                SettingsManager.setKeyboardPalette(context, it)
                            },
                        )
                    }
                }
            }
        }
    }
}
