package com.dhikr.app

import android.app.Application
import android.content.Context
import android.util.Log
import com.dhikr.app.audio.AudioCuePlayer
import com.dhikr.app.audio.AudioStore
import com.dhikr.app.data.database.DhikrDatabase
import com.dhikr.app.data.progress.CounterEngine
import com.dhikr.app.data.progress.RoomProgressStore
import com.dhikr.app.data.preferences.PreferencesRepository
import com.dhikr.app.data.repository.DhikrRepository
import com.dhikr.app.haptics.HapticPlayer
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Clock

/**
 * Manual dependency container (D-008: no DI framework). One instance per process, so the app,
 * and later widgets and notification receivers, share the same database, DataStore and state.
 */
class AppContainer(context: Context) {
    /** Process-lifetime scope for repositories (not tied to any screen). Never GlobalScope. */
    val appScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default +
            CoroutineExceptionHandler { _, e -> Log.e("Dhikr", "Background work failed", e) },
    )
    val clock: Clock = Clock.systemDefaultZone()

    val database: DhikrDatabase by lazy { DhikrDatabase.build(context) }
    val dhikrRepository: DhikrRepository by lazy { DhikrRepository(database.dhikrDao(), database.prayerSetDao(), clock) }
    val preferences: PreferencesRepository = PreferencesRepository.create(context, appScope)
    val haptics = HapticPlayer(context)
    val audioStore = AudioStore(context.filesDir, context.contentResolver)
    val audio: AudioCuePlayer by lazy { AudioCuePlayer(context, audioStore) }

    /** The one counter engine for every surface (app now; widgets/notification later). */
    val engine: CounterEngine by lazy {
        CounterEngine(
            library = dhikrRepository.observeLibrary(),
            prayerSet = dhikrRepository.observePrayerSet(),
            store = RoomProgressStore(database.progressDao(), clock),
            clock = clock,
            scope = appScope,
        )
    }

    init {
        appScope.launch { dhikrRepository.ensureSeeded() }
        engine // start loading routines and today's counts
    }
}

class DhikrApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        container // start seeding / loading early
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as DhikrApplication).container
