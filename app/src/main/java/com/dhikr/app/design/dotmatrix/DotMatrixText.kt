package com.dhikr.app.design.dotmatrix

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dhikr.app.design.theme.DhikrMotion
import com.dhikr.app.design.theme.DhikrTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Draws a [DotGrid] as circles. When [grid] changes, dots cross-fade
 * (on over 80 ms, off over 120 ms) so digits morph instead of jumping — spec §8.
 * Unlit dots are always drawn in [offColor]: the visible grid is the look.
 */
@Composable
fun DotMatrixText(
    grid: DotGrid,
    contentDescription: String,
    modifier: Modifier = Modifier,
    litColor: Color = DhikrTheme.colors.ink,
    offColor: Color = DhikrTheme.colors.dotOff,
    maxPitch: Dp = Dp.Infinity,
    drawOff: Boolean = true,
) {
    var previous by remember { mutableStateOf(grid) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(grid) {
        if (previous != grid) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(DhikrMotion.DOT_OFF_MS, easing = LinearEasing))
            previous = grid
        }
    }
    Canvas(modifier.semantics { this.contentDescription = contentDescription }) {
        val maxPitchPx = if (maxPitch == Dp.Infinity) Float.MAX_VALUE else maxPitch.toPx()
        val g = DotGeometry.fit(grid.cols, grid.rows, size.width, size.height, maxPitch = maxPitchPx)
        val t = progress.value
        val onAlpha = (t * DhikrMotion.DOT_OFF_MS / DhikrMotion.DOT_ON_MS).coerceAtMost(1f)
        val prev = previous.takeIf { it.cols == grid.cols && it.rows == grid.rows }
        for (r in 0 until grid.rows) for (c in 0 until grid.cols) {
            val now = grid[c, r]
            val was = prev?.get(c, r) ?: now
            val alpha = when {
                now && was -> 1f
                now -> onAlpha
                was -> 1f - t
                else -> 0f
            }
            val center = Offset(g.centerX(c), g.centerY(r))
            if (drawOff && alpha < 1f) drawCircle(offColor, g.radius, center)
            if (alpha > 0f) drawCircle(litColor.copy(alpha = litColor.alpha * alpha), g.radius, center)
        }
    }
}

/** Convenience for Latin/digit text in the built-in 5×7 font. */
@Composable
fun DotMatrixLabel(
    text: String,
    modifier: Modifier = Modifier,
    litColor: Color = DhikrTheme.colors.ink,
    offColor: Color = DhikrTheme.colors.dotOff,
    maxPitch: Dp = 6.dp,
    drawOff: Boolean = false,
) {
    val grid = remember(text) { DotGrid.text(text) }
    DotMatrixText(grid, text, modifier, litColor, offColor, maxPitch, drawOff)
}

/**
 * Arabic (or any shaped text) as dots. Rasterises off the main thread and caches per text,
 * because shaping + sampling is too expensive to repeat per frame.
 */
@Composable
fun rememberRasterGrid(text: String, rows: Int): DotGrid {
    val grid by produceState(DotGrid.Empty, text, rows) {
        value = withContext(Dispatchers.Default) { DotMatrixRenderer.rasterize(text, rows) }
    }
    return grid
}
