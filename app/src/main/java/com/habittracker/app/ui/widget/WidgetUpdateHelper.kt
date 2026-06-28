package com.habittracker.app.ui.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.res.Configuration
import com.habittracker.app.R

/** True if the system is in dark (night) mode. */
fun isDarkMode(context: Context): Boolean =
    (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

/**
 * Utility for broadcasting data-change events to widget providers.
 * Called from ViewModels after any toggle/clear operation.
 */
object WidgetUpdateHelper {

    /** Notify all widgets that habit data has changed. */
    fun notifyDataChanged(context: Context) {
        val manager = AppWidgetManager.getInstance(context)

        // WeeklyGrid — force HabitRowFactory.onDataSetChanged()
        val weeklyIds = manager.getAppWidgetIds(
            ComponentName(context, WeeklyGridWidgetProvider::class.java)
        )
        manager.notifyAppWidgetViewDataChanged(weeklyIds, R.id.widget_list)

        // QuickCheckin — force CheckinRowFactory.onDataSetChanged()
        val checkinIds = manager.getAppWidgetIds(
            ComponentName(context, QuickCheckinWidgetProvider::class.java)
        )
        manager.notifyAppWidgetViewDataChanged(checkinIds, R.id.checkin_list)

        // Full layout refresh (catches header/dark-mode changes)
        WeeklyGridWidgetProvider.requestUpdate(context)
        QuickCheckinWidgetProvider.requestUpdate(context)
    }
}
