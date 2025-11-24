package com.gemnav.core.shim

import android.util.Log
import com.gemnav.core.feature.FeatureGate
import com.gemnav.core.here.HereEngineManager
import com.gemnav.core.here.TruckConfig
import com.gemnav.data.navigation.*
import com.gemnav.data.route.*

/**
 * HereShim - Safe wrapper for HERE SDK commercial truck routing.
 * Enforces Pro-tier gating, handles SDK exceptions, validates truck
 * restrictions, and provides fallback routes when HERE fails.
 * 
 * All public methods check SafeMode and FeatureGate before proceeding.
 */
object HereShim {
    private const val TAG = "HereShim"
    
    private var isInitialized = false
    private var lastError: Exception? = null
    private var isTruckMode = true
    
    /**
     * Initialize the HERE shim layer.
     * @return true if initialization successful, false otherwise
     */
    fun initialize(): Boolean {
        return try {
            logInfo("HereShim initialized")
            isInitialized = true
            true
        } catch (e: Exception) {
            logError("Failed to initialize HereShim", e)
            lastError = e
            SafeModeManager.reportFailure("HereShim", e)
            false
        }
    }
    
    /**
     * Request a truck-legal route between two points.
     * 
     * Enforces:
     * - SafeMode check (blocks if active)
     * - FeatureGate check (Pro tier only)
     * - SDK initialization check
     * - TruckConfig validation
     * 
     * @param start Origin coordinates (lat, lng)
     * @param end Destination coordinates (lat, lng)
     * @param truckConfig Vehicle specifications
     * @return TruckRouteResult (Success or Failure)
     */
    fun requestTruckRoute(
        start: LatLng,
        end: LatLng,
        truckConfig: TruckConfig
    ): TruckRouteResult {
        // 1. SafeMode check
        if (SafeModeManager.isSafeModeEnabled()) {
            logWarning("Truck routing blocked - SafeMode is active")
            return TruckRouteResult.Failure(
                errorMessage = "Safe Mode is active. Truck routing unavailable.",
                errorCode = TruckRouteError.SAFE_MODE_ACTIVE
            )
        }
        
        // 2. FeatureGate check - Pro tier only
        if (!FeatureGate.areCommercialRoutingFeaturesEnabled()) {
            logWarning("Truck routing blocked - not Pro tier")
            return TruckRouteResult.Failure(
                errorMessage = "Truck routing requires Pro subscription.",
                errorCode = TruckRouteError.FEATURE_NOT_ENABLED
            )
        }
        
        // 3. SDK initialization check
        if (!HereEngineManager.isReady()) {
            logWarning("HERE SDK not initialized")
            return TruckRouteResult.Failure(
                errorMessage = "HERE SDK not available.",
                errorCode = TruckRouteError.SDK_NOT_INITIALIZED
            )
        }
        
        // 4. Validate truck config
        if (!truckConfig.isValid()) {
            logWarning("Invalid truck configuration")
            return TruckRouteResult.Failure(
                errorMessage = "Invalid truck specifications.",
                errorCode = TruckRouteError.INVALID_TRUCK_CONFIG
            )
        }
        
        // 5. Validate coordinates
        if (!isValidCoordinate(start) || !isValidCoordinate(end)) {
            logWarning("Invalid coordinates: start=$start, end=$end")
            return TruckRouteResult.Failure(
                errorMessage = "Invalid route coordinates.",
                errorCode = TruckRouteError.INVALID_COORDINATES
            )
        }
        
        return try {
            logInfo("Requesting truck route: $start -> $end")
            logInfo("Truck config: ${truckConfig.weightKg}kg, ${truckConfig.heightCm}cm (safe: ${truckConfig.getSafeHeight()}cm)")
            
            // TODO: Actual HERE SDK routing call
            // val routingEngine = HereEngineManager.getRoutingEngine()
            // val truckOptions = HereEngineManager.createTruckOptions(truckConfig)
            // val waypoints = listOf(
            //     Waypoint(GeoCoordinates(start.latitude, start.longitude)),
            //     Waypoint(GeoCoordinates(end.latitude, end.longitude))
            // )
            // val route = routingEngine.calculateRoute(waypoints, TruckOptions(truckOptions))
            
            // STUB: Return mock successful route for pipeline testing
            val mockRoute = createMockRoute(start, end, truckConfig)
            logInfo("Truck route calculated: ${mockRoute.distanceMeters}m, ${mockRoute.durationSeconds}s")
            
            TruckRouteResult.Success(mockRoute)
        } catch (e: Exception) {
            logError("Truck route calculation failed", e)
            lastError = e
            SafeModeManager.reportFailure("HereShim.requestTruckRoute", e)
            
            // Attempt fallback
            getFallbackRoute(start, end, truckConfig)
        }
    }
    
    /**
     * Create mock route data for pipeline testing.
     * TODO: Remove when real HERE SDK integration complete
     */
    private fun createMockRoute(start: LatLng, end: LatLng, config: TruckConfig): TruckRouteData {
        // Calculate approximate distance (simplified)
        val distanceKm = calculateApproxDistance(start, end)
        val distanceMeters = (distanceKm * 1000).toLong()
        
        // Estimate duration (average 60 km/h for trucks)
        val durationSeconds = ((distanceKm / 60.0) * 3600).toLong()
        
        // Generate mock warnings based on truck config
        val warnings = mutableListOf<TruckWarning>()
        
        if (config.heightCm > 380) {
            warnings.add(TruckWarning(
                type = WarningType.LOW_BRIDGE,
                location = LatLng(
                    (start.latitude + end.latitude) / 2,
                    (start.longitude + end.longitude) / 2
                ),
                description = "Low bridge ahead - clearance 4.0m",
                severity = if (config.heightCm > 400) WarningSeverity.WARNING else WarningSeverity.INFO
            ))
        }
        
        if (config.weightKg > 40000) {
            warnings.add(TruckWarning(
                type = WarningType.WEIGHT_LIMIT,
                location = end,
                description = "Weight restriction - 40t limit",
                severity = WarningSeverity.WARNING
            ))
        }
        
        return TruckRouteData(
            distanceMeters = distanceMeters,
            durationSeconds = durationSeconds,
            polylineCoordinates = listOf(start, end), // Simplified
            warnings = warnings,
            tollInfo = TollInfo(
                estimatedCost = distanceKm * 0.15, // ~$0.15/km estimate
                currency = "USD",
                tollPoints = (distanceKm / 50).toInt().coerceAtLeast(1)
            ),
            isFallback = false
        )
    }
    
    /**
     * Parse navigation steps from TruckRouteData.
     * Converts HERE route data to navigation steps for turn-by-turn.
     * 
     * @param routeData The truck route data from HERE SDK
     * @return List of navigation steps
     */
    fun parseSteps(routeData: TruckRouteData): List<NavStep> {
        if (routeData.polylineCoordinates.size < 2) {
            logWarning("Cannot parse steps - insufficient route points")
            return emptyList()
        }
        
        val steps = mutableListOf<NavStep>()
        val coordinates = routeData.polylineCoordinates
        
        // Generate mock steps along the route
        // TODO: Replace with actual HERE SDK maneuver parsing
        
        // Start step
        steps.add(NavStep(
            instruction = "Start route",
            maneuverIcon = NavManeuver.DEPART,
            distanceMeters = 0.0,
            streetName = null,
            location = coordinates.first()
        ))
        
        // For real HERE SDK integration:
        // route.sections.forEach { section ->
        //     section.maneuvers.forEach { maneuver ->
        //         steps.add(NavStep(
        //             instruction = maneuver.text,
        //             maneuverIcon = mapHereManeuverAction(maneuver.action),
        //             distanceMeters = maneuver.lengthInMeters.toDouble(),
        //             streetName = maneuver.roadName,
        //             location = LatLng(maneuver.coordinates.latitude, maneuver.coordinates.longitude)
        //         ))
        //     }
        // }
        
        // Mock intermediate steps based on distance
        val totalDistance = routeData.distanceMeters.toDouble()
        val numSteps = (totalDistance / 2000).toInt().coerceIn(1, 10) // One step per ~2km
        
        for (i in 1..numSteps) {
            val progress = i.toDouble() / (numSteps + 1)
            val lat = coordinates.first().latitude + 
                (coordinates.last().latitude - coordinates.first().latitude) * progress
            val lng = coordinates.first().longitude + 
                (coordinates.last().longitude - coordinates.first().longitude) * progress
            
            val maneuver = when {
                i % 3 == 0 -> NavManeuver.RIGHT
                i % 3 == 1 -> NavManeuver.LEFT
                else -> NavManeuver.STRAIGHT
            }
            
            steps.add(NavStep(
                instruction = "${getManeuverText(maneuver)} onto Route $i",
                maneuverIcon = maneuver,
                distanceMeters = totalDistance / (numSteps + 1),
                streetName = "Route $i",
                location = LatLng(lat, lng)
            ))
        }
        
        // Arrival step
        steps.add(NavStep(
            instruction = "Arrive at destination",
            maneuverIcon = NavManeuver.ARRIVE,
            distanceMeters = 0.0,
            streetName = null,
            location = coordinates.last()
        ))
        
        logInfo("Parsed ${steps.size} navigation steps")
        return steps
    }
    
    /**
     * Map HERE SDK maneuver action to NavManeuver enum.
     * TODO: Implement actual mapping when HERE SDK integrated
     */
    private fun mapHereManeuverAction(action: String): NavManeuver {
        return when (action.lowercase()) {
            "turn_left" -> NavManeuver.LEFT
            "turn_right" -> NavManeuver.RIGHT
            "turn_slight_left" -> NavManeuver.SLIGHT_LEFT
            "turn_slight_right" -> NavManeuver.SLIGHT_RIGHT
            "turn_sharp_left" -> NavManeuver.SHARP_LEFT
            "turn_sharp_right" -> NavManeuver.SHARP_RIGHT
            "u_turn" -> NavManeuver.UTURN
            "enter_highway", "merge" -> NavManeuver.MERGE
            "exit_highway", "exit" -> NavManeuver.EXIT
            "roundabout" -> NavManeuver.ROUNDABOUT
            "ferry" -> NavManeuver.FERRY
            "arrive" -> NavManeuver.ARRIVE
            "depart" -> NavManeuver.DEPART
            else -> NavManeuver.STRAIGHT
        }
    }
    
    /**
     * Get human-readable text for a maneuver type.
     */
    private fun getManeuverText(maneuver: NavManeuver): String {
        return when (maneuver) {
            NavManeuver.STRAIGHT -> "Continue straight"
            NavManeuver.LEFT -> "Turn left"
            NavManeuver.RIGHT -> "Turn right"
            NavManeuver.SLIGHT_LEFT -> "Bear left"
            NavManeuver.SLIGHT_RIGHT -> "Bear right"
            NavManeuver.SHARP_LEFT -> "Sharp left"
            NavManeuver.SHARP_RIGHT -> "Sharp right"
            NavManeuver.UTURN -> "Make a U-turn"
            NavManeuver.MERGE -> "Merge"
            NavManeuver.EXIT -> "Take exit"
            NavManeuver.ROUNDABOUT -> "Enter roundabout"
            NavManeuver.FERRY -> "Board ferry"
            NavManeuver.ARRIVE -> "Arrive"
            NavManeuver.DEPART -> "Depart"
        }
    }
    
    /**
     * Create a NavRoute from TruckRouteData for navigation.
     */
    fun createNavRoute(routeData: TruckRouteData): NavRoute {
        return NavRoute(
            steps = parseSteps(routeData),
            polylineCoordinates = routeData.polylineCoordinates,
            totalDistanceMeters = routeData.distanceMeters.toDouble(),
            totalDurationSeconds = routeData.durationSeconds,
            isTruckRoute = true,
            isFallback = routeData.isFallback
        )
    }
    
    /**
     * Calculate approximate distance between two points (Haversine formula simplified).
     */
    private fun calculateApproxDistance(start: LatLng, end: LatLng): Double {
        val latDiff = Math.abs(start.latitude - end.latitude)
        val lngDiff = Math.abs(start.longitude - end.longitude)
        // Very rough approximation: 1 degree ≈ 111 km
        return Math.sqrt(latDiff * latDiff + lngDiff * lngDiff) * 111.0
    }
    
    /**
     * Validate coordinate is within valid range.
     */
    private fun isValidCoordinate(coord: LatLng): Boolean {
        return coord.latitude >= -90 && coord.latitude <= 90 &&
               coord.longitude >= -180 && coord.longitude <= 180
    }
    
    /**
     * Provide a fallback route when HERE SDK fails.
     */
    private fun getFallbackRoute(start: LatLng, end: LatLng, config: TruckConfig): TruckRouteResult {
        return try {
            logWarning("Using fallback route - truck restrictions NOT verified")
            
            val distanceKm = calculateApproxDistance(start, end)
            val fallbackRoute = TruckRouteData(
                distanceMeters = (distanceKm * 1000).toLong(),
                durationSeconds = ((distanceKm / 60.0) * 3600).toLong(),
                polylineCoordinates = listOf(start, end),
                warnings = listOf(
                    TruckWarning(
                        type = WarningType.OTHER,
                        location = start,
                        description = "FALLBACK ROUTE - Truck restrictions NOT verified. Use extreme caution.",
                        severity = WarningSeverity.CRITICAL
                    )
                ),
                isFallback = true
            )
            
            TruckRouteResult.Success(fallbackRoute)
        } catch (e: Exception) {
            logError("Fallback route also failed", e)
            TruckRouteResult.Failure(
                errorMessage = "Route calculation failed completely.",
                errorCode = TruckRouteError.UNKNOWN
            )
        }
    }
    
    // ==================== Legacy Methods (kept for compatibility) ====================
    
    fun setTruckSpecs(specs: TruckSpecs): Boolean {
        return try {
            if (!validateTruckSpecs(specs)) {
                logWarning("Invalid truck specs provided")
                return false
            }
            logInfo("Truck specs set: ${specs.heightCm}cm height, ${specs.weightKg}kg weight")
            true
        } catch (e: Exception) {
            logError("Error setting truck specs", e)
            lastError = e
            false
        }
    }
    
    private fun validateTruckSpecs(specs: TruckSpecs): Boolean {
        return try {
            if (specs.heightCm < 200 || specs.heightCm > 450) return false
            if (specs.weightKg < 1000 || specs.weightKg > 80000) return false
            if (specs.lengthCm < 300 || specs.lengthCm > 2500) return false
            if (specs.widthCm < 150 || specs.widthCm > 300) return false
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun getTruckRoute(
        origin: Pair<Double, Double>,
        destination: Pair<Double, Double>,
        specs: TruckSpecs? = null
    ): TruckRoute? {
        // Convert to new API
        val config = specs?.let {
            TruckConfig(
                heightCm = it.heightCm,
                widthCm = it.widthCm,
                lengthCm = it.lengthCm,
                weightKg = it.weightKg,
                axleCount = it.axleCount,
                trailerCount = it.trailerCount,
                hasHazmat = it.hasHazmat,
                hazmatClasses = it.hazmatClasses
            )
        } ?: TruckConfig()
        
        val result = requestTruckRoute(
            LatLng(origin.first, origin.second),
            LatLng(destination.first, destination.second),
            config
        )
        
        return when (result) {
            is TruckRouteResult.Success -> TruckRoute(
                distanceMeters = result.route.distanceMeters,
                durationSeconds = result.route.durationSeconds,
                warnings = result.route.warnings.map { it.description },
                isFallback = result.route.isFallback
            )
            is TruckRouteResult.Failure -> null
        }
    }
    
    fun setTruckMode(enabled: Boolean) {
        isTruckMode = enabled
        logInfo("Truck mode ${if (enabled) "enabled" else "disabled"}")
    }
    
    fun isTruckModeEnabled(): Boolean = isTruckMode
    fun isAvailable(): Boolean = isInitialized && lastError == null
    fun getLastError(): Exception? = lastError
    fun clearError() { lastError = null }
    
    private fun logInfo(message: String) = Log.i(TAG, message)
    private fun logWarning(message: String) = Log.w(TAG, message)
    private fun logError(message: String, e: Exception) = Log.e(TAG, message, e)
    
    // ==================== Legacy Data Classes ====================
    
    data class TruckSpecs(
        val heightCm: Int = 400,
        val weightKg: Int = 36000,
        val lengthCm: Int = 1800,
        val widthCm: Int = 255,
        val axleCount: Int = 5,
        val trailerCount: Int = 1,
        val hasHazmat: Boolean = false,
        val hazmatClasses: List<String> = emptyList()
    )
    
    data class TruckRoute(
        val distanceMeters: Long = 0,
        val durationSeconds: Long = 0,
        val polyline: String = "",
        val steps: List<String> = emptyList(),
        val restrictions: List<String> = emptyList(),
        val warnings: List<String> = emptyList(),
        val isFallback: Boolean = false,
        val tollCost: Double? = null
    )
    
    data class HeightRestriction(
        val latitude: Double,
        val longitude: Double,
        val clearanceHeightCm: Int,
        val description: String,
        val severity: RestrictionSeverity
    )
    
    enum class RestrictionSeverity {
        INFO, WARNING, CRITICAL
    }
}
