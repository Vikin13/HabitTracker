package com.habittracker.app.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.habittracker.app.R
import com.habittracker.app.MainActivity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Weekly habit grid widget — read-only.
 * Shows one row per habit, each with 7 day dots (Mon-Sun).
 * Tapping a row opens [MainActivity].
 */
class WeeklyGridWidgetProvider : AppWidgetProvider() {

    companion object {
        const val WIDGET_UPDATE_ACTION = "com.habittracker.app.action.WIDGET_UPDATE"

        fun requestUpdate(context: Context) {
            val intent = Intent(context, WeeklyGridWidgetProvider::class.java).apply {
                action = WIDGET_UPDATE_ACTION
            }
            context.sendBroadcast(intent)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == WIDGET_UPDATE_ACTION) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, WeeklyGridWidgetProvider::class.java)
            )
            onUpdate(context, manager, ids)
        }
        super.onReceive(context, intent)
    }

    @Suppress("DEPRECATION")
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val today = LocalDate.now()
        val monday = today.with(DayOfWeek.MONDAY)
        val dayIds = intArrayOf(
            R.id.header_day_0, R.id.header_day_1, R.id.header_day_2, R.id.header_day_3,
            R.id.header_day_4, R.id.header_day_5, R.id.header_day_6
        )
        val dayLabels = (0..6).map { i ->
            monday.plusDays(i.toLong())
                .dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.ENGLISH)
        }

        for (widgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_weekly_grid)

            val dark = isDarkMode(context)
            if (dark) {
                views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_bg_dark)
                for (id in dayIds) views.setTextColor(id, 0xFFBBBBBB.toInt())
            }

            for (i in 0..6) {
                views.setTextViewText(dayIds[i], dayLabels[i])
            }

            // Template intent — starts the app when any row is tapped
            val templateIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val templatePending = PendingIntent.getActivity(
                context, widgetId, templateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            // Connect to RemoteViewsService for dynamic rows
            val serviceIntent = Intent(context, WidgetHabitRowService::class.java)
            views.setRemoteAdapter(R.id.widget_list, serviceIntent)
            views.setEmptyView(R.id.widget_list, android.R.id.empty)

            views.setPendingIntentTemplate(R.id.widget_list, templatePending)

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
