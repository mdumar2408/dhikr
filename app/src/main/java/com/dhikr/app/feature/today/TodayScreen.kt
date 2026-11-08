package com.dhikr.app.feature.today

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhikr.app.design.components.BeadProgress
import com.dhikr.app.design.components.BeadRing
import com.dhikr.app.design.components.Hairline
import com.dhikr.app.design.components.MonoLabel
import com.dhikr.app.design.dotmatrix.DotMatrixLabel
import com.dhikr.app.design.dotmatrix.DotMatrixText
import com.dhikr.app.design.dotmatrix.rememberRasterGrid
import com.dhikr.app.design.theme.DhikrTheme

@Composable
fun TodayScreen(viewModel: TodayViewModel, onOpenCounter: (Long?) -> Unit, onOpenLibrary: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TodayContent(state, onOpenCounter, onOpenLibrary)
}

@Composable
fun TodayContent(state: TodayUiState, onOpenCounter: (Long?) -> Unit, onOpenLibrary: () -> Unit) {
    val c = DhikrTheme.colors
    val sp = DhikrTheme.spacing
    LazyColumn(Modifier.fillMaxSize().background(c.canvas).padding(horizontal = sp.gutter)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = sp.l), verticalAlignment = Alignment.CenterVertically) {
                DotMatrixLabel("TODAY", Modifier.width(110.dp).height(22.dp), maxPitch = 4.dp)
                Spacer(Modifier.weight(1f))
                MonoLabel(state.dateLabel)
            }
        }
        if (!state.loaded) return@LazyColumn // a few ms on cold start; avoid flashing the empty state
        if (state.rows.isEmpty()) {
            item { EmptyToday(onOpenLibrary) }
            return@LazyColumn
        }
        item {
            Box(Modifier.fillMaxWidth().padding(top = sp.l), contentAlignment = Alignment.Center) {
                BeadRing(state.fraction, state.dayComplete, Modifier.size(180.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    DotMatrixLabel(
                        state.done.toString(), Modifier.width(100.dp).height(36.dp), maxPitch = 5.dp,
                        litColor = if (state.dayComplete) c.ember else c.ink,
                    )
                    Spacer(Modifier.height(6.dp))
                    MonoLabel("/ ${state.totalTarget}")
                }
            }
        }
        item {
            // "Now" — the current Dhikr's Arabic, shaped by Android and then sampled into dots.
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = sp.l)
                    .clip(RoundedCornerShape(sp.padRadius))
                    .background(c.surface)
                    .clickable { onOpenCounter(null) }
                    .padding(sp.m),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MonoLabel(if (state.dayComplete) "Day complete · keep going" else "Continue")
                Spacer(Modifier.height(sp.s))
                DotMatrixText(
                    grid = rememberRasterGrid(state.currentArabic, rows = 20),
                    contentDescription = state.currentArabic,
                    maxPitch = 4.dp,
                    drawOff = false,
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                )
                Spacer(Modifier.height(sp.xs))
                Text(state.currentTransliteration, color = c.inkMuted, style = DhikrTheme.type.bodySmall)
            }
            Spacer(Modifier.height(sp.m))
        }
        items(state.rows, key = { it.id }) { row ->
            Hairline()
            Column(
                Modifier.fillMaxWidth().clickable { onOpenCounter(row.id) }.padding(vertical = sp.s),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(row.transliteration, color = c.ink, style = DhikrTheme.type.body)
                    if (row.isCurrent) {
                        Spacer(Modifier.width(sp.xs))
                        MonoLabel("· now")
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        row.arabic, color = c.inkMuted,
                        style = DhikrTheme.type.arabicSmall.copy(textDirection = TextDirection.Rtl),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(sp.s)) {
                    BeadProgress(row.count / row.target.toFloat(), row.complete, Modifier.width(150.dp).height(8.dp))
                    MonoLabel(
                        if (row.complete) "${row.count} / ${row.target} ✓" else "${row.count} / ${row.target}",
                        color = if (row.complete) c.ember else c.inkMuted,
                    )
                }
            }
        }
        item { Hairline(); Spacer(Modifier.height(sp.xl)) }
    }
}

@Composable
private fun EmptyToday(onOpenLibrary: () -> Unit) {
    val c = DhikrTheme.colors
    Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        BeadRing(0f, false, Modifier.size(160.dp))
        Spacer(Modifier.height(24.dp))
        Text("Choose your daily dhikr", color = c.ink, style = DhikrTheme.type.title)
        Spacer(Modifier.height(8.dp))
        Text("Enable the ones you want to practise each day.", color = c.inkMuted, style = DhikrTheme.type.bodySmall)
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier.clip(RoundedCornerShape(50)).background(c.ink).clickable(onClick = onOpenLibrary)
                .padding(horizontal = 24.dp, vertical = 14.dp),
        ) { Text("Open library", color = c.canvas, style = DhikrTheme.type.bodySmall) }
    }
}
