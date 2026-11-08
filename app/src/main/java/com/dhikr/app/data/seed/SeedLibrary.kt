package com.dhikr.app.data.seed

/**
 * Initial library. Every reference below was checked against sunnah.com on 2026-11-08
 * (see docs/SEED_LIBRARY.md). Where the hadith gives no count, the target is only a
 * personal default and the reference says so. Never add an entry without a verified source.
 *
 * [key] is permanent: it lets seeding skip entries that already exist or that the user
 * deleted. Never change or reuse a key.
 */
data class SeedDhikr(
    val key: String,
    val arabic: String,
    val transliteration: String,
    val translation: String,
    val reference: String,
    val target: Int,
    val enabledByDefault: Boolean,
)

object SeedLibrary {
    val entries: List<SeedDhikr> = listOf(
        SeedDhikr(
            key = "tasbih",
            arabic = "سُبْحَانَ ٱللَّٰهِ",
            transliteration = "SubhanAllah",
            translation = "Glory be to Allah",
            reference = "33 times after every prayer — Sahih Muslim 597a",
            target = 33,
            enabledByDefault = true,
        ),
        SeedDhikr(
            key = "tahmid",
            arabic = "ٱلْحَمْدُ لِلَّٰهِ",
            transliteration = "Alhamdulillah",
            translation = "All praise is due to Allah",
            reference = "33 times after every prayer — Sahih Muslim 597a",
            target = 33,
            enabledByDefault = true,
        ),
        SeedDhikr(
            key = "takbir",
            arabic = "ٱللَّٰهُ أَكْبَرُ",
            transliteration = "Allahu Akbar",
            translation = "Allah is the Greatest",
            reference = "33 times after every prayer — Sahih Muslim 597a",
            target = 33,
            enabledByDefault = true,
        ),
        SeedDhikr(
            key = "tahlil",
            arabic = "لَا إِلَٰهَ إِلَّا ٱللَّٰهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ ٱلْمُلْكُ وَلَهُ ٱلْحَمْدُ، وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ",
            transliteration = "La ilaha illallahu wahdahu la sharika lah, lahul-mulku wa lahul-hamd, wa huwa 'ala kulli shay'in qadir",
            translation = "None has the right to be worshipped but Allah alone, without partner. His is the dominion and His is the praise, and He has power over all things.",
            reference = "100 times a day — Sahih al-Bukhari 3293; once to complete 100 after prayer — Sahih Muslim 597a",
            target = 100,
            enabledByDefault = false,
        ),
        SeedDhikr(
            key = "istighfar",
            arabic = "أَسْتَغْفِرُ ٱللَّٰهَ",
            transliteration = "Astaghfirullah",
            translation = "I seek forgiveness from Allah",
            reference = "\"I seek forgiveness from Allah a hundred times a day\" — Sahih Muslim 2702a",
            target = 100,
            enabledByDefault = true,
        ),
        SeedDhikr(
            key = "subhanallah_wa_bihamdihi",
            arabic = "سُبْحَانَ ٱللَّٰهِ وَبِحَمْدِهِ",
            transliteration = "SubhanAllahi wa bihamdihi",
            translation = "Glory be to Allah and praise be to Him",
            reference = "100 times a day — Sahih al-Bukhari 6405",
            target = 100,
            enabledByDefault = false,
        ),
        SeedDhikr(
            key = "two_light_words",
            arabic = "سُبْحَانَ ٱللَّٰهِ ٱلْعَظِيمِ، سُبْحَانَ ٱللَّٰهِ وَبِحَمْدِهِ",
            transliteration = "SubhanAllahil-'Azim, SubhanAllahi wa bihamdihi",
            translation = "Glory be to Allah the Magnificent; glory be to Allah and praise be to Him",
            reference = "Sahih al-Bukhari 6406 (no count given; 100 is a personal default)",
            target = 100,
            enabledByDefault = false,
        ),
        SeedDhikr(
            key = "hawqala",
            arabic = "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِٱللَّٰهِ",
            transliteration = "La hawla wa la quwwata illa billah",
            translation = "There is no might and no power except with Allah",
            reference = "A treasure of Paradise — Sahih al-Bukhari 6384 (no count given; 100 is a personal default)",
            target = 100,
            enabledByDefault = false,
        ),
        SeedDhikr(
            key = "salam_after_prayer",
            arabic = "ٱللَّٰهُمَّ أَنْتَ ٱلسَّلَامُ وَمِنْكَ ٱلسَّلَامُ، تَبَارَكْتَ ذَا ٱلْجَلَالِ وَٱلْإِكْرَامِ",
            transliteration = "Allahumma antas-salam wa minkas-salam, tabarakta dhal-jalali wal-ikram",
            translation = "O Allah, You are Peace and from You is peace. Blessed are You, Possessor of majesty and honour.",
            reference = "After the salam, following istighfar three times — Sahih Muslim 591",
            target = 1,
            enabledByDefault = false,
        ),
        SeedDhikr(
            key = "salawat",
            arabic = "ٱللَّٰهُمَّ صَلِّ عَلَىٰ مُحَمَّدٍ",
            transliteration = "Allahumma salli 'ala Muhammad",
            translation = "O Allah, send blessings upon Muhammad",
            reference = "Sahih Muslim 408 (no count given; 100 is a personal default)",
            target = 100,
            enabledByDefault = false,
        ),
    )

    /**
     * Default after-prayer set (D-022), in order, with per-prayer counts:
     *  - Astaghfirullah ×3 then "Allahumma antas-salam" — Sahih Muslim 591
     *  - SubhanAllah ×33, Alhamdulillah ×33, Allahu Akbar ×33, then the tahlil once to complete
     *    100 — Sahih Muslim 597a. (Muslim 596a narrates takbir ×34; the user can set 34.)
     * Seeded once on a fresh install; afterwards it belongs to the user.
     */
    val prayerSet: List<Pair<String, Int>> = listOf(
        "istighfar" to 3,
        "salam_after_prayer" to 1,
        "tasbih" to 33,
        "tahmid" to 33,
        "takbir" to 33,
        "tahlil" to 1,
    )
}
