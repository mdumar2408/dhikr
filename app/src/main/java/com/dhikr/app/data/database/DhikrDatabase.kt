package com.dhikr.app.data.database

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver

@Database(
    entities = [DhikrEntity::class, DailyProgressEntity::class, PrayerSetItemEntity::class, DailySummaryEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class DhikrDatabase : RoomDatabase() {
    abstract fun dhikrDao(): DhikrDao
    abstract fun prayerSetDao(): PrayerSetDao

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
