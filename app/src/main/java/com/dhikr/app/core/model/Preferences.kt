package com.dhikr.app.core.model

import kotlinx.serialization.Serializable

/**
 * User-facing preferences, persisted as JSON in a typed DataStore (D-019).
 * Enums are stored by NAME: never rename an enum constant without a migration;
 * unknown values fall back to defaults (see UserPreferencesSerializer).
 */

@Serializable enum class ThemeMode { System, Dark, Light }

/** Bead animation on the counter — DESIGN_SPEC §12. */
@Serializable enum class BeadStyle { Slide, Ring, Row }

@Serializable enum class SpringFeel { Soft, Crisp }

/** Haptic played on every count. Availability depends on the device motor (D-013). */
@Serializable enum class TapHaptic { ClockTick, KeyTap, SegmentTick, Pulse, Clack, Off }

/** Haptic played when a target is reached. */
@Serializable enum class TargetHaptic { DoubleBuzz, Confirm, Thud, ThreeBeads, Off }

/** What the counter does when the current Dhikr reaches its target — D-015. */
@Serializable enum class AfterTarget { NextDhikr, Stay }

/**
 * Owner-chosen defaults (D-017): Slide, Soft, Clock tick, move to next. The target cue default
 * changed from Confirm to DoubleBuzz after on-device testing showed the switch was easy to miss (D-024).
 */
@Serializable
data class CountingFeel(
    val beadStyle: BeadStyle = BeadStyle.Slide,
    val spring: SpringFeel = SpringFeel.Soft,
    val tapHaptic: TapHaptic = TapHaptic.ClockTick,
    val targetHaptic: TargetHaptic = TargetHaptic.DoubleBuzz,
    val afterTarget: AfterTarget = AfterTarget.NextDhikr,
)

/** What you hear when Flow moves to the next dhikr (DESIGN_SPEC §14.5, D-030). */
@Serializable enum class SwitchSound { Off, Chime, ReciteNext }

/**
 * Sound settings (Phase 4). Sounds are always optional: the counter is fully usable silent.
 * The chime follows the ringer (silent/vibrate → no chime); recitations play as media.
 */
@Serializable
data class SoundSettings(
    val switchSound: SwitchSound = SwitchSound.Chime,
    /** Play the current dhikr's own recording while counting; never overlaps itself. */
    val reciteWhileCounting: Boolean = false,
)

@Serializable
data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.System,
    val feel: CountingFeel = CountingFeel(),
    val sound: SoundSettings = SoundSettings(),
)
