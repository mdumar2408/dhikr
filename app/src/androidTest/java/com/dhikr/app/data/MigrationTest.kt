package com.dhikr.app.data

import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.room3.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dhikr.app.data.database.DhikrDatabase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * offline-first-android: every schema bump ships with a migration test. v1 → v2 must keep all
 * data (the owner's phone already has real counts) and add prayers_mask with default 0.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val file = File(instrumentation.targetContext.cacheDir, "migration-test.db")

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = instrumentation,
        file = file,
        driver = AndroidSQLiteDriver(),
        databaseClass = DhikrDatabase::class,
    )

    @Test
    fun v1ToV2KeepsDataAndAddsPrayerMask() = runTest {
        helper.createDatabase(1).use { c ->
            c.execSQL(
                """INSERT INTO dhikr (id, seed_key, arabic, transliteration, translation, reference, default_target,
                   audio_ref, is_enabled, is_system_provided, sort_order, search_key, created_at, updated_at, deleted_at)
                   VALUES (1, 'tasbih', 'سبحان الله', 'SubhanAllah', '', '', 33, NULL, 1, 1, 0, 'x', 0, 0, NULL)""",
            )
            c.execSQL(
                """INSERT INTO daily_progress (dhikr_id, local_date, context, target, completed_count, created_at, updated_at)
                   VALUES (1, '2026-11-08', 'fajr', 33, 33, 0, 0)""",
            )
            c.execSQL(
                """INSERT INTO daily_summary (local_date, prayers_completed, total_target, total_completed,
                   completed_dhikr_count, is_completed, updated_at) VALUES ('2026-11-08', 1, 33, 33, 1, 0, 0)""",
            )
        }
        helper.runMigrationsAndValidate(2).use { c ->
            c.prepare("SELECT completed_count FROM daily_progress WHERE local_date = '2026-11-08'").use { s ->
                s.step(); assertEquals(33L, s.getLong(0))
            }
            c.prepare("SELECT prayers_completed, prayers_mask FROM daily_summary WHERE local_date = '2026-11-08'").use { s ->
                s.step()
                assertEquals(1L, s.getLong(0))
                assertEquals(0L, s.getLong(1))
            }
        }
    }
}
