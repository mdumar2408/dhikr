package com.dhikr.app.design.dotmatrix

import androidx.core.graphics.createBitmap
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import kotlin.math.ceil

/**
 * Turns arbitrary text — especially Arabic — into a [DotGrid] via the "raster path":
 *
 *  1. Android's text stack (StaticLayout → HarfBuzz) shapes the text, so Arabic letters
 *     take their joined, contextual forms and run right-to-left.
 *  2. The shaped text is drawn into an ALPHA_8 bitmap at `rows × oversample` pixels tall.
 *  3. Each oversample×oversample block becomes one dot, lit if its average coverage ≥ [threshold].
 *
 * Never map Arabic code points to per-letter glyphs — that breaks joining (spec §8).
 * This is CPU work: call it off the main thread or cache the result (see rememberRasterGrid).
 */
object DotMatrixRenderer {

    fun rasterize(
        text: String,
        rows: Int,
        typeface: Typeface = Typeface.DEFAULT,
        oversample: Int = 6,
        threshold: Float = 0.45f,
        rtl: Boolean = text.any { Character.getDirectionality(it) == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC },
    ): DotGrid {
        if (text.isBlank() || rows <= 0) return DotGrid.Empty
        val heightPx = rows * oversample
        val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            color = Color.WHITE
            // Fit the font's full ascent+descent (Arabic diacritics sit high and low) into heightPx.
            textSize = heightPx.toFloat()
            val fm = fontMetrics
            textSize *= heightPx / (fm.descent - fm.ascent)
        }
        val textWidth = ceil(paint.measureText(text)).toInt().coerceAtLeast(1)
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, textWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(if (rtl) TextDirectionHeuristics.RTL else TextDirectionHeuristics.LTR)
            .setIncludePad(false)
            .setMaxLines(1)
            .build()

        val cols = ceil(textWidth / oversample.toFloat()).toInt()
        val bmp = createBitmap(cols * oversample, heightPx, Bitmap.Config.ALPHA_8)
        try {
            val canvas = Canvas(bmp)
            canvas.translate(0f, (heightPx - layout.height) / 2f)
            layout.draw(canvas)
            return sample(bmp, cols, rows, oversample, threshold)
        } finally {
            bmp.recycle()
        }
    }

    private fun sample(bmp: Bitmap, cols: Int, rows: Int, os: Int, threshold: Float): DotGrid {
        val alpha = IntArray(bmp.width * bmp.height)
        // ALPHA_8 → getPixels returns colours whose alpha channel carries coverage.
        bmp.getPixels(alpha, 0, bmp.width, 0, 0, bmp.width, bmp.height)
        val cells = BooleanArray(cols * rows)
        val block = (os * os).toFloat()
        for (r in 0 until rows) for (c in 0 until cols) {
            var sum = 0
            for (y in r * os until (r + 1) * os) for (x in c * os until (c + 1) * os) {
                sum += alpha[y * bmp.width + x] ushr 24
            }
            cells[r * cols + c] = sum / (255f * block) >= threshold
        }
        return DotGrid(cols, rows, cells)
    }
}
