# Claude Code skills setup report (2026-11-08)

## 1. Environment

| Item | State |
|---|---|
| Claude Code | Desktop app (Code tab). No `claude` CLI on PATH. Supports project skills (`.claude/skills/`) and plugins |
| Skills before setup | None (no `~/.claude/skills`, no project `.claude/`) |
| Android toolchain | Android Studio 2026.2.2, SDK (platforms 36 + 37.0, build-tools 36.0.0, platform-tools 37.0.1), JDK 21 (Temurin) + Studio JBR, Gradle 9.6.0 wrapper, AGP 9.5.1 |
| Device | Nothing Phone (2a), Android 16 / API 36, wireless adb |
| Python, `gh` | Not installed (affects only the skipped skills that ship scripts) |

## 2. Repositories evaluated

All four are plain Markdown `SKILL.md` collections, cloned shallowly and inspected locally. **None ships hooks or MCP servers.** Executable content found: Python helper scripts in a few skills (r8-analyzer, play-policy-insights, gradle-run, release-kotlin-library, run-github-project), and `curl | bash` installers for the Android CLI on macOS and Linux, inside `android-cli`. None of those skills were installed.

| Repo @ commit | Licence | Skills | Install method upstream | Verdict |
|---|---|---|---|---|
| **android/skills** (official) @ bf68e2d | Apache-2.0 | 35 | Claude plugin marketplace | 5 installed |
| **chrisbanes/skills** @ 72628ae | Apache-2.0 | 21 | Claude plugin marketplace | 6 installed |
| **rcosteira79/android-skills** @ 0f9af5a | MIT | 21 | Claude plugin marketplace | 5 installed |
| **Drjacky/claude-android-ninja** @ 5d40f86 | Apache-2.0 | 1 large skill (40 references) | Copy into skills dir | **Not installed** |

**Why not install them as plugins?** The plugin route installs every skill in each repo, about 80 in total. Most are irrelevant (camera, cars, TV, Wear, XR, KMP, Retrofit, Koin, release workflows). Some contradict our decisions, and they'd all compete for activation. Instead I vendored only the relevant skill folders into the repo (D-011).

## 3. Skills installed (vendored, verbatim)

| Skill | From | Why it's useful here |
|---|---|---|
| glance-appwidget-updates-and-memory | android | Widget update mechanics, RemoteViews bitmap limits (Phase 6) |
| glance-canonical-layouts | android | Glance layout patterns (Phase 6) |
| jetpack-compose-performance | android | Recomposition and animation cost for the rapid-tap counter |
| edge-to-edge | android | Insets on API 35+ (required since we target 36) |
| navigation-3 | android | Input for the open navigation decision D-010 |
| compose-animations | chrisbanes | Dot cross-fades, press states, completion |
| compose-component-design | chrisbanes | API shape for BeadPad, BeadRow, DotMatrixText |
| compose-state-and-effects | chrisbanes | State hoisting, effects, ViewModel state |
| compose-ui-testing-patterns | chrisbanes | Compose UI tests, previews |
| kotlin-concurrency-and-flow | chrisbanes | Scopes, Flow, the serialised counter engine |
| kotlin-api-design | chrisbanes | Domain types, use-case APIs |
| android-data-layer | rcosteira79 | Repository and error-boundary pattern |
| datastore | rcosteira79 | Proto/typed DataStore |
| android-testing | rcosteira79 | Unit and instrumented test strategy |
| android-debugging | rcosteira79 | adb, logcat, crash and ANR triage |
| android-ux | rcosteira79 | Touch targets, accessibility (visual style overridden by our design system) |

Provenance, commits and licences: `.claude/skills/THIRD_PARTY_SKILLS.md` and `.claude/skills/_third-party-licenses/`.

## 4. Skills deliberately not installed

| Skill(s) | Reason |
|---|---|
| claude-android-ninja (whole repo) | One monolithic skill that mandates Hilt, Navigation 3 and multi-module convention plugins (plus Firebase, Sentry, Detekt templates). It conflicts with D-001 and D-008 (single module, no DI framework) and would duplicate the vendored skills. Worth revisiting only if we modularise |
| rcosteira79 `android-dev` | Its baseline mandates Hilt and `:core`/`:feature` modules |
| android `testing-setup` | Installs Hilt on non-KMP projects by default |
| chrisbanes `compose-performance`, rcosteira79 `compose`, `kotlin-flows`, `kotlin-coroutines` | Duplicate the installed official or chrisbanes equivalents |
| `gradle-run`, `r8-analyzer`, `play-policy-insights` | Ship Python scripts (Python isn't installed). Gradle quirks are documented in CLAUDE.md instead. Revisit r8 and play-policy for Phase 8 and Play release |
| `android-cli` | The installer is `curl \| bash` for macOS and Linux only. Not needed: we use Gradle and adb directly |
| `agp-9-upgrade` | We're already on AGP 9.5 with built-in Kotlin |
| Camera, cars, TV, Wear, XR, ink, identity, Play billing, media cast, on-device AI, KMP, Retrofit, Ktor, Koin, Paging, Coil, RxJava, PDF, release and GitHub-workflow skills | Out of scope for Dhikr V1 |

## 5. Project-specific skills created

| Skill | Covers |
|---|---|
| `nothing-design-system` | Tokens, the dot-matrix engine (glyph and Arabic raster paths), components, no proprietary assets |
| `android-widget-engineering` | Glance widgets, per-instance selection, bitmap rendering, updates, the lock-screen notification with "+1" (verified facts: no lock-screen widgets, no Glyph from background) |
| `tactile-ui-engineering` | Tap pipeline, no debounce, haptics constants by API level, press and completion animation, hold-to-reset |
| `dhikr-domain-engineering` | Library, enabled and active concepts, target snapshots, completion and streak policy, date and timezone rules, Arabic normalisation, seed data integrity |
| `offline-first-android` | Room (KSP, schema export, atomic increments, FK RESTRICT plus soft delete, migrations), Proto DataStore, AppContainer DI, cross-surface refresh |

Each has a `description` (when to activate), implementation guidelines, conventions, common mistakes and testing requirements. Where an installed skill already covers something (Glance mechanics, Compose APIs, DataStore basics), the project skill points to it instead of repeating it.

## 6. Verification

- All 21 skills have valid frontmatter, `name` matching the folder, and a non-empty description. ✅
- No duplicate skill names across vendored and custom skills. ✅
- **Discovery in a live session: not yet confirmed.** This setup session started in `D:\Work\Github`, so it doesn't load `dhikr/.claude/skills` (invoking `nothing-design-system` here returned "Unknown skill", as expected). To confirm, open a new Code session with the folder `D:\Work\Github\dhikr` and ask "which project skills are available?". All 21 should be listed.
- Note: the vendored `glance-appwidget-updates-and-memory` description is about 1,400 characters, longer than the 1,024-character guideline of the open Agent Skills spec. It's kept verbatim from Google. If it fails to load, shorten its `description:` locally and note it in THIRD_PARTY_SKILLS.md.

## 7. Dependencies and tools required later

| Phase | Adds |
|---|---|
| 1 | Navigation (Compose or Nav3, D-010), lifecycle-viewmodel-compose / runtime-compose |
| 2 | Room + KSP plugin + `androidx.room` Gradle plugin, Proto DataStore + protobuf Gradle plugin (or a JSON-typed DataStore fallback) |
| 3 | kotlinx-coroutines-test, Turbine, room-testing |
| 4 | Media3 (only if SoundPool or MediaPlayer prove insufficient) |
| 6 | Glance appwidget + glance-appwidget-testing, WorkManager (midnight refresh) |
| 7 | Glyph SDK AAR (minSdk 33, D-204) |
| 8 | Release keystore, R8. Python, optionally, for `r8-analyzer` |

MockK was requested in the brief. The project skills prefer **fakes** (fake repositories and clocks) for domain and ViewModel tests. MockK will be added only where a fake is impractical, as a recorded decision.

## 8. Recommended next steps

1. Open a new session in `D:\Work\Github\dhikr` and confirm the 21 skills are discovered.
2. Commit `.claude/`, `CLAUDE.md` and the docs.
3. Resume Phase 1: answer the open design questions (accent colour, Arabic font, bead row, themes) and decide D-010 (navigation).
