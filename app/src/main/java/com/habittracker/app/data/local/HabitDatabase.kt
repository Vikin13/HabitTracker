package com.habittracker.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.habittracker.app.data.local.dao.HabitDao
import com.habittracker.app.data.local.dao.RecordDao
import com.habittracker.app.data.local.entity.HabitEntity
import com.habittracker.app.data.local.entity.RecordEntity

@Database(
    entities = [HabitEntity::class, RecordEntity::class],
    version = 3,
    exportSchema = false
)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun recordDao(): RecordDao
}
