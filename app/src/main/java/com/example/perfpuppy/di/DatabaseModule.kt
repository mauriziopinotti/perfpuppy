package com.example.perfpuppy.di

import android.content.Context
import androidx.room.Room
import com.example.perfpuppy.data.AlertsDao
import com.example.perfpuppy.data.AlertsDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
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