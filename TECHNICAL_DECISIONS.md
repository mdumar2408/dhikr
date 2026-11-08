# Technical Decisions

Architectural decisions for Dhikr. Update this file whenever a decision is made or changed,
so later sessions don't drift. Format: decision, why, status. Newest entries go at the bottom of each section.

## Platform and toolchain

| # | Decision | Why | Status |
|---|---|---|---|
| D-001 | Single native Android app, Kotlin, Jetpack Compose, one `:app` module | Spec §11: avoid premature modularisation | Accepted (2026-11-08) |
| D-002 | Package / applicationId `com.dhikr.app` | Spec §11. The applicationId is permanent once on Play: check it is free before the first Play upload | Accepted, recheck before Play |
| D-003 | `minSdk 26` (Android 8.0) | Nothing phones are 14+, but 26 costs nothing and keeps future non-Nothing users possible. 26 also gives adaptive icons and `java.time` without desugaring | Accepted |
| D-004 | `targetSdk 36` (Android 16) | Nothing OS 4.1 is Android 16. Raise to 37 once it is tested on an Android 17 device | Accepted |
| D-005 | `compileSdk 37` | Newest level supported by AGP 9.5; current AndroidX releases expect it. Compiling against a newer level does not change runtime behaviour | Accepted |
| D-006 | AGP 9.5.1, Gradle 9.6.0, Kotlin 2.4.21, JDK 17 bytecode | Latest stable on 2026-11-08. AGP 9 has built-in Kotlin, so the `kotlin-android` plugin is not applied; only the Compose compiler plugin is | Accepted |
| D-007 | Gradle Version Catalog (`gradle/libs.versions.toml`) + Compose BOM | Spec §12. One place for versions; the BOM keeps Compose libraries mutually compatible | Accepted |
| D-008 | No DI framework (Hilt) yet; manual construction via an `AppContainer` | Spec §12/§14: add Hilt only if wiring gets complex. Revisit in Phase 6 (widgets need the same repository) | Proposed |
| D-010 | **Navigation 3** (1.2.1) with `@Serializable` `NavKey` routes (kotlinx-serialization 1.12.0 plus the Kotlin serialization plugin) and `rememberNavBackStack`. Tabs are top-level: the stack is `[Today]` or `[Today, Tab]`, with `CounterRoute` pushed on top (bottom bar hidden). ViewModels are scoped per entry via `rememberViewModelStoreNavEntryDecorator` | The app-owned back stack is a plain list, which makes tab and Flow logic simple. It's Google's current Compose navigation | Accepted (2026-11-08) |
| D-018 | Phase 1 uses in-memory `CounterStore`/`PreferencesStore` (process lifetime) behind the same shape the Room and DataStore implementations will have. All mutations go through the pure `FlowPolicy`, and increments are atomic via `MutableStateFlow.compareAndSet` | Lets the UI and Flow rules be built and tested before persistence. Counts and settings reset on app restart until Phase 2 | Accepted, temporary (remove in Phase 2) |
| D-011 | Claude Code skills are vendored as project skills in `.claude/skills/` (16 third-party + 5 project-specific), not installed as global plugins | Versioned with the repo, scoped to this project, reviewed before install. Plugins would pull in about 80 irrelevant skills. Hilt- and multi-module-mandating skills were excluded | Accepted (2026-11-08) |
| D-012 | Bead and counter animation uses **native Compose** (`Animatable` spring + `Canvas`), with no Lottie or Rive. One spring-animated count value drives all beads | Zero size cost, physics-driven, rapid-tap safe, shares drawing with widget bitmaps. Rive adds ~2.4 MB download (13 MB AAR). Lottie needs authored clips and can't express per-bead state cleanly | Accepted (2026-11-08) |
| D-013 | Haptics via a `Haptics` interface: View constants first; primitives only when `arePrimitivesSupported` says yes; predefined or waveform fallbacks | **Measured on the Phone (2a): zero composition primitives supported, no amplitude control.** View constants are tuned by Nothing and respect system settings | Accepted. Per-count and completion presets to be picked by feel in Bead Lab |
| D-014 | Light and dark themes, following the system by default, with an override in Settings | Owner request. Both token sets verified on the device | Accepted (2026-11-08) |
| D-015 | **Flow** is the default counting mode: auto-advance through enabled Dhikrs in library order when a target is reached, skip completed ones, loop into rounds after all are complete. The "next Dhikr" decision lives in the counter engine (a serialised use case), not the UI | Owner request: "user should focus on tapping". Engine-side placement keeps boundary taps correct and lets widgets and notifications share it | Accepted (2026-11-08) |
| D-016 | `DhikrEntity` gets `sortOrder: Int`, and the Library supports drag-to-reorder. Flow order = enabled Dhikrs by `sortOrder` | The order of the routine must be explicit and user-owned | Accepted. Lands in Phase 2 schema v1, so no migration is needed |
| D-017 | Counting feel is user-configurable (bead style, spring, per-count haptic, target haptic, after-target behaviour) and stored in Proto DataStore. Defaults: Slide, Soft, Clock tick, Confirm, Move to next | Owner's picks, tested in Bead Lab on the device | Accepted (2026-11-08) |
| D-019 | Preferences use a **typed DataStore with a kotlinx-serialization JSON serializer** (`datastore` 1.3.0) instead of Proto DataStore | Same guarantees (typed, atomic, Flow), with no protoc/protobuf Gradle plugin, and the serialization plugin is already in use. `ignoreUnknownKeys` and `coerceInputValues` give forward compatibility. Corruption → defaults | Accepted (2026-11-08) |
| D-020 | **Room 3** (3.0.3, `androidx.room3`, KSP 2.3.13) with `AndroidSQLiteDriver` (platform SQLite, no bundled native lib). Schema exported to `app/schemas/` and committed | Room 3 is the current major version (stable July 2026): coroutines-first, KSP-only. Starting a new project on 2.x would mean a migration later | Accepted (2026-11-08) |
| D-021 | Drag-to-reorder uses `sh.calvin.reorderable` 3.1.0 (Apache-2.0) | Mature, small, Compose-native. Hand-rolling drag + autoscroll + accessibility isn't worth it | Accepted (2026-11-08) |
| D-022 | **Two practices, separately counted:** five prayer sessions (Fajr…Isha) running an editable after-prayer set (`prayer_set_item`, own per-prayer counts), plus daily targets (the enabled library routine). `daily_progress` is keyed by `(dhikr_id, local_date, context)` where context ∈ {fajr, dhuhr, asr, maghrib, isha, daily} | Owner request. Astaghfirullah ×3 after a prayer and ×100 for the day are different goals. Folded into schema v1 before any release, so no migration | Accepted (2026-11-08) |
| D-023 | **Streak day** = all five prayer sets complete AND every enabled daily target complete (prayer-only if no daily targets are enabled). Stored as `daily_summary.prayers_completed` + `is_completed` | Owner request. Replaces the earlier "all daily targets" rule | Accepted (2026-11-08) |
| D-024 | Default target cue changed from `Confirm` to **Double buzz** (waveform 45/90/45 ms). Optional sound cue in Phase 4, Glyph flash in Phase 7 | Owner testing: the switch to the next dhikr was easy to miss with `CONFIRM` on the Phone (2a) | Accepted (2026-11-08) |
| D-025 | V1 has **no prayer-time calculation or location**: the user picks the prayer slot, and the app suggests the first incomplete one. Day boundary = local midnight (not Maghrib) | Keeps V1 offline and permission-free. Prayer-time suggestion (offline calculation, manual city) can be added later without schema changes | Accepted (2026-11-08) |
| D-009 | Gradle wrapper pins the distribution SHA-256 | Supply-chain safety: Gradle refuses a tampered download | Accepted |

## Data (planned, Phase 2+)

| # | Decision | Why | Status |
|---|---|---|---|
| D-101 | Room for library, progress, sessions, summaries; Proto DataStore for preferences | Spec §10 | Accepted |
| D-102 | Daily progress rows snapshot the target for that date | Changing a target must never rewrite history (spec §5D) | Accepted |
| D-103 | Soft-delete Dhikrs (`deletedAt`) instead of hard delete | Keeps history and streaks intact | Proposed |

## Nothing platform

| # | Decision | Why | Status |
|---|---|---|---|
| D-201 | Glyph effects only while the app is in the foreground. Widgets and notifications never drive Glyphs | Glyph Developer Kit README: "Only foreground applications are allowed to be used." | Accepted |
| D-202 | Glyph integration sits behind a `GlyphController` interface with a no-op default implementation | Lets the app run on non-Nothing devices and in tests | Proposed |
| D-204 | Resolve the Glyph AAR's `minSdk 33` against the app's `minSdk 26`. Options: (a) raise the app's minSdk to 33 (Android 13); (b) keep 26 and use `tools:overrideLibrary="com.nothing.thirdparty"`, calling Glyph only when `SDK_INT >= 33` and on a Nothing device. Leaning towards (a) for simplicity | Verified 2026-11-08 from the AAR manifest | Open (decide in Phase 7) |
| D-203 | Lock screen: build an ongoing notification with an "increment" action as the guaranteed path. Also declare the Glance widget with `keyguard` category and test it on the device | Lock-screen widgets for third-party apps are an Android 16 QPR2 feature (Pixel first). **Device test 2026-11-08: Nothing OS 4.1 (B4.1-260928-1157) on the Phone (2a) has no lock-screen widget option**, so the notification is the lock-screen experience. Keep the `keyguard` category: it costs nothing if a future Nothing OS adds support | Accepted, verified on device |
