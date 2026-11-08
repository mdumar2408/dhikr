package com.dhikr.app.feature.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.dhikr.app.core.model.DhikrDraft
import com.dhikr.app.design.components.MonoLabel
import com.dhikr.app.design.components.PillChip
import com.dhikr.app.design.theme.DhikrTheme

/**
 * Add / edit form (spec §6.3): only "Arabic or transliteration" is required; target defaults to 33.
 * Validation lives in DhikrDraft; this composable only renders state and forwards events.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DhikrEditorSheet(
    editor: Editor,
    onChange: (DhikrDraft) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onDelete: (id: Long, name: String) -> Unit,
    onAudioPicked: (Uri) -> Unit = {},
    onAudioPlay: (String) -> Unit = {},
    onAudioRemove: () -> Unit = {},
) {
    val c = DhikrTheme.colors
    val sp = DhikrTheme.spacing
    val d = editor.draft
    var confirmDelete by remember { mutableStateOf(false) }
    // System file picker (Storage Access Framework): no storage permission needed.
    val pickAudio = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onAudioPicked)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surfaceRaised,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = sp.gutter),
        ) {
            MonoLabel(if (editor.id == null) "Add dhikr" else "Edit dhikr", color = c.ink)
            Spacer(Modifier.height(sp.m))

            Field("Arabic", d.arabic, { onChange(d.copy(arabic = it)) }, rtl = true, style = DhikrTheme.type.arabicSmall)
            Field("Transliteration", d.transliteration, { onChange(d.copy(transliteration = it)) })
            if (DhikrDraft.Error.NeedsText in editor.errors) {
                Text("Enter the Arabic or the transliteration.", color = c.ember, style = DhikrTheme.type.bodySmall)
                Spacer(Modifier.height(sp.xs))
            }
            Field("Meaning (optional)", d.translation, { onChange(d.copy(translation = it)) }, singleLine = false)

            MonoLabel("Daily target")
            Row(Modifier.padding(vertical = sp.xs), horizontalArrangement = Arrangement.spacedBy(sp.xs)) {
                listOf(33, 99, 100).forEach { t -> PillChip("$t", d.target == t, { onChange(d.copy(target = t)) }) }
                NumberField(d.target, { onChange(d.copy(target = it)) }, Modifier.width(96.dp))
            }
            if (DhikrDraft.Error.TargetOutOfRange in editor.errors) {
                Text("Target must be between 1 and ${DhikrDraft.MAX_TARGET}.", color = c.ember, style = DhikrTheme.type.bodySmall)
            }
            Spacer(Modifier.height(sp.s))
            Field("Reference (optional)", d.reference, { onChange(d.copy(reference = it)) }, singleLine = false)

            MonoLabel("Recording (optional)")
            if (editor.id == null) {
                Text("Save first, then open it again to add a recording.", color = c.inkFaint,
                    style = DhikrTheme.type.bodySmall, modifier = Modifier.padding(vertical = sp.xs))
            } else {
                Row(Modifier.padding(vertical = sp.xs), horizontalArrangement = Arrangement.spacedBy(sp.xs)) {
                    val ref = editor.audioRef
                    if (ref == null) {
                        PillChip(if (editor.audioBusy) "Adding…" else "Add recording", false, { if (!editor.audioBusy) pickAudio.launch(arrayOf("audio/*")) })
                    } else {
                        PillChip("▶ Play", false, { onAudioPlay(ref) })
                        PillChip("Replace", false, { pickAudio.launch(arrayOf("audio/*")) })
                        PillChip("Remove", false, onAudioRemove)
                    }
                }
                editor.audioMessage?.let { Text(it, color = c.inkMuted, style = DhikrTheme.type.bodySmall) }
                Text(
                    "Use only recordings you made or have permission to use.",
                    color = c.inkFaint, style = DhikrTheme.type.bodySmall, modifier = Modifier.padding(top = 2.dp),
                )
            }

            Spacer(Modifier.height(sp.m))
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                if (editor.id != null) {
                    TextButton(onClick = { confirmDelete = true }) { Text("Delete", color = c.inkMuted) }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Cancel", color = c.inkMuted) }
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(c.ink).clickable(onClick = onSave)
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                ) { Text("Save", color = c.canvas, style = DhikrTheme.type.bodySmall) }
            }
            Spacer(Modifier.height(sp.xl))
        }
    }

    if (confirmDelete && editor.id != null) {
        val name = d.transliteration.ifBlank { d.arabic }
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Remove $name?") },
            text = { Text("It leaves your library and routine. Past counts are kept, and you can undo right after.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete(editor.id, name) }) { Text("Remove", color = c.ember) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep", color = c.ink) } },
            containerColor = c.surfaceRaised,
            titleContentColor = c.ink,
            textContentColor = c.inkMuted,
        )
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    rtl: Boolean = false,
    singleLine: Boolean = true,
    style: TextStyle = DhikrTheme.type.body,
) {
    val c = DhikrTheme.colors
    MonoLabel(label)
    BasicTextField(
        value = value,
        onValueChange = onValue,
        singleLine = singleLine,
        textStyle = style.copy(
            color = c.ink,
            textDirection = if (rtl) TextDirection.Rtl else TextDirection.Content,
            textAlign = if (rtl) TextAlign.End else TextAlign.Start,
        ),
        cursorBrush = SolidColor(c.ink),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 14.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, c.hairline, RoundedCornerShape(14.dp))
            .background(c.canvas)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .semantics { contentDescription = label },
    )
}

@Composable
private fun NumberField(value: Int, onValue: (Int) -> Unit, modifier: Modifier) {
    val c = DhikrTheme.colors
    var text by remember(value) { mutableStateOf(value.toString()) }
    BasicTextField(
        value = text,
        onValueChange = { s ->
            val digits = s.filter(Char::isDigit).take(6)
            text = digits
            digits.toIntOrNull()?.let(onValue) ?: onValue(0)
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = DhikrTheme.type.body.copy(color = c.ink, textAlign = TextAlign.Center),
        cursorBrush = SolidColor(c.ink),
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, c.hairline, RoundedCornerShape(50))
            .padding(vertical = 12.dp)
            .semantics { contentDescription = "Custom target" },
    )
}
