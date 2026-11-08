package com.dhikr.app.widget

import com.dhikr.app.core.model.AfterTarget
import com.dhikr.app.core.model.CountContext
import com.dhikr.app.data.progress.EngineState
import com.dhikr.app.domain.flow.FlowPolicy

/** Everything a widget shows. Pure projection of engine state — unit-tested, no Glance types. */
data class WidgetModel(
    val title: String,
    val arabic: String,
    val count: Int,
    val target: Int,
    val complete: Boolean,
    /** "FLOW · 2/4" or "PINNED". */
    val modeLabel: String,
    val hasAudio: Boolean,
    /** Nothing to count: no daily routine, or the pinned dhikr was disabled/deleted. */
    val empty: Boolean,
) {
    val fraction: Float get() = if (target == 0) 0f else (count.toFloat() / target).coerceIn(0f, 1f)

    companion object {
        val Empty = WidgetModel("Dhikr", "", 0, 0, false, "", false, empty = true)

        /**
         * [pinnedId] = null → follows the daily-targets Flow (same dhikr the app would show).
         * Otherwise shows that dhikr's daily count; if it's no longer in the routine, falls back to
         * Flow so a widget never breaks after a library edit (android-widget-engineering).
         */
        fun project(state: EngineState, afterTarget: AfterTarget, pinnedId: Long?): WidgetModel {
            val daily = state.session(CountContext.Daily)
            val routine = FlowPolicy.routine(daily.library)
            if (routine.isEmpty()) return Empty
            val pinned = pinnedId?.let { id -> routine.firstOrNull { it.id == id } }
            val dhikr = pinned ?: FlowPolicy.current(daily, afterTarget) ?: return Empty
            val total = daily.countOf(dhikr.id)
            val flows = pinned == null && afterTarget == AfterTarget.NextDhikr
            val round = if (flows) FlowPolicy.round(routine, daily.counts) else 1
            val shown = if (flows) (total - dhikr.target * (round - 1)).coerceAtLeast(0) else total
            return WidgetModel(
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
