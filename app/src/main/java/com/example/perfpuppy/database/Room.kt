package com.example.perfpuppy.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface AlertsDao {

    @Query("SELECT * FROM alerts ORDER BY created_at DESC")
    fun getDatabaseAlerts(): LiveData<List<DatabaseAlertItem>>

    @Insert
    fun insert(alerts: DatabaseAlertItem)
}

@Database(entities = [DatabaseAlertItem::class], version = 4)
abstract class AlertsDatabase : RoomDatabase() {
    abstract val alertsDao: AlertsDao
}
