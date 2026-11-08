package com.dhikr.app.data.database

import androidx.room3.Dao
import androidx.room3.Embedded
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

/** A prayer-set row joined with its Dhikr. */
data class PrayerSetRow(
    @Embedded(prefix = "item_") val item: PrayerSetItemEntity,
    @Embedded val dhikr: DhikrEntity,
)

@Dao
interface PrayerSetDao {

    /** The after-prayer set in order. Soft-deleted Dhikrs drop out of the set automatically. */
    @Query(
        """
        SELECT p.id AS item_id, p.dhikr_id AS item_dhikr_id, p.position AS item_position, p.target AS item_target, d.*
        FROM prayer_set_item p JOIN dhikr d ON d.id = p.dhikr_id
        WHERE d.deleted_at IS NULL
        ORDER BY p.position, p.id
        """,
    )
    fun observeSet(): Flow<List<PrayerSetRow>>

    @Query("SELECT COUNT(*) FROM prayer_set_item")
    suspend fun count(): Int

    @Insert
    suspend fun insertAll(items: List<PrayerSetItemEntity>)
}
