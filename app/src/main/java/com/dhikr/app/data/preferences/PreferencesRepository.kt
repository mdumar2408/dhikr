package com.dhikr.app.data.preferences

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import com.dhikr.app.core.model.AfterTarget
import com.dhikr.app.core.model.BeadStyle
import com.dhikr.app.core.model.CountingFeel
import com.dhikr.app.core.model.SpringFeel
import com.dhikr.app.core.model.TapHaptic
import com.dhikr.app.core.model.TargetHaptic
import com.dhikr.app.core.model.ThemeMode
import com.dhikr.app.core.model.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Typed DataStore with a JSON serializer (D-019). Pure JVM — unit-testable without a device.
 * - ignoreUnknownKeys: files written by a newer app version still load.
 * - coerceInputValues: an unknown enum name (e.g. a removed haptic) falls back to its default.
 */
object UserPreferencesSerializer : Serializer<UserPreferences> {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    override val defaultValue: UserPreferences = UserPreferences()

    override suspend fun readFrom(input: InputStream): UserPreferences = try {
        json.decodeFromString(UserPreferences.serializer(), input.readBytes().decodeToString())
    } catch (e: SerializationException) {
        throw CorruptionException("Unreadable preferences", e)
    } catch (e: IllegalArgumentException) {
        throw CorruptionException("Unreadable preferences", e)
    }

    override suspend fun writeTo(t: UserPreferences, output: OutputStream) {
        output.write(json.encodeToString(UserPreferences.serializer(), t).encodeToByteArray())
    }
}

class PreferencesRepository(private val store: DataStore<UserPreferences>, scope: CoroutineScope) {

    /** Hot, always-current preferences. Starts with defaults until the file is read (a few ms). */
    val prefs: StateFlow<UserPreferences> = store.data
        .catch { e -> if (e is IOException) emit(UserPreferencesSerializer.defaultValue) else throw e }
        .stateIn(scope, SharingStarted.Eagerly, UserPreferencesSerializer.defaultValue)

    suspend fun setTheme(mode: ThemeMode) = store.updateData { it.copy(themeMode = mode) }
    suspend fun setBeadStyle(v: BeadStyle) = updateFeel { it.copy(beadStyle = v) }
    suspend fun setSpring(v: SpringFeel) = updateFeel { it.copy(spring = v) }
    suspend fun setTapHaptic(v: TapHaptic) = updateFeel { it.copy(tapHaptic = v) }
    suspend fun setTargetHaptic(v: TargetHaptic) = updateFeel { it.copy(targetHaptic = v) }
    suspend fun setAfterTarget(v: AfterTarget) = updateFeel { it.copy(afterTarget = v) }

    private suspend fun updateFeel(f: (CountingFeel) -> CountingFeel) = store.updateData { it.copy(feel = f(it.feel)) }

    companion object {
        /** Exactly one DataStore per file per process — created only by AppContainer. */
        fun create(context: Context, scope: CoroutineScope): PreferencesRepository {
            val store = DataStoreFactory.create(
                serializer = UserPreferencesSerializer,
                corruptionHandler = ReplaceFileCorruptionHandler { UserPreferencesSerializer.defaultValue },
                scope = scope,
                produceFile = { context.applicationContext.dataStoreFile("user_preferences.json") },
            )
            return PreferencesRepository(store, scope)
        }
    }
}
