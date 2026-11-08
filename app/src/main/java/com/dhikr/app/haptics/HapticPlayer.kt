package com.dhikr.app.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import com.dhikr.app.core.model.TapHaptic
import com.dhikr.app.core.model.TargetHaptic
import com.dhikr.app.domain.flow.FlowEvent

/**
 * Plays the user's chosen haptics (D-013, D-017).
 *
 * Preference order, from the Phone (2a) measurements (no composition primitives, no amplitude
 * control): View haptic constants first (tuned by the OEM, respect the system touch-feedback
 * setting, need no permission), then predefined effects, then short on/off waveforms.
 * Called synchronously from the tap handler — never from a LaunchedEffect.
 */
class HapticPlayer(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }?.takeIf { it.hasVibrator() }

    /** Options shown in Settings: only ones this device can actually play. */
    val availableTapHaptics: List<TapHaptic> = TapHaptic.entries.filter { h ->
        when (h) {
            TapHaptic.SegmentTick -> Build.VERSION.SDK_INT >= 34
            TapHaptic.Pulse, TapHaptic.Clack -> vibrator != null
            else -> true
        }
    }
    val availableTargetHaptics: List<TargetHaptic> = TargetHaptic.entries.filter { h ->
        when (h) {
            TargetHaptic.DoubleBuzz, TargetHaptic.Thud, TargetHaptic.ThreeBeads -> vibrator != null
            else -> true
        }
    }

    /** Maps a Flow event to the right haptic: target haptic on completion, tap haptic otherwise. */
    fun onFlowEvent(view: View, event: FlowEvent, tap: TapHaptic, target: TargetHaptic) {
        when (event) {
            is FlowEvent.Counted -> play(view, tap)
            is FlowEvent.TargetReached -> play(view, target)
            is FlowEvent.RoutineComplete -> {
                play(view, target)
                view.postDelayed({ play(view, target) }, 220) // "Confirm, twice" — spec §13
            }
            FlowEvent.NothingEnabled -> Unit
        }
    }

    fun play(view: View, h: TapHaptic) {
        when (h) {
            TapHaptic.ClockTick -> view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            TapHaptic.KeyTap -> view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            TapHaptic.SegmentTick ->
                if (Build.VERSION.SDK_INT >= 34) {
                    view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
                } else {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
            TapHaptic.Pulse -> waveform(longArrayOf(0, 6))
            TapHaptic.Clack -> waveform(longArrayOf(0, 7, 38, 5))
            TapHaptic.Off -> Unit
        }
    }

    fun play(view: View, h: TargetHaptic) {
        when (h) {
            // Long enough to feel unmistakably different from the per-count tick, even mid-tapping.
            TargetHaptic.DoubleBuzz -> waveform(longArrayOf(0, 45, 90, 45))
            TargetHaptic.Confirm ->
                if (Build.VERSION.SDK_INT >= 30) {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                } else {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                }
            TargetHaptic.Thud ->
                if (Build.VERSION.SDK_INT >= 29) vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                else waveform(longArrayOf(0, 30))
            TargetHaptic.ThreeBeads -> waveform(longArrayOf(0, 10, 80, 10, 80, 14))
            TargetHaptic.Off -> Unit
        }
    }

    private fun waveform(timings: LongArray) = vibrate(VibrationEffect.createWaveform(timings, -1))

    /**
     * Target cue where there is no View to perform haptics on (lock-screen notification actions).
     * View-only constants (Confirm) fall back to the Double buzz waveform.
     */
    fun playTargetWithoutView(h: TargetHaptic) {
        when (h) {
            TargetHaptic.Off -> Unit
            TargetHaptic.Thud ->
                if (Build.VERSION.SDK_INT >= 29) vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                else waveform(longArrayOf(0, 30))
            TargetHaptic.ThreeBeads -> waveform(longArrayOf(0, 10, 80, 10, 80, 14))
            TargetHaptic.DoubleBuzz, TargetHaptic.Confirm -> waveform(longArrayOf(0, 45, 90, 45))
        }
    }

    private fun vibrate(effect: VibrationEffect) {
        val v = vibrator ?: return
        if (Build.VERSION.SDK_INT >= 33) {
            // USAGE_TOUCH: scaled/silenced by the user's system touch-feedback intensity.
            v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH))
        } else {
            v.vibrate(effect)
        }
    }
}
