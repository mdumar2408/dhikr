package com.dhikr.app.data.progress

import com.dhikr.app.core.model.CountContext
import com.dhikr.app.data.database.DailySummaryEntity
import com.dhikr.app.data.database.ProgressDao
import com.dhikr.app.data.database.SummaryDao
import com.dhikr.app.domain.history.DaySummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate

/** One stored count: how many times [dhikrId] was said on a date, in a context. */
data class ProgressRecord(val dhikrId: Long, val context: CountContext, val count: Int, val target: Int)

/**
 * Where counts live. An interface so the engine can be unit-tested with an in-memory fake,
 * while the app uses Room ([RoomProgressStore]).
 */
interface ProgressStore {
    fun observeDate(date: LocalDate): Flow<List<ProgressRecord>>
    suspend fun increment(dhikrId: Long, date: LocalDate, context: CountContext, target: Int)
    suspend fun decrement(dhikrId: Long, date: LocalDate, context: CountContext)
    suspend fun reset(dhikrId: Long, date: LocalDate, context: CountContext)

    /** The day's summary, rewritten whenever that day's counts or (today's) routines change. */
    suspend fun saveSummary(summary: DaySummary)
}

class RoomProgressStore(
    private val dao: ProgressDao,
    private val summaryDao: SummaryDao,
    private val clock: Clock,
) : ProgressStore {

    override fun observeDate(date: LocalDate): Flow<List<ProgressRecord>> =
        dao.observeDate(date.toString()).map { rows ->
            rows.mapNotNull { r ->
                CountContext.fromStorage(r.context)?.let { ProgressRecord(r.dhikrId, it, r.completedCount, r.target) }
            }
        }

    override suspend fun increment(dhikrId: Long, date: LocalDate, context: CountContext, target: Int) =
        dao.increment(dhikrId, date.toString(), context.storageKey, target, clock.millis())

    override suspend fun decrement(dhikrId: Long, date: LocalDate, context: CountContext) =
        dao.decrement(dhikrId, date.toString(), context.storageKey, clock.millis())

    override suspend fun reset(dhikrId: Long, date: LocalDate, context: CountContext) =
        dao.reset(dhikrId, date.toString(), context.storageKey, clock.millis())

    override suspend fun saveSummary(summary: DaySummary) = summaryDao.upsert(summary.toEntity(clock.millis()))
}

fun DaySummary.toEntity(now: Long) = DailySummaryEntity(
    localDate = date.toString(),
    prayersCompleted = prayersCompleted,
    prayersMask = prayersMask,
    totalTarget = totalTarget,
    totalCompleted = totalCompleted,
    completedDhikrCount = completedDhikrCount,
    isCompleted = isCompleted,
    updatedAt = now,
)

fun DailySummaryEntity.toDomain() = DaySummary(
    date = LocalDate.parse(localDate),
    prayersMask = prayersMask,
    totalTarget = totalTarget,
    totalCompleted = totalCompleted,
    completedDhikrCount = completedDhikrCount,
    isCompleted = isCompleted,
)
