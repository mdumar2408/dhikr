package com.dhikr.app.data.history

import com.dhikr.app.core.model.CountContext
import com.dhikr.app.data.database.SummaryDao
import com.dhikr.app.data.progress.toDomain
import com.dhikr.app.domain.history.DaySummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** One dhikr's count on a past day, in one context. Names include since-deleted dhikrs. */
data class DayDetailItem(
    val context: CountContext,
    val transliteration: String,
    val arabic: String,
    val count: Int,
    val target: Int,
) {
    val complete: Boolean get() = count >= target
}

/** Read side of history: day summaries for the calendar/streaks and per-day detail. */
class HistoryRepository(private val dao: SummaryDao) {

    fun observeSummaries(): Flow<Map<LocalDate, DaySummary>> =
        dao.observeAll().map { rows -> rows.associate { r -> r.toDomain().let { it.date to it } } }

    suspend fun dayDetail(date: LocalDate): List<DayDetailItem> = dao.dayDetail(date.toString()).mapNotNull { r ->
        CountContext.fromStorage(r.context)?.let { DayDetailItem(it, r.transliteration, r.arabic, r.completedCount, r.target) }
    }
}
