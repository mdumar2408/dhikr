package com.dhikr.app.domain.history

import com.dhikr.app.core.model.CountContext
import com.dhikr.app.domain.flow.FlowPolicy
import com.dhikr.app.domain.flow.FlowState
import java.time.LocalDate

/** What one day achieved. Persisted as daily_summary; never recomputed for past days. */
data class DaySummary(
    val date: LocalDate,
    /** Bit i = CountContext.prayers[i] complete. */
    val prayersMask: Int,
    val totalTarget: Int,
    val totalCompleted: Int,
    val completedDhikrCount: Int,
    val isCompleted: Boolean,
) {
    val prayersCompleted: Int get() = Integer.bitCount(prayersMask)
    fun prayerComplete(ctx: CountContext): Boolean = prayersMask and (1 shl CountContext.prayers.indexOf(ctx)) != 0

    /** Something was counted — a "partial" day on the calendar if not completed. */
    val hasActivity: Boolean get() = prayersMask != 0 || totalCompleted > 0
}

/**
 * The streak-day rule (D-023, refined in D-032):
 *  - a prayer session is complete when its set is non-empty and every item reached its count;
 *  - the day is complete when it has *something* to do, all five prayers are complete (if a
 *    prayer set exists), and every enabled daily target is complete (if any exist).
 * Daily progress is capped per dhikr, so one dhikr can't make up for another.
 */
object DaySummaryPolicy {

    fun summarize(date: LocalDate, sessions: Map<CountContext, FlowState>): DaySummary {
        var mask = 0
        var hasPrayerSet = false
        CountContext.prayers.forEachIndexed { i, ctx ->
            val s = sessions[ctx] ?: return@forEachIndexed
            val set = FlowPolicy.routine(s.library)
            if (set.isNotEmpty()) hasPrayerSet = true
            if (set.isNotEmpty() && set.all { s.countOf(it.id) >= it.target }) mask = mask or (1 shl i)
        }
        val daily = sessions[CountContext.Daily]
        val routine = daily?.let { FlowPolicy.routine(it.library) }.orEmpty()
        val totalTarget = routine.sumOf { it.target }
        val totalCompleted = routine.sumOf { minOf(daily!!.countOf(it.id), it.target) }
        val completedDhikr = routine.count { daily!!.countOf(it.id) >= it.target }

        val prayersOk = !hasPrayerSet || Integer.bitCount(mask) == CountContext.prayers.size
        val dailyOk = routine.isEmpty() || completedDhikr == routine.size
        val applicable = hasPrayerSet || routine.isNotEmpty()
        return DaySummary(date, mask, totalTarget, totalCompleted, completedDhikr, applicable && prayersOk && dailyOk)
    }
}

data class Streaks(val current: Int, val longest: Int, val totalCompletedDays: Int)

/**
 * Streaks from completed dates (dhikr-domain skill rule 6):
 *  - today counts if complete; if today isn't complete *yet*, the streak continues from yesterday;
 *  - any past date that isn't complete (including a day the app wasn't opened) breaks it.
 * Pure date arithmetic on LocalDate, so DST and timezone changes can't create or lose days.
 */
object StreakCalculator {

    fun compute(completed: Set<LocalDate>, today: LocalDate): Streaks {
        var current = 0
        var day = if (today in completed) today else today.minusDays(1)
        while (day in completed) {
            current++
            day = day.minusDays(1)
        }
        var longest = 0
        var run = 0
        var previous: LocalDate? = null
        for (d in completed.filter { !it.isAfter(today) }.sorted()) {
            run = if (previous != null && d == previous.plusDays(1)) run + 1 else 1
            longest = maxOf(longest, run)
            previous = d
        }
        return Streaks(current, maxOf(longest, current), completed.count { !it.isAfter(today) })
    }
}
