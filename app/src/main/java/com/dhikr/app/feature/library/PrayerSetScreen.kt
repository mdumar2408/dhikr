package com.dhikr.app.feature.library

import com.dhikr.app.design.components.DhikrIcons
import com.dhikr.app.design.components.DhikrIcon
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhikr.app.core.model.PrayerSetItem
import com.dhikr.app.design.components.Hairline
import com.dhikr.app.design.components.MonoLabel
import com.dhikr.app.design.components.PillChip
import com.dhikr.app.design.theme.DhikrTheme
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun PrayerSetScreen(viewModel: PrayerSetViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val c = DhikrTheme.colors
    val sp = DhikrTheme.spacing
    val view = LocalView.current

    var rows by remember { mutableStateOf(state.items) }
    var dragging by remember { mutableStateOf(false) }
    if (!dragging && rows != state.items) rows = state.items

    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val f = rows.indexOfFirst { it.itemId == from.key }
        val t = rows.indexOfFirst { it.itemId == to.key }
        if (f >= 0 && t >= 0) {
            rows = rows.toMutableList().apply { add(t, removeAt(f)) }
            view.performHapticFeedback(if (android.os.Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.SEGMENT_TICK else HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().background(c.canvas).padding(horizontal = sp.gutter)) {
            item(key = "intro") {
                Text("Said after each prayer, in this order.", color = c.inkMuted, style = DhikrTheme.type.bodySmall)
                Spacer(Modifier.height(sp.xs))
                MonoLabel("Total per prayer · ${rows.sumOf { it.target }}", color = c.inkFaint)
                Spacer(Modifier.height(sp.s))
            }
            if (state.loaded && rows.isEmpty()) {
                item(key = "empty") {
                    Text("Nothing here yet",
                        color = c.inkMuted, style = DhikrTheme.type.body, modifier = Modifier.padding(vertical = sp.l))
                }
            }
            items(rows, key = { it.itemId }) { item ->
                ReorderableItem(reorderState, key = item.itemId) { isDragging ->
                    Column(Modifier.background(if (isDragging) c.surfaceRaised else c.canvas)) {
                        Hairline()
                        Row(Modifier.fillMaxWidth().padding(vertical = sp.s), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(sp.touchTarget)
                                    .draggableHandle(
                                        onDragStarted = { dragging = true; view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) },
                                        onDragStopped = { dragging = false; viewModel.onReorder(rows.map { it.itemId }) },
                                    )
                                    .semantics { contentDescription = "Reorder ${item.dhikr.displayName}" },
                                contentAlignment = Alignment.Center,
                            ) { DhikrIcon(DhikrIcons.Drag, null, tint = c.inkFaint) }
                            Column(Modifier.weight(1f)) {
                                Text(item.dhikr.displayName, color = c.ink, style = DhikrTheme.type.body,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(item.dhikr.arabic, color = c.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    style = DhikrTheme.type.arabicSmall.copy(textDirection = TextDirection.Rtl))
                            }
                            Box(
                                Modifier.defaultMinSize(56.dp, 48.dp).clip(RoundedCornerShape(50))
                                    .border(1.dp, c.hairline, RoundedCornerShape(50))
                                    .clickable { viewModel.onEditCount(item) }
                                    .semantics { contentDescription = "Count ${item.target}, edit" }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) { MonoLabel("×${item.target}", color = c.ink) }
                            Box(
                                Modifier.size(sp.touchTarget).clip(RoundedCornerShape(50))
                                    .clickable { viewModel.onRemove(item) }
                                    .semantics { contentDescription = "Remove ${item.dhikr.displayName} from the set" },
                                contentAlignment = Alignment.Center,
                            ) { Text("×", color = c.inkFaint, style = DhikrTheme.type.title) }
                        }
                    }
                }
            }
            item(key = "footer") { Hairline(); Spacer(Modifier.height(120.dp)) }
        }
        Box(
            Modifier.align(Alignment.BottomEnd).padding(sp.l)
                .clip(RoundedCornerShape(50)).background(c.ink)
                .clickable(onClick = viewModel::onPick)
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DhikrIcon(DhikrIcons.Add, null, tint = c.canvas, size = 18.dp)
                Spacer(Modifier.width(8.dp))
                Text("Add to set", color = c.canvas, style = DhikrTheme.type.bodySmall)
            }
        }
    }

    if (state.picking) PickSheet(state, viewModel)
    state.editing?.let { CountDialog(it, viewModel::onSaveCount, viewModel::onEditDismiss) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickSheet(state: PrayerSetUiState, viewModel: PrayerSetViewModel) {
    val c = DhikrTheme.colors
    ModalBottomSheet(
        onDismissRequest = viewModel::onPickDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surfaceRaised,
    ) {
        LazyColumn(Modifier.fillMaxWidth().padding(horizontal = DhikrTheme.spacing.gutter)) {
            item { MonoLabel("Add to after-prayer set", color = c.ink); Spacer(Modifier.height(12.dp)) }
            if (state.addable.isEmpty()) {
                item { Text("All dhikr are already in the set.", color = c.inkMuted, style = DhikrTheme.type.body) }
            }
            items(state.addable, key = { it.id }) { d ->
                Hairline()
                Column(Modifier.fillMaxWidth().clickable { viewModel.onAdd(d) }.padding(vertical = 12.dp)) {
                    Text(d.displayName, color = c.ink, style = DhikrTheme.type.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(d.arabic, color = c.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = DhikrTheme.type.arabicSmall.copy(textDirection = TextDirection.Rtl))
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun CountDialog(item: PrayerSetItem, onSave: (Int) -> Unit, onDismiss: () -> Unit) {
    val c = DhikrTheme.colors
    var text by remember(item.itemId) { mutableStateOf(item.target.toString()) }
    val value = text.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surfaceRaised,
        titleContentColor = c.ink,
        title = { Text("${item.dhikr.displayName} · count after each prayer") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 3, 10, 33, 34).forEach { n -> PillChip("$n", value == n, { text = n.toString() }) }
                }
                Spacer(Modifier.height(12.dp))
                BasicTextField(
                    value = text,
                    onValueChange = { s -> text = s.filter(Char::isDigit).take(6) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = DhikrTheme.type.title.copy(color = c.ink, textAlign = TextAlign.Center),
                    cursorBrush = SolidColor(c.ink),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .border(1.dp, c.hairline, RoundedCornerShape(14.dp)).padding(12.dp)
                        .semantics { contentDescription = "Count" },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { value?.let(onSave) }, enabled = value != null && value > 0) { Text("Save", color = c.ink) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.inkMuted) } },
    )
}
