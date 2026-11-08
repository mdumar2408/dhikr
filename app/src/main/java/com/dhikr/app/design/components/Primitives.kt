package com.dhikr.app.design.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dhikr.app.design.theme.DhikrTheme

/** Monospace caps label (TARGET, STREAK, nav labels). */
@Composable
fun MonoLabel(text: String, modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color = DhikrTheme.colors.inkMuted) {
    Text(text.uppercase(), modifier, color = color, style = DhikrTheme.type.label, maxLines = 1)
}

/** Hairline divider — rows are separated by lines, never cards. */
@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(0.5.dp).background(DhikrTheme.colors.hairline))
}

/** Selectable pill used in Settings. 48 dp minimum touch height. */
@Composable
fun PillChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = DhikrTheme.colors
    Box(
        modifier
            .defaultMinSize(minHeight = DhikrTheme.spacing.touchTarget)
            .clip(RoundedCornerShape(50))
            .background(if (selected) c.ink else c.canvas)
            .border(1.dp, if (selected) c.ink else c.hairline, RoundedCornerShape(50))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (selected) c.canvas else c.ink, style = DhikrTheme.type.bodySmall)
    }
}

/** Round toggle dot: filled = enabled. */
@Composable
fun ToggleDot(on: Boolean, modifier: Modifier = Modifier) {
    val c = DhikrTheme.colors
    Canvas(modifier.size(14.dp)) {
        if (on) drawCircle(c.ink) else drawCircle(c.inkFaint, style = Stroke(1.5.dp.toPx()), radius = size.minDimension / 2 - 1.dp.toPx())
    }
}

enum class RoutineDotState { Done, Now, Todo }

/** One dot per Dhikr in today's routine: done ●, now ◉, todo ○. */
@Composable
fun RoutineDots(states: List<RoutineDotState>, modifier: Modifier = Modifier) {
    val c = DhikrTheme.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        states.forEach { s ->
            Canvas(Modifier.size(10.dp)) {
                val r = size.minDimension / 2
                when (s) {
                    RoutineDotState.Done -> drawCircle(c.ink, r)
                    RoutineDotState.Now -> {
                        drawCircle(c.ink, r, style = Stroke(1.5.dp.toPx()))
                        drawCircle(c.ink, r * 0.45f)
                    }
                    RoutineDotState.Todo -> drawCircle(c.inkFaint, r - 1.dp.toPx(), style = Stroke(1.dp.toPx()))
                }
            }
        }
    }
}

/** Bottom navigation: line icon over a monospace caps label; the active tab is lit (ink), the rest faint. No pill. */
@Composable
fun DotNavBar(
    items: List<Pair<String, androidx.compose.ui.graphics.vector.ImageVector>>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = DhikrTheme.colors
    Column(modifier.fillMaxWidth()) {
        Hairline()
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceAround) {
            items.forEachIndexed { i, (label, icon) ->
                val active = i == selectedIndex
                Column(
                    Modifier
                        .defaultMinSize(minWidth = 64.dp, minHeight = 56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Tab) { onSelect(i) }
                        .semantics { selected = active }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    DhikrIcon(icon, null, tint = if (active) c.ink else c.inkFaint, size = 22.dp)
                    Spacer(Modifier.height(4.dp))
                    MonoLabel(label, color = if (active) c.ink else c.inkFaint)
                }
            }
        }
    }
}
