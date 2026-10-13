package com.xenonware.mindcontrol.ui.res

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.ui.unit.dp

/** Content is fully cleared at the top edge and fully visible [height] below it. */
fun Modifier.topFadingEdge(height: Dp = 16.dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val h = height.toPx()
        if (h > 0) {
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
    }

/** Content is fully cleared at the bottom edge and fully visible [height] above it. */
fun Modifier.bottomFadingEdge(height: Dp = 16.dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val h = height.toPx()
        if (h > 0) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startY = size.height - h,
                    endY = size.height
                ),
                topLeft = Offset(0f, size.height - h),
                size = Size(size.width, h),
                blendMode = BlendMode.DstOut
            )
        }
    }

/** Fades content out at both the top and bottom edges based on scroll state. */
fun Modifier.verticalFadingEdges(
    scrollState: ScrollState? = null,
    height: Dp = 16.dp
): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val h = height.toPx()
        val showTop = scrollState == null || scrollState.canScrollBackward
        val showBottom = scrollState == null || scrollState.canScrollForward

        if (showTop && h > 0) {
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
        if (showBottom && h > 0) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startY = size.height - h,
                    endY = size.height
                ),
                topLeft = Offset(0f, size.height - h),
                size = Size(size.width, h),
                blendMode = BlendMode.DstOut
            )
        }
    }

/** Fades content out at both the top and bottom edges based on LazyListState. */
fun Modifier.verticalFadingEdges(
    lazyListState: LazyListState,
    height: Dp = 16.dp
): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val h = height.toPx()
        val showTop = lazyListState.canScrollBackward
        val showBottom = lazyListState.canScrollForward

        if (showTop && h > 0) {
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
        if (showBottom && h > 0) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startY = size.height - h,
                    endY = size.height
                ),
                topLeft = Offset(0f, size.height - h),
                size = Size(size.width, h),
                blendMode = BlendMode.DstOut
            )
        }
    }

/** Fades content out over [width] at both the left and right edges. */
fun Modifier.horizontalFadingEdges(width: Dp = 16.dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val w = width.toPx()
        if (w > 0) {
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
    }

/** Fades content out at both the left and right edges based on scroll state. */
fun Modifier.horizontalFadingEdges(
    scrollState: ScrollState,
    width: Dp = 16.dp
): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val w = width.toPx()
        val showStart = scrollState.canScrollBackward
        val showEnd = scrollState.canScrollForward

        if (showStart && w > 0) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startX = 0f,
                    endX = w
                ),
                size = Size(w, size.height),
                blendMode = BlendMode.DstOut
            )
        }
        if (showEnd && w > 0) {
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
    }
