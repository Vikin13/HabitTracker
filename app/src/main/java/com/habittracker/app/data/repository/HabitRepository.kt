package com.habittracker.app.data.repository

import com.habittracker.app.data.local.dao.HabitDao
import com.habittracker.app.data.local.dao.RecordDao
import com.habittracker.app.data.local.entity.HabitEntity
import com.habittracker.app.data.local.entity.RecordEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepository @Inject constructor(
    private val habitDao: HabitDao,
    private val recordDao: RecordDao
) {
    // ── Habits ──────────────────────────────────────────────────

    val allActiveHabits: Flow<List<HabitEntity>> = habitDao.getAllActiveHabits()

    val allHabits: Flow<List<HabitEntity>> = habitDao.getAllHabits()

    val archivedHabits: Flow<List<HabitEntity>> = habitDao.getArchivedHabits()

    fun getHabitById(habitId: Long): Flow<HabitEntity?> = habitDao.getHabitById(habitId)

    suspend fun addHabit(
        name: String,
        emoji: String,
        reminderTime: String?,
        endDate: Long?,
        weeklyTarget: Int,
        sortOrder: Int
    ): Long {
        return habitDao.insert(
            HabitEntity(
                name = name,
                emoji = emoji,
                reminderTime = reminderTime,
                endDate = endDate,
                weeklyTarget = weeklyTarget,
                sortOrder = sortOrder
            )
        )
    }

    suspend fun updateHabit(habit: HabitEntity) = habitDao.update(habit)

    suspend fun deleteHabit(habit: HabitEntity) = habitDao.delete(habit)

    suspend fun archiveHabit(habitId: Long) = habitDao.archiveHabit(habitId)

    suspend fun unarchiveHabit(habitId: Long) = habitDao.unarchiveHabit(habitId)

    suspend fun updateSortOrder(habitId: Long, order: Int) = habitDao.updateSortOrder(habitId)

    // ── Records ─────────────────────────────────────────────────

    /** Toggle check-in for a habit on a given date. Returns true if checked in, false if unchecked. */
    suspend fun toggleRecord(habitId: Long, dateMillis: Long): Boolean {
        val existing = recordDao.getRecord(habitId, dateMillis)
        return if (existing != null) {
            recordDao.delete(existing)
            false
        } else {
            recordDao.insert(
                RecordEntity(
                    habitId = habitId,
                    date = dateMillis,
                    completedAt = System.currentTimeMillis()
                )
            )
            true
        }
    }

    suspend fun isCompletedOnDate(habitId: Long, dateMillis: Long): Boolean =
        recordDao.isCompletedOnDate(habitId, dateMillis) > 0

    fun getRecordsInRange(habitId: Long, startDate: Long, endDate: Long): Flow<List<RecordEntity>> =
        recordDao.getRecordsInRange(habitId, startDate, endDate)

    fun getTodayRecords(todayDate: Long): Flow<List<RecordEntity>> =
        recordDao.getTodayRecords(todayDate)

    suspend fun getTotalCompletedCount(habitId: Long): Int =
        recordDao.getTotalCompletedCount(habitId)

    suspend fun getCompletionDates(habitId: Long): List<Long> =
        recordDao.getCompletionDates(habitId)

    suspend fun getRecordsByDate(dateMillis: Long): List<RecordEntity> =
        recordDao.getRecordsByDate(dateMillis)

    suspend fun getRecordsInDateRange(startDate: Long, endDate: Long): List<RecordEntity> =
        recordDao.getRecordsInDateRange(startDate, endDate)

    suspend fun getAllRecordsForHabit(habitId: Long): List<RecordEntity> =
        recordDao.getAllRecordsForHabit(habitId)
}
