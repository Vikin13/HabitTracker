package com.habittracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.habittracker.app.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    /** Active (not currently paused) first, then paused at end. */
    @Query("SELECT * FROM habits WHERE isActive = 1 ORDER BY (pausedAt IS NOT NULL AND resumedAt IS NULL) ASC, sortOrder ASC, name ASC")
    fun getAllActiveHabits(): Flow<List<HabitEntity>>

    /** All non-deleted habits, active first then paused. */
    @Query("SELECT * FROM habits WHERE isActive = 1 ORDER BY (pausedAt IS NOT NULL AND resumedAt IS NULL) ASC, sortOrder ASC, name ASC")
    fun getAllVisibleHabits(): Flow<List<HabitEntity>>

    /** Every habit including deleted ones. */
    @Query("SELECT * FROM habits ORDER BY sortOrder ASC, name ASC")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :habitId")
    fun getHabitById(habitId: Long): Flow<HabitEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(habit: HabitEntity): Long

    @Update
    suspend fun update(habit: HabitEntity)

    @Delete
    suspend fun delete(habit: HabitEntity)

    @Query("UPDATE habits SET sortOrder = :order WHERE id = :habitId")
    suspend fun updateSortOrder(habitId: Long, order: Int)

    @Query("UPDATE habits SET pausedAt = :pausedAt, resumedAt = NULL WHERE id = :habitId")
    suspend fun pauseHabit(habitId: Long, pausedAt: Long = System.currentTimeMillis())

    @Query("UPDATE habits SET resumedAt = :resumedAt WHERE id = :habitId")
    suspend fun resumeHabit(habitId: Long, resumedAt: Long = System.currentTimeMillis())
}
