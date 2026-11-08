package com.dhikr.app.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.updateAll
import com.dhikr.app.MainActivity
import com.dhikr.app.R
import com.dhikr.app.appContainer
import com.dhikr.app.core.model.AfterTarget
import com.dhikr.app.core.model.CountContext
import com.dhikr.app.core.model.LockScreenMode
import com.dhikr.app.core.model.UserPreferences
import com.dhikr.app.data.progress.CounterEngine
import com.dhikr.app.data.progress.EngineState
import com.dhikr.app.domain.flow.FlowEvent
import com.dhikr.app.widget.DhikrWidget
import com.dhikr.app.widget.WidgetMode
import com.dhikr.app.widget.WidgetModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** The text a lock-screen notification shows. Pure (no Android types) → unit-tested. */
data class LockScreenContent(
    val title: String,
    val text: String,
    val count: Int,
    val target: Int,
    val countable: Boolean,
) {
    companion object {
        fun from(m: WidgetModel): LockScreenContent = when {
            m.empty -> LockScreenContent(m.contextLabel.lowercase().replaceFirstChar { it.uppercase() }, m.emptyMessage, 0, 0, countable = false)
            else -> LockScreenContent(
                // Where taps go, first — "Asr · SubhanAllah" / "Daily · SubhanAllah" (D-040).
                title = "${m.contextLabel.lowercase().replaceFirstChar { it.uppercase() }} · ${m.title}",
                text = buildString {
                    append("${m.count} / ${m.target}")
                    if (m.previousDone != null) append("  ·  ${m.previousDone.lowercase().replaceFirstChar { it.uppercase() }} done")
                    else if (m.modeLabel.isNotBlank()) append("  ·  ${m.modeLabel}")
                },
                count = m.count,
                target = m.target,
                countable = m.countable,
            )
        }

        fun modeOf(s: LockScreenMode): WidgetMode = if (s == LockScreenMode.Prayer) WidgetMode.Prayer else WidgetMode.Daily
    }
}

/**
 * Lock-screen counting (D-203, D-041, D-044): an ongoing, silent notification. Tapping it opens
 * [LockScreenCounterActivity] over the lock screen; its "+1" / "Undo" actions need the PIN on
 * Nothing OS (D-043) but are handy after unlock. It counts through the same CounterEngine as the app and
 * widget, so every surface agrees.
 */
object LockScreenCounter {
    /**
     * v2: IMPORTANCE_DEFAULT with no sound/vibration. v1 ("counter") was IMPORTANCE_LOW, which
     * Android files under "silent" — hidden from the lock screen when the user's "show silent
     * notifications" is off (seen on the Phone (2a)). Channel importance can't be raised in place,
     * so v1 is deleted and v2 created.
     */
    private const val CHANNEL = "lockscreen_counter_v2"
    private const val OLD_CHANNEL = "counter"
    private const val NOTIFICATION_ID = 1001
    const val ACTION_INCREMENT = "com.dhikr.app.action.INCREMENT"
    const val ACTION_UNDO = "com.dhikr.app.action.UNDO"
    const val ACTION_DISMISSED = "com.dhikr.app.action.DISMISSED"

    /** Swiped away by the user (Android 14+ allows it): stay hidden until the app is opened again. */
    @Volatile var dismissed = false

    /** The context of the last lock-screen count, so Undo targets the right prayer even after a switch. */
    @Volatile private var lastContext: CountContext? = null

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /**
     * Whether the phone shows any notifications on the lock screen (a system-wide user choice).
     * Null when the value can't be read — then we simply don't warn.
     */
    fun systemShowsLockScreenNotifications(context: Context): Boolean? = runCatching {
        android.provider.Settings.Secure.getInt(context.contentResolver, "lock_screen_show_notifications") != 0
    }.getOrNull()

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.deleteNotificationChannel(OLD_CHANNEL)
        val channel = NotificationChannel(CHANNEL, "Lock-screen counter", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Count dhikr from the lock screen without unlocking"
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(channel)
    }

    /** Posts or removes the notification to match the current state. Safe to call often. */
    fun refresh(context: Context, state: EngineState, prefs: UserPreferences) {
        val nm = NotificationManagerCompat.from(context)
        if (!prefs.lockScreen.enabled || !canPost(context)) {
            nm.cancel(NOTIFICATION_ID)
            return
        }
        if (dismissed || !state.loaded) return
        val model = WidgetModel.project(state, prefs.feel.afterTarget, LockScreenContent.modeOf(prefs.lockScreen.mode))
        try {
            nm.notify(NOTIFICATION_ID, build(context, LockScreenContent.from(model)))
        } catch (_: SecurityException) {
            // Permission revoked between the check and notify — nothing to show.
        }
    }

    private fun build(context: Context, c: LockScreenContent): android.app.Notification {
        ensureChannel(context)
        // Tapping the notification opens the show-when-locked counter (D-044) — no unlock needed.
        val open = counterScreenIntent(context)
        val b = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_dhikr)
            .setContentTitle(c.title)
            .setContentText(c.text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // pre-O equivalent of the channel importance
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            // Dhikr names aren't sensitive: show the full counter on the lock screen.
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(open)
            .setDeleteIntent(broadcast(context, ACTION_DISMISSED, 3))
        if (c.countable && c.target > 0) {
            b.setProgress(c.target, c.count.coerceAtMost(c.target), false)
            b.addAction(action(context, "+1", ACTION_INCREMENT, 1))
            b.addAction(action(context, "Undo", ACTION_UNDO, 2))
        }
        return b.build()
    }

    fun counterScreenIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 10,
        Intent(context, LockScreenCounterActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun action(context: Context, title: String, act: String, code: Int) =
        NotificationCompat.Action.Builder(R.drawable.ic_stat_dhikr, title, broadcast(context, act, code))
            // Count without unlocking (API 31+ honours this; earlier versions never required it).
            .setAuthenticationRequired(false)
            .build()

    private fun broadcast(context: Context, act: String, code: Int) = PendingIntent.getBroadcast(
        context, code, Intent(context, CounterActionReceiver::class.java).setAction(act),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Counts one tap from the lock screen. Returns the event so the caller can give feedback. */
    fun count(engine: CounterEngine, prefs: UserPreferences): FlowEvent? {
        val ctx = when (prefs.lockScreen.mode) {
            LockScreenMode.Prayer -> engine.state.value.nextPrayer() ?: return null
            LockScreenMode.Daily -> CountContext.Daily
        }
        lastContext = ctx
        return engine.increment(ctx, if (ctx.isPrayer) AfterTarget.NextDhikr else prefs.feel.afterTarget)
    }

    fun undo(engine: CounterEngine) {
        lastContext?.let(engine::undo)
    }

    /** Keeps the notification in step with every count (app, widget, lock screen) and settings. */
    @OptIn(FlowPreview::class)
    fun startRefresher(context: Context, scope: CoroutineScope) {
        val app = context.applicationContext
        val c = app.appContainer
        scope.launch {
            combine(c.engine.state, c.preferences.prefs) { s, p -> s to p }
                .distinctUntilChanged()
                .debounce(120)
                .collect { (s, p) -> refresh(app, s, p) }
        }
    }
}

/** Handles the notification's buttons. Not exported: only our own PendingIntents can reach it. */
class CounterActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync() // DB work happens off the main thread, within the receiver's time limit
        val app = context.applicationContext
        val container = app.appContainer
        container.appScope.launch {
            try {
                when (intent.action) {
                    LockScreenCounter.ACTION_DISMISSED -> LockScreenCounter.dismissed = true
                    LockScreenCounter.ACTION_INCREMENT, LockScreenCounter.ACTION_UNDO -> {
                        withTimeoutOrNull(5_000) { container.engine.awaitLoaded() } ?: return@launch
                        val prefs = container.preferences.prefs.value
                        if (intent.action == LockScreenCounter.ACTION_INCREMENT) {
                            val event = LockScreenCounter.count(container.engine, prefs)
                            // Felt eyes-free: the target cue when this tap finished a dhikr (no per-tap buzz).
                            if (event is FlowEvent.TargetReached || event is FlowEvent.RoutineComplete) {
                                container.haptics.playTargetWithoutView(prefs.feel.targetHaptic)
                            }
                        } else {
                            LockScreenCounter.undo(container.engine)
                        }
                        withTimeoutOrNull(3_000) { container.engine.flush() }
                        LockScreenCounter.refresh(app, container.engine.state.value, prefs)
                        DhikrWidget().updateAll(app)
                    }
                }
            } finally {
                result.finish()
            }
        }
    }
}

/** After a reboot or an app update, starting the process is enough: the refresher re-posts. */
class RestoreCounterReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Exported for system broadcasts only: ignore anything else.
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        context.applicationContext.appContainer // initialises the engine; the refresher does the rest
    }
}
