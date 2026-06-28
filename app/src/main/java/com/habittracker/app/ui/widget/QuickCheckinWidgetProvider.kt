package com.habittracker.app.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.habittracker.app.MainActivity
import com.habittracker.app.R
import com.habittracker.app.data.repository.WidgetRepository
import com.habittracker.app.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Quick check-in widget — interactive toggle without opening the app.
 *
 * Two tap targets per row:
 * - **Background** → opens the app (via [OPEN_APP_ACTION])
 * - **Toggle circle** → toggles the habit in-place (via [TOGGLE_PREFIX])
 */
class QuickCheckinWidgetProvider : AppWidgetProvider() {

    companion object {
        const val WIDGET_UPDATE_ACTION = "com.habittracker.app.action.QUICK_CHECKIN_UPDATE"
        const val OPEN_APP_ACTION = "com.habittracker.app.OPEN_APP"
        const val TOGGLE_PREFIX = "com.habittracker.app.TOGGLE_"
        const val EXTRA_WIDGET_TOGGLE = "widget_toggle_habit_id"

        fun requestUpdate(context: Context) {
            val intent = Intent(context, QuickCheckinWidgetProvider::class.java).apply {
                action = WIDGET_UPDATE_ACTION
            }
            context.sendBroadcast(intent)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WIDGET_UPDATE_ACTION -> {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(
                    ComponentName(context, QuickCheckinWidgetProvider::class.java)
                )
                onUpdate(context, manager, ids)
            }
            OPEN_APP_ACTION -> {
                // Row background tapped → open the app
                context.startActivity(
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                )
                return
            }
            else -> {
                // Toggle circle tapped: "com.habittracker.app.TOGGLE_{id}"
                val action = intent.action
                if (action?.startsWith(TOGGLE_PREFIX) == true) {
                    val habitId = action.removePrefix(TOGGLE_PREFIX).toLongOrNull()
                    if (habitId != null) {
                        runBlocking(Dispatchers.IO) {
                            WidgetRepository(context).toggleRecord(habitId, DateUtils.todayMillis())
                        }
                        // Refresh this widget so the toggle circle updates
                        val manager = AppWidgetManager.getInstance(context)
                        val ids = manager.getAppWidgetIds(
                            ComponentName(context, QuickCheckinWidgetProvider::class.java)
                        )
                        onUpdate(context, manager, ids)
                        // Also notify weekly grid if present
                        WeeklyGridWidgetProvider.requestUpdate(context)
                    }
                    return
                }
            }
        }
        super.onReceive(context, intent)
    }

    @Suppress("DEPRECATION")
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_quick_checkin)

            // Dark mode background
            if (isDarkMode(context)) {
                views.setInt(R.id.checkin_container, "setBackgroundResource", R.drawable.widget_bg_dark)
            }

            // Template broadcast — fires QuickCheckinWidgetProvider.onReceive with fill-in merged
            val templateIntent = Intent(context, QuickCheckinWidgetProvider::class.java)
            val templatePending = PendingIntent.getBroadcast(
                context, widgetId, templateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            // Connect to RemoteViewsService
            val serviceIntent = Intent(context, QuickCheckinRowService::class.java)
            views.setRemoteAdapter(R.id.checkin_list, serviceIntent)
            views.setEmptyView(R.id.checkin_list, android.R.id.empty)

            views.setPendingIntentTemplate(R.id.checkin_list, templatePending)

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
