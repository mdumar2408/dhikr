package com.dhikr.app.data

import com.dhikr.app.core.model.AfterTarget
import com.dhikr.app.core.model.CountingFeel
import com.dhikr.app.core.model.Dhikr
import com.dhikr.app.core.model.ThemeMode
import com.dhikr.app.core.model.UserPreferences
import com.dhikr.app.domain.flow.FlowEvent
import com.dhikr.app.domain.flow.FlowPolicy
import com.dhikr.app.domain.flow.FlowState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Phase 1 state holders. Process-lifetime only — Room (Phase 2/3) and Proto DataStore (Phase 2)
 * replace them behind the same shape. Every mutation goes through [FlowPolicy], so the rules
 * the UI exercises today are the rules the real engine will run.
 */
class CounterStore(library: List<Dhikr>) {
    private val _state = MutableStateFlow(FlowState(library))
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
    fun setEnabled(id: Long, enabled: Boolean) = _state.update { FlowPolicy.setEnabled(it, id, enabled) }
}

class PreferencesStore {
    private val _prefs = MutableStateFlow(UserPreferences())
    val prefs: StateFlow<UserPreferences> = _prefs.asStateFlow()

    fun setTheme(mode: ThemeMode) = _prefs.update { it.copy(themeMode = mode) }
    fun setFeel(transform: (CountingFeel) -> CountingFeel) = _prefs.update { it.copy(feel = transform(it.feel)) }
}
