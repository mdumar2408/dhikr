package com.dhikr.app.design.dotmatrix

import androidx.core.graphics.createBitmap
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

/**
 * Renders a [DotGrid] into a bitmap for surfaces that can't run Compose drawing:
 * Glance widgets (Phase 6) and notifications. Uses the same [DotGeometry] math as
 * [DotMatrixText], so widgets and the app look identical.
 */
object DotMatrixBitmap {
    fun render(
        grid: DotGrid,
        widthPx: Int,
        heightPx: Int,
        litArgb: Int,
        offArgb: Int?,
        dotRatio: Float = 2f / 3f,
    ): Bitmap {
        require(widthPx > 0 && heightPx > 0) { "bitmap size must be positive" }
        val bmp = createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val g = DotGeometry.fit(grid.cols, grid.rows, widthPx.toFloat(), heightPx.toFloat(), dotRatio)
        val lit = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = litArgb }
        val off = offArgb?.let { c -> Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c } }
        for (r in 0 until grid.rows) for (c in 0 until grid.cols) {
            val paint = if (grid[c, r]) lit else off ?: continue
            canvas.drawCircle(g.centerX(c), g.centerY(r), g.radius, paint)
        }
        return bmp
    }

    /**
     * 1-byte-per-pixel mask of either the lit or the unlit dots (Phase 6 widgets). Tinted in the
     * widget with a day/night ColorProvider, so one mask serves both themes and stays far below the
     * RemoteViews bitmap memory cap (4× smaller than ARGB_8888).
     */
    fun renderMask(grid: DotGrid, widthPx: Int, heightPx: Int, lit: Boolean, dotRatio: Float = 2f / 3f): Bitmap {
        require(widthPx > 0 && heightPx > 0) { "bitmap size must be positive" }
        val bmp = createBitmap(widthPx, heightPx, Bitmap.Config.ALPHA_8)
        val canvas = Canvas(bmp)
        val g = DotGeometry.fit(grid.cols, grid.rows, widthPx.toFloat(), heightPx.toFloat(), dotRatio)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = 255 }
        for (r in 0 until grid.rows) for (c in 0 until grid.cols) {
            if (grid[c, r] == lit) canvas.drawCircle(g.centerX(c), g.centerY(r), g.radius, paint)
        }
        return bmp
    }
}
