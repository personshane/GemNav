package com.gemnav.android.app.main_flow

import com.gemnav.android.app.api.*
import com.gemnav.android.app.main_flow.models.*
import com.gemnav.android.core.TierManager
import com.google.maps.model.TravelMode
import com.here.sdk.core.GeoCoordinates
import com.here.sdk.transport.TruckSpecifications
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteRepository @Inject constructor(
    private val tierManager: TierManager,
    private val directionsClient: DirectionsApiClient,
    private val hereClient: HereApiClient
) {
    
    /**
     * Calculate route based on user tier.
     * 
     * Free: Returns placeholder route (actual routing via Maps app intent)
     * Plus: Uses Google Directions API
     * Pro: Uses HERE SDK (truck) or Google Directions API (car mode toggle)
     */
    suspend fun calculateRoute(
        origin: Destination,
        destination: Destination,
        waypoints: List<Destination> = emptyList(),
        options: RouteOptions = RouteOptions()
    ): Result<Route> {
        return when (tierManager.currentTier) {
            TierManager.Tier.FREE -> calculateFreeRoute(origin, destination)
            TierManager.Tier.PLUS -> calculatePlusRoute(origin, destination, waypoints, options)
            TierManager.Tier.PRO -> calculateProRoute(origin, destination, waypoints, options)
        }
    }
    
    /**
     * Free tier: Return placeholder route.
     * Actual routing happens via Google Maps app intent.
     */
    private suspend fun calculateFreeRoute(
        origin: Destination,
        destination: Destination
    ): Result<Route> {
        return Result.success(
            Route(
                id = "free_${System.currentTimeMillis()}",
                origin = origin,
                destination = destination,
                distanceMeters = 0,
                durationSeconds = 0,
                polyline = "",
                summary = "Route via Google Maps"
            )
        )
    }
    
    /**
     * Plus tier: Use Google Directions API.
     */
    private suspend fun calculatePlusRoute(
        origin: Destination,
        destination: Destination,
        waypoints: List<Destination>,
        options: RouteOptions
    ): Result<Route> {
        return try {
            val originLoc = Location(origin.latitude, origin.longitude)
            val destLoc = Location(destination.latitude, destination.longitude)
            val waypointLocs = waypoints.map { Location(it.latitude, it.longitude) }
            
            val result = directionsClient.getDirections(
                origin = originLoc,
                destination = destLoc,
                waypoints = waypointLocs,
                travelMode = TravelMode.DRIVING,
                alternatives = true
            )
            
            // Convert first route to our Route model
            val primaryRoute = result.routes.firstOrNull()
                ?: return Result.failure(Exception("No routes found"))
            
            val route = Route(
                id = "plus_${System.currentTimeMillis()}",
                origin = origin,
                destination = destination,
                waypoints = waypoints,
                distanceMeters = directionsClient.extractDistance(result, 0),
                durationSeconds = directionsClient.extractDuration(result, 0),
                polyline = primaryRoute.overviewPolyline.encodedPath,
                summary = primaryRoute.summary
            )
            
            Result.success(route)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Pro tier: Use HERE SDK for truck routing or Google Directions for car mode.
     */
    private suspend fun calculateProRoute(
        origin: Destination,
        destination: Destination,
        waypoints: List<Destination>,
        options: RouteOptions
    ): Result<Route> {
        return try {
            if (options.routingEngine == RoutingEngine.HERE && options.truckSpecs != null) {
                // Truck routing via HERE SDK
                calculateHereRoute(origin, destination, waypoints, options)
            } else {
                // Car routing via Google Directions API
                calculatePlusRoute(origin, destination, waypoints, options)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Calculate route using HERE SDK (Pro tier, truck mode).
     */
    private suspend fun calculateHereRoute(
        origin: Destination,
        destination: Destination,
        waypoints: List<Destination>,
        options: RouteOptions
    ): Result<Route> {
        return try {
            val originCoords = GeoCoordinates(origin.latitude, origin.longitude)
            val destCoords = GeoCoordinates(destination.latitude, destination.longitude)
            val waypointCoords = waypoints.map { 
                GeoCoordinates(it.latitude, it.longitude) 
            }
            
            val truckSpecs = options.truckSpecs
                ?: return Result.failure(Exception("Truck specifications required"))
            
            val result = hereClient.calculateTruckRoute(
                origin = originCoords,
                destination = destCoords,
                waypoints = waypointCoords,
                truckSpecs = truckSpecs,
                avoidTolls = options.avoidTolls,
                avoidFerries = options.avoidFerries
            )
            
            val primaryRoute = result.routes.firstOrNull()
                ?: return Result.failure(Exception("No routes found"))
            
            val route = Route(
                id = "pro_here_${System.currentTimeMillis()}",
                origin = origin,
                destination = destination,
                waypoints = waypoints,
                distanceMeters = primaryRoute.lengthInMeters.toLong(),
                durationSeconds = primaryRoute.duration.seconds,
                polyline = "", // HERE SDK uses different polyline format
                summary = "Truck route via HERE SDK"
            )
            
            Result.success(route)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get alternative routes.
     * Returns up to 3 alternative routes based on tier.
     */
    suspend fun getAlternativeRoutes(
        origin: Destination,
        destination: Destination,
        waypoints: List<Destination> = emptyList(),
        options: RouteOptions = RouteOptions()
    ): Result<List<Route>> {
        // For Free tier, no alternatives (uses Maps app)
        if (tierManager.currentTier == TierManager.Tier.FREE) {
            return Result.success(emptyList())
        }
        
        // For Plus/Pro, calculate route and extract alternatives
        // Implementation would parse all routes from DirectionsResult/HereResult
        return Result.success(emptyList()) // TODO: Implement alternatives parsing
    }
    
    /**
     * Cleanup API clients.
     */
    fun shutdown() {
        directionsClient.shutdown()
        hereClient.shutdown()
    }
}
