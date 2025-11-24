package com.gemnav.data.ai

import com.gemnav.data.route.LatLng

/**
 * IntentModel.kt - Sealed classes for AI-parsed navigation intents.
 * MP-020: Advanced AI Intent System
 * 
 * Supports complex natural language queries like:
 * - "Route me to the nearest truck stop with showers"
 * - "Find a motel with truck parking near Flagstaff"
 * - "Add a stop at Walmart on the way"
 */

/**
 * Top-level sealed class for all navigation intents parsed by Gemini.
 */
sealed class NavigationIntent {
    
    /**
     * Direct navigation to a destination.
     * Examples: "Navigate to Phoenix", "Take me home"
     */
    data class NavigateTo(
        val destinationText: String,
        val destinationCoords: LatLng? = null,
        val confidence: Float = 0f
    ) : NavigationIntent()
    
    /**
     * Find a Point of Interest.
     * Examples: "Find nearest truck stop with showers", "Find cheap diesel nearby"
     */
    data class FindPOI(
        val poiType: POIType,
        val filters: POIFilters = POIFilters(),
        val nearLocation: String? = null,
        val nearCoords: LatLng? = null,
        val confidence: Float = 0f
    ) : NavigationIntent()
    
    /**
     * Add a stop to current/planned route.
     * Examples: "Add a stop at Walmart on the way", "Stop at the next rest area"
     */
    data class AddStop(
        val stopType: StopType,
        val destinationText: String? = null,
        val poiType: POIType? = null,
        val confidence: Float = 0f
    ) : NavigationIntent()
    
    /**
     * Modify route preferences/constraints.
     * Examples: "Avoid mountains", "Take the fastest route", "Avoid tolls"
     */
    data class RoutePreferences(
        val settings: RouteSettings,
        val confidence: Float = 0f
    ) : NavigationIntent()
    
    /**
     * General question (not actionable navigation).
     * Examples: "How far to Denver?", "What's traffic like?"
     */
    data class Question(
        val query: String,
        val confidence: Float = 0f
    ) : NavigationIntent()
    
    /**
     * Intent could not be classified.
     */
    data class Unknown(
        val rawText: String,
        val reason: String = "Could not parse intent"
    ) : NavigationIntent()
}

/**
 * POI types supported for search.
 */
enum class POIType {
    TRUCK_STOP,
    GAS_STATION,
    DIESEL,
    REST_AREA,
    HOTEL,
    MOTEL,
    RESTAURANT,
    FAST_FOOD,
    PARKING,
    TRUCK_PARKING,
    WALMART,
    GROCERY,
    WEIGH_STATION,
    REPAIR_SHOP,
    CAR_WASH,
    HOSPITAL,
    PHARMACY,
    ATM,
    OTHER
}

/**
 * Filters for POI search.
 */
data class POIFilters(
    val hasShowers: Boolean? = null,
    val hasOvernightParking: Boolean? = null,
    val hasTruckParking: Boolean? = null,
    val hasHazmatAccess: Boolean? = null,
    val hasDiesel: Boolean? = null,
    val hasRestrooms: Boolean? = null,
    val isOpen24Hours: Boolean? = null,
    val maxPrice: Double? = null,
    val minRating: Float? = null,
    val maxDistanceMiles: Double? = null,
    val brandPreference: String? = null
)

/**
 * Type of stop being added.
 */
enum class StopType {
    SPECIFIC_DESTINATION,  // Named place
    POI_TYPE,              // Type of POI (gas, food, etc.)
    NEXT_REST_AREA,
    WAYPOINT
}

/**
 * Route settings/preferences.
 */
data class RouteSettings(
    val avoidTolls: Boolean? = null,
    val avoidHighways: Boolean? = null,
    val avoidFerries: Boolean? = null,
    val avoidMountains: Boolean? = null,
    val avoidSnow: Boolean? = null,
    val preferFastest: Boolean? = null,
    val preferShortest: Boolean? = null,
    val preferScenic: Boolean? = null,
    val truckMode: Boolean? = null,
    val hazmatRestrictions: List<String>? = null
)

/**
 * Result of intent classification.
 */
sealed class IntentClassificationResult {
    data class Success(
        val intent: NavigationIntent,
        val processingTimeMs: Long = 0
    ) : IntentClassificationResult()
    
    data class Failure(
        val reason: String,
        val rawText: String
    ) : IntentClassificationResult()
}

/**
 * Result of intent resolution (converting intent to actionable route request).
 */
sealed class IntentResolutionResult {
    data class RouteRequest(
        val request: AiRouteRequest,
        val explanation: String? = null
    ) : IntentResolutionResult()
    
    data class NeedsMoreInfo(
        val prompt: String,
        val missingFields: List<String>
    ) : IntentResolutionResult()
    
    data class NotSupported(
        val reason: String
    ) : IntentResolutionResult()
    
    data class Failure(
        val reason: String
    ) : IntentResolutionResult()
}
