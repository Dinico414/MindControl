package com.xenonware.mindcontrol.ui.layouts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xenon.mylibrary.ActivityScreen
import com.xenonware.mindcontrol.R
import com.xenonware.mindcontrol.SettingsManager

/**
 * Assigns a single controller action to one keyboard key, for gamepad mode.
 *
 * There are no press types here: a gamepad button maps to exactly one action, so choosing a row
 * replaces any previous binding. "None" clears it, which makes the key do nothing and appear
 * dimmed on the keyboard.
 */
@Composable
fun GamepadButtonPickerScreen(
    keyCode: Int,
    name: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    // Bindings are per profile, so this screen edits the profile that is currently in force.
    val profile = remember { SettingsManager.getActiveGamepadProfile(context) }
    var selected by remember {
        mutableStateOf(SettingsManager.getGamepadBinding(context, profile, keyCode))
    }

    val displayName = name.ifBlank { keyCode.toString() }

    ActivityScreen(
        modifier = modifier,
        titleText = displayName,
        onNavigationIconClick = onBack,
        navigationIcon = {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back)
            )
        },
        expandable = false,
        contentModifier = Modifier.fillMaxSize(),
        content = { paddingValues ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 16.dp,
                    bottom = 16.dp + paddingValues.calculateBottomPadding(),
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.gamepad_pick_action),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // "None" clears the binding: the key then does nothing and is drawn dimmed.
                item {
                    GamepadActionRow(
                        label = stringResource(R.string.action_none),
                        isSelected = selected == null,
                        iconRes = R.drawable.remove_selection_24px
                    ) {
                        selected = null
                        SettingsManager.setGamepadBinding(context, profile, keyCode, null)
                    }
                }

                // One section per physical area of the controller, so related controls sit
                // together instead of in a single long list.
                SettingsManager.GAMEPAD_GROUPS.forEach { group ->
                    if (group.actions.isEmpty()) return@forEach

                    item(key = "header-${group.titleRes}") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = stringResource(group.titleRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(group.actions.size) { index ->
                        val action = group.actions[index]
                        GamepadActionRow(
                            label = action.label,
                            iconRes = action.iconRes,
                            // Value comparison, so buttons and stick directions each match once.
                            isSelected = selected == action
                        ) {
                            selected = action
                            SettingsManager.setGamepadBinding(context, profile, keyCode, action)
                        }
                    }
                }
            }
        }
    )
}

/** One selectable gamepad action. The tick marks the current binding. */
@Composable
private fun GamepadActionRow(
    label: String,
    isSelected: Boolean,
    @androidx.annotation.DrawableRes iconRes: Int? = null,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier
            .fillMaxWidth()
            .clip(if (isSelected) RoundedCornerShape(50.dp) else RoundedCornerShape(4.dp))
    ) {
        ListItem(
            headlineContent = { Text(label, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface) },
            leadingContent = {
                val tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                // Each row shows the glyph of the control it maps to; the generic arrow is the
                // fallback for the entry that has no artwork ("None").
                if (iconRes != null) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardDoubleArrowRight,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            modifier = Modifier
                .clickable { onClick() }
                .padding(vertical = 4.dp),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}
