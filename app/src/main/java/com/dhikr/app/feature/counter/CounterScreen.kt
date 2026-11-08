package com.dhikr.app.feature.counter

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onLongClick
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhikr.app.core.model.AfterTarget
import com.dhikr.app.core.model.CountContext
import com.dhikr.app.core.model.BeadStyle
import com.dhikr.app.core.model.SpringFeel
import com.dhikr.app.design.components.BeadPad
import com.dhikr.app.design.components.BeadString
import com.dhikr.app.design.components.MonoLabel
import com.dhikr.app.design.components.RoutineDots
import com.dhikr.app.design.dotmatrix.DotGrid
import com.dhikr.app.design.dotmatrix.DotMatrixText
import com.dhikr.app.design.theme.DhikrTheme
import com.dhikr.app.haptics.HapticPlayer
import com.dhikr.app.audio.AudioCuePlayer
import androidx.lifecycle.compose.LifecycleStartEffect

@Composable
fun CounterScreen(viewModel: CounterViewModel, haptics: HapticPlayer, audio: AudioCuePlayer, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val view = LocalView.current
    // Leaving the counter (or the app going to background) stops any recitation.
    LifecycleStartEffect(Unit) { onStopOrDispose { audio.stop() } }
    CounterContent(
        state = state,
        onTap = {
            val event = viewModel.onTap()
            // Feedback order (tactile skill): state first, then haptic, then sound — same frame.
            haptics.onFlowEvent(view, event, state.feel.tapHaptic, state.feel.targetHaptic)
            audio.onFlowEvent(event, state.sound)
        },
        onUndo = viewModel::onUndo,
        onReset = viewModel::onReset,
        onSwipe = viewModel::onSwipe,
        onToggleRecite = viewModel::onToggleRecite,
        onBack = onBack,
    )
}

@Composable
fun CounterContent(
    state: CounterUiState,
    onTap: () -> Unit,
    onUndo: () -> Unit,
    onReset: () -> Unit,
    onSwipe: (Int) -> Unit,
    onToggleRecite: () -> Unit,
    onBack: () -> Unit,
) {
    val c = DhikrTheme.colors
    val sp = DhikrTheme.spacing
    val dhikr = state.dhikr

    // One spring-animated value drives the beads (D-012). When the Dhikr changes, snap — never
    // animate 33 → 0 backwards through the string.
    val progress = remember { Animatable(state.displayCount.toFloat()) }
    val lastId = remember { longArrayOf(dhikr?.id ?: -1L) }
    LaunchedEffect(dhikr?.id, state.displayCount, state.feel.spring) {
        val id = dhikr?.id ?: -1L
        if (id != lastId[0]) {
            lastId[0] = id
            progress.snapTo(state.displayCount.toFloat())
        } else {
            progress.animateTo(
                state.displayCount.toFloat(),
                when (state.feel.spring) {
                    SpringFeel.Soft -> spring(dampingRatio = 0.62f, stiffness = 420f)
                    SpringFeel.Crisp -> spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)
                },
            )
        }
    }

    Column(Modifier.fillMaxSize().background(c.canvas).padding(horizontal = sp.gutter)) {
        Box {
            // ---- Read-only top area (swipe here to switch Dhikr manually) ----
            Column(
                Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        var dx = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { dx = 0f },
                            onDragEnd = { if (dx < -120f) onSwipe(1) else if (dx > 120f) onSwipe(-1) },
                        ) { _, amount -> dx += amount }
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(Modifier.fillMaxWidth().height(sp.touchTarget), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(sp.touchTarget).clip(RoundedCornerShape(50)).clickable(onClick = onBack),
                        contentAlignment = Alignment.Center,
                    ) { Text("‹", color = c.ink, style = DhikrTheme.type.title) }
                    MonoLabel(
                        when {
                            state.context.isPrayer -> "${state.context.label} · ${state.position} / ${state.routineSize}"
                            state.feel.afterTarget == AfterTarget.NextDhikr -> "Flow · ${state.position} / ${state.routineSize}"
                            else -> "Single · ${state.position} / ${state.routineSize}"
                        },
                        color = if (state.context.isPrayer) c.ink else c.inkMuted,
                    )
                    Spacer(Modifier.weight(1f))
                    if (state.round > 1) MonoLabel("Round ${state.round}", color = c.ink)
                }
                if (dhikr == null) {
                    Spacer(Modifier.height(sp.xl))
                    Text(
                        if (state.context.isPrayer) "Your after-prayer set is empty. Add dhikr to it in Library." else "Enable a dhikr in your library to start.",
                        color = c.inkMuted, style = DhikrTheme.type.body, textAlign = TextAlign.Center,
                    )
                } else {
                    Spacer(Modifier.height(sp.m))
                    // Long adhkar (e.g. the tahlil) use the smaller Arabic size so the pad keeps its space.
                    val long = dhikr.arabic.length > 40
                    Text(
                        dhikr.arabic, color = c.ink, maxLines = 3, overflow = TextOverflow.Ellipsis,
                        style = (if (long) DhikrTheme.type.arabicSmall else DhikrTheme.type.arabic)
                            .copy(textDirection = TextDirection.Rtl, textAlign = TextAlign.Center),
                    )
                    Text(dhikr.transliteration, color = c.inkMuted, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        style = if (long) DhikrTheme.type.bodySmall else DhikrTheme.type.body, textAlign = TextAlign.Center)
                    Text(dhikr.translation, color = c.inkFaint, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        style = DhikrTheme.type.bodySmall, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(sp.m))
                    RoutineDots(state.routineDots)
                }
                Spacer(Modifier.height(sp.s))
                BeadString(
                    style = state.feel.beadStyle,
                    progress = progress.value,
                    target = state.target,
                    complete = state.complete,
                    modifier = Modifier.fillMaxWidth().height(if (state.feel.beadStyle == BeadStyle.Ring) 200.dp else 56.dp),
                )
            }

            // ---- NEXT card: slides over the top area for ~2 s after a target ----
            NextCardOverlay(state.announcement, state.context, Modifier.align(Alignment.TopCenter).padding(top = sp.touchTarget))
        }

        // ---- Bead pad: the one primary action ----
        BeadPad(
            onTap = onTap,
            accessibilityLabel = dhikr?.let { "Count ${it.transliteration}, ${state.displayCount} of ${state.target}" } ?: "Counter",
            modifier = Modifier.fillMaxWidth().weight(1f).padding(top = sp.m, bottom = sp.m),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                DotMatrixText(
                    grid = remember(state.displayCount) { DotGrid.text(state.displayCount.toString()) },
                    contentDescription = state.displayCount.toString(),
                    litColor = if (state.complete) c.ember else c.ink,
                    maxPitch = 15.dp,
                    modifier = Modifier.width(280.dp).height(110.dp),
                )
                Spacer(Modifier.height(sp.s))
                MonoLabel(
                    when {
                        state.sessionComplete -> "${state.context.label} complete · extra"
                        state.complete -> "Complete · / ${state.target}"
                        else -> "/ ${state.target}"
                    },
                    color = if (state.complete) c.ember else c.inkMuted,
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(bottom = sp.s),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.defaultMinSize(56.dp, 48.dp).clip(RoundedCornerShape(50)).clickable(onClick = onUndo).padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) { MonoLabel("Undo") }
            if (dhikr?.audioRef != null) {
                // Quick toggle for "recite while counting" — only when this dhikr has a recording.
                Box(
                    Modifier.defaultMinSize(56.dp, 48.dp).clip(RoundedCornerShape(50)).clickable(onClick = onToggleRecite)
                        .semantics { contentDescription = "Recite while counting: ${if (state.sound.reciteWhileCounting) "on" else "off"}" }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) { MonoLabel(if (state.sound.reciteWhileCounting) "♪ On" else "♪ Off", color = if (state.sound.reciteWhileCounting) c.ink else c.inkFaint) }
            }
            HoldToReset(onReset)
        }
    }
}

/**
 * Hold for 1.2 s to reset the current dhikr — no dialog, but impossible to trigger by accident
 * (spec §6.1). Releasing early cancels. A ring fills while holding.
 */
@Composable
private fun HoldToReset(onReset: () -> Unit) {
    val c = DhikrTheme.colors
    val view = LocalView.current
    val currentOnReset by rememberUpdatedState(onReset)
    val fill = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Row(
        Modifier
            .defaultMinSize(56.dp, 48.dp)
            .clip(RoundedCornerShape(50))
            .semantics {
                contentDescription = "Reset current dhikr. Touch and hold."
                onLongClick(label = "Reset") { currentOnReset(); true }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    val job = scope.launch {
                        fill.snapTo(0f)
                        fill.animateTo(1f, tween(HOLD_MS, easing = LinearEasing))
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        currentOnReset()
                    }
                    waitForUpOrCancellation()
                    job.cancel()
                    scope.launch { fill.animateTo(0f, tween(150)) }
                }
            }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(16.dp)) {
            val stroke = 2.dp.toPx()
            drawCircle(c.hairline, style = Stroke(stroke))
            drawArc(c.ink, -90f, 360f * fill.value, false, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Spacer(Modifier.width(8.dp))
        MonoLabel("Hold to reset")
    }
}

private const val HOLD_MS = 1_200

/** Separate function so the un-scoped AnimatedVisibility is used (not ColumnScope's). */
@Composable
private fun NextCardOverlay(announcement: Announcement?, context: CountContext, modifier: Modifier) {
    // Keep the last value while the exit animation runs.
    val last = remember { arrayOfNulls<Announcement>(1) }
    if (announcement != null) last[0] = announcement
    AnimatedVisibility(
        visible = announcement != null,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier,
    ) {
        last[0]?.let { NextCard(it, context) }
    }
}

@Composable
private fun NextCard(a: Announcement, context: CountContext) {
    val c = DhikrTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(c.surfaceRaised)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MonoLabel("✓ ${a.completed.transliteration} · ${a.completed.target}", color = c.ember)
        Spacer(Modifier.height(10.dp))
        if (a.routineComplete && context.isPrayer) {
            MonoLabel("${context.label} tasbeeh complete", color = c.ink)
            Spacer(Modifier.height(6.dp))
            Text("May Allah accept it. Extra taps are still counted.", color = c.inkMuted, style = DhikrTheme.type.bodySmall)
        } else if (a.routineComplete) {
            MonoLabel(if (a.round == 1) "Routine complete" else "Round ${a.round} complete", color = c.ink)
            Spacer(Modifier.height(6.dp))
            Text("Keep tapping to begin round ${a.round + 1}", color = c.inkMuted, style = DhikrTheme.type.bodySmall)
        } else if (a.next != null) {
            MonoLabel("Next", color = c.inkMuted)
            Text(
                a.next.arabic, color = c.ink,
                style = DhikrTheme.type.arabic.copy(textDirection = TextDirection.Rtl, textAlign = TextAlign.Center),
            )
            Text("${a.next.transliteration} · 0 / ${a.next.target}", color = c.inkMuted, style = DhikrTheme.type.body)
        }
    }
}
