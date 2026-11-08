package com.dhikr.app.design.dotmatrix

/**
 * An immutable grid of dots: the common currency between the font, the Arabic rasteriser,
 * the Compose renderer and widget bitmaps. Pure Kotlin.
 */
class DotGrid(val cols: Int, val rows: Int, private val cells: BooleanArray) {
    init {
        require(cols >= 0 && rows >= 0) { "negative size" }
        require(cells.size == cols * rows) { "cells (${cells.size}) != $cols×$rows" }
    }

    operator fun get(col: Int, row: Int): Boolean = cells[row * cols + col]

    val litCount: Int get() = cells.count { it }

    override fun equals(other: Any?): Boolean =
        other is DotGrid && cols == other.cols && rows == other.rows && cells.contentEquals(other.cells)

    override fun hashCode(): Int = 31 * (31 * cols + rows) + cells.contentHashCode()

    override fun toString(): String = buildString {
        for (r in 0 until rows) {
            for (c in 0 until cols) append(if (get(c, r)) '●' else '·')
            if (r < rows - 1) append('\n')
        }
    }

    companion object {
        val Empty = DotGrid(0, 0, BooleanArray(0))

        /**
         * Lays out [text] with [DotMatrixFont], [gap] blank columns between glyphs.
         * Characters missing from the font render as blank cells (never crash on user input).
         */
        fun text(text: String, gap: Int = 1): DotGrid {
            if (text.isEmpty()) return Empty
            val w = DotMatrixFont.WIDTH
            val h = DotMatrixFont.HEIGHT
            val cols = text.length * w + (text.length - 1) * gap
            val cells = BooleanArray(cols * h)
            text.forEachIndexed { i, ch ->
                val glyph = DotMatrixFont.glyph(ch) ?: return@forEachIndexed
                val x0 = i * (w + gap)
                for (cell in glyph.indices) {
                    if (glyph[cell]) cells[(cell / w) * cols + x0 + cell % w] = true
                }
            }
            return DotGrid(cols, h, cells)
        }
    }
}

/** Where to draw each dot for a grid fitted into a box. Shared by Compose drawing and bitmaps. */
data class DotGeometry(val pitch: Float, val radius: Float, val originX: Float, val originY: Float) {
    fun centerX(col: Int) = originX + (col + 0.5f) * pitch
    fun centerY(row: Int) = originY + (row + 0.5f) * pitch

    companion object {
        /**
         * Largest square pitch that fits [cols]×[rows] into [width]×[height], centred.
         * [dotRatio] = dot diameter / pitch (spec: pitch = 1.5 × diameter → 0.667).
         * [maxPitch] caps the size so short strings don't become huge.
         */
        fun fit(
            cols: Int, rows: Int, width: Float, height: Float,
            dotRatio: Float = 2f / 3f, maxPitch: Float = Float.MAX_VALUE,
        ): DotGeometry {
            if (cols == 0 || rows == 0) return DotGeometry(0f, 0f, 0f, 0f)
            val pitch = minOf(width / cols, height / rows, maxPitch)
            return DotGeometry(
                pitch = pitch,
                radius = pitch * dotRatio / 2f,
                originX = (width - cols * pitch) / 2f,
                originY = (height - rows * pitch) / 2f,
            )
        }
    }
}
