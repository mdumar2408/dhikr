package com.dhikr.app.design.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.dhikr.app.core.model.ThemeMode

private val LocalDhikrColors = staticCompositionLocalOf<DhikrColors> { error("DhikrTheme not provided") }
private val LocalDhikrSpacing = staticCompositionLocalOf { DhikrSpacing() }
private val LocalDhikrType = staticCompositionLocalOf { DhikrType() }

/**
 * Entry point for all design tokens. Feature code reads `DhikrTheme.colors` etc. — never hex literals.
 * Tokens are also mapped onto a Material 3 ColorScheme so stock M3 components don't show default purple.
 */
object DhikrTheme {
    val colors: DhikrColors
        @Composable @ReadOnlyComposable get() = LocalDhikrColors.current
    val spacing: DhikrSpacing
        @Composable @ReadOnlyComposable get() = LocalDhikrSpacing.current
    val type: DhikrType
        @Composable @ReadOnlyComposable get() = LocalDhikrType.current
}

@Composable
fun DhikrTheme(mode: ThemeMode = ThemeMode.System, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
    }
    val colors = if (dark) DarkColors else LightColors
    val scheme = if (dark) {
        darkColorScheme(
            primary = colors.ink, onPrimary = colors.canvas,
            background = colors.canvas, onBackground = colors.ink,
            surface = colors.canvas, onSurface = colors.ink,
            surfaceVariant = colors.surface, onSurfaceVariant = colors.inkMuted,
            surfaceContainer = colors.surfaceRaised, outline = colors.hairline,
            error = colors.ember,
        )
    } else {
        lightColorScheme(
            primary = colors.ink, onPrimary = colors.canvas,
            background = colors.canvas, onBackground = colors.ink,
            surface = colors.canvas, onSurface = colors.ink,
            surfaceVariant = colors.surface, onSurfaceVariant = colors.inkMuted,
            surfaceContainer = colors.surfaceRaised, outline = colors.hairline,
            error = colors.ember,
        )
    }
    CompositionLocalProvider(
        LocalDhikrColors provides colors,
        LocalDhikrSpacing provides DhikrSpacing(),
        LocalDhikrType provides DhikrType(),
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
