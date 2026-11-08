package com.dhikr.app.data.progress

import com.dhikr.app.core.model.AfterTarget
import com.dhikr.app.core.model.CountContext
import com.dhikr.app.core.model.Dhikr
import com.dhikr.app.core.model.PrayerSetItem
import com.dhikr.app.domain.flow.FlowEvent
import com.dhikr.app.domain.history.DaySummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** In-memory ProgressStore with the same semantics as the Room one. */
private class FakeStore : ProgressStore {
    val rows = MutableStateFlow<Map<Triple<Long, LocalDate, CountContext>, Pair<Int, Int>>>(emptyMap())
    override fun observeDate(date: LocalDate): Flow<List<ProgressRecord>> = rows.map { m ->
        m.filterKeys { it.second == date }.map { (k, v) -> ProgressRecord(k.first, k.third, v.first, v.second) }
    }
    override suspend fun increment(dhikrId: Long, date: LocalDate, context: CountContext, target: Int) {
        rows.value = rows.value + (Triple(dhikrId, date, context) to ((rows.value[Triple(dhikrId, date, context)]?.first ?: 0) + 1 to target))
    }
    override suspend fun decrement(dhikrId: Long, date: LocalDate, context: CountContext) {
        val k = Triple(dhikrId, date, context)
        rows.value[k]?.let { (c, t) -> rows.value = rows.value + (k to (maxOf(c - 1, 0) to t)) }
    }
    override suspend fun reset(dhikrId: Long, date: LocalDate, context: CountContext) {
        val k = Triple(dhikrId, date, context)
        rows.value[k]?.let { (_, t) -> rows.value = rows.value + (k to (0 to t)) }
    }
    val summaries = mutableMapOf<LocalDate, DaySummary>()
    override suspend fun saveSummary(summary: DaySummary) {
        summaries[summary.date] = summary
    }
}

/** A clock whose instant the test can move forward (midnight rollover). */
private class MutableClock(var now: Instant, private val zone: ZoneId = ZoneOffset.UTC) : Clock() {
    override fun getZone(): ZoneId = zone
    override fun withZone(zone: ZoneId): Clock = MutableClock(now, zone)
    override fun instant(): Instant = now
}

@OptIn(ExperimentalCoroutinesApi::class)
class CounterEngineTest {

    private fun d(id: Long, target: Int, enabled: Boolean = true) = Dhikr(id, "ذ$id", "D$id", "", target, enabled, id.toInt())

    private val astaghfir = d(1, target = 100)
    private val tasbih = d(2, target = 33)
    private val library = listOf(astaghfir, tasbih)
    private val prayerSet = listOf(PrayerSetItem(10, astaghfir, 0, 3), PrayerSetItem(11, tasbih, 1, 2))

    private fun TestScope.engine(store: ProgressStore = FakeStore(), clock: Clock = MutableClock(Instant.parse("2026-11-08T10:00:00Z"))): CounterEngine {
        // A plain scope on the test scheduler: advanceUntilIdle() runs foreground work only, so
        // backgroundScope would never let the engine's collectors run.
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        return CounterEngine(MutableStateFlow(library), MutableStateFlow(prayerSet), store, clock, scope, dispatcher)
    }

    @Test
    fun `prayer and daily counts are separate contexts`() = runTest {
        val store = FakeStore()
        val e = engine(store)
        advanceUntilIdle()
        repeat(3) { e.increment(CountContext.Fajr, AfterTarget.NextDhikr) } // astaghfir ×3 in Fajr
        e.increment(CountContext.Daily, AfterTarget.NextDhikr)              // astaghfir ×1 daily
        advanceUntilIdle()
        assertEquals(3, e.state.value.session(CountContext.Fajr).countOf(1))
        assertEquals(1, e.state.value.session(CountContext.Daily).countOf(1))
        assertEquals(0, e.state.value.session(CountContext.Dhuhr).countOf(1))
        val date = LocalDate.parse("2026-11-08")
        assertEquals(3, store.rows.value[Triple(1L, date, CountContext.Fajr)]?.first)
        assertEquals(1, store.rows.value[Triple(1L, date, CountContext.Daily)]?.first)
    }

    @Test
    fun `prayer session uses the set's own counts and completes`() = runTest {
        val e = engine()
        advanceUntilIdle()
        val events = (1..5).map { e.increment(CountContext.Asr, AfterTarget.Stay) } // Stay is ignored for prayers
        assertTrue(events[2] is FlowEvent.TargetReached) // astaghfir 3 → next is tasbih
        assertTrue(events[4] is FlowEvent.RoutineComplete) // tasbih 2 → set complete
    }

    @Test
    fun `counts survive a restart because they come back from the store`() = runTest {
        val store = FakeStore()
        val first = engine(store)
        advanceUntilIdle()
        repeat(7) { first.increment(CountContext.Daily, AfterTarget.NextDhikr) }
        advanceUntilIdle()
        val second = engine(store) // new process
        advanceUntilIdle()
        assertEquals(7, second.state.value.session(CountContext.Daily).countOf(1))
        assertTrue(second.state.value.loaded)
    }

    @Test
    fun `undo and reset are persisted`() = runTest {
        val store = FakeStore()
        val e = engine(store)
        advanceUntilIdle()
        repeat(5) { e.increment(CountContext.Daily, AfterTarget.NextDhikr) }
        e.undo(CountContext.Daily)
        advanceUntilIdle()
        val date = LocalDate.parse("2026-11-08")
        assertEquals(4, store.rows.value[Triple(1L, date, CountContext.Daily)]?.first)
        e.resetCurrent(CountContext.Daily, AfterTarget.NextDhikr)
        advanceUntilIdle()
        assertEquals(0, store.rows.value[Triple(1L, date, CountContext.Daily)]?.first)
        assertEquals(0, e.state.value.session(CountContext.Daily).countOf(1))
    }

    @Test
    fun `rapid taps are never lost`() = runTest {
        val store = FakeStore()
        val e = engine(store)
        advanceUntilIdle()
        repeat(250) { e.increment(CountContext.Daily, AfterTarget.Stay) }
        advanceUntilIdle()
        assertEquals(250, e.state.value.session(CountContext.Daily).countOf(1))
        assertEquals(250, store.rows.value[Triple(1L, LocalDate.parse("2026-11-08"), CountContext.Daily)]?.first)
    }

    @Test
    fun `midnight rolls over to a fresh day`() = runTest {
        val store = FakeStore()
        val clock = MutableClock(Instant.parse("2026-11-08T23:59:59Z"))
        val e = engine(store, clock)
        advanceUntilIdle()
        repeat(4) { e.increment(CountContext.Daily, AfterTarget.NextDhikr) }
        advanceUntilIdle()
        clock.now = Instant.parse("2026-11-09T00:00:01Z")
        e.increment(CountContext.Daily, AfterTarget.NextDhikr)
        advanceUntilIdle()
        assertEquals(LocalDate.parse("2026-11-09"), e.state.value.date)
        assertEquals(1, e.state.value.session(CountContext.Daily).countOf(1))
        assertEquals(4, store.rows.value[Triple(1L, LocalDate.parse("2026-11-08"), CountContext.Daily)]?.first)
        assertEquals(1, store.rows.value[Triple(1L, LocalDate.parse("2026-11-09"), CountContext.Daily)]?.first)
    }

    @Test
    fun `the day summary is saved and becomes complete only with all prayers and daily targets`() = runTest {
        val store = FakeStore()
        val e = engine(store)
        advanceUntilIdle()
        val date = LocalDate.parse("2026-11-08")
        CountContext.prayers.forEach { p -> repeat(5) { e.increment(p, AfterTarget.NextDhikr) } } // 3 + 2 each
        advanceUntilIdle()
        assertEquals(0b11111, store.summaries[date]?.prayersMask)
        assertEquals(false, store.summaries[date]?.isCompleted) // daily targets not done yet
        repeat(133) { e.increment(CountContext.Daily, AfterTarget.NextDhikr) } // 100 + 33
        advanceUntilIdle()
        assertEquals(true, store.summaries[date]?.isCompleted)
        e.undo(CountContext.Daily)
        advanceUntilIdle()
        assertEquals(false, store.summaries[date]?.isCompleted) // undo un-completes the day
    }

    @Test
    fun `a pinned widget counts its dhikr without moving the app's flow`() = runTest {
        val store = FakeStore()
        val e = engine(store)
        advanceUntilIdle()
        e.select(CountContext.Daily, 1) // the app is on astaghfir
        repeat(40) { e.incrementDhikr(CountContext.Daily, 2) } // widget pinned to tasbih (target 33) — keeps counting past it
        e.flush()
        advanceUntilIdle()
        assertEquals(40, e.state.value.session(CountContext.Daily).countOf(2))
        assertEquals(1L, e.state.value.session(CountContext.Daily).manualId)
        assertEquals(40, store.rows.value[Triple(2L, LocalDate.parse("2026-11-08"), CountContext.Daily)]?.first)
    }

    @Test
    fun `writes from elsewhere (widget) are merged when idle`() = runTest {
        val store = FakeStore()
        val e = engine(store)
        advanceUntilIdle()
        store.increment(2, LocalDate.parse("2026-11-08"), CountContext.Daily, 33) // e.g. a widget tap
        advanceUntilIdle()
        assertEquals(1, e.state.value.session(CountContext.Daily).countOf(2))
    }
}
