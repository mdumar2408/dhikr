package com.dhikr.app.data.database

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Schema v1 (offline-first-android skill, D-101–D-103, D-016).
 *
 * Dhikrs are never hard-deleted ([deletedAt] = soft delete) so history keeps its foreign keys.
 * The progress and summary tables are created now so Phase 3 needs no migration.
 */
@Entity(
    tableName = "dhikr",
    indices = [
        Index(value = ["seed_key"], unique = true),
        Index(value = ["sort_order"]),
    ],
)
data class DhikrEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Stable key for seed entries; null for user-made ones. Unique, so seeding is idempotent. */
    @ColumnInfo(name = "seed_key") val seedKey: String? = null,
    val arabic: String,
    val transliteration: String,
    val translation: String,
    /** Source/note ("description/reference" in the spec). */
    val reference: String,
    @ColumnInfo(name = "default_target") val defaultTarget: Int,
    @ColumnInfo(name = "audio_ref") val audioRef: String? = null,
    @ColumnInfo(name = "is_enabled") val isEnabled: Boolean,
    @ColumnInfo(name = "is_system_provided") val isSystemProvided: Boolean,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    /** Normalised text for search (ArabicText.searchKey of all text fields). */
    @ColumnInfo(name = "search_key") val searchKey: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)

/**
 * One row per Dhikr, per local date, per *context* (D-022). The context separates the five
 * prayer sessions from daily targets: Astaghfirullah ×3 after Fajr and Astaghfirullah ×100 for
 * the day are different counters. [target] is a snapshot so history never changes (D-102).
 */
@Entity(
    tableName = "daily_progress",
    foreignKeys = [
        ForeignKey(
            entity = DhikrEntity::class,
            parentColumns = ["id"],
            childColumns = ["dhikr_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["dhikr_id", "local_date", "context"], unique = true),
        Index(value = ["local_date", "context"]),
    ],
)
data class DailyProgressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "dhikr_id") val dhikrId: Long,
    /** ISO yyyy-MM-dd in the device's local zone at the time of counting. */
    @ColumnInfo(name = "local_date") val localDate: String,
    /** CountContext.storageKey: "fajr", "dhuhr", "asr", "maghrib", "isha" or "daily". */
    val context: String,
    val target: Int,
    @ColumnInfo(name = "completed_count") val completedCount: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/**
 * The after-prayer set: an ordered list of Dhikrs with their own per-prayer targets
 * (independent of the Dhikr's daily default target). One template for all five prayers in V1.
 */
@Entity(
    tableName = "prayer_set_item",
    foreignKeys = [
        ForeignKey(
            entity = DhikrEntity::class,
            parentColumns = ["id"],
            childColumns = ["dhikr_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["dhikr_id"]), Index(value = ["position"])],
)
data class PrayerSetItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "dhikr_id") val dhikrId: Long,
    val position: Int,
    val target: Int,
)

/** Derived per-day totals, recomputed in the same transaction as progress changes (Phase 3/5). */
@Entity(tableName = "daily_summary")
data class DailySummaryEntity(
    @PrimaryKey @ColumnInfo(name = "local_date") val localDate: String,
    /** 0–5: prayer sessions whose whole set reached its targets. */
    @ColumnInfo(name = "prayers_completed") val prayersCompleted: Int,
    @ColumnInfo(name = "total_target") val totalTarget: Int,
    @ColumnInfo(name = "total_completed") val totalCompleted: Int,
    @ColumnInfo(name = "completed_dhikr_count") val completedDhikrCount: Int,
    /** Streak day (D-023): all five prayer sets AND every daily target complete. */
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
