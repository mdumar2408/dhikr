package com.dhikr.app.data.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

/** Reads are Flows, writes are suspend — never on the main thread (Room 3 enforces this). */
@Dao
interface DhikrDao {

    @Query("SELECT * FROM dhikr WHERE deleted_at IS NULL ORDER BY sort_order, id")
    fun observeLibrary(): Flow<List<DhikrEntity>>

    @Query("SELECT * FROM dhikr WHERE id = :id")
    suspend fun byId(id: Long): DhikrEntity?

    @Query("SELECT * FROM dhikr WHERE seed_key = :key")
    suspend fun bySeedKey(key: String): DhikrEntity?

    @Query("SELECT seed_key FROM dhikr WHERE seed_key IS NOT NULL")
    suspend fun allSeedKeys(): List<String>

    @Query("SELECT COALESCE(MAX(sort_order), -1) FROM dhikr WHERE deleted_at IS NULL")
    suspend fun maxSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: DhikrEntity): Long

    /** Seed rows: IGNORE so a seed_key collision (already seeded, or user-deleted) is a no-op. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSeeds(entities: List<DhikrEntity>): List<Long>

    @Update
    suspend fun update(entity: DhikrEntity)

    @Query("UPDATE dhikr SET is_enabled = :enabled, updated_at = :now WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean, now: Long)

    @Query("UPDATE dhikr SET deleted_at = :now, is_enabled = 0, updated_at = :now WHERE id = :id")
    suspend fun softDelete(id: Long, now: Long)

    @Query("UPDATE dhikr SET deleted_at = NULL, is_enabled = :enabled, updated_at = :now WHERE id = :id")
    suspend fun restore(id: Long, enabled: Boolean, now: Long)

    @Query("UPDATE dhikr SET sort_order = :order, updated_at = :now WHERE id = :id")
    suspend fun setSortOrder(id: Long, order: Int, now: Long)

    /** Rewrites sort_order 0..n-1 in the given order, atomically. */
    @Transaction
    suspend fun reorder(idsInOrder: List<Long>, now: Long) {
        idsInOrder.forEachIndexed { index, id -> setSortOrder(id, index, now) }
    }
}
