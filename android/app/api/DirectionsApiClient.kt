package com.gemnav.app.api

import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.model.DirectionsResult
import com.google.maps.model.LatLng
import com.google.maps.model.TravelMode
import com.google.maps.model.Unit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

/**
 * Client for Google Directions API integration.
 * 
 * TIER RESTRICTIONS:
 * - Free tier: Not used (intents to Maps app)
 * - Plus tier: Full access (car routing)
 * - Pro tier: Used for car mode toggle only (truck routing uses HERE SDK)
 * 
 * Features:
 * - Route calculation with waypoints
 * - Alternative routes
 * - Traffic-aware routing
 * - ETA calculation
 * - Distance and duration estimates
 */
class DirectionsApiClient(private val apiKey: String) {
    
    private val geoApiContext: GeoApiContext = GeoApiContext.Builder()
        .apiKey(apiKey)
        .build()
    
    /**
     * Calculate route between origin and destination with optional waypoints.
     * 
     * @param origin Starting location (lat,lng)
     * @param destination End location (lat,lng)
     * @param waypoints Optional list of waypoint locations
     * @param travelMode Travel mode (driving, walking, bicycling, transit)
     * @param alternatives Whether to return alternative routes
     * @param departureTime Optional departure time for traffic calculation
     * @return Directions result with routes
     */
    suspend fun getDirections(
        origin: Location,
        destination: Location,
        waypoints: List<Location> = emptyList(),
        travelMode: TravelMode = TravelMode.DRIVING,
        alternatives: Boolean = true,
        departureTime: Instant? = null
    ): DirectionsResult = withContext(Dispatchers.IO) {
        
        val originLatLng = LatLng(origin.latitude, origin.longitude)
        val destinationLatLng = LatLng(destination.latitude, destination.longitude)
        
        val request = DirectionsApi.newRequest(geoApiContext)
            .origin(originLatLng)
            .destination(destinationLatLng)
            .mode(travelMode)
            .alternatives(alternatives)
            .units(Unit.IMPERIAL) // US/Canada default
        
        // Add waypoints if provided
        if (waypoints.isNotEmpty()) {
            val waypointArray = waypoints.map { 
                LatLng(it.latitude, it.longitude) 
            }.toTypedArray()
            request.waypoints(*waypointArray)
        }
        
        // Add departure time for traffic-aware routing
        if (departureTime != null) {
            request.departureTime(departureTime)
        }
        
        request.await()
    }
    
    /**
     * Optimize waypoint order for fastest route.
     * 
     * @param origin Starting location
     * @param destination End location
     * @param waypoints List of waypoints to optimize
     * @return Optimized waypoint order
     */
    suspend fun optimizeWaypoints(
        origin: Location,
        destination: Location,
        waypoints: List<Location>
    ): DirectionsResult = withContext(Dispatchers.IO) {
        
        val originLatLng = LatLng(origin.latitude, origin.longitude)
        val destinationLatLng = LatLng(destination.latitude, destination.longitude)
        val waypointArray = waypoints.map { 
            LatLng(it.latitude, it.longitude) 
        }.toTypedArray()
        
        DirectionsApi.newRequest(geoApiContext)
            .origin(originLatLng)
            .destination(destinationLatLng)
            .waypoints(*waypointArray)
            .optimizeWaypoints(true)
            .mode(TravelMode.DRIVING)
            .units(Unit.IMPERIAL)
            .await()
    }
    
    /**
     * Calculate route avoiding specific features.
     * 
     * @param origin Starting location
     * @param destination End location
     * @param avoidTolls Avoid toll roads
     * @param avoidHighways Avoid highways
     * @param avoidFerries Avoid ferries
     * @return Directions result
     */
    suspend fun getDirectionsWithAvoidances(
        origin: Location,
        destination: Location,
        avoidTolls: Boolean = false,
        avoidHighways: Boolean = false,
        avoidFerries: Boolean = false
    ): DirectionsResult = withContext(Dispatchers.IO) {
        
        val originLatLng = LatLng(origin.latitude, origin.longitude)
        val destinationLatLng = LatLng(destination.latitude, destination.longitude)
        
        val request = DirectionsApi.newRequest(geoApiContext)
            .origin(originLatLng)
            .destination(destinationLatLng)
            .mode(TravelMode.DRIVING)
            .units(Unit.IMPERIAL)
        
        if (avoidTolls) request.avoid(DirectionsApi.RouteRestriction.TOLLS)
        if (avoidHighways) request.avoid(DirectionsApi.RouteRestriction.HIGHWAYS)
        if (avoidFerries) request.avoid(DirectionsApi.RouteRestriction.FERRIES)
        
        request.await()
    }
    
    /**
     * Calculate ETA for a route.
     * Helper method to extract duration from DirectionsResult.
     * 
     * @param result Directions result
     * @param routeIndex Index of route to use (0 for primary)
     * @return Duration in seconds
     */
    fun extractDuration(result: DirectionsResult, routeIndex: Int = 0): Long {
        return result.routes.getOrNull(routeIndex)
            ?.legs?.sumOf { it.duration.inSeconds }
            ?: 0L
    }
    
    /**
     * Calculate total distance for a route.
     * 
     * @param result Directions result
     * @param routeIndex Index of route to use (0 for primary)
     * @return Distance in meters
     */
    fun extractDistance(result: DirectionsResult, routeIndex: Int = 0): Long {
        return result.routes.getOrNull(routeIndex)
            ?.legs?.sumOf { it.distance.inMeters }
            ?: 0L
    }
    
    /**
     * Shutdown the API context.
     * Call this when the client is no longer needed.
     */
    fun shutdown() {
        geoApiContext.shutdown()
    }
}

/**
 * Simple location data class for API calls.
 */
data class Location(
    val latitude: Double,
    val longitude: Double
)
