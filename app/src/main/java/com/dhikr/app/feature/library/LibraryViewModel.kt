package com.dhikr.app.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhikr.app.core.model.Dhikr
import com.dhikr.app.data.CounterStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class LibraryUiState(val dhikrs: List<Dhikr> = emptyList())

class LibraryViewModel(private val store: CounterStore) : ViewModel() {
    val uiState: StateFlow<LibraryUiState> = store.state
        .map { s -> LibraryUiState(s.library.sortedBy { it.sortOrder }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState(store.state.value.library.sortedBy { it.sortOrder }))

    fun onToggle(id: Long) {
        val d = store.state.value.library.firstOrNull { it.id == id } ?: return
        store.setEnabled(id, !d.isEnabled)
    }
}
