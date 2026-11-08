package com.dhikr.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.dhikr.app.appContainer
import com.dhikr.app.design.components.DotNavBar
import com.dhikr.app.design.theme.DhikrTheme
import com.dhikr.app.feature.counter.CounterScreen
import com.dhikr.app.feature.counter.CounterViewModel
import com.dhikr.app.feature.history.HistoryScreen
import com.dhikr.app.feature.history.HistoryViewModel
import com.dhikr.app.core.model.CountContext
import com.dhikr.app.feature.library.LibraryHost
import com.dhikr.app.feature.library.LibraryViewModel
import com.dhikr.app.feature.library.PrayerSetViewModel
import com.dhikr.app.feature.settings.SettingsScreen
import com.dhikr.app.feature.settings.SettingsViewModel
import com.dhikr.app.feature.today.TodayScreen
import com.dhikr.app.feature.today.TodayViewModel
import kotlinx.serialization.Serializable

/** Navigation 3 keys (D-010). @Serializable so the back stack survives configuration changes. */
@Serializable data object TodayRoute : NavKey
@Serializable data object LibraryRoute : NavKey
@Serializable data object HistoryRoute : NavKey
@Serializable data object SettingsRoute : NavKey
/**
 * Full-screen counter for one practice context (CountContext.storageKey: "daily" or a prayer).
 * [dhikrId] = null → continue Flow from the current Dhikr.
 */
@Serializable data class CounterRoute(val context: String = "daily", val dhikrId: Long? = null) : NavKey

private val tabs: List<NavKey> = listOf(TodayRoute, LibraryRoute, HistoryRoute, SettingsRoute)
private val tabLabels = listOf("Today", "Library", "History", "Settings")

/** Tabs are top-level: the stack is [Today] or [Today, tab], with Counter pushed on top. */
private fun NavBackStack<NavKey>.selectTab(tab: NavKey) {
    while (size > 1) removeAt(lastIndex)
    if (tab != TodayRoute) add(tab)
}

@Composable
fun DhikrNavigation() {
    val container = LocalContext.current.appContainer
    val backStack = rememberNavBackStack(TodayRoute)
    val top = backStack.lastOrNull()
    val showBar = top !is CounterRoute

    Column(Modifier.fillMaxSize().background(DhikrTheme.colors.canvas).safeDrawingPadding()) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.weight(1f),
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<TodayRoute> {
                    TodayScreen(
                        viewModel { TodayViewModel(container.engine, container.preferences, container.history) },
                        onOpenPrayer = { ctx -> backStack.add(CounterRoute(ctx.storageKey)) },
                        onOpenDaily = { id -> backStack.add(CounterRoute(CountContext.Daily.storageKey, id)) },
                        onOpenLibrary = { backStack.selectTab(LibraryRoute) },
                    )
                }
                entry<LibraryRoute> {
                    LibraryHost(
                        viewModel { LibraryViewModel(container.dhikrRepository, container.audioStore) },
                        container.audio,
                        viewModel { PrayerSetViewModel(container.dhikrRepository) },
                    )
                }
                entry<HistoryRoute> { HistoryScreen(viewModel { HistoryViewModel(container.history, container.engine) }) }
                entry<SettingsRoute> {
                    SettingsScreen(
                        viewModel { SettingsViewModel(container.preferences, container.haptics) },
                        container.haptics,
                        container.audio,
                    )
                }
                entry<CounterRoute> { key ->
                    CounterScreen(
                        viewModel {
                            CounterViewModel(
                                container.engine,
                                container.preferences,
                                CountContext.fromStorage(key.context) ?: CountContext.Daily,
                                key.dhikrId,
                            )
                        },
                        container.haptics,
                        container.audio,
                        onBack = { backStack.removeLastOrNull() },
                    )
                }
            },
        )
        if (showBar) {
            DotNavBar(
                items = tabLabels,
                selectedIndex = tabs.indexOf(top).coerceAtLeast(0),
                onSelect = { backStack.selectTab(tabs[it]) },
            )
        }
    }
}
