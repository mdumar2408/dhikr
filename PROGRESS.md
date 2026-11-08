# Development Progress

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
