package com.dhikr.app.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhikr.app.data.CounterStore
import com.dhikr.app.data.preferences.PreferencesRepository
import com.dhikr.app.domain.flow.FlowPolicy
import com.dhikr.app.domain.flow.FlowState
import com.dhikr.app.core.model.CountingFeel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class TodayRow(
    val id: Long,
    val arabic: String,
    val transliteration: String,
    val count: Int,
    val target: Int,
    val isCurrent: Boolean,
) {
    val complete: Boolean get() = count >= target
}

data class TodayUiState(
    /** False until the library has loaded from the database — don't show the empty state yet. */
    val loaded: Boolean = false,
    val dateLabel: String = "",
    val rows: List<TodayRow> = emptyList(),
    /** Sum of per-dhikr progress, each capped at its target (one dhikr can't make up for another). */
    val done: Int = 0,
    val totalTarget: Int = 0,
    val currentId: Long? = null,
    val currentArabic: String = "",
    val currentTransliteration: String = "",
) {
    val fraction: Float get() = if (totalTarget == 0) 0f else done / totalTarget.toFloat()
    val dayComplete: Boolean get() = totalTarget > 0 && done >= totalTarget
}

class TodayViewModel(store: CounterStore, prefs: PreferencesRepository) : ViewModel() {

    val uiState: StateFlow<TodayUiState> =
        combine(store.state, prefs.prefs) { s, p -> project(s, p.feel) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), project(store.state.value, prefs.prefs.value.feel))

    private fun project(s: FlowState, feel: CountingFeel): TodayUiState {
        val routine = FlowPolicy.routine(s.library)
        val current = FlowPolicy.current(s, feel.afterTarget)
        return TodayUiState(
            loaded = s.loaded,
            dateLabel = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)).uppercase(),
            rows = routine.map {
                TodayRow(it.id, it.arabic, it.transliteration, s.countOf(it.id), it.target, it.id == current?.id)
            },
            done = routine.sumOf { minOf(s.countOf(it.id), it.target) },
            totalTarget = routine.sumOf { it.target },
            currentId = current?.id,
            currentArabic = current?.arabic.orEmpty(),
            currentTransliteration = current?.transliteration.orEmpty(),
        )
    }
}
