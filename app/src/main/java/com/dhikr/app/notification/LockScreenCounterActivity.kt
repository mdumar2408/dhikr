package com.dhikr.app.notification

import com.dhikr.app.design.components.IconAction
import com.dhikr.app.design.components.DhikrIcons
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhikr.app.appContainer
import com.dhikr.app.design.components.BeadPad
import com.dhikr.app.design.components.MonoLabel
import com.dhikr.app.design.dotmatrix.DotGrid
import com.dhikr.app.design.dotmatrix.DotMatrixText
import com.dhikr.app.design.theme.DhikrTheme
import com.dhikr.app.widget.WidgetModel

/**
 * Minimal counter shown **over the lock screen** (D-044). Notification buttons and Quick Settings
 * tiles ask for the PIN on Nothing OS 4.1 (D-043); an activity marked show-when-locked can be
 * opened from the notification without unlocking — the platform mechanism alarm and phone apps use.
 *
 * Deliberately limited: it shows only the current practice, dhikr and count (already public on the
 * notification) and offers no way into the rest of the app, so nothing private is exposed.
 */
class LockScreenCounterActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        // Counting sessions shouldn't be cut off by the screen timeout.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        val c = appContainer
        setContent {
            val prefs by c.preferences.prefs.collectAsStateWithLifecycle()
            val state by c.engine.state.collectAsStateWithLifecycle()
            val model = WidgetModel.project(state, prefs.feel.afterTarget, LockScreenContent.modeOf(prefs.lockScreen.mode))
            val view = LocalView.current
            DhikrTheme(prefs.themeMode) {
                val col = DhikrTheme.colors
                Column(
                    Modifier.fillMaxSize().background(col.canvas).safeDrawingPadding().padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Where taps go — always first (D-040).
                        MonoLabel(model.previousDone?.let { "$it → ${model.contextLabel}" } ?: model.contextLabel,
                            color = if (model.previousDone != null) col.ember else col.ink)
                        Spacer(Modifier.weight(1f))
                        IconAction(DhikrIcons.Check, "Done", { finish() })
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(model.arabic, color = col.ink, maxLines = 2,
                        style = DhikrTheme.type.arabicSmall.copy(textDirection = TextDirection.Rtl, textAlign = TextAlign.Center))
                    Text(model.title, color = col.inkMuted, style = DhikrTheme.type.body, maxLines = 1)
                    MonoLabel(model.modeLabel)
                    BeadPad(
                        onTap = {
                            if (model.countable) {
                                val event = LockScreenCounter.count(c.engine, prefs)
                                event?.let {
                                    c.haptics.onFlowEvent(view, it, prefs.feel.tapHaptic, prefs.feel.targetHaptic)
                                    c.audio.onFlowEvent(it, prefs.sound)
                                }
                            }
                        },
                        accessibilityLabel = "${model.contextLabel}: count ${model.title}, ${model.count} of ${model.target}",
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 16.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            DotMatrixText(
                                grid = remember(model.count) { DotGrid.text(model.count.toString()) },
                                contentDescription = model.count.toString(),
                                litColor = if (model.complete) col.ember else col.ink,
                                maxPitch = 15.dp,
                                modifier = Modifier.width(260.dp).height(100.dp),
                            )
                            Spacer(Modifier.height(10.dp))
                            MonoLabel(if (model.empty) model.emptyMessage else "/ ${model.target}",
                                color = if (model.complete) col.ember else col.inkMuted)
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        IconAction(DhikrIcons.Undo, "Undo", { LockScreenCounter.undo(c.engine) }, tint = col.inkMuted)
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        appContainer.audio.stop()
    }
}
