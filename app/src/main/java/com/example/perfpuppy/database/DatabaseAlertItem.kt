package com.example.perfpuppy.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.perfpuppy.domain.AlertItem

@Entity(tableName = "alerts")
data class DatabaseAlertItem constructor(

    @PrimaryKey(autoGenerate = true)
    val id: Int,

    @ColumnInfo(name = "message")
    val message: String,

    @ColumnInfo(name = "above_th")
    val aboveTh: Boolean,

    @ColumnInfo(name = "created_at")
    val timestamp: Long,
)

fun List<DatabaseAlertItem>.asDomainModel(): List<AlertItem> {
    return map {
        AlertItem(
            id = it.id,
            message = it.message,
            aboveTh = it.aboveTh,
            timestamp = it.timestamp,
        )
    }
}