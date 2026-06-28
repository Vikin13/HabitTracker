package com.habittracker.app.di

import android.content.Context
import androidx.room.Room
import com.habittracker.app.data.local.HabitDatabase
import com.habittracker.app.data.local.dao.HabitDao
import com.habittracker.app.data.local.dao.RecordDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideHabitDatabase(@ApplicationContext context: Context): HabitDatabase {
        return Room.databaseBuilder(
            context,
            HabitDatabase::class.java,
            "habit_tracker.db"
        )
            .addMigrations(HabitDatabase.MIGRATION_3_4, HabitDatabase.MIGRATION_4_5, HabitDatabase.MIGRATION_5_6, HabitDatabase.MIGRATION_6_7)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideHabitDao(database: HabitDatabase): HabitDao = database.habitDao()

    @Provides
    fun provideRecordDao(database: HabitDatabase): RecordDao = database.recordDao()
}
