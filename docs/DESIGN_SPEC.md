# Dhikr — Visual Design Specification (v0.1, for approval)

> A digital Tasbeeh: one calm object you hold in one hand.
> Monochrome, dot-matrix, tactile. Inspired by industrial minimalism, using no proprietary Nothing assets.

---

## 1. Principles → concrete rules

| Principle | Rule we enforce |
|---|---|
| One primary action per screen | The counter screen has exactly one large target: the **bead pad** |
| One-handed | Everything you touch while counting lives in the **bottom 60%** of the screen. The top area is read-only |
| Calm | Monochrome. A single accent colour (**ember**) appears only when a target is completed |
| Tangible | Dot-matrix numerals, a physical "press" on the pad, a bead row that fills like a real tasbeeh |
| Restrained | No cards-in-cards, no charts on Today, no badges, no confetti |

---

## 2. Colour tokens

Dark is the default (OLED black saves battery and looks best with dot-matrix). Light mode is a "paper and ink" theme.

| Token | Dark | Light | Use |
|---|---|---|---|
| `canvas` | `#000000` | `#F3F1EC` | Screen background |
| `surface` | `#0E0E0E` | `#E9E6DF` | Bead pad, list rows (pressed) |
| `surfaceRaised` | `#181818` | `#DEDAD1` | Sheets, dialogs |
| `hairline` | `#262626` | `#CFCAC0` | Dividers, outlines |
| `ink` | `#F2F2F2` | `#111111` | Primary text, lit dots |
| `inkMuted` | `#8A8A8A` | `#5E5B55` | Secondary text |
| `inkFaint` | `#4A4A4A` | `#9C978D` | Labels, metadata |
| `dotOff` | `#1C1C1C` | `#DCD7CD` | Unlit dot-matrix cells |
| `ember` | `#E8452C` | `#C9341D` | Completion only (target reached, day complete) |

All text pairs meet WCAG AA (ink/inkMuted on canvas ≥ 4.5:1). `inkFaint` is used only for non-essential labels of 12 sp or more.

## 3. Typography

| Role | Face | Size / line | Notes |
|---|---|---|---|
| `counter` | **Dot-matrix** (our own 5×7 font) | ~120 dp tall | The count on the counter screen |
| `display` | Dot-matrix | 32–40 dp tall | Screen titles ("TODAY"), totals |
| `arabic` | Noto Naskh Arabic (system) | 32 sp / 1.6 | Dhikr text. Bundling **Amiri** (OFL) is an option for a more classical look — see §11 |
| `arabicDot` | Arabic shaped by Android, then sampled into dots | 28–40 dp | Widgets and headers only, never for long text |
| `title` | Roboto Medium | 20 sp / 28 | |
| `body` | Roboto | 16 sp / 24 | Transliteration, meaning |
| `label` | Monospace, uppercase, +8% letter spacing | 12 sp / 16 | "TARGET", "STREAK", nav labels |

Dot-matrix is for **numbers and short headings only**. Readable text (meaning, references, settings) stays in normal fonts.

## 4. Spacing, shape and size tokens

- **Spacing** (4 dp grid): `xxs 4 · xs 8 · s 12 · m 16 · l 24 · xl 32 · xxl 48 · xxxl 64`
- **Screen gutter**: 24 dp
- **Shape**: `none 0 · s 8 · m 16 · l 28 · pill 50%`. The bead pad uses `l` (28 dp) for a soft physical slab
- **Minimum touch target**: 48 × 48 dp. Secondary controls are 56 dp
- **Dot grid**: dot diameter `d`, pitch `1.5 d`. Counter uses `d = 14 dp`, display uses `d = 5 dp`, widgets 3–8 dp depending on size

## 5. Navigation

```
Today ──► Counter (full screen, no bottom bar)
Library ──► Dhikr detail / edit sheet
History
Settings
```

- **Bottom bar**: four text labels in monospace caps (`TODAY  LIBRARY  HISTORY  SETTINGS`). The active one gets a single lit dot above it. No icons, no Material pill.
- **Counter** is a full-screen destination opened from Today. It hides the bottom bar for maximum focus. Back (gesture) returns to Today.
- The app opens on **Counter** for the last active Dhikr if you used it in the last 30 minutes; otherwise on **Today**. (Adjustable in Settings.)

## 6. Screen layouts

### 6.1 Counter — the heart of the app

```
┌──────────────────────────────┐
│ ‹  2 / 4             ⋯       │  top bar: back · position · overflow (reset, edit)
│                              │
│      سُبْحَانَ ٱللَّٰهِ            │  Arabic (32 sp), centred
│      SubhanAllah             │  transliteration (body, muted)
│      Glory be to Allah       │  meaning (body, faint) — optional
│                              │  ◄ swipe here to switch Dhikr ►
│  ●●●●●●●●●●○○○○○○○○○○○○○○○○○  │  bead row: 33 beads = progress toward target
├──────────────────────────────┤
│                              │
│          ██ ██ ██            │
│          ██ ██ ██   / 100    │  dot-matrix count, target beside it
│                              │
│                              │  BEAD PAD — the whole lower area is one
│                              │  tap target (~60% of the screen height)
│      tap anywhere to count   │  hint shown only for the first 3 sessions
│                              │
│   [ undo ]         [ ♪ on ]  │  bottom corners: undo −1 · audio toggle
└──────────────────────────────┘
```

- **Bead pad**: a `surface` slab with radius `l`. Tap = +1. On press the slab darkens by one step and the count's dots "settle" (0.97 scale, 90 ms). No ripple splash.
- **Bead row**: 33 beads (the traditional tasbeeh). Each bead = target ÷ 33. If the target is 33, one bead lights per tap. Beads turn **ember** when the target is reached.
- **Switching Dhikr**: horizontal swipe on the **top** half only, plus `2 / 4` as a tappable picker. Never on the pad, to prevent accidental switches.
- **Undo**: always present, bottom-left, 56 dp. Removes the last increment. No confirmation needed: it's cheap to redo.
- **Reset**: in the overflow menu. **Hold to reset** (1.2 s with a filling ring), no dialog. Prevents accidents without nagging.
- **Completion**: when count = target → one stronger haptic, beads and count turn ember for 1.5 s, and the label "COMPLETE" appears. By default the counter then **advances to the next dhikr** (Flow, §13). With "Stay on this dhikr" selected, counting continues past the target instead. Extra dhikr is never blocked.

### 6.2 Today

```
┌──────────────────────────────┐
│ TODAY              SAT 8 NOV │
│                              │
│        ⣿⣿⣿⣿⣿⣿⣷⣄            │  overall progress as a dot ring
│        175 / 350             │  dot-matrix total
│        ● 12-day streak       │
│                              │
│ ─────────────────────────── │
│ سُبْحَانَ ٱللَّٰهِ   SubhanAllah  │
│ ●●●●●●●●●○○○○○  64 / 100   › │  one row per enabled Dhikr
│ ─────────────────────────── │
│ ٱلْحَمْدُ لِلَّٰهِ  Alhamdulillah │
│ ●●●●●●●●●●●●●●  100 ✓      › │  completed row: beads in ember
│ ─────────────────────────── │
│                              │
│ TODAY  LIBRARY  HISTORY  SET │
└──────────────────────────────┘
```

- Rows are separated by hairline dividers, not cards. Tapping a row opens Counter on that Dhikr.
- **Empty state** (no enabled Dhikrs): a single unlit 33-bead ring, "Choose your daily dhikr" and a **Open library** button.

### 6.3 Library

- Search field at the top. Then the list: Arabic on the right (RTL-aligned), transliteration and target on the left, and a **toggle dot** (lit = enabled) on the far right, 48 dp.
- **Add** = a floating "+ Add dhikr" pill at the bottom right (thumb zone). It opens a **bottom sheet** with only two required-looking fields (Arabic or transliteration, at least one), plus target (default 33) and optional meaning, reference and audio collapsed under "More".
- Long-press or swipe a row to edit or delete. Deletion is a soft delete, with an "Undo" snackbar.

### 6.4 History

- A **dot calendar**: month grid, one dot per day. Completed = ink dot, partial = hollow ring, ember = today. No heat-map gradients.
- Below it: current streak, longest streak, total completed days (dot-matrix numerals, three columns).
- Tapping a day shows that day's per-dhikr totals in a sheet.

### 6.5 Settings

A plain list: audio, haptics (off · subtle · strong), theme (system · dark · light), keep screen on while counting, open-on-launch behaviour, about and licences.

## 7. Interaction states

| Element | Rest | Pressed | Disabled | Complete |
|---|---|---|---|---|
| Bead pad | `surface` | `surfaceRaised` + 0.97 count scale | — | ember count + beads 1.5 s |
| Dot (matrix) | `dotOff` | — | `dotOff` | `ember` |
| Text button | `inkMuted` | `ink` | `inkFaint` | — |
| Toggle dot | ring `hairline` | — | — | filled `ink` |
| Bead row | lit `ink` / off `dotOff` | — | — | `ember` |

## 8. Motion and haptics

- **Count change**: dots that switch on fade in over 80 ms, dots that switch off fade out over 120 ms, so digits morph rather than jump. No rolling or flipping.
- **Durations**: `fast 90 ms · base 180 ms · slow 320 ms`. Easing is standard decelerate. Respects the system "remove animations" setting.
- **Haptics**: each tap → `CLOCK_TICK` (crisp, short). Target reached → `CONFIRM` (API 30+) or a double tick on older Android. Hold-to-reset complete → `LONG_PRESS`. Everything can be turned off in Settings.
- **Glyph** (Phase 7, foreground only): the C bar mirrors the active Dhikr's progress toward its target.

## 9. Widgets (Phase 6, designs only)

| Compact (2 × 2) | Expanded (4 × 2) |
|---|---|
| Dhikr name (label) | Arabic (bitmap, dot-sampled) + transliteration |
| Big dot-matrix count | Big count · `/ target` |
| `/ 100` | 33-bead progress row |
| Whole widget = tap +1 | Tap count = +1 · audio icon status |

- Rendered as **bitmaps** using the same `DotMatrixRenderer` as the app, so they look identical.
- Each widget instance can pin its own Dhikr. The count comes from the same database as the app.
- Lock screen: an ongoing notification ("SubhanAllah · 64 / 100", action: **+1**), see D-203.

## 10. Accessibility

- The bead pad is announced as a button: "Count SubhanAllah, 64 of 100".
- Count changes are announced politely every 10 taps (not every tap, to avoid noise), and at completion.
- Dot-matrix text always carries a `contentDescription` with the plain number or word.
- Font scaling: readable text scales with system font size. The dot-matrix counter scales down to fit rather than overflowing.

## 11. Decisions (2026-11-08)

1. **Accent colour**: ember (`#E8452C` dark / `#C9341D` light). ✅ Approved.
2. **Arabic font**: system Noto Naskh. ✅ Approved.
3. **Bead row**: 33 beads (traditional set). Animation style still to be chosen in Bead Lab (§12).
4. **Themes**: **both light and dark**, following the system setting by default, with an override in Settings. ✅ Approved, and both verified on the device in Bead Lab.

## 12. Bead animation and per-count haptics (exploration, 2026-11-08)

### Animation technology

| Option | Size cost | Fit | Verdict |
|---|---|---|---|
| **Compose native** (`Animatable` spring + `Canvas`) | 0 | Physics-driven and per-tap, matches the dot-matrix drawing exactly, and shares code with the widget bitmaps | ✅ **Chosen** |
| Lottie (`lottie-compose` 6.7.1) | ~0.4 MB AAR + JSON assets | Great for pre-authored, timeline-based clips. Can't easily express "bead *k* of *n* at spring position *p*". Needs After Effects authoring and licensed assets | Optional, only for a one-off completion flourish if ever wanted |
| Rive (`rive-android` 11.16.2) | 13 MB AAR, ~2.4 MB download / ~7 MB install (native libraries) | Interactive state machines. Needs the Rive editor. Heavy for one counter | ❌ Too heavy for the benefit |

**Technique:** one spring-animated float `p` (the count) drives every bead: bead *k* is waiting while `p ≤ k`, in flight while `k < p < k+1`, and counted once `p ≥ k+1`. Retargeting a running spring keeps its velocity, so 10+ taps per second stream smoothly with no queue and no dropped taps.

### Three styles to choose from (try them in Bead Lab)

- **Slide**: a tasbeeh on a string. Uncounted beads (hollow) wait on the right, and each count slides one bead across the gap to join the counted stack. The 33rd "imam" bead is elongated. *Most literal.*
- **Ring**: a loop of 33 that rotates one bead per count past a "now" mark at 12 o'clock. *Most compact; doubles as the Today ring.*
- **Row**: the spec's 33-dot progress row. Each newly lit dot pops in (1.5× → 1×). *Most minimal.*
- Spring feel: **Soft** (damping 0.62, slight settle bounce) or **Crisp** (no bounce).

### Haptics: what the Phone (2a) can actually do

Measured on the device with `Vibrator.arePrimitivesSupported` / `hasAmplitudeControl`:
**no composition primitives** (CLICK, TICK, LOW_TICK, THUD, SPIN, rises and falls all ✗) and **no amplitude control**.

So the available vocabulary is:
1. **View haptic constants** (`CLOCK_TICK`, `KEYBOARD_TAP`, `SEGMENT_FREQUENT_TICK`, `CONFIRM`). These are tuned by Nothing for this motor, respect the user's system touch-feedback setting, and need no permission. ← preferred.
2. **Predefined effects** (`EFFECT_TICK`, `EFFECT_CLICK`, `EFFECT_DOUBLE_CLICK`, `EFFECT_HEAVY_CLICK`).
3. **On/off waveforms** (e.g. a 6 ms pulse, or a "clack": 7 ms on, 38 off, 5 on). These are the only way to make a custom texture here, at the cost of needing the `VIBRATE` permission.

The app will still *use* primitives on devices that support them (detected at runtime), with these as fallbacks.

### Defaults (chosen by the owner on the device, 2026-11-08) — all user-configurable

| Setting | Default | Options in Settings → Counting feel |
|---|---|---|
| Bead animation | **Slide** | Slide · Ring · Row |
| Spring | **Soft** (damping 0.62, stiffness 420) | Soft · Crisp |
| Haptic per count | **Clock tick** (View `CLOCK_TICK`) | Clock tick · Key tap · Segment tick · Pulse · Clack · Off. Options the motor can't play are hidden, not shown as fallbacks |
| Haptic at target | **Confirm** (View `CONFIRM`) | Confirm · Thud · Three beads · Off |

Settings shows a small live preview pad, so each option can be felt before choosing.

## 13. Flow — continuous counting through the library

**Goal:** the user only taps. The app moves through their daily dhikrs in order, and *tells* them, without them having to look, when to start the next one.

### Behaviour

1. **Order**: enabled Dhikrs in the user's library order. The Library gets drag-to-reorder, so the order *is* the routine (e.g. SubhanAllah → Alhamdulillah → Allahu Akbar).
2. **Start**: Counter opens on the **first Dhikr not yet complete today**. If it's partly done (e.g. 10/33 from the widget), it continues from 10.
3. **Reaching the target** (the tap that makes `count == target`):
   - the **target haptic** (Confirm) fires instead of the tick, so it can be felt eyes-free;
   - the beads and count flash ember;
   - the counter **advances immediately**: the very next tap counts for the next Dhikr. No pause, no dialog, no tap swallowed.
   - A **"NEXT"** card slides in over the top area for about 2 s: `NEXT · ٱلْحَمْدُ لِلَّٰهِ · Alhamdulillah · 0 / 33`.
   - Optional (Phase 4): play the next Dhikr's audio once as a spoken cue. Optional (Phase 7): a full Glyph flash on the back of the phone.
4. **Skipping**: Dhikrs already complete today are skipped.
5. **All complete**: a stronger cue (Confirm, twice) and a calm **"Day complete"** state with the day's total. If the user keeps tapping, Flow **loops into round 2**: it starts again at the first Dhikr, counting from its target up to 2× target, and the header shows "Round 2". Extra dhikr is never blocked.
6. **Undo** reverses the last increment *wherever it happened*. Undo straight after an auto-advance goes back to the previous Dhikr at `target − 1`, and Flow returns there.
7. **Manual override** still exists (swipe the top area, or the `2 / 4` picker), but it's never required.
8. **Setting**: "After target" → **Move to next dhikr** (default) · Stay on this dhikr (classic single counter).

### Counter layout changes

```
┌──────────────────────────────┐
│ ‹  FLOW · 2 / 4      ROUND 1 │  position in today's routine
│      ٱلْحَمْدُ لِلَّٰهِ            │
│      Alhamdulillah           │
│  ○○●○  ← routine dots        │  one dot per enabled dhikr: done ● · now ◉ · todo ○
│ ═══●●●●●●○○○○○○○○════════   │  slide bead string (current dhikr)
├──────────────────────────────┤
│        BEAD PAD (unchanged)  │
└──────────────────────────────┘
```

### Edge cases (must have tests)

- Target reached by the **widget or notification**: the next app open starts at the next incomplete Dhikr. The widget in "Follow flow" mode advances the same way (Phase 6).
- A Dhikr is **disabled or reordered mid-flow**: Flow recomputes "next" from the live order. The current Dhikr stays current until its target is reached.
- **Target edited mid-flow** to below the current count: that Dhikr becomes complete immediately, with no retroactive haptic. Flow advances on the next tap.
- **Midnight** during Flow: the next tap belongs to the new day, so Flow restarts at the first Dhikr, round 1.
- **Only one Dhikr enabled**: reaching the target shows "Day complete", then loops on the same Dhikr in rounds.
- **Rapid tapping across the boundary**: taps 33 and 34 arriving within ~50 ms must land on Dhikr A (33) and Dhikr B (1). The decision is made in the counter engine, serialised, never in the UI.
