package com.habittracker.app.data.repository

import android.content.Context
import com.habittracker.app.data.local.DatabaseProvider
import com.habittracker.app.data.local.entity.HabitEntity
import com.habittracker.app.data.local.entity.RecordEntity
import com.habittracker.app.data.local.entity.isActiveOn
import com.habittracker.app.util.DateUtils
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Lightweight data access for AppWidgets.
 * Widgets cannot use Hilt injection, so this repository creates its own Room instance.
 */
class WidgetRepository(context: Context) {

    private val db = DatabaseProvider.provide(context)

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

    /** One row in the weekly grid: a habit + its 7-day completion. */
    data class HabitWeekRow(
        val habitId: Long,
        val emoji: String,
        val name: String,
        /** Mon-Sun, true if completed on that day. */
        val days: List<Boolean>,
        val isPaused: Boolean
    )

    /** Look up a single habit by ID (one-shot, for reminder scheduling). */
    suspend fun getHabitById(habitId: Long): HabitEntity? =
        db.habitDao().getHabitByIdOnce(habitId)

    // ── Queries ────────────────────────────────────────────────────

    /** Get today's active habits with their current completion status. */
    suspend fun getTodayHabits(): List<WidgetHabit> {
        val today = DateUtils.todayMillis()
        val allHabits = habitDao.getAllActiveHabitsOnce()
        val todayDate = LocalDate.now()
        val activeHabits = allHabits.filter { it.isActiveOn(todayDate) }

        val weekStart = DateUtils.startOfDay(DateUtils.currentWeekStart())
        val weekEnd = DateUtils.startOfDay(DateUtils.currentWeekEnd())

        return activeHabits.map { habit ->
            val todayRecords = recordDao.getRecordsInRangeOnce(habit.id, today, today)
            val weekRecords = recordDao.getRecordsInRangeOnce(habit.id, weekStart, weekEnd)
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

    /** Get each active habit with its per-day completion for the current week. */
    suspend fun getWeeklyHabitRows(): List<HabitWeekRow> {
        val allHabits = habitDao.getAllActiveHabitsOnce()
        val todayDate = LocalDate.now()
        val activeHabits = allHabits.filter { it.isActiveOn(todayDate) }

        val monday = todayDate.with(DayOfWeek.MONDAY)
        val days = (0..6).map { monday.plusDays(it.toLong()) }
        val dayMillis = days.map { DateUtils.startOfDay(it) }

        return activeHabits.map { habit ->
            val weekRecords = recordDao.getRecordsInRangeOnce(habit.id, dayMillis.first(), dayMillis.last())
            val completedDates = weekRecords.map { it.date }.toSet()
            HabitWeekRow(
                habitId = habit.id,
                emoji = habit.emoji,
                name = habit.name,
                days = dayMillis.map { it in completedDates },
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
