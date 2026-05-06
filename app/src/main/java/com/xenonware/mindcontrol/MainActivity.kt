package com.xenonware.mindcontrol

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import android.view.Surface
import android.view.WindowManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenonware.mindcontrol.ui.theme.MindControlTheme
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            MindControlTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    MindControlMainScreen()
                }
            }
        }
    }
}

@Composable
fun MindControlMainScreen(modifier: Modifier = Modifier) {
    var selectedButton by remember { mutableStateOf<Pair<Int, String>?>(null) }

    if (selectedButton != null) {
        ButtonConfigScreen(
            keyCode = selectedButton!!.first,
            name = selectedButton!!.second,
            modifier = modifier,
            onBack = { selectedButton = null })
    } else {
        GridScreen(
            modifier = modifier,
            onButtonSelected = { code, name -> selectedButton = Pair(code, name) })
    }
}

@Composable
fun GridScreen(modifier: Modifier = Modifier, onButtonSelected: (Int, String) -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        // Top Part (Weight 2f) -> represents 2 rows out of 6
        Row(
            modifier = Modifier
                .weight(2f)
                .fillMaxWidth()
        ) {
            // AI Button (Weight 1f out of 2 -> 2 columns out of 4)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(end = 4.dp, bottom = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { onButtonSelected(131, "AI Button") },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AI Button",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    fontFamily = QuicksandTitleVariable,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(8.dp)
                )
            }

            // Right Top Column (Weight 1f out of 2 -> 2 columns out of 4)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Camera Up
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(start = 4.dp, bottom = 1.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { onButtonSelected(133, "Camera Up") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Camera Up",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(8.dp),
                        fontFamily = QuicksandTitleVariable
                    )
                }
                // Camera Down
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 1.dp, bottom = 4.dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { onButtonSelected(132, "Camera Down") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Camera Down",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(8.dp),
                        fontFamily = QuicksandTitleVariable
                    )
                }
            }
        }

        // Bottom Part (Weight 4f) -> represents 4 rows out of 6
        Row(
            modifier = Modifier
                .weight(4f)
                .fillMaxWidth()
        ) {
            // Toggles Container (Weight 1f out of 2 -> 2 columns out of 4)
            TogglesContainer(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )

            // Right Bottom Column (Weight 1f out of 2 -> 2 columns out of 4)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Volume Up (Weight 1f out of 4 -> 1 row out of 4)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 4.dp, bottom = 1.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { onButtonSelected(24, "Volume Up") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Volume Up",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(8.dp),
                        fontFamily = QuicksandTitleVariable
                    )
                }
                // Volume Down (Weight 1f out of 4 -> 1 row out of 4)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 1.dp, bottom = 4.dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { onButtonSelected(25, "Volume Down") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Volume Down",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(8.dp),
                        fontFamily = QuicksandTitleVariable
                    )
                }
                // Row for Camera and Focus (Weight 2f out of 4 -> 2 rows out of 4)
                Row(
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxWidth()
                ) {
                    // Camera Button (Weight 1f out of 2 -> 1 column out of 2)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(start = 4.dp, end = 1.dp, top = 4.dp)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 4.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { onButtonSelected(27, "Camera Button") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Camera\nButton",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(8.dp),
                            fontFamily = QuicksandTitleVariable
                        )
                    }
                    // Focus Button (Weight 1f out of 2 -> 1 column out of 2)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(start = 1.dp, top = 4.dp)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { onButtonSelected(134, "Focus Button") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Focus\nButton",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(8.dp),
                            fontFamily = QuicksandTitleVariable
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TogglesContainer(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val accessibilityManager =
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    var isServiceEnabled by remember {
        mutableStateOf(
            accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
                .any { it.resolveInfo.serviceInfo.packageName == context.packageName })
    }

    var disableInCamera by remember { mutableStateOf(SettingsManager.isDisableInCamera(context)) }
    var defaultWhenVolumeVisible by remember {
        mutableStateOf(
            SettingsManager.isDefaultWhenVolumeVisible(
                context
            )
        )
    }

    var shizukuPermission by remember { mutableStateOf(false) }
    var shizukuAvailable by remember { mutableStateOf(false) }

    // Refresh status when returning to app
    LaunchedEffect(Unit) {
        while (true) {
            isServiceEnabled =
                accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
                    .any { it.resolveInfo.serviceInfo.packageName == context.packageName }

            shizukuAvailable = try {
                Shizuku.pingBinder()
            } catch (e: Exception) {
                false
            }
            if (shizukuAvailable) {
                shizukuPermission = try {
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                } catch (e: Exception) {
                    false
                }
            }

            kotlinx.coroutines.delay(2000)
        }
    }

    Card(
        modifier = modifier.padding(top = 4.dp, end = 4.dp).clip(RoundedCornerShape(16.dp)), colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = "Settings",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                style = MaterialTheme.typography.titleLarge,
                fontFamily = QuicksandTitleVariable
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                ,
                colors = CardDefaults.cardColors(
                    containerColor = if (isServiceEnabled) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (isServiceEnabled) "Accessibility: ACTIVE" else "Accessibility: INACTIVE",
                        color = if (isServiceEnabled) Color(0xFF2E7D32) else Color(0xFFC62828),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (shizukuAvailable && shizukuPermission) Color(0xFFE8F5E9) else Color(
                        0xFFFFEBEE
                    )
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (shizukuAvailable) {
                            if (shizukuPermission) "Shizuku: AUTHORIZED" else "Shizuku: UNAUTHORIZED"
                        } else "Shizuku: NOT RUNNING",
                        color = if (shizukuAvailable && shizukuPermission) Color(0xFF2E7D32) else Color(
                            0xFFC62828
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (shizukuAvailable && !shizukuPermission) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(onClick = {
                            try {
                                Shizuku.requestPermission(0)
                            } catch (e: Exception) {
                                Log.e("MainActivity", "Shizuku request error", e)
                            }
                        }, contentPadding = PaddingValues(4.dp)) {
                            Text("Authorize", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    "Disable in camera",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = disableInCamera, onCheckedChange = {
                        disableInCamera = it
                        SettingsManager.setDisableInCamera(context, it)
                    })
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    "Default volume if slider visible",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = defaultWhenVolumeVisible, onCheckedChange = {
                        defaultWhenVolumeVisible = it
                        SettingsManager.setDefaultWhenVolumeVisible(context, it)
                    })
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    context.startActivity(intent)
                }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(8.dp)
            ) {
                Text("Accessibility Settings", style = MaterialTheme.typography.bodySmall)
            }

            if (!Settings.System.canWrite(context)) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                        intent.data = ("package:" + context.packageName).toUri()
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Text("Allow Sys Settings", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun ButtonConfigScreen(
    keyCode: Int,
    name: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    var isScreenOff by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(text = "$name Configuration", style = MaterialTheme.typography.titleLarge, color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (keyCode != 27 && keyCode != 134) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                if (!isScreenOff) {
                    FilledTonalButton(onClick = { isScreenOff = false }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)) {
                        Text("Screen On")
                    }
                } else {
                    FilledTonalButton(onClick = { isScreenOff = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onPrimary)) {
                        Text("Screen On", color = Color.White)
                    }
                }

                if (isScreenOff) {
                    FilledTonalButton(onClick = { isScreenOff = true }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)) {
                        Text("Screen Off")
                    }
                } else {
                    FilledTonalButton(onClick = { isScreenOff = true }, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onPrimary)) {
                        Text("Screen Off", color = Color.White)
                    }
                }
            }
        } else {
            Text(
                "Screen On only",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(8.dp)
            )
        }

        val stateStr = if (isScreenOff) "OFF" else "ON"

        Spacer(modifier = Modifier.height(16.dp))

        val types = if (keyCode == 132 || keyCode == 133) {
            listOf("SINGLE")
        } else {
            listOf("SINGLE", "DOUBLE", "TRIPLE", "LONG")
        }

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            types.forEach { type ->
                MindControlActionSelector(keyCode, stateStr, type)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindControlActionSelector(keyCode: Int, state: String, type: String) {
    val context = LocalContext.current
    val currentAction =
        remember(keyCode, state, type) { mutableStateOf(SettingsManager.getAction(context, keyCode, state, type)) }

    val actions = listOf(
        SettingsManager.ACTION_DEFAULT,
        SettingsManager.ACTION_NONE,
        SettingsManager.ACTION_PLAY_PAUSE,
        SettingsManager.ACTION_NEXT,
        SettingsManager.ACTION_PREVIOUS,
        SettingsManager.ACTION_VOLUME_UP,
        SettingsManager.ACTION_VOLUME_DOWN,
        SettingsManager.ACTION_FLASHLIGHT,
        SettingsManager.ACTION_SCREENSHOT,
        SettingsManager.ACTION_LOCK,
        SettingsManager.ACTION_HOME,
        SettingsManager.ACTION_BACK,
        SettingsManager.ACTION_RECENTS,
        SettingsManager.ACTION_NOTIFICATIONS,
        SettingsManager.ACTION_QUICK_SETTINGS,
        SettingsManager.ACTION_ASSISTANT,
        SettingsManager.ACTION_BRIGHTNESS_UP,
        SettingsManager.ACTION_BRIGHTNESS_DOWN,
        SettingsManager.ACTION_ROTATE_TOGGLE,
        SettingsManager.ACTION_SCROLL_UP,
        SettingsManager.ACTION_SCROLL_DOWN
    )

    var showMenu by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(text = "$type: ", modifier = Modifier.weight(1f), color = Color.White)
        Box {
            OutlinedButton(onClick = { showMenu = true }) {
                Text(currentAction.value, color = Color.White)
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                actions.forEach { action ->
                    DropdownMenuItem(text = { Text(action) }, onClick = {
                        SettingsManager.setAction(context, keyCode, state, type, action)
                        currentAction.value = action
                        showMenu = false
                    })
                }
            }
        }
    }
}
