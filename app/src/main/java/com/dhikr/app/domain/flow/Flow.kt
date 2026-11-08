package com.dhikr.app.domain.flow

import com.dhikr.app.core.model.AfterTarget
import com.dhikr.app.core.model.Dhikr

/**
 * Flow — continuous counting through the routine (DESIGN_SPEC §13, D-015).
 * Pure functions over immutable state: no Android, no clock, no I/O. Fully unit-tested.
 *
 * Terminology: a Dhikr's *goal* in round r is `target × r`. The *round* is the lowest r ≥ 1
 * in which at least one routine Dhikr hasn't met its goal.
 */
data class FlowState(
    val library: List<Dhikr>,
    val counts: Map<Long, Int> = emptyMap(),
    /** Most recent increment last; used by undo. */
    val history: List<Long> = emptyList(),
    /** Dhikr the user picked by hand; overrides auto-selection until its goal is met (or always, in Stay mode). */
    val manualId: Long? = null,
    /** False until the library has been read from storage once (avoids an "empty" flash). */
    val loaded: Boolean = false,
    /**
     * Daily targets loop into rounds after the routine is complete (§13). Prayer sessions don't:
     * once the set is done, extra taps count on its last item (§14.1).
     */
    val loops: Boolean = true,
) {
    fun countOf(id: Long): Int = counts[id] ?: 0
}

sealed interface FlowEvent {
    /** An ordinary count. */
    data class Counted(val dhikr: Dhikr, val count: Int) : FlowEvent

    /** This tap met [completed]'s goal; Flow continues with [next]. */
    data class TargetReached(val completed: Dhikr, val next: Dhikr, val round: Int) : FlowEvent

    /** This tap completed every Dhikr in the routine for [round]. Round [round]+1 starts with the next tap. */
    data class RoutineComplete(val completed: Dhikr, val round: Int) : FlowEvent

    /** Nothing to count: no enabled Dhikr. */
    data object NothingEnabled : FlowEvent
}

object FlowPolicy {

    /** Enabled Dhikrs in the user's routine order (sortOrder, then id for stability). */
    fun routine(library: List<Dhikr>): List<Dhikr> =
        library.filter { it.isEnabled }.sortedWith(compareBy({ it.sortOrder }, { it.id }))

    fun round(routine: List<Dhikr>, counts: Map<Long, Int>): Int {
        if (routine.isEmpty()) return 1
        // Smallest r such that some dhikr has count < target*r  ⇔  min over dhikrs of (count / target) + 1.
        return routine.minOf { (counts[it.id] ?: 0) / it.target } + 1
    }

    fun current(state: FlowState, afterTarget: AfterTarget): Dhikr? {
        val routine = routine(state.library)
        if (routine.isEmpty()) return null
        val manual = state.manualId?.let { id -> routine.firstOrNull { it.id == id } }
        if (afterTarget == AfterTarget.Stay) return manual ?: routine.first()
        val round = round(routine, state.counts)
        if (!state.loops && round > 1) return manual ?: routine.last() // session complete
        if (manual != null && state.countOf(manual.id) < manual.target * round) return manual
        return routine.first { state.countOf(it.id) < it.target * round }
    }

    fun increment(state: FlowState, afterTarget: AfterTarget): Pair<FlowState, FlowEvent> {
        val dhikr = current(state, afterTarget) ?: return state to FlowEvent.NothingEnabled
        val routine = routine(state.library)
        val roundBefore = round(routine, state.counts)
        val newCount = state.countOf(dhikr.id) + 1
        var next = state.copy(
            counts = state.counts + (dhikr.id to newCount),
            history = state.history + dhikr.id,
        )

        val event: FlowEvent = when (afterTarget) {
            AfterTarget.Stay ->
                if (newCount % dhikr.target == 0) {
                    FlowEvent.TargetReached(dhikr, dhikr, newCount / dhikr.target)
                } else {
                    FlowEvent.Counted(dhikr, newCount)
                }

            AfterTarget.NextDhikr -> {
                val goal = dhikr.target * roundBefore
                when {
                    !state.loops && roundBefore > 1 -> FlowEvent.Counted(dhikr, newCount) // extra after the set
                    newCount != goal -> FlowEvent.Counted(dhikr, newCount)
                    round(routine, next.counts) > roundBefore -> {
                        next = next.copy(manualId = null)
                        FlowEvent.RoutineComplete(dhikr, roundBefore)
                    }
                    else -> {
                        next = next.copy(manualId = null)
                        FlowEvent.TargetReached(dhikr, current(next, afterTarget)!!, roundBefore)
                    }
                }
            }
        }
        return next to event
    }

    /** Reverses the most recent increment wherever it happened; current is then recomputed. */
    fun undo(state: FlowState): FlowState {
        val last = state.history.lastOrNull() ?: return state
        val count = (state.countOf(last) - 1).coerceAtLeast(0)
        return state.copy(
            counts = state.counts + (last to count),
            history = state.history.dropLast(1),
            // Return focus to where the undone tap happened.
            manualId = last,
        )
    }

    /** Sets one Dhikr's count back to 0 (hold-to-reset). Its taps leave the undo history. */
    fun reset(state: FlowState, id: Long): FlowState = state.copy(
        counts = state.counts + (id to 0),
        history = state.history.filter { it != id },
        manualId = id,
    )

    /** Replaces counts with what storage holds (e.g. after a widget wrote, or on a new day). */
    fun withCounts(state: FlowState, counts: Map<Long, Int>): FlowState = state.copy(counts = counts)

    fun select(state: FlowState, id: Long): FlowState =
        if (state.library.any { it.id == id }) state.copy(manualId = id) else state

    /** Replaces the library (from storage). Counts are kept; a manual pick that vanished is cleared. */
    fun withLibrary(state: FlowState, library: List<Dhikr>): FlowState = state.copy(
        library = library,
        manualId = state.manualId?.takeIf { id -> library.any { it.id == id && it.isEnabled } },
        loaded = true,
    )

    fun setEnabled(state: FlowState, id: Long, enabled: Boolean): FlowState = state.copy(
        library = state.library.map { if (it.id == id) it.copy(isEnabled = enabled) else it },
        manualId = state.manualId.takeUnless { it == id && !enabled },
    )
}
