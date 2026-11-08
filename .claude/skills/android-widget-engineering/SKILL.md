---
name: android-widget-engineering
description: Use when building or changing Dhikr home-screen widgets or the lock-screen notification — Jetpack Glance GlanceAppWidget/Receiver, appwidget-provider XML, sizes, per-instance configuration, increment actions, widget updates and refresh, dot-matrix bitmaps in widgets, the ongoing counter notification and its action receiver. Read alongside the installed glance-appwidget-updates-and-memory and glance-canonical-layouts skills.
---

# Widget and lock-screen engineering (Dhikr)

Pair this with the vendored official skills: `glance-appwidget-updates-and-memory` (update mechanics, RemoteViews memory) and `glance-canonical-layouts` (layout patterns). This file adds the **project-specific** rules.

## Verified platform facts (Phone (2a), Nothing OS 4.1, API 36 — checked 2026-11-08)

- **No third-party lock-screen widgets.** The lock-screen experience is an **ongoing notification with a "+1" action** (D-203). Still declare `android:widgetCategory="home_screen|keyguard"`: harmless, and future-proof.
- **Glyph cannot be driven from widgets or notifications** (the SDK is foreground-only, D-201).

## Architecture

```
widget/
  DhikrWidget.kt              GlanceAppWidget (compact + expanded via SizeMode.Responsive)
  DhikrWidgetReceiver.kt      GlanceAppWidgetReceiver
  IncrementAction.kt          ActionCallback → IncrementDhikrUseCase → SurfaceRefresher
  DhikrWidgetConfigActivity   picks a Dhikr per widget instance
notification/
  CounterNotification.kt      builds the ongoing notification
  CounterActionReceiver.kt    BroadcastReceiver for "+1" (exported=false)
```

- **One source of truth:** the widget reads `DhikrRepository` (Room) and writes through the same use cases as the app. No counters in Glance state.
- **Per-instance selection:** store only the selected `dhikrId` in the instance's Glance state (`PreferencesGlanceStateDefinition`, key `selected_dhikr_id`). Fall back to "last active Dhikr" from DataStore if it's unset, or if the Dhikr was disabled or deleted.
- `provideGlance`: load data **before** `provideContent` where possible, then observe with `collectAsState` inside it so updates stream. Keep work small: widget processes are memory-limited.

## Sizes and layout

- `SizeMode.Responsive(setOf(DpSize(110.dp, 110.dp) /*compact 2x2*/, DpSize(250.dp, 110.dp) /*expanded 4x2*/))`.
- Provider XML: `minWidth/minHeight`, `targetCellWidth/Height` (API 31+), `resizeMode="horizontal|vertical"`, `updatePeriodMillis="0"` (we update on events, not timers), `previewLayout` for the picker, `description`.
- The whole compact widget is the tap target. In expanded, the count block is the tap target and the header opens the app.
- **Dot-matrix in widgets = bitmaps.** Render with `DotMatrixBitmap` (shared with the app) at the exact pixel size: `LocalSize.current` × `LocalContext.current.resources.displayMetrics.density`. Cap the bitmap at about 600×300 px. Render the Arabic header to a bitmap via the same raster path. Never ship oversized bitmaps: RemoteViews has a size limit (see the vendored memory skill).
- Colours: our tokens, via `ColorProvider(day, night)`. Respect the system dark/light mode in widgets.

## Updates

- After any count change from **any** surface: `DhikrWidget().updateAll(context)` via `SurfaceRefresher`. Also refresh when a Dhikr is renamed, disabled or deleted, when targets change, and at **local midnight** (counts visibly roll to 0). Use one `WorkManager` one-time work scheduled for the next midnight that re-enqueues itself, plus `ACTION_DATE_CHANGED`/`TIMEZONE_CHANGED` handling.
- Rapid widget taps: each `ActionCallback` call is one increment, and the DB increment is atomic. Don't debounce. Update after the write completes.
- Audio from the widget: plays only if audio is enabled *and* `widgetAudioEnabled` is on (off by default). No haptics from widgets.

## Ongoing notification (lock screen)

- Channel `counter`, `IMPORTANCE_LOW` (silent, no heads-up), created at app start.
- Needs the `POST_NOTIFICATIONS` runtime permission (API 33+). Request it only when the user turns on "Show on lock screen" in Settings, never at first launch.
- `setOngoing(true)`, `setOnlyAlertOnce(true)`, `setVisibility(VISIBILITY_PUBLIC)`, `setSilent(true)`, a small monochrome icon, and the content "SubhanAllah · 64 / 100".
- Action "+1": an explicit `PendingIntent.getBroadcast(…, FLAG_IMMUTABLE)` to the non-exported `CounterActionReceiver`. On API 31+, build the action with `Notification.Action.Builder.setAuthenticationRequired(false)` so it works without unlocking.
- The receiver uses `goAsync()` → coroutine → use case → refresh notification and widgets → `finish()`, within about 10 s.
- Android 14+: users can swipe the notification away. Respect that, and don't re-post until the next in-app count or until they re-enable it.

## Common mistakes

- Keeping a count in Glance state (it drifts from the app).
- `updatePeriodMillis` polling. It wastes battery and is limited to 30 minutes anyway.
- Doing DB work on the main thread in a `BroadcastReceiver.onReceive`.
- Large or full-resolution bitmaps, which cause `TransactionTooLargeException` or a blank widget.
- An exported receiver for the notification action. That lets any app increment counts.
- Using `PendingIntent.FLAG_MUTABLE` without needing it.

## Testing (test widgets independently from screens)

- `glance-appwidget-testing` (`runGlanceAppWidgetUnitTest`): compact vs expanded content at both sizes; an unset selection falls back to the last active Dhikr; a deleted Dhikr shows the "Choose a dhikr" state.
- `IncrementAction` unit test with a fake repository: invokes the use case exactly once per callback, then calls refresh.
- Instrumented: `CounterActionReceiver` increments and updates the notification. 30 rapid broadcasts → +30.
- On device (manual, recorded in PROGRESS.md): add both sizes, add two instances with different Dhikrs, tap quickly, check the lock-screen "+1" while locked, check the midnight rollover (use `adb shell date` only on emulators; on the device, use the fake-clock debug setting).
