package com.dhikr.app.data.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

/** One past-day progress row with its dhikr's names (including soft-deleted dhikrs). */
data class DayDetailRow(
    val context: String,
    val transliteration: String,
    val arabic: String,
    @androidx.room3.ColumnInfo(name = "completed_count") val completedCount: Int,
    val target: Int,
)

@Dao
interface SummaryDao {

    /** Replaces the whole row for that date (it's derived data; nothing references it). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(summary: DailySummaryEntity)

    @Query("SELECT * FROM daily_summary ORDER BY local_date")
    fun observeAll(): Flow<List<DailySummaryEntity>>

    @Query(
        """
        SELECT p.context, d.transliteration, d.arabic, p.completed_count, p.target
        FROM daily_progress p JOIN dhikr d ON d.id = p.dhikr_id
        WHERE p.local_date = :date AND p.completed_count > 0
        ORDER BY p.context, p.id -- rows are created on the first count, so this is the order they were said
        """,
    )
    suspend fun dayDetail(date: String): List<DayDetailRow>
}
