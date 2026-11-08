package com.dhikr.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhikr.app.core.model.AfterTarget
import com.dhikr.app.core.model.BeadStyle
import com.dhikr.app.core.model.SpringFeel
import com.dhikr.app.core.model.TapHaptic
import com.dhikr.app.core.model.TargetHaptic
import com.dhikr.app.core.model.ThemeMode
import com.dhikr.app.core.model.UserPreferences
import com.dhikr.app.data.preferences.PreferencesRepository
import com.dhikr.app.haptics.HapticPlayer
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val repo: PreferencesRepository, haptics: HapticPlayer) : ViewModel() {
    val prefs: StateFlow<UserPreferences> = repo.prefs
    val tapOptions: List<TapHaptic> = haptics.availableTapHaptics
    val targetOptions: List<TargetHaptic> = haptics.availableTargetHaptics

    fun setTheme(m: ThemeMode) = save { repo.setTheme(m) }
    fun setBeadStyle(s: BeadStyle) = save { repo.setBeadStyle(s) }
    fun setSpring(s: SpringFeel) = save { repo.setSpring(s) }
    fun setTapHaptic(h: TapHaptic) = save { repo.setTapHaptic(h) }
    fun setTargetHaptic(h: TargetHaptic) = save { repo.setTargetHaptic(h) }
    fun setAfterTarget(a: AfterTarget) = save { repo.setAfterTarget(a) }

    private fun save(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
