package com.dhikr.app.design.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dhikr.app.core.model.BeadStyle
import com.dhikr.app.design.theme.DhikrColors
import com.dhikr.app.design.theme.DhikrTheme
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

private const val SET = 33

private fun smooth(t: Float): Float { val x = t.coerceIn(0f, 1f); return x * x * (3 - 2 * x) }

/**
 * The tasbeeh beads (DESIGN_SPEC §12). Every style is drawn from one animated value
 * [progress] = the count, spring-animated by the caller. Bead k is waiting while
 * progress ≤ k, in flight between k and k+1, and counted once progress ≥ k+1.
 * Because one value drives all beads, rapid taps stream instead of queueing.
 */
@Composable
fun BeadString(
    style: BeadStyle,
    progress: Float,
    target: Int,
    complete: Boolean,
    modifier: Modifier = Modifier,
) {
    val c = DhikrTheme.colors
    Canvas(modifier.semantics { contentDescription = "${progress.toInt()} of $target" }) {
        when (style) {
            BeadStyle.Slide -> slide(progress, target, complete, c)
            BeadStyle.Ring -> ring(progress, target, complete, c)
            BeadStyle.Row -> row(progress, target, complete, c)
        }
    }
}

/** A string of beads: uncounted wait on the right, each count slides one across the gap. */
private fun DrawScope.slide(p: Float, target: Int, complete: Boolean, c: DhikrColors) {
    val r = minOf(11.dp.toPx(), size.height * 0.36f)
    val pitch = r * 2.15f
    val cy = size.height / 2
    val gap = 46.dp.toPx()
    val gapLeft = size.width / 2 - gap
    val gapRight = size.width / 2 + gap
    drawLine(c.hairline, Offset(0f, cy), Offset(size.width, cy), 1.5.dp.toPx(), StrokeCap.Round)
    val base = floor(p).toInt()
    for (k in (base - 24)..(base + 24)) {
        if (k < 0) continue
        val t = p - k
        val x = when {
            t <= 0f -> gapRight - t * pitch
            t >= 1f -> gapLeft - (t - 1f) * pitch
            else -> gapRight + (gapLeft - gapRight) * smooth(t)
        }
        if (x < -2 * r || x > size.width + 2 * r) continue
        val counted = t >= 1f
        val colour = when {
            complete && counted -> c.ember
            counted -> c.ink
            t > 0f -> lerp(c.inkFaint, c.ink, smooth(t))
            else -> c.inkFaint
        }
        if (target >= 3 && (k + 1) % target == 0) {
            // Last bead of a set: the elongated "imam" bead.
            drawRoundRect(
                colour, Offset(x - r * 0.75f, cy - r * 1.35f),
                Size(r * 1.5f, r * 2.7f), CornerRadius(r * 0.75f),
            )
        } else {
            drawCircle(colour, r, Offset(x, cy))
            if (t <= 0f) drawCircle(c.canvas, r * 0.42f, Offset(x, cy)) // uncounted beads are hollow
        }
    }
}

/** A loop of 33 that rotates one bead per count past a "now" mark at 12 o'clock. */
private fun DrawScope.ring(p: Float, target: Int, complete: Boolean, c: DhikrColors) {
    val step = 2 * PI / SET
    val r = minOf(6.5.dp.toPx(), size.minDimension * 0.04f)
    val radius = size.minDimension / 2 - r * 2.2f
    val centre = Offset(size.width / 2, size.height / 2)
    drawCircle(c.hairline, radius, centre, style = Stroke(1.5.dp.toPx()))
    val counted = floor(p + 1e-3f).toInt()
    val setStart = Math.floorDiv(counted - 1, SET) * SET
    for (k in (ceil(p).toInt() - SET + 1)..ceil(p).toInt()) {
        val a = -PI / 2 + (k - p + 0.5) * step
        val pos = Offset(centre.x + radius * cos(a).toFloat(), centre.y + radius * sin(a).toFloat())
        val isCounted = k >= 0 && k in setStart until counted
        drawCircle(
            when {
                isCounted && complete -> c.ember
                isCounted -> c.ink
                else -> c.dotOff
            },
            r, pos,
        )
    }
    drawCircle(c.inkMuted, 2.dp.toPx(), Offset(centre.x, centre.y - radius - r * 2f))
}

/** The 33-dot progress row; each newly lit dot pops in (1.5× → 1×). */
private fun DrawScope.row(p: Float, target: Int, complete: Boolean, c: DhikrColors) {
    val pitch = size.width / SET
    val r = minOf(pitch * 0.32f, size.height * 0.3f)
    val cy = size.height / 2
    val perBead = target / SET.toFloat()
    for (i in 0 until SET) {
        val t = (p / perBead - i).coerceIn(0f, 1f)
        val x = pitch * (i + 0.5f)
        drawCircle(c.dotOff, r, Offset(x, cy))
        if (t > 0f) {
            val pop = 1f + 0.5f * (1f - smooth(t))
            drawCircle((if (complete) c.ember else c.ink).copy(alpha = smooth(t)), r * pop, Offset(x, cy))
        }
    }
}

/** Static progress row for lists (Today rows, widgets later): [beads] dots, lit by fraction. */
@Composable
fun BeadProgress(fraction: Float, complete: Boolean, modifier: Modifier = Modifier, beads: Int = 14) {
    val c = DhikrTheme.colors
    Canvas(modifier) {
        val pitch = size.width / beads
        val r = minOf(pitch * 0.3f, size.height / 2)
        val lit = floor(fraction.coerceIn(0f, 1f) * beads).toInt()
        for (i in 0 until beads) {
            drawCircle(
                when {
                    i < lit && complete -> c.ember
                    i < lit -> c.ink
                    else -> c.dotOff
                },
                r, Offset(pitch * (i + 0.5f), size.height / 2),
            )
        }
    }
}

/** Overall-progress ring of 33 dots (Today). */
@Composable
fun BeadRing(fraction: Float, complete: Boolean, modifier: Modifier = Modifier) {
    val c = DhikrTheme.colors
    Canvas(modifier) {
        val radius = size.minDimension / 2 - 6.dp.toPx()
        val r = 3.5.dp.toPx()
        val lit = floor(fraction.coerceIn(0f, 1f) * SET).toInt()
        for (i in 0 until SET) {
            val a = -PI / 2 + i * 2 * PI / SET
            drawCircle(
                when {
                    i < lit && complete -> c.ember
                    i < lit -> c.ink
                    else -> c.dotOff
                },
                r,
                Offset(size.width / 2 + radius * cos(a).toFloat(), size.height / 2 + radius * sin(a).toFloat()),
            )
        }
    }
}
