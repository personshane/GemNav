package com.gemnav.core.shim

import android.util.Log
import com.gemnav.core.maps.google.*
import com.gemnav.data.maps.*
import com.gemnav.data.navigation.*
import com.gemnav.data.route.LatLng
import com.gemnav.core.safety.SafeModeManager

/**
 * MapsShim - Safe wrapper for all Google Maps SDK interactions.
 * Prevents crashes from SDK updates, null responses, or API changes.
 */
object MapsShim {
    private const val TAG = "MapsShim"
    
    private var isInitialized = false
    private var lastError: Exception? = null
    
    /**
     * Initialize the Maps shim layer.
     * @return true if initialization successful, false otherwise
     */
    fun initialize(): Boolean {
        return try {
            // TODO: Initialize Google Maps SDK safely
            logInfo("MapsShim initialized")
            isInitialized = true
            true
        } catch (e: Exception) {
            logError("Failed to initialize MapsShim", e)
            lastError = e
            SafeModeManager.reportFailure("MapsShim", e)
            false
        }
    }
    
    /**
     * Safely get current location with null guards.
     * @return Pair of (latitude, longitude) or null if unavailable
     */
    fun getCurrentLocation(): Pair<Double, Double>? {
        return try {
            if (!isInitialized) {
                logWarning("MapsShim not initialized, returning null location")
                return null
            }
            // TODO: Implement actual location retrieval from Maps SDK
            // Stub: Return null until implemented
            null
        } catch (e: Exception) {
            logError("Error getting current location", e)
            SafeModeManager.reportFailure("MapsShim.getCurrentLocation", e)
            null
        }
    }
    
    /**
     * Safely request a route between two points.
     * @param origin Start coordinates
     * @param destination End coordinates
     * @return Route data or null if unavailable
     */
    fun getRoute(origin: Pair<Double, Double>, destination: Pair<Double, Double>): RouteResult? {
        return try {
            if (!isInitialized) {
                logWarning("MapsShim not initialized, returning null route")
                return null
            }
            // TODO: Implement actual route request from Maps SDK
            // Stub: Return null until implemented
            null
        } catch (e: Exception) {
            logError("Error getting route", e)
            SafeModeManager.reportFailure("MapsShim.getRoute", e)
            null
        }
    }
    
    /**
     * Safely search for places with the given query.
     * @param query Search string
     * @return List of place results or empty list if unavailable
     */
    fun searchPlaces(query: String): List<PlaceResult> {
        return try {
            if (!isInitialized) {
                logWarning("MapsShim not initialized, returning empty places")
                return emptyList()
            }
            if (query.isBlank()) {
                return emptyList()
            }
            // TODO: Implement actual places search from Maps SDK
            // Stub: Return empty list until implemented
            emptyList()
        } catch (e: Exception) {
            logError("Error searching places: $query", e)
            SafeModeManager.reportFailure("MapsShim.searchPlaces", e)
            emptyList()
        }
    }
    
    /**
     * Check if Maps SDK is available and functioning.
     */
    fun isAvailable(): Boolean {
        return try {
            isInitialized && lastError == null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get the last error that occurred.
     */
    fun getLastError(): Exception? = lastError
    
    /**
     * Clear the last error.
     */
    fun clearError() {
        lastError = null
    }
    
    // Centralized logging methods
    private fun logInfo(message: String) {
        Log.i(TAG, message)
    }
    
    private fun logWarning(message: String) {
        Log.w(TAG, message)
    }
    
    private fun logError(message: String, e: Exception) {
        Log.e(TAG, message, e)
    }
    
    /**
     * Stub data class for route results.
     * TODO: Replace with actual Maps SDK route data
     */
    data class RouteResult(
        val distanceMeters: Long = 0,
        val durationSeconds: Long = 0,
        val polyline: String = "",
        val steps: List<String> = emptyList()
    )
    
    /**
     * Parse navigation steps from Google Directions API response.
     * MP-019: Full implementation for Plus tier turn-by-turn.
     * 
     * @param response The DirectionsResponse from Google Directions API
     * @return List of navigation steps for NavigationEngine
     */
    fun parseGoogleSteps(response: DirectionsResponse): List<NavStep> {
        if (response.routes.isEmpty()) {
            logWarning("Cannot parse steps - no routes in response")
            return emptyList()
        }
        
        val route = response.routes.first()
        val steps = mutableListOf<NavStep>()
        
        // Process all legs (supports multi-waypoint routes)
        for (leg in route.legs) {
            for (step in leg.steps) {
                val instruction = DirectionsApiClient.stripHtml(step.htmlInstructions)
                val maneuver = mapGoogleManeuver(step.maneuver)
                
                steps.add(NavStep(
                    instruction = instruction,
                    maneuverIcon = maneuver,
                    distanceMeters = step.distance.value.toDouble(),
                    streetName = extractStreetName(instruction),
                    location = step.startLocation.toLatLng()
                ))
            }
        }
        
        // Add arrival step
        route.legs.lastOrNull()?.let { lastLeg ->
            steps.add(NavStep(
                instruction = "Arrive at destination",
                maneuverIcon = NavManeuver.ARRIVE,
                distanceMeters = 0.0,
                streetName = lastLeg.endAddress,
                location = lastLeg.endLocation.toLatLng()
            ))
        }
        
        logInfo("Parsed ${steps.size} navigation steps from Google Directions")
        return steps
    }
    
    /**
     * Parse navigation steps from legacy RouteResult (deprecated stub).
     * @deprecated Use parseGoogleSteps(DirectionsResponse) instead
     */
    @Deprecated("Use parseGoogleSteps(DirectionsResponse) instead", ReplaceWith("parseGoogleSteps(response)"))
    fun parseStepsGoogle(routeResult: RouteResult?): List<NavStep> {
        if (routeResult == null) {
            logWarning("Cannot parse steps - null route result")
            return emptyList()
        }
        logWarning("parseStepsGoogle: deprecated - use parseGoogleSteps(DirectionsResponse)")
        return emptyList()
    }
    
    /**
     * Create a NavRoute from Google Directions API result.
     * MP-019: Full implementation for Plus tier.
     */
    fun createNavRoute(result: DirectionsResult.Success): NavRoute {
        val steps = parseGoogleSteps(
            DirectionsResponse(
                status = "OK",
                routes = listOf(result.route)
            )
        )
        
        return NavRoute(
            steps = steps,
            polylineCoordinates = result.polylineCoordinates,
            totalDistanceMeters = result.totalDistanceMeters.toDouble(),
            totalDurationSeconds = result.totalDurationSeconds.toLong(),
            isTruckRoute = false,
            isFallback = false
        )
    }
    
    /**
     * Create a NavRoute from Google route data for navigation.
     * @deprecated Use createNavRoute(DirectionsResult.Success) instead
     */
    @Deprecated("Use createNavRoute(DirectionsResult.Success) instead")
    fun createNavRoute(routeResult: RouteResult?, polylineCoordinates: List<LatLng>): NavRoute? {
        if (routeResult == null) return null
        
        @Suppress("DEPRECATION")
        val steps = parseStepsGoogle(routeResult)
        if (steps.isEmpty()) {
            logWarning("Cannot create NavRoute - no steps parsed")
            return null
        }
        
        return NavRoute(
            steps = steps,
            polylineCoordinates = polylineCoordinates,
            totalDistanceMeters = routeResult.distanceMeters.toDouble(),
            totalDurationSeconds = routeResult.durationSeconds,
            isTruckRoute = false,
            isFallback = false
        )
    }
    
    /**
     * Map Google maneuver string to NavManeuver enum.
     */
    private fun mapGoogleManeuver(maneuver: String?): NavManeuver {
        return when (maneuver) {
            GoogleManeuverTypes.TURN_LEFT -> NavManeuver.LEFT
            GoogleManeuverTypes.TURN_RIGHT -> NavManeuver.RIGHT
            GoogleManeuverTypes.TURN_SLIGHT_LEFT -> NavManeuver.SLIGHT_LEFT
            GoogleManeuverTypes.TURN_SLIGHT_RIGHT -> NavManeuver.SLIGHT_RIGHT
            GoogleManeuverTypes.TURN_SHARP_LEFT -> NavManeuver.SHARP_LEFT
            GoogleManeuverTypes.TURN_SHARP_RIGHT -> NavManeuver.SHARP_RIGHT
            GoogleManeuverTypes.UTURN_LEFT, GoogleManeuverTypes.UTURN_RIGHT -> NavManeuver.UTURN
            GoogleManeuverTypes.MERGE -> NavManeuver.MERGE
            GoogleManeuverTypes.RAMP_LEFT, GoogleManeuverTypes.FORK_LEFT, GoogleManeuverTypes.KEEP_LEFT -> NavManeuver.SLIGHT_LEFT
            GoogleManeuverTypes.RAMP_RIGHT, GoogleManeuverTypes.FORK_RIGHT, GoogleManeuverTypes.KEEP_RIGHT -> NavManeuver.SLIGHT_RIGHT
            GoogleManeuverTypes.ROUNDABOUT_LEFT, GoogleManeuverTypes.ROUNDABOUT_RIGHT -> NavManeuver.ROUNDABOUT
            GoogleManeuverTypes.FERRY, GoogleManeuverTypes.FERRY_TRAIN -> NavManeuver.FERRY
            GoogleManeuverTypes.STRAIGHT -> NavManeuver.STRAIGHT
            null -> NavManeuver.STRAIGHT
            else -> NavManeuver.STRAIGHT
        }
    }
    
    /**
     * Extract street name from instruction text.
     */
    private fun extractStreetName(instruction: String): String? {
        // Common patterns: "Turn left onto Main St", "Continue on Highway 101"
        val patterns = listOf(
            Regex("onto (.+)$"),
            Regex("on (.+)$"),
            Regex("toward (.+)$"),
            Regex("via (.+)$")
        )
        
        for (pattern in patterns) {
            pattern.find(instruction)?.let { match ->
                return match.groupValues.getOrNull(1)?.trim()
            }
        }
        
        return null
    }
    
    /**
     * Stub data class for place results.
     * TODO: Replace with actual Maps SDK place data
     */
    data class PlaceResult(
        val placeId: String = "",
        val name: String = "",
        val address: String = "",
        val latitude: Double = 0.0,
        val longitude: Double = 0.0
    )
}
