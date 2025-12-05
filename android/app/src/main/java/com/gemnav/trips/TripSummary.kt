package com.gemnav.trips

data class TripSummary(
    val id: Long,
    val startTimestamp: Long,
    val endTimestamp: Long?,
    val distanceMeters: Double,
    val encodedPath: String
)
