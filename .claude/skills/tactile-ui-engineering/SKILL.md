---
name: tactile-ui-engineering
description: Use when implementing or reviewing touch and feedback in Dhikr — the bead pad tap handling, haptics (HapticFeedbackConstants / Vibrator), press states, counter micro-animations, completion feedback, hold-to-reset, undo, audio feedback timing, and rapid-tap reliability.
---

# Tactile UI engineering

Goal: counting feels like a physical tasbeeh. **Every tap registers immediately, exactly once, with crisp but restrained feedback.**

## The tap pipeline (order matters)

```
pointer down/up on BeadPad
  → ViewModel.onIncrement()            // synchronous state update first
      → _uiState.update { count + 1 }  // optimistic, instant UI
      → haptics.tick()                 // same frame
      → audio.playIfEnabled()          // non-blocking, never awaited
      → persistence via counter engine (serialised, transactional — see dhikr-domain-engineering)
```

- The UI never waits on the database to show the new count. The count engine owns ordering and durability (Phase 3).
- **Rapid taps:** use `Modifier.pointerInput` with `detectTapGestures(onTap = …)`, or `clickable` with `indication = null`. Do NOT debounce or throttle. Each tap is a separate `onIncrement()` call. Test with 20 taps per second.
- Double-tap detection must be **off** on the pad: `detectTapGestures` with an `onDoubleTap` handler delays `onTap`. Never pass `onDoubleTap`.
- Multi-touch: two fingers landing together should count as two taps only if they are two distinct down/up pairs. Default handling is fine; don't add custom multi-pointer logic without a test.

## Haptics

**Device fact (Phone (2a), measured 2026-11-08):** `arePrimitivesSupported` is false for *every* `VibrationEffect.Composition` primitive, and `hasAmplitudeControl()` is false. On this device, primitive compositions silently fail or need fallbacks. Always gate primitives on `arePrimitivesSupported` and keep View-constant, predefined or on/off-waveform fallbacks (D-013). The chosen presets are recorded in `docs/DESIGN_SPEC.md` §12.

- Prefer view-based haptics: `LocalView.current.performHapticFeedback(constant)`. It respects the user's system "touch feedback" setting and needs no `VIBRATE` permission.
  - Tap: `HapticFeedbackConstants.CLOCK_TICK`.
  - Target reached: `CONFIRM` on API 30 and up. Below that, `LONG_PRESS`.
  - Hold-to-reset complete: `LONG_PRESS`. Reject/limit: `REJECT` (API 30+).
- Wrap these in a small `Haptics` interface (`tick()`, `success()`, `heavy()`) so the app setting "haptics off · subtle · strong" is applied in one place and is fakeable in tests.
- Only use `Vibrator`/`VibrationEffect` if "strong" needs more than the constants give. That requires `<uses-permission android:name="android.permission.VIBRATE"/>`, and you must check `vibrator.hasVibrator()`.
- Never vibrate from a widget or notification action on every tap. That's surprising outside the app.

## Press state and micro-animations

- Pad press: background `surface` → `surfaceRaised`, content `scale 0.97`, both via `animateFloatAsState(tween(90))`. Release returns over 120 ms. Collect the press from `MutableInteractionSource.collectIsPressedAsState()`.
- Count change: dot cross-fade (dots turning on over 80 ms, off over 120 ms). Animate per-dot alpha from the old and new `DotGrid`. **Don't use `AnimatedContent` slides** for each digit: that's too busy at 10 taps a second.
- Completion: one `success()` haptic, beads and count turn ember for 1.5 s, then settle back to ink with the completed state kept. Trigger only on the *transition* `count == target` (`count - 1 < target <= count`), not on every tap after.
- Hold-to-reset: a 1.2 s press with a progress ring drawn on `Canvas`. Release early = cancel. On completion, `heavy()` haptic and reset. Implement as `awaitPointerEventScope` + `withTimeoutOrNull`, not a `LaunchedEffect` loop with `delay(16)`.
- Undo: a single tap, no confirmation, removes the most recent increment. Debounce is not needed. Show a brief "−1" dot flash on the count.
- Follow `compose-animations` for API choice and `jetpack-compose-performance` to keep animations off the composition phase (use `graphicsLayer`/draw lambdas).

## Accessibility

- The pad is `Role.Button` with label "Count {name}, {count} of {target}". TalkBack double-tap = one increment.
- Announce progress politely every 10 counts and at completion, not every tap.
- Keep feedback non-visual-only: never rely on colour alone. The completion state also shows the "COMPLETE" label.

## Common mistakes

- Persisting before updating UI state, so taps lag under disk I/O.
- `LaunchedEffect(count) { haptic() }`: misses repeated identical values and double-fires on recomposition. Fire haptics from the event handler.
- Animating with `delay()` loops in coroutines instead of Animatable or transition APIs.
- Haptic on every recomposition from a `SideEffect`.
- Blocking the main thread with `MediaPlayer.prepare()`. Prepare audio ahead of time (see the audio notes in CLAUDE.md).

## Testing

- Unit test (ViewModel with a fake clock, haptics and repo): 100 rapid `onIncrement()` calls → count 100, `tick()` ×100, `success()` exactly once when the target is 33.
- Compose UI test: `performClick()` ×50 on the pad → semantics state reads 50. Undo → 49.
- Hold-to-reset UI test: `performTouchInput { down(center); advanceEventTime(1300); up() }` → reset. With 800 ms → no reset.
- On device: manually verify the haptic strength on the Phone (2a), and check that 10+ taps per second feel 1:1.
