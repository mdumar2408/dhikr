package com.dhikr.app.data

import com.dhikr.app.core.model.AfterTarget
import com.dhikr.app.core.model.Dhikr
import com.dhikr.app.domain.flow.FlowEvent
import com.dhikr.app.domain.flow.FlowPolicy
import com.dhikr.app.domain.flow.FlowState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Counting session state. Since Phase 2 the library comes from Room ([setLibrary] is fed by
 * DhikrRepository.observeLibrary in AppContainer). Counts themselves are still in memory until the
 * Phase 3 engine persists them to daily_progress (D-018).
 * Every mutation goes through [FlowPolicy].
 */
class CounterStore {
    private val _state = MutableStateFlow(FlowState(library = emptyList()))
    val state: StateFlow<FlowState> = _state.asStateFlow()

    /** Atomic (compare-and-set) so concurrent taps from different surfaces never lose a count. */
    fun increment(afterTarget: AfterTarget): FlowEvent {
        while (true) {
            val old = _state.value
            val (new, event) = FlowPolicy.increment(old, afterTarget)
            if (_state.compareAndSet(old, new)) return event
        }
    }

    fun undo() = _state.update(FlowPolicy::undo)
    fun select(id: Long) = _state.update { FlowPolicy.select(it, id) }
    fun setLibrary(library: List<Dhikr>) = _state.update { FlowPolicy.withLibrary(it, library) }
}
