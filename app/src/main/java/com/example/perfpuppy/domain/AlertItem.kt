package com.example.perfpuppy.domain

import com.example.perfpuppy.database.DatabaseAlertItem
import java.text.DateFormat
import java.text.DateFormat.LONG
import java.text.DateFormat.SHORT
import java.util.*

data class AlertItem(
    val id: Int? = null,
    val message: String,
    val aboveTh: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
) {
    val formattedDate: String = DateFormat.getDateTimeInstance(LONG, SHORT).format(Date(timestamp))
}

fun AlertItem.asDatabaseModel(): DatabaseAlertItem =
    DatabaseAlertItem(
        id = this.id ?: 0,
        message = this.message,
        aboveTh = this.aboveTh,
        timestamp = this.timestamp,
    )
