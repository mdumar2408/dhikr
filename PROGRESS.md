# Development Progress

## Phase 2 — Persistence and Dhikr library (done 2026-11-08, awaiting owner review)

- **Room 3** (D-020) schema v1: `dhikr` (soft delete, `seed_key`, `sort_order`, `search_key`), `daily_progress` keyed by **(dhikr, date, context)**, `prayer_set_item`, `daily_summary` (with `prayers_completed`). Schema JSON is in `app/schemas/`.
- **Verified seed library**: 10 entries, every reference checked on sunnah.com (`docs/SEED_LIBRARY.md`), plus the default **after-prayer set** (istighfar ×3, *Allahumma antas-salam*, 33/33/33, tahlil ×1).
- **Preferences** persisted with a JSON-typed DataStore (D-019).
- **Library**: search (Arabic diacritics-insensitive), enable/disable, add/edit sheet (only Arabic or transliteration required), delete with confirmation plus undo (soft delete), drag-to-reorder (D-021).
- Owner feedback folded in: **prayer tasbeeh + daily targets** model and the **new streak rule** (DESIGN_SPEC §14, D-022/D-023/D-025). **Double buzz** is now the default target cue (D-024).
- Tests: **33 unit tests pass** (ArabicText, DhikrDraft, SeedLibrary, preferences serializer, dot matrix, Flow). **7 instrumented Room tests pass on the Phone (2a)** (seeding, prayer set, soft delete/restore, no resurrection, add/edit, reorder, toggle).
- On device: seeded library shown. Toggle + add survive a force-stop.
- Not yet verified on the device by hand: drag-reorder gesture, delete → undo snackbar, search typing (covered by repository tests and VM logic, not by UI tests).
- **Still in memory: counts** (and Flow position). Persisting counts per context is the Phase 3 engine.

### Updated roadmap (owner request §14)
- **Phase 3 — Counter engine + practices:** persist counts per context (transactional, atomic), prayer-session Flow (Fajr…Isha) and daily-target Flow on the same engine, Today as a chooser (Prayer tasbeeh / Daily targets), prayer-set editor, hold-to-reset, undo persisted, Compose UI tests.
- **Phase 4 — Audio and cues:** optional switch chime / spoken next-dhikr, audio per dhikr.
- **Phase 5 — History and streaks:** daily summaries with the new streak rule (5 prayers + daily targets), calendar.
- Phases 6–8 unchanged (widgets, Nothing integrations, release).

## Tooling: Claude Code skills (2026-11-08)

- 16 vendored skills + 5 project skills in `.claude/skills/`; `CLAUDE.md` created. Report: `docs/SKILLS_SETUP_REPORT.md`
- [ ] Confirm discovery in a new session opened in `D:\Work\Github\dhikr`

## Phase 1 — Foundation and design system (in progress)

Step 1 (2026-11-08): visual design spec written → `docs/DESIGN_SPEC.md`.
Step 2 (2026-11-08): design answers recorded (ember accent, Noto Naskh, light and dark). Bead Lab explored animation and haptics on the device (no haptic primitives on the Phone (2a)). Owner picked the defaults: **Slide, Soft spring, Clock tick, Confirm**, all configurable. Added **Flow** (§13): auto-advance through the library routine when a target is reached (D-015 to D-017).
Step 3 (2026-11-08): **Phase 1 implemented** and verified on the Phone (2a):
- Navigation 3 (D-010). Theme tokens light and dark plus system (D-014). Dot-matrix engine (font, `DotGrid`, Arabic raster path, animated `DotMatrixText`, `DotMatrixBitmap`). Components (`BeadString` Slide/Ring/Row, `BeadRing`, `BeadProgress`, `BeadPad`, `DotNavBar`, `RoutineDots`, `PillChip`).
- Pure `FlowPolicy` (D-015) with an in-memory store (D-018). `HapticPlayer` (D-013). Screens: Today, Counter (Flow + NEXT card + undo + swipe), Library (enable toggle), History (calendar shell), Settings (theme + counting feel with live preview).
- `testDebugUnitTest`: **20/20 pass** (DotMatrixTest 8, FlowPolicyTest 12). `assembleDebug` OK. Installed over wireless adb.
- On device: Today renders the shaped Arabic in dots. 33 taps on SubhanAllah → NEXT card → Alhamdulillah 0/33 (Flow 2/4). Light theme correct. No crashes in logcat.
- Not yet done: lint run, Compose UI tests (planned with Phase 3's engine), hold-to-reset (Phase 3), library add/edit/reorder (Phase 2).
- Known limitation: counts and settings are in memory and reset when the app process dies (fixed by Phase 2 persistence).

Original plan:
1. Package skeleton: `design/{theme,dotmatrix,components}`, `feature/{today,counter,library,history,settings}`, `navigation/`
2. Theme: colour, type and spacing tokens as Kotlin objects + `DhikrTheme` (dark/light)
3. Dot-matrix engine: `DotMatrixFont` (5×7 digits + Latin caps), `DotMatrixRenderer` (glyph → grid; text/Arabic → shaped bitmap → sampled grid), `DotMatrixText` (Compose Canvas, animated dots), `DotMatrixBitmap` (for widgets later)
4. Components: `BeadRow`, `BeadRing`, `BeadPad`, `DotNavBar`, `HairlineRow`
5. Navigation Compose with 4 destinations + full-screen Counter route; static placeholder data, **no database yet** (Phase 2)
6. Counter screen layout with local in-memory count (real engine is Phase 3)
7. Unit tests: font glyph integrity, renderer sampling; Compose preview for each screen

Acceptance criteria:
- [x] App launches into Today; bottom bar switches the 4 destinations; back works as expected
- [x] Counter opens from Today, hides the bar, counts on pad tap (in memory), and Flow advances at the target
- [x] Dot-matrix digits render crisply at counter and display sizes; Arabic renders correctly shaped (joined letters) when sampled to dots
- [x] Dark and light themes both render with tokens only (no hard-coded colours in screens)
- [x] Unit tests pass (20/20); `assembleDebug` builds; runs on the Phone (2a) without crashes
- [x] TECHNICAL_DECISIONS.md updated (D-010, D-012 to D-018)

## Phase 0 — Environment and platform feasibility (in progress)

Done (2026-11-08):
- [x] Project skeleton written by hand: Gradle Kotlin DSL, version catalog, `:app` module, manifest, `MainActivity` with a device-info screen
- [x] Gradle wrapper properties (Gradle 9.6.0, checksum pinned)
- [x] `TECHNICAL_DECISIONS.md`, this file, `docs/PHASE_0.md`
- [x] Git repository initialised
- [x] Facts checked: Phone (2a) on Nothing OS 4.1 = Android 16; Glyph Developer Kit supports Phone (2a) (device `23111`)

Not done yet (needs your machine):
- [x] Android Studio installed, SDK installed, `gradlew` generated, debug APK builds
- [x] Debugging enabled on the Phone (2a) (wireless); app runs and shows API 36 / B4.1
- [x] Lock-screen test: no third-party lock-screen widgets on Nothing OS 4.1 / Phone (2a)
- [x] Glyph test: SDK connects, registers and drives progress/animation on Phone (2a) (LEDs confirmed lit)
- [ ] First commit

Environment notes (2026-11-08):
- Android Studio 2026.2.2 installed (winget). Android SDK **not yet installed** — run Studio's setup wizard.
- Gradle wrapper generated (`gradlew`, `gradle-wrapper.jar`). Plugin resolution (AGP 9.5.1) works.
- **Avast HTTPS scanning** re-signs TLS traffic with "Avast Web/Mail Shield Root". Java doesn't trust it
  → Gradle fails with "plugin not found" / PKIX errors. Workarounds: Avast exclusion for Java/Studio,
  or run Gradle on JDK 21 with `-Djavax.net.ssl.trustStoreType=Windows-ROOT`.
  → Resolved: Avast HTTPS scanning turned off; `gradlew` now resolves dependencies with Studio's JBR.
- Android SDK installed via command-line tools at `%LOCALAPPDATA%\Android\Sdk`:
  platform-tools 37.0.1, platforms android-36 + android-37.0, build-tools 36.0.0. `local.properties` points at it.
- ✅ `gradlew assembleDebug` — BUILD SUCCESSFUL (app-debug.apk, 11.1 MB, unminified debug build).
- Next: connect the Phone (2a) with USB debugging and run `gradlew installDebug`.

Device facts (fill in from the device-info screen):
- Device: Nothing A142 (Phone (2a), product `PacmanIND`)
- Android release / API: 16 / **API 36** (confirmed 2026-11-08)
- Build (`Build.DISPLAY`): B4.1-260928-1157, security patch 2026-10-01
- Connection: USB enumeration failed ("Device Descriptor Request Failed" — likely cable). Using **wireless debugging** (paired; mDNS finds the connect port automatically)
- ✅ `installDebug` + cold launch OK (~1.6 s), no crashes in logcat
- Lock-screen widgets available: **no** — Nothing OS 4.1 (B4.1-260928-1157) has no "widgets on lock screen" option for third-party apps (checked by user, 2026-11-08). → Lock-screen counting will use the notification fallback (D-203)
- Glyph debug mode works: yes (enabled 2026-11-08 08:44, auto-expires after 48 h)
- Glyph SDK test (throwaway app `com.dhikr.glyphtest`, not in this repo):
  - AAR: `glyph-matrix-sdk-2.0.aar` from github.com/Nothing-Developer-Programme/Glyph-Developer-Kit `sdk/`,
    114,689 bytes, sha256 `329393019db5f0f987c6245855d13fa273d06756c68829ca0f6ae686ba336da1`
  - Service `com.nothing.thirdparty/.GlyphService` connected in ~70 ms; `Common.is23111()` = true;
    `register(DEVICE_23111)` = true; `openSession()` OK
  - `displayProgress(C, 0..100)` and `animate(A+B)` returned without errors
  - LEDs visibly lit: **yes** — C bar showed 50% (confirmed by user, 2026-11-08)
  - ⚠ The AAR declares **minSdk 33**; Dhikr is minSdk 26 → decision needed in Phase 7 (TECHNICAL_DECISIONS D-204)
