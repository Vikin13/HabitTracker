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
    version = 7,
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

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE habits SET createdAt = 1777564800000 WHERE name = '哥哥哥哥'")
            }
        }

        /** Fix existing habits with weeklyTarget=0 to daily default of 7. */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE habits SET weeklyTarget = 7 WHERE weeklyTarget = 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Drop isArchived column + add pausedAt/resumedAt.
                // SQLite can't DROP COLUMN, so recreate the table.
                db.execSQL("CREATE TABLE IF NOT EXISTS `habits_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `emoji` TEXT NOT NULL, `reminderTime` TEXT, `endDate` INTEGER, `weeklyTarget` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, `isActive` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `pausedAt` INTEGER, `resumedAt` INTEGER)")
                db.execSQL("INSERT INTO habits_new (id, name, emoji, reminderTime, endDate, weeklyTarget, sortOrder, isActive, createdAt, pausedAt, resumedAt) SELECT id, name, emoji, reminderTime, endDate, weeklyTarget, sortOrder, isActive, createdAt, CASE WHEN isArchived = 1 THEN createdAt ELSE NULL END, NULL FROM habits")
                db.execSQL("DROP TABLE habits")
                db.execSQL("ALTER TABLE habits_new RENAME TO habits")
            }
        }
    }
}
