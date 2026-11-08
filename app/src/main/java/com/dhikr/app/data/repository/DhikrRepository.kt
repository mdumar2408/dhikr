package com.dhikr.app.data.repository

import com.dhikr.app.core.model.ArabicText
import com.dhikr.app.core.model.Dhikr
import com.dhikr.app.core.model.DhikrDraft
import com.dhikr.app.data.database.DhikrDao
import com.dhikr.app.data.database.DhikrEntity
import com.dhikr.app.data.database.PrayerSetDao
import com.dhikr.app.data.database.PrayerSetItemEntity
import com.dhikr.app.core.model.PrayerSetItem
import com.dhikr.app.data.seed.SeedLibrary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock

/**
 * The library's single source of truth (Room). The app, and later widgets and the notification,
 * all read and write through here.
 */
class DhikrRepository(
    private val dao: DhikrDao,
    private val prayerSetDao: PrayerSetDao,
    private val clock: Clock,
) {

    fun observeLibrary(): Flow<List<Dhikr>> = dao.observeLibrary().map { rows -> rows.map { it.toDomain() } }

    /** The after-prayer set, in order (D-022). */
    fun observePrayerSet(): Flow<List<PrayerSetItem>> = prayerSetDao.observeSet().map { rows ->
        rows.map { PrayerSetItem(it.item.id, it.dhikr.toDomain(), it.item.position, it.item.target) }
    }

    /** Appends a Dhikr to the after-prayer set. Defaults to its own daily target, capped at 33. */
    suspend fun addToPrayerSet(dhikrId: Long, target: Int) {
        require(target in 1..DhikrDraft.MAX_TARGET)
        prayerSetDao.insert(PrayerSetItemEntity(dhikrId = dhikrId, position = prayerSetDao.maxPosition() + 1, target = target))
    }

    suspend fun removeFromPrayerSet(itemId: Long) = prayerSetDao.delete(itemId)

    suspend fun setPrayerTarget(itemId: Long, target: Int) {
        require(target in 1..DhikrDraft.MAX_TARGET)
        prayerSetDao.setTarget(itemId, target)
    }

    suspend fun reorderPrayerSet(itemIdsInOrder: List<Long>) = prayerSetDao.reorder(itemIdsInOrder)

    /**
     * Inserts seed entries that have never existed. Idempotent, and never resurrects a seed the
     * user deleted (soft-deleted rows keep their seed_key). New seeds in future app versions are
     * appended after the user's own order.
     */
    suspend fun ensureSeeded() {
        val existing = dao.allSeedKeys().toSet()
        val freshInstall = existing.isEmpty()
        val missing = SeedLibrary.entries.filter { it.key !in existing }
        if (missing.isEmpty()) return
        insertSeedDhikrs(missing)
        // The prayer set is seeded once, on a fresh install; after that it belongs to the user.
        if (freshInstall && prayerSetDao.count() == 0) {
            prayerSetDao.insertAll(
                SeedLibrary.prayerSet.mapIndexedNotNull { i, (key, target) ->
                    dao.bySeedKey(key)?.let { PrayerSetItemEntity(dhikrId = it.id, position = i, target = target) }
                },
            )
        }
    }

    private suspend fun insertSeedDhikrs(missing: List<com.dhikr.app.data.seed.SeedDhikr>) {
        val now = clock.millis()
        val start = dao.maxSortOrder() + 1
        dao.insertSeeds(
            missing.mapIndexed { i, s ->
                DhikrEntity(
                    seedKey = s.key,
                    arabic = ArabicText.nfc(s.arabic),
                    transliteration = s.transliteration,
                    translation = s.translation,
                    reference = s.reference,
                    defaultTarget = s.target,
                    isEnabled = s.enabledByDefault,
                    isSystemProvided = true,
                    sortOrder = start + i,
                    searchKey = searchKeyOf(s.arabic, s.transliteration, s.translation),
                    createdAt = now,
                    updatedAt = now,
                )
            },
        )
    }

    /** New entries are enabled and go to the end of the routine. Returns the new id. */
    suspend fun add(draft: DhikrDraft): Long {
        val d = draft.normalized()
        require(d.validate().isEmpty()) { "invalid draft: ${d.validate()}" }
        val now = clock.millis()
        return dao.insert(
            DhikrEntity(
                arabic = d.arabic,
                transliteration = d.transliteration,
                translation = d.translation,
                reference = d.reference,
                defaultTarget = d.target,
                isEnabled = true,
                isSystemProvided = false,
                sortOrder = dao.maxSortOrder() + 1,
                searchKey = searchKeyOf(d.arabic, d.transliteration, d.translation),
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun update(id: Long, draft: DhikrDraft) {
        val d = draft.normalized()
        require(d.validate().isEmpty()) { "invalid draft: ${d.validate()}" }
        val current = dao.byId(id) ?: return
        dao.update(
            current.copy(
                arabic = d.arabic,
                transliteration = d.transliteration,
                translation = d.translation,
                reference = d.reference,
                defaultTarget = d.target,
                searchKey = searchKeyOf(d.arabic, d.transliteration, d.translation),
                updatedAt = clock.millis(),
            ),
        )
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) = dao.setEnabled(id, enabled, clock.millis())

    /** Attach (or with null, detach) an app-private recording. Returns the previous ref so the caller can delete the file. */
    suspend fun setAudio(id: Long, ref: String?): String? {
        val previous = dao.byId(id)?.audioRef
        dao.setAudio(id, ref, clock.millis())
        return previous
    }

    /** Soft delete: hidden from the library, history kept. Returns the previous enabled state for undo. */
    suspend fun delete(id: Long): Boolean {
        val wasEnabled = dao.byId(id)?.isEnabled ?: false
        dao.softDelete(id, clock.millis())
        return wasEnabled
    }

    suspend fun restore(id: Long, enabled: Boolean) = dao.restore(id, enabled, clock.millis())

    suspend fun reorder(idsInOrder: List<Long>) = dao.reorder(idsInOrder, clock.millis())

    private fun searchKeyOf(vararg parts: String) = ArabicText.searchKey(parts.joinToString(" "))
}

internal fun DhikrEntity.toDomain() = Dhikr(
    id = id,
    arabic = arabic,
    transliteration = transliteration,
    translation = translation,
    target = defaultTarget,
    isEnabled = isEnabled,
    sortOrder = sortOrder,
    reference = reference,
    isSystemProvided = isSystemProvided,
    audioRef = audioRef,
)
