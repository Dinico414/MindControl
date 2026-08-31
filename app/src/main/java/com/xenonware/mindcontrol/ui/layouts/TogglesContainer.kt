package com.xenonware.mindcontrol.ui.layouts

import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import com.xenon.mylibrary.res.XenonDialog
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenonware.mindcontrol.R
import com.xenonware.mindcontrol.SettingsManager
import com.xenonware.mindcontrol.ShellManager
import com.xenonware.mindcontrol.ui.theme.Palette
import com.xenonware.mindcontrol.ui.theme.PaletteRow
import kotlinx.coroutines.delay
import rikka.shizuku.Shizuku
import kotlin.time.Duration.Companion.milliseconds

fun openAppInfo(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e("TogglesContainer", "Error opening app info", e)
    }
}

@SuppressLint("BatteryLife")
@Composable
fun TogglesContainer(
    modifier: Modifier = Modifier,
    hasKeyboard: Boolean = false,
    devicePalette: Palette,
    keyboardPalette: Palette,
    onDevicePaletteChange: (Palette) -> Unit,
    onKeyboardPaletteChange: (Palette) -> Unit,
) {
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
    var overrideScreenOff by remember {
        mutableStateOf(
            SettingsManager.isOverrideScreenOffEnabled(
                context
            )
        )
    }
    var volumeLongPressSkip by remember {
        mutableStateOf(
            SettingsManager.isVolumeLongPressSkipEnabled(
                context
            )
        )
    }

    var isNotificationListenerEnabled by remember { mutableStateOf(false) }
    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
    var isBatteryOptimized by remember {
        mutableStateOf(!powerManager.isIgnoringBatteryOptimizations(context.packageName))
    }
    var showAccessibilityDisclosure by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            isServiceEnabled =
                accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
                    .any { it.resolveInfo.serviceInfo.packageName == context.packageName }

            isNotificationListenerEnabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                ?.contains(context.packageName) == true
            
            isBatteryOptimized = !powerManager.isIgnoringBatteryOptimizations(context.packageName)

            delay(2000.milliseconds)
        }
    }

    Card(
        modifier = modifier
            .padding(
                top = 4.dp, end = 4.dp, bottom = if (hasKeyboard) 4.dp else 0.dp
            )
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
                .verticalScroll(scrollState)
        ) {
            val versionName = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } catch (_: Exception) {
                "Unknown"
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(0.5f),
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = QuicksandTitleVariable
                )
                Text(
                    text = stringResource(R.string.version_prefix) + versionName,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(0.25f),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = QuicksandTitleVariable
                )
            }

            // --- Accessibility Status (Always Visible) ---
            Card(
                onClick = {
                    if (!isServiceEnabled) showAccessibilityDisclosure = true
                    else {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isServiceEnabled) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ),
                border = BorderStroke(
                    1.dp, if (isServiceEnabled) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (isServiceEnabled) stringResource(R.string.accessibility_active) else stringResource(R.string.accessibility_inactive),
                        color = if (isServiceEnabled) Color(0xFF2E7D32) else Color(0xFFC62828),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // --- Privileged Access Status (Shizuku or Root) ---
            val isRooted = ShellManager.isDeviceRooted()
            val shizukuInstalled = try {
                context.packageManager.getPackageInfo("moe.shizuku.privileged.api", PackageManager.PackageInfoFlags.of(0))
                true
            } catch (_: Exception) {
                false
            }

            val shizukuAvailable = ShellManager.isShizukuAvailable()
            val rootAvailable = ShellManager.isRootAvailable()

            if (rootAvailable) {
                // Root is available, show ONLY the green root box.
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    border = BorderStroke(1.dp, Color(0xFF2E7D32))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.root_authorized),
                            color = Color(0xFF2E7D32),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else if (isRooted || shizukuInstalled) {
                // Root box (red) if rooted
                if (isRooted) {
                    Card(
                        onClick = {
                            Thread { ShellManager.isRootAvailable() }.start()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        border = BorderStroke(1.dp, Color(0xFFC62828))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(R.string.root_unauthorized),
                                color = Color(0xFFC62828),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Shizuku box
                Card(
                    onClick = {
                        if (shizukuInstalled) {
                            if (!shizukuAvailable) {
                                val intent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                                if (intent != null) context.startActivity(intent)
                            } else {
                                try {
                                    Shizuku.requestPermission(0)
                                } catch (e: Exception) {
                                    Log.e("TogglesContainer", "Shizuku request error", e)
                                }
                            }
                        } else {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=moe.shizuku.privileged.api".toUri()))
                            } catch (_: Exception) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api".toUri()))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (shizukuAvailable) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    ),
                    border = BorderStroke(
                        1.dp, if (shizukuAvailable) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        val statusText = when {
                            shizukuAvailable -> stringResource(R.string.shizuku_authorized)
                            shizukuInstalled -> stringResource(R.string.shizuku_unauthorized)
                            else -> stringResource(R.string.shizuku_not_installed)
                        }
                        Text(
                            text = statusText,
                            color = if (shizukuAvailable) Color(0xFF2E7D32) else Color(0xFFC62828),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // --- Battery Optimization Status ---
            if (isBatteryOptimized) {
                Card(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = "package:${context.packageName}".toUri()
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("TogglesContainer", "Error opening battery optimization settings", e)
                            // Fallback to general settings if package-specific fails
                            val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            context.startActivity(fallbackIntent)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    border = BorderStroke(1.dp, Color(0xFFC62828))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.battery_opt_on),
                            color = Color(0xFFC62828),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(R.string.battery_opt_desc),
                            color = Color(0xFFC62828).copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            // --- Media Control / Notification Listener (Hide if Active) ---
            if (!isNotificationListenerEnabled) {
                Card(
                    onClick = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    border = BorderStroke(1.dp, Color(0xFFC62828))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.media_control_inactive),
                            color = Color(0xFFC62828),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // --- System Settings (Hide if Active) ---
            if (!Settings.System.canWrite(context)) {
                Card(
                    onClick = {
                        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                        intent.data = "package:${context.packageName}".toUri()
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    border = BorderStroke(1.dp, Color(0xFFC62828))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.system_settings_denied),
                            color = Color(0xFFC62828),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // --- Notification Policy / DND (Hide if Active) ---
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                Card(
                    onClick = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    border = BorderStroke(1.dp, Color(0xFFC62828))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.dnd_access_denied),
                            color = Color(0xFFC62828),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // --- POST_NOTIFICATIONS (Hide if Active) ---
            if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Card(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    border = BorderStroke(1.dp, Color(0xFFC62828))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.notifications_denied),
                            color = Color(0xFFC62828),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (showAccessibilityDisclosure) {
                XenonDialog(
                    properties = DialogProperties(usePlatformDefaultWidth = true),
                    onDismissRequest = { showAccessibilityDisclosure = false },
                    title = stringResource(R.string.accessibility_disclosure_title),
                    confirmButtonText = stringResource(R.string.grant_permission),
                    onConfirmButtonClick = {
                        showAccessibilityDisclosure = false
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    },
                    content = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                stringResource(R.string.accessibility_disclosure_content),
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Surface(
                                onClick = { openAppInfo(context) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                shape = RoundedCornerShape(20.dp),
                                color = Color.Transparent,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.Info,
                                        contentDescription = "Accessibility Guide",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.accessibility_guide),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    stringResource(R.string.disable_in_camera),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = disableInCamera, onCheckedChange = {
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
                    stringResource(R.string.default_volume_slider),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = defaultWhenVolumeVisible, onCheckedChange = {
                    defaultWhenVolumeVisible = it
                    SettingsManager.setDefaultWhenVolumeVisible(context, it)
                })
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    stringResource(R.string.override_screen_off),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = overrideScreenOff, onCheckedChange = {
                    overrideScreenOff = it
                    SettingsManager.setOverrideScreenOffEnabled(context, it)
                })
            }

            if (!ShellManager.isAvailable()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    Text(
                        stringResource(R.string.volume_skip_screen_off),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = volumeLongPressSkip, onCheckedChange = {
                        volumeLongPressSkip = it
                        SettingsManager.setVolumeLongPressSkipEnabled(context, it)
                    })
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            PaletteRow(
                label = stringResource(R.string.device_color),
                selected = devicePalette,
                onSelect = onDevicePaletteChange,
                options = listOf(Palette.Black, Palette.White, Palette.Pink, Palette.Blue),
            )

            Spacer(modifier = Modifier.height(8.dp))

            PaletteRow(
                label = stringResource(R.string.keyboard_color),
                selected = keyboardPalette,
                onSelect = onKeyboardPaletteChange,
                options = Palette.entries.toList(),
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}