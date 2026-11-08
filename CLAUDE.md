# Dhikr — project guide for Claude

Native Android Dhikr counter and daily-remembrance tracker. It targets Nothing phones first (Phone (2a), Nothing OS 4.1, **API 36**), is offline-first, with no accounts, backend or analytics. Distribution: signed APK first, Google Play later.

**Read first every session:** `PROGRESS.md` (where we are), `TECHNICAL_DECISIONS.md` (what's decided), `docs/DESIGN_SPEC.md` (how it looks).
The owner is an experienced Kotlin and backend developer who is new to Android. Explain Android-specific concepts as you go.

## Workflow (mandatory)

- Work in **phases** (see the roadmap in `PROGRESS.md`). Per phase: explain → Android concepts → acceptance criteria → affected files → implement → explain key code → build and test → update docs → **wait for the owner's confirmation before the next phase.**
- Inspect before editing. Make minimal, focused changes. Never overwrite a file you haven't read.
- Record every architectural decision or change in `TECHNICAL_DECISIONS.md`. Update `PROGRESS.md` at the end of each step.
- Never claim something compiled, passed or works on the device unless you ran it. Report failures with their output.
- Commit only when asked. Commit messages end with the attribution line the harness provides.

## Skill usage protocol

For every task: (1) identify the relevant skills below, (2) read their `SKILL.md` before implementing, (3) apply and combine them, (4) follow this file's architecture, (5) implement, (6) test, (7) build, (8) report results and open issues.
**On conflict:** correctness and official Android platform requirements > this CLAUDE.md and TECHNICAL_DECISIONS.md > project skills > vendored skills.

| Task | Skills |
|---|---|
| Any UI: screens, components, theme, dot-matrix | `nothing-design-system`, `compose-component-design`, `compose-state-and-effects`, `android-ux` (a11y and touch targets only) |
| Counter interaction, haptics, animation | `tactile-ui-engineering`, `compose-animations`, `jetpack-compose-performance` |
| Dhikr rules, targets, streaks, dates | `dhikr-domain-engineering` |
| Room, DataStore, repositories, DI wiring | `offline-first-android`, `android-data-layer`, `datastore` |
| Widgets, lock-screen notification | `android-widget-engineering`, `glance-appwidget-updates-and-memory`, `glance-canonical-layouts` |
| Coroutines and Flow | `kotlin-concurrency-and-flow` |
| Kotlin API shape | `kotlin-api-design` |
| Navigation | `navigation-3` (see D-010 before choosing) |
| Edge-to-edge and insets | `edge-to-edge` |
| Tests | `android-testing`, `compose-ui-testing-patterns`, plus the testing section of the relevant project skill |
| Crashes, ANRs, logcat, adb | `android-debugging` |

Vendored skills and their provenance: `.claude/skills/THIRD_PARTY_SKILLS.md`.

## Architecture

Single module `:app`, package `com.dhikr.app`. Pragmatic MVVM + Repository + Use cases.

```
core/{model,common,util}        plain Kotlin types, Clock helpers
domain/{repository,usecase}     interfaces, use cases, pure policies (no android.* imports)
data/{database,entity,dao,repository,preferences}
design/{theme,dotmatrix,components}
feature/{today,counter,library,selection,history,settings}   Screen + ViewModel + UiState per feature
widget/  notification/  audio/  glyph/
```

- **DI:** manual `AppContainer` on `DhikrApplication` (D-008). No Hilt or Koin unless a decision says so.
- **State:** each screen has an immutable `UiState` data class exposed as `StateFlow` from its ViewModel. Collect with `collectAsStateWithLifecycle()`. Events go up as lambdas.
- **One counter engine:** every increment, undo or reset from the app, widget or notification goes through the same use case. No duplicate counting logic anywhere.
- **Time:** inject `java.time.Clock`. Dates are the device's local `LocalDate`.
- **Glyph:** behind a `GlyphController` interface with a no-op default. Foreground only (D-201). The SDK AAR needs minSdk 33 (D-204, open).

## Coding standards

- Idiomatic Kotlin. Immutable UI state. Coroutines and Flow. **No `GlobalScope`**, no `runBlocking` in app code, no `!!` without a comment explaining why it's safe.
- **No business logic in composables.** Composables render state and forward events.
- All DB and disk I/O is asynchronous (suspend or Flow). Never on the main thread.
- Tokens only in UI (`DhikrTheme`). No hard-coded colours or dimensions in `feature/`.
- No new dependency without a reason recorded in TECHNICAL_DECISIONS.md. Stable versions from `gradle/libs.versions.toml`. Verify current versions online before adding. **Never invent SDK APIs:** check the docs, or `javap` the artifact.
- Strings in `res/values/strings.xml`. Layouts must work in RTL (Arabic UI later).
- Tests: unit tests for every business rule, integration tests for persistence, widget tests separate from screen tests.
- No Firebase, Retrofit, analytics or any network code in V1.

## Build, run and test

The toolchain is AGP 9.5.1, Gradle 9.6.0 (wrapper), Kotlin 2.4.21 (AGP built-in Kotlin, plus the Compose compiler plugin), compileSdk 37, targetSdk 36, minSdk 26. The SDK is at `%LOCALAPPDATA%\Android\Sdk`.

```powershell
.\gradlew.bat assembleDebug        # build
.\gradlew.bat testDebugUnitTest    # JVM unit tests
.\gradlew.bat lintDebug            # lint
.\gradlew.bat installDebug         # install on the connected device
```

**Claude's shell on this PC needs three things before running Gradle, Java or adb** (PowerShell, with the sandbox disabled):
```powershell
$t="$env:USERPROFILE\.gradle\tmp"; $env:TEMP=$t; $env:TMP=$t        # sandbox TEMP path is too long for Java's AF_UNIX sockets
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
$adb="$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
```
If Gradle reports "plugin not found" or PKIX/certificate errors, Avast HTTPS scanning has been switched back on (see PROGRESS.md).

**Device:** the Phone (2a) connects over **wireless debugging** (USB enumeration fails with the current cable). `adb devices` shows it via mDNS. If two entries appear, set `$env:ANDROID_SERIAL` to one. If none appear, ask the owner to re-enable Wireless debugging (re-pair if the pairing was lost).
Useful checks: `adb shell am start -W -n com.dhikr.app/.MainActivity`, `adb exec-out screencap -p > shot.png`, `adb logcat -d -b crash`.

## Product guardrails

- Calm and tactile, not gamified: no push nudges, leaderboards or confetti.
- Arabic text and any religious references in seed data must be accurate and sourced. If unsure, flag it rather than invent.
- Audio recordings need redistribution rights, recorded in `docs/` before they're bundled.
- Don't copy Nothing's proprietary fonts, assets or branding.
