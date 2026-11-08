package com.dhikr.app.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.dhikr.app.appContainer
import com.dhikr.app.core.model.CountContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Widget tap = +1, through the same CounterEngine as the app (no counter in the widget).
 * No haptics or sounds from the home screen (android-widget-engineering).
 */
class IncrementAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val container = context.appContainer
        val engine = container.engine
        // The tap may have cold-started the process: wait for today's counts, or we'd count on nothing.
        withTimeoutOrNull(5_000) { engine.awaitLoaded() } ?: return
        val pinned = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)[DhikrWidget.KEY_PINNED]
        if (pinned != null) {
            engine.incrementDhikr(CountContext.Daily, pinned)
        } else {
            engine.increment(CountContext.Daily, container.preferences.prefs.value.feel.afterTarget)
        }
        // Make the write durable before the system may stop the process, then redraw.
        withTimeoutOrNull(3_000) { engine.flush() }
        DhikrWidget().updateAll(context)
    }
}

/**
 * Keeps widgets in step when counts change elsewhere (in the app). Debounced so fast counting in
 * the app doesn't rebuild RemoteViews on every tap; does nothing when no widget is placed.
 */
object WidgetRefresher {
    @OptIn(FlowPreview::class)
    fun start(context: Context, scope: CoroutineScope) {
        val app = context.applicationContext
        val engine = app.appContainer.engine
        scope.launch {
            engine.state
                .map { s -> s.date to s.session(CountContext.Daily).let { it.counts to it.library } }
                .distinctUntilChanged()
                .drop(1)
                .debounce(600)
                .collect {
                    val ids = GlanceAppWidgetManager(app).getGlanceIds(DhikrWidget::class.java)
                    if (ids.isNotEmpty()) DhikrWidget().updateAll(app)
                }
        }
    }
}

