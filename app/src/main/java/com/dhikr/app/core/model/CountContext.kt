package com.dhikr.app.core.model

/**
 * Where a count belongs (D-022). The five obligatory prayers each have their own after-prayer
 * session; [Daily] is the user's own targets outside the prayers.
 * [storageKey] is persisted — never change these strings.
 */
enum class CountContext(val storageKey: String, val label: String) {
    Fajr("fajr", "Fajr"),
    Dhuhr("dhuhr", "Dhuhr"),
    Asr("asr", "Asr"),
    Maghrib("maghrib", "Maghrib"),
    Isha("isha", "Isha"),
    Daily("daily", "Daily");

    val isPrayer: Boolean get() = this != Daily

    companion object {
        val prayers: List<CountContext> = listOf(Fajr, Dhuhr, Asr, Maghrib, Isha)
        fun fromStorage(key: String): CountContext? = entries.firstOrNull { it.storageKey == key }
    }
}

/** One entry of the after-prayer set: which Dhikr and how many times after each prayer. */
data class PrayerSetItem(val itemId: Long, val dhikr: Dhikr, val position: Int, val target: Int)
