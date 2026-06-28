package com.habittracker.app.ui.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.habittracker.app.R
import com.habittracker.app.data.repository.WidgetRepository
import kotlinx.coroutines.runBlocking

/**
 * RemoteViewsService that provides today's habit rows for the quick check-in widget.
 *
 * Each row has **two** fill-in intents with different actions:
 * - [ROW_ROOT][R.id.checkin_row_root] → `"OPEN_APP"` → opens [MainActivity]
 * - [TOGGLE][R.id.checkin_toggle] → `"TOGGLE_{id}"` → toggles in-place (no app open)
 */
class QuickCheckinRowService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        CheckinRowFactory(applicationContext)
}

class CheckinRowFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    companion object {
        private const val TAG = "CheckinRowFactory"
    }

    private var rows: List<WidgetRepository.WidgetHabit> = emptyList()
    private var loadError: String? = null

    override fun onCreate() {
        loadRows()
    }

    override fun onDataSetChanged() {
        loadRows()
    }

    private fun loadRows() {
        try {
            runBlocking {
                rows = WidgetRepository(context).getTodayHabits()
            }
            loadError = null
            android.util.Log.d(TAG, "Loaded ${rows.size} rows")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to load rows", e)
            rows = emptyList()
            loadError = e.message ?: "Unknown error"
        }
    }

    override fun onDestroy() = Unit

    override fun getCount(): Int = if (rows.isEmpty()) 1 else rows.size      // always ≥ 1

    override fun getViewAt(position: Int): RemoteViews {
        android.util.Log.d(TAG, "getViewAt($position) rows.size=${rows.size}")
        if (rows.isNotEmpty() && position < rows.size) {
            return buildCheckinRow(rows[position])
        }

        // Fallback / empty row
        val views = RemoteViews(context.packageName, R.layout.widget_checkin_row)
        views.setTextViewText(R.id.checkin_row_emoji, "")
        views.setTextViewText(R.id.checkin_row_name, if (loadError != null) "⚠ Error loading habits" else "No habits yet")
        if (isDarkMode(context)) {
            views.setTextColor(R.id.checkin_row_name, 0xFFDDDDDD.toInt())
        }
        views.setImageViewResource(
            R.id.checkin_toggle,
            if (isDarkMode(context)) R.drawable.ic_circle_missed_dark
            else R.drawable.ic_circle_missed
        )
        return views
    }

    private fun buildCheckinRow(row: WidgetRepository.WidgetHabit): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_checkin_row)

        views.setTextViewText(R.id.checkin_row_emoji, row.emoji)
        views.setTextViewText(R.id.checkin_row_name, row.name)
        if (isDarkMode(context)) {
            views.setTextColor(R.id.checkin_row_name, 0xFFDDDDDD.toInt())
        }
        views.setImageViewResource(
            R.id.checkin_toggle,
            if (row.isCompletedToday) R.drawable.ic_circle_completed
            else if (isDarkMode(context)) R.drawable.ic_circle_missed_dark
            else R.drawable.ic_circle_missed
        )

        // ── Row background → open the app ──────────────────────────
        val openFillIn = Intent().apply {
            action = "com.habittracker.app.OPEN_APP"
            data = Uri.parse("habitcheck://open/${row.id}")
        }
        views.setOnClickFillInIntent(R.id.checkin_row_root, openFillIn)

        // ── Toggle circle → toggle only, no app open ───────────────
        val toggleFillIn = Intent().apply {
            action = "com.habittracker.app.TOGGLE_${row.id}"
            data = Uri.parse("habitcheck://toggle/${row.id}")
            putExtra(QuickCheckinWidgetProvider.EXTRA_WIDGET_TOGGLE, row.id)
        }
        views.setOnClickFillInIntent(R.id.checkin_toggle, toggleFillIn)

        return views
    }

    override fun getItemId(position: Int): Long =
        if (rows.isNotEmpty() && position < rows.size) rows[position].id
        else position.toLong()

    override fun hasStableIds(): Boolean = true

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1
}
