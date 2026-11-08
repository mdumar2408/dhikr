package com.dhikr.app.core.model

/** User-facing preferences (persisted with Proto DataStore from Phase 2; in memory for Phase 1). */

enum class ThemeMode { System, Dark, Light }

/** Bead animation on the counter — DESIGN_SPEC §12. */
enum class BeadStyle { Slide, Ring, Row }

enum class SpringFeel { Soft, Crisp }

/** Haptic played on every count. Availability depends on the device motor (D-013). */
enum class TapHaptic { ClockTick, KeyTap, SegmentTick, Pulse, Clack, Off }

/** Haptic played when a target is reached. */
enum class TargetHaptic { Confirm, Thud, ThreeBeads, Off }

/** What the counter does when the current Dhikr reaches its target — D-015. */
enum class AfterTarget { NextDhikr, Stay }

/** Owner-chosen defaults (D-017): Slide, Soft, Clock tick, Confirm, move to next. */
data class CountingFeel(
    val beadStyle: BeadStyle = BeadStyle.Slide,
    val spring: SpringFeel = SpringFeel.Soft,
    val tapHaptic: TapHaptic = TapHaptic.ClockTick,
    val targetHaptic: TargetHaptic = TargetHaptic.Confirm,
    val afterTarget: AfterTarget = AfterTarget.NextDhikr,
)

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.System,
    val feel: CountingFeel = CountingFeel(),
)
