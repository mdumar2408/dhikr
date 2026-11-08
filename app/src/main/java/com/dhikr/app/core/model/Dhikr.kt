package com.dhikr.app.core.model

/**
 * A saved Dhikr in the user's library (domain model; the Room entity arrives in Phase 2).
 * [sortOrder] defines the daily routine order used by Flow (D-016).
 */
data class Dhikr(
    val id: Long,
    val arabic: String,
    val transliteration: String,
    val translation: String,
    val target: Int,
    val isEnabled: Boolean,
    val sortOrder: Int,
) {
    init {
        require(target > 0) { "target must be positive" }
        require(arabic.isNotBlank() || transliteration.isNotBlank()) { "a dhikr needs Arabic or transliteration" }
    }
}
