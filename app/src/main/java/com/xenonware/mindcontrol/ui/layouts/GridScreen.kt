package com.xenonware.mindcontrol.ui.layouts

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.FilterCenterFocus
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenonware.mindcontrol.ButtonState
import com.xenonware.mindcontrol.R
import com.xenonware.mindcontrol.ui.theme.BlueTheme
import com.xenonware.mindcontrol.ui.theme.GreenTheme
import com.xenonware.mindcontrol.ui.theme.Palette
import com.xenonware.mindcontrol.ui.theme.PaletteTheme
import com.xenonware.mindcontrol.ui.theme.RedTheme
import com.xenonware.mindcontrol.ui.theme.YellowTheme

@Composable
fun GridScreen(
    modifier: Modifier = Modifier,
    devicePalette: Palette,
    keyboardPalette: Palette,
    onDevicePaletteChange: (Palette) -> Unit,
    onKeyboardPaletteChange: (Palette) -> Unit,
    onButtonSelected: (Int, String) -> Unit,
) {
    val pressedKeys by ButtonState.pressedKeys.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        // Top Part (Weight 2f)
        Row(
            modifier = Modifier
                .weight(2f)
                .fillMaxWidth()
        ) {
            // AI Button
            RedTheme {
                Surface(
                    onClick = { onButtonSelected(131, "AI Button") },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(end = 4.dp, bottom = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (pressedKeys.contains(131)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier.weight(0.333f), contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.stars),
                                contentDescription = null,
                                tint = if (pressedKeys.contains(131)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Box(
                            modifier = Modifier.weight(0.667f), contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.ai_button),
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                                fontFamily = QuicksandTitleVariable,
                                color = if (pressedKeys.contains(131)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Right Top Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                YellowTheme {
                    // Camera Up
                    Surface(
                        onClick = { onButtonSelected(133, "Camera Up") },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(start = 4.dp, bottom = 1.dp),
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = 4.dp,
                            bottomEnd = 4.dp
                        ),
                        color = if (pressedKeys.contains(133)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier.weight(0.333f),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowUp,
                                    contentDescription = null,
                                    tint = if (pressedKeys.contains(133)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Box(
                                modifier = Modifier.weight(0.667f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.camera_up),
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                    color = if (pressedKeys.contains(133)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontFamily = QuicksandTitleVariable
                                )
                            }
                        }
                    }
                    // Camera Down
                    Surface(
                        onClick = { onButtonSelected(132, "Camera Down") },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 1.dp, bottom = 4.dp),
                        shape = RoundedCornerShape(
                            topStart = 4.dp,
                            topEnd = 4.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp
                        ),
                        color = if (pressedKeys.contains(132)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier.weight(0.333f),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = if (pressedKeys.contains(132)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Box(
                                modifier = Modifier.weight(0.667f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.camera_down),
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                    color = if (pressedKeys.contains(132)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontFamily = QuicksandTitleVariable
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Part (Weight 4f)
        Row(
            modifier = Modifier
                .weight(4f)
                .fillMaxWidth()
        ) {
            val configuration = LocalConfiguration.current
            val hasKeyboard = configuration.keyboard != Configuration.KEYBOARD_NOKEYS

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                TogglesContainer(
                    modifier = Modifier
                        .weight(if (hasKeyboard) 3f else 4f)
                        .fillMaxWidth(),
                    hasKeyboard = hasKeyboard,
                    devicePalette = devicePalette,
                    keyboardPalette = keyboardPalette,
                    onDevicePaletteChange = onDevicePaletteChange,
                    onKeyboardPaletteChange = onKeyboardPaletteChange,
                )

                if (hasKeyboard) {
                    PaletteTheme(palette = keyboardPalette) {
                        Surface(
                            onClick = { onButtonSelected(111, "Keyboard Button") },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(end = 4.dp, top = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = if (pressedKeys.contains(111)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier.weight(0.333f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Keyboard,
                                        contentDescription = null,
                                        // Now uses the onPrimary color from keyboardPalette
                                        tint = if (pressedKeys.contains(111)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Box(
                                    modifier = Modifier.weight(0.667f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.keyboard),
                                        style = MaterialTheme.typography.titleMedium,
                                        textAlign = TextAlign.Center,
                                        color = if (pressedKeys.contains(111)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontFamily = QuicksandTitleVariable
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Right Bottom Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                GreenTheme {
                    // Volume Up
                    Surface(
                        onClick = { onButtonSelected(24, "Volume Up") },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 4.dp, bottom = 1.dp),
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = 4.dp,
                            bottomEnd = 4.dp
                        ),
                        color = if (pressedKeys.contains(24)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier.weight(0.333f),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AddCircle,
                                    contentDescription = null,
                                    tint = if (pressedKeys.contains(24)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Box(
                                modifier = Modifier.weight(0.667f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.volume_up),
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                    color = if (pressedKeys.contains(24)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontFamily = QuicksandTitleVariable
                                )
                            }
                        }
                    }
                    // Volume Down
                    Surface(
                        onClick = { onButtonSelected(25, "Volume Down") },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 1.dp, bottom = 4.dp),
                        shape = RoundedCornerShape(
                            topStart = 4.dp,
                            topEnd = 4.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp
                        ),
                        color = if (pressedKeys.contains(25)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier.weight(0.333f),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.RemoveCircle,
                                    contentDescription = null,
                                    tint = if (pressedKeys.contains(25)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Box(
                                modifier = Modifier.weight(0.667f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.volume_down),
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                    color = if (pressedKeys.contains(25)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontFamily = QuicksandTitleVariable
                                )
                            }
                        }
                    }
                }
                // Camera + Focus row
                Row(
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxWidth()
                ) {
                    BlueTheme {
                        // Camera Button
                        Surface(
                            onClick = { onButtonSelected(27, "Camera Button") },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(start = 4.dp, end = 1.dp, top = 4.dp),
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 4.dp,
                                bottomStart = 16.dp,
                                bottomEnd = 4.dp
                            ),
                            color = if (pressedKeys.contains(27)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CameraAlt,
                                    contentDescription = null,
                                    tint = if (pressedKeys.contains(27)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.camera_button),
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                    color = if (pressedKeys.contains(27)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontFamily = QuicksandTitleVariable
                                )
                            }
                        }
                        // Focus Button
                        Surface(
                            onClick = { onButtonSelected(134, "Focus Button") },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(start = 1.dp, top = 4.dp),
                            shape = RoundedCornerShape(
                                topStart = 4.dp,
                                topEnd = 16.dp,
                                bottomStart = 4.dp,
                                bottomEnd = 16.dp
                            ),
                            color = if (pressedKeys.contains(134)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FilterCenterFocus,
                                    contentDescription = null,
                                    tint = if (pressedKeys.contains(134)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.focus_button),
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                    color = if (pressedKeys.contains(134)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontFamily = QuicksandTitleVariable
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}