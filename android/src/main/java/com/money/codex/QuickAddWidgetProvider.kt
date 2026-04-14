package com.money.codex

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

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
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, QuickAddWidgetProvider::class.java))
            ids.forEach { widgetId ->
                manager.updateAppWidget(widgetId, buildViews(context))
            }
        }

        private fun buildViews(context: Context): RemoteViews {
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
                setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)
                setOnClickPendingIntent(R.id.widgetAction, pendingIntent)
            }
        }
    }
}
