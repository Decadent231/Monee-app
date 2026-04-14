package com.money.codex

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.money.codex.data.AuthSessionStore
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class QuickAddWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { widgetId ->
            appWidgetManager.updateAppWidget(widgetId, buildViews(context))
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        refreshAll(context)
    }

    companion object {
        private val moneyFormat = DecimalFormat("#,##0.00")

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, QuickAddWidgetProvider::class.java))
            ids.forEach { widgetId ->
                manager.updateAppWidget(widgetId, buildViews(context))
            }
        }

        private fun buildViews(context: Context): RemoteViews {
            AuthSessionStore.init(context)
            val user = AuthSessionStore.currentUser()
            val reminder = ReminderScheduler.loadSettings(context)
            val snapshot = WidgetSnapshotStore.read(context)
            val today = LocalDate.now()

            val monthLabel = today.format(DateTimeFormatter.ofPattern("MM月", Locale.CHINA))
            val dayLabel = today.format(DateTimeFormatter.ofPattern("dd", Locale.CHINA))
            val weekdayLabel = today.dayOfWeek.toChineseLabel()
            val reminderLabel = if (reminder.enabled) {
                String.format(Locale.getDefault(), "提醒 %02d:%02d", reminder.hour, reminder.minute)
            } else {
                "提醒未开启"
            }
            val nameLabel = user?.nickname ?: "未登录"
            val todayExpenseLabel = "¥${moneyFormat.format(snapshot.todayExpense)}"
            val dailyAvailableLabel = "¥${moneyFormat.format(snapshot.dailyAvailable)}"
            val ratio = when {
                snapshot.dailyAvailable <= 0.0 -> 0
                else -> ((snapshot.todayExpense / snapshot.dailyAvailable) * 100).toInt().coerceIn(0, 100)
            }
            val ratioLabel = if (snapshot.dailyAvailable <= 0.0) {
                "预算未设置"
            } else {
                "$ratio% 日度占比"
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                action = ReminderScheduler.ACTION_QUICK_ADD
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                4010,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            return RemoteViews(context.packageName, R.layout.widget_quick_add).apply {
                setTextViewText(R.id.widgetMonth, monthLabel)
                setTextViewText(R.id.widgetDay, dayLabel)
                setTextViewText(R.id.widgetWeekday, weekdayLabel)
                setTextViewText(R.id.widgetStatus, nameLabel)
                setTextViewText(R.id.widgetTodayExpense, todayExpenseLabel)
                setTextViewText(R.id.widgetDailyAvailable, dailyAvailableLabel)
                setTextViewText(R.id.widgetRatio, ratioLabel)
                setTextViewText(R.id.widgetReminder, reminderLabel)
                setProgressBar(R.id.widgetProgress, 100, ratio, false)
                setTextViewText(R.id.widgetAction, "＋ 快速记一笔")
                setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)
                setOnClickPendingIntent(R.id.widgetAction, pendingIntent)
            }
        }

        private fun java.time.DayOfWeek.toChineseLabel(): String {
            return when (value) {
                1 -> "周一"
                2 -> "周二"
                3 -> "周三"
                4 -> "周四"
                5 -> "周五"
                6 -> "周六"
                else -> "周日"
            }
        }
    }
}
