package com.dhikr.app.data.progress

import com.dhikr.app.core.model.CountContext
import com.dhikr.app.data.database.ProgressDao
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
}

class RoomProgressStore(private val dao: ProgressDao, private val clock: Clock) : ProgressStore {

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
}
