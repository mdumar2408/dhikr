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
