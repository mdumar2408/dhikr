package com.dhikr.app.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.view.HapticFeedbackConstants
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhikr.app.core.model.Dhikr
import com.dhikr.app.design.components.Hairline
import com.dhikr.app.design.components.MonoLabel
import com.dhikr.app.design.components.ToggleDot
import com.dhikr.app.design.dotmatrix.DotMatrixLabel
import com.dhikr.app.design.theme.DhikrTheme
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun LibraryScreen(viewModel: LibraryViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.lastDeleted) {
        val d = state.lastDeleted ?: return@LaunchedEffect
        val result = snackbar.showSnackbar("${d.name} removed", actionLabel = "Undo", duration = SnackbarDuration.Long)
        if (result == SnackbarResult.ActionPerformed) viewModel.onUndoDelete() else viewModel.onDeletedShown()
    }

    Box(Modifier.fillMaxSize()) {
        LibraryList(
            state = state,
            onQuery = viewModel::onQuery,
            onToggle = viewModel::onToggle,
            onEdit = viewModel::onEdit,
            onReorder = viewModel::onReorder,
        )
        AddPill(viewModel::onAdd, Modifier.align(Alignment.BottomEnd).padding(DhikrTheme.spacing.l))
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 88.dp))
    }

    state.editor?.let { editor ->
        DhikrEditorSheet(
            editor = editor,
            onChange = viewModel::onEditorChange,
            onSave = viewModel::onEditorSave,
            onDismiss = viewModel::onEditorDismiss,
            onDelete = { id, name -> viewModel.onDelete(id, name) },
        )
    }
}

@Composable
private fun LibraryList(
    state: LibraryUiState,
    onQuery: (String) -> Unit,
    onToggle: (Dhikr) -> Unit,
    onEdit: (Dhikr) -> Unit,
    onReorder: (List<Long>) -> Unit,
) {
    val c = DhikrTheme.colors
    val sp = DhikrTheme.spacing
    val view = LocalView.current

    // Local copy so rows move instantly while dragging; persisted once when the drag ends.
    var rows by remember { mutableStateOf(state.dhikrs) }
    var dragging by remember { mutableStateOf(false) }
    if (!dragging && rows != state.dhikrs) rows = state.dhikrs

    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIndex = rows.indexOfFirst { it.id == from.key }
        val toIndex = rows.indexOfFirst { it.id == to.key }
        if (fromIndex >= 0 && toIndex >= 0) {
            rows = rows.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
            view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(c.canvas).padding(horizontal = sp.gutter),
    ) {
        item(key = "header") {
            SearchField(state.query, onQuery)
            Spacer(Modifier.height(sp.s))
            MonoLabel(
                if (state.canReorder) "● in routine · drag ≡ to order" else "${rows.size} found",
                color = c.inkFaint,
            )
            Spacer(Modifier.height(sp.s))
        }
        if (state.loaded && rows.isEmpty()) {
            item(key = "empty") {
                Text(
                    if (state.query.isBlank()) "Your library is empty. Add your first dhikr." else "No dhikr matches “${state.query}”.",
                    color = c.inkMuted, style = DhikrTheme.type.body, modifier = Modifier.padding(vertical = sp.l),
                )
            }
        }
        items(rows, key = { it.id }) { d ->
            ReorderableItem(reorderState, key = d.id, enabled = state.canReorder) { isDragging ->
                Column(Modifier.background(if (isDragging) c.surfaceRaised else c.canvas)) {
                    Hairline()
                    Row(
                        Modifier.fillMaxWidth().clickable { onEdit(d) }.padding(vertical = sp.s),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (state.canReorder) {
                            Box(
                                Modifier
                                    .size(sp.touchTarget)
                                    .draggableHandle(
                                        onDragStarted = {
                                            dragging = true
                                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                        },
                                        onDragStopped = {
                                            dragging = false
                                            onReorder(rows.map { it.id })
                                        },
                                    )
                                    .semantics { contentDescription = "Reorder ${d.displayName}" },
                                contentAlignment = Alignment.Center,
                            ) { Text("≡", color = c.inkFaint, style = DhikrTheme.type.title) }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                d.displayName, color = if (d.isEnabled) c.ink else c.inkMuted,
                                style = DhikrTheme.type.body, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                d.arabic, color = if (d.isEnabled) c.inkMuted else c.inkFaint, maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = DhikrTheme.type.arabicSmall.copy(textDirection = TextDirection.Rtl),
                            )
                            MonoLabel("Target ${d.target}", color = c.inkFaint)
                        }
                        Box(
                            Modifier
                                .size(sp.touchTarget)
                                .clip(RoundedCornerShape(50))
                                .clickable(role = Role.Switch) { onToggle(d) }
                                .semantics {
                                    contentDescription = "Include ${d.displayName} in daily routine"
                                    stateDescription = if (d.isEnabled) "On" else "Off"
                                },
                            contentAlignment = Alignment.Center,
                        ) { ToggleDot(d.isEnabled) }
                    }
                }
            }
        }
        item(key = "footer") {
            Hairline()
            Spacer(Modifier.height(120.dp)) // room for the Add pill
        }
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit) {
    val c = DhikrTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, c.hairline, RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (query.isEmpty()) Text("Search  ·  سبحان  ·  SubhanAllah", color = c.inkFaint, style = DhikrTheme.type.body)
        BasicTextField(
            value = query, onValueChange = onQuery, singleLine = true,
            textStyle = DhikrTheme.type.body.copy(color = c.ink),
            cursorBrush = SolidColor(c.ink),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Search library" },
        )
    }
}

@Composable
private fun AddPill(onAdd: () -> Unit, modifier: Modifier) {
    val c = DhikrTheme.colors
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(c.ink)
            .clickable(onClick = onAdd)
            .padding(horizontal = 22.dp, vertical = 14.dp),
    ) { Text("+ Add dhikr", color = c.canvas, style = DhikrTheme.type.bodySmall) }
}
