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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import com.xenon.mylibrary.res.XenonDialog
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenonware.mindcontrol.PermissionStatus
import com.xenonware.mindcontrol.R
import com.xenonware.mindcontrol.SettingsManager
import com.xenonware.mindcontrol.ShellManager
import com.xenonware.mindcontrol.ui.theme.Palette
import com.xenonware.mindcontrol.ui.theme.PaletteRow
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

// Status colors (same style as the existing cards)
private val GreenContainer = Color(0xFFE8F5E9)
private val GreenContent = Color(0xFF2E7D32)
private val YellowContainer = Color(0xFFFFF8E1)
private val YellowContent = Color(0xFF9A6B00)
private val RedContainer = Color(0xFFFFEBEE)
private val RedContent = Color(0xFFC62828)

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

private fun isShizukuInstalled(context: Context): Boolean = try {
    context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, PackageManager.PackageInfoFlags.of(0))
    true
} catch (_: Exception) {
    false
}

private fun openShizukuStorePage(context: Context) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$SHIZUKU_PACKAGE".toUri()))
    } catch (_: Exception) {
        context.startActivity(Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$SHIZUKU_PACKAGE".toUri()))
    }
}

/** Yellow = available, red = denied / not running / not installed, green = granted. */
@Composable
private fun PermissionStatusCard(
    text: String,
    status: PermissionStatus,
    onClick: (() -> Unit)?,
) {
    val (container, content) = when (status) {
        PermissionStatus.GRANTED -> GreenContainer to GreenContent
        PermissionStatus.AVAILABLE -> YellowContainer to YellowContent
        PermissionStatus.DENIED, PermissionStatus.UNAVAILABLE -> RedContainer to RedContent
    }
    Card(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = container,
            disabledContainerColor = container,
            contentColor = content,
            disabledContentColor = content
        ),
        border = BorderStroke(1.dp, content)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = text,
                color = content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
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

    // Root / Shizuku state (reactive, never blocks the UI thread, never prompts on its own)
    val rootStatus by ShellManager.rootStatus.collectAsState()
    val shizukuStatus by ShellManager.shizukuStatus.collectAsState()
    var shizukuInstalled by remember { mutableStateOf(isShizukuInstalled(context)) }
    val shellGranted =
        rootStatus == PermissionStatus.GRANTED || shizukuStatus == PermissionStatus.GRANTED

    LaunchedEffect(Unit) {
        while (true) {
            isServiceEnabled =
                accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
                    .any { it.resolveInfo.serviceInfo.packageName == context.packageName }

            isNotificationListenerEnabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                ?.contains(context.packageName) == true

            isBatteryOptimized = !powerManager.isIgnoringBatteryOptimizations(context.packageName)

            shizukuInstalled = isShizukuInstalled(context)

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
                    fontSize = 20.sp,
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
                    containerColor = if (isServiceEnabled) GreenContainer else RedContainer
                ),
                border = BorderStroke(
                    1.dp, if (isServiceEnabled) GreenContent else RedContent
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (isServiceEnabled) stringResource(R.string.accessibility_active) else stringResource(R.string.accessibility_inactive),
                        color = if (isServiceEnabled) GreenContent else RedContent,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // --- Privileged Access Status (Root / Shizuku) ---
            // Root box: only on rooted devices. Yellow = available, red = denied, green = granted.
            if (rootStatus != PermissionStatus.UNAVAILABLE) {
                PermissionStatusCard(
                    text = when (rootStatus) {
                        PermissionStatus.GRANTED -> stringResource(R.string.root_authorized)
                        PermissionStatus.DENIED -> stringResource(R.string.root_denied)
                        else -> stringResource(R.string.root_available)
                    },
                    status = rootStatus,
                    onClick = if (rootStatus != PermissionStatus.GRANTED) {
                        { ShellManager.requestRoot() }
                    } else null
                )
            }

            // Shizuku box: hidden as soon as root is granted.
            if (rootStatus != PermissionStatus.GRANTED) {
                PermissionStatusCard(
                    text = when {
                        !shizukuInstalled -> stringResource(R.string.shizuku_not_installed)
                        shizukuStatus == PermissionStatus.GRANTED -> stringResource(R.string.shizuku_authorized)
                        shizukuStatus == PermissionStatus.DENIED -> stringResource(R.string.shizuku_denied)
                        shizukuStatus == PermissionStatus.AVAILABLE -> stringResource(R.string.shizuku_available)
                        else -> stringResource(R.string.shizuku_not_running)
                    },
                    status = if (!shizukuInstalled) PermissionStatus.UNAVAILABLE else shizukuStatus,
                    onClick = when {
                        !shizukuInstalled -> {
                            { openShizukuStorePage(context) }
                        }
                        shizukuStatus != PermissionStatus.GRANTED -> {
                            { ShellManager.requestShizuku(context) }
                        }
                        else -> null
                    }
                )
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
                    colors = CardDefaults.cardColors(containerColor = RedContainer),
                    border = BorderStroke(1.dp, RedContent)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.battery_opt_on),
                            color = RedContent,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(R.string.battery_opt_desc),
                            color = RedContent.copy(alpha = 0.7f),
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
                    colors = CardDefaults.cardColors(containerColor = RedContainer),
                    border = BorderStroke(1.dp, RedContent)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.media_control_inactive),
                            color = RedContent,
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
                    colors = CardDefaults.cardColors(containerColor = RedContainer),
                    border = BorderStroke(1.dp, RedContent)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.system_settings_denied),
                            color = RedContent,
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
                    colors = CardDefaults.cardColors(containerColor = RedContainer),
                    border = BorderStroke(1.dp, RedContent)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.dnd_access_denied),
                            color = RedContent,
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
                    colors = CardDefaults.cardColors(containerColor = RedContainer),
                    border = BorderStroke(1.dp, RedContent)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.notifications_denied),
                            color = RedContent,
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

            if (!shellGranted) {
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

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, "https://www.buymeacoffee.com/xenonware".toUri())
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("TogglesContainer", "Error opening Buy Me a Coffee link", e)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_buy_me_a_coffee),
                        contentDescription = stringResource(R.string.buy_me_a_coffee),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.buy_me_a_coffee),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.buy_me_a_coffee_description),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}