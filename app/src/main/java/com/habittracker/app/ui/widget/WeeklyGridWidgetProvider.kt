package com.habittracker.app.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.habittracker.app.R
import com.habittracker.app.data.repository.WidgetRepository
import com.habittracker.app.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WeeklyGridWidgetProvider : AppWidgetProvider() {

    companion object {
        const val WIDGET_UPDATE_ACTION = "com.habittracker.app.action.WIDGET_UPDATE"
        private val DAY_LABELS = arrayOf("M", "T", "W", "T", "F", "S", "S")

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
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = WidgetRepository(context)
                val weekDays = repo.getCurrentWeekDays()

                for (widgetId in appWidgetIds) {
                    val views = buildWeekGridViews(context, weekDays)

                    val tapIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val tapPending = PendingIntent.getActivity(
                        context, widgetId, tapIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_root, tapPending)

                    withContext(Dispatchers.Main) {
                        appWidgetManager.updateAppWidget(widgetId, views)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun buildWeekGridViews(context: Context, weekDays: List<WidgetRepository.WidgetDay>): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_weekly_grid)
        val completedColor = 0xFF6750A4.toInt()
        val missedColor = 0xFFBDBDBD.toInt()
        val todayColor = 0xFF6750A4.toInt()

        weekDays.forEachIndexed { index, day ->
            val labelId = when (index) {
                0 -> R.id.label_0; 1 -> R.id.label_1; 2 -> R.id.label_2
                3 -> R.id.label_3; 4 -> R.id.label_4; 5 -> R.id.label_5
                6 -> R.id.label_6; else -> return@forEachIndexed
            }
            val dotId = when (index) {
                0 -> R.id.dot_0; 1 -> R.id.dot_1; 2 -> R.id.dot_2
                3 -> R.id.dot_3; 4 -> R.id.dot_4; 5 -> R.id.dot_5
                6 -> R.id.dot_6; else -> return@forEachIndexed
            }

            views.setTextViewText(labelId, DAY_LABELS[index])
            views.setTextColor(labelId, if (day.isToday) todayColor else 0xFF888888.toInt())

            if (day.isCompleted) {
                views.setImageViewResource(dotId, R.drawable.ic_circle_filled)
                views.setInt(dotId, "setColorFilter", completedColor)
            } else {
                views.setImageViewResource(dotId, R.drawable.ic_circle_outline)
                views.setInt(dotId, "setColorFilter", missedColor)
            }
        }

        return views
    }
}
