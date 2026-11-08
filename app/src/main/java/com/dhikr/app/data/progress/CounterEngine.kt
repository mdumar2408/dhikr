package com.dhikr.app.data.progress

import android.util.Log
import com.dhikr.app.core.model.AfterTarget
import com.dhikr.app.core.model.CountContext
import com.dhikr.app.core.model.Dhikr
import com.dhikr.app.core.model.PrayerSetItem
import com.dhikr.app.domain.flow.FlowEvent
import com.dhikr.app.domain.flow.FlowPolicy
import com.dhikr.app.domain.flow.FlowState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

/** Everything the counter screens need: today's date and one Flow session per context. */
data class EngineState(
    val date: LocalDate,
    val sessions: Map<CountContext, FlowState>,
    val countsLoaded: Boolean = false,
) {
    fun session(context: CountContext): FlowState = sessions.getValue(context)
    val loaded: Boolean get() = countsLoaded && sessions.values.all { it.loaded }
}

/**
 * The single counter engine (dhikr-domain-engineering, D-015, D-022). App screens — and later
 * widgets and the notification — count only through here.
 *
 * - **Instant:** a tap updates in-memory state with FlowPolicy (compare-and-set, never loses a
 *   tap) and returns the [FlowEvent] synchronously, so the haptic fires in the same frame.
 * - **Durable, in order:** each change is queued to a single consumer that writes to the
 *   [ProgressStore] (atomic SQL increments) in tap order.
 * - **Consistent:** storage changes made elsewhere are merged in whenever no write is pending.
 * - **Day-safe:** the local date is checked on every action; a new day starts from stored counts
 *   for that date (normally zero), never carrying yesterday over.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CounterEngine(
    library: Flow<List<Dhikr>>,
    prayerSet: Flow<List<PrayerSetItem>>,
    private val store: ProgressStore,
    private val clock: Clock,
    scope: CoroutineScope,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val _state = MutableStateFlow(EngineState(today(), emptySessions()))
    val state: StateFlow<EngineState> = _state.asStateFlow()

    private val date = MutableStateFlow(today())
    private val writes = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private val pending = AtomicInteger(0)

    init {
        // Single writer: preserves tap order and keeps the main thread free.
        scope.launch(ioDispatcher) {
            for (write in writes) {
                try {
                    write()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save a count", e)
                } finally {
                    pending.decrementAndGet()
                }
            }
        }
        scope.launch {
            combine(library, prayerSet) { l, p -> l to p }.collect { (l, p) ->
                _state.update { applyRoutines(it, l, p) }
            }
        }
        scope.launch {
            date.flatMapLatest { d -> store.observeDate(d).map { d to it } }.collect { (d, records) ->
                // Our own writes are already reflected in memory; only merge when nothing is in flight.
                if (pending.get() == 0) _state.update { applyCounts(it, d, records) }
            }
        }
    }

    fun increment(context: CountContext, afterTarget: AfterTarget): FlowEvent {
        ensureToday()
        val mode = if (context.isPrayer) AfterTarget.NextDhikr else afterTarget
        while (true) {
            val old = _state.value
            val (session, event) = FlowPolicy.increment(old.session(context), mode)
            if (_state.compareAndSet(old, old.copy(sessions = old.sessions + (context to session)))) {
                countedDhikr(event)?.let { d -> enqueue { store.increment(d.id, old.date, context, d.target) } }
                return event
            }
        }
    }

    fun undo(context: CountContext) {
        ensureToday()
        while (true) {
            val old = _state.value
            val last = old.session(context).history.lastOrNull() ?: return
            val session = FlowPolicy.undo(old.session(context))
            if (_state.compareAndSet(old, old.copy(sessions = old.sessions + (context to session)))) {
                enqueue { store.decrement(last, old.date, context) }
                return
            }
        }
    }

    /** Hold-to-reset: sets the current Dhikr of [context] back to 0 for today. */
    fun resetCurrent(context: CountContext, afterTarget: AfterTarget) {
        ensureToday()
        val mode = if (context.isPrayer) AfterTarget.NextDhikr else afterTarget
        while (true) {
            val old = _state.value
            val current = FlowPolicy.current(old.session(context), mode) ?: return
            val session = FlowPolicy.reset(old.session(context), current.id)
            if (_state.compareAndSet(old, old.copy(sessions = old.sessions + (context to session)))) {
                enqueue { store.reset(current.id, old.date, context) }
                return
            }
        }
    }

    fun select(context: CountContext, dhikrId: Long) =
        _state.update { it.copy(sessions = it.sessions + (context to FlowPolicy.select(it.session(context), dhikrId))) }

    /** Call when the app returns to the foreground: rolls over to a new day if midnight passed. */
    fun refreshDate() = ensureToday()

    private fun ensureToday() {
        val today = today()
        if (_state.value.date == today) return
        _state.update { s ->
            if (s.date == today) {
                s
            } else {
                // New day: fresh counts and undo history; routines carry over.
                s.copy(
                    date = today,
                    sessions = s.sessions.mapValues { (_, f) -> f.copy(counts = emptyMap(), history = emptyList(), manualId = null) },
                    countsLoaded = false,
                )
            }
        }
        date.value = today
    }

    private fun enqueue(write: suspend () -> Unit) {
        pending.incrementAndGet()
        writes.trySend(write)
    }

    private fun today(): LocalDate = LocalDate.now(clock)

    companion object {
        private const val TAG = "CounterEngine"

        private fun emptySessions(): Map<CountContext, FlowState> = CountContext.entries.associateWith {
            FlowState(library = emptyList(), loops = !it.isPrayer)
        }

        private fun countedDhikr(e: FlowEvent): Dhikr? = when (e) {
            is FlowEvent.Counted -> e.dhikr
            is FlowEvent.TargetReached -> e.completed
            is FlowEvent.RoutineComplete -> e.completed
            FlowEvent.NothingEnabled -> null
        }

        /** Daily = the enabled library; each prayer = the after-prayer set with its own counts. */
        internal fun applyRoutines(s: EngineState, library: List<Dhikr>, prayerSet: List<PrayerSetItem>): EngineState {
            val prayerRoutine = prayerSet.distinctBy { it.dhikr.id }.map {
                it.dhikr.copy(target = it.target, isEnabled = true, sortOrder = it.position)
            }
            return s.copy(
                sessions = s.sessions.mapValues { (ctx, f) ->
                    FlowPolicy.withLibrary(f, if (ctx.isPrayer) prayerRoutine else library)
                },
            )
        }

        internal fun applyCounts(s: EngineState, date: LocalDate, records: List<ProgressRecord>): EngineState {
            if (date != s.date) return s // a late emission for a previous day
            val byContext = records.groupBy { it.context }
            return s.copy(
                sessions = s.sessions.mapValues { (ctx, f) ->
                    FlowPolicy.withCounts(f, byContext[ctx].orEmpty().associate { it.dhikrId to it.count })
                },
                countsLoaded = true,
            )
        }
    }
}
