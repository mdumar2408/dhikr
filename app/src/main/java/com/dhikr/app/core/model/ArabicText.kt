package com.dhikr.app.core.model

import java.text.Normalizer

/**
 * Arabic text helpers (dhikr-domain-engineering skill).
 * Stored text keeps its harakat; search uses a separate, forgiving key.
 */
object ArabicText {

    fun nfc(s: String): String = Normalizer.normalize(s, Normalizer.Form.NFC)

    /**
     * Search key: lower-cased Latin, Arabic without harakat/tatweel, unified alef forms,
     * ta marbuta → ha, alef maqsura → ya. "سُبْحَانَ ٱللَّٰهِ" and "سبحان الله" give the same key.
     */
    fun searchKey(s: String): String {
        val sb = StringBuilder(s.length)
        for (ch in nfc(s)) {
            when (ch) {
                in 'ً'..'ٟ', 'ٰ', 'ـ', 'ۖ', in 'ۗ'..'ۭ' -> Unit // harakat, dagger alef, tatweel, Quranic marks
                'أ', 'إ', 'آ', 'ٱ' -> sb.append('ا')
                'ة' -> sb.append('ه')
                'ى' -> sb.append('ي')
                else -> sb.append(ch.lowercaseChar())
            }
        }
        return sb.toString().replace(Regex("[\\s'’`\\-]+"), " ").trim()
    }
}
