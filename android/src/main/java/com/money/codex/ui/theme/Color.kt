package com.money.codex.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

enum class AppThemePreset(val label: String) {
    Ocean("海盐蓝"),
    Mint("薄荷绿"),
    Sunset("日落橙"),
    Graphite("石墨灰"),
    Midnight("午夜黑"),
    Obsidian("曜石黑"),
    Nocturne("深夜蓝"),
    Carbon("碳素黑")
}

data class ThemePalette(
    val brand: Color,
    val brandSurface: Color,
    val income: Color,
    val expense: Color,
    val appBackground: Color,
    val textMuted: Color,
    val cardSurface: Color,
    val onSurface: Color,
    val isDark: Boolean = false
)

private val Palettes = mapOf(
    AppThemePreset.Ocean to ThemePalette(
        brand = Color(0xFF1B5E8A),
        brandSurface = Color(0xFFEAF4FB),
        income = Color(0xFF0F9D6E),
        expense = Color(0xFFD14343),
        appBackground = Color(0xFFF3F6FA),
        textMuted = Color(0xFF64748B),
        cardSurface = Color.White,
        onSurface = Color(0xFF0F172A)
    ),
    AppThemePreset.Mint to ThemePalette(
        brand = Color(0xFF0F766E),
        brandSurface = Color(0xFFE7F8F4),
        income = Color(0xFF0F9D6E),
        expense = Color(0xFFDC4C57),
        appBackground = Color(0xFFF3FBF8),
        textMuted = Color(0xFF5B7078),
        cardSurface = Color.White,
        onSurface = Color(0xFF0F172A)
    ),
    AppThemePreset.Sunset to ThemePalette(
        brand = Color(0xFFB45309),
        brandSurface = Color(0xFFFFF1E6),
        income = Color(0xFF0F9D6E),
        expense = Color(0xFFD14343),
        appBackground = Color(0xFFFFF8F2),
        textMuted = Color(0xFF7A6B63),
        cardSurface = Color.White,
        onSurface = Color(0xFF1F2937)
    ),
    AppThemePreset.Graphite to ThemePalette(
        brand = Color(0xFF475569),
        brandSurface = Color(0xFFF1F5F9),
        income = Color(0xFF16A34A),
        expense = Color(0xFFDC2626),
        appBackground = Color(0xFFF8FAFC),
        textMuted = Color(0xFF64748B),
        cardSurface = Color.White,
        onSurface = Color(0xFF0F172A)
    ),
    AppThemePreset.Midnight to ThemePalette(
        brand = Color(0xFF3B82F6),
        brandSurface = Color(0xFF1D2533),
        income = Color(0xFF22C55E),
        expense = Color(0xFFEF4444),
        appBackground = Color(0xFF0B1220),
        textMuted = Color(0xFF94A3B8),
        cardSurface = Color(0xFF111A2A),
        onSurface = Color(0xFFE2E8F0),
        isDark = true
    ),
    AppThemePreset.Obsidian to ThemePalette(
        brand = Color(0xFF64748B),
        brandSurface = Color(0xFF1F2430),
        income = Color(0xFF34D399),
        expense = Color(0xFFFB7185),
        appBackground = Color(0xFF0E1117),
        textMuted = Color(0xFF94A3B8),
        cardSurface = Color(0xFF161B22),
        onSurface = Color(0xFFE5E7EB),
        isDark = true
    ),
    AppThemePreset.Nocturne to ThemePalette(
        brand = Color(0xFF6366F1),
        brandSurface = Color(0xFF1E1B36),
        income = Color(0xFF22C55E),
        expense = Color(0xFFF87171),
        appBackground = Color(0xFF121227),
        textMuted = Color(0xFFA5B4FC),
        cardSurface = Color(0xFF1A1B32),
        onSurface = Color(0xFFE0E7FF),
        isDark = true
    ),
    AppThemePreset.Carbon to ThemePalette(
        brand = Color(0xFF14B8A6),
        brandSurface = Color(0xFF142123),
        income = Color(0xFF4ADE80),
        expense = Color(0xFFFB7185),
        appBackground = Color(0xFF0A0D0F),
        textMuted = Color(0xFF9CA3AF),
        cardSurface = Color(0xFF121619),
        onSurface = Color(0xFFE5E7EB),
        isDark = true
    )
)

object ThemeState {
    var preset by mutableStateOf(AppThemePreset.Ocean)
    val palette: ThemePalette get() = Palettes[preset] ?: Palettes.getValue(AppThemePreset.Ocean)
}

val Brand: Color get() = ThemeState.palette.brand
val BrandSurface: Color get() = ThemeState.palette.brandSurface
val Income: Color get() = ThemeState.palette.income
val Expense: Color get() = ThemeState.palette.expense
val AppBackground: Color get() = ThemeState.palette.appBackground
val TextMuted: Color get() = ThemeState.palette.textMuted
val AppCardSurface: Color get() = ThemeState.palette.cardSurface
val AppOnSurface: Color get() = ThemeState.palette.onSurface
val IsDarkThemePreset: Boolean get() = ThemeState.palette.isDark
