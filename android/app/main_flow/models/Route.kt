package com.gemnav.android.app.main_flow.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Route(
    val id: String,
    val origin: Destination,
    val destination: Destination,
    val waypoints: List<Destination> = emptyList(),
    val distanceMeters: Int,
    val durationSeconds: Int,
    val polyline: String,
    val summary: String,
    val warnings: List<String> = emptyList(),
    val routeType: RouteType = RouteType.FASTEST,
    val trafficDelaySeconds: Int = 0,
    val tollInfo: TollInfo? = null
) : Parcelable {
    
    val distanceText: String
        get() = formatDistance(distanceMeters)
    
    val durationText: String
        get() = formatDuration(durationSeconds + trafficDelaySeconds)
    
    private fun formatDistance(meters: Int): String {
        val miles = meters / 1609.34
        return when {
            miles < 0.1 -> "${(meters * 3.28084).toInt()} ft"
            miles < 10 -> String.format("%.1f mi", miles)
            else -> String.format("%.0f mi", miles)
        }
    }
    
    private fun formatDuration(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }
}

enum class RouteType {
    FASTEST,
    SHORTEST,
    ECO_FRIENDLY
}

@Parcelize
data class TollInfo(
    val hasTolls: Boolean,
    val estimatedCost: Double? = null,
    val currency: String = "USD"
) : Parcelable
