package com.dhikr.app.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhikr.app.core.model.CountContext
import com.dhikr.app.core.model.CountingFeel
import com.dhikr.app.data.preferences.PreferencesRepository
import com.dhikr.app.data.history.HistoryRepository
import com.dhikr.app.domain.history.DaySummaryPolicy
import com.dhikr.app.domain.history.StreakCalculator
import com.dhikr.app.data.progress.CounterEngine
import com.dhikr.app.data.progress.EngineState
import com.dhikr.app.domain.flow.FlowPolicy
import com.dhikr.app.domain.flow.FlowState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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

/** One after-prayer session: how many items of the set are done. */
data class PrayerStatus(val context: CountContext, val itemsDone: Int, val itemsTotal: Int) {
    val complete: Boolean get() = itemsTotal > 0 && itemsDone >= itemsTotal
    val started: Boolean get() = itemsDone > 0
}

data class TodayUiState(
    /** False until routines and today's counts have loaded — don't show empty states yet. */
    val loaded: Boolean = false,
    val dateLabel: String = "",
    val prayers: List<PrayerStatus> = emptyList(),
    /** First prayer whose set isn't complete — the one the Prayer card opens. */
    val suggestedPrayer: CountContext? = null,
    val rows: List<TodayRow> = emptyList(),
    /** Sum of per-dhikr progress, each capped at its target (one dhikr can't make up for another). */
    val done: Int = 0,
    val totalTarget: Int = 0,
    val currentArabic: String = "",
    val currentTransliteration: String = "",
    /** Streak day (D-023/D-032) — the same rule History uses (DaySummaryPolicy). */
    val dayComplete: Boolean = false,
    /** Current streak in days (counts today once it's complete). */
    val streak: Int = 0,
) {
    val fraction: Float get() = if (totalTarget == 0) 0f else done / totalTarget.toFloat()
    val dailyComplete: Boolean get() = totalTarget == 0 || done >= totalTarget
    val prayersComplete: Int get() = prayers.count { it.complete }
    val hasPrayerSet: Boolean get() = prayers.any { it.itemsTotal > 0 }
}

class TodayViewModel(
    private val engine: CounterEngine,
    prefs: PreferencesRepository,
    history: HistoryRepository,
) : ViewModel() {

    val uiState: StateFlow<TodayUiState> =
        combine(engine.state, prefs.prefs, history.observeSummaries()) { s, p, summaries ->
            val completed = summaries.values.filter { it.isCompleted }.map { it.date }.toMutableSet()
            val todayComplete = DaySummaryPolicy.summarize(s.date, s.sessions).isCompleted
            // Use the live state for today (the saved summary may be a few ms behind).
            if (todayComplete) completed += s.date else completed -= s.date
            project(s, p.feel).copy(
                dayComplete = todayComplete,
                streak = StreakCalculator.compute(completed, s.date).current,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), project(engine.state.value, prefs.prefs.value.feel))

    /** Call on resume so the screen rolls over after midnight. */
    fun onResume() = engine.refreshDate()

    private fun project(s: EngineState, feel: CountingFeel): TodayUiState {
        val prayers = CountContext.prayers.map { ctx -> prayerStatus(ctx, s.session(ctx)) }
        val daily = s.session(CountContext.Daily)
        val routine = FlowPolicy.routine(daily.library)
        val current = FlowPolicy.current(daily, feel.afterTarget)
        return TodayUiState(
            loaded = s.loaded,
            dateLabel = s.date.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)).uppercase(),
            prayers = prayers,
            suggestedPrayer = prayers.firstOrNull { !it.complete && it.itemsTotal > 0 }?.context,
            rows = routine.map {
                TodayRow(it.id, it.arabic, it.transliteration, daily.countOf(it.id), it.target, it.id == current?.id)
            },
            done = routine.sumOf { minOf(daily.countOf(it.id), it.target) },
            totalTarget = routine.sumOf { it.target },
            currentArabic = current?.arabic.orEmpty(),
            currentTransliteration = current?.transliteration.orEmpty(),
        )
    }

    private fun prayerStatus(ctx: CountContext, f: FlowState): PrayerStatus {
        val set = FlowPolicy.routine(f.library)
        return PrayerStatus(ctx, set.count { f.countOf(it.id) >= it.target }, set.size)
    }
}
