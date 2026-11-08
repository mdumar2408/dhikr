package com.dhikr.app.data.database

import android.content.Context
import androidx.room3.AutoMigration
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver

/**
 * Schema history (offline-first-android: never destructive):
 *  v1 — Phase 2/3 tables.
 *  v2 — daily_summary.prayers_mask (auto-migration; existing rows get 0).
 */
@Database(
    entities = [DhikrEntity::class, DailyProgressEntity::class, PrayerSetItemEntity::class, DailySummaryEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class DhikrDatabase : RoomDatabase() {
    abstract fun dhikrDao(): DhikrDao
    abstract fun prayerSetDao(): PrayerSetDao
    abstract fun progressDao(): ProgressDao
    abstract fun summaryDao(): SummaryDao

    companion object {
        const val NAME = "dhikr.db"

        /**
         * One instance per process (AppContainer). Uses the platform SQLite via AndroidSQLiteDriver
         * (no bundled native library). No destructive fallback: a missing migration must crash in
         * development rather than silently wipe someone's history.
         */
        fun build(context: Context): DhikrDatabase =
            Room.databaseBuilder<DhikrDatabase>(context.applicationContext, NAME)
                .setDriver(AndroidSQLiteDriver())
                .build()
    }
}
