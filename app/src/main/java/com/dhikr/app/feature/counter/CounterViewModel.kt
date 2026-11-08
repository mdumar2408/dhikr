package com.dhikr.app.feature.counter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhikr.app.core.model.AfterTarget
import com.dhikr.app.core.model.CountContext
import com.dhikr.app.core.model.CountingFeel
import com.dhikr.app.core.model.Dhikr
import com.dhikr.app.core.model.SoundSettings
import com.dhikr.app.data.preferences.PreferencesRepository
import com.dhikr.app.data.progress.CounterEngine
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

/** Shown for ~2 s after a target: what was completed and what to say next (spec §13/§14). */
data class Announcement(
    val completed: Dhikr,
    val next: Dhikr?,
    val round: Int,
    /** The whole routine (daily) or the whole after-prayer set (prayer) is done. */
    val routineComplete: Boolean,
    val serial: Long,
)

data class CounterUiState(
    val context: CountContext = CountContext.Daily,
    val dhikr: Dhikr? = null,
    /** Count within the current round (Flow) or the running total (Stay / finished prayer set). */
    val displayCount: Int = 0,
    val target: Int = 33,
    /** Show the completed (ember) state on the count. */
    val complete: Boolean = false,
    /** Prayer only: every item of the set reached its count. */
    val sessionComplete: Boolean = false,
    val round: Int = 1,
    val position: Int = 0,
    val routineSize: Int = 0,
    val routineDots: List<RoutineDotState> = emptyList(),
    val feel: CountingFeel = CountingFeel(),
    val sound: SoundSettings = SoundSettings(),
    val announcement: Announcement? = null,
) {
    /** Daily Flow vs. classic single counter; prayer sessions always flow. */
    val flows: Boolean get() = context.isPrayer || feel.afterTarget == AfterTarget.NextDhikr
}

class CounterViewModel(
    private val engine: CounterEngine,
    private val prefs: PreferencesRepository,
    private val context: CountContext,
    startDhikrId: Long?,
) : ViewModel() {

    private val announcement = MutableStateFlow<Announcement?>(null)
    private var clearJob: Job? = null
    private var serial = 0L

    init {
        engine.refreshDate()
        startDhikrId?.let { engine.select(context, it) }
    }

    val uiState: StateFlow<CounterUiState> =
        combine(engine.state, prefs.prefs, announcement) { s, p, a -> project(s.session(context), p.feel, a).copy(sound = p.sound) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                project(engine.state.value.session(context), prefs.prefs.value.feel, null).copy(sound = prefs.prefs.value.sound),
            )

    fun onToggleRecite() {
        val on = !prefs.prefs.value.sound.reciteWhileCounting
        viewModelScope.launch { prefs.setReciteWhileCounting(on) }
    }

    /**
     * Called synchronously from the pad's tap handler. Returns the event so the caller can fire
     * the matching haptic in the same frame (tactile-ui-engineering: never via LaunchedEffect).
     */
    fun onTap(): FlowEvent {
        val event = engine.increment(context, prefs.prefs.value.feel.afterTarget)
        val flows = context.isPrayer || prefs.prefs.value.feel.afterTarget == AfterTarget.NextDhikr
        when (event) {
            is FlowEvent.TargetReached ->
                if (flows) announce(Announcement(event.completed, event.next, event.round, false, ++serial))
            is FlowEvent.RoutineComplete ->
                announce(Announcement(event.completed, null, event.round, true, ++serial))
            else -> Unit
        }
        return event
    }

    fun onUndo() = engine.undo(context)

    fun onReset() = engine.resetCurrent(context, prefs.prefs.value.feel.afterTarget)

    /** Manual override: swipe on the top area moves to the previous/next Dhikr in the routine. */
    fun onSwipe(direction: Int) {
        val s = engine.state.value.session(context)
        val routine = FlowPolicy.routine(s.library)
        val current = FlowPolicy.current(s, mode()) ?: return
        val i = routine.indexOfFirst { it.id == current.id }
        val target = routine.getOrNull((i + direction).mod(routine.size)) ?: return
        engine.select(context, target.id)
    }

    private fun mode() = if (context.isPrayer) AfterTarget.NextDhikr else prefs.prefs.value.feel.afterTarget

    private fun announce(a: Announcement) {
        announcement.value = a
        clearJob?.cancel()
        clearJob = viewModelScope.launch {
            delay(2_200)
            announcement.value = null
        }
    }

    private fun project(s: FlowState, feel: CountingFeel, a: Announcement?): CounterUiState {
        val mode = if (context.isPrayer) AfterTarget.NextDhikr else feel.afterTarget
        val routine = FlowPolicy.routine(s.library)
        val current = FlowPolicy.current(s, mode)
            ?: return CounterUiState(context = context, feel = feel, announcement = a)
        val count = s.countOf(current.id)
        val flows = mode == AfterTarget.NextDhikr
        val round = if (flows) FlowPolicy.round(routine, s.counts) else 1
        val sessionComplete = context.isPrayer && round > 1
        val display = when {
            sessionComplete || !flows -> count
            else -> (count - current.target * (round - 1)).coerceAtLeast(0)
        }
        return CounterUiState(
            context = context,
            dhikr = current,
            displayCount = display,
            target = current.target,
            complete = sessionComplete || (!flows && count >= current.target),
            sessionComplete = sessionComplete,
            round = if (context.isPrayer) 1 else round,
            position = routine.indexOfFirst { it.id == current.id } + 1,
            routineSize = routine.size,
            routineDots = routine.map {
                when {
                    it.id == current.id && !sessionComplete -> RoutineDotState.Now
                    s.countOf(it.id) >= it.target * (if (sessionComplete) 1 else round) -> RoutineDotState.Done
                    else -> RoutineDotState.Todo
                }
            },
            feel = feel,
            announcement = a,
        )
    }
}
