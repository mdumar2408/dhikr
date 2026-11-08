package com.dhikr.app.notification

import com.dhikr.app.core.model.LockScreenMode
import com.dhikr.app.widget.WidgetMode
import com.dhikr.app.widget.WidgetModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockScreenContentTest {
    private fun model(
        ctx: String = "ASR", title: String = "SubhanAllah", count: Int = 12, target: Int = 33,
        mode: String = "Asr · 3/6", previous: String? = null, empty: Boolean = false, message: String = "",
    ) = WidgetModel(ctx, title, "", count, target, false, mode, false, previous, empty, message)

    @Test
    fun `title says where taps go, then the dhikr`() {
        val c = LockScreenContent.from(model())
        assertEquals("Asr · SubhanAllah", c.title)
        assertEquals("12 / 33  ·  Asr · 3/6", c.text)
        assertTrue(c.countable)
    }

    @Test
    fun `daily mode is labelled Daily`() {
        assertEquals("Daily · SubhanAllah", LockScreenContent.from(model(ctx = "DAILY", mode = "Flow · 1/4")).title)
    }

    @Test
    fun `after finishing a prayer it confirms it`() {
        val c = LockScreenContent.from(model(ctx = "DHUHR", count = 0, target = 3, previous = "✓ FAJR"))
        assertEquals("Dhuhr · SubhanAllah", c.title)
        assertEquals("0 / 3  ·  ✓ fajr done", c.text)
    }

    @Test
    fun `all prayers done is not countable`() {
        val c = LockScreenContent.from(model(ctx = "PRAYER", empty = true, message = "All five prayers complete"))
        assertFalse(c.countable)
        assertEquals("All five prayers complete", c.text)
    }

    @Test
    fun `settings map to the same modes as the widget`() {
        assertEquals(WidgetMode.Prayer, LockScreenContent.modeOf(LockScreenMode.Prayer))
        assertEquals(WidgetMode.Daily, LockScreenContent.modeOf(LockScreenMode.Daily))
    }
}
