package com.xenonware.mindcontrol.ui.layouts

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import com.xenon.mylibrary.ActivityScreen
import com.xenon.mylibrary.res.XenonDialog
import com.xenonware.mindcontrol.R
import com.xenonware.mindcontrol.SettingsManager
import com.xenonware.mindcontrol.ShellManager
import com.xenonware.mindcontrol.ui.res.ActionIcon
import com.xenonware.mindcontrol.ui.res.AodStylePickerDialog
import com.xenonware.mindcontrol.ui.res.LiftToWakeWarningDialog
import com.xenonware.mindcontrol.ui.res.disabledReasonFor
import com.xenonware.mindcontrol.ui.res.getActionDisplayName
import com.xenonware.mindcontrol.ui.res.getTypeDisplayName
import com.xenonware.mindcontrol.ui.res.horizontalFadingEdges
import com.xenonware.mindcontrol.ui.res.isActionDisabled
import com.xenonware.mindcontrol.ui.res.topFadingEdge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

data class ActionConfig(val keyCode: Int, val state: String, val type: String)

val ActionConfigSaver = listSaver<ActionConfig?, Any>(
    save = { if (it == null) emptyList() else listOf(it.keyCode, it.state, it.type) },
    restore = { if (it.isEmpty()) null else ActionConfig(it[0] as Int, it[1] as String, it[2] as String) }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionSelectionScreen(
    config: ActionConfig,
    onBack: () -> Unit,
    onActionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = listOf(
        stringResource(R.string.tab_actions),
        stringResource(R.string.tab_apps),
        stringResource(R.string.tab_shortcuts),
        stringResource(R.string.tab_system),
        stringResource(R.string.tab_media),
        stringResource(R.string.tab_display)
    )
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()
    val isScreenOff = config.state == "OFF"

    var shellReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            shellReady = ShellManager.isAvailable()
            kotlinx.coroutines.delay(2000.milliseconds)
        }
    }

    val displayType = getTypeDisplayName(config.type)

    ActivityScreen(
        modifier = modifier,
        titleText = stringResource(R.string.select_action_title, displayType),
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
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.height(16.dp))

                PrimaryScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    edgePadding = 32.dp,
                    indicator = {},
                    divider = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalFadingEdges(28.dp)
                ) {
                    tabs.forEachIndexed { index, title ->
                        val selected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(30.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                )
                                .then(
                                    if (!selected) Modifier.border(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(30.dp)
                                    ) else Modifier
                                )
                                .clickable {
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .topFadingEdge(16.dp)
                ) { page ->
                    val contentPadding = PaddingValues(
                        top = 16.dp,
                        bottom = 16.dp + paddingValues.calculateBottomPadding(),
                        start = 16.dp,
                        end = 16.dp
                    )
                    when (page) {
                        0 -> ActionsTab(config, onActionSelected, shellReady, contentPadding)
                        1 -> AppsTab(config, onActionSelected, isScreenOff, contentPadding)
                        2 -> ShortcutsTab(config, onActionSelected, isScreenOff, contentPadding)
                        3 -> SystemTab(config, onActionSelected, shellReady, contentPadding)
                        4 -> MediaTab(config, onActionSelected, shellReady, contentPadding)
                        5 -> DisplayTab(config, onActionSelected, shellReady, contentPadding)
                    }
                }
            }
        }
    )
}

@Composable
fun ActionsTab(
    config: ActionConfig,
    onActionSelected: (String) -> Unit,
    shellReady: Boolean,
    contentPadding: PaddingValues
) {
    val actions = listOf(
        SettingsManager.ACTION_NONE,
        SettingsManager.ACTION_DEFAULT,
        SettingsManager.ACTION_HOME,
        SettingsManager.ACTION_BACK,
        SettingsManager.ACTION_RECENTS,
        SettingsManager.ACTION_SHOW_MENU,
        SettingsManager.ACTION_LOCK,
        SettingsManager.ACTION_AOD,
        SettingsManager.ACTION_FLASHLIGHT,
        SettingsManager.ACTION_SCREENSHOT,
        SettingsManager.ACTION_QUICK_SETTINGS,
        SettingsManager.ACTION_LAST_APP,
        SettingsManager.ACTION_APP_INFO,
        SettingsManager.ACTION_POWER_DIALOG,
        SettingsManager.ACTION_GOOGLE_SEARCH,
        SettingsManager.ACTION_ASSISTANT,
        SettingsManager.ACTION_SCROLL_UP,
        SettingsManager.ACTION_SCROLL_DOWN,
        SettingsManager.ACTION_SCROLL_UP_SMOOTH,
        SettingsManager.ACTION_SCROLL_DOWN_SMOOTH,
        SettingsManager.ACTION_SCROLL_UP_SMOOTH_FAST,
        SettingsManager.ACTION_SCROLL_DOWN_SMOOTH_FAST,
        SettingsManager.ACTION_COPY,
        SettingsManager.ACTION_CUT,
        SettingsManager.ACTION_PASTE,
        SettingsManager.ACTION_SPEED_DIAL,
        SettingsManager.ACTION_URL,
        SettingsManager.ACTION_QR_CODE,
    )
    ActionList(actions, config, onActionSelected, shellReady, contentPadding)
}

@Composable
fun AppsTab(
    config: ActionConfig,
    onActionSelected: (String) -> Unit,
    isScreenOff: Boolean,
    contentPadding: PaddingValues
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val scope = rememberCoroutineScope()
    val apps = remember { mutableStateListOf<AppItem>() }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
            val appList = resolveInfos.map {
                AppItem(
                    it.loadLabel(pm).toString(),
                    it.activityInfo.packageName,
                    it.loadIcon(pm)
                )
            }.distinctBy { it.packageName }.sortedBy { it.name }
            withContext(Dispatchers.Main) {
                apps.clear()
                apps.addAll(appList)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (isScreenOff) {
            item {
                Text(
                    stringResource(R.string.wake_screen_apps_msg),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
        items(apps.size) { index ->
            val app = apps[index]
            val shape = when {
                apps.size == 1 -> RoundedCornerShape(30.dp)
                index == 0 -> RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                index == apps.size - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 30.dp, bottomEnd = 30.dp)
                else -> RoundedCornerShape(4.dp)
            }
            Surface(
                color = if (isScreenOff)
                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = shape,
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    headlineContent = {
                        Text(
                            app.name,
                            color = if (isScreenOff)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    supportingContent = {
                        Text(
                            app.packageName,
                            color = if (isScreenOff)
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    leadingContent = {
                        val bitmap = remember(app.packageName) { app.icon.toBitmap().asImageBitmap() }
                        Image(
                            bitmap,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .then(if (isScreenOff) Modifier.alpha(0.38f) else Modifier)
                        )
                    },
                    modifier = Modifier
                        .clickable(enabled = !isScreenOff) {
                            SettingsManager.setAction(
                                context,
                                config.keyCode,
                                config.state,
                                config.type,
                                SettingsManager.PREFIX_APP + app.packageName
                            )
                            onActionSelected(app.name)
                        }
                        .padding(vertical = 4.dp),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
    }
}

data class AppItem(val name: String, val packageName: String, val icon: Drawable)

data class ShortcutItem(val name: String, val packageName: String, val className: String, val icon: Drawable)

@Composable
fun ShortcutsTab(
    config: ActionConfig,
    onActionSelected: (String) -> Unit,
    isScreenOff: Boolean,
    contentPadding: PaddingValues
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val scope = rememberCoroutineScope()
    val shortcutItems = remember { mutableStateListOf<ShortcutItem>() }

    @Suppress("DEPRECATION")
    val shortcutLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data ?: return@rememberLauncherForActivityResult

            // Log for debugging
            Log.d("ShortcutsTab", "Shortcut picker returned data: ${data.extras?.keySet()}")

            val shortcutIntent =
                data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT, Intent::class.java)

            val name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME) ?: "Shortcut"
            val uri = shortcutIntent?.toUri(Intent.URI_INTENT_SCHEME)

            if (uri != null) {
                Log.d("ShortcutsTab", "Saving shortcut: $name -> $uri")
                SettingsManager.setAction(
                    context,
                    config.keyCode,
                    config.state,
                    config.type,
                    SettingsManager.PREFIX_SHORTCUT + uri + "||" + name
                )
                onActionSelected(name)
            } else {
                Log.e("ShortcutsTab", "Failed to extract shortcut intent from result")
            }
        }
    }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            val intent = Intent(Intent.ACTION_CREATE_SHORTCUT)
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            val itemList = resolveInfos.map {
                ShortcutItem(
                    it.activityInfo.loadLabel(pm).toString().ifEmpty { it.loadLabel(pm).toString() },
                    it.activityInfo.packageName,
                    it.activityInfo.name,
                    it.loadIcon(pm)
                )
            }.sortedBy { it.name }
            withContext(Dispatchers.Main) {
                shortcutItems.clear()
                shortcutItems.addAll(itemList)
            }
        }
    }

    if (shortcutItems.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_shortcuts_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (isScreenOff) {
                item {
                    Text(
                        stringResource(R.string.wake_screen_shortcuts_msg),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            items(shortcutItems.size) { index ->
                val item = shortcutItems[index]
                val shape = when {
                    shortcutItems.size == 1 -> RoundedCornerShape(30.dp)
                    index == 0 -> RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                    index == shortcutItems.size - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 30.dp, bottomEnd = 30.dp)
                    else -> RoundedCornerShape(4.dp)
                }
                Surface(
                    color = if (isScreenOff)
                        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = shape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                item.name,
                                color = if (isScreenOff)
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        supportingContent = {
                            Text(
                                item.packageName,
                                color = if (isScreenOff)
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingContent = {
                            val bitmap = remember(item.packageName + item.className) { item.icon.toBitmap().asImageBitmap() }
                            Image(
                                bitmap,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .then(if (isScreenOff) Modifier.alpha(0.38f) else Modifier)
                            )
                        },
                        modifier = Modifier
                            .clickable(enabled = !isScreenOff) {
                                val intent = Intent(Intent.ACTION_CREATE_SHORTCUT).apply {
                                    component = ComponentName(item.packageName, item.className)
                                    addCategory(Intent.CATEGORY_DEFAULT)
                                }
                                try {
                                    shortcutLauncher.launch(intent)
                                } catch (e: Exception) {
                                    Log.e("ShortcutsTab", "Failed to launch shortcut picker", e)
                                }
                            }
                            .padding(vertical = 4.dp),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }
}

@Composable
fun SystemTab(
    config: ActionConfig,
    onActionSelected: (String) -> Unit,
    shellReady: Boolean,
    contentPadding: PaddingValues
) {
    val actions = listOf(
        SettingsManager.ACTION_VIBRATE_RINGER,
        SettingsManager.ACTION_CYCLE_SOUND_MODE,
        SettingsManager.ACTION_DND,
        SettingsManager.ACTION_NOTIFICATIONS,
        SettingsManager.ACTION_BRIGHTNESS_UP,
        SettingsManager.ACTION_BRIGHTNESS_DOWN,
        SettingsManager.ACTION_AUTO_BRIGHTNESS_TOGGLE,
        SettingsManager.ACTION_WIFI_TOGGLE,
        SettingsManager.ACTION_BLUETOOTH_TOGGLE,
        SettingsManager.ACTION_DATA_TOGGLE,
        SettingsManager.ACTION_NFC_TOGGLE,
        SettingsManager.ACTION_LOCATION_TOGGLE,
        SettingsManager.ACTION_ROTATE_TOGGLE,
        SettingsManager.ACTION_ROTATE_360,
        SettingsManager.ACTION_AUTOROTATE_TOGGLE
    )
    ActionList(actions, config, onActionSelected, shellReady, contentPadding)
}

@Composable
fun MediaTab(
    config: ActionConfig,
    onActionSelected: (String) -> Unit,
    shellReady: Boolean,
    contentPadding: PaddingValues
) {
    val actions = listOf(
        SettingsManager.ACTION_VOLUME_UP,
        SettingsManager.ACTION_VOLUME_DOWN,
        SettingsManager.ACTION_MUTE_VOL,
        SettingsManager.ACTION_VOLUME_DIALOG,
        SettingsManager.ACTION_MUTE_MIC_TOGGLE,
        SettingsManager.ACTION_PLAY_PAUSE,
        SettingsManager.ACTION_STOP,
        SettingsManager.ACTION_PREVIOUS,
        SettingsManager.ACTION_NEXT,
        SettingsManager.ACTION_FAST_FORWARD,
        SettingsManager.ACTION_REWIND,
        SettingsManager.ACTION_STEP_FORWARD,
        SettingsManager.ACTION_STEP_BACKWARD,
    )
    ActionList(actions, config, onActionSelected, shellReady, contentPadding)
}

@Composable
fun DisplayTab(
    config: ActionConfig,
    onActionSelected: (String) -> Unit,
    shellReady: Boolean,
    contentPadding: PaddingValues
) {
    val sizeActions = listOf(
        SettingsManager.ACTION_ASPECT_RATIO_FULL_4_3,
        SettingsManager.ACTION_ASPECT_RATIO_FULL_16_9,
        SettingsManager.ACTION_ASPECT_RATIO_CYCLE,
        SettingsManager.ACTION_ASPECT_RATIO_UP,
        SettingsManager.ACTION_ASPECT_RATIO_DOWN,
        SettingsManager.ACTION_SIZE_FULL,
        SettingsManager.ACTION_SIZE_4_3,
        SettingsManager.ACTION_SIZE_16_9,
        SettingsManager.ACTION_CUSTOM_SIZE_TOGGLE,
    )
    val densityActions = listOf(
        SettingsManager.ACTION_DENSITY_CYCLE,
        SettingsManager.ACTION_DENSITY_300,
        SettingsManager.ACTION_DENSITY_400,
        SettingsManager.ACTION_CUSTOM_DENSITY_TOGGLE,
    )

    val context = LocalContext.current
    var showCustomSizeDialog by rememberSaveable { mutableStateOf(false) }
    var showCustomDensityDialog by rememberSaveable { mutableStateOf(false) }

    var widthInput by rememberSaveable { mutableStateOf("") }
    var heightInput by rememberSaveable { mutableStateOf("") }
    var densityInput by rememberSaveable { mutableStateOf("") }

    fun calculateAspectRatio(w: Int, h: Int): String {
        fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
        val div = gcd(w, h)
        val sw = w / div
        val sh = h / div
        val standard = when {
            (sw == 16 && sh == 9) || (sw == 9 && sh == 16) -> if (sw > sh) "16:9" else "9:16"
            (sw == 4 && sh == 3) || (sw == 3 && sh == 4) -> if (sw > sh) "4:3" else "3:4"
            (sw == 1 && sh == 1) -> "1:1"
            (sw == 3 && sh == 2) || (sw == 2 && sh == 3) -> if (sw > sh) "3:2" else "2:3"
            (sw == 5 && sh == 4) || (sw == 4 && sh == 5) -> if (sw > sh) "5:4" else "4:5"
            (sw == 18 && sh == 9) || (sw == 9 && sh == 18) -> if (sw > sh) "18:9 (2:1)" else "9:18 (1:2)"
            (sw == 19 && sh == 9) || (sw == 9 && sh == 19) -> if (sw > sh) "19:9" else "9:19"
            (sw == 20 && sh == 9) || (sw == 9 && sh == 20) -> if (sw > sh) "20:9" else "9:20"
            (sw == 21 && sh == 9) || (sw == 9 && sh == 21) -> if (sw > sh) "21:9" else "9:21"
            else -> {
                val ratio = if (h > 0) w.toFloat() / h else 1f
                String.format(Locale.US, "%.2f:1 (%d:%d)", ratio, sw, sh)
            }
        }
        return standard
    }

    val customSizeBaseName = stringResource(R.string.action_custom_size_toggle)
    val customDensityBaseName = stringResource(R.string.action_custom_density_toggle)

    if (showCustomSizeDialog) {
        val wVal = widthInput.toIntOrNull()
        val hVal = heightInput.toIntOrNull()
        val isValid = wVal != null && hVal != null && wVal in 650..2000 && hVal in 650..2000
        val ratioText = if (wVal != null && hVal != null && wVal > 0 && hVal > 0) {
            calculateAspectRatio(wVal, hVal)
        } else null

        XenonDialog(
            properties = DialogProperties(usePlatformDefaultWidth = true),
            onDismissRequest = { showCustomSizeDialog = false },
            title = stringResource(R.string.custom_size_title),
            confirmButtonText = stringResource(R.string.ok),
            onConfirmButtonClick = {
                if (wVal != null && hVal != null && isValid) {
                    val sizeString = "${wVal}x${hVal}"
                    SettingsManager.setAction(
                        context,
                        config.keyCode,
                        config.state,
                        config.type,
                        SettingsManager.PREFIX_CUSTOM_SIZE + sizeString
                    )
                    onActionSelected("$customSizeBaseName: $sizeString")
                    showCustomSizeDialog = false
                }
            },
            content = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = widthInput,
                        onValueChange = { widthInput = it.filter { char -> char.isDigit() } },
                        singleLine = true,
                        label = { Text(stringResource(R.string.width_hint)) },
                        placeholder = { Text("Width") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = heightInput,
                        onValueChange = { heightInput = it.filter { char -> char.isDigit() } },
                        singleLine = true,
                        label = { Text(stringResource(R.string.height_hint)) },
                        placeholder = { Text("Height") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (ratioText != null) {
                        Text(
                            text = stringResource(R.string.aspect_ratio_label, ratioText),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (!isValid && (widthInput.isNotEmpty() || heightInput.isNotEmpty())) {
                        Text(
                            text = stringResource(R.string.invalid_size_error),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        )
    }

    if (showCustomDensityDialog) {
        val dVal = densityInput.toIntOrNull()
        val isValid = dVal != null && dVal in 150..600

        XenonDialog(
            properties = DialogProperties(usePlatformDefaultWidth = true),
            onDismissRequest = { showCustomDensityDialog = false },
            title = stringResource(R.string.custom_density_title),
            confirmButtonText = stringResource(R.string.ok),
            onConfirmButtonClick = {
                if (dVal != null && isValid) {
                    SettingsManager.setAction(
                        context,
                        config.keyCode,
                        config.state,
                        config.type,
                        SettingsManager.PREFIX_CUSTOM_DENSITY + dVal
                    )
                    onActionSelected("$customDensityBaseName: $dVal")
                    showCustomDensityDialog = false
                }
            },
            content = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = densityInput,
                        onValueChange = { densityInput = it.filter { char -> char.isDigit() } },
                        singleLine = true,
                        label = { Text(stringResource(R.string.density_hint)) },
                        placeholder = { Text("Density") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!isValid && densityInput.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.invalid_density_error),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(sizeActions.size) { index ->
            val action = sizeActions[index]
            val disabled = isActionDisabled(action, shellReady)
            val disabledReasonRes = disabledReasonFor(action, shellReady)
            val displayName = getActionDisplayName(action)
            val shape = when {
                sizeActions.size == 1 -> RoundedCornerShape(30.dp)
                index == 0 -> RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                index == sizeActions.size - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 30.dp, bottomEnd = 30.dp)
                else -> RoundedCornerShape(4.dp)
            }
            Surface(
                color = if (disabled)
                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = shape,
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    headlineContent = {
                        Text(
                            displayName,
                            color = if (disabled)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    supportingContent = disabledReasonRes?.let { resId ->
                        {
                            Text(
                                stringResource(resId),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    leadingContent = {
                        ActionIcon(
                            action = action,
                            modifier = Modifier.size(24.dp),
                            tint = if (disabled)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                            else MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .clickable(enabled = !disabled) {
                            if (action == SettingsManager.ACTION_CUSTOM_SIZE_TOGGLE) {
                                val currentSavedAction =
                                    SettingsManager.getAction(context, config.keyCode, config.state, config.type)
                                if (currentSavedAction.startsWith(SettingsManager.PREFIX_CUSTOM_SIZE)) {
                                    val parts = currentSavedAction.removePrefix(SettingsManager.PREFIX_CUSTOM_SIZE).split("x")
                                    widthInput = parts.getOrNull(0) ?: ""
                                    heightInput = parts.getOrNull(1) ?: ""
                                } else {
                                    widthInput = ""
                                    heightInput = ""
                                }
                                showCustomSizeDialog = true
                            } else {
                                SettingsManager.setAction(context, config.keyCode, config.state, config.type, action)
                                onActionSelected(displayName)
                            }
                        }
                        .padding(vertical = 4.dp),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(densityActions.size) { index ->
            val action = densityActions[index]
            val disabled = isActionDisabled(action, shellReady)
            val disabledReasonRes = disabledReasonFor(action, shellReady)
            val displayName = getActionDisplayName(action)
            val shape = when {
                densityActions.size == 1 -> RoundedCornerShape(30.dp)
                index == 0 -> RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                index == densityActions.size - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 30.dp, bottomEnd = 30.dp)
                else -> RoundedCornerShape(4.dp)
            }
            Surface(
                color = if (disabled)
                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = shape,
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    headlineContent = {
                        Text(
                            displayName,
                            color = if (disabled)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    supportingContent = disabledReasonRes?.let { resId ->
                        {
                            Text(
                                stringResource(resId),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    leadingContent = {
                        ActionIcon(
                            action = action,
                            modifier = Modifier.size(24.dp),
                            tint = if (disabled)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                            else MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .clickable(enabled = !disabled) {
                            if (action == SettingsManager.ACTION_CUSTOM_DENSITY_TOGGLE) {
                                val currentSavedAction =
                                    SettingsManager.getAction(context, config.keyCode, config.state, config.type)
                                if (currentSavedAction.startsWith(SettingsManager.PREFIX_CUSTOM_DENSITY)) {
                                    densityInput = currentSavedAction.removePrefix(SettingsManager.PREFIX_CUSTOM_DENSITY)
                                } else {
                                    densityInput = ""
                                }
                                showCustomDensityDialog = true
                            } else {
                                SettingsManager.setAction(context, config.keyCode, config.state, config.type, action)
                                onActionSelected(displayName)
                            }
                        }
                        .padding(vertical = 4.dp),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
    }
}

@Composable
fun ActionList(
    actions: List<String>,
    config: ActionConfig,
    onActionSelected: (String) -> Unit,
    shellReady: Boolean = true,
    contentPadding: PaddingValues
) {
    val context = LocalContext.current
    var showInputDialog by rememberSaveable { mutableStateOf<String?>(null) }
    var showAodStyleDialog by rememberSaveable { mutableStateOf(false) }
    var showLiftToWakeWarning by rememberSaveable { mutableStateOf(false) }
    var inputValue by rememberSaveable { mutableStateOf("") }

    val aodActionName = stringResource(R.string.action_aod)
    val styleConcentric = stringResource(R.string.style_concentric)
    val styleStacked = stringResource(R.string.style_stacked)
    val styleInline = stringResource(R.string.style_inline)
    val styleAnalog = stringResource(R.string.style_analog)
    val styleStackedDot = stringResource(R.string.style_stacked_dot)
    val styleStackedDigital = stringResource(R.string.style_stacked_digital)
    val styleInlineDot = stringResource(R.string.style_inline_dot)
    val styleInlineDigital = stringResource(R.string.style_inline_digital)
    val stylePlanets = stringResource(R.string.style_planets)
    val styleSpinner = stringResource(R.string.style_spinner)
    val stylePixelStacked = stringResource(R.string.style_pixel_stacked)
    val stylePixelInline = stringResource(R.string.style_pixel_inline)
    val styleBlocks = stringResource(R.string.style_blocks)
    val styleBars = stringResource(R.string.style_bars)
    val speedDialName = stringResource(R.string.speed_dial)
    val urlName = stringResource(R.string.url)
    val qrCodeName = stringResource(R.string.qr_code)

    if (showLiftToWakeWarning) {
        LiftToWakeWarningDialog(
            onDismissRequest = { showLiftToWakeWarning = false },
            onIgnore = { dontShowAgain ->
                if (dontShowAgain) SettingsManager.setShowLiftToWakeWarning(context, false)
                showLiftToWakeWarning = false
                showAodStyleDialog = true
            },
            onDisable = { dontShowAgain ->
                if (dontShowAgain) SettingsManager.setShowLiftToWakeWarning(context, false)
                showLiftToWakeWarning = false
                showAodStyleDialog = true
            }
        )
    }

    if (showAodStyleDialog) {
        AodStylePickerDialog(
            onDismissRequest = { showAodStyleDialog = false },
            onStyleSelected = { style, mediaEnabled ->
                SettingsManager.setAodStyle(context, style)
                SettingsManager.setAodMediaEnabled(context, mediaEnabled)
                SettingsManager.setAction(context, config.keyCode, config.state, config.type, SettingsManager.ACTION_AOD)
                val styleName = when (style) {
                    SettingsManager.AodStyle.CONCENTRIC -> styleConcentric
                    SettingsManager.AodStyle.STACKED -> styleStacked
                    SettingsManager.AodStyle.INLINE -> styleInline
                    SettingsManager.AodStyle.ANALOG -> styleAnalog
                    SettingsManager.AodStyle.STACKED_DOT -> styleStackedDot
                    SettingsManager.AodStyle.STACKED_DIGITAL -> styleStackedDigital
                    SettingsManager.AodStyle.INLINE_DOT -> styleInlineDot
                    SettingsManager.AodStyle.INLINE_DIGITAL -> styleInlineDigital
                    SettingsManager.AodStyle.PLANETS -> stylePlanets
                    SettingsManager.AodStyle.SPINNER -> styleSpinner
                    SettingsManager.AodStyle.PIXEL_STACKED -> stylePixelStacked
                    SettingsManager.AodStyle.PIXEL_INLINE -> stylePixelInline
                    SettingsManager.AodStyle.BLOCKS -> styleBlocks
                    SettingsManager.AodStyle.BARS -> styleBars
                }
                onActionSelected("$aodActionName: $styleName")
                showAodStyleDialog = false
            }
        )
    }

    if (showInputDialog != null) {
        val title = when (showInputDialog) {
            SettingsManager.ACTION_SPEED_DIAL -> stringResource(R.string.enter_number)
            SettingsManager.ACTION_URL -> stringResource(R.string.enter_url)
            SettingsManager.ACTION_QR_CODE -> stringResource(R.string.enter_qr_content)
            else -> ""
        }
        val label = when (showInputDialog) {
            SettingsManager.ACTION_SPEED_DIAL -> stringResource(R.string.phone_number_hint)
            SettingsManager.ACTION_URL -> stringResource(R.string.link_hint)
            SettingsManager.ACTION_QR_CODE -> stringResource(R.string.text_hint)
            else -> ""
        }
        XenonDialog(
            onDismissRequest = { showInputDialog = null },
            properties = DialogProperties(usePlatformDefaultWidth = true),
            title = title,
            confirmButtonText = stringResource(R.string.ok),
            onConfirmButtonClick = {
                val prefix = when (showInputDialog) {
                    SettingsManager.ACTION_SPEED_DIAL -> SettingsManager.PREFIX_SPEED_DIAL
                    SettingsManager.ACTION_URL -> SettingsManager.PREFIX_URL
                    SettingsManager.ACTION_QR_CODE -> SettingsManager.PREFIX_QR_CODE
                    else -> ""
                }
                SettingsManager.setAction(context, config.keyCode, config.state, config.type, prefix + inputValue)
                onActionSelected(
                    when (showInputDialog) {
                        SettingsManager.ACTION_SPEED_DIAL -> speedDialName
                        SettingsManager.ACTION_URL -> urlName
                        SettingsManager.ACTION_QR_CODE -> qrCodeName
                        else -> ""
                    }
                )
                showInputDialog = null
            },
            content = {
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    singleLine = true,
                    label = { Text(label) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(actions.size) { index ->
            val action = actions[index]
            val disabled = isActionDisabled(action, shellReady)
            val disabledReasonRes = disabledReasonFor(action, shellReady)

            val displayName = getActionDisplayName(action)

            val isContinuum = action == SettingsManager.ACTION_VOLUME_UP ||
                    action == SettingsManager.ACTION_VOLUME_DOWN ||
                    action == SettingsManager.ACTION_SCROLL_UP_SMOOTH ||
                    action == SettingsManager.ACTION_SCROLL_DOWN_SMOOTH ||
                    action == SettingsManager.ACTION_SCROLL_UP_SMOOTH_FAST ||
                    action == SettingsManager.ACTION_SCROLL_DOWN_SMOOTH_FAST ||
                    action == "TAP_SCROLL_UP_SMOOTH" ||
                    action == "TAP_SCROLL_DOWN_SMOOTH" ||
                    action == SettingsManager.ACTION_BRIGHTNESS_UP ||
                    action == SettingsManager.ACTION_BRIGHTNESS_DOWN

            val shape = when {
                actions.size == 1 -> RoundedCornerShape(30.dp)
                index == 0 -> RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                index == actions.size - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 30.dp, bottomEnd = 30.dp)
                else -> RoundedCornerShape(4.dp)
            }

            Surface(
                color = if (disabled)
                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = shape,
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    headlineContent = {
                        Text(
                            displayName,
                            color = if (disabled)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    supportingContent = disabledReasonRes?.let { resId ->
                        {
                            Text(
                                stringResource(resId),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    leadingContent = {
                        ActionIcon(
                            action = action,
                            modifier = Modifier.size(24.dp),
                            tint = if (disabled)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                            else MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingContent = if (isContinuum) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardDoubleArrowRight,
                                contentDescription = null,
                                tint = if (disabled)
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else null,
                    modifier = Modifier
                        .clickable(enabled = !disabled) {
                            if (action == SettingsManager.ACTION_AOD) {
                                if (SettingsManager.shouldShowLiftToWakeWarning(context)) {
                                    showLiftToWakeWarning = true
                                } else {
                                    showAodStyleDialog = true
                                }
                            } else if (action == SettingsManager.ACTION_SPEED_DIAL ||
                                action == SettingsManager.ACTION_URL ||
                                action == SettingsManager.ACTION_QR_CODE
                            ) {
                                showInputDialog = action
                                val currentSavedAction =
                                    SettingsManager.getAction(context, config.keyCode, config.state, config.type)
                                val prefix = when (action) {
                                    SettingsManager.ACTION_SPEED_DIAL -> SettingsManager.PREFIX_SPEED_DIAL
                                    SettingsManager.ACTION_URL -> SettingsManager.PREFIX_URL
                                    SettingsManager.ACTION_QR_CODE -> SettingsManager.PREFIX_QR_CODE
                                    else -> ""
                                }
                                inputValue =
                                    if (currentSavedAction.startsWith(prefix)) currentSavedAction.removePrefix(prefix) else ""
                            } else {
                                SettingsManager.setAction(context, config.keyCode, config.state, config.type, action)
                                onActionSelected(displayName)
                            }
                        }
                        .padding(vertical = 4.dp),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
    }
}