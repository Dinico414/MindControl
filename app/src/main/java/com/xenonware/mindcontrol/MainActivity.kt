package com.xenonware.mindcontrol

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xenonware.mindcontrol.ui.theme.MindControlTheme
import androidx.core.net.toUri
import rikka.shizuku.Shizuku
import android.content.pm.PackageManager
import android.util.Log

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MindControlTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MindControlMainScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MindControlMainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    var isServiceEnabled by remember {
        mutableStateOf(accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
            .any { it.resolveInfo.serviceInfo.packageName == context.packageName })
    }

    var disableInCamera by remember { mutableStateOf(SettingsManager.isDisableInCamera(context)) }
    var defaultWhenVolumeVisible by remember { mutableStateOf(SettingsManager.isDefaultWhenVolumeVisible(context)) }

    var shizukuPermission by remember { mutableStateOf(false) }
    var shizukuAvailable by remember { mutableStateOf(false) }

    // Refresh status when returning to app
    LaunchedEffect(Unit) {
        while(true) {
            isServiceEnabled = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
                .any { it.resolveInfo.serviceInfo.packageName == context.packageName }
            
            shizukuAvailable = try { Shizuku.pingBinder() } catch (e: Exception) { false }
            if (shizukuAvailable) {
                shizukuPermission = try { 
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED 
                } catch (e: Exception) { false }
            }
            
            kotlinx.coroutines.delay(2000)
        }
    }

    Column(modifier = modifier.padding(16.dp).verticalScroll(scrollState)) {
        Text(text = "MindControl Settings", style = MaterialTheme.typography.headlineMedium)

        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isServiceEnabled) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isServiceEnabled) "Accessibility: ACTIVE" else "Accessibility: INACTIVE",
                    color = if (isServiceEnabled) Color(0xFF2E7D32) else Color(0xFFC62828),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (shizukuAvailable && shizukuPermission) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (shizukuAvailable) {
                        if (shizukuPermission) "Shizuku: AUTHORIZED" else "Shizuku: UNAUTHORIZED"
                    } else "Shizuku: NOT RUNNING",
                    color = if (shizukuAvailable && shizukuPermission) Color(0xFF2E7D32) else Color(0xFFC62828),
                    style = MaterialTheme.typography.titleMedium
                )
                if (shizukuAvailable && !shizukuPermission) {
                    Button(onClick = { 
                        try { Shizuku.requestPermission(0) } catch (e: Exception) {
                            Log.e("MainActivity", "Shizuku request error", e)
                        }
                    }) {
                        Text("Authorize Shizuku")
                    }
                }
                if (!shizukuAvailable) {
                    Text("Shizuku allows capturing keys while the screen is off (Android 15+).", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Disable mapping when camera used", style = MaterialTheme.typography.titleMedium)
                Text("Allows shutter buttons to work normally in camera apps", style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = disableInCamera,
                onCheckedChange = {
                    disableInCamera = it
                    SettingsManager.setDisableInCamera(context, it)
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Default behavior if volume slider is visible", style = MaterialTheme.typography.titleMedium)
                Text("Volume buttons act normally while the volume panel is shown", style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = defaultWhenVolumeVisible,
                onCheckedChange = {
                    defaultWhenVolumeVisible = it
                    SettingsManager.setDefaultWhenVolumeVisible(context, it)
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Open Accessibility Settings")
        }

        if (!Settings.System.canWrite(context)) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                    intent.data = ("package:" + context.packageName).toUri()
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Allow System Settings (for Brightness)")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val buttons = listOf(
            131 to "AI Button",
            133 to "Camera Up",
            132 to "Camera Down",
            24 to "Volume Up",
            25 to "Volume Down",
            27 to "Camera Button",
            134 to "Focus Button",
        )

        buttons.forEach { (code, name) ->
            MindControlButtonConfig(code, name)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
fun MindControlButtonConfig(keyCode: Int, name: String) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(text = name, style = MaterialTheme.typography.titleLarge)
        Text(text = "Key Code: $keyCode", style = MaterialTheme.typography.bodySmall)

        Button(onClick = { expanded = !expanded }) {
            Text(if (expanded) "Hide Configuration" else "Configure Actions")
        }

        if (expanded) {
            MindControlStateConfig(keyCode, "ON", "Screen On")
            
            // Camera (27) and Focus (134) buttons do not have Screen Off configuration
            if (keyCode != 27 && keyCode != 134) {
                Spacer(modifier = Modifier.height(8.dp))
                MindControlStateConfig(keyCode, "OFF", "Screen Off")
            }
        }
    }
}

@Composable
fun MindControlStateConfig(keyCode: Int, state: String, label: String) {
    Column(modifier = Modifier.padding(start = 16.dp)) {
        Text(text = label, style = MaterialTheme.typography.titleMedium)
        
        val types = if (keyCode == 132 || keyCode == 133) {
            listOf("SINGLE")
        } else {
            listOf("SINGLE", "DOUBLE", "TRIPLE", "LONG")
        }
        
        types.forEach { type ->
            MindControlActionSelector(keyCode, state, type)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindControlActionSelector(keyCode: Int, state: String, type: String) {
    val context = LocalContext.current
    val currentAction = remember { mutableStateOf(SettingsManager.getAction(context, keyCode, state, type)) }

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

    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = "$type: ", modifier = Modifier.weight(1f))
        Box {
            OutlinedButton(onClick = { showMenu = true }) {
                Text(currentAction.value)
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                actions.forEach { action ->
                    DropdownMenuItem(
                        text = { Text(action) },
                        onClick = {
                            SettingsManager.setAction(context, keyCode, state, type, action)
                            currentAction.value = action
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}
