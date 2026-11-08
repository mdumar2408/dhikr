package com.dhikr.app.feature.counter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhikr.app.core.model.AfterTarget
import com.dhikr.app.core.model.CountingFeel
import com.dhikr.app.core.model.Dhikr
import com.dhikr.app.data.CounterStore
import com.dhikr.app.data.PreferencesStore
import com.dhikr.app.design.components.RoutineDotState
import com.dhikr.app.domain.flow.FlowEvent
import com.dhikr.app.domain.flow.FlowPolicy
import com.dhikr.app.domain.flow.FlowState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Shown for ~2 s after a target: what was completed and what to say next (spec §13). */
data class Announcement(
    val completed: Dhikr,
    val next: Dhikr?,
    val round: Int,
    val routineComplete: Boolean,
    val serial: Long,
)

data class CounterUiState(
    val dhikr: Dhikr? = null,
    /** Count within the current round (Flow) or the running total (Stay). */
    val displayCount: Int = 0,
    val target: Int = 33,
    val complete: Boolean = false,
    val round: Int = 1,
    val position: Int = 0,
    val routineSize: Int = 0,
    val routineDots: List<RoutineDotState> = emptyList(),
    val feel: CountingFeel = CountingFeel(),
    val announcement: Announcement? = null,
)

class CounterViewModel(
    private val store: CounterStore,
    private val prefs: PreferencesStore,
    startDhikrId: Long?,
) : ViewModel() {

    private val announcement = MutableStateFlow<Announcement?>(null)
    private var clearJob: Job? = null
    private var serial = 0L

    init {
        startDhikrId?.let(store::select)
    }

    val uiState: StateFlow<CounterUiState> =
        combine(store.state, prefs.prefs, announcement) { s, p, a -> project(s, p.feel, a) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                project(store.state.value, prefs.prefs.value.feel, null),
            )

    /**
     * Called synchronously from the pad's tap handler. Returns the event so the caller can fire
     * the matching haptic in the same frame (tactile-ui-engineering: never via LaunchedEffect).
     */
    fun onTap(): FlowEvent {
        val event = store.increment(prefs.prefs.value.feel.afterTarget)
        when (event) {
            is FlowEvent.TargetReached ->
                if (prefs.prefs.value.feel.afterTarget == AfterTarget.NextDhikr) {
                    announce(Announcement(event.completed, event.next, event.round, false, ++serial))
                }
            is FlowEvent.RoutineComplete ->
                announce(Announcement(event.completed, null, event.round, true, ++serial))
            else -> Unit
        }
        return event
    }

    fun onUndo() = store.undo()

    /** Manual override: swipe on the top area moves to the previous/next Dhikr in the routine. */
    fun onSwipe(direction: Int) {
        val s = store.state.value
        val routine = FlowPolicy.routine(s.library)
        val current = FlowPolicy.current(s, prefs.prefs.value.feel.afterTarget) ?: return
        val i = routine.indexOfFirst { it.id == current.id }
        val target = routine.getOrNull((i + direction).mod(routine.size)) ?: return
        store.select(target.id)
    }

    private fun announce(a: Announcement) {
        announcement.value = a
        clearJob?.cancel()
        clearJob = viewModelScope.launch {
            delay(2_200)
            announcement.value = null
        }
    }

    private fun project(s: FlowState, feel: CountingFeel, a: Announcement?): CounterUiState {
        val routine = FlowPolicy.routine(s.library)
        val current = FlowPolicy.current(s, feel.afterTarget)
            ?: return CounterUiState(feel = feel, announcement = a)
        val count = s.countOf(current.id)
        val flow = feel.afterTarget == AfterTarget.NextDhikr
        val round = if (flow) FlowPolicy.round(routine, s.counts) else 1
        val display = if (flow) (count - current.target * (round - 1)).coerceAtLeast(0) else count
        return CounterUiState(
            dhikr = current,
            displayCount = display,
            target = current.target,
            complete = !flow && count >= current.target,
            round = round,
            position = routine.indexOfFirst { it.id == current.id } + 1,
            routineSize = routine.size,
            routineDots = routine.map {
                when {
                    it.id == current.id -> RoutineDotState.Now
                    s.countOf(it.id) >= it.target * round -> RoutineDotState.Done
                    else -> RoutineDotState.Todo
                }
            },
            feel = feel,
            announcement = a,
        )
    }
}
