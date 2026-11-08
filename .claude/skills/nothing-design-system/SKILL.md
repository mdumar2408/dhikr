---
name: nothing-design-system
description: Use when building or reviewing any Dhikr UI — screens, components, colours, typography, spacing, motion, the dot-matrix renderer (DotMatrixFont/Renderer/Text/Bitmap), the bead row/ring/pad, or anything visual in a widget. Covers the monochrome dot-matrix language and how to stay Nothing-inspired without copying Nothing assets.
---

# Dhikr design system (Nothing-inspired, original)

The source of truth for layouts is `docs/DESIGN_SPEC.md`. This skill says **how to implement** it in Compose and keep it consistent.

## Non-negotiables

1. **No proprietary assets.** Never bundle or reference Nothing's fonts (NDot, NType and so on), logos, wallpapers, sounds or the Glyph iconography. The dot-matrix font is our own 5×7 design in `DotMatrixFont.kt`. Don't name things "Nothing" in user-facing text.
2. **Tokens only.** Screens and components read colours, type and spacing from `DhikrTheme` (`design/theme/`). No hex literals or raw `dp` values in feature code. The only exceptions are inside `design/` itself.
3. **Monochrome first.** `ember` (the accent) is reserved for *completion*: a target reached or a day complete. Never use it for buttons, links, selection or errors.
4. **Dot-matrix is for numbers and short labels.** Counts, totals, screen titles of 1–2 words. Readable text (meaning, references, settings) uses normal fonts.
5. **One primary action per screen.** On Counter that's the bead pad. Don't add competing buttons to the pad area beyond undo and audio.

## Token structure (`design/theme/`)

```kotlin
@Immutable data class DhikrColors(val canvas: Color, val surface: Color, val surfaceRaised: Color,
    val hairline: Color, val ink: Color, val inkMuted: Color, val inkFaint: Color,
    val dotOff: Color, val ember: Color)
val LocalDhikrColors = staticCompositionLocalOf<DhikrColors> { error("No DhikrTheme") }
object DhikrTheme { val colors @Composable get() = LocalDhikrColors.current /* + type, spacing */ }
```

- Provide tokens through `CompositionLocal`s in `DhikrTheme { }`. Also map them onto a Material 3 `ColorScheme`, so stock M3 components (dialogs, text fields, snackbars) don't show purple defaults.
- Spacing: `xxs 4 · xs 8 · s 12 · m 16 · l 24 · xl 32 · xxl 48 · xxxl 64` dp. Gutter 24 dp. Minimum touch target 48 dp.
- Motion: `fast 90 · base 180 · slow 320` ms. Read `Settings.Global.ANIMATOR_DURATION_SCALE`, or rely on Compose's built-in scaling, and never force animations when it's 0.

## Dot-matrix engine (`design/dotmatrix/`)

| File | Responsibility |
|---|---|
| `DotMatrixFont` | Pure-Kotlin glyph table: `Char → BooleanArray(5*7)`. Digits, A–Z, a few symbols (`/ : - . ✓`). **No Android imports**, so it's unit-testable on the JVM |
| `DotMatrixRenderer` | Converts content into a `DotGrid(cols, rows, lit: BitSet)`. Two paths: (a) glyph path for font text; (b) **raster path** for anything else, especially Arabic: lay out with `android.text.StaticLayout` + `TextPaint` (so HarfBuzz shapes and joins the letters), draw to an `ALPHA_8` bitmap, then sample each cell (a cell is lit if average coverage ≥ threshold, default 0.45) |
| `DotMatrixText` | Compose `Canvas` that draws a `DotGrid` with `drawCircle`. Animates per-dot alpha between grids (on 80 ms, off 120 ms) |
| `DotMatrixBitmap` | Renders a `DotGrid` to an `ARGB_8888` `Bitmap` for Glance widgets and notifications. Shares drawing math with `DotMatrixText`, never duplicates it |

Rules:
- **Arabic must go through the raster path.** Never map Arabic code points to per-letter glyphs: that breaks joining and contextual forms. Set `TextDirection`/`Layout.Alignment` for RTL.
- Dot pitch = 1.5 × dot diameter. Unlit dots are drawn in `dotOff`, never left empty: the visible grid is what makes the look.
- Cache grids by `(text, font, cols, rows)`. Rasterising Arabic is too expensive per frame. Compute in `remember(key)` or off the main thread for widgets.
- Always add `Modifier.semantics { contentDescription = plainText }` to dot-matrix composables.

## Components (`design/components/`)

- `BeadRow(progress: Float, beads: Int = 33, complete: Boolean)` — lit beads = `floor(progress * beads)`, all ember when complete.
- `BeadRing` — same, laid out on a circle (Today screen overall progress).
- `BeadPad(onTap, enabled)` — the large tap slab: `surface` → `surfaceRaised` on press, 0.97 content scale, no ripple (`indication = null`, and provide our own press state).
- `DotNavBar` — monospace caps labels, one lit dot above the active one.
- `HairlineRow` — list row separated by a 0.5 dp `hairline` divider, not a Card.

Follow `compose-component-design` for API shape: `modifier` as the first optional parameter, slots for content, no hard-coded sizes that callers can't override.

## Common mistakes

- Using `MaterialTheme.colorScheme.primary` directly in feature code. Use `DhikrTheme.colors`.
- Ripple effects or elevation shadows. The look is flat and pressed, not floating.
- Rendering the dot-matrix count with a `Text` in a dotted font file. We draw dots ourselves, so the app and widgets match exactly.
- Recomputing the Arabic raster in composition on every recomposition.
- Letting the counter overflow at large font scales. Scale the dot size down to fit the available width.

## Testing

- JVM unit tests: every glyph in `DotMatrixFont` is exactly 35 cells, and digits 0–9 are pairwise distinct. `DotGrid` composition for a multi-character string has the correct width (`chars*5 + (chars-1)*gap`).
- Instrumented test: render "سُبْحَانَ ٱللَّٰهِ" through the raster path and assert the grid is non-empty and wider than tall. On the device, visually check that the joins survive (attach a screenshot to the PR or report).
- Compose previews for every component in dark and light.
