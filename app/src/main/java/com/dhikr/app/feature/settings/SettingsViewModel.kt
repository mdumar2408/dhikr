package com.dhikr.app.feature.settings

import androidx.lifecycle.ViewModel
import com.dhikr.app.core.model.AfterTarget
import com.dhikr.app.core.model.BeadStyle
import com.dhikr.app.core.model.SpringFeel
import com.dhikr.app.core.model.TapHaptic
import com.dhikr.app.core.model.TargetHaptic
import com.dhikr.app.core.model.ThemeMode
import com.dhikr.app.core.model.UserPreferences
import com.dhikr.app.data.PreferencesStore
import com.dhikr.app.haptics.HapticPlayer
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(private val store: PreferencesStore, haptics: HapticPlayer) : ViewModel() {
    val prefs: StateFlow<UserPreferences> = store.prefs
    val tapOptions: List<TapHaptic> = haptics.availableTapHaptics
    val targetOptions: List<TargetHaptic> = haptics.availableTargetHaptics

    fun setTheme(m: ThemeMode) = store.setTheme(m)
    fun setBeadStyle(s: BeadStyle) = store.setFeel { it.copy(beadStyle = s) }
    fun setSpring(s: SpringFeel) = store.setFeel { it.copy(spring = s) }
    fun setTapHaptic(h: TapHaptic) = store.setFeel { it.copy(tapHaptic = h) }
    fun setTargetHaptic(h: TargetHaptic) = store.setFeel { it.copy(targetHaptic = h) }
    fun setAfterTarget(a: AfterTarget) = store.setFeel { it.copy(afterTarget = a) }
}
