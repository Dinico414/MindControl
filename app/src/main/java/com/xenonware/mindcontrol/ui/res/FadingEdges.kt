package com.xenonware.mindcontrol.ui.res

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp

/** Content is fully cleared at the top edge and fully visible [height] below it. */
fun Modifier.topFadingEdge(height: Dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val h = height.toPx()
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Black, Color.Transparent),
                startY = 0f,
                endY = h
            ),
            size = Size(size.width, h),
            blendMode = BlendMode.DstOut
        )
    }

/** Fades content out over [width] at both the left and right edges. */
fun Modifier.horizontalFadingEdges(width: Dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val w = width.toPx()
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Black, Color.Transparent),
                startX = 0f,
                endX = w
            ),
            size = Size(w, size.height),
            blendMode = BlendMode.DstOut
        )
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color.Black),
                startX = size.width - w,
                endX = size.width
            ),
            topLeft = Offset(size.width - w, 0f),
            size = Size(w, size.height),
            blendMode = BlendMode.DstOut
        )
    }