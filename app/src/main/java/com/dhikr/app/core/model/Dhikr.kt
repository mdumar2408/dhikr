package com.dhikr.app.core.model

/**
 * A saved Dhikr in the user's library (domain model; persisted as DhikrEntity).
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
    /** Source or note, e.g. "Sahih Muslim 597a". Empty for user-made entries without one. */
    val reference: String = "",
    val isSystemProvided: Boolean = false,
    /** App-private audio file ("audio/<uuid>.<ext>" under filesDir), or null. */
    val audioRef: String? = null,
) {
    init {
        require(target > 0) { "target must be positive" }
        require(arabic.isNotBlank() || transliteration.isNotBlank()) { "a dhikr needs Arabic or transliteration" }
    }

    /** Best label for places that need one short name. */
    val displayName: String get() = transliteration.ifBlank { arabic }
}

/** What the add/edit form produces. Validation lives here, not in the UI. */
data class DhikrDraft(
    val arabic: String = "",
    val transliteration: String = "",
    val translation: String = "",
    val reference: String = "",
    val target: Int = DEFAULT_TARGET,
) {
    enum class Error { NeedsText, TargetOutOfRange }

    fun validate(): Set<Error> = buildSet {
        if (arabic.isBlank() && transliteration.isBlank()) add(Error.NeedsText)
        if (target !in 1..MAX_TARGET) add(Error.TargetOutOfRange)
    }

    /** Trimmed, Unicode-NFC-normalised copy — the form keeps what the user typed, storage gets this. */
    fun normalized(): DhikrDraft = copy(
        arabic = ArabicText.nfc(arabic.trim()),
        transliteration = transliteration.trim(),
        translation = translation.trim(),
        reference = reference.trim(),
    )

    companion object {
        const val DEFAULT_TARGET = 33
        const val MAX_TARGET = 100_000

        fun from(d: Dhikr) = DhikrDraft(d.arabic, d.transliteration, d.translation, d.reference, d.target)
    }
}
