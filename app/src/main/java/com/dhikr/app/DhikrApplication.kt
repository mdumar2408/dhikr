package com.dhikr.app

import android.app.Application
import android.content.Context
import com.dhikr.app.data.CounterStore
import com.dhikr.app.data.PreferencesStore
import com.dhikr.app.data.sample.SampleLibrary
import com.dhikr.app.haptics.HapticPlayer

/**
 * Manual dependency container (D-008: no DI framework yet). One instance per process,
 * so the app, and later widgets and notification receivers, share the same state.
 */
class AppContainer(context: Context) {
    val counterStore = CounterStore(SampleLibrary.dhikrs)
    val preferencesStore = PreferencesStore()
    val haptics = HapticPlayer(context)
}

class DhikrApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as DhikrApplication).container
