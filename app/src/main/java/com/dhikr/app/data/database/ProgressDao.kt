package com.dhikr.app.data.database

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Daily counts per (dhikr, local date, context) — D-022.
 *
 * Increments are done in SQL (`completed_count + 1`), never read-modify-write in Kotlin, so
 * concurrent writers (app, widget, notification) can't lose taps. UPSERT needs SQLite 3.24
 * (Android 11+); minSdk is 26, so we use INSERT OR IGNORE + UPDATE inside one transaction.
 */
@Dao
interface ProgressDao {

    @Query("SELECT * FROM daily_progress WHERE local_date = :date")
    fun observeDate(date: String): Flow<List<DailyProgressEntity>>

    @Query("SELECT * FROM daily_progress WHERE local_date = :date AND context = :context AND dhikr_id = :dhikrId")
    suspend fun get(dhikrId: Long, date: String, context: String): DailyProgressEntity?

    @Query(
        """
        INSERT OR IGNORE INTO daily_progress (dhikr_id, local_date, context, target, completed_count, created_at, updated_at)
        VALUES (:dhikrId, :date, :context, :target, 0, :now, :now)
        """,
    )
    suspend fun ensureRow(dhikrId: Long, date: String, context: String, target: Int, now: Long)

    /** Also refreshes today's target snapshot: a target edited during the day applies to today (rule 2). */
    @Query(
        """
        UPDATE daily_progress SET completed_count = completed_count + :delta, target = :target, updated_at = :now
        WHERE dhikr_id = :dhikrId AND local_date = :date AND context = :context
        """,
    )
    suspend fun add(dhikrId: Long, date: String, context: String, delta: Int, target: Int, now: Long)

    @Query(
        """
        UPDATE daily_progress SET completed_count = MAX(completed_count - 1, 0), updated_at = :now
        WHERE dhikr_id = :dhikrId AND local_date = :date AND context = :context
        """,
    )
    suspend fun decrement(dhikrId: Long, date: String, context: String, now: Long)

    @Query(
        """
        UPDATE daily_progress SET completed_count = 0, updated_at = :now
        WHERE dhikr_id = :dhikrId AND local_date = :date AND context = :context
        """,
    )
    suspend fun reset(dhikrId: Long, date: String, context: String, now: Long)

    @Transaction
    suspend fun increment(dhikrId: Long, date: String, context: String, target: Int, now: Long) {
        ensureRow(dhikrId, date, context, target, now)
        add(dhikrId, date, context, 1, target, now)
    }
}
