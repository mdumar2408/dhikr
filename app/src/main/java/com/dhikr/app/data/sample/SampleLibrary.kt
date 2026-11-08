package com.dhikr.app.data.sample

import com.dhikr.app.core.model.Dhikr

/**
 * Phase 1 in-memory library so the UI shell has real content.
 * Replaced by the Room-backed, source-referenced seed library in Phase 2.
 * These are the common post-prayer tasbih phrases (33 each) plus istighfar.
 */
object SampleLibrary {
    val dhikrs: List<Dhikr> = listOf(
        Dhikr(1, "سُبْحَانَ ٱللَّٰهِ", "SubhanAllah", "Glory be to Allah", 33, isEnabled = true, sortOrder = 0),
        Dhikr(2, "ٱلْحَمْدُ لِلَّٰهِ", "Alhamdulillah", "All praise is due to Allah", 33, isEnabled = true, sortOrder = 1),
        Dhikr(3, "ٱللَّٰهُ أَكْبَرُ", "Allahu Akbar", "Allah is the Greatest", 33, isEnabled = true, sortOrder = 2),
        Dhikr(4, "أَسْتَغْفِرُ ٱللَّٰهَ", "Astaghfirullah", "I seek forgiveness from Allah", 100, isEnabled = true, sortOrder = 3),
        Dhikr(5, "لَا إِلَٰهَ إِلَّا ٱللَّٰهُ", "La ilaha illallah", "There is no god but Allah", 100, isEnabled = false, sortOrder = 4),
    )
}
