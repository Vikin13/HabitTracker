package com.habittracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.habittracker.app.util.DateUtils
import java.time.LocalDate

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val emoji: String = "✅",
    val reminderTime: String? = null,
    val endDate: Long? = null,
    val weeklyTarget: Int = 7,          // target completions per week (default = daily)
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    /** When the current (or last) pause started. null = never paused. */
    val pausedAt: Long? = null,
    /** When the last pause ended. null = currently still paused (if pausedAt != null). */
    val resumedAt: Long? = null
)

/** True if the habit is currently paused (pausedAt set, not yet resumed). */
val HabitEntity.isCurrentlyPaused: Boolean get() = pausedAt != null && resumedAt == null

/** Check whether this habit is active (not within a pause period) on the given date. */
fun HabitEntity.isActiveOn(date: LocalDate): Boolean {
    if (pausedAt == null) return true  // never paused → always active
    val pauseStart = DateUtils.toLocalDate(pausedAt)
    if (resumedAt == null) return date < pauseStart  // currently paused
    val pauseEnd = DateUtils.toLocalDate(resumedAt)
    return date < pauseStart || date >= pauseEnd  // active before pause or after resume
}

/** Check whether this habit is active (not within a pause period) on the given millis date. */
fun HabitEntity.isActiveOn(dateMillis: Long): Boolean = isActiveOn(DateUtils.toLocalDate(dateMillis))
