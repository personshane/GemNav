package com.gemnav.app.api

import com.here.sdk.core.GeoCoordinates
import com.here.sdk.core.errors.InstantiationErrorException
import com.here.sdk.routing.*
import com.here.sdk.transport.TruckSpecifications
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Client for HERE SDK routing integration.
 * 
 * TIER RESTRICTIONS:
 * - Free tier: Not available
 * - Plus tier: Not available
 * - Pro tier: EXCLUSIVE (truck routing with legal compliance)
 * 
 * CRITICAL: Never mix HERE routing data with Google Maps UI/tiles.
 * HERE routing used for calculation only. Display in HERE MapView.
 * 
 * Features:
 * - Commercial truck routing with legal restrictions
 * - Height, weight, hazmat compliance
 * - Truck-specific route optimization
 * - Legal routing for commercial vehicles
 */
class HereApiClient {
    
    private var routingEngine: RoutingEngine? = null
    
    init {
        try {
            routingEngine = RoutingEngine()
        } catch (e: InstantiationErrorException) {
            throw RuntimeException("HERE SDK routing engine initialization failed", e)
        }
    }
    
    /**
     * Calculate truck route with vehicle specifications and restrictions.
     * 
     * @param origin Starting location
     * @param destination End location
     * @param waypoints Optional waypoints
     * @param truckSpecs Truck specifications (height, weight, axles, hazmat)
     * @param avoidTolls Avoid toll roads
     * @param avoidFerries Avoid ferries
     * @return HERE route result
     */
    suspend fun calculateTruckRoute(
        origin: GeoCoordinates,
        destination: GeoCoordinates,
        waypoints: List<GeoCoordinates> = emptyList(),
        truckSpecs: TruckSpecifications,
        avoidTolls: Boolean = false,
        avoidFerries: Boolean = false
    ): CalculateRouteResult = suspendCancellableCoroutine { continuation ->
        
        val waypointList = mutableListOf<Waypoint>().apply {
            add(Waypoint(origin))
            waypoints.forEach { add(Waypoint(it)) }
            add(Waypoint(destination))
        }
        
        val routeOptions = TruckOptions().apply {
            routeOptions.apply {
                // Enable alternatives for user choice
                alternatives = 3
                
                // Optimization mode
                optimizationMode = OptimizationMode.FASTEST
                
                // Avoidances
                if (avoidTolls) {
                    avoidanceOptions.roadFeatures.add(RoadFeatures.TOLL_ROAD)
                }
                if (avoidFerries) {
                    avoidanceOptions.roadFeatures.add(RoadFeatures.FERRY)
                }
                
                // Always avoid unpaved roads for trucks
                avoidanceOptions.roadFeatures.add(RoadFeatures.DIRT_ROAD)
            }
            
            // Apply truck specifications
            this.truckSpecifications = truckSpecs
            
            // Enable route details for turn-by-turn
            routeOptions.enableRouteHandle = true
        }
        
        routingEngine?.calculateRoute(
            waypointList,
            routeOptions,
            object : CalculateRouteCallback {
                override fun onRouteCalculated(
                    routingError: RoutingError?,
                    routes: List<Route>?
                ) {
                    if (routingError != null) {
                        continuation.resumeWithException(
                            HereRoutingException(routingError)
                        )
                    } else if (routes != null && routes.isNotEmpty()) {
                        continuation.resume(
                            CalculateRouteResult(
                                routes = routes,
                                error = null
                            )
                        )
                    } else {
                        continuation.resumeWithException(
                            HereRoutingException(RoutingError.NO_ROUTE_FOUND)
                        )
                    }
                }
            }
        )
        
        continuation.invokeOnCancellation {
            // Cancel routing calculation if needed
        }
    }
    
    /**
     * Calculate standard car route using HERE SDK.
     * Used when Pro tier user toggles to car mode.
     * 
     * @param origin Starting location
     * @param destination End location
     * @param waypoints Optional waypoints
     * @return HERE route result
     */
    suspend fun calculateCarRoute(
        origin: GeoCoordinates,
        destination: GeoCoordinates,
        waypoints: List<GeoCoordinates> = emptyList()
    ): CalculateRouteResult = suspendCancellableCoroutine { continuation ->
        
        val waypointList = mutableListOf<Waypoint>().apply {
            add(Waypoint(origin))
            waypoints.forEach { add(Waypoint(it)) }
            add(Waypoint(destination))
        }
        
        val carOptions = CarOptions().apply {
            routeOptions.apply {
                alternatives = 3
                optimizationMode = OptimizationMode.FASTEST
                enableRouteHandle = true
            }
        }
        
        routingEngine?.calculateRoute(
            waypointList,
            carOptions,
            object : CalculateRouteCallback {
                override fun onRouteCalculated(
                    routingError: RoutingError?,
                    routes: List<Route>?
                ) {
                    if (routingError != null) {
                        continuation.resumeWithException(
                            HereRoutingException(routingError)
                        )
                    } else if (routes != null && routes.isNotEmpty()) {
                        continuation.resume(
                            CalculateRouteResult(
                                routes = routes,
                                error = null
                            )
                        )
                    } else {
                        continuation.resumeWithException(
                            HereRoutingException(RoutingError.NO_ROUTE_FOUND)
                        )
                    }
                }
            }
        )
        
        continuation.invokeOnCancellation {
            // Cancel routing calculation if needed
        }
    }
    
    /**
     * Create truck specifications from user input.
     * Helper method for building TruckSpecifications object.
     * 
     * @param heightMeters Truck height in meters
     * @param weightKg Truck gross weight in kilograms
     * @param lengthMeters Truck length in meters
     * @param widthMeters Truck width in meters
     * @param axleCount Number of axles
     * @param trailerCount Number of trailers
     * @param hazardousGoods List of hazardous material types
     * @return TruckSpecifications object
     */
    fun createTruckSpecs(
        heightMeters: Double,
        weightKg: Int,
        lengthMeters: Double,
        widthMeters: Double,
        axleCount: Int,
        trailerCount: Int = 0,
        hazardousGoods: List<HazardousGood> = emptyList()
    ): TruckSpecifications {
        return TruckSpecifications().apply {
            grossWeightInKilograms = weightKg
            heightInMeters = heightMeters
            lengthInMeters = lengthMeters
            widthInMeters = widthMeters
            axleCount = axleCount
            trailerCount = trailerCount
            if (hazardousGoods.isNotEmpty()) {
                this.hazardousGoods = hazardousGoods
            }
        }
    }
    
    /**
     * Cleanup resources.
     */
    fun shutdown() {
        routingEngine = null
    }
}

/**
 * HERE routing result wrapper.
 */
data class CalculateRouteResult(
    val routes: List<Route>,
    val error: RoutingError?
)

/**
 * HERE routing exception wrapper.
 */
class HereRoutingException(
    val routingError: RoutingError,
    message: String = "HERE routing error: ${routingError.name}"
) : Exception(message)
