package com.dhikr.app.domain.history

import com.dhikr.app.core.model.CountContext
import com.dhikr.app.core.model.Dhikr
import com.dhikr.app.domain.flow.FlowState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class StreakCalculatorTest {
    private val today = LocalDate.parse("2026-11-08")
    private fun days(vararg offsets: Long) = offsets.map { today.minusDays(it) }.toSet()

    @Test
    fun `empty history gives zeros`() {
        assertEquals(Streaks(0, 0, 0), StreakCalculator.compute(emptySet(), today))
    }

    @Test
    fun `today complete counts`() {
        assertEquals(Streaks(3, 3, 3), StreakCalculator.compute(days(0, 1, 2), today))
    }

    @Test
    fun `today not yet complete keeps yesterday's streak alive`() {
        assertEquals(2, StreakCalculator.compute(days(1, 2), today).current)
    }

    @Test
    fun `a missed day breaks the streak but longest remembers`() {
        val s = StreakCalculator.compute(days(0, 1, 3, 4, 5, 6), today) // gap on day 2
        assertEquals(2, s.current)
        assertEquals(4, s.longest)
        assertEquals(6, s.totalCompletedDays)
    }

    @Test
    fun `missing yesterday and today means no current streak`() {
        assertEquals(0, StreakCalculator.compute(days(2, 3), today).current)
    }

    @Test
    fun `DST change does not create or lose a day`() {
        // Europe/London clocks go back on 2026-10-25: the local dates are still consecutive.
        val zone = ZoneId.of("Europe/London")
        val d1 = ZonedDateTime.of(2026, 10, 24, 23, 30, 0, 0, zone).toLocalDate()
        val d2 = ZonedDateTime.of(2026, 10, 25, 23, 30, 0, 0, zone).toLocalDate()
        val d3 = ZonedDateTime.of(2026, 10, 26, 0, 30, 0, 0, zone).toLocalDate()
        assertEquals(3, StreakCalculator.compute(setOf(d1, d2, d3), d3).current)
    }

    @Test
    fun `future dates are ignored`() {
        assertEquals(Streaks(1, 1, 1), StreakCalculator.compute(setOf(today, today.plusDays(1)), today))
    }
}

class DaySummaryPolicyTest {
    private val date = LocalDate.parse("2026-11-08")
    private fun d(id: Long, target: Int, enabled: Boolean = true) = Dhikr(id, "ذ", "D$id", "", target, enabled, id.toInt())

    private fun sessions(prayerCounts: Map<Long, Int>?, daily: List<Dhikr>, dailyCounts: Map<Long, Int>): Map<CountContext, FlowState> {
        val set = listOf(d(1, 3), d(2, 33))
        return CountContext.entries.associateWith { ctx ->
            if (ctx.isPrayer) FlowState(if (prayerCounts == null) emptyList() else set, counts = prayerCounts.orEmpty(), loops = false)
            else FlowState(daily, counts = dailyCounts)
        }
    }

    @Test
    fun `complete needs all five prayers and all daily targets`() {
        val daily = listOf(d(5, 100))
        val all = mapOf(1L to 3, 2L to 33)
        assertTrue(DaySummaryPolicy.summarize(date, sessions(all, daily, mapOf(5L to 100))).isCompleted)
        assertFalse(DaySummaryPolicy.summarize(date, sessions(all, daily, mapOf(5L to 99))).isCompleted)
        assertFalse(DaySummaryPolicy.summarize(date, sessions(mapOf(1L to 3, 2L to 32), daily, mapOf(5L to 100))).isCompleted)
    }

    @Test
    fun `no daily targets - prayers alone decide`() {
        val s = DaySummaryPolicy.summarize(date, sessions(mapOf(1L to 3, 2L to 33), emptyList(), emptyMap()))
        assertTrue(s.isCompleted)
        assertEquals(5, s.prayersCompleted)
    }

    @Test
    fun `nothing set up is never a completed day`() {
        assertFalse(DaySummaryPolicy.summarize(date, sessions(null, emptyList(), emptyMap())).isCompleted)
    }

    @Test
    fun `daily progress is capped per dhikr`() {
        val daily = listOf(d(5, 10), d(6, 10))
        val s = DaySummaryPolicy.summarize(date, sessions(null, daily, mapOf(5L to 50, 6L to 0)))
        assertEquals(10, s.totalCompleted)
        assertEquals(1, s.completedDhikrCount)
        assertFalse(s.isCompleted)
        assertTrue(s.hasActivity)
    }

    @Test
    fun `prayer mask records which prayers`() {
        val set = listOf(d(1, 3))
        val sessions = CountContext.entries.associateWith { ctx ->
            when (ctx) {
                CountContext.Fajr, CountContext.Maghrib -> FlowState(set, counts = mapOf(1L to 3), loops = false)
                CountContext.Daily -> FlowState(emptyList())
                else -> FlowState(set, loops = false)
            }
        }
        val s = DaySummaryPolicy.summarize(date, sessions)
        assertTrue(s.prayerComplete(CountContext.Fajr))
        assertTrue(s.prayerComplete(CountContext.Maghrib))
        assertFalse(s.prayerComplete(CountContext.Asr))
        assertEquals(2, s.prayersCompleted)
    }
}
