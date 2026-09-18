package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun QrCodeView(
    content: String,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    darkColor: Color = Color.Black,
    lightColor: Color = Color.White
) {
    val matrixSize = 25
    val grid = remember(content) {
        val matrix = Array(matrixSize) { BooleanArray(matrixSize) }

        // Finder patterns helper
        fun drawFinder(startX: Int, startY: Int) {
            for (r in 0 until 7) {
                for (c in 0 until 7) {
                    val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                    val isCenter = r in 2..4 && c in 2..4
                    matrix[startY + r][startX + c] = isBorder || isCenter
                }
            }
        }

        // 3 corner finder patterns
        drawFinder(0, 0)
        drawFinder(matrixSize - 7, 0)
        drawFinder(0, matrixSize - 7)

        // Timing patterns
        for (i in 8 until matrixSize - 8) {
            matrix[6][i] = (i % 2 == 0)
            matrix[i][6] = (i % 2 == 0)
        }

        // Fill remaining data modules deterministically based on content
        val hash = abs(content.hashCode())
        val seed = if (hash == 0) 1 else hash
        var rng = seed.toLong()

        for (r in 0 until matrixSize) {
            for (c in 0 until matrixSize) {
                val inTopLeft = r < 8 && c < 8
                val inTopRight = r < 8 && c >= matrixSize - 8
                val inBottomLeft = r >= matrixSize - 8 && c < 8
                val isTiming = (r == 6 && c in 8 until matrixSize - 8) || (c == 6 && r in 8 until matrixSize - 8)

                if (!inTopLeft && !inTopRight && !inBottomLeft && !isTiming) {
                    rng = (rng * 1103515245 + 12345) and 0x7fffffff
                    matrix[r][c] = (rng % 2 == 0L)
                }
            }
        }
        matrix
    }

    Canvas(modifier = modifier.size(size)) {
        drawRect(color = lightColor)
        val cellSize = this.size.width / matrixSize
        for (r in 0 until matrixSize) {
            for (c in 0 until matrixSize) {
                if (grid[r][c]) {
                    drawRect(
                        color = darkColor,
                        topLeft = Offset(c * cellSize, r * cellSize),
                        size = Size(cellSize + 0.5f, cellSize + 0.5f)
                    )
                }
            }
        }
    }
}
