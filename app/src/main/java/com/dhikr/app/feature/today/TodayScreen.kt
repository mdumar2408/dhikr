package com.dhikr.app.feature.today

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhikr.app.core.model.CountContext
import com.dhikr.app.design.components.BeadProgress
import com.dhikr.app.design.components.BeadRing
import com.dhikr.app.design.components.Hairline
import com.dhikr.app.design.components.MonoLabel
import com.dhikr.app.design.dotmatrix.DotMatrixLabel
import com.dhikr.app.design.theme.DhikrTheme

@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    onOpenPrayer: (CountContext) -> Unit,
    onOpenDaily: (Long?) -> Unit,
    onOpenLibrary: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose { }
    }
    TodayContent(state, onOpenPrayer, onOpenDaily, onOpenLibrary)
}

/**
 * Today is a chooser between the two practices (DESIGN_SPEC §14.4): Prayer tasbeeh and Daily
 * targets, as two large thumb-reachable cards, followed by the daily rows.
 */
@Composable
fun TodayContent(
    state: TodayUiState,
    onOpenPrayer: (CountContext) -> Unit,
    onOpenDaily: (Long?) -> Unit,
    onOpenLibrary: () -> Unit,
) {
    val c = DhikrTheme.colors
    val sp = DhikrTheme.spacing
    LazyColumn(Modifier.fillMaxSize().background(c.canvas).padding(horizontal = sp.gutter)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = sp.l), verticalAlignment = Alignment.CenterVertically) {
                DotMatrixLabel("TODAY", Modifier.width(110.dp).height(22.dp), maxPitch = 4.dp)
                Spacer(Modifier.weight(1f))
                MonoLabel(state.dateLabel)
            }
            if (state.loaded && (state.dayComplete || state.streak > 0)) {
                Spacer(Modifier.height(sp.xs))
                MonoLabel(
                    buildString {
                        append("● ")
                        if (state.streak > 0) append("${state.streak}-day streak")
                        if (state.dayComplete) append(if (state.streak > 0) " · today complete" else "Day complete")
                    },
                    color = if (state.dayComplete) c.ember else c.inkMuted,
                )
            }
        }
        if (!state.loaded) return@LazyColumn // a few ms on cold start; avoid flashing empty states

        item { PrayerCard(state, onOpenPrayer, onOpenLibrary) }
        item { DailyCard(state, onOpenDaily, onOpenLibrary) }

        if (state.rows.isNotEmpty()) item { Spacer(Modifier.height(sp.l)) }
        items(state.rows, key = { it.id }) { row ->
            Hairline()
            Column(Modifier.fillMaxWidth().clickable { onOpenDaily(row.id) }.padding(vertical = sp.s)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(row.transliteration, color = c.ink, style = DhikrTheme.type.body)
                    if (row.isCurrent) {
                        Spacer(Modifier.width(sp.xs))
                        MonoLabel("· now")
                    }
                    Spacer(Modifier.weight(1f))
                    Text(row.arabic, color = c.inkMuted, maxLines = 1,
                        style = DhikrTheme.type.arabicSmall.copy(textDirection = TextDirection.Rtl))
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
        item { if (state.rows.isNotEmpty()) Hairline(); Spacer(Modifier.height(sp.xl)) }
    }
}

@Composable
private fun PrayerCard(state: TodayUiState, onOpenPrayer: (CountContext) -> Unit, onOpenLibrary: () -> Unit) {
    val c = DhikrTheme.colors
    val sp = DhikrTheme.spacing
    val allDone = state.prayersComplete == 5
    val next = state.suggestedPrayer
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = sp.l)
            .clip(RoundedCornerShape(sp.padRadius))
            .background(c.surface)
            .clickable(enabled = state.hasPrayerSet) { onOpenPrayer(next ?: CountContext.Fajr) }
            .padding(sp.m),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonoLabel("Prayer tasbeeh", color = c.ink)
            Spacer(Modifier.weight(1f))
            MonoLabel("${state.prayersComplete} / 5", color = if (allDone) c.ember else c.inkMuted)
        }
        if (!state.hasPrayerSet) {
            Spacer(Modifier.height(sp.s))
            Text("No after-prayer set yet", color = c.inkMuted, style = DhikrTheme.type.bodySmall)
            Spacer(Modifier.height(sp.xs))
            Text("Set up ›", color = c.ink, style = DhikrTheme.type.bodySmall,
                modifier = Modifier.clickable(onClick = onOpenLibrary).padding(vertical = 8.dp))
            return@Column
        }
        Spacer(Modifier.height(sp.m))
        // One target per prayer — tapping a prayer opens its session directly (any order).
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            state.prayers.forEach { p ->
                PrayerChip(p, isNext = p.context == next, onClick = { onOpenPrayer(p.context) })
            }
        }
        Spacer(Modifier.height(sp.m))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (allDone) "All five complete" else "Next · ${next?.label ?: "—"}",
                color = if (allDone) c.ember else c.ink, style = DhikrTheme.type.body,
            )
            Spacer(Modifier.weight(1f))
            MonoLabel(if (allDone) "" else "Start ›", color = c.ink)
        }
    }
}

@Composable
private fun PrayerChip(p: PrayerStatus, isNext: Boolean, onClick: () -> Unit) {
    val c = DhikrTheme.colors
    Column(
        Modifier
            .defaultMinSize(minWidth = 52.dp, minHeight = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .then(if (isNext) Modifier.border(1.dp, c.ink, RoundedCornerShape(14.dp)) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = "${p.context.label}: " + when {
                    p.complete -> "complete"
                    p.started -> "${p.itemsDone} of ${p.itemsTotal} done"
                    else -> "not started"
                }
            }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(Modifier.size(14.dp)) {
            val r = size.minDimension / 2
            when {
                p.complete -> drawCircle(c.ember, r)
                p.started -> {
                    drawCircle(c.ink, r - 1.dp.toPx(), style = Stroke(1.5.dp.toPx()))
                    drawCircle(c.ink, r * 0.45f)
                }
                else -> drawCircle(c.inkFaint, r - 1.dp.toPx(), style = Stroke(1.dp.toPx()))
            }
        }
        Spacer(Modifier.height(6.dp))
        MonoLabel(p.context.label, color = if (p.complete) c.ember else if (isNext) c.ink else c.inkMuted)
    }
}

@Composable
private fun DailyCard(state: TodayUiState, onOpenDaily: (Long?) -> Unit, onOpenLibrary: () -> Unit) {
    val c = DhikrTheme.colors
    val sp = DhikrTheme.spacing
    val hasTargets = state.totalTarget > 0
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = sp.s)
            .clip(RoundedCornerShape(sp.padRadius))
            .background(c.surface)
            .clickable { if (hasTargets) onOpenDaily(null) else onOpenLibrary() }
            .padding(sp.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.Center) {
            BeadRing(state.fraction, hasTargets && state.dailyComplete, Modifier.size(88.dp))
            DotMatrixLabel(
                "${(state.fraction * 100).toInt()}", Modifier.width(44.dp).height(16.dp), maxPitch = 2.5.dp,
                litColor = if (hasTargets && state.dailyComplete) c.ember else c.ink,
            )
        }
        Spacer(Modifier.width(sp.m))
        Column(Modifier.weight(1f)) {
            MonoLabel("Daily targets", color = c.ink)
            Spacer(Modifier.height(6.dp))
            if (hasTargets) {
                Text("${state.done} / ${state.totalTarget}", color = c.ink, style = DhikrTheme.type.title)
                Text(
                    if (state.dailyComplete) "Complete" else state.currentTransliteration,
                    color = if (state.dailyComplete) c.ember else c.inkMuted, style = DhikrTheme.type.bodySmall, maxLines = 1,
                )
            } else {
                Text("No targets yet · Library ›", color = c.inkMuted, style = DhikrTheme.type.bodySmall)
            }
        }
    }
}
