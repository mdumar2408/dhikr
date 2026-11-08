package com.dhikr.app.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhikr.app.core.model.Dhikr
import com.dhikr.app.core.model.DhikrDraft
import com.dhikr.app.core.model.PrayerSetItem
import com.dhikr.app.data.repository.DhikrRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PrayerSetUiState(
    val loaded: Boolean = false,
    val items: List<PrayerSetItem> = emptyList(),
    /** Library dhikrs not yet in the set (each dhikr appears at most once). */
    val addable: List<Dhikr> = emptyList(),
    val picking: Boolean = false,
    /** Item whose count is being edited. */
    val editing: PrayerSetItem? = null,
)

/** Edits the after-prayer set (D-022). One template for all five prayers in V1. */
class PrayerSetViewModel(private val repo: DhikrRepository) : ViewModel() {

    private val ui = MutableStateFlow(PrayerSetUiState())

    val uiState: StateFlow<PrayerSetUiState> =
        combine(repo.observePrayerSet(), repo.observeLibrary(), ui) { set, lib, u ->
            val inSet = set.map { it.dhikr.id }.toSet()
            u.copy(loaded = true, items = set, addable = lib.filter { it.id !in inSet })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PrayerSetUiState())

    fun onPick() = ui.update { it.copy(picking = true) }
    fun onPickDismiss() = ui.update { it.copy(picking = false) }

    /** New items default to their daily target, capped at 33 (a typical after-prayer count). */
    fun onAdd(d: Dhikr) {
        ui.update { it.copy(picking = false) }
        viewModelScope.launch { repo.addToPrayerSet(d.id, minOf(d.target, 33)) }
    }

    fun onRemove(item: PrayerSetItem) {
        viewModelScope.launch { repo.removeFromPrayerSet(item.itemId) }
    }

    fun onEditCount(item: PrayerSetItem) = ui.update { it.copy(editing = item) }
    fun onEditDismiss() = ui.update { it.copy(editing = null) }

    fun onSaveCount(target: Int) {
        val item = ui.value.editing ?: return
        if (target !in 1..DhikrDraft.MAX_TARGET) return
        ui.update { it.copy(editing = null) }
        viewModelScope.launch { repo.setPrayerTarget(item.itemId, target) }
    }

    fun onReorder(itemIds: List<Long>) {
        viewModelScope.launch { repo.reorderPrayerSet(itemIds) }
    }
}
