package com.dhikr.app.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhikr.app.appContainer
import com.dhikr.app.design.components.Hairline
import com.dhikr.app.design.components.MonoLabel
import com.dhikr.app.design.theme.DhikrTheme
import kotlinx.coroutines.launch

/**
 * Chooses what one widget shows: follow Flow (default) or a pinned dhikr. Opened by the launcher
 * when reconfiguring; adding a widget doesn't require it (configuration_optional).
 */
class DhikrWidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // If the user backs out, the launcher must not keep a half-configured widget.
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        enableEdgeToEdge()
        val container = appContainer
        setContent {
            val prefs by container.preferences.prefs.collectAsStateWithLifecycle()
            val engineState by container.engine.state.collectAsStateWithLifecycle()
            val scope = rememberCoroutineScope()
            val routine = engineState.session(com.dhikr.app.core.model.CountContext.Daily).library.filter { it.isEnabled }
                .sortedBy { it.sortOrder }

            fun choose(pinnedId: Long?) = scope.launch {
                val glanceId = GlanceAppWidgetManager(this@DhikrWidgetConfigActivity).getGlanceIdBy(appWidgetId)
                updateAppWidgetState(this@DhikrWidgetConfigActivity, glanceId) { p ->
                    if (pinnedId == null) p.remove(DhikrWidget.KEY_PINNED) else p[DhikrWidget.KEY_PINNED] = pinnedId
                }
                DhikrWidget().update(this@DhikrWidgetConfigActivity, glanceId)
                setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
                finish()
            }

            DhikrTheme(prefs.themeMode) {
                val c = DhikrTheme.colors
                LazyColumn(Modifier.fillMaxSize().background(c.canvas).safeDrawingPadding().padding(horizontal = 24.dp())) {
                    item {
                        Spacer(Modifier.height(24.dp()))
                        MonoLabel("Widget shows", color = c.ink)
                        Spacer(Modifier.height(16.dp()))
                        Hairline()
                        Column(Modifier.fillMaxWidth().clickable { choose(null) }.padding(vertical = 16.dp())) {
                            Text("Follow Flow", color = c.ink, style = DhikrTheme.type.body)
                            Text("Moves through your daily targets, like the counter", color = c.inkMuted, style = DhikrTheme.type.bodySmall)
                        }
                        Spacer(Modifier.height(16.dp()))
                        MonoLabel("Or always one dhikr")
                    }
                    items(routine, key = { it.id }) { d ->
                        Hairline()
                        Column(Modifier.fillMaxWidth().clickable { choose(d.id) }.padding(vertical = 14.dp())) {
                            Text(d.displayName, color = c.ink, style = DhikrTheme.type.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(d.arabic, color = c.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                style = DhikrTheme.type.arabicSmall.copy(textDirection = TextDirection.Rtl))
                        }
                    }
                    item { Hairline(); Spacer(Modifier.height(32.dp())) }
                }
            }
        }
    }
}

private fun Int.dp() = androidx.compose.ui.unit.Dp(this.toFloat())
