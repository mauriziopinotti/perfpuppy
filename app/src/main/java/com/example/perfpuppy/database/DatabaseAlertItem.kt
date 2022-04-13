package com.example.perfpuppy.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.perfpuppy.domain.AlertItem

@Entity(tableName = "alerts")
data class DatabaseAlertItem constructor(
    @PrimaryKey(autoGenerate = true)
    val id: Int,

    val message: String
)

fun List<DatabaseAlertItem>.asDomainModel(): List<AlertItem> {
    return map {
        AlertItem(
            id = it.id,
            message = it.message
        )
    }
}