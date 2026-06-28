package com.habittracker.app.ui.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.habittracker.app.R
import com.habittracker.app.data.repository.WidgetRepository
import kotlinx.coroutines.runBlocking

/**
 * RemoteViewsService that provides habit rows for the weekly grid widget.
 * Each row = one habit + 7 day dots (Mon-Sun).
 *
 * When there are no habits (or the query fails), a single placeholder row
 * is returned so the widget never shows a blank ListView.
 */
class WidgetHabitRowService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        HabitRowFactory(applicationContext)
}

class HabitRowFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    companion object {
        private const val TAG = "HabitRowFactory"
    }

    private var rows: List<WidgetRepository.HabitWeekRow> = emptyList()
    private var loadError: String? = null

    override fun onCreate() {
        Log.d(TAG, "onCreate")
        loadRows()
    }

    override fun onDataSetChanged() {
        Log.d(TAG, "onDataSetChanged")
        loadRows()
    }

    private fun loadRows() {
        try {
            runBlocking {
                rows = WidgetRepository(context).getWeeklyHabitRows()
            }
            loadError = null
            Log.d(TAG, "Loaded ${rows.size} rows")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load rows", e)
            rows = emptyList()
            loadError = e.message ?: "Unknown error"
        }
    }

    override fun onDestroy() = Unit

    override fun getCount(): Int = if (rows.isEmpty()) 1 else rows.size      // always ≥ 1

    override fun getViewAt(position: Int): RemoteViews {
        Log.d(TAG, "getViewAt($position) rows.size=${rows.size}")
        // If rows loaded successfully, show actual data
        if (rows.isNotEmpty() && position < rows.size) {
            return buildHabitRow(rows[position])
        }

        // Fallback / empty row
        val views = RemoteViews(context.packageName, R.layout.widget_habit_row)
        views.setTextViewText(R.id.row_emoji, if (loadError != null) "⚠" else "")
        return views
    }

    private fun buildHabitRow(row: WidgetRepository.HabitWeekRow): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_habit_row)

        views.setTextViewText(R.id.row_emoji, row.emoji)

        row.days.forEachIndexed { index, completed ->
            val dotId = when (index) {
                0 -> R.id.row_dot_0; 1 -> R.id.row_dot_1; 2 -> R.id.row_dot_2
                3 -> R.id.row_dot_3; 4 -> R.id.row_dot_4; 5 -> R.id.row_dot_5
                6 -> R.id.row_dot_6; else -> return@forEachIndexed
            }
            views.setImageViewResource(
                dotId,
                if (completed) R.drawable.ic_circle_completed
                else if (isDarkMode(context)) R.drawable.ic_circle_missed_dark
                else R.drawable.ic_circle_missed
            )
        }

        // Fill-in intent — combined with template to open the app
        val fillIn = Intent().apply {
            data = android.net.Uri.parse("habitcheck://open/${row.habitId}")
        }
        views.setOnClickFillInIntent(R.id.row_root, fillIn)

        return views
    }

    override fun getItemId(position: Int): Long =
        if (rows.isNotEmpty() && position < rows.size) rows[position].habitId
        else position.toLong()

    override fun hasStableIds(): Boolean = true

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1
}
