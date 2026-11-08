package com.dhikr.app.feature.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.dhikr.app.design.components.MonoLabel
import com.dhikr.app.design.dotmatrix.DotMatrixLabel
import com.dhikr.app.design.theme.DhikrTheme
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Dot calendar shell (spec §6.4). Completion history needs persisted daily summaries (Phase 5),
 * so for now every day is drawn unfilled and today is marked — no invented data.
 */
@Composable
fun HistoryScreen() {
    val c = DhikrTheme.colors
    val sp = DhikrTheme.spacing
    val today = remember { LocalDate.now() }
    val first = today.withDayOfMonth(1)
    val leading = first.dayOfWeek.value - 1 // Monday-first
    val days = today.lengthOfMonth()
    Column(Modifier.fillMaxSize().background(c.canvas).padding(horizontal = sp.gutter)) {
        DotMatrixLabel("HISTORY", Modifier.padding(top = sp.l).width(150.dp).height(22.dp), maxPitch = 4.dp)
        Spacer(Modifier.height(sp.l))
        MonoLabel("${today.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${today.year}")
        Spacer(Modifier.height(sp.m))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { MonoLabel(it, color = c.inkFaint) }
        }
        Spacer(Modifier.height(sp.s))
        Canvas(Modifier.fillMaxWidth().aspectRatio(7f / 6f)) {
            val cell = size.width / 7
            for (d in 1..days) {
                val i = leading + d - 1
                val center = Offset(cell * (i % 7 + 0.5f), cell * (i / 7 + 0.5f))
                val r = cell * 0.16f
                if (d == today.dayOfMonth) {
                    drawCircle(c.ember, r * 1.5f, center, style = Stroke(1.5.dp.toPx()))
                }
                drawCircle(c.inkFaint, r, center, style = Stroke(1.dp.toPx()))
            }
        }
        Spacer(Modifier.height(sp.l))
        Text(
            "Completed days and streaks will fill in here once your counts are saved.",
            color = c.inkMuted, style = DhikrTheme.type.bodySmall,
        )
    }
}
