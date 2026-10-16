package com.xenonware.mindcontrol.ui.layouts

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.xenon.mylibrary.ActivityScreen
import com.xenonware.mindcontrol.R
import com.xenonware.mindcontrol.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Lets the user choose which apps' keyboards switch to the gamepad setup.
 *
 * A screen of its own, laid out with [ActivityScreen] like the action and button-config screens.
 * While one of the selected apps is in the foreground the service reports a controller as
 * connected; everywhere else the phone is untouched.
 */
@Composable
fun GamepadAppPickerScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val scope = rememberCoroutineScope()
    val apps = remember { mutableStateListOf<AppEntry>() }
    var selected by remember { mutableStateOf(SettingsManager.getGamepadApps(context)) }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
            val list = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
                .map {
                    // Icons are decoded here, off the main thread: that can touch disk and should
                    // not happen during composition.
                    val bitmap = runCatching { it.loadIcon(pm).toBitmap() }.getOrNull()
                    AppEntry(
                        name = it.loadLabel(pm).toString(),
                        packageName = it.activityInfo.packageName,
                        icon = bitmap
                    )
                }
                .distinctBy { it.packageName }
                .sortedBy { it.name }
            withContext(Dispatchers.Main) {
                apps.clear()
                apps.addAll(list)
            }
        }
    }

    // Persisted on every toggle, so leaving the screen never loses a change.
    fun toggle(packageName: String) {
        val updated = selected.toMutableSet().apply {
            if (!add(packageName)) remove(packageName)
        }
        selected = updated
        SettingsManager.setGamepadApps(context, updated)
    }

    ActivityScreen(
        modifier = modifier,
        titleText = stringResource(R.string.gamepad_apps_title),
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
                contentPadding = paddingValues,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(apps) { app ->
                    val isSelected = app.packageName in selected
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    app.name,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            supportingContent = {
                                Text(
                                    app.packageName,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            // Checkbox first, then the launcher icon clipped into a circle.
                            leadingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { toggle(app.packageName) }
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val bitmap = app.icon
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .clickable { toggle(app.packageName) }
                                .padding(vertical = 4.dp),
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        }
    )
}

/**
 * Minimal app descriptor for the picker; avoids depending on the action-screen model.
 *
 * [icon] is nullable because a package can legitimately have no launcher icon on some OEM builds;
 * a missing icon renders as an empty circle so every row keeps the same alignment.
 */
private data class AppEntry(
    val name: String,
    val packageName: String,
    val icon: android.graphics.Bitmap?
)
