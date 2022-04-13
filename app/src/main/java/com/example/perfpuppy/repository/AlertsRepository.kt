package com.example.perfpuppy.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.example.perfpuppy.data.AlertsDatabase
import com.example.perfpuppy.database.DatabaseAlertItem
import com.example.perfpuppy.database.asDomainModel
import com.example.perfpuppy.domain.AlertItem
import com.example.perfpuppy.domain.asDatabaseModel
import javax.inject.Inject

class AlertsRepository @Inject constructor(
    private val database: AlertsDatabase
) {

    val alerts: LiveData<List<AlertItem>> =
        Transformations.map(database.alertsDao.getDatabaseAlerts()) {
            it.asDomainModel()
        }

    suspend fun getAlerts(): LiveData<List<DatabaseAlertItem>> {
        return database.alertsDao.getDatabaseAlerts()
    }

    suspend fun addAlert(alert: AlertItem) {
        database.alertsDao.insert(alert.asDatabaseModel())
    }
}