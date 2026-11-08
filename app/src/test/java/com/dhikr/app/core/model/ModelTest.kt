package com.dhikr.app.core.model

import com.dhikr.app.data.seed.SeedLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.Normalizer

class ArabicTextTest {
    @Test
    fun `diacritised and plain arabic give the same search key`() {
        assertEquals(ArabicText.searchKey("سبحان الله"), ArabicText.searchKey("سُبْحَانَ ٱللَّٰهِ"))
    }

    @Test
    fun `alef forms, ta marbuta and alef maqsura are unified`() {
        assertEquals(ArabicText.searchKey("اكبر"), ArabicText.searchKey("أَكْبَرُ"))
        assertEquals(ArabicText.searchKey("صلاه"), ArabicText.searchKey("صلاة"))
        assertEquals(ArabicText.searchKey("علي"), ArabicText.searchKey("عَلَىٰ"))
    }

    @Test
    fun `latin is case-insensitive and punctuation-tolerant`() {
        assertEquals(ArabicText.searchKey("subhanallah"), ArabicText.searchKey("SubhanAllah"))
        assertEquals(ArabicText.searchKey("la ilaha"), ArabicText.searchKey("La  ilaha"))
        assertNotEquals(ArabicText.searchKey("alhamdulillah"), ArabicText.searchKey("allahu akbar"))
    }
}

class DhikrDraftTest {
    @Test
    fun `needs arabic or transliteration`() {
        assertEquals(setOf(DhikrDraft.Error.NeedsText), DhikrDraft().validate())
        assertTrue(DhikrDraft(arabic = "سبحان الله").validate().isEmpty())
        assertTrue(DhikrDraft(transliteration = "SubhanAllah").validate().isEmpty())
    }

    @Test
    fun `target must be within range`() {
        assertEquals(setOf(DhikrDraft.Error.TargetOutOfRange), DhikrDraft(transliteration = "x", target = 0).validate())
        assertEquals(setOf(DhikrDraft.Error.TargetOutOfRange), DhikrDraft(transliteration = "x", target = 100_001).validate())
        assertTrue(DhikrDraft(transliteration = "x", target = 1).validate().isEmpty())
    }

    @Test
    fun `normalized trims and NFC-normalises`() {
        val decomposed = Normalizer.normalize("أ", Normalizer.Form.NFD)
        val n = DhikrDraft(arabic = "  $decomposed ", transliteration = " x ").normalized()
        assertEquals("أ", n.arabic)
        assertEquals("x", n.transliteration)
    }
}

class SeedLibraryTest {
    @Test
    fun `seed keys are unique and every entry is sourced`() {
        val keys = SeedLibrary.entries.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
        SeedLibrary.entries.forEach { s ->
            assertTrue("${s.key} has no reference", s.reference.isNotBlank())
            assertTrue("${s.key} target", s.target > 0)
            assertTrue("${s.key} arabic", s.arabic.isNotBlank())
            assertEquals("${s.key} not NFC", Normalizer.normalize(s.arabic, Normalizer.Form.NFC), s.arabic)
        }
    }

    @Test
    fun `prayer set refers only to seeded keys with positive counts`() {
        val keys = SeedLibrary.entries.map { it.key }.toSet()
        SeedLibrary.prayerSet.forEach { (k, n) ->
            assertTrue("unknown $k", k in keys)
            assertTrue(n > 0)
        }
        // 33 + 33 + 33 + 1 = 100 (Sahih Muslim 597a)
        val counts = SeedLibrary.prayerSet.toMap()
        assertEquals(100, counts.getValue("tasbih") + counts.getValue("tahmid") + counts.getValue("takbir") + counts.getValue("tahlil"))
    }
}
