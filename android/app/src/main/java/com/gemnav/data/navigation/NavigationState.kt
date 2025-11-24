package com.gemnav.data.navigation

import com.gemnav.data.route.LatLng

/**
 * Navigation maneuver types for turn-by-turn instructions.
 */
enum class NavManeuver {
    STRAIGHT,
    LEFT,
    RIGHT,
    SLIGHT_LEFT,
    SLIGHT_RIGHT,
    SHARP_LEFT,
    SHARP_RIGHT,
    UTURN,
    MERGE,
    EXIT,
    ROUNDABOUT,
    FERRY,
    ARRIVE,
    DEPART
}

/**
 * Single navigation step/maneuver.
 */
data class NavStep(
    val instruction: String,
    val maneuverIcon: NavManeuver,
    val distanceMeters: Double,
    val streetName: String?,
    val location: LatLng,
    val exitNumber: Int? = null,
    val laneInfo: String? = null
)

/**
 * Complete route with navigation steps.
 */
data class NavRoute(
    val steps: List<NavStep>,
    val polylineCoordinates: List<LatLng>,
    val totalDistanceMeters: Double,
    val totalDurationSeconds: Long,
    val isTruckRoute: Boolean = false,
    val isFallback: Boolean = false
)

/**
 * Sealed class representing navigation state machine.
 */
sealed class NavigationState {
    
    /**
     * No active navigation session.
     */
    object Idle : NavigationState()
    
    /**
     * Route is being loaded/calculated.
     */
    object LoadingRoute : NavigationState()
    
    /**
     * Active navigation in progress.
     */
    data class Navigating(
        val currentStep: NavStep,
        val nextStep: NavStep?,
        val currentStepIndex: Int,
        val totalSteps: Int,
        val progressPct: Float,
        val distanceToNextMeters: Double,
        val distanceRemainingMeters: Double,
        val etaSeconds: Long,
        val currentBearing: Float = 0f
    ) : NavigationState()
    
    /**
     * User has deviated from the route.
     */
    data class OffRoute(
        val reason: String,
        val deviationMeters: Double,
        val lastKnownLocation: LatLng
    ) : NavigationState()
    
    /**
     * Route is being recalculated.
     */
    data class Recalculating(
        val reason: String = "Route recalculation in progress"
    ) : NavigationState()
    
    /**
     * Navigation completed - arrived at destination.
     */
    data class Finished(
        val totalDistanceTraveled: Double,
        val totalTimeSeconds: Long,
        val destinationName: String?
    ) : NavigationState()
    
    /**
     * Navigation blocked by SafeMode or tier restrictions.
     */
    data class Blocked(
        val reason: String
    ) : NavigationState()
}

/**
 * Navigation event for one-shot actions.
 */
sealed class NavigationEvent {
    data class StepChanged(val step: NavStep, val index: Int) : NavigationEvent()
    data class ApproachingStep(val step: NavStep, val distanceMeters: Double) : NavigationEvent()
    data class OffRouteDetected(val deviationMeters: Double) : NavigationEvent()
    object RouteRecalculated : NavigationEvent()
    object NavigationStarted : NavigationEvent()
    object NavigationStopped : NavigationEvent()
    data class Arrived(val destinationName: String?) : NavigationEvent()
    data class Error(val message: String) : NavigationEvent()
}
