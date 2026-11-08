package com.dhikr.app.data

import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dhikr.app.data.database.DhikrDatabase
import com.dhikr.app.data.repository.DhikrRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock

/** Real Room 3 on the device: atomic increments, context separation, floors. */
@RunWith(AndroidJUnit4::class)
class ProgressDaoTest {

    private lateinit var db: DhikrDatabase
    private var dhikrId = 0L
    private val date = "2026-11-08"

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), DhikrDatabase::class.java)
            .setDriver(AndroidSQLiteDriver())
            .build()
        val repo = DhikrRepository(db.dhikrDao(), db.prayerSetDao(), Clock.systemUTC())
        repo.ensureSeeded()
        dhikrId = repo.observeLibrary().first().first().id
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun concurrentIncrementsAreAllCounted() = runTest {
        val dao = db.progressDao()
        withContext(Dispatchers.IO) {
            (1..200).map { async { dao.increment(dhikrId, date, "daily", 33, it.toLong()) } }.awaitAll()
        }
        assertEquals(200, dao.get(dhikrId, date, "daily")?.completedCount)
    }

    @Test
    fun contextsAreSeparateRows() = runTest {
        val dao = db.progressDao()
        repeat(3) { dao.increment(dhikrId, date, "fajr", 3, 0) }
        repeat(5) { dao.increment(dhikrId, date, "daily", 100, 0) }
        assertEquals(3, dao.get(dhikrId, date, "fajr")?.completedCount)
        assertEquals(5, dao.get(dhikrId, date, "daily")?.completedCount)
        assertEquals(2, dao.observeDate(date).first().size)
    }

    @Test
    fun decrementNeverGoesBelowZeroAndResetZeroes() = runTest {
        val dao = db.progressDao()
        dao.increment(dhikrId, date, "daily", 33, 0)
        repeat(3) { dao.decrement(dhikrId, date, "daily", 0) }
        assertEquals(0, dao.get(dhikrId, date, "daily")?.completedCount)
        repeat(4) { dao.increment(dhikrId, date, "daily", 33, 0) }
        dao.reset(dhikrId, date, "daily", 0)
        assertEquals(0, dao.get(dhikrId, date, "daily")?.completedCount)
    }

    @Test
    fun targetSnapshotFollowsTodaysEdits() = runTest {
        val dao = db.progressDao()
        dao.increment(dhikrId, date, "daily", 33, 0)
        dao.increment(dhikrId, date, "daily", 100, 0) // target edited during the day
        assertEquals(100, dao.get(dhikrId, date, "daily")?.target)
    }
}
