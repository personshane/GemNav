package com.gemnav.app.voice

/**
 * Voice command definitions for all tiers
 */
sealed class VoiceCommand {
    // Navigation commands
    data class Navigate(
        val destination: String,
        val waypoints: List<String> = emptyList()
    ) : VoiceCommand()
    
    data class AddStop(val location: String) : VoiceCommand()
    object CancelNavigation : VoiceCommand()
    object RecenterMap : VoiceCommand()
    
    // Audio commands
    object MuteVoice : VoiceCommand()
    object UnmuteVoice : VoiceCommand()
    object RepeatInstruction : VoiceCommand()
    
    // Information queries
    object GetETA : VoiceCommand()
    object GetDistanceRemaining : VoiceCommand()
    object GetTrafficStatus : VoiceCommand()
    data class SearchAlongRoute(
        val query: String,
        val filters: Map<String, String> = emptyMap()
    ) : VoiceCommand()
    
    // Route modifications
    data class AvoidRouteFeature(val feature: RouteFeature) : VoiceCommand()
    object ShowAlternativeRoutes : VoiceCommand()
    data class OptimizeRoute(val criterion: OptimizationCriterion) : VoiceCommand()
    
    // Truck-specific (Pro tier)
    data class FindTruckPOI(val type: TruckPOIType) : VoiceCommand()
    object CheckBridgeClearances : VoiceCommand()
    object CheckHeightRestrictions : VoiceCommand()
    object CheckWeightLimits : VoiceCommand()
    object FindWeighStations : VoiceCommand()
    
    // Conversation
    data class Clarification(val question: String) : VoiceCommand()
    object Unknown : VoiceCommand()
}

/**
 * Route features that can be avoided
 */
enum class RouteFeature {
    TOLLS,
    HIGHWAYS,
    FERRIES,
    UNPAVED_ROADS
}

/**
 * Route optimization criteria
 */
enum class OptimizationCriterion {
    FASTEST,
    SHORTEST,
    ECO_FRIENDLY,
    AVOID_TRAFFIC
}

/**
 * Truck-specific POI types (Pro tier)
 */
enum class TruckPOIType(val displayName: String) {
    TRUCK_STOP("truck stops"),
    WEIGH_STATION("weigh stations"),
    REST_AREA("rest areas with truck parking"),
    DIESEL_STATION("diesel fuel stations"),
    TRUCK_WASH("truck washes"),
    REPAIR_SHOP("truck repair shops")
}

/**
 * Command execution result
 */
sealed class CommandResult {
    data class Success(val message: String, val data: Any? = null) : CommandResult()
    data class Error(val message: String) : CommandResult()
    data class TierRestricted(val message: String) : CommandResult()
    data class NeedsClarification(val question: String) : CommandResult()
}
