package com.gemnav.core.maps.google

import com.gemnav.data.route.LatLng

/**
 * Google Directions API response models.
 * MP-019: Plus tier turn-by-turn routing.
 * 
 * These models mirror the Google Directions API JSON structure
 * for parsing route responses.
 */

/**
 * Top-level Directions API response.
 */
data class DirectionsResponse(
    val status: String,
    val routes: List<DirectionsRoute>,
    val errorMessage: String? = null
)

/**
 * A single route from the Directions API.
 */
data class DirectionsRoute(
    val summary: String,
    val legs: List<DirectionsLeg>,
    val overviewPolyline: DirectionsPolyline,
    val copyrights: String? = null,
    val warnings: List<String> = emptyList()
)

/**
 * A leg of a route (between waypoints).
 */
data class DirectionsLeg(
    val startAddress: String,
    val endAddress: String,
    val startLocation: DirectionsLatLng,
    val endLocation: DirectionsLatLng,
    val distance: DirectionsValue,
    val duration: DirectionsValue,
    val durationInTraffic: DirectionsValue? = null,
    val steps: List<DirectionsStep>
)

/**
 * A single navigation step within a leg.
 */
data class DirectionsStep(
    val htmlInstructions: String,
    val distance: DirectionsValue,
    val duration: DirectionsValue,
    val startLocation: DirectionsLatLng,
    val endLocation: DirectionsLatLng,
    val polyline: DirectionsPolyline,
    val maneuver: String? = null,
    val travelMode: String = "DRIVING"
)

/**
 * Encoded polyline.
 */
data class DirectionsPolyline(
    val points: String
)

/**
 * Distance or duration value.
 */
data class DirectionsValue(
    val text: String,
    val value: Int // meters for distance, seconds for duration
)

/**
 * Lat/Lng from Directions API.
 */
data class DirectionsLatLng(
    val lat: Double,
    val lng: Double
) {
    fun toLatLng(): LatLng = LatLng(lat, lng)
}

/**
 * Result wrapper for Directions API calls.
 */
sealed class DirectionsResult {
    data class Success(
        val route: DirectionsRoute,
        val polylineCoordinates: List<LatLng>,
        val totalDistanceMeters: Int,
        val totalDurationSeconds: Int
    ) : DirectionsResult()
    
    data class Failure(
        val errorMessage: String,
        val errorCode: DirectionsError
    ) : DirectionsResult()
}

/**
 * Directions API error codes.
 */
enum class DirectionsError {
    OK,
    NOT_FOUND,
    ZERO_RESULTS,
    MAX_WAYPOINTS_EXCEEDED,
    INVALID_REQUEST,
    OVER_DAILY_LIMIT,
    OVER_QUERY_LIMIT,
    REQUEST_DENIED,
    UNKNOWN_ERROR,
    NETWORK_ERROR,
    PARSE_ERROR,
    SAFE_MODE_ACTIVE,
    FEATURE_NOT_ENABLED
}

/**
 * Google maneuver types mapped from API strings.
 */
object GoogleManeuverTypes {
    const val TURN_LEFT = "turn-left"
    const val TURN_RIGHT = "turn-right"
    const val TURN_SLIGHT_LEFT = "turn-slight-left"
    const val TURN_SLIGHT_RIGHT = "turn-slight-right"
    const val TURN_SHARP_LEFT = "turn-sharp-left"
    const val TURN_SHARP_RIGHT = "turn-sharp-right"
    const val UTURN_LEFT = "uturn-left"
    const val UTURN_RIGHT = "uturn-right"
    const val STRAIGHT = "straight"
    const val MERGE = "merge"
    const val RAMP_LEFT = "ramp-left"
    const val RAMP_RIGHT = "ramp-right"
    const val FORK_LEFT = "fork-left"
    const val FORK_RIGHT = "fork-right"
    const val KEEP_LEFT = "keep-left"
    const val KEEP_RIGHT = "keep-right"
    const val ROUNDABOUT_LEFT = "roundabout-left"
    const val ROUNDABOUT_RIGHT = "roundabout-right"
    const val FERRY = "ferry"
    const val FERRY_TRAIN = "ferry-train"
}
