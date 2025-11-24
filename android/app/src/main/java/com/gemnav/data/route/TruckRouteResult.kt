package com.gemnav.data.route

/**
 * Result of a truck route calculation from HERE SDK.
 * Contains route data, warnings, and restriction information.
 */
sealed class TruckRouteResult {
    
    /**
     * Successful truck route calculation.
     */
    data class Success(
        val route: TruckRouteData
    ) : TruckRouteResult()
    
    /**
     * Route calculation failed.
     */
    data class Failure(
        val errorMessage: String,
        val errorCode: TruckRouteError = TruckRouteError.UNKNOWN
    ) : TruckRouteResult()
}

/**
 * Complete truck route data.
 */
data class TruckRouteData(
    val distanceMeters: Long,
    val durationSeconds: Long,
    val polylineCoordinates: List<LatLng>,
    val warnings: List<TruckWarning>,
    val tollInfo: TollInfo? = null,
    val isFallback: Boolean = false
    // TODO: Add maneuvers for turn-by-turn navigation
    // val maneuvers: List<Maneuver> = emptyList()
)

/**
 * Simple lat/lng coordinate.
 */
data class LatLng(
    val latitude: Double,
    val longitude: Double
)

/**
 * Warning about route restrictions.
 */
data class TruckWarning(
    val type: WarningType,
    val location: LatLng,
    val description: String,
    val severity: WarningSeverity
)

enum class WarningType {
    LOW_BRIDGE,
    WEIGHT_LIMIT,
    HEIGHT_RESTRICTION,
    WIDTH_RESTRICTION,
    LENGTH_RESTRICTION,
    HAZMAT_RESTRICTION,
    TUNNEL_RESTRICTION,
    ROAD_CLOSED,
    STEEP_GRADE,
    SHARP_TURN,
    OTHER
}

enum class WarningSeverity {
    INFO,       // Advisory only
    WARNING,    // Tight clearance, proceed with caution
    CRITICAL    // Vehicle cannot pass safely
}

/**
 * Toll information for the route.
 */
data class TollInfo(
    val estimatedCost: Double,
    val currency: String = "USD",
    val tollPoints: Int
)

/**
 * Error codes for route calculation failures.
 */
enum class TruckRouteError {
    UNKNOWN,
    SDK_NOT_INITIALIZED,
    FEATURE_NOT_ENABLED,
    SAFE_MODE_ACTIVE,
    INVALID_COORDINATES,
    INVALID_TRUCK_CONFIG,
    NO_ROUTE_FOUND,
    NETWORK_ERROR,
    SERVER_ERROR,
    RATE_LIMITED
}

/**
 * State for truck route UI display.
 */
sealed class TruckRouteState {
    object Idle : TruckRouteState()
    object Loading : TruckRouteState()
    data class Success(val data: TruckRouteData) : TruckRouteState()
    data class Error(val message: String, val code: TruckRouteError) : TruckRouteState()
}
