package com.money.codex

import android.content.Context

data class WidgetSnapshot(
    val todayExpense: Double = 0.0,
    val dailyAvailable: Double = 0.0
)

object WidgetSnapshotStore {
    private const val PREF_NAME = "monee_widget_snapshot"
    private const val KEY_TODAY_EXPENSE = "today_expense"
    private const val KEY_DAILY_AVAILABLE = "daily_available"

    fun read(context: Context): WidgetSnapshot {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return WidgetSnapshot(
            todayExpense = prefs.getFloat(KEY_TODAY_EXPENSE, 0f).toDouble(),
            dailyAvailable = prefs.getFloat(KEY_DAILY_AVAILABLE, 0f).toDouble()
        )
    }

    fun save(context: Context, snapshot: WidgetSnapshot) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_TODAY_EXPENSE, snapshot.todayExpense.toFloat())
            .putFloat(KEY_DAILY_AVAILABLE, snapshot.dailyAvailable.toFloat())
            .apply()
    }
}
