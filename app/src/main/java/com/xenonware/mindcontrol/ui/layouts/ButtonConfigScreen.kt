package com.xenonware.mindcontrol.ui.layouts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.xenon.mylibrary.res.XenonDialog
import com.xenon.mylibrary.values.MediumCornerRadius
import com.xenon.mylibrary.values.SmallestCornerRadius
import com.xenonware.mindcontrol.R
import com.xenonware.mindcontrol.SettingsManager
import com.xenonware.mindcontrol.ui.res.ActionIcon
import com.xenonware.mindcontrol.ui.res.getActionDisplayName
import com.xenonware.mindcontrol.ui.res.getTypeDisplayName
import com.xenonware.mindcontrol.ui.theme.BlueTheme
import com.xenonware.mindcontrol.ui.theme.GreenTheme
import com.xenonware.mindcontrol.ui.theme.Palette
import com.xenonware.mindcontrol.ui.theme.PaletteTheme
import com.xenonware.mindcontrol.ui.theme.RedTheme
import com.xenonware.mindcontrol.ui.theme.YellowTheme

@Composable
fun ButtonConfigScreen(
    keyCode: Int,
    name: String,
    modifier: Modifier = Modifier,
    keyboardPalette: Palette,
    isFromKeyboard: Boolean = false,
    isScreenOff: Boolean,
    shellPermission: Boolean,
    onScreenOffChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onSelectAction: (Int, String, String) -> Unit,
) {
    val context = LocalContext.current
    var showDisabledDialog by rememberSaveable { mutableStateOf(false) }
    var showFocusWarningDialog by rememberSaveable { mutableStateOf(false) }

    val overrideScreenOff = remember { SettingsManager.isOverrideScreenOffEnabled(context) }
    val isVolumeButton = keyCode == 24 || keyCode == 25

    val themeWrapper: @Composable (@Composable () -> Unit) -> Unit = when {
        isFromKeyboard || keyCode == 111 -> { content -> PaletteTheme(palette = keyboardPalette) { content() } }
        keyCode == 131 -> { content -> RedTheme { content() } }
        keyCode == 133 || keyCode == 132 -> { content -> YellowTheme { content() } }
        keyCode == 24 || keyCode == 25 -> { content -> GreenTheme { content() } }
        keyCode == 27 || keyCode == 134 -> { content -> BlueTheme { content() } }
        else -> { content -> content() }
    }

    themeWrapper {
        if (showDisabledDialog) {
            XenonDialog(
                properties = DialogProperties(usePlatformDefaultWidth = true),
                onDismissRequest = { showDisabledDialog = false },
                title = stringResource(R.string.notice),
                confirmButtonText = stringResource(R.string.ok),
                onConfirmButtonClick = { showDisabledDialog = false },
                content = {
                    val message = when {
                        !overrideScreenOff -> stringResource(R.string.override_disabled_msg)
                        keyCode == 27 -> stringResource(R.string.camera_limit_msg)
                        isVolumeButton && !shellPermission -> stringResource(R.string.volume_shell_msg)
                        !shellPermission -> stringResource(R.string.non_volume_shell_msg)
                        else -> stringResource(R.string.config_unavailable_msg)
                    }
                    Text(message)
                }
            )
        }
        if (showFocusWarningDialog) {
            XenonDialog(
                properties = DialogProperties(usePlatformDefaultWidth = true),
                onDismissRequest = { showFocusWarningDialog = false },
                title = stringResource(R.string.warning),
                confirmButtonText = stringResource(R.string.i_understand),
                onConfirmButtonClick = {
                    showFocusWarningDialog = false
                    onScreenOffChange(true)
                },
                content = {
                    Text(stringResource(R.string.focus_warning_msg))
                }
            )
        }
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            Column(modifier = modifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = stringResource(R.string.button_config_title, name),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { onScreenOffChange(false) },
                        border = if (isScreenOff) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                        colors = if (!isScreenOff) ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                        else ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) { Text(stringResource(R.string.screen_on)) }

                    Button(
                        onClick = {
                            if (!overrideScreenOff) {
                                showDisabledDialog = true // Will show the "Override Off" message
                            } else if (keyCode == 27) {
                                showDisabledDialog = true // Hardware limitation
                            } else if (isVolumeButton && !shellPermission) {
                                showDisabledDialog = true // Use toggle instead
                            } else if (!isVolumeButton && !shellPermission) {
                                showDisabledDialog = true // Requires Shell access
                            } else if (keyCode == 134 && !isScreenOff) {
                                showFocusWarningDialog = true
                            } else {
                                onScreenOffChange(true)
                            }
                        },
                        border = if (!isScreenOff) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                        colors = when {
                            !overrideScreenOff || keyCode == 27 || (isVolumeButton && !shellPermission) || (!isVolumeButton && !shellPermission) -> ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                            isScreenOff -> ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                            else -> ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        }
                    ) { Text(stringResource(R.string.screen_off)) }
                }

                val stateStr = if (isScreenOff) "OFF" else "ON"

                Spacer(modifier = Modifier.height(16.dp))

                val pressTypes = if (keyCode == 132 || keyCode == 133) listOf("SINGLE_PRESS")
                else listOf("SINGLE_PRESS", "DOUBLE_PRESS", "TRIPLE_PRESS")
                val holdTypes = if (keyCode == 132 || keyCode == 133) emptyList()
                else listOf("HOLD", "PRESS_AND_HOLD")

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    pressTypes.forEachIndexed { index, type ->
                        val shape = when {
                            pressTypes.size == 1 -> RoundedCornerShape(MediumCornerRadius)
                            index == 0 -> RoundedCornerShape(
                                topStart = MediumCornerRadius,
                                topEnd = MediumCornerRadius,
                                bottomStart = SmallestCornerRadius,
                                bottomEnd = SmallestCornerRadius
                            )
                            index == pressTypes.size - 1 -> RoundedCornerShape(
                                topStart = SmallestCornerRadius,
                                topEnd = SmallestCornerRadius,
                                bottomStart = MediumCornerRadius,
                                bottomEnd = MediumCornerRadius
                            )
                            else -> RoundedCornerShape(SmallestCornerRadius)
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = shape,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            MindControlActionSelector(keyCode, stateStr, type, onSelectAction)
                        }
                    }

                    if (holdTypes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp)) // 14 + 2 (from verticalArrangement) = 16dp
                        holdTypes.forEachIndexed { index, type ->
                            val shape = when (index) {
                                0 -> RoundedCornerShape(
                                    topStart = MediumCornerRadius,
                                    topEnd = MediumCornerRadius,
                                    bottomStart = SmallestCornerRadius,
                                    bottomEnd = SmallestCornerRadius
                                )
                                holdTypes.size - 1 -> RoundedCornerShape(
                                    topStart = SmallestCornerRadius,
                                    topEnd = SmallestCornerRadius,
                                    bottomStart = MediumCornerRadius,
                                    bottomEnd = MediumCornerRadius
                                )
                                else -> RoundedCornerShape(SmallestCornerRadius)
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = shape,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                MindControlActionSelector(keyCode, stateStr, type, onSelectAction)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MindControlActionSelector(
    keyCode: Int,
    state: String,
    type: String,
    onSelectAction: (Int, String, String) -> Unit
) {
    val context = LocalContext.current
    val action = remember(keyCode, state, type) {
        SettingsManager.getAction(context, keyCode, state, type)
    }

    val shortcutLabel = stringResource(R.string.tab_shortcuts)
    val speedDialLabel = stringResource(R.string.speed_dial)
    val urlLabel = stringResource(R.string.url)
    val qrCodeLabel = stringResource(R.string.qr_code)
    val actionDisplayName = getActionDisplayName(action)

    val displayAction = remember(action, actionDisplayName, shortcutLabel, speedDialLabel, urlLabel, qrCodeLabel) {
        if (action.startsWith(SettingsManager.PREFIX_APP)) {
            val pkg = action.removePrefix(SettingsManager.PREFIX_APP)
            try {
                val pm = context.packageManager
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            } catch (_: Exception) {
                pkg
            }
        } else if (action.startsWith(SettingsManager.PREFIX_SHORTCUT)) {
            val parts = action.removePrefix(SettingsManager.PREFIX_SHORTCUT).split("||")
            parts.getOrNull(1) ?: shortcutLabel
        } else if (action.startsWith(SettingsManager.PREFIX_SPEED_DIAL)) {
            "$speedDialLabel: " + action.removePrefix(SettingsManager.PREFIX_SPEED_DIAL)
        } else if (action.startsWith(SettingsManager.PREFIX_URL)) {
            "$urlLabel: " + action.removePrefix(SettingsManager.PREFIX_URL)
        } else if (action.startsWith(SettingsManager.PREFIX_QR_CODE)) {
            "$qrCodeLabel: " + action.removePrefix(SettingsManager.PREFIX_QR_CODE)
        } else {
            actionDisplayName
        }
    }

    val displayType = getTypeDisplayName(type)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "$displayType: ",
            modifier = Modifier.width(165.dp),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start
        )
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedButton(
            onClick = { onSelectAction(keyCode, state, type) },
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = displayAction,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                ActionIcon(
                    action = action,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(start = 4.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}