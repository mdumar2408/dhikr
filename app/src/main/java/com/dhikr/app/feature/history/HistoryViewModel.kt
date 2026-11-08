package com.dhikr.app.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhikr.app.data.history.DayDetailItem
import com.dhikr.app.data.history.HistoryRepository
import com.dhikr.app.data.progress.CounterEngine
import com.dhikr.app.domain.history.DaySummary
import com.dhikr.app.domain.history.StreakCalculator
import com.dhikr.app.domain.history.Streaks
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

enum class DayMark { Complete, Partial, Empty, Future }

data class DayDetail(val date: LocalDate, val summary: DaySummary?, val items: List<DayDetailItem>)

data class HistoryUiState(
    val today: LocalDate = LocalDate.now(),
    val month: YearMonth = YearMonth.now(),
    val summaries: Map<LocalDate, DaySummary> = emptyMap(),
    val streaks: Streaks = Streaks(0, 0, 0),
    val detail: DayDetail? = null,
) {
    val canGoNext: Boolean get() = month < YearMonth.from(today)

    fun mark(date: LocalDate): DayMark = when {
        date.isAfter(today) -> DayMark.Future
        summaries[date]?.isCompleted == true -> DayMark.Complete
        summaries[date]?.hasActivity == true -> DayMark.Partial
        else -> DayMark.Empty
    }
}

class HistoryViewModel(private val history: HistoryRepository, engine: CounterEngine) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.from(engine.state.value.date))
    private val detail = MutableStateFlow<DayDetail?>(null)
    private val today = engine.state.map { it.date }.distinctUntilChanged()

    val uiState: StateFlow<HistoryUiState> =
        combine(history.observeSummaries(), today, month, detail) { summaries, t, m, d ->
            HistoryUiState(
                today = t,
                month = m,
                summaries = summaries,
                streaks = StreakCalculator.compute(summaries.values.filter { it.isCompleted }.map { it.date }.toSet(), t),
                detail = d,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState(today = engine.state.value.date))

    fun onPrevMonth() = month.update { it.minusMonths(1) }
    fun onNextMonth() = month.update { m -> if (m < YearMonth.from(uiState.value.today)) m.plusMonths(1) else m }

    fun onSelectDay(date: LocalDate) {
        viewModelScope.launch {
            detail.value = DayDetail(date, uiState.value.summaries[date], history.dayDetail(date))
        }
    }

    fun onDismissDetail() {
        detail.value = null
    }
}
