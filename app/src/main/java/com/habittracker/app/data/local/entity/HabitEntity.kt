package com.habittracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

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
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
