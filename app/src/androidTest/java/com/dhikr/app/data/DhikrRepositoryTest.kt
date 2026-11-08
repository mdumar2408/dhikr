package com.dhikr.app.data

import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dhikr.app.core.model.DhikrDraft
import com.dhikr.app.data.database.DhikrDatabase
import com.dhikr.app.data.repository.DhikrRepository
import com.dhikr.app.data.seed.SeedLibrary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** Runs against a real (in-memory) Room 3 database on the device. */
@RunWith(AndroidJUnit4::class)
class DhikrRepositoryTest {

    private lateinit var db: DhikrDatabase
    private lateinit var repo: DhikrRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), DhikrDatabase::class.java)
            .setDriver(AndroidSQLiteDriver())
            .build()
        repo = DhikrRepository(db.dhikrDao(), db.prayerSetDao(), Clock.fixed(Instant.parse("2026-11-08T06:00:00Z"), ZoneOffset.UTC))
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun seedingIsIdempotentAndOrdered() = runTest {
        repo.ensureSeeded()
        repo.ensureSeeded()
        val lib = repo.observeLibrary().first()
        assertEquals(SeedLibrary.entries.size, lib.size)
        assertEquals(SeedLibrary.entries.map { it.transliteration }, lib.map { it.transliteration })
        assertEquals(listOf("SubhanAllah", "Alhamdulillah", "Allahu Akbar", "Astaghfirullah"),
            lib.filter { it.isEnabled }.map { it.transliteration })
    }

    @Test
    fun prayerSetIsSeededInOrderWithItsOwnCounts() = runTest {
        repo.ensureSeeded()
        val set = repo.observePrayerSet().first()
        assertEquals(listOf(3, 1, 33, 33, 33, 1), set.map { it.target })
        assertEquals("Astaghfirullah", set.first().dhikr.transliteration)
        assertEquals(100, set.first().dhikr.target) // the daily default is separate from the per-prayer count
    }

    @Test
    fun deletedSeedIsNeverResurrected() = runTest {
        repo.ensureSeeded()
        val tasbih = repo.observeLibrary().first().first { it.transliteration == "SubhanAllah" }
        repo.delete(tasbih.id)
        repo.ensureSeeded()
        assertFalse(repo.observeLibrary().first().any { it.id == tasbih.id })
    }

    @Test
    fun softDeleteHidesAndRestoreBringsBack() = runTest {
        repo.ensureSeeded()
        val d = repo.observeLibrary().first().first { it.isEnabled }
        val wasEnabled = repo.delete(d.id)
        assertTrue(wasEnabled)
        assertFalse(repo.observeLibrary().first().any { it.id == d.id })
        repo.restore(d.id, wasEnabled)
        assertTrue(repo.observeLibrary().first().first { it.id == d.id }.isEnabled)
    }

    @Test
    fun addAppendsEnabledAndEditUpdates() = runTest {
        repo.ensureSeeded()
        val id = repo.add(DhikrDraft(transliteration = "  Ya Hayyu Ya Qayyum ", target = 41))
        val added = repo.observeLibrary().first().last()
        assertEquals(id, added.id)
        assertEquals("Ya Hayyu Ya Qayyum", added.transliteration)
        assertTrue(added.isEnabled)
        repo.update(id, DhikrDraft(transliteration = "Ya Hayyu Ya Qayyum", target = 100))
        assertEquals(100, repo.observeLibrary().first().last().target)
    }

    @Test
    fun reorderPersists() = runTest {
        repo.ensureSeeded()
        val ids = repo.observeLibrary().first().map { it.id }
        val reversed = ids.reversed()
        repo.reorder(reversed)
        assertEquals(reversed, repo.observeLibrary().first().map { it.id })
    }

    @Test
    fun toggleEnabledPersists() = runTest {
        repo.ensureSeeded()
        val d = repo.observeLibrary().first().first { !it.isEnabled }
        repo.setEnabled(d.id, true)
        assertTrue(repo.observeLibrary().first().first { it.id == d.id }.isEnabled)
    }
}
