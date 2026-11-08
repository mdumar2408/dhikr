package com.dhikr.app.widget

import com.dhikr.app.core.model.AfterTarget
import com.dhikr.app.core.model.CountContext
import com.dhikr.app.data.progress.EngineState
import com.dhikr.app.domain.flow.FlowPolicy

/**
 * What one widget counts (stored per widget instance). Every mode always says on the widget
 * which practice a tap goes to, so a count's destination is never ambiguous (D-040).
 */
sealed interface WidgetMode {
    /** Daily targets, moving through the routine like the counter's Flow. */
    data object Daily : WidgetMode

    /** Prayer tasbeeh for the next incomplete prayer (the one Today suggests). */
    data object Prayer : WidgetMode

    /** Always one dhikr, counted toward daily targets. */
    data class Pinned(val dhikrId: Long) : WidgetMode

    companion object {
        const val DAILY = "daily"
        const val PRAYER = "prayer"
        fun from(mode: String?, pinnedId: Long?): WidgetMode = when {
            pinnedId != null -> Pinned(pinnedId)
            mode == PRAYER -> Prayer
            else -> Daily
        }
    }
}

/** Everything a widget shows. Pure projection of engine state — unit-tested, no Glance types. */
data class WidgetModel(
    /** Where a tap goes: "DAILY" or the prayer name ("ASR"). Always shown on the widget. */
    val contextLabel: String,
    val title: String,
    val arabic: String,
    val count: Int,
    val target: Int,
    val complete: Boolean,
    /** "FLOW · 2/4", "ASR · 3/6" or "PINNED". */
    val modeLabel: String,
    val hasAudio: Boolean,
    /** Shown once a prayer's set is finished and the widget has moved on: "✓ ASR". */
    val previousDone: String? = null,
    /** Nothing to count: no routine/prayer set, or every prayer is done. */
    val empty: Boolean,
    /** Message for the empty state. */
    val emptyMessage: String = "Choose daily dhikr in the app",
    /** Taps should count (false → tapping opens the app). */
    val countable: Boolean = !empty,
) {
    val fraction: Float get() = if (target == 0) 0f else (count.toFloat() / target).coerceIn(0f, 1f)

    companion object {
        private fun empty(label: String, message: String) =
            WidgetModel(label, "Dhikr", "", 0, 0, false, "", false, empty = true, emptyMessage = message)

        fun project(state: EngineState, afterTarget: AfterTarget, mode: WidgetMode): WidgetModel = when (mode) {
            WidgetMode.Prayer -> projectPrayer(state)
            WidgetMode.Daily -> projectDaily(state, afterTarget, pinnedId = null)
            is WidgetMode.Pinned -> projectDaily(state, afterTarget, mode.dhikrId)
        }

        /** Same prayer as Today's card; the set runs in order with its own per-prayer counts. */
        private fun projectPrayer(state: EngineState): WidgetModel {
            val hasSet = CountContext.prayers.any { FlowPolicy.routine(state.session(it).library).isNotEmpty() }
            if (!hasSet) return empty("PRAYER", "Set up your after-prayer list in the app")
            val prayer = state.nextPrayer()
                ?: return empty("PRAYER", "All five prayers complete").copy(complete = true)
            val session = state.session(prayer)
            val set = FlowPolicy.routine(session.library)
            val dhikr = FlowPolicy.current(session, AfterTarget.NextDhikr) ?: return empty(prayer.label.uppercase(), "")
            val index = CountContext.prayers.indexOf(prayer)
            // Just moved on from a finished prayer and haven't started this one: confirm it.
            val previous = CountContext.prayers.getOrNull(index - 1)
                ?.takeIf { state.prayerComplete(it) && set.all { d -> session.countOf(d.id) == 0 } }
            return WidgetModel(
                contextLabel = prayer.label.uppercase(),
                title = dhikr.displayName,
                arabic = dhikr.arabic,
                count = session.countOf(dhikr.id),
                target = dhikr.target,
                complete = false,
                modeLabel = "${prayer.label} · ${set.indexOf(dhikr) + 1}/${set.size}",
                hasAudio = dhikr.audioRef != null,
                previousDone = previous?.let { "✓ ${it.label.uppercase()}" },
                empty = false,
            )
        }

        /**
         * [pinnedId] = null → follows the daily-targets Flow (same dhikr the app would show).
         * Otherwise shows that dhikr's daily count; if it's no longer in the routine, falls back to
         * Flow so a widget never breaks after a library edit (android-widget-engineering).
         */
        private fun projectDaily(state: EngineState, afterTarget: AfterTarget, pinnedId: Long?): WidgetModel {
            val daily = state.session(CountContext.Daily)
            val routine = FlowPolicy.routine(daily.library)
            if (routine.isEmpty()) return empty("DAILY", "Choose daily dhikr in the app")
            val pinned = pinnedId?.let { id -> routine.firstOrNull { it.id == id } }
            val dhikr = pinned ?: FlowPolicy.current(daily, afterTarget) ?: return empty("DAILY", "")
            val total = daily.countOf(dhikr.id)
            val flows = pinned == null && afterTarget == AfterTarget.NextDhikr
            val round = if (flows) FlowPolicy.round(routine, daily.counts) else 1
            val shown = if (flows) (total - dhikr.target * (round - 1)).coerceAtLeast(0) else total
            return WidgetModel(
                contextLabel = "DAILY",
                title = dhikr.displayName,
                arabic = dhikr.arabic,
                count = shown,
                target = dhikr.target,
                complete = !flows && total >= dhikr.target,
                modeLabel = if (pinned != null) "Pinned" else "Flow · ${routine.indexOf(dhikr) + 1}/${routine.size}",
                hasAudio = dhikr.audioRef != null,
                empty = false,
            )
        }
    }
}
