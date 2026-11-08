package com.dhikr.app.design.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dhikr.app.design.theme.DhikrTheme

/**
 * The app's own line icons (D-045): 24-unit grid, 1.6 stroke, round caps — drawn to match the
 * monospace / dot-matrix look. Built in code, so no icon library dependency.
 * Colour comes from the `tint` at the call site.
 */
object DhikrIcons {
    val Today by lazy { line("Today", "M12 3.5v2 M12 18.5v2 M3.5 12h2 M18.5 12h2 M6 6l1.4 1.4 M16.6 16.6L18 18 M6 18l1.4-1.4 M16.6 7.4L18 6 M8.5 12a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0 -7 0") }
    val Library by lazy { line("Library", "M6 4.5h11.5v15H7.5A1.5 1.5 0 0 1 6 18z M6 18a1.5 1.5 0 0 1 1.5-1.5h10 M9.5 8.5h5") }
    val History by lazy { line("History", "M4.5 6.5h15v13h-15z M4.5 10.5h15 M8.5 4v4 M15.5 4v4") }
    val Settings by lazy { line("Settings", "M4 7h9 M17 7h3 M4 17h3 M11 17h9 M13 7a2 2 0 1 0 4 0a2 2 0 1 0 -4 0 M7 17a2 2 0 1 0 4 0a2 2 0 1 0 -4 0") }
    val Back by lazy { line("Back", "M15 5l-7 7 7 7") }
    val ChevronLeft by lazy { line("Previous", "M14.5 6l-6 6 6 6") }
    val ChevronRight by lazy { line("Next", "M9.5 6l6 6-6 6") }
    val Undo by lazy { line("Undo", "M9 5L5 9l4 4 M5 9h9.5a4.5 4.5 0 0 1 0 9H10") }
    val Reset by lazy { line("Reset", "M5 12a7 7 0 1 0 2.05-4.95 M7 3.5v3.6h3.6") }
    val SoundOn by lazy { line("Sound on", "M4 9.5h3.5L12 5.5v13l-4.5-4H4z M15.5 9a4 4 0 0 1 0 6 M18 6.5a7.5 7.5 0 0 1 0 11") }
    val SoundOff by lazy { line("Sound off", "M4 9.5h3.5L12 5.5v13l-4.5-4H4z M16 9.5l5 5 M21 9.5l-5 5") }
    val Add by lazy { line("Add", "M12 5v14 M5 12h14") }
    val Search by lazy { line("Search", "M4.5 10.5a6 6 0 1 0 12 0a6 6 0 1 0 -12 0 M15 15l5 5") }
    val Check by lazy { line("Check", "M5 12.5l4.5 4.5L19 7.5") }
    val Drag by lazy { line("Drag", "M5 9h14 M5 15h14") }
    val Widget by lazy { line("Widget", "M4.5 4.5h6v6h-6z M13.5 4.5h6v6h-6z M4.5 13.5h6v6h-6z M13.5 13.5h6v6h-6z") }

    private fun line(name: String, pathData: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .addPath(
                pathData = addPathNodes(pathData),
                stroke = SolidColor(Color.Black), // replaced by the Icon tint
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
            .build()
}

/** A line icon in the current ink colour. Pass a description only when the icon stands alone. */
@Composable
fun DhikrIcon(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = DhikrTheme.colors.ink,
    size: Dp = 24.dp,
) {
    Icon(icon, contentDescription, modifier.size(size), tint = tint)
}

/** Round 48 dp icon-only button (Back, Undo, month arrows). */
@Composable
fun IconAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = DhikrTheme.colors.ink,
) {
    Box(
        modifier
            .size(DhikrTheme.spacing.touchTarget)
            .clip(RoundedCornerShape(50))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        DhikrIcon(icon, contentDescription, tint = if (enabled) tint else DhikrTheme.colors.hairline)
    }
}
