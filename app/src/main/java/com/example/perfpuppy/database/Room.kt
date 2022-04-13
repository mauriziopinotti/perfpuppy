package com.example.perfpuppy.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.perfpuppy.database.DatabaseAlertItem

@Dao
interface AlertsDao {

    @Query("select * from alerts")
    fun getDatabaseAlerts(): LiveData<List<DatabaseAlertItem>>

//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    fun insertAll(alerts: List<DatabaseAlertItem>)

    @Insert
    fun insert(alerts: DatabaseAlertItem)

//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    fun insertAlertDetails(databaseAlertDetails: DatabaseAlertDetails)
}

@Database(entities = [DatabaseAlertItem::class], version = 3)
abstract class AlertsDatabase : RoomDatabase() {
    abstract val alertsDao: AlertsDao
}
