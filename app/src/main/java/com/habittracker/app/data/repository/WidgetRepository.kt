package com.habittracker.app.data.repository

import android.content.Context
import androidx.room.Room
import com.habittracker.app.data.local.HabitDatabase
import com.habittracker.app.data.local.entity.RecordEntity
import com.habittracker.app.data.local.entity.isActiveOn
import com.habittracker.app.util.DateUtils
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.flow.first

/**
 * Lightweight data access for AppWidgets.
 * Widgets cannot use Hilt injection, so this repository creates its own Room instance.
 */
class WidgetRepository(context: Context) {

    private val db = Room.databaseBuilder(
        context.applicationContext,
        HabitDatabase::class.java,
        "habit_tracker_database"
    ).build()

    private val habitDao = db.habitDao()
    private val recordDao = db.recordDao()

    // ── Data types for widget consumption ──────────────────────────

    data class WidgetHabit(
        val id: Long,
        val name: String,
        val emoji: String,
        val isCompletedToday: Boolean,
        val weeklyTarget: Int,
        val currentWeekCount: Int,
        val sortOrder: Int,
        val isPaused: Boolean
    )

    data class WidgetDay(
        val dateMillis: Long,
        val dayLabel: String,
        val isCompleted: Boolean,
        val isToday: Boolean
    )

    // ── Queries ────────────────────────────────────────────────────

    /** Get today's active habits with their current completion status. */
    suspend fun getTodayHabits(): List<WidgetHabit> {
        val today = DateUtils.todayMillis()
        val allHabits = habitDao.getAllActiveHabits().first()
        val todayDate = LocalDate.now()
        val activeHabits = allHabits.filter { it.isActiveOn(todayDate) }

        val weekStart = DateUtils.startOfDay(DateUtils.currentWeekStart())
        val weekEnd = DateUtils.startOfDay(DateUtils.currentWeekEnd())

        return activeHabits.map { habit ->
            val todayRecords = recordDao.getRecordsInRange(habit.id, today, today).first()
            val weekRecords = recordDao.getRecordsInRange(habit.id, weekStart, weekEnd).first()
            WidgetHabit(
                id = habit.id,
                name = habit.name,
                emoji = habit.emoji,
                isCompletedToday = todayRecords.isNotEmpty(),
                weeklyTarget = habit.weeklyTarget.coerceAtLeast(1),
                currentWeekCount = weekRecords.size,
                sortOrder = habit.sortOrder,
                isPaused = habit.pausedAt != null && habit.resumedAt == null
            )
        }
    }

    /** Get the current week's 7 days with completion status (across all habits). */
    suspend fun getCurrentWeekDays(): List<WidgetDay> {
        val today = LocalDate.now()
        val monday = today.with(DayOfWeek.MONDAY)
        val days = (0..6).map { monday.plusDays(it.toLong()) }
        val weekStart = DateUtils.startOfDay(days.first())
        val weekEnd = DateUtils.startOfDay(days.last())

        val allRecords = recordDao.getRecordsInDateRange(weekStart, weekEnd)

        val completionMap = mutableMapOf<Long, Boolean>()
        allRecords.forEach { record ->
            val dateKey = record.date
            if (completionMap[dateKey] != true) {
                completionMap[dateKey] = true
            }
        }

        return days.map { day ->
            val dayMillis = DateUtils.startOfDay(day)
            WidgetDay(
                dateMillis = dayMillis,
                dayLabel = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                isCompleted = completionMap[dayMillis] ?: false,
                isToday = day == today
            )
        }
    }

    /** Toggle a habit's completion status for the given date. */
    suspend fun toggleRecord(habitId: Long, dateMillis: Long): Boolean {
        val existing = recordDao.getRecord(habitId, dateMillis)
        return if (existing != null) {
            recordDao.delete(existing)
            false
        } else {
            recordDao.insert(
                RecordEntity(habitId = habitId, date = dateMillis)
            )
            true
        }
    }
}
