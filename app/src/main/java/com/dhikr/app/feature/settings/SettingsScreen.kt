package com.dhikr.app.feature.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhikr.app.core.model.AfterTarget
import com.dhikr.app.core.model.BeadStyle
import com.dhikr.app.core.model.SpringFeel
import com.dhikr.app.core.model.TapHaptic
import com.dhikr.app.core.model.TargetHaptic
import com.dhikr.app.core.model.ThemeMode
import com.dhikr.app.design.components.BeadPad
import com.dhikr.app.design.components.BeadString
import com.dhikr.app.design.components.Hairline
import com.dhikr.app.design.components.MonoLabel
import com.dhikr.app.design.components.PillChip
import com.dhikr.app.design.dotmatrix.DotMatrixLabel
import com.dhikr.app.design.theme.DhikrTheme
import com.dhikr.app.haptics.HapticPlayer
import com.dhikr.app.audio.AudioCuePlayer
import com.dhikr.app.audio.Cue
import com.dhikr.app.core.model.SwitchSound
import androidx.compose.material3.Text

private const val PREVIEW_TARGET = 11

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, haptics: HapticPlayer, audio: AudioCuePlayer) {
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val feel = prefs.feel
    val view = LocalView.current
    val c = DhikrTheme.colors
    val sp = DhikrTheme.spacing

    Column(
        Modifier.fillMaxSize().background(c.canvas).verticalScroll(rememberScrollState()).padding(horizontal = sp.gutter),
    ) {
        DotMatrixLabel("SETTINGS", Modifier.padding(top = sp.l).width(170.dp).height(22.dp), maxPitch = 4.dp)

        Section("Theme")
        Options(ThemeMode.entries, prefs.themeMode, { it.name }, viewModel::setTheme)

        Section("Counting feel")
        PreviewPad(feel.beadStyle, feel.spring) { reachedTarget ->
            if (reachedTarget) haptics.play(view, feel.targetHaptic) else haptics.play(view, feel.tapHaptic)
        }
        Label("Bead animation")
        Options(BeadStyle.entries, feel.beadStyle, { it.name }, viewModel::setBeadStyle)
        Label("Spring")
        Options(SpringFeel.entries, feel.spring, { it.name }, viewModel::setSpring)
        Label("Haptic on each count")
        Options(viewModel.tapOptions, feel.tapHaptic, ::tapLabel) {
            viewModel.setTapHaptic(it)
            haptics.play(view, it)
        }
        Label("Haptic when a target is reached")
        Options(viewModel.targetOptions, feel.targetHaptic, ::targetLabel) {
            viewModel.setTargetHaptic(it)
            haptics.play(view, it)
        }

        Section("After a target")
        Options(
            AfterTarget.entries, feel.afterTarget,
            { if (it == AfterTarget.NextDhikr) "Move to next dhikr" else "Stay on this dhikr" },
            viewModel::setAfterTarget,
        )

        Section("Sound")
        Label("When moving to the next dhikr")
        Options(
            SwitchSound.entries, prefs.sound.switchSound,
            {
                when (it) {
                    SwitchSound.Off -> "Off"
                    SwitchSound.Chime -> "Chime"
                    SwitchSound.ReciteNext -> "Recite next"
                }
            },
        ) {
            viewModel.setSwitchSound(it)
            if (it != SwitchSound.Off) audio.play(Cue.Chime()) // preview
        }
        Text(
            "The chime is silent when your phone is on silent or vibrate. \"Recite next\" plays the next dhikr's own recording, or the chime if it has none.",
            color = c.inkFaint, style = DhikrTheme.type.bodySmall, modifier = Modifier.padding(top = sp.xs),
        )
        Label("Recite while counting")
        Options(listOf(false, true), prefs.sound.reciteWhileCounting, { if (it) "On" else "Off" }, viewModel::setReciteWhileCounting)
        Text(
            "Plays a dhikr's recording as you count, never overlapping itself. Add recordings when editing a dhikr in Library.",
            color = c.inkFaint, style = DhikrTheme.type.bodySmall, modifier = Modifier.padding(top = sp.xs),
        )
        Spacer(Modifier.height(sp.xxl))
    }
}

private fun tapLabel(h: TapHaptic) = when (h) {
    TapHaptic.ClockTick -> "Clock tick"
    TapHaptic.KeyTap -> "Key tap"
    TapHaptic.SegmentTick -> "Segment tick"
    TapHaptic.Pulse -> "Pulse"
    TapHaptic.Clack -> "Clack"
    TapHaptic.Off -> "Off"
}

private fun targetLabel(h: TargetHaptic) = when (h) {
    TargetHaptic.DoubleBuzz -> "Double buzz"
    TargetHaptic.Confirm -> "Confirm"
    TargetHaptic.Thud -> "Thud"
    TargetHaptic.ThreeBeads -> "Three beads"
    TargetHaptic.Off -> "Off"
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(DhikrTheme.spacing.l))
    Hairline()
    Spacer(Modifier.height(DhikrTheme.spacing.m))
    MonoLabel(title, color = DhikrTheme.colors.ink)
}

@Composable
private fun Label(text: String) {
    Spacer(Modifier.height(DhikrTheme.spacing.m))
    MonoLabel(text)
}

@Composable
private fun <T> Options(items: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = DhikrTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(DhikrTheme.spacing.xs),
    ) {
        items.forEach { PillChip(label(it), it == selected, { onSelect(it) }) }
    }
}

/** Live preview: feel the current choices. Every 11th tap plays the target haptic. */
@Composable
private fun PreviewPad(style: BeadStyle, springFeel: SpringFeel, onHaptic: (reachedTarget: Boolean) -> Unit) {
    var count by remember { mutableIntStateOf(0) }
    val p = remember { Animatable(0f) }
    LaunchedEffect(count, springFeel) {
        p.animateTo(
            count.toFloat(),
            if (springFeel == SpringFeel.Soft) spring(0.62f, 420f) else spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow),
        )
    }
    BeadPad(
        onTap = {
            count += 1
            onHaptic(count % PREVIEW_TARGET == 0)
        },
        accessibilityLabel = "Preview counter, $count",
        modifier = Modifier.fillMaxWidth().height(if (style == BeadStyle.Ring) 220.dp else 120.dp).padding(top = DhikrTheme.spacing.s),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BeadString(
                style, p.value, PREVIEW_TARGET, complete = count > 0 && count % PREVIEW_TARGET == 0,
                modifier = Modifier.fillMaxWidth().height(if (style == BeadStyle.Ring) 170.dp else 56.dp),
            )
            MonoLabel("Tap to preview · target every $PREVIEW_TARGET", color = DhikrTheme.colors.inkFaint)
        }
    }
}
