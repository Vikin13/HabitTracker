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

/**
 * Weekly habit grid widget — read-only.
 * Displays Mon–Sun completion status in a 7-cell grid.
 *
 * Data refresh flow:
 *   [Toggle in app] → [broadcast WIDGET_UPDATE] → [onReceive → onUpdate]
 *   Periodic WorkManager fallback (not yet implemented).
 */
class WeeklyGridWidgetProvider : AppWidgetProvider() {

    companion object {
        const val WIDGET_UPDATE_ACTION = "com.habittracker.app.action.WIDGET_UPDATE"

        /** Send a broadcast to force all widget instances to refresh. */
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

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_weekly_grid)

            // Tap opens the app
            val tapIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val tapPending = PendingIntent.getActivity(
                context, widgetId, tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.grid_container, tapPending)

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
