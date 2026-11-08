package com.dhikr.app.feature.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhikr.app.core.model.CountContext
import com.dhikr.app.design.components.Hairline
import com.dhikr.app.design.components.MonoLabel
import com.dhikr.app.design.dotmatrix.DotMatrixLabel
import com.dhikr.app.design.theme.DhikrTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Dot calendar + streaks (spec §6.4, D-023/D-032). */
@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val c = DhikrTheme.colors
    val sp = DhikrTheme.spacing

    Column(
        Modifier.fillMaxSize().background(c.canvas).verticalScroll(rememberScrollState()).padding(horizontal = sp.gutter),
    ) {
        DotMatrixLabel("HISTORY", Modifier.padding(top = sp.l).width(150.dp).height(22.dp), maxPitch = 4.dp)
        Spacer(Modifier.height(sp.l))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat("Current", state.streaks.current, highlight = state.mark(state.today) == DayMark.Complete)
            Stat("Longest", state.streaks.longest)
            Stat("Days", state.streaks.totalCompletedDays)
        }
        Spacer(Modifier.height(sp.s))
        Text(
            "A day counts when all five prayer tasbeeh and your daily targets are complete.",
            color = c.inkFaint, style = DhikrTheme.type.bodySmall,
        )

        Spacer(Modifier.height(sp.l))
        Row(verticalAlignment = Alignment.CenterVertically) {
            NavArrow("‹", "Previous month", enabled = true, onClick = viewModel::onPrevMonth)
            Spacer(Modifier.weight(1f))
            MonoLabel("${state.month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${state.month.year}", color = c.ink)
            Spacer(Modifier.weight(1f))
            NavArrow("›", "Next month", enabled = state.canGoNext, onClick = viewModel::onNextMonth)
        }
        Spacer(Modifier.height(sp.s))
        Calendar(state, viewModel::onSelectDay)

        Spacer(Modifier.height(sp.m))
        Row(horizontalArrangement = Arrangement.spacedBy(sp.m), verticalAlignment = Alignment.CenterVertically) {
            Legend(DayMark.Complete, "Complete")
            Legend(DayMark.Partial, "Partial")
            Legend(DayMark.Empty, "No activity")
        }
        Spacer(Modifier.height(sp.xxl))
    }

    state.detail?.let { DayDetailSheet(it, viewModel::onDismissDetail) }
}

@Composable
private fun Stat(label: String, value: Int, highlight: Boolean = false) {
    val c = DhikrTheme.colors
    Column(
        Modifier.semantics(mergeDescendants = true) { contentDescription = "$label: $value" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DotMatrixLabel(value.toString(), Modifier.width(90.dp).height(34.dp), maxPitch = 5.dp,
            litColor = if (highlight && value > 0) c.ember else c.ink)
        Spacer(Modifier.height(6.dp))
        MonoLabel(label)
    }
}

@Composable
private fun NavArrow(glyph: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    val c = DhikrTheme.colors
    Box(
        Modifier.size(48.dp).clip(RoundedCornerShape(50)).clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { Text(glyph, color = if (enabled) c.ink else c.hairline, style = DhikrTheme.type.title) }
}

@Composable
private fun Calendar(state: HistoryUiState, onSelect: (LocalDate) -> Unit) {
    val c = DhikrTheme.colors
    val first = state.month.atDay(1)
    val leading = first.dayOfWeek.value - 1 // Monday-first
    val days = state.month.lengthOfMonth()
    Row(Modifier.fillMaxWidth()) {
        listOf("M", "T", "W", "T", "F", "S", "S").forEach {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { MonoLabel(it, color = c.inkFaint) }
        }
    }
    Spacer(Modifier.height(6.dp))
    val cells = leading + days
    for (week in 0 until (cells + 6) / 7) {
        Row(Modifier.fillMaxWidth()) {
            for (col in 0 until 7) {
                val dayNum = week * 7 + col - leading + 1
                Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                    if (dayNum in 1..days) {
                        val date = state.month.atDay(dayNum)
                        DayCell(date, state.mark(date), isToday = date == state.today, onSelect)
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, mark: DayMark, isToday: Boolean, onSelect: (LocalDate) -> Unit) {
    val c = DhikrTheme.colors
    val label = date.format(DateTimeFormatter.ofPattern("d MMMM", Locale.ENGLISH)) + ": " + when (mark) {
        DayMark.Complete -> "complete"
        DayMark.Partial -> "partly done"
        DayMark.Empty -> "no activity"
        DayMark.Future -> "upcoming"
    }
    Column(
        Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
            .clickable(enabled = mark != DayMark.Future) { onSelect(date) }
            .semantics(mergeDescendants = true) { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        MarkDot(mark, isToday, Modifier.size(16.dp))
        Spacer(Modifier.height(2.dp))
        Text("${date.dayOfMonth}", color = if (mark == DayMark.Future) c.hairline else c.inkFaint, style = DhikrTheme.type.label)
    }
}

@Composable
private fun MarkDot(mark: DayMark, isToday: Boolean, modifier: Modifier) {
    val c = DhikrTheme.colors
    Canvas(modifier) {
        val r = size.minDimension / 2
        if (isToday) drawCircle(c.ember, r, style = Stroke(1.5.dp.toPx()))
        val inner = r * 0.62f
        when (mark) {
            DayMark.Complete -> drawCircle(c.ink, inner)
            DayMark.Partial -> drawCircle(c.ink, inner - 1.dp.toPx(), style = Stroke(1.5.dp.toPx()))
            DayMark.Empty -> drawCircle(c.dotOff, inner * 0.6f)
            DayMark.Future -> drawCircle(c.dotOff, inner * 0.35f)
        }
    }
}

@Composable
private fun Legend(mark: DayMark, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MarkDot(mark, isToday = false, Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        MonoLabel(text, color = DhikrTheme.colors.inkFaint)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDetailSheet(detail: DayDetail, onDismiss: () -> Unit) {
    val c = DhikrTheme.colors
    val sp = DhikrTheme.spacing
    val s = detail.summary
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surfaceRaised,
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = sp.gutter)) {
            MonoLabel(detail.date.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ENGLISH)), color = c.ink)
            Spacer(Modifier.height(sp.xs))
            Text(
                when {
                    s == null || !s.hasActivity -> "Nothing was counted this day."
                    s.isCompleted -> "Complete — all prayers and daily targets."
                    else -> "Prayers ${s.prayersCompleted} / 5 · daily ${s.totalCompleted} / ${s.totalTarget}"
                },
                color = if (s?.isCompleted == true) c.ember else c.inkMuted, style = DhikrTheme.type.body,
            )
            Spacer(Modifier.height(sp.m))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                CountContext.prayers.forEach { p ->
                    val done = s?.prayerComplete(p) == true
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Canvas(Modifier.size(12.dp)) {
                            if (done) drawCircle(c.ember) else drawCircle(c.inkFaint, size.minDimension / 2 - 1.dp.toPx(), style = Stroke(1.dp.toPx()))
                        }
                        Spacer(Modifier.height(4.dp))
                        MonoLabel(p.label, color = if (done) c.ember else c.inkMuted)
                    }
                }
            }
            detail.items.groupBy { it.context }.toSortedMap(compareBy { it.ordinal }).forEach { (ctx, items) ->
                Spacer(Modifier.height(sp.m))
                MonoLabel(if (ctx.isPrayer) "After ${ctx.label}" else "Daily targets", color = c.ink)
                items.forEach { item ->
                    Hairline()
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(item.transliteration.ifBlank { item.arabic }, color = c.ink, style = DhikrTheme.type.bodySmall,
                            maxLines = 1, modifier = Modifier.weight(1f))
                        MonoLabel("${item.count} / ${item.target}", color = if (item.complete) c.ember else c.inkMuted)
                    }
                }
            }
            if (detail.items.isNotEmpty()) {
                Spacer(Modifier.height(sp.xs))
                Text("Targets shown as they were on that day.", color = c.inkFaint, style = DhikrTheme.type.bodySmall)
            }
            Spacer(Modifier.height(sp.xl))
        }
    }
}
