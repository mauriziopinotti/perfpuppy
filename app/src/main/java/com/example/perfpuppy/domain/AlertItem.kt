package com.example.perfpuppy.domain

import com.example.perfpuppy.database.DatabaseAlertItem

data class AlertItem(
    val id: Int? = null,
    val message: String,
)

fun AlertItem.asDatabaseModel(): DatabaseAlertItem =
    DatabaseAlertItem(
        id = this.id ?: 0,
        message = this.message,
    )
