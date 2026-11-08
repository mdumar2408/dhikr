package com.dhikr.app.design.dotmatrix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DotMatrixTest {

    @Test
    fun `every glyph is exactly 5x7 and printable glyphs are not blank`() {
        for (c in DotMatrixFont.supportedChars) {
            val g = assertNotNull(DotMatrixFont.glyph(c)).let { DotMatrixFont.glyph(c)!! }
            assertEquals("glyph '$c'", 35, g.size)
            if (c != ' ') assertTrue("glyph '$c' is blank", g.any { it })
        }
    }

    @Test
    fun `digits 0-9 are pairwise distinct`() {
        val digits = ('0'..'9').map { DotMatrixFont.glyph(it)!!.toList() }
        assertEquals(10, digits.toSet().size)
    }

    @Test
    fun `lowercase maps to uppercase and unknown chars are null`() {
        assertTrue(DotMatrixFont.glyph('a')!!.contentEquals(DotMatrixFont.glyph('A')!!))
        assertNull(DotMatrixFont.glyph('ع'))
    }

    @Test
    fun `text grid width is chars x 5 plus gaps`() {
        val g = DotGrid.text("123")
        assertEquals(3 * 5 + 2, g.cols)
        assertEquals(7, g.rows)
        // the gap column between glyphs is always unlit
        for (r in 0 until 7) assertFalse(g[5, r])
    }

    @Test
    fun `unknown characters render blank instead of crashing`() {
        val g = DotGrid.text("1ع1")
        val blankCol0 = 6 // second glyph starts at column 6
        for (r in 0 until 7) for (c in blankCol0 until blankCol0 + 5) assertFalse(g[c, r])
    }

    @Test
    fun `empty text gives empty grid`() {
        assertEquals(DotGrid.Empty, DotGrid.text(""))
    }

    @Test
    fun `geometry fits and centres the grid`() {
        val g = DotGeometry.fit(cols = 10, rows = 5, width = 200f, height = 50f)
        assertEquals(10f, g.pitch, 0.001f) // limited by height: 50 / 5
        assertEquals(50f, g.originX, 0.001f) // (200 - 100) / 2
        assertEquals(0f, g.originY, 0.001f)
        assertEquals(10f / 3f, g.radius, 0.001f) // diameter = 2/3 pitch
    }

    @Test
    fun `geometry respects max pitch`() {
        val g = DotGeometry.fit(cols = 2, rows = 2, width = 1000f, height = 1000f, maxPitch = 8f)
        assertEquals(8f, g.pitch, 0.001f)
    }
}
