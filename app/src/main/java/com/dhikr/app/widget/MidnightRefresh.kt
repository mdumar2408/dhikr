package com.dhikr.app.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.dhikr.app.appContainer
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Rolls widgets over to the new day shortly after local midnight (counts visibly return to 0).
 * One-time work that re-schedules itself — no periodic polling, no exact alarms.
 */
class MidnightRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext.appContainer
        c.engine.refreshDate()
        DhikrWidget().updateAll(applicationContext)
        // The lock-screen refresher reacts to the date change too; this makes sure it runs now.
        com.dhikr.app.notification.LockScreenCounter.refresh(applicationContext, c.engine.state.value, c.preferences.prefs.value)
        MidnightRefresh.schedule(applicationContext)
        return Result.success()
    }
}

object MidnightRefresh {
    private const val NAME = "dhikr-midnight-refresh"

    /** Delay until 00:01 tomorrow in the device's current zone. Pure; unit-tested. */
    fun delayUntilNextMidnight(now: ZonedDateTime): Duration {
        val next = LocalDate.from(now).plusDays(1).atStartOfDay(now.zone).plusMinutes(1)
        return Duration.between(now, next)
    }

    fun schedule(context: Context) {
        val delay = delayUntilNextMidnight(ZonedDateTime.now())
        val request = OneTimeWorkRequestBuilder<MidnightRefreshWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(NAME)
    }
}
