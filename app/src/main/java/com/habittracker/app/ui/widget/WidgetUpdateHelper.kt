package com.habittracker.app.ui.widget

import android.content.Context

/**
 * Utility for broadcasting data-change events to widget providers.
 * Called from ViewModels after any toggle/clear operation.
 */
object WidgetUpdateHelper {

    /** Notify all widgets that habit data has changed. */
    fun notifyDataChanged(context: Context) {
        WeeklyGridWidgetProvider.requestUpdate(context)
        QuickCheckinWidgetProvider.requestUpdate(context)
    }
}
