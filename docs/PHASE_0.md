# Phase 0 — Environment and platform feasibility

This is a reference to come back to. Each section explains one concept and points at the
file in this repository where it shows up.

---

## 1. Android Studio project setup

1. Install the latest stable **Android Studio** from developer.android.com/studio. It ships its
   own JDK (the "JetBrains Runtime") and installs the Android SDK to
   `%LOCALAPPDATA%\Android\Sdk`.
2. In the first-run wizard, choose **Standard** setup. Then open **SDK Manager** and make sure
   these are installed:
   - SDK Platforms → **Android 17 (API 37)** (we compile against it) and **Android 16 (API 36)**
   - SDK Tools → Android SDK Build-Tools, Platform-Tools (this includes `adb`), Google USB Driver
3. **File → Open →** `D:\Work\Github\dhikr`. Studio writes `local.properties` (the path to your
   SDK, git-ignored) and runs a **Gradle sync**.
4. Create the Gradle wrapper scripts. Claude's sandbox couldn't run Gradle, so `gradlew` and
   `gradle/wrapper/gradle-wrapper.jar` are missing. After the sync, open the Gradle tool window
   (the elephant icon) → **Tasks → build setup → wrapper**, and double-click it. Commit the three
   files it creates.

We wrote the project by hand instead of using the "New Project → Empty Activity" wizard, so that
every line is intentional and explained. The result is the same shape the wizard produces.

## 2. Android project structure

```
dhikr/
├─ settings.gradle.kts        ← which modules exist + where dependencies come from
├─ build.gradle.kts           ← root: declares plugin versions (apply false)
├─ gradle.properties          ← Gradle/AGP flags
├─ gradle/
│  ├─ libs.versions.toml      ← version catalog: every dependency version
│  └─ wrapper/                ← pins the Gradle version for everyone
└─ app/                       ← the one module that becomes the APK
   ├─ build.gradle.kts        ← SDK levels, applicationId, dependencies
   └─ src/
      ├─ main/
      │  ├─ AndroidManifest.xml
      │  ├─ java/com/dhikr/app/MainActivity.kt   (Kotlin also lives under java/)
      │  └─ res/              ← resources: strings, themes, icons, drawables
      ├─ test/                ← JVM unit tests (fast, no device)
      └─ androidTest/         ← instrumented tests (run on a device)
```

**Resources (`res/`)** are non-code files that get compiled into the `R` class. For example,
`R.string.app_name` is an `Int` ID, and the right variant is chosen at runtime for language,
screen density, dark mode and so on. Folder suffixes such as `mipmap-anydpi` or `values-ar`
are *qualifiers*.

## 3. SDK and API levels

| Setting | Value | Meaning |
|---|---|---|
| `compileSdk` | 37 | Which Android APIs the **compiler** knows about. Doesn't affect runtime behaviour |
| `targetSdk` | 36 | "I have tested on Android 16." The OS turns on the behaviour changes up to this level (edge-to-edge, background limits and so on) |
| `minSdk` | 26 | The oldest OS allowed to install the app. Calling a newer API needs an `if (Build.VERSION.SDK_INT >= …)` check, and Lint warns if you forget |

Phone (2a) on Nothing OS 4.1 is **Android 16 = API 36**. The device-info screen prints the
real value. Record it in `PROGRESS.md`, because QPR (quarterly) releases share API 36 but can
differ in features.

## 4. Gradle and dependency management

- **Gradle** is the build tool. **AGP** (Android Gradle Plugin) teaches it to build Android apps.
  The **wrapper** (`gradlew`) downloads the exact Gradle version listed in
  `gradle-wrapper.properties`, so every machine builds the same way. Its SHA-256 is pinned.
- **Version catalog**: `libs.versions.toml` has three sections. `[versions]` holds the numbers,
  `[libraries]` the coordinates, and `[plugins]` the Gradle plugins. In build files they appear
  as type-safe accessors: `libs.androidx.core.ktx`, `alias(libs.plugins.kotlin.compose)`.
- **Compose BOM**: a "bill of materials" that pins a compatible set of Compose library
  versions, so you bump one number instead of six.
- **AGP 9 built-in Kotlin**: from AGP 9 onwards you no longer apply `org.jetbrains.kotlin.android`.
  Only the Compose compiler plugin is added, and its version must equal the Kotlin version.
- Configurations: `implementation` (compiled + shipped), `debugImplementation` (debug builds
  only, e.g. the preview tooling), `testImplementation` (unit tests only).

Useful commands, once `gradlew` exists:

```bash
./gradlew assembleDebug        # build app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug         # build + install on the connected phone
./gradlew test                 # JVM unit tests
./gradlew lint                 # static checks
```

## 5. AndroidManifest.xml

The manifest is read by the OS at install time, before any of your code runs. It declares:

- **Components**: activities, services, broadcast receivers and content providers. Widgets
  (Phase 6) are receivers declared here too.
- **Intent filters**: `MAIN` + `LAUNCHER` puts the app icon in the drawer.
- **`exported`**: whether *other* apps (including the launcher) may start the component.
  Since Android 12 you must state this explicitly on anything that has an intent filter.
- **Permissions**: none yet. Later we'll add `POST_NOTIFICATIONS` (Phase 7, a runtime
  permission since API 33) and `com.nothing.ketchum.permission.ENABLE` (Glyph).

## 6. MainActivity

An **Activity** is a screen-level entry point that the OS creates and destroys. Its lifecycle is
`onCreate → onStart → onResume → (visible) → onPause → onStop → onDestroy`.
**Rotation, a dark-mode switch or a language change destroy and recreate it.** That is why
state never lives in the Activity itself: later it goes in a `ViewModel` (which survives
recreation) and in Room/DataStore (which survives process death).

`setContent { … }` hands the window to Jetpack Compose. The UI is a function of state, and
`@Composable` functions re-run ("recompose") when the state they read changes. The app
will use a **single Activity** and switch screens with Navigation Compose.

## 7. Nothing Phone USB debugging

1. **Settings → About phone** → tap **Build number** 7 times (on some Nothing OS versions it
   sits under *Software info*). Enter your PIN, and "You are now a developer" appears.
2. **Settings → System → Developer options** → turn on **USB debugging**.
3. Connect with a data-capable USB-C cable and accept the **"Allow USB debugging?"** RSA
   fingerprint prompt on the phone (tick "Always allow from this computer").
4. Check the connection:

```bash
adb devices
```

```bash
adb shell getprop ro.build.version.sdk
```

```bash
adb shell getprop ro.build.display.id
```

If `adb devices` shows nothing on Windows, install **Google USB Driver** from the SDK Manager,
and switch the phone's USB mode to *File transfer*. **Wireless debugging** (Developer options
→ Wireless debugging → pair with code) also works from Studio's *Device Manager* pairing dialog.

`adb` lives in `%LOCALAPPDATA%\Android\Sdk\platform-tools`. Add it to `PATH` to use it outside Studio.

## 8. Physical-device deployment

- Pick the phone in Studio's device dropdown and press **Run ▶**. Studio runs `installDebug`
  and launches `MainActivity`.
- **Debug** builds are signed automatically with a per-machine debug key. Testers can't
  upgrade from a debug APK to a release APK (the signatures differ).
- For tester APKs (Phase 8) we'll create a **release keystore** once, keep it out of git and
  back it up. Losing it means testers have to uninstall to upgrade.
- **Logcat** (bottom panel) is the device log. Filter it with `package:mine`.

## 9. Lock-screen widget feasibility

**What exists:**
- Android 15 brought lock-screen widgets back for **tablets** (the "communal hub").
- **Android 16 QPR2** brought "Widgets on lock screen" to **phones**, first on Pixels. Third-party
  apps can appear there. Tapping a widget requires the user to **unlock**, and anyone can
  *see* the widgets ([9to5Google](https://9to5google.com/2025/08/20/android-qpr2-lock-screen-widgets/)).
- Nothing OS's own lock screen widgets (and "Lock Glimpse" on the 3a series) are Nothing's
  first-party feature. **I found no public API for third-party widgets on Nothing OS 4.1 /
  Phone (2a).**

**So "Android 16" does not mean "interactive lock-screen widgets for any app".** Even where the
feature exists, a tap needs an unlock, so it isn't a "count without unlocking" path.

**Device test (do this in Phase 0):**
1. Settings → Lock screen (and Display) → look for **"Widgets on lock screen"** or similar.
2. Long-press the lock screen. Can you add widgets from *other* apps (try Google Clock or Keep)?
3. Record the answer in `PROGRESS.md`.

**Plan (TECHNICAL_DECISIONS D-203):**
- The Glance widget will declare `android:widgetCategory="home_screen|keyguard"`. Where the
  host supports lock-screen widgets it can show up there for free. Not guaranteed.
- **Guaranteed fallback: an ongoing notification** with the current Dhikr, the count and an
  **Increment** action handled by a `BroadcastReceiver` that updates the same repository. Set
  `VISIBILITY_PUBLIC` so it shows on the lock screen, and `setAuthenticationRequired(false)`
  so the action runs without unlocking.
- Notification caveats: it needs the `POST_NOTIFICATIONS` permission. Since Android 14 users
  can swipe away most "ongoing" notifications. It is a *notification*, not a widget: limited
  layout, no dot-matrix freedom, and it sits in the notification stack.

| | Native lock-screen widget | Notification action |
|---|---|---|
| Availability on Phone (2a) | Unconfirmed, test needed | Yes, all Android versions |
| Tap without unlocking | No (unlock required) | Yes (with `setAuthenticationRequired(false)`) |
| Visual freedom | Full Glance layout | Standard notification template + small custom view |
| Can be dismissed | Removed by the user from the hub | Swipe (Android 14+) |

## 10. Glyph SDK feasibility

Source: [Nothing-Developer-Programme/Glyph-Developer-Kit](https://github.com/Nothing-Developer-Programme/Glyph-Developer-Kit).

- **Supported**: Phone (1), (2), **(2a)**, (2a) Plus, (3a)/(3a) Pro, (4a), (4b), on Nothing devices
  running Android 14 or newer. Phone (2a) is device `DEVICE_23111` (`Common.is23111()`).
- **Integration**: one AAR file in `app/libs/`, the permission
  `com.nothing.ketchum.permission.ENABLE`, and `<meta-data android:name="NothingKey" android:value="test"/>`
  in `<application>`. Apps targeting Android 16 no longer *need* an API key, but Nothing
  recommends keeping the entry.
- **Debug mode** (needed with the `test` key; it expires after 48 h):

```bash
adb shell settings put global nt_glyph_interface_debug_enable 1
```

- **API shape**: `GlyphManager.getInstance(context)` → `init(callback)` → in
  `onServiceConnected`: `register(Glyph.DEVICE_23111)` and `openSession()` → drive LEDs with
  `GlyphFrame` via `toggle()`, `animate()` or `displayProgress()` → `turnOff()`, `closeSession()`, `unInit()`.
- **Phone (2a) zones**: C1–C24 = indices 0–23 (C1 bottom-left, C24 top-right), B = 24, A = 25.
  `displayProgress()` drives zone C, which makes it a natural fit for "progress toward today's target".
- **Hard restriction**: *only foreground apps may use the SDK*. So the home-screen widget,
  the notification action and the lock screen **cannot** light Glyphs. Glyph feedback is an
  in-app (counter screen open) feature only.

**Feasibility test:** download the AAR from the repo's releases, enable debug mode, then run
the sample, or a throwaway activity that calls `displayProgress` at 50% on the (2a). We won't
add the AAR to Dhikr until Phase 7.

---

### Phase 0 acceptance criteria
- [ ] Project opens and syncs in Android Studio with no errors
- [ ] `gradlew` generated and committed
- [ ] App installs on the Phone (2a) and shows Device / Android API / Build
- [ ] Lock-screen widget availability recorded
- [ ] Glyph debug mode enabled and a test progress shown on zone C
- [ ] Initial commit pushed
