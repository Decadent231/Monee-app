package com.money.codex.data

import android.content.Context
import com.money.codex.BudgetAlertPalette
import com.money.codex.BudgetAlertSettingsUi

object AppPreferences {
    private const val PREF_NAME = "money_local_preferences"
    private const val KEY_BUDGET_WARNING_PERCENT = "budget_warning_percent"
    private const val KEY_BUDGET_PALETTE = "budget_palette"

    @Volatile
    private var initialized = false
    private lateinit var appContext: Context

    fun init(context: Context) {
        if (initialized) return
        appContext = context.applicationContext
        initialized = true
    }

    private fun prefs() = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun budgetAlertSettings(): BudgetAlertSettingsUi {
        if (!initialized) {
            return BudgetAlertSettingsUi()
        }
        val percent = prefs().getInt(KEY_BUDGET_WARNING_PERCENT, 80).coerceIn(50, 100)
        val palette = BudgetAlertPalette.fromKey(
            prefs().getString(KEY_BUDGET_PALETTE, BudgetAlertPalette.Sunset.key).orEmpty()
        )
        return BudgetAlertSettingsUi(
            warningPercent = percent,
            palette = palette
        )
    }

    fun saveBudgetAlertSettings(settings: BudgetAlertSettingsUi) {
        if (!initialized) return
        prefs().edit()
            .putInt(KEY_BUDGET_WARNING_PERCENT, settings.warningPercent.coerceIn(50, 100))
            .putString(KEY_BUDGET_PALETTE, settings.palette.key)
            .apply()
    }
}
