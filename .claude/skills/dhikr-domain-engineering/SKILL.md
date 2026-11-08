---
name: dhikr-domain-engineering
description: Use when modelling or changing Dhikr business rules — Dhikr library entities, enabled vs active Dhikr, per-Dhikr counters, daily targets, daily progress and summaries, completion, streaks (current/longest/total), midnight rollover, timezone changes, target changes mid-day, seed data, or Arabic text/transliteration/translation content.
---

# Dhikr domain engineering

## Core concepts (keep them distinct)

| Concept | Meaning | Stored where |
|---|---|---|
| **Library** | Every saved Dhikr, including disabled and soft-deleted ones | `DhikrEntity` (`deletedAt` nullable) |
| **Enabled** | Part of the user's daily practice | `DhikrEntity.isEnabled` |
| **Active** | Currently selected in the counter UI | DataStore `lastActiveDhikrId`, plus a per-widget selection |
| **Daily progress** | Count for one Dhikr on one local date, with the target *snapshotted* | `DailyDhikrProgressEntity(dhikrId, localDate, target, completedCount)`, unique on `(dhikrId, localDate)` |
| **Daily summary** | Derived totals for a date | `DailySummaryEntity` (recomputed in the same transaction as progress changes) |

## Rules (the policy — change only with a TECHNICAL_DECISIONS.md entry)

1. **Local date** = `LocalDate.now(clock.withZone(ZoneId.systemDefault()))` at the moment of the tap. Inject `java.time.Clock` everywhere. Never call `System.currentTimeMillis()` or `LocalDate.now()` without a clock.
2. **Target snapshot:** the first increment of a day creates the progress row with `target = dhikr.defaultTarget` *at that moment*. Editing `defaultTarget` later in the day updates **today's** row (the user's intent for today). It never rewrites past rows.
3. **Applicable Dhikrs for a day** = the Dhikrs that were enabled on that day. Record this by creating a progress row (count 0) for each enabled Dhikr when the day is first opened, *and* for any Dhikr enabled later that day. Disabling mid-day deletes today's row only if its count is 0. Otherwise it is kept and stays applicable.
4. **Day completed** ⇔ at least one applicable Dhikr exists AND every applicable row has `completedCount >= target`. No enabled Dhikrs → not completed (and not "missed" either: see rule 6).
5. Counting **past** the target is allowed and recorded. Progress percentages clamp at 100% per Dhikr, so one Dhikr can't make up for another.
6. **Streak:** walk back through local dates from today. Today counts if complete. If today is not complete *yet*, the streak continues from yesterday (today isn't "missed" until it's over). The streak breaks on the first past date that is not completed. Days with no applicable Dhikrs **break** the streak (spec: "not automatically considered completed").
7. **Longest streak / total completed days** are computed from `DailySummaryEntity.isCompleted` history, **never** from the current enabled list.
8. **Timezone change:** dates already written stay as written. New taps use the new zone. A user flying east may "lose" or "repeat" a date. That's accepted and documented, never "fixed" by rewriting rows.
9. **Midnight rollover while the counter is open:** the ViewModel observes a `currentDate: Flow<LocalDate>` (ticker aligned to the next local midnight, plus `ACTION_DATE_CHANGED`/`ACTION_TIMEZONE_CHANGED` broadcasts). The next tap after midnight goes to the new date's row. Never carry yesterday's in-memory count over.

## Flow (continuous counting) — D-015

10. **Routine order** = enabled, non-deleted Dhikrs sorted by `sortOrder` (ties broken by `id`).
11. **Current Dhikr** = the first in routine order whose today count is below `target × round`. Round 1 means "not yet at target". When every Dhikr is complete, round = 2 and so on.
12. `IncrementInFlowUseCase` runs in one serialised step (Mutex in the engine plus a DB transaction): resolve current → increment it → if this tap made `count == target × round`, emit `FlowEvent.TargetReached(dhikr, next)` or `FlowEvent.DayComplete`. The UI reacts to events (haptic, NEXT card). **It never decides the next Dhikr itself.**
13. Undo pops the last increment from an in-memory stack of `(dhikrId, date)` entries for the session, which is also reconstructable from the DB. The current Dhikr is then recomputed by rule 11, so undo across a boundary returns to the previous Dhikr naturally.
14. Mid-flow changes (reorder, disable, target edit) never alter past increments. "Current" is simply recomputed on the next tap.
15. Setting `afterTarget = STAY` bypasses rule 11: the selected Dhikr stays current regardless of its count.

## Pure-Kotlin domain layer

- `domain/` has **no Android imports**: `StreakCalculator`, `DailyProgressCalculator`, `DayCompletionPolicy` take plain data in and give plain data out, so they're trivially JVM-testable.
- Use cases (`IncrementDhikrUseCase`, `UndoIncrementUseCase`, `ResetDhikrUseCase`, `ToggleEnabledUseCase`, `UpdateTargetUseCase`) orchestrate repositories and the clock. They're the only path to mutate counts, and the widget, notification and app all call the same ones.

## Arabic content and seed data

- Store Arabic as NFC-normalised UTF-8 **with diacritics (harakat)** as authored. Search uses a separate normalised key: strip harakat (U+064B–U+065F, U+0670), unify alef forms (أ إ آ ٱ → ا), ta marbuta → ha, lowercase the transliteration.
- Seed entries must come from a verifiable source, with the reference recorded in `description` (e.g. "Sahih Muslim 2691"). Never invent hadith references. If unsure, leave the reference empty and flag it for the user to verify.
- Seed Dhikrs are rows with `isSystemProvided = true`. They're editable, but deleting one soft-deletes it, and re-seeding must never resurrect a user-deleted seed (key seeds by a stable `seedKey`).
- Default targets: 33 for tasbih/tahmid/takbir sets, 100 for others, unless the source says otherwise.

## Common mistakes

- Calculating today's overall progress from `dhikrDao.enabled()` × `defaultTarget` instead of today's snapshotted rows.
- Using `Instant` epoch-day math (`epochMillis / 86_400_000`), which is UTC dates and breaks for every timezone except UTC.
- Resetting counts at midnight with an alarm or WorkManager job. There's nothing to reset: a new date simply means a new row.
- Treating "no Dhikr enabled" as a completed day.
- Letting the widget compute progress itself. It must read the same use cases and DAOs.

## Testing (required for any change here)

- `StreakCalculatorTest`: empty history; a single completed today; today incomplete with yesterday completed (streak = yesterday's); a gap day; a day with no applicable Dhikrs; longest-streak in the middle of history; a DST transition date.
- `DayCompletionPolicyTest`: target changed mid-day (today's row updated, yesterday's untouched); a Dhikr enabled after completion makes the day incomplete again; a Dhikr disabled with count > 0 stays applicable.
- Rollover test with a fake `Clock` crossing 23:59:59.999 → 00:00. The next increment lands on the new date.
- Timezone test: the same `Instant` in Asia/Kolkata vs America/Los_Angeles maps to different local dates.
- `FlowTest`: A(33) → B(33) → C(33). Tap 33 completes A, and tap 34 lands on B. Already-complete Dhikrs are skipped. All complete → `DayComplete`, then round 2 starts at A from 33. Undo after an advance returns to A at 32. Disabling B mid-flow skips it. A single Dhikr loops in rounds. 200 concurrent increments straddling a boundary give exact totals. A midnight crossing restarts at A, round 1.
