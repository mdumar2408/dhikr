package com.dhikr.app.widget

import com.dhikr.app.core.model.AfterTarget
import com.dhikr.app.core.model.CountContext
import com.dhikr.app.core.model.Dhikr
import com.dhikr.app.data.progress.EngineState
import com.dhikr.app.domain.flow.FlowState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    private val istighfar = Dhikr(3, "أستغفر الله", "Astaghfirullah", "", 3, true, 0)

    private fun state(
        daily: Map<Long, Int> = emptyMap(),
        library: List<Dhikr> = listOf(a, b),
        prayerCounts: Map<CountContext, Map<Long, Int>> = emptyMap(),
        // As the engine builds it: prayer-set targets and positions as sortOrder.
        prayerSet: List<Dhikr> = listOf(istighfar.copy(sortOrder = 0), a.copy(target = 2, sortOrder = 1)),
    ) = EngineState(
        date = LocalDate.parse("2026-11-08"),
        sessions = CountContext.entries.associateWith { ctx ->
            if (ctx == CountContext.Daily) FlowState(library, counts = daily, loaded = true)
            else FlowState(prayerSet, counts = prayerCounts[ctx].orEmpty(), loaded = true, loops = false)
        },
        countsLoaded = true,
    )

    // ---- Daily -------------------------------------------------------------------------------

    @Test
    fun `daily mode says DAILY and follows flow like the counter`() {
        val m = WidgetModel.project(state(daily = mapOf(1L to 33, 2L to 5)), AfterTarget.NextDhikr, WidgetMode.Daily)
        assertEquals("DAILY", m.contextLabel)
        assertEquals("Alhamdulillah", m.title)
        assertEquals(5, m.count)
        assertEquals("Flow · 2/2", m.modeLabel)
        assertTrue(m.hasAudio)
    }

    @Test
    fun `pinned shows that dhikr even after its target, still DAILY`() {
        val m = WidgetModel.project(state(daily = mapOf(1L to 40)), AfterTarget.NextDhikr, WidgetMode.Pinned(1))
        assertEquals("DAILY", m.contextLabel)
        assertEquals("SubhanAllah", m.title)
        assertEquals(40, m.count)
        assertTrue(m.complete)
        assertEquals("Pinned", m.modeLabel)
    }

    @Test
    fun `a pinned dhikr that was disabled falls back to flow`() {
        val m = WidgetModel.project(state(library = listOf(a.copy(isEnabled = false), b)), AfterTarget.NextDhikr, WidgetMode.Pinned(1))
        assertEquals("Alhamdulillah", m.title)
        assertFalse(m.empty)
    }

    @Test
    fun `no daily routine shows the empty state`() {
        assertTrue(WidgetModel.project(state(library = emptyList()), AfterTarget.NextDhikr, WidgetMode.Daily).empty)
    }

    @Test
    fun `round 2 shows the count within the round`() {
        val m = WidgetModel.project(state(daily = mapOf(1L to 34, 2L to 33)), AfterTarget.NextDhikr, WidgetMode.Daily)
        assertEquals(1, m.count)
    }

    // ---- Prayer ------------------------------------------------------------------------------

    @Test
    fun `prayer mode names the prayer and uses the set's own counts`() {
        val m = WidgetModel.project(state(prayerCounts = mapOf(CountContext.Fajr to mapOf(3L to 1))), AfterTarget.Stay, WidgetMode.Prayer)
        assertEquals("FAJR", m.contextLabel)
        assertEquals("Astaghfirullah", m.title)
        assertEquals(1, m.count)
        assertEquals(3, m.target)
        assertEquals("Fajr · 1/2", m.modeLabel)
        assertNull(m.previousDone)
    }

    @Test
    fun `after a prayer completes the widget moves on and confirms it`() {
        val fajrDone = mapOf(CountContext.Fajr to mapOf(3L to 3, 1L to 2))
        val m = WidgetModel.project(state(prayerCounts = fajrDone), AfterTarget.NextDhikr, WidgetMode.Prayer)
        assertEquals("DHUHR", m.contextLabel)
        assertEquals("✓ FAJR", m.previousDone)
        // Once Dhuhr has started, the confirmation goes away.
        val started = fajrDone + (CountContext.Dhuhr to mapOf(3L to 1))
        assertNull(WidgetModel.project(state(prayerCounts = started), AfterTarget.NextDhikr, WidgetMode.Prayer).previousDone)
    }

    @Test
    fun `all five complete is an empty, completed state`() {
        val done = CountContext.prayers.associateWith { mapOf(3L to 3, 1L to 2) }
        val m = WidgetModel.project(state(prayerCounts = done), AfterTarget.NextDhikr, WidgetMode.Prayer)
        assertTrue(m.empty)
        assertTrue(m.complete)
        assertFalse(m.countable)
    }

    @Test
    fun `prayer counts never leak into daily and vice versa`() {
        val s = state(daily = mapOf(3L to 50), library = listOf(istighfar.copy(target = 100)), prayerCounts = emptyMap())
        val prayer = WidgetModel.project(s, AfterTarget.NextDhikr, WidgetMode.Prayer)
        assertEquals(0, prayer.count) // Fajr's istighfar is untouched by the 50 daily ones
        assertEquals(50, WidgetModel.project(s, AfterTarget.NextDhikr, WidgetMode.Daily).count)
    }

    @Test
    fun `mode parsing - pinned wins, prayer, default daily`() {
        assertEquals(WidgetMode.Pinned(7), WidgetMode.from(WidgetMode.PRAYER, 7))
        assertEquals(WidgetMode.Prayer, WidgetMode.from(WidgetMode.PRAYER, null))
        assertEquals(WidgetMode.Daily, WidgetMode.from(null, null))
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
