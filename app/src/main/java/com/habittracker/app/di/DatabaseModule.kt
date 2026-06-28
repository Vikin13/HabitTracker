package com.habittracker.app.di

import android.content.Context
import com.habittracker.app.data.local.DatabaseProvider
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
    fun provideHabitDatabase(@ApplicationContext context: Context): HabitDatabase =
        DatabaseProvider.provide(context)

    @Provides
    fun provideHabitDao(database: HabitDatabase): HabitDao = database.habitDao()

    @Provides
    fun provideRecordDao(database: HabitDatabase): RecordDao = database.recordDao()
}
