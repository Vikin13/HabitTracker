package com.habittracker.app.data.local

import android.content.Context
import androidx.room.Room

/**
 * Shared singleton [HabitDatabase] holder.
 *
 * Both the Hilt-injected [DatabaseModule][com.habittracker.app.di.DatabaseModule]
 * and [WidgetRepository][com.habittracker.app.data.repository.WidgetRepository]
 * use this provider so that **all** Room operations share a single
 * [AppDatabase] instance.  This guarantees that Room's [InvalidationTracker]
 * — which drives reactive Flow emissions — fires for every write regardless
 * of which layer performed it.
 */
object DatabaseProvider {

    @Volatile
    private var database: HabitDatabase? = null

    fun provide(context: Context): HabitDatabase {
        return database ?: synchronized(this) {
            database ?: Room.databaseBuilder(
                context.applicationContext,
                HabitDatabase::class.java,
                "habit_tracker.db"
            )
                .addMigrations(
                    HabitDatabase.MIGRATION_3_4,
                    HabitDatabase.MIGRATION_4_5,
                    HabitDatabase.MIGRATION_5_6,
                    HabitDatabase.MIGRATION_6_7
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { database = it }
        }
    }
}
