package com.dhikr.app.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhikr.app.core.model.ArabicText
import com.dhikr.app.core.model.Dhikr
import com.dhikr.app.core.model.DhikrDraft
import com.dhikr.app.data.repository.DhikrRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The add/edit sheet: null id = adding. */
data class Editor(val id: Long?, val draft: DhikrDraft, val errors: Set<DhikrDraft.Error> = emptySet())

/** A just-deleted entry, kept so the snackbar can undo it. */
data class Deleted(val id: Long, val name: String, val wasEnabled: Boolean)

data class LibraryUiState(
    val loaded: Boolean = false,
    val dhikrs: List<Dhikr> = emptyList(),
    val query: String = "",
    val editor: Editor? = null,
    val lastDeleted: Deleted? = null,
) {
    /** Reordering only makes sense on the full, unfiltered list. */
    val canReorder: Boolean get() = query.isBlank()
}

class LibraryViewModel(private val repo: DhikrRepository) : ViewModel() {

    private val ui = MutableStateFlow(LibraryUiState())

    val uiState: StateFlow<LibraryUiState> = combine(repo.observeLibrary(), ui) { all, u ->
        val key = ArabicText.searchKey(u.query)
        u.copy(
            loaded = true,
            dhikrs = if (key.isEmpty()) all else all.filter { d ->
                ArabicText.searchKey("${d.arabic} ${d.transliteration} ${d.translation}").contains(key)
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun onQuery(q: String) = ui.update { it.copy(query = q) }

    fun onToggle(d: Dhikr) {
        viewModelScope.launch { repo.setEnabled(d.id, !d.isEnabled) }
    }

    fun onAdd() = ui.update { it.copy(editor = Editor(null, DhikrDraft())) }
    fun onEdit(d: Dhikr) = ui.update { it.copy(editor = Editor(d.id, DhikrDraft.from(d))) }
    fun onEditorChange(draft: DhikrDraft) = ui.update { s -> s.copy(editor = s.editor?.copy(draft = draft, errors = emptySet())) }
    fun onEditorDismiss() = ui.update { it.copy(editor = null) }

    fun onEditorSave() {
        val editor = ui.value.editor ?: return
        val errors = editor.draft.validate()
        if (errors.isNotEmpty()) {
            ui.update { it.copy(editor = editor.copy(errors = errors)) }
            return
        }
        ui.update { it.copy(editor = null) }
        viewModelScope.launch {
            if (editor.id == null) repo.add(editor.draft) else repo.update(editor.id, editor.draft)
        }
    }

    /** Soft delete (history is kept) with an undo window. */
    fun onDelete(id: Long, name: String) {
        ui.update { it.copy(editor = null) }
        viewModelScope.launch {
            val wasEnabled = repo.delete(id)
            ui.update { it.copy(lastDeleted = Deleted(id, name, wasEnabled)) }
        }
    }

    fun onUndoDelete() {
        val d = ui.value.lastDeleted ?: return
        ui.update { it.copy(lastDeleted = null) }
        viewModelScope.launch { repo.restore(d.id, d.wasEnabled) }
    }

    fun onDeletedShown() = ui.update { it.copy(lastDeleted = null) }

    /** Called once when a drag ends, with the full list in its new order. */
    fun onReorder(idsInOrder: List<Long>) {
        viewModelScope.launch { repo.reorder(idsInOrder) }
    }
}
