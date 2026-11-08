package com.dhikr.app.design.dotmatrix

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
        val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
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
}
