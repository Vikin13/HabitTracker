package com.habittracker.app.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.habittracker.app.MainActivity
import com.habittracker.app.R
import com.habittracker.app.data.repository.WidgetRepository
import kotlinx.coroutines.runBlocking

/**
 * Receives exact-alarm intents from [ReminderScheduler] and posts a
 * notification reminding the user to complete their habit.
 *
 * After posting the notification it automatically reschedules the
 * alarm for the next day.
 */
class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_REMIND = "com.habittracker.app.ACTION_REMIND"
        const val EXTRA_HABIT_ID = "habit_id"

        private const val CHANNEL_ID = "habit_reminder"
        private const val NOTIFICATION_ID_BASE = 2000
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMIND) return

        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        if (habitId == -1L) return

        // Look up the habit name + emoji
        val habit = runBlocking {
            WidgetRepository(context).getHabitById(habitId)
        }
        if (habit == null) {
            // Habit was deleted — cancel future alarms
            ReminderScheduler.cancel(context, habitId)
            return
        }

        val name = habit.name
        val emoji = habit.emoji

        createChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_circle_completed)
            .setContentTitle("$emoji $name")
            .setContentText("Time to complete your habit!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openPending)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(habitId.toInt() + NOTIFICATION_ID_BASE, notification)

        // Reschedule for tomorrow using the same reminder time
        ReminderScheduler.schedule(context, habitId, habit.reminderTime)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Habit reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily habit check-in reminders"
            }
            nm.createNotificationChannel(channel)
        }
    }
}
