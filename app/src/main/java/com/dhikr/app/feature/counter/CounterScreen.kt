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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhikr.app.core.model.AfterTarget
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

@Composable
fun CounterScreen(viewModel: CounterViewModel, haptics: HapticPlayer, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val view = LocalView.current
    CounterContent(
        state = state,
        onTap = {
            val event = viewModel.onTap()
            haptics.onFlowEvent(view, event, state.feel.tapHaptic, state.feel.targetHaptic)
        },
        onUndo = viewModel::onUndo,
        onSwipe = viewModel::onSwipe,
        onBack = onBack,
    )
}

@Composable
fun CounterContent(
    state: CounterUiState,
    onTap: () -> Unit,
    onUndo: () -> Unit,
    onSwipe: (Int) -> Unit,
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
                        if (state.feel.afterTarget == AfterTarget.NextDhikr) "Flow · ${state.position} / ${state.routineSize}"
                        else "Single · ${state.position} / ${state.routineSize}",
                    )
                    Spacer(Modifier.weight(1f))
                    if (state.round > 1) MonoLabel("Round ${state.round}", color = c.ink)
                }
                if (dhikr == null) {
                    Spacer(Modifier.height(sp.xl))
                    Text("Enable a dhikr in your library to start.", color = c.inkMuted, style = DhikrTheme.type.body)
                } else {
                    Spacer(Modifier.height(sp.m))
                    Text(
                        dhikr.arabic, color = c.ink,
                        style = DhikrTheme.type.arabic.copy(textDirection = TextDirection.Rtl, textAlign = TextAlign.Center),
                    )
                    Text(dhikr.transliteration, color = c.inkMuted, style = DhikrTheme.type.body)
                    Text(dhikr.translation, color = c.inkFaint, style = DhikrTheme.type.bodySmall, textAlign = TextAlign.Center)
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
            NextCardOverlay(state.announcement, Modifier.align(Alignment.TopCenter).padding(top = sp.touchTarget))
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
                    if (state.complete) "Complete · / ${state.target}" else "/ ${state.target}",
                    color = if (state.complete) c.ember else c.inkMuted,
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = sp.s), horizontalArrangement = Arrangement.SpaceBetween) {
            Box(
                Modifier.defaultMinSize(56.dp, 48.dp).clip(RoundedCornerShape(50)).clickable(onClick = onUndo).padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) { MonoLabel("Undo") }
        }
    }
}

/** Separate function so the un-scoped AnimatedVisibility is used (not ColumnScope's). */
@Composable
private fun NextCardOverlay(announcement: Announcement?, modifier: Modifier) {
    // Keep the last value while the exit animation runs.
    val last = remember { arrayOfNulls<Announcement>(1) }
    if (announcement != null) last[0] = announcement
    AnimatedVisibility(
        visible = announcement != null,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier,
    ) {
        last[0]?.let { NextCard(it) }
    }
}

@Composable
private fun NextCard(a: Announcement) {
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
        if (a.routineComplete) {
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
