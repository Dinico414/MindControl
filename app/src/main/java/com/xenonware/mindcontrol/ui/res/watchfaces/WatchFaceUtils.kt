package com.xenonware.mindcontrol.ui.res.watchfaces

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.min

internal val PIXEL_PATTERNS = arrayOf(
    intArrayOf(0x7, 0x5, 0x5, 0x5, 0x7), // 0
    intArrayOf(0x6, 0x2, 0x2, 0x2, 0x7), // 1
    intArrayOf(0x7, 0x1, 0x7, 0x4, 0x7), // 2
    intArrayOf(0x7, 0x1, 0x7, 0x1, 0x7), // 3
    intArrayOf(0x5, 0x5, 0x7, 0x1, 0x1), // 4
    intArrayOf(0x7, 0x4, 0x7, 0x1, 0x7), // 5
    intArrayOf(0x7, 0x4, 0x7, 0x5, 0x7), // 6
    intArrayOf(0x7, 0x1, 0x3, 0x6, 0x4), // 7
    intArrayOf(0x7, 0x5, 0x7, 0x5, 0x7), // 8
    intArrayOf(0x7, 0x5, 0x7, 0x1, 0x7)  // 9
)

internal fun DrawScope.drawPixelDigit(digit: Int, offset: Offset, width: Float, height: Float, color: Color, alpha: Float) {
    val pattern = PIXEL_PATTERNS[digit.coerceIn(0, 9)]
    val cellSize = min(width / 3f, height / 5f)
    val startX = offset.x + (width - cellSize * 3f) / 2f
    val startY = offset.y + (height - cellSize * 5f) / 2f
    
    val padding = cellSize * 0.08f
    val innerSize = cellSize - padding * 2

    for (row in 0 until 5) {
        val rowBits = pattern[row]
        for (col in 0 until 3) {
            val bit = (rowBits shr (2 - col)) and 1
            if (bit == 1) {
                val x1 = startX + col * cellSize + padding
                val y1 = startY + row * cellSize + padding
                val x2 = x1 + innerSize
                val y2 = y1 + innerSize

                // Default Beveling: outer corners of the 3x5 bounding box are "cut"
                // Meaning the right angle faces the INSIDE.
                var type = when {
                    col == 0 && row == 0 -> "br"
                    col == 2 && row == 0 -> "bl"
                    col == 0 && row == 4 -> "tr"
                    col == 2 && row == 4 -> "tl"
                    else -> "full"
                }

                // Digit specific overrides
                when (digit) {
                    1 -> {
                        if (col == 0 && row == 0) type = "br"
                        if (col == 0 && row == 4) type = "br"
                        if (col == 2 && row == 4) type = "bl"
                    }
                    2 -> {
                        if (col == 0 && row == 2) type = "br"
                        if (col == 2 && row == 2) type = "tl"
                    }
                    3 -> {
                        if (col == 0 && row == 2) type = "tr"
                        if (col == 2 && row == 2) type = "bl"
                    }
                    4 -> {
                        if (col == 0 && row == 0) type = "bl"
                        if (col == 0 && row == 2) type = "tr"
                    }
                    5 -> {
                        if (col == 0 && row == 0) type = "full"
                        if (col == 2 && row == 0) type = "tl"
                        if (col == 2 && row == 2) type = "bl"
                    }
                    6 -> {
                        if (col == 2 && row == 2) type = "bl"
                    }
                    7 -> {
                        if (col == 0 && row == 0) type = "full"
                        if (col == 2 && row == 2) type = "tl"
                        if (col == 1 && row == 2) type = "br"
                        if (col == 1 && row == 3) type = "tl"
                        if (col == 0 && row == 3) type = "br"
                        if (col == 0 && row == 4) type = "full"
                    }
                    8 -> {
                        if (col == 0 && row == 2) type = "tr"
                        if (col == 2 && row == 2) type = "bl"
                    }
                    9 -> {
                        if (col == 0 && row == 2) type = "tr"
                        if (col == 0 && row == 4) type = "tr"
                        if (col == 1 && row == 4) type = "full"
                        if (col == 2 && row == 4) type = "tl"
                    }
                }

                if (type != "full") {
                    val p = Path().apply {
                        when (type) {
                            "tl" -> {
                                moveTo(x1, y1)
                                lineTo(x2, y1)
                                lineTo(x1, y2)
                            }
                            "tr" -> {
                                moveTo(x2, y1)
                                lineTo(x1, y1)
                                lineTo(x2, y2)
                            }
                            "bl" -> {
                                moveTo(x1, y2)
                                lineTo(x1, y1)
                                lineTo(x2, y2)
                            }
                            "br" -> {
                                moveTo(x2, y2)
                                lineTo(x2, y1)
                                lineTo(x1, y2)
                            }
                        }
                        close()
                    }
                    drawPath(p, color, alpha)
                } else {
                    drawRect(
                        color = color,
                        topLeft = Offset(x1, y1),
                        size = Size(innerSize, innerSize),
                        alpha = alpha
                    )
                }
            }
        }
    }
}

internal val NOTHING_DOT_PATTERNS = arrayOf(
    intArrayOf(0x6, 0x9, 0x9, 0x9, 0x9, 0x9, 0x6), // 0
    intArrayOf(0x4, 0x4, 0x4, 0x4, 0x4, 0x4, 0x4), // 1
    intArrayOf(0xE, 0x1, 0x1, 0x6, 0x8, 0x8, 0xF), // 2
    intArrayOf(0xE, 0x1, 0x1, 0x6, 0x1, 0x1, 0xE), // 3
    intArrayOf(0x9, 0x9, 0x9, 0xF, 0x1, 0x1, 0x1), // 4
    intArrayOf(0xF, 0x8, 0x8, 0xE, 0x1, 0x1, 0xE), // 5
    intArrayOf(0x6, 0x8, 0x8, 0xE, 0x9, 0x9, 0x6), // 6
    intArrayOf(0xF, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1), // 7
    intArrayOf(0x6, 0x9, 0x9, 0x6, 0x9, 0x9, 0x6), // 8
    intArrayOf(0x6, 0x9, 0x9, 0x7, 0x1, 0x1, 0x6)  // 9
)

internal val DIGITAL_SEGMENTS = intArrayOf(
    0x3F, 0x06, 0x5B, 0x4F, 0x66, 0x6D, 0x7D, 0x07, 0x7F, 0x6F
)

internal fun DrawScope.drawNothingDigit(digit: Int, offset: Offset, width: Float, height: Float, color: Color, alpha: Float) {
    val pattern = NOTHING_DOT_PATTERNS[digit.coerceIn(0, 9)]
    
    // Use a uniform cell size to ensure horizontal and vertical spacing are identical.
    val cellSize = min(width / 4f, height / 7f)
    
    // Center the 4x7 grid within the allocated width/height.
    val startX = offset.x + (width - cellSize * 4f) / 2f
    val startY = offset.y + (height - cellSize * 7f) / 2f
    
    // Dot radius relative to the cell size.
    val dotRadius = cellSize * 0.38f
    
    for (row in 0 until 7) {
        val rowBits = pattern[row]
        for (col in 0 until 4) {
            val bit = (rowBits shr (3 - col)) and 1
            if (bit == 1) {
                val cx = startX + col * cellSize + cellSize / 2f
                val cy = startY + row * cellSize + cellSize / 2f
                drawCircle(color, dotRadius, Offset(cx, cy), alpha)
            }
        }
    }
}

internal fun DrawScope.drawDigitalDigit(digit: Int, offset: Offset, width: Float, height: Float, color: Color, alpha: Float) {
    val segments = DIGITAL_SEGMENTS[digit.coerceIn(0, 9)]
    val t = width * 0.18f // thickness
    val r = t / 2f       // point depth
    val g = t / 4f       // Gap that ensures 45-degree alignment (r = 2g is the meeting distance)
    
    fun drawSeg(pts: List<Offset>) {
        val p = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
            close()
        }
        drawPath(p, color, alpha)
    }

    val x = offset.x
    val y = offset.y
    val mh = y + height / 2f

    // Standard meeting points for a 7-segment display
    val p1 = Offset(x + r, y + r)      // Top-Left
    val p2 = Offset(x + width - r, y + r)  // Top-Right
    val p3 = Offset(x + r, mh)         // Mid-Left
    val p4 = Offset(x + width - r, mh)     // Mid-Right
    val p5 = Offset(x + r, y + height - r)  // Bottom-Left
    val p6 = Offset(x + width - r, y + height - r) // Bottom-Right

    // Offset based on gap to make horizontal segments shorter for alignment
    val hg = g * 1.5f 

    // --- Horizontal Segments (A, G, D) ---

    // Segment A (Top)
    if ((segments and 0x01) != 0) {
        val ly = y + r
        drawSeg(listOf(
            Offset(p1.x + hg, ly),     // Left Point
            Offset(p1.x + hg + r, y),  // Top-Left
            Offset(p2.x - hg - r, y),  // Top-Right
            Offset(p2.x - hg, ly),     // Right Point
            Offset(p2.x - hg - r, y + t), // Bottom-Right
            Offset(p1.x + hg + r, y + t)  // Bottom-Left
        ))
    }

    // Segment G (Middle)
    if ((segments and 0x40) != 0) {
        drawSeg(listOf(
            Offset(p3.x + hg, mh),     // Left Point
            Offset(p3.x + hg + r, mh - r), // Top-Left
            Offset(p4.x - hg - r, mh - r), // Top-Right
            Offset(p4.x - hg, mh),     // Right Point
            Offset(p4.x - hg - r, mh + r), // Bottom-Right
            Offset(p3.x + hg + r, mh + r)  // Bottom-Left
        ))
    }

    // Segment D (Bottom)
    if ((segments and 0x08) != 0) {
        val ly = y + height - r
        drawSeg(listOf(
            Offset(p5.x + hg, ly),     // Left Point
            Offset(p5.x + hg + r, y + height - t), // Top-Left
            Offset(p6.x - hg - r, y + height - t), // Top-Right
            Offset(p6.x - hg, ly),     // Right Point
            Offset(p6.x - hg - r, y + height), // Bottom-Right
            Offset(p5.x + hg + r, y + height)  // Bottom-Left
        ))
    }

    // --- Vertical Segments (F, B, E, C) ---

    // Segment F (Top Left)
    if ((segments and 0x20) != 0) {
        val lx = x + r
        drawSeg(listOf(
            Offset(lx, p1.y + g),           // Top Point
            Offset(x + t, p1.y + g + r),    // Top-Right
            Offset(x + t, p3.y - g - r),    // Bottom-Right
            Offset(lx, p3.y - g),           // Bottom Point
            Offset(x, p3.y - g - r),        // Bottom-Left
            Offset(x, p1.y + g + r)         // Top-Left
        ))
    }

    // Segment B (Top Right)
    if ((segments and 0x02) != 0) {
        val lx = x + width - r
        drawSeg(listOf(
            Offset(lx, p2.y + g),           // Top Point
            Offset(x + width, p2.y + g + r),    // Top-Right
            Offset(x + width, p4.y - g - r),    // Bottom-Right
            Offset(lx, p4.y - g),           // Bottom Point
            Offset(x + width - t, p4.y - g - r), // Bottom-Left
            Offset(x + width - t, p2.y + g + r)  // Top-Left
        ))
    }

    // Segment E (Bottom Left)
    if ((segments and 0x10) != 0) {
        val lx = x + r
        drawSeg(listOf(
            Offset(lx, p3.y + g),           // Top Point
            Offset(x + t, p3.y + g + r),    // Top-Right
            Offset(x + t, p5.y - g - r),    // Bottom-Right
            Offset(lx, p5.y - g),           // Bottom Point
            Offset(x, p5.y - g - r),        // Bottom-Left
            Offset(x, p3.y + g + r)         // Top-Left
        ))
    }

    // Segment C (Bottom Right)
    if ((segments and 0x04) != 0) {
        val lx = x + width - r
        drawSeg(listOf(
            Offset(lx, p4.y + g),           // Top Point
            Offset(x + width, p4.y + g + r),    // Top-Right
            Offset(x + width, p6.y - g - r),    // Bottom-Right
            Offset(lx, p6.y - g),           // Bottom Point
            Offset(x + width - t, p6.y - g - r), // Bottom-Left
            Offset(x + width - t, p4.y + g + r)  // Top-Left
        ))
    }
}
