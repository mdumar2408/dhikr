package com.dhikr.app.widget

import com.dhikr.app.core.model.AfterTarget
import com.dhikr.app.core.model.CountContext
import com.dhikr.app.core.model.Dhikr
import com.dhikr.app.data.progress.EngineState
import com.dhikr.app.domain.flow.FlowState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** Widgets are tested apart from screens (android-widget-engineering). */
class WidgetModelTest {
    private val a = Dhikr(1, "سبحان الله", "SubhanAllah", "", 33, true, 0)
    private val b = Dhikr(2, "الحمد لله", "Alhamdulillah", "", 33, true, 1, audioRef = "audio/x.m4a")

    private fun state(counts: Map<Long, Int>, library: List<Dhikr> = listOf(a, b)) = EngineState(
        date = LocalDate.parse("2026-11-08"),
        sessions = CountContext.entries.associateWith { ctx ->
            if (ctx == CountContext.Daily) FlowState(library, counts = counts, loaded = true) else FlowState(emptyList(), loaded = true, loops = false)
        },
        countsLoaded = true,
    )

    @Test
    fun `follows flow like the counter`() {
        val m = WidgetModel.project(state(mapOf(1L to 33, 2L to 5)), AfterTarget.NextDhikr, pinnedId = null)
        assertEquals("Alhamdulillah", m.title)
        assertEquals(5, m.count)
        assertEquals("Flow · 2/2", m.modeLabel)
        assertTrue(m.hasAudio)
    }

    @Test
    fun `pinned shows that dhikr even after its target`() {
        val m = WidgetModel.project(state(mapOf(1L to 40)), AfterTarget.NextDhikr, pinnedId = 1)
        assertEquals("SubhanAllah", m.title)
        assertEquals(40, m.count)
        assertTrue(m.complete)
        assertEquals("Pinned", m.modeLabel)
    }

    @Test
    fun `a pinned dhikr that was disabled falls back to flow`() {
        val m = WidgetModel.project(state(emptyMap(), listOf(a.copy(isEnabled = false), b)), AfterTarget.NextDhikr, pinnedId = 1)
        assertEquals("Alhamdulillah", m.title)
        assertFalse(m.empty)
    }

    @Test
    fun `no daily routine shows the empty state`() {
        assertTrue(WidgetModel.project(state(emptyMap(), emptyList()), AfterTarget.NextDhikr, null).empty)
    }

    @Test
    fun `round 2 shows the count within the round`() {
        val m = WidgetModel.project(state(mapOf(1L to 34, 2L to 33)), AfterTarget.NextDhikr, null)
        assertEquals("SubhanAllah", m.title)
        assertEquals(1, m.count)
    }
}

class MidnightRefreshTest {
    @Test
    fun `delay runs to one minute past the next local midnight`() {
        val now = ZonedDateTime.of(2026, 11, 8, 23, 0, 0, 0, ZoneId.of("Asia/Kolkata"))
        assertEquals(Duration.ofMinutes(61), MidnightRefresh.delayUntilNextMidnight(now))
    }

    @Test
    fun `DST night is handled by the zone (23h day)`() {
        // Europe/London springs forward on 2026-03-29 01:00 → 02:00.
        val now = ZonedDateTime.of(2026, 3, 28, 12, 0, 0, 0, ZoneId.of("Europe/London"))
        assertEquals(Duration.ofHours(12).plusMinutes(1), MidnightRefresh.delayUntilNextMidnight(now))
    }
}
