package com.dhikr.app.domain.flow

import com.dhikr.app.core.model.AfterTarget
import com.dhikr.app.core.model.Dhikr
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FlowPolicyTest {

    private fun d(id: Long, target: Int = 3, order: Int = id.toInt(), enabled: Boolean = true) =
        Dhikr(id, "ذكر$id", "D$id", "", target, enabled, order)

    private val a = d(1)
    private val b = d(2)
    private val c = d(3)
    private val flow = AfterTarget.NextDhikr

    private fun tap(s: FlowState, n: Int = 1, mode: AfterTarget = flow): Pair<FlowState, List<FlowEvent>> {
        var state = s
        val events = mutableListOf<FlowEvent>()
        repeat(n) { val (ns, e) = FlowPolicy.increment(state, mode); state = ns; events += e }
        return state to events
    }

    @Test
    fun `routine is enabled dhikrs in sortOrder`() {
        val lib = listOf(d(1, order = 2), d(2, order = 0), d(3, order = 1, enabled = false))
        assertEquals(listOf(2L, 1L), FlowPolicy.routine(lib).map { it.id })
    }

    @Test
    fun `tap that meets the target completes A and the next tap lands on B`() {
        val (s, events) = tap(FlowState(listOf(a, b, c)), 4)
        assertEquals(3, s.countOf(1))
        assertEquals(1, s.countOf(2))
        val reached = events[2] as FlowEvent.TargetReached
        assertEquals(a, reached.completed)
        assertEquals(b, reached.next)
        assertTrue(events[3] is FlowEvent.Counted)
    }

    @Test
    fun `already complete dhikrs are skipped`() {
        val s = FlowState(listOf(a, b, c), counts = mapOf(2L to 3))
        val (after, events) = tap(s, 3)
        assertEquals(c, (events.last() as FlowEvent.TargetReached).next)
        assertEquals(3, after.countOf(1))
    }

    @Test
    fun `completing the routine emits RoutineComplete then round 2 starts at A`() {
        val (s, events) = tap(FlowState(listOf(a, b, c)), 9)
        val done = events.last() as FlowEvent.RoutineComplete
        assertEquals(1, done.round)
        assertEquals(2, FlowPolicy.round(FlowPolicy.routine(s.library), s.counts))
        assertEquals(a, FlowPolicy.current(s, flow))
        val (s2, _) = tap(s)
        assertEquals(4, s2.countOf(1)) // round 2 counts from the target upwards
    }

    @Test
    fun `undo right after an advance returns to the previous dhikr`() {
        val (s, _) = tap(FlowState(listOf(a, b)), 3)
        assertEquals(b, FlowPolicy.current(s, flow))
        val undone = FlowPolicy.undo(s)
        assertEquals(2, undone.countOf(1))
        assertEquals(a, FlowPolicy.current(undone, flow))
    }

    @Test
    fun `undo with no history is a no-op`() {
        val s = FlowState(listOf(a))
        assertEquals(s, FlowPolicy.undo(s))
    }

    @Test
    fun `disabling the next dhikr mid-flow skips it`() {
        val (s, _) = tap(FlowState(listOf(a, b, c)), 2)
        val (after, events) = tap(FlowPolicy.setEnabled(s, 2, false))
        assertEquals(c, (events.single() as FlowEvent.TargetReached).next)
        assertEquals(0, after.countOf(2))
    }

    @Test
    fun `a single dhikr loops in rounds`() {
        val (s, events) = tap(FlowState(listOf(a)), 7)
        assertTrue(events[2] is FlowEvent.RoutineComplete)
        assertTrue(events[5] is FlowEvent.RoutineComplete)
        assertEquals(2, (events[5] as FlowEvent.RoutineComplete).round)
        assertEquals(7, s.countOf(1))
    }

    @Test
    fun `manual selection is honoured until its target, then flow resumes in order`() {
        val s = FlowPolicy.select(FlowState(listOf(a, b, c)), 3)
        val (after, events) = tap(s, 3)
        assertEquals(c, (events.last() as FlowEvent.TargetReached).completed)
        assertEquals(a, (events.last() as FlowEvent.TargetReached).next)
        assertNull(after.manualId)
    }

    @Test
    fun `stay mode keeps counting the same dhikr and signals each multiple of target`() {
        val (s, events) = tap(FlowState(listOf(a, b)), 7, AfterTarget.Stay)
        assertEquals(7, s.countOf(1))
        assertEquals(0, s.countOf(2))
        assertEquals(listOf(2, 5), events.indices.filter { events[it] is FlowEvent.TargetReached })
    }

    @Test
    fun `nothing enabled yields NothingEnabled and no state change`() {
        val s = FlowState(listOf(d(1, enabled = false)))
        val (after, e) = FlowPolicy.increment(s, flow)
        assertEquals(FlowEvent.NothingEnabled, e)
        assertEquals(s, after)
    }

    @Test
    fun `prayer session does not loop - extra taps go to the last item`() {
        val s0 = FlowState(listOf(a, b), loops = false)
        val (s, events) = tap(s0, 8)
        assertTrue(events[5] is FlowEvent.RoutineComplete)
        assertTrue(events[6] is FlowEvent.Counted && (events[6] as FlowEvent.Counted).dhikr == b)
        assertTrue(events[7] is FlowEvent.Counted)
        assertEquals(3, s.countOf(1))
        assertEquals(5, s.countOf(2))
        assertEquals(b, FlowPolicy.current(s, flow))
    }

    @Test
    fun `reset puts the dhikr back to zero and removes it from undo history`() {
        val (s, _) = tap(FlowState(listOf(a, b)), 4) // a=3, b=1
        val r = FlowPolicy.reset(s, 1)
        assertEquals(0, r.countOf(1))
        assertEquals(1, r.countOf(2))
        assertEquals(listOf(2L), r.history)
        assertEquals(a, FlowPolicy.current(r, flow))
    }

    @Test
    fun `lowering a target below the current count completes it without a retroactive event`() {
        val big = d(1, target = 10)
        val (s, _) = tap(FlowState(listOf(big, b)), 5)
        val lowered = s.copy(library = listOf(big.copy(target = 3), b))
        assertEquals(b, FlowPolicy.current(lowered, flow))
        val (_, e) = tap(lowered)
        assertTrue(e.single() is FlowEvent.Counted)
    }
}
