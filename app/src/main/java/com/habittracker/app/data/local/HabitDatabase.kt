package com.habittracker.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.habittracker.app.data.local.dao.HabitDao
import com.habittracker.app.data.local.dao.RecordDao
import com.habittracker.app.data.local.entity.HabitEntity
import com.habittracker.app.data.local.entity.RecordEntity

@Database(
    entities = [HabitEntity::class, RecordEntity::class],
    version = 4,
    exportSchema = false
)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun recordDao(): RecordDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
