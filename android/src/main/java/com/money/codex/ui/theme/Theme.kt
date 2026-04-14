package com.money.codex.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect

@Composable
fun MoneyTheme(
    themePreset: AppThemePreset = AppThemePreset.Ocean,
    content: @Composable () -> Unit
) {
    SideEffect {
        ThemeState.preset = themePreset
    }

    val colors = if (IsDarkThemePreset) {
        darkColorScheme(
            primary = Brand,
            secondary = Brand,
            tertiary = Income,
            background = AppBackground,
            surface = AppCardSurface,
            onSurface = AppOnSurface,
            onSurfaceVariant = TextMuted
        )
    } else {
        lightColorScheme(
            primary = Brand,
            secondary = Brand,
            tertiary = Income,
            surface = AppCardSurface,
            background = AppBackground,
            onSurface = AppOnSurface,
            onSurfaceVariant = TextMuted
        )
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        content = content
    )
}
