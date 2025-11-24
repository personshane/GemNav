package com.gemnav.app.ui.route

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemnav.app.models.Destination
import com.gemnav.core.feature.FeatureGate
import com.gemnav.core.here.TruckConfig
import com.gemnav.core.shim.HereShim
import com.gemnav.core.shim.MapsShim
import com.gemnav.data.ai.*
import com.gemnav.data.route.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * RouteDetailsViewModel - Handles routing functionality with feature gating.
 * Supports both regular car routing (Google Maps) and commercial truck routing (HERE).
 */
class RouteDetailsViewModel : ViewModel() {
    
    companion object {
        private const val TAG = "RouteDetailsViewModel"
    }
    
    sealed class RouteState {
        object Idle : RouteState()
        object Loading : RouteState()
        data class Success(val route: RouteInfo) : RouteState()
        data class Error(val message: String) : RouteState()
    }
    
    data class RouteInfo(
        val distanceMeters: Long,
        val durationSeconds: Long,
        val isTruckRoute: Boolean,
        val isFallback: Boolean,
        val warnings: List<String>,
        val steps: List<String>
    )
    
    private val _routeState = MutableStateFlow<RouteState>(RouteState.Idle)
    val routeState: StateFlow<RouteState> = _routeState
    
    private val _origin = MutableStateFlow<Destination?>(null)
    val origin: StateFlow<Destination?> = _origin
    
    private val _destination = MutableStateFlow<Destination?>(null)
    val destination: StateFlow<Destination?> = _destination
    
    private val _waypoints = MutableStateFlow<List<Destination>>(emptyList())
    val waypoints: StateFlow<List<Destination>> = _waypoints
    
    private val _isTruckMode = MutableStateFlow(false)
    val isTruckMode: StateFlow<Boolean> = _isTruckMode
    
    private val _truckSpecs = MutableStateFlow<HereShim.TruckSpecs?>(null)
    val truckSpecs: StateFlow<HereShim.TruckSpecs?> = _truckSpecs
    
    private val _featureSummary = MutableStateFlow(FeatureGate.getFeatureSummary())
    val featureSummary: StateFlow<FeatureGate.FeatureSummary> = _featureSummary
    
    // ==================== LOCATION TRACKING (MP-018) ====================
    
    private val _currentUserLocation = MutableStateFlow<LatLng?>(null)
    val currentUserLocation: StateFlow<LatLng?> = _currentUserLocation
    
    private val _isNavigating = MutableStateFlow(false)
    val isNavigating: StateFlow<Boolean> = _isNavigating
    
    /**
     * Update current user location from LocationViewModel.
     * Called by UI when location changes.
     */
    fun updateUserLocation(location: LatLng?) {
        _currentUserLocation.value = location
        
        // TODO MP-017: If navigation active, trigger:
        // - upcomingStepDistance calculation
        // - lane guidance updates
        // - route deviation detection
        // - recalculation triggers
        if (_isNavigating.value && location != null) {
            checkNavigationProgress(location)
        }
    }
    
    /**
     * Check navigation progress against current route.
     * TODO MP-017: Full turn-by-turn implementation
     */
    private fun checkNavigationProgress(location: LatLng) {
        // Stub for MP-017 turn-by-turn navigation
        Log.d(TAG, "Navigation progress check at: ${location.latitude}, ${location.longitude}")
        // TODO: Calculate distance to next step
        // TODO: Check if off-route (> 50m deviation)
        // TODO: Trigger recalculation if needed
    }
    
    /**
     * Start turn-by-turn navigation.
     * TODO MP-017: Full implementation
     */
    fun startNavigation() {
        _isNavigating.value = true
        Log.i(TAG, "Navigation started")
        // TODO: Initialize TTS
        // TODO: Start step announcements
    }
    
    /**
     * Stop navigation.
     */
    fun stopNavigation() {
        _isNavigating.value = false
        Log.i(TAG, "Navigation stopped")
    }
    
    /**
     * Set route origin.
     */
    fun setOrigin(dest: Destination) {
        _origin.value = dest
    }
    
    /**
     * Set route destination.
     */
    fun setDestination(dest: Destination) {
        _destination.value = dest
    }
    
    /**
     * Add waypoint.
     * Gated by FeatureGate.areMultiWaypointEnabled()
     */
    fun addWaypoint(waypoint: Destination) {
        if (!FeatureGate.areMultiWaypointEnabled()) {
            Log.d(TAG, "Multi-waypoint blocked - Plus/Pro required")
            // TODO: Show upgrade prompt
            return
        }
        
        val current = _waypoints.value.toMutableList()
        if (current.size < 10) { // Max 10 waypoints per PRD
            current.add(waypoint)
            _waypoints.value = current
        }
    }
    
    /**
     * Set truck mode.
     * Gated by FeatureGate.areCommercialRoutingFeaturesEnabled()
     */
    fun setTruckMode(enabled: Boolean) {
        if (enabled && !FeatureGate.areCommercialRoutingFeaturesEnabled()) {
            Log.d(TAG, "Truck mode blocked - Pro required or SafeMode active")
            // TODO: Show upgrade prompt or safe mode warning
            return
        }
        
        _isTruckMode.value = enabled
        HereShim.setTruckMode(enabled)
    }
    
    /**
     * Set truck specifications.
     */
    fun setTruckSpecs(specs: HereShim.TruckSpecs) {
        if (!FeatureGate.areCommercialRoutingFeaturesEnabled()) {
            Log.d(TAG, "Truck specs blocked - Pro required")
            return
        }
        
        _truckSpecs.value = specs
        HereShim.setTruckSpecs(specs)
    }
    
    /**
     * Calculate route.
     */
    fun calculateRoute() {
        val orig = _origin.value
        val dest = _destination.value
        
        if (orig == null || dest == null) {
            _routeState.value = RouteState.Error("Please set origin and destination")
            return
        }
        
        _routeState.value = RouteState.Loading
        
        viewModelScope.launch {
            try {
                val route = if (_isTruckMode.value) {
                    calculateTruckRoute(orig, dest)
                } else {
                    calculateCarRoute(orig, dest)
                }
                
                if (route != null) {
                    _routeState.value = RouteState.Success(route)
                } else {
                    _routeState.value = RouteState.Error("Could not calculate route")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Route calculation failed", e)
                _routeState.value = RouteState.Error("Route calculation failed")
            }
        }
    }
    
    /**
     * Calculate truck route using HERE SDK.
     * Gated by FeatureGate.areCommercialRoutingFeaturesEnabled()
     */
    private fun calculateTruckRoute(origin: Destination, dest: Destination): RouteInfo? {
        if (!FeatureGate.areCommercialRoutingFeaturesEnabled()) {
            Log.d(TAG, "Truck routing blocked - falling back to car route")
            // TODO: Notify user about fallback
            return calculateCarRoute(origin, dest)
        }
        
        Log.d(TAG, "Calculating truck route: ${origin.name} -> ${dest.name}")
        
        val truckRoute = HereShim.getTruckRoute(
            origin = Pair(origin.latitude, origin.longitude),
            destination = Pair(dest.latitude, dest.longitude),
            specs = _truckSpecs.value
        )
        
        return if (truckRoute != null) {
            RouteInfo(
                distanceMeters = truckRoute.distanceMeters,
                durationSeconds = truckRoute.durationSeconds,
                isTruckRoute = true,
                isFallback = truckRoute.isFallback,
                warnings = truckRoute.warnings,
                steps = truckRoute.steps
            )
        } else {
            Log.w(TAG, "Truck route failed - falling back to car route")
            calculateCarRoute(origin, dest)
        }
    }
    
    /**
     * Calculate car route using Google Maps SDK.
     * Gated by FeatureGate.areInAppMapsEnabled()
     */
    private fun calculateCarRoute(origin: Destination, dest: Destination): RouteInfo? {
        if (!FeatureGate.areInAppMapsEnabled()) {
            Log.d(TAG, "In-app routing blocked - would trigger Maps intent")
            // TODO: Trigger Google Maps app via intent
            return null
        }
        
        Log.d(TAG, "Calculating car route: ${origin.name} -> ${dest.name}")
        
        val mapsRoute = MapsShim.getRoute(
            origin = Pair(origin.latitude, origin.longitude),
            destination = Pair(dest.latitude, dest.longitude)
        )
        
        return if (mapsRoute != null) {
            RouteInfo(
                distanceMeters = mapsRoute.distanceMeters,
                durationSeconds = mapsRoute.durationSeconds,
                isTruckRoute = false,
                isFallback = false,
                warnings = emptyList(),
                steps = mapsRoute.steps
            )
        } else {
            null
        }
    }
    
    /**
     * Clear route.
     */
    fun clearRoute() {
        _routeState.value = RouteState.Idle
        _origin.value = null
        _destination.value = null
        _waypoints.value = emptyList()
    }
    
    /**
     * Refresh feature state.
     */
    fun refreshFeatureState() {
        _featureSummary.value = FeatureGate.getFeatureSummary()
    }
    
    // ==================== New Truck Route API (MP-013) ====================
    
    private val _truckRouteState = MutableStateFlow<TruckRouteState>(TruckRouteState.Idle)
    val truckRouteState: StateFlow<TruckRouteState> = _truckRouteState
    
    private val _currentTruckConfig = MutableStateFlow(TruckConfig())
    val currentTruckConfig: StateFlow<TruckConfig> = _currentTruckConfig
    
    /**
     * Update truck configuration.
     */
    fun updateTruckConfig(config: TruckConfig) {
        _currentTruckConfig.value = config
    }
    
    /**
     * Request truck route using new sealed class API.
     * Enforces Pro-tier gating through HereShim.
     * 
     * @param startLat Origin latitude
     * @param startLng Origin longitude  
     * @param endLat Destination latitude
     * @param endLng Destination longitude
     */
    fun requestTruckRoute(startLat: Double, startLng: Double, endLat: Double, endLng: Double) {
        // Quick gate check for UI feedback
        if (!FeatureGate.areCommercialRoutingFeaturesEnabled()) {
            _truckRouteState.value = TruckRouteState.Error(
                message = "Truck routing requires Pro subscription",
                code = TruckRouteError.FEATURE_NOT_ENABLED
            )
            return
        }
        
        _truckRouteState.value = TruckRouteState.Loading
        
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                HereShim.requestTruckRoute(
                    start = LatLng(startLat, startLng),
                    end = LatLng(endLat, endLng),
                    truckConfig = _currentTruckConfig.value
                )
            }
            
            _truckRouteState.value = when (result) {
                is TruckRouteResult.Success -> {
                    Log.i(TAG, "Truck route success: ${result.route.distanceMeters}m")
                    TruckRouteState.Success(result.route)
                }
                is TruckRouteResult.Failure -> {
                    Log.w(TAG, "Truck route failed: ${result.errorMessage}")
                    TruckRouteState.Error(result.errorMessage, result.errorCode)
                }
            }
        }
    }
    
    /**
     * Request truck route using Destination objects.
     */
    fun requestTruckRoute(start: Destination, end: Destination) {
        requestTruckRoute(start.latitude, start.longitude, end.latitude, end.longitude)
    }
    
    /**
     * Check if Pro tier features are available.
     */
    fun isProTier(): Boolean = FeatureGate.areCommercialRoutingFeaturesEnabled()
    
    /**
     * Check if Plus tier features (in-app maps) are available.
     */
    fun isPlusTier(): Boolean = FeatureGate.areInAppMapsEnabled()
    
    /**
     * Callback when Google Maps surface is ready.
     * Used for Plus tier map initialization.
     */
    fun onGoogleMapReady() {
        Log.i(TAG, "Google Maps surface ready")
        // TODO: Trigger camera move to destination
        // TODO: Request route polyline from Gemini
    }
    
    /**
     * Callback when Google Maps fails to initialize.
     */
    fun onGoogleMapError(error: String) {
        Log.w(TAG, "Google Maps error: $error")
        // Silently handled - UI shows error state
    }
    
    // ==================== AI ROUTE INTEGRATION (MP-016) ====================
    
    private val _aiRouteState = MutableStateFlow<AiRouteState>(AiRouteState.Idle)
    val aiRouteState: StateFlow<AiRouteState> = _aiRouteState
    
    /**
     * Apply AI-generated route suggestion to actual routing.
     * Routes to appropriate engine based on mode.
     */
    fun applyAiRouteSuggestion(suggestion: AiRouteSuggestion) {
        Log.i(TAG, "Applying AI route: ${suggestion.destinationName} (mode: ${suggestion.mode})")
        
        // Set origin and destination from suggestion
        _origin.value = Destination(
            id = "ai_origin",
            name = "Current Location",
            address = "",
            latitude = suggestion.origin.latitude,
            longitude = suggestion.origin.longitude
        )
        
        _destination.value = Destination(
            id = "ai_dest",
            name = suggestion.destinationName,
            address = suggestion.notes ?: "",
            latitude = suggestion.destination.latitude,
            longitude = suggestion.destination.longitude
        )
        
        // Set waypoints if any
        _waypoints.value = suggestion.waypoints.mapIndexed { index, latLng ->
            Destination(
                id = "ai_waypoint_$index",
                name = "Waypoint ${index + 1}",
                address = "",
                latitude = latLng.latitude,
                longitude = latLng.longitude
            )
        }
        
        // Route based on mode
        when (suggestion.mode) {
            AiRouteMode.CAR -> {
                _isTruckMode.value = false
                // Trigger car routing through existing flow
                calculateRoute()
            }
            AiRouteMode.TRUCK -> {
                if (FeatureGate.areCommercialRoutingFeaturesEnabled()) {
                    _isTruckMode.value = true
                    // Use HERE truck routing
                    requestTruckRoute(
                        suggestion.origin.latitude, suggestion.origin.longitude,
                        suggestion.destination.latitude, suggestion.destination.longitude
                    )
                } else {
                    // Fall back to car routing if truck not available
                    Log.w(TAG, "Truck mode requested but not available - falling back to car")
                    _isTruckMode.value = false
                    calculateRoute()
                }
            }
        }
    }
    
    /**
     * Handle AI route result from Search/Voice ViewModels.
     */
    fun onAiRouteResult(result: AiRouteResult) {
        when (result) {
            is AiRouteResult.Success -> {
                Log.i(TAG, "Received AI route success: ${result.suggestion.destinationName}")
                _aiRouteState.value = AiRouteState.Success(result.suggestion)
                applyAiRouteSuggestion(result.suggestion)
            }
            is AiRouteResult.Failure -> {
                Log.w(TAG, "Received AI route failure: ${result.reason}")
                _aiRouteState.value = AiRouteState.Error(result.reason)
            }
        }
    }
    
    /**
     * Clear AI route state.
     */
    fun clearAiRouteState() {
        _aiRouteState.value = AiRouteState.Idle
    }
    
    /**
     * Clear truck route state.
     */
    fun clearTruckRoute() {
        _truckRouteState.value = TruckRouteState.Idle
    }
}
