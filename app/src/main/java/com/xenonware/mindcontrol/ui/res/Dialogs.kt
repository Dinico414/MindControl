package com.xenonware.mindcontrol.ui.res

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.xenon.mylibrary.res.XenonDialog
import com.xenonware.mindcontrol.R
import com.xenonware.mindcontrol.SettingsManager
import com.xenonware.mindcontrol.ShellManager
import com.xenonware.mindcontrol.ui.res.watchfaces.AnalogAodStyle
import com.xenonware.mindcontrol.ui.res.watchfaces.AnalogGroupWatchFace
import com.xenonware.mindcontrol.ui.res.watchfaces.BarsAodStyle
import com.xenonware.mindcontrol.ui.res.watchfaces.BlocksAodStyle
import com.xenonware.mindcontrol.ui.res.watchfaces.ConcentricAodStyle
import com.xenonware.mindcontrol.ui.res.watchfaces.CubedAodStyle
import com.xenonware.mindcontrol.ui.res.watchfaces.DigitalAbstractAodStyle
import com.xenonware.mindcontrol.ui.res.watchfaces.InlineAodStyle
import com.xenonware.mindcontrol.ui.res.watchfaces.InlineDigitalAodStyle
import com.xenonware.mindcontrol.ui.res.watchfaces.InlineDotAodStyle
import com.xenonware.mindcontrol.ui.res.watchfaces.PixelInlineAodStyle
import com.xenonware.mindcontrol.ui.res.watchfaces.PixelStackedAodStyle
import com.xenonware.mindcontrol.ui.res.watchfaces.PlanetsAodStyle
import com.xenonware.mindcontrol.ui.res.watchfaces.SpinnerAodStyle
import com.xenonware.mindcontrol.ui.res.watchfaces.StackedAodStyle
import com.xenonware.mindcontrol.ui.res.watchfaces.StackedDigitalAodStyle
import com.xenonware.mindcontrol.ui.res.watchfaces.StackedDotAodStyle
import com.xenonware.mindcontrol.ui.res.watchfaces.TextAodStyle
import com.xenonware.mindcontrol.ui.res.watchfaces.TextWatchFace
import com.xenonware.mindcontrol.ui.res.watchfaces.TrackAodStyle
import kotlinx.coroutines.launch

@Composable
fun LiftToWakeWarningDialog(
    onDismissRequest: () -> Unit,
    onIgnore: (Boolean) -> Unit,
    onDisable: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var dontShowAgain by remember { mutableStateOf(false) }

    XenonDialog(
        properties = DialogProperties(usePlatformDefaultWidth = true),
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.disable_lift_to_wake_title),
        confirmButtonText = stringResource(R.string.disable),
        onConfirmButtonClick = {
            onDisable(dontShowAgain)
            try {
                // Open Display Settings where the user confirmed the setting is located
                val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)

                // If Shizuku is available, scroll to the bottom after a short delay
                if (ShellManager.isAvailable()) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        // 1. Slow swipe to collapse the header and move the list
                        // 2. sleep to let the animation finish
                        // 3. MOVE_END to jump focus to the bottom
                        // 4. Repeated DPAD_DOWN to settle on the absolute last item
                        val scrollCommand = buildString {
                            append("input swipe 500 1000 500 200 10")
                            append(" && sleep 0.8")
                            append(" && input keyevent 123")
                            repeat(25) { append(" && input keyevent 20") }
                        }
                        ShellManager.runShellCommand(scrollCommand)
                    }, 1500L)
                }
            } catch (_: Exception) {
                try {
                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                } catch (_: Exception) {
                    // Fallback failed
                }
            }
        },
        actionButton1Text = stringResource(R.string.ignore),
        onActionButton1Click = {
            onIgnore(dontShowAgain)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.lift_to_wake_msg),
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { dontShowAgain = !dontShowAgain }
            ) {
                Checkbox(
                    checked = dontShowAgain,
                    onCheckedChange = { dontShowAgain = it }
                )
                Text(
                    text = stringResource(R.string.dont_show_again),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AodStylePickerDialog(
    onDismissRequest: () -> Unit,
    onStyleSelected: (SettingsManager.AodStyle, Boolean) -> Unit
) {
    val context = LocalContext.current
    val styles = SettingsManager.getAvailableAodStyles()
    val carouselState = rememberCarouselState(itemCount = { styles.size })
    val selectedIndex = carouselState.currentItem
    val scope = rememberCoroutineScope()
    val carouselHeight = with(LocalDensity.current) { 600.toDp() }
    val maxItemWidth = (carouselHeight - 16.dp) / 1.15f

    var mediaControlsEnabled by remember { mutableStateOf(SettingsManager.isAodMediaEnabled(context)) }
    var lockEnabled by remember { mutableStateOf(SettingsManager.isAodLockEnabled(context)) }
    var showTextOptionsDialog by remember { mutableStateOf(false) }
    var showAnalogOptionsDialog by remember { mutableStateOf(false) }

    if (showTextOptionsDialog) {
        TextClockOptionsDialog(
            onDismissRequest = { showTextOptionsDialog = false },
            onSave = { showTextOptionsDialog = false }
        )
    }

    if (showAnalogOptionsDialog) {
        AnalogClockOptionsDialog(
            onDismissRequest = { showAnalogOptionsDialog = false },
            onSave = { showAnalogOptionsDialog = false }
        )
    }

    XenonDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = true),
        title = stringResource(R.string.select_aod_style_title),
        confirmButtonText = stringResource(R.string.select),
        onConfirmButtonClick = {
            SettingsManager.setAodLockEnabled(context, lockEnabled)
            onStyleSelected(styles[selectedIndex], mediaControlsEnabled)
        },
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalCenteredHeroCarousel(
                    state = carouselState,
                    maxItemWidth = maxItemWidth,
                    modifier = Modifier
                        .height(carouselHeight)
                        .fillMaxWidth(),
                    itemSpacing = 8.dp,
                    contentPadding = PaddingValues(horizontal = 24.dp)
                ) { index ->
                    val style = styles[index]
                    val isSelected = selectedIndex == index
                    
                    val borderStroke = androidx.compose.foundation.BorderStroke(
                        if (isSelected) 4.dp else 2.dp, 
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    val itemShape = MaterialTheme.shapes.extraLarge

                    val styleName = when (style) {
                        SettingsManager.AodStyle.TEXT -> stringResource(R.string.style_text)
                        SettingsManager.AodStyle.CONCENTRIC -> stringResource(R.string.style_concentric)
                        SettingsManager.AodStyle.STACKED -> stringResource(R.string.style_stacked)
                        SettingsManager.AodStyle.INLINE -> stringResource(R.string.style_inline)
                        SettingsManager.AodStyle.ANALOG -> stringResource(R.string.style_analog)
                        SettingsManager.AodStyle.STACKED_DOT -> stringResource(R.string.style_stacked_dot)
                        SettingsManager.AodStyle.STACKED_DIGITAL -> stringResource(R.string.style_stacked_digital)
                        SettingsManager.AodStyle.INLINE_DOT -> stringResource(R.string.style_inline_dot)
                        SettingsManager.AodStyle.INLINE_DIGITAL -> stringResource(R.string.style_inline_digital)
                        SettingsManager.AodStyle.PLANETS -> stringResource(R.string.style_planets)
                        SettingsManager.AodStyle.SPINNER -> stringResource(R.string.style_spinner)
                        SettingsManager.AodStyle.PIXEL_STACKED -> stringResource(R.string.style_pixel_stacked)
                        SettingsManager.AodStyle.PIXEL_INLINE -> stringResource(R.string.style_pixel_inline)
                        SettingsManager.AodStyle.BLOCKS -> stringResource(R.string.style_blocks)
                        SettingsManager.AodStyle.BARS -> stringResource(R.string.style_bars)
                        SettingsManager.AodStyle.TRACK -> stringResource(R.string.style_track)
                        SettingsManager.AodStyle.DIGITAL_ABSTRACT -> stringResource(R.string.style_digital_abstract)
                        SettingsManager.AodStyle.CUBED -> stringResource(R.string.style_cubes)
                    }

                    AodStyleOption(
                        name = styleName,
                        style = style,
                        onClick = {
                            scope.launch {
                                carouselState.animateScrollToItem(index)
                            }
                        },
                        onGearClick = when (style) {
                            SettingsManager.AodStyle.TEXT -> { { showTextOptionsDialog = true } }
                            SettingsManager.AodStyle.ANALOG -> { { showAnalogOptionsDialog = true } }
                            else -> null
                        },
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .aspectRatio(1f / 1.15f)
                            .maskClip(itemShape)
                            .maskBorder(borderStroke, itemShape)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.aod_media_controls),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = mediaControlsEnabled,
                        onCheckedChange = { mediaControlsEnabled = it }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.lock),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = lockEnabled,
                        onCheckedChange = { lockEnabled = it }
                    )
                }
            }
        }
    )
}

@Composable
fun AodStyleOption(
    name: String,
    style: SettingsManager.AodStyle,
    onClick: () -> Unit,
    onGearClick: (() -> Unit)? = null,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        shape = androidx.compose.ui.graphics.RectangleShape
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Preview
            Box(modifier = Modifier.fillMaxSize().alpha(0.8f)) {
                when (style) {
                    SettingsManager.AodStyle.TEXT -> {
                        TextAodStyle(
                            isActive = true,
                            notifications = emptyList(),
                            mediaInfo = null,
                            isCharging = false,
                            batteryLevel = 80,
                            animatedTextAlpha = 0f,
                            offsetY = 0f
                        )
                    }
                    SettingsManager.AodStyle.CONCENTRIC -> {
                        ConcentricAodStyle(
                            isActive = true,
                            notifications = emptyList(),
                            mediaInfo = null,
                            isCharging = false,
                            batteryLevel = 80,
                            animatedTextAlpha = 0f,
                            offsetY = 0f
                        )
                    }
                    SettingsManager.AodStyle.STACKED -> {
                        StackedAodStyle(
                            isActive = true,
                            notifications = emptyList(),
                            mediaInfo = null,
                            isCharging = false,
                            batteryLevel = 80,
                            animatedTextAlpha = 0f,
                            offsetY = 0f
                        )
                    }
                    SettingsManager.AodStyle.INLINE -> {
                        InlineAodStyle(
                            isActive = true,
                            notifications = emptyList(),
                            mediaInfo = null,
                            isCharging = false,
                            batteryLevel = 80,
                            animatedTextAlpha = 0f,
                            offsetY = 0f
                        )
                    }
                    SettingsManager.AodStyle.ANALOG -> {
                        AnalogAodStyle(
                            isActive = true,
                            notifications = emptyList(),
                            mediaInfo = null,
                            isCharging = false,
                            batteryLevel = 80,
                            animatedTextAlpha = 0f,
                            offsetY = 0f
                        )
                    }
                    SettingsManager.AodStyle.STACKED_DOT -> {
                        StackedDotAodStyle(
                            isActive = true,
                            notifications = emptyList(),
                            mediaInfo = null,
                            isCharging = false,
                            batteryLevel = 80,
                            animatedTextAlpha = 0f,
                            offsetY = 0f
                        )
                    }
                    SettingsManager.AodStyle.STACKED_DIGITAL -> {
                        StackedDigitalAodStyle(
                            isActive = true,
                            notifications = emptyList(),
                            mediaInfo = null,
                            isCharging = false,
                            batteryLevel = 80,
                            animatedTextAlpha = 0f,
                            offsetY = 0f
                        )
                    }
                    SettingsManager.AodStyle.INLINE_DOT -> {
                        InlineDotAodStyle(
                            isActive = true,
                            notifications = emptyList(),
                            mediaInfo = null,
                            isCharging = false,
                            batteryLevel = 80,
                            animatedTextAlpha = 0f,
                            offsetY = 0f
                        )
                    }
                    SettingsManager.AodStyle.INLINE_DIGITAL -> {
                        InlineDigitalAodStyle(
                            isActive = true,
                            notifications = emptyList(),
                            mediaInfo = null,
                            isCharging = false,
                            batteryLevel = 80,
                            animatedTextAlpha = 0f,
                            offsetY = 0f
                        )
                    }
                    SettingsManager.AodStyle.PLANETS -> {
                        PlanetsAodStyle(
                            isActive = true,
                            notifications = emptyList(),
                            mediaInfo = null,
                            isCharging = false,
                            batteryLevel = 80,
                            animatedTextAlpha = 0f,
                            offsetY = 0f
                        )
                    }
                    SettingsManager.AodStyle.SPINNER -> {
                        SpinnerAodStyle(
                            isActive = true,
                            notifications = emptyList(),
                            mediaInfo = null,
                            isCharging = false,
                            batteryLevel = 80,
                            animatedTextAlpha = 0f,
                            offsetY = 0f
                        )
                    }
                    SettingsManager.AodStyle.PIXEL_STACKED -> {
                        PixelStackedAodStyle(
                            isActive = true,
                            notifications = emptyList(),
                            mediaInfo = null,
                            isCharging = false,
                            batteryLevel = 80,
                            animatedTextAlpha = 0f,
                            offsetY = 0f
                        )
                    }
                    SettingsManager.AodStyle.PIXEL_INLINE -> {
                        PixelInlineAodStyle(
                            isActive = true,
                            notifications = emptyList(),
                            mediaInfo = null,
                            isCharging = false,
                            batteryLevel = 80,
                            animatedTextAlpha = 0f,
                            offsetY = 0f
                        )
                    }
                    SettingsManager.AodStyle.BLOCKS -> {
                        BlocksAodStyle(
                            isActive = true,
                            notifications = emptyList(),
                            mediaInfo = null,
                            isCharging = false,
                            batteryLevel = 80,
                            animatedTextAlpha = 0f,
                            offsetY = 0f
                        )
                    }
                    SettingsManager.AodStyle.BARS -> {
                        BarsAodStyle(
                            isActive = true,
                            notifications = emptyList(),
                            mediaInfo = null,
                            isCharging = false,
                            batteryLevel = 80,
                            animatedTextAlpha = 0f,
                            offsetY = 0f
                        )
                    }
                    SettingsManager.AodStyle.TRACK -> {
                        TrackAodStyle(
                            isActive = true,
                            notifications = emptyList(),
                            mediaInfo = null,
                            isCharging = false,
                            batteryLevel = 80,
                            animatedTextAlpha = 0f,
                            offsetY = 0f
                        )
                    }
                    SettingsManager.AodStyle.DIGITAL_ABSTRACT -> {
                        DigitalAbstractAodStyle(
                            isActive = true,
                            notifications = emptyList(),
                            mediaInfo = null,
                            isCharging = false,
                            batteryLevel = 80,
                            animatedTextAlpha = 0f,
                            offsetY = 0f
                        )
                    }
                    SettingsManager.AodStyle.CUBED -> {
                        CubedAodStyle(
                            isActive = true,
                            notifications = emptyList(),
                            mediaInfo = null,
                            isCharging = false,
                            batteryLevel = 80,
                            animatedTextAlpha = 0f,
                            offsetY = 0f
                        )
                    }
                }
            }

            // Gear icon overlay for clocks with options
            if (onGearClick != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    IconButton(
                        onClick = onGearClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.clock_options),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            // Label overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(name, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TextClockOptionsDialog(
    onDismissRequest: () -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    var layout by remember { mutableStateOf(SettingsManager.getTextClockLayout(context)) }
    var substyle by remember { mutableStateOf(SettingsManager.getTextClockSubstyle(context)) }
    var fontFamilyKey by remember { mutableStateOf(SettingsManager.getTextClockFontFamily(context)) }
    var fontWeightInt by remember { mutableIntStateOf(SettingsManager.getTextClockFontWeight(context)) }
    var isItalic by remember { mutableStateOf(SettingsManager.isTextClockItalic(context)) }

    XenonDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = true),
        title = stringResource(R.string.clock_options),
        confirmButtonText = stringResource(R.string.select),
        onConfirmButtonClick = {
            SettingsManager.setTextClockLayout(context, layout)
            SettingsManager.setTextClockSubstyle(context, substyle)
            SettingsManager.setTextClockFontFamily(context, fontFamilyKey)
            SettingsManager.setTextClockFontWeight(context, fontWeightInt)
            SettingsManager.setTextClockItalic(context, isItalic)
            onSave()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Preview Card
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .aspectRatio(1f),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    TextWatchFace(
                        isActive = true,
                        overrideLayout = layout,
                        overrideSubstyle = substyle,
                        overrideFontFamilyKey = fontFamilyKey,
                        overrideFontWeightInt = fontWeightInt,
                        overrideIsItalic = isItalic
                    )
                }
            }

            // Section 1: Layout Mode (Single-choice Inline vs Stacked)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.layout),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = layout == SettingsManager.TextClockLayout.INLINE,
                        onClick = { layout = SettingsManager.TextClockLayout.INLINE },
                        label = { Text(stringResource(R.string.layout_inline)) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = layout == SettingsManager.TextClockLayout.STACKED,
                        onClick = { layout = SettingsManager.TextClockLayout.STACKED },
                        label = { Text(stringResource(R.string.layout_stacked)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Section 2: Style Selector List
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.clock_style),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                val styleScrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalFadingEdges(styleScrollState, 16.dp)
                        .horizontalScroll(styleScrollState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val styles = listOf(
                        SettingsManager.TextClockSubstyle.MODERN to stringResource(R.string.substyle_modern),
                        SettingsManager.TextClockSubstyle.NOTHING to stringResource(R.string.substyle_nothing),
                        SettingsManager.TextClockSubstyle.DIGITAL to stringResource(R.string.substyle_digital),
                        SettingsManager.TextClockSubstyle.PIXEL to stringResource(R.string.substyle_pixel)
                    )
                    styles.forEach { (st, label) ->
                        FilterChip(
                            selected = substyle == st,
                            onClick = { substyle = st },
                            label = { Text(label) }
                        )
                    }
                }
            }

            // Section 3: Modern Font Options
            AnimatedVisibility(visible = substyle == SettingsManager.TextClockSubstyle.MODERN) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.font_customization),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Font Family
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.font_family),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val familyScrollState = rememberScrollState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalFadingEdges(familyScrollState, 16.dp)
                                .horizontalScroll(familyScrollState),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val families = listOf(
                                "QUICKSAND" to stringResource(R.string.font_quicksand),
                                "SANS_SERIF" to stringResource(R.string.font_sans_serif),
                                "SERIF" to stringResource(R.string.font_serif),
                                "MONOSPACE" to stringResource(R.string.font_monospace),
                                "CURSIVE" to stringResource(R.string.font_cursive)
                            )
                            families.forEach { (key, name) ->
                                FilterChip(
                                    selected = fontFamilyKey == key,
                                    onClick = { fontFamilyKey = key },
                                    label = { Text(name) }
                                )
                            }
                        }
                    }

                    // Font Weight
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.font_weight),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val weightScrollState = rememberScrollState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalFadingEdges(weightScrollState, 16.dp)
                                .horizontalScroll(weightScrollState),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val weights = listOf(
                                300 to stringResource(R.string.weight_light),
                                400 to stringResource(R.string.weight_normal),
                                500 to stringResource(R.string.weight_medium),
                                700 to stringResource(R.string.weight_bold),
                                800 to stringResource(R.string.weight_extra_bold)
                            )
                            weights.forEach { (wt, name) ->
                                FilterChip(
                                    selected = fontWeightInt == wt,
                                    onClick = { fontWeightInt = wt },
                                    label = { Text(name) }
                                )
                            }
                        }
                    }

                    // Italic Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.font_italic),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = isItalic,
                            onCheckedChange = { isItalic = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnalogClockOptionsDialog(
    onDismissRequest: () -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    var substyle by remember { mutableStateOf(SettingsManager.getAnalogClockSubstyle(context)) }

    XenonDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = true),
        title = stringResource(R.string.clock_options),
        confirmButtonText = stringResource(R.string.select),
        onConfirmButtonClick = {
            SettingsManager.setAnalogClockSubstyle(context, substyle)
            onSave()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Preview Card
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .aspectRatio(1f),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AnalogGroupWatchFace(
                        isActive = true,
                        overrideSubstyle = substyle
                    )
                }
            }

            // Substyle Selector
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.clock_style),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                val styleScrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalFadingEdges(styleScrollState, 16.dp)
                        .horizontalScroll(styleScrollState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val styles = listOf(
                        SettingsManager.AnalogClockSubstyle.CLASSIC to stringResource(R.string.substyle_classic),
                        SettingsManager.AnalogClockSubstyle.PLANETS to stringResource(R.string.substyle_planets),
                        SettingsManager.AnalogClockSubstyle.ABSTRACT to stringResource(R.string.substyle_abstract)
                    )
                    styles.forEach { (st, label) ->
                        FilterChip(
                            selected = substyle == st,
                            onClick = { substyle = st },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }
    }
}