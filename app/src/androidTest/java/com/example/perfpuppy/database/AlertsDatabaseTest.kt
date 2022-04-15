package com.example.perfpuppy.database

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.perfpuppy.util.getOrAwaitValue
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.*


@RunWith(AndroidJUnit4::class)
class AlertsDatabaseTest {

    private lateinit var alertsDao: AlertsDao
    private lateinit var db: AlertsDatabase

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AlertsDatabase::class.java).build()
        alertsDao = db.alertsDao
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun writeAlertAndReadInList() {
        // Add test item
        val item = DatabaseAlertItem(
            id = Random().nextInt(),
            message = "TEST MESSAGE",
            aboveTh = true,
            timestamp = 394452000000
        )
        alertsDao.insert(item)

        // Get test items
        val returnedItems = alertsDao.getAllAlerts().getOrAwaitValue()

        // Check test item is returned
        assertTrue(returnedItems.contains(item))
    }
}
