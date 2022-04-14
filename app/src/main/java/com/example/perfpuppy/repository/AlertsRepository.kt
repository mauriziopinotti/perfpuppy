package com.example.perfpuppy.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.example.perfpuppy.database.AlertsDatabase
import com.example.perfpuppy.database.asDomainModel
import com.example.perfpuppy.domain.AlertItem
import com.example.perfpuppy.domain.asDatabaseModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class AlertsRepository @Inject constructor(
    private val database: AlertsDatabase
) {

    val alerts: LiveData<List<AlertItem>> =
        Transformations.map(database.alertsDao.getDatabaseAlerts()) {
            it.asDomainModel()
        }

    fun addAlert(alert: AlertItem) {
        CoroutineScope(Dispatchers.IO).launch {
            database.alertsDao.insert(alert.asDatabaseModel())
        }
    }
}