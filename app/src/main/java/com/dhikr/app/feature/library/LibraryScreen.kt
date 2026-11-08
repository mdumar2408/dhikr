package com.dhikr.app.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhikr.app.design.components.Hairline
import com.dhikr.app.design.components.MonoLabel
import com.dhikr.app.design.components.ToggleDot
import com.dhikr.app.design.dotmatrix.DotMatrixLabel
import com.dhikr.app.design.theme.DhikrTheme

@Composable
fun LibraryScreen(viewModel: LibraryViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val c = DhikrTheme.colors
    val sp = DhikrTheme.spacing
    LazyColumn(Modifier.fillMaxSize().background(c.canvas).padding(horizontal = sp.gutter)) {
        item {
            DotMatrixLabel("LIBRARY", Modifier.padding(top = sp.l).width(150.dp).height(22.dp), maxPitch = 4.dp)
            Spacer(Modifier.height(sp.s))
            MonoLabel("Tap to include in your daily routine", color = c.inkFaint)
            Spacer(Modifier.height(sp.m))
        }
        items(state.dhikrs, key = { it.id }) { d ->
            Hairline()
            Row(
                Modifier
                    .fillMaxWidth()
                    .toggleable(d.isEnabled, role = Role.Switch) { viewModel.onToggle(d.id) }
                    .padding(vertical = sp.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(d.transliteration, color = if (d.isEnabled) c.ink else c.inkMuted, style = DhikrTheme.type.body)
                    MonoLabel("Target ${d.target}", color = c.inkFaint)
                }
                Text(
                    d.arabic, color = if (d.isEnabled) c.ink else c.inkFaint,
                    style = DhikrTheme.type.arabicSmall.copy(textDirection = TextDirection.Rtl),
                )
                Spacer(Modifier.width(sp.m))
                ToggleDot(d.isEnabled)
            }
        }
        item {
            Hairline()
            Spacer(Modifier.height(sp.l))
            MonoLabel("Add · edit · reorder come with the saved library", color = c.inkFaint)
            Spacer(Modifier.height(sp.xl))
        }
    }
}
