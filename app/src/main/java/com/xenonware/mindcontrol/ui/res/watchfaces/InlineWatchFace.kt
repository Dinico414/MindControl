package com.xenonware.mindcontrol.ui.res.watchfaces

import android.service.notification.StatusBarNotification
import android.text.format.DateFormat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenonware.mindcontrol.MediaInfo
import com.xenonware.mindcontrol.ui.res.UnifiedAodStyle
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.max
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun InlineAodStyle(
    isActive: Boolean,
    notifications: List<StatusBarNotification>,
    mediaInfo: MediaInfo?,
    isCharging: Boolean,
    batteryLevel: Int,
    animatedTextAlpha: Float,
    offsetY: Float,
    isMediaEnabled: Boolean = true
) {
    UnifiedAodStyle(
        isActive = isActive,
        notifications = notifications,
        mediaInfo = mediaInfo,
        isCharging = isCharging,
        batteryLevel = batteryLevel,
        animatedTextAlpha = animatedTextAlpha,
        offsetY = offsetY,
        isMediaEnabled = isMediaEnabled,
        watchFace = { InlineWatchFace(isActive = isActive) }
    )
}

@Composable
fun InlineWatchFace(isActive: Boolean) {
    val textMeasurer = rememberTextMeasurer()
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)
    val locale = LocalConfiguration.current.locales[0]
    val localDensity = LocalDensity.current

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.7f,
        animationSpec = tween(500),
        label = "alpha"
    )

    var time by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val isFullyInactive = !isActive

    LaunchedEffect(isFullyInactive) {
        while (true) {
            time = System.currentTimeMillis()
            if (!isFullyInactive) {
                delay(16.milliseconds)
            } else {
                val calendar = Calendar.getInstance().apply { timeInMillis = time }
                val seconds = calendar.get(Calendar.SECOND)
                val millis = calendar.get(Calendar.MILLISECOND)
                val delayToNextMinute = 60000L - (seconds * 1000L + millis)
                delay(max(delayToNextMinute, 100L).milliseconds)
            }
        }
    }

    val calendar = Calendar.getInstance().apply { timeInMillis = time }
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val displayHour = if (is24Hour) {
        hour
    } else {
        val hour12 = hour % 12
        if (hour12 == 0) 12 else hour12
    }
    val minute = calendar.get(Calendar.MINUTE)

    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.75f)
            .aspectRatio(1f)
            .graphicsLayer(
                alpha = animatedAlpha
            )
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = w / 2f

        fun pxToSp(px: Float): TextUnit =
            (px / localDensity.density / localDensity.fontScale).sp

        val textStyle = TextStyle(
            color = Color.White,
            fontSize = pxToSp(r * 0.47f),
            fontWeight = FontWeight.Bold,
            fontFamily = QuicksandTitleVariable
        )

        val hourText = String.format(locale, "%02d", displayHour)
        val colonText = ":"
        val minuteText = String.format(locale, "%02d", minute)

        val hourLayout = textMeasurer.measure(hourText, textStyle)
        val colonLayout = textMeasurer.measure(colonText, textStyle)
        val minuteLayout = textMeasurer.measure(minuteText, textStyle)

        val totalWidth = hourLayout.size.width + colonLayout.size.width + minuteLayout.size.width
        val hourLeft = cx - totalWidth / 2f
        val colonLeft = hourLeft + hourLayout.size.width
        val minuteLeft = colonLeft + colonLayout.size.width

        drawText(
            textLayoutResult = hourLayout,
            topLeft = Offset(hourLeft, cy - hourLayout.size.height / 2f)
        )
        drawText(
            textLayoutResult = colonLayout,
            topLeft = Offset(colonLeft, cy - colonLayout.size.height / 2f),
            alpha = 1f
        )
        drawText(
            textLayoutResult = minuteLayout,
            topLeft = Offset(minuteLeft, cy - minuteLayout.size.height / 2f),
            alpha = 1f
        )
    }
}
