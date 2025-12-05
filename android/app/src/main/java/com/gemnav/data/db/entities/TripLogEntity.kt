package com.gemnav.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trip_logs")
data class TripLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTimestamp: Long,
    val endTimestamp: Long?,
    val distanceMeters: Double,
    val encodedPath: String
)
