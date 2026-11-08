package com.dhikr.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhikr.app.design.theme.DhikrTheme
import com.dhikr.app.navigation.DhikrNavigation

/**
 * Single activity. The UI is Compose; screens are Navigation 3 entries (see DhikrNavigation).
 * State lives in the AppContainer stores, not here — the activity is recreated on rotation,
 * theme and locale changes.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefsStore = appContainer.preferences
        setContent {
            val prefs by prefsStore.prefs.collectAsStateWithLifecycle()
            DhikrTheme(prefs.themeMode) {
                SystemBars(DhikrTheme.colors.isDark)
                DhikrNavigation()
            }
        }
    }

    /** Status/navigation bar icons follow the app theme (which can differ from the system theme). */
    @Composable
    private fun SystemBars(dark: Boolean) {
        LaunchedEffect(dark) {
            val style = if (dark) {
                SystemBarStyle.dark(Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
            }
            enableEdgeToEdge(style, style)
        }
    }
}
