package com.habittracker.app.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import java.util.Calendar

/**
 * Schedules / cancels daily exact alarms for habit reminders.
 *
 * Each alarm fires once at the habit's configured time.  The
 * [ReminderReceiver] then posts a notification and re-schedules
 * for the next day (see [ReminderReceiver]).
 */
object ReminderScheduler {

    private const val PREFS_NAME = "reminder_schedules"
    private const val KEY_PREFIX = "reminder_"
    private const val REQUEST_BASE = 1000

    /** Unique PendingIntent request code per habit (stable across reboots). */
    private fun requestCode(habitId: Long) = REQUEST_BASE + habitId.toInt()

    /**
     * Schedule a daily reminder for [habitId] at [reminderTime] ("HH:mm").
     * If [reminderTime] is null the call is ignored.
     */
    fun schedule(context: Context, habitId: Long, reminderTime: String?) {
        if (reminderTime == null) return

        val parts = reminderTime.split(":")
        if (parts.size != 2) return
        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return

        val triggerMillis = nextTriggerMillis(hour, minute)

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMIND
            putExtra(ReminderReceiver.EXTRA_HABIT_ID, habitId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode(habitId), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerMillis, pendingIntent),
            pendingIntent
        )

        // Store the schedule so we can cancel later
        prefs(context).edit().putLong(KEY_PREFIX + habitId, triggerMillis).apply()
    }

    /** Cancel the scheduled reminder for [habitId], if any. */
    fun cancel(context: Context, habitId: Long) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMIND
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode(habitId), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)

        prefs(context).edit().remove(KEY_PREFIX + habitId).apply()
    }

    /** Reschedule all stored reminders (e.g., after boot or app upgrade). */
    fun rescheduleAll(context: Context) {
        val map = prefs(context).all
        map.forEach { (key, value) ->
            if (key.startsWith(KEY_PREFIX) && value is Long) {
                val habitId = key.removePrefix(KEY_PREFIX).toLongOrNull() ?: return@forEach
                // Look up the habit's current reminder time from the DB
                val repo = com.habittracker.app.data.repository.WidgetRepository(context)
                val habit = kotlinx.coroutines.runBlocking {
                    repo.getHabitById(habitId)
                }
                if (habit != null) {
                    schedule(context, habitId, habit.reminderTime)
                } else {
                    prefs(context).edit().remove(key).apply()
                }
            }
        }
    }

    // ── helpers ──────────────────────────────────────────────

    /** Return epoch millis for the next [hour]:[minute] (today if still future, else tomorrow). */
    private fun nextTriggerMillis(hour: Int, minute: Int): Long {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return if (cal.timeInMillis <= now) {
            // Already passed today → schedule for tomorrow
            cal.add(Calendar.DAY_OF_MONTH, 1)
            cal.timeInMillis
        } else {
            cal.timeInMillis
        }
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Internal: re-schedule the same habit for the next day (called from [ReminderReceiver]). */
    internal fun rescheduleNextDay(context: Context, habitId: Long) {
        // Read the stored trigger time — the habit hasn't changed, so just schedule for +24h
        val previousTrigger = prefs(context).getLong(KEY_PREFIX + habitId, 0L)
        if (previousTrigger == 0L) return
        val nextTrigger = previousTrigger + 24 * 60 * 60 * 1000L

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMIND
            putExtra(ReminderReceiver.EXTRA_HABIT_ID, habitId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode(habitId), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(nextTrigger, pendingIntent),
            pendingIntent
        )
        prefs(context).edit().putLong(KEY_PREFIX + habitId, nextTrigger).apply()
    }
}
