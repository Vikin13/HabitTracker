package com.habittracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.habittracker.app.data.local.entity.RecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {

    /** Check if a habit was completed on a specific date. */
    @Query("SELECT COUNT(*) FROM records WHERE habitId = :habitId AND date = :dateMillis")
    suspend fun isCompletedOnDate(habitId: Long, dateMillis: Long): Int

    /** Get the single record for a habit on a specific date (if it exists). */
    @Query("SELECT * FROM records WHERE habitId = :habitId AND date = :dateMillis LIMIT 1")
    suspend fun getRecord(habitId: Long, dateMillis: Long): RecordEntity?

    /** Get all records for a habit within a date range. */
    @Query("SELECT * FROM records WHERE habitId = :habitId AND date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getRecordsInRange(habitId: Long, startDate: Long, endDate: Long): Flow<List<RecordEntity>>

    /** Get all records for a date (all habits). */
    @Query("SELECT * FROM records WHERE date = :dateMillis")
    suspend fun getRecordsByDate(dateMillis: Long): List<RecordEntity>

    /** Get all records in a date range across ALL habits (for calendar dots). */
    @Query("SELECT * FROM records WHERE date >= :startDate AND date <= :endDate")
    suspend fun getRecordsInDateRange(startDate: Long, endDate: Long): List<RecordEntity>

    /** Get all records for a habit (all-time, for stats). */
    @Query("SELECT * FROM records WHERE habitId = :habitId ORDER BY date ASC")
    suspend fun getAllRecordsForHabit(habitId: Long): List<RecordEntity>

    /** Signal that fires whenever the records table changes. */
    @Query("SELECT COUNT(*) FROM records")
    fun anyRecordCount(): Flow<Int>

    /** Get all records for today for all active habits. */
    @Query("""
        SELECT r.* FROM records r
        INNER JOIN habits h ON r.habitId = h.id
        WHERE r.date = :todayDate AND h.isActive = 1
    """)
    fun getTodayRecords(todayDate: Long): Flow<List<RecordEntity>>

    /** Get total completion count for a habit. */
    @Query("SELECT COUNT(*) FROM records WHERE habitId = :habitId")
    suspend fun getTotalCompletedCount(habitId: Long): Int

    /** Get distinct completion dates for a habit (for streak calculation). */
    @Query("SELECT DISTINCT date FROM records WHERE habitId = :habitId ORDER BY date DESC")
    suspend fun getCompletionDates(habitId: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: RecordEntity)

    @Delete
    suspend fun delete(record: RecordEntity)

    @Query("DELETE FROM records")
    suspend fun deleteAll()
}
