package com.example.perfpuppy.di

import android.content.Context
import androidx.room.Room
import com.example.perfpuppy.database.AlertsDao
import com.example.perfpuppy.database.AlertsDatabase
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
    fun provideAppDatabase(@ApplicationContext appContext: Context): AlertsDatabase {
        return Room.databaseBuilder(
            appContext,
            AlertsDatabase::class.java,
            "Alerts"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideChannelDao(alertsDatabase: AlertsDatabase): AlertsDao {
        return alertsDatabase.alertsDao
    }
}