package com.dhikr.app.notification

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.glance.appwidget.updateAll
import com.dhikr.app.R
import com.dhikr.app.appContainer
import com.dhikr.app.domain.flow.FlowEvent
import com.dhikr.app.widget.DhikrWidget
import com.dhikr.app.widget.WidgetModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Quick Settings tile counter (D-043): pull down the shade and tap to count. Nothing OS doesn't
 * open Quick Settings while locked, so this mainly helps after unlock; if it is clicked while
 * locked, it opens the over-the-lock-screen counter (D-044) instead.
 * Uses the same mode setting and CounterEngine as the lock-screen notification.
 */
class CounterTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        render()
    }

    override fun onClick() {
        super.onClick()
        if (isLocked) {
            // Counting in place needs the PIN on Nothing OS (D-043); open the over-the-lock-screen counter instead.
            if (Build.VERSION.SDK_INT >= 34) {
                startActivityAndCollapse(LockScreenCounter.counterScreenIntent(this))
            } else {
                @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
                startActivityAndCollapse(
                    android.content.Intent(this, LockScreenCounterActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
            return
        }
        val c = applicationContext.appContainer
        c.appScope.launch {
            withTimeoutOrNull(5_000) { c.engine.awaitLoaded() } ?: return@launch
            val prefs = c.preferences.prefs.value
            val event = LockScreenCounter.count(c.engine, prefs)
            if (event is FlowEvent.TargetReached || event is FlowEvent.RoutineComplete) {
                c.haptics.playTargetWithoutView(prefs.feel.targetHaptic)
            }
            withTimeoutOrNull(3_000) { c.engine.flush() }
            render()
            LockScreenCounter.refresh(applicationContext, c.engine.state.value, prefs)
            DhikrWidget().updateAll(applicationContext)
        }
    }

    /** Label = where the tap goes and the count, e.g. "Asr +1" / "Alhamdulillah 8/33". */
    private fun render() {
        val tile = qsTile ?: return
        val c = applicationContext.appContainer
        val prefs = c.preferences.prefs.value
        val model = WidgetModel.project(c.engine.state.value, prefs.feel.afterTarget, LockScreenContent.modeOf(prefs.lockScreen.mode))
        val label = model.contextLabel.lowercase().replaceFirstChar { it.uppercase() }
        if (model.empty || !model.countable) {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "Dhikr"
            if (Build.VERSION.SDK_INT >= 29) tile.subtitle = model.emptyMessage.take(30)
        } else {
            tile.state = Tile.STATE_ACTIVE
            tile.label = "$label +1"
            if (Build.VERSION.SDK_INT >= 29) tile.subtitle = "${model.title} ${model.count}/${model.target}"
        }
        tile.contentDescription = "${tile.label}, ${if (Build.VERSION.SDK_INT >= 29) tile.subtitle else ""}"
        tile.icon = Icon.createWithResource(this, R.drawable.ic_stat_dhikr)
        tile.updateTile()
    }
}
