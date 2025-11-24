package com.gemnav.core.navigation

import android.util.Log
import com.gemnav.core.feature.FeatureGate
import com.gemnav.core.shim.SafeModeManager
import com.gemnav.data.navigation.*
import com.gemnav.data.route.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlin.math.*

/**
 * NavigationEngine - Core turn-by-turn navigation logic.
 * 
 * Responsibilities:
 * - Track progress along route
 * - Advance through navigation steps
 * - Detect off-route conditions
 * - Emit navigation state updates
 * - Coordinate with TTS for voice guidance
 */
class NavigationEngine {
    
    companion object {
        private const val TAG = "NavigationEngine"
        
        // Distance thresholds (meters)
        private const val STEP_COMPLETION_RADIUS = 30.0
        private const val APPROACHING_THRESHOLD = 150.0
        private const val OFF_ROUTE_THRESHOLD = 50.0
        private const val SEVERE_OFF_ROUTE_THRESHOLD = 150.0
        
        // Timing
        private const val ARRIVAL_RADIUS = 25.0
        
        // Earth radius for Haversine (meters)
        private const val EARTH_RADIUS_METERS = 6371000.0
    }
    
    private val _navigationState = MutableStateFlow<NavigationState>(NavigationState.Idle)
    val navigationState: StateFlow<NavigationState> = _navigationState
    
    private val _navigationEvents = MutableSharedFlow<NavigationEvent>(extraBufferCapacity = 10)
    val navigationEvents: SharedFlow<NavigationEvent> = _navigationEvents
    
    private var currentRoute: NavRoute? = null
    private var currentStepIndex: Int = 0
    private var totalDistanceTraveled: Double = 0.0
    private var navigationStartTime: Long = 0L
    private var lastLocation: LatLng? = null
    private var isActive: Boolean = false
    
    private val tts = NavigationTts()
    
    /**
     * Start navigation with a prepared route.
     */
    fun startNavigation(route: NavRoute): Boolean {
        // Safety checks
        if (SafeModeManager.isSafeModeEnabled()) {
            Log.w(TAG, "Navigation blocked - SafeMode active")
            _navigationState.value = NavigationState.Blocked("Safe Mode is active")
            return false
        }
        
        if (!FeatureGate.areInAppMapsEnabled()) {
            Log.w(TAG, "Navigation blocked - Free tier")
            _navigationState.value = NavigationState.Blocked("Navigation requires Plus or Pro subscription")
            return false
        }
        
        if (route.steps.isEmpty()) {
            Log.w(TAG, "Cannot start navigation - no steps")
            _navigationState.value = NavigationState.Blocked("Invalid route - no navigation steps")
            return false
        }
        
        currentRoute = route
        currentStepIndex = 0
        totalDistanceTraveled = 0.0
        navigationStartTime = System.currentTimeMillis()
        lastLocation = null
        isActive = true
        
        val firstStep = route.steps.first()
        val nextStep = route.steps.getOrNull(1)
        
        _navigationState.value = NavigationState.Navigating(
            currentStep = firstStep,
            nextStep = nextStep,
            currentStepIndex = 0,
            totalSteps = route.steps.size,
            progressPct = 0f,
            distanceToNextMeters = firstStep.distanceMeters,
            distanceRemainingMeters = route.totalDistanceMeters,
            etaSeconds = route.totalDurationSeconds
        )
        
        _navigationEvents.tryEmit(NavigationEvent.NavigationStarted)
        tts.speak("Navigation started. ${firstStep.instruction}")
        
        Log.i(TAG, "Navigation started with ${route.steps.size} steps")
        return true
    }

    /**
     * Update navigation with new location.
     * This is the main navigation loop entry point.
     */
    fun updateLocation(location: LatLng) {
        if (!isActive) return
        
        val route = currentRoute ?: return
        val steps = route.steps
        
        if (currentStepIndex >= steps.size) {
            handleArrival()
            return
        }
        
        // Track distance traveled
        lastLocation?.let { last ->
            totalDistanceTraveled += haversineDistance(last, location)
        }
        lastLocation = location
        
        val currentStep = steps[currentStepIndex]
        val distanceToStep = haversineDistance(location, currentStep.location)
        
        // Check for off-route
        val distanceFromRoute = calculateDistanceFromRoute(location, route.polylineCoordinates)
        if (distanceFromRoute > OFF_ROUTE_THRESHOLD) {
            handleOffRoute(location, distanceFromRoute)
            return
        }
        
        // Check if approaching destination
        if (currentStepIndex == steps.size - 1 && distanceToStep < ARRIVAL_RADIUS) {
            handleArrival()
            return
        }
        
        // Check if we should advance to next step
        if (distanceToStep < STEP_COMPLETION_RADIUS && currentStepIndex < steps.size - 1) {
            advanceToNextStep()
            return
        }
        
        // Check if approaching next step
        if (distanceToStep < APPROACHING_THRESHOLD) {
            val approaching = NavigationEvent.ApproachingStep(currentStep, distanceToStep)
            _navigationEvents.tryEmit(approaching)
        }
        
        // Update state with current progress
        updateNavigatingState(location, distanceToStep)
    }
    
    /**
     * Advance to the next navigation step.
     */
    private fun advanceToNextStep() {
        val route = currentRoute ?: return
        currentStepIndex++
        
        if (currentStepIndex >= route.steps.size) {
            handleArrival()
            return
        }
        
        val newStep = route.steps[currentStepIndex]
        val nextStep = route.steps.getOrNull(currentStepIndex + 1)
        
        _navigationEvents.tryEmit(NavigationEvent.StepChanged(newStep, currentStepIndex))
        tts.speak(newStep.instruction)
        
        Log.d(TAG, "Advanced to step ${currentStepIndex + 1}/${route.steps.size}: ${newStep.instruction}")
    }
    
    /**
     * Update the Navigating state with current progress.
     */
    private fun updateNavigatingState(location: LatLng, distanceToNext: Double) {
        val route = currentRoute ?: return
        val currentStep = route.steps[currentStepIndex]
        val nextStep = route.steps.getOrNull(currentStepIndex + 1)
        
        // Calculate remaining distance
        var distanceRemaining = distanceToNext
        for (i in (currentStepIndex + 1) until route.steps.size) {
            distanceRemaining += route.steps[i].distanceMeters
        }
        
        // Calculate progress
        val progressPct = (totalDistanceTraveled / route.totalDistanceMeters).toFloat().coerceIn(0f, 1f)
        
        // Estimate ETA
        val elapsedSeconds = (System.currentTimeMillis() - navigationStartTime) / 1000
        val averageSpeed = if (elapsedSeconds > 0) totalDistanceTraveled / elapsedSeconds else 10.0
        val etaSeconds = if (averageSpeed > 0) (distanceRemaining / averageSpeed).toLong() else route.totalDurationSeconds
        
        // Calculate bearing to next step
        val bearing = calculateBearing(location, currentStep.location)
        
        _navigationState.value = NavigationState.Navigating(
            currentStep = currentStep,
            nextStep = nextStep,
            currentStepIndex = currentStepIndex,
            totalSteps = route.steps.size,
            progressPct = progressPct,
            distanceToNextMeters = distanceToNext,
            distanceRemainingMeters = distanceRemaining,
            etaSeconds = etaSeconds,
            currentBearing = bearing
        )
    }

    /**
     * Handle off-route condition.
     */
    private fun handleOffRoute(location: LatLng, deviation: Double) {
        Log.w(TAG, "Off route detected: ${deviation}m deviation")
        
        val reason = when {
            deviation > SEVERE_OFF_ROUTE_THRESHOLD -> "Significantly off route"
            else -> "Off route - recalculating"
        }
        
        _navigationState.value = NavigationState.OffRoute(
            reason = reason,
            deviationMeters = deviation,
            lastKnownLocation = location
        )
        
        _navigationEvents.tryEmit(NavigationEvent.OffRouteDetected(deviation))
        tts.speak("Off route. Recalculating.")
    }
    
    /**
     * Handle arrival at destination.
     */
    private fun handleArrival() {
        val route = currentRoute
        val elapsedSeconds = (System.currentTimeMillis() - navigationStartTime) / 1000
        val destinationName = route?.steps?.lastOrNull()?.streetName
        
        isActive = false
        
        _navigationState.value = NavigationState.Finished(
            totalDistanceTraveled = totalDistanceTraveled,
            totalTimeSeconds = elapsedSeconds,
            destinationName = destinationName
        )
        
        _navigationEvents.tryEmit(NavigationEvent.Arrived(destinationName))
        tts.speak("You have arrived at your destination.")
        
        Log.i(TAG, "Navigation finished. Traveled: ${totalDistanceTraveled}m in ${elapsedSeconds}s")
    }
    
    /**
     * Stop navigation.
     */
    fun stopNavigation() {
        if (!isActive) return
        
        isActive = false
        currentRoute = null
        currentStepIndex = 0
        
        _navigationState.value = NavigationState.Idle
        _navigationEvents.tryEmit(NavigationEvent.NavigationStopped)
        
        Log.i(TAG, "Navigation stopped by user")
    }
    
    /**
     * Request route recalculation.
     */
    fun requestRecalc() {
        if (!isActive) return
        
        _navigationState.value = NavigationState.Recalculating("User requested recalculation")
        tts.speak("Recalculating route.")
        
        // The ViewModel will handle the actual recalculation
        // and call startNavigation() with the new route
    }
    
    /**
     * Update route after recalculation.
     */
    fun updateRoute(newRoute: NavRoute) {
        if (!isActive) return
        
        currentRoute = newRoute
        currentStepIndex = 0
        
        val firstStep = newRoute.steps.firstOrNull() ?: return
        val nextStep = newRoute.steps.getOrNull(1)
        
        _navigationState.value = NavigationState.Navigating(
            currentStep = firstStep,
            nextStep = nextStep,
            currentStepIndex = 0,
            totalSteps = newRoute.steps.size,
            progressPct = 0f,
            distanceToNextMeters = firstStep.distanceMeters,
            distanceRemainingMeters = newRoute.totalDistanceMeters,
            etaSeconds = newRoute.totalDurationSeconds
        )
        
        _navigationEvents.tryEmit(NavigationEvent.RouteRecalculated)
        tts.speak("Route updated. ${firstStep.instruction}")
    }
    
    /**
     * Check if navigation is currently active.
     */
    fun isNavigating(): Boolean = isActive

    // ==================== GEOMETRY UTILITIES ====================
    
    /**
     * Calculate Haversine distance between two points in meters.
     */
    private fun haversineDistance(p1: LatLng, p2: LatLng): Double {
        val lat1 = Math.toRadians(p1.latitude)
        val lat2 = Math.toRadians(p2.latitude)
        val dLat = Math.toRadians(p2.latitude - p1.latitude)
        val dLon = Math.toRadians(p2.longitude - p1.longitude)
        
        val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        val c = 2 * asin(sqrt(a))
        
        return EARTH_RADIUS_METERS * c
    }
    
    /**
     * Calculate bearing from p1 to p2 in degrees.
     */
    private fun calculateBearing(p1: LatLng, p2: LatLng): Float {
        val lat1 = Math.toRadians(p1.latitude)
        val lat2 = Math.toRadians(p2.latitude)
        val dLon = Math.toRadians(p2.longitude - p1.longitude)
        
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        
        var bearing = Math.toDegrees(atan2(y, x))
        bearing = (bearing + 360) % 360
        
        return bearing.toFloat()
    }
    
    /**
     * Calculate minimum distance from a point to a polyline.
     */
    private fun calculateDistanceFromRoute(point: LatLng, polyline: List<LatLng>): Double {
        if (polyline.isEmpty()) return Double.MAX_VALUE
        if (polyline.size == 1) return haversineDistance(point, polyline[0])
        
        var minDistance = Double.MAX_VALUE
        
        for (i in 0 until polyline.size - 1) {
            val segmentStart = polyline[i]
            val segmentEnd = polyline[i + 1]
            val distance = pointToLineSegmentDistance(point, segmentStart, segmentEnd)
            if (distance < minDistance) {
                minDistance = distance
            }
        }
        
        return minDistance
    }
    
    /**
     * Calculate distance from point to line segment.
     */
    private fun pointToLineSegmentDistance(point: LatLng, lineStart: LatLng, lineEnd: LatLng): Double {
        val lineLength = haversineDistance(lineStart, lineEnd)
        if (lineLength < 1.0) {
            return haversineDistance(point, lineStart)
        }
        
        // Project point onto line segment
        val dx = lineEnd.longitude - lineStart.longitude
        val dy = lineEnd.latitude - lineStart.latitude
        val px = point.longitude - lineStart.longitude
        val py = point.latitude - lineStart.latitude
        
        var t = (px * dx + py * dy) / (dx * dx + dy * dy)
        t = t.coerceIn(0.0, 1.0)
        
        val nearestLat = lineStart.latitude + t * dy
        val nearestLng = lineStart.longitude + t * dx
        
        return haversineDistance(point, LatLng(nearestLat, nearestLng))
    }
    
    /**
     * Get current navigation step (if navigating).
     */
    fun getCurrentStep(): NavStep? {
        return currentRoute?.steps?.getOrNull(currentStepIndex)
    }
    
    /**
     * Get next navigation step (if available).
     */
    fun getNextStep(): NavStep? {
        return currentRoute?.steps?.getOrNull(currentStepIndex + 1)
    }
    
    /**
     * Get current route.
     */
    fun getCurrentRoute(): NavRoute? = currentRoute
}
