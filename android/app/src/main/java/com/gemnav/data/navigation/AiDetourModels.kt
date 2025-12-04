package com.gemnav.data.navigation

import com.gemnav.core.places.PlaceResult
import com.gemnav.data.route.LatLng

/**
 * MP-023: Data models for detour cost calculation and add-stop flow.
 * 
 * PLUS TIER ONLY - Used for along-route POI detour estimation.
 */

/**
 * Detour metrics when adding a POI as an intermediate stop.
 */
data class DetourInfo(
    /** Additional distance in meters compared to original route */
    val extraDistanceMeters: Int,
    /** Additional duration in seconds compared to original route */
    val extraDurationSeconds: Int,
    /** Original route distance in meters */
    val baseDistanceMeters: Int,
    /** Original route duration in seconds */
    val baseDurationSeconds: Int
) {
    /** Total route distance with detour */
    val totalDistanceMeters: Int get() = baseDistanceMeters + extraDistanceMeters
    
    /** Total route duration with detour */
    val totalDurationSeconds: Int get() = baseDurationSeconds + extraDurationSeconds
    
    /** Extra distance in miles (for display) */
    val extraDistanceMiles: Double get() = extraDistanceMeters / 1609.34
    
    /** Extra duration in minutes (for display) */
    val extraDurationMinutes: Int get() = (extraDurationSeconds + 30) / 60
    
    /** Format detour as human-readable string */
    fun formatDetour(): String {
        val miles = String.format("%.1f", extraDistanceMiles)
        return "+$extraDurationMinutes min, +$miles mi"
    }
}

/**
 * State for POI selection and detour calculation.
 */
sealed class DetourState {
    object Idle : DetourState()
    object Calculating : DetourState()
    data class Ready(val poi: SelectedPoi, val detourInfo: DetourInfo) : DetourState()
    data class Error(val message: String) : DetourState()
    data class Blocked(val reason: String) : DetourState()
}

/**
 * Represents a POI selected for potential detour.
 */
data class SelectedPoi(
    val name: String,
    val address: String?,
    val latLng: LatLng,
    val rating: Float? = null,
    val placeId: String? = null,
    val source: String = "google_places"
) {
    companion object {
        fun fromPlaceResult(place: PlaceResult): SelectedPoi {
            return SelectedPoi(
                name = place.name,
                address = place.address,
                latLng = LatLng(place.lat, place.lng),
                rating = place.rating?.toFloat(),
                placeId = place.placeId,
                source = "google_places"
            )
        }
    }
}
