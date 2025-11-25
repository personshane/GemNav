package com.gemnav.app.ui.route

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemnav.app.models.Destination
import com.gemnav.core.feature.FeatureGate
import com.gemnav.core.here.TruckConfig
import com.gemnav.core.maps.google.DirectionsApiClient
import com.gemnav.core.maps.google.DirectionsResult
import com.gemnav.core.navigation.NavigationEngine
import com.gemnav.core.navigation.AiVoiceEvent
import com.gemnav.core.navigation.DetourInfo
import com.gemnav.core.navigation.DetourState
import com.gemnav.core.navigation.SelectedPoi
import com.gemnav.core.navigation.TruckPoi
import com.gemnav.core.navigation.TruckPoiState
import com.gemnav.core.navigation.TruckPoiType
import com.gemnav.core.shim.GeminiShim
import com.gemnav.core.shim.HereShim
import com.gemnav.core.shim.HereTruckPoiClient
import com.gemnav.core.shim.MapsShim
import com.gemnav.core.shim.RouteDetailsViewModelProvider
import com.gemnav.core.shim.SafeModeManager
import com.gemnav.core.subscription.TierManager
import com.gemnav.data.ai.*
import com.gemnav.data.navigation.*
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
    
    // ==================== NAVIGATION ENGINE (MP-017) ====================
    
    private val navigationEngine = NavigationEngine()
    
    val navigationState: StateFlow<NavigationState> = navigationEngine.navigationState
    val navigationEvents = navigationEngine.navigationEvents
    
    private val _currentNavRoute = MutableStateFlow<NavRoute?>(null)
    val currentNavRoute: StateFlow<NavRoute?> = _currentNavRoute
    
    // ==================== GOOGLE DIRECTIONS API (MP-019) ====================
    
    sealed class GoogleRouteState {
        object Idle : GoogleRouteState()
        object Loading : GoogleRouteState()
        data class Success(val polyline: List<LatLng>, val distanceMeters: Int, val durationSeconds: Int) : GoogleRouteState()
        data class Error(val message: String) : GoogleRouteState()
    }
    
    private val _googleRouteState = MutableStateFlow<GoogleRouteState>(GoogleRouteState.Idle)
    val googleRouteState: StateFlow<GoogleRouteState> = _googleRouteState
    
    private val _currentGooglePolyline = MutableStateFlow<List<LatLng>>(emptyList())
    val currentGooglePolyline: StateFlow<List<LatLng>> = _currentGooglePolyline
    
    // ==================== HERE ROUTE POLYLINE (MP-025) ====================
    
    private val _currentHerePolyline = MutableStateFlow<List<LatLng>>(emptyList())
    val currentHerePolyline: StateFlow<List<LatLng>> = _currentHerePolyline
    
    /**
     * MP-025: Get active HERE route polyline for truck POI filtering.
     * 
     * PRO TIER ONLY - Returns empty list for Free/Plus tiers.
     */
    fun getActiveHereRoutePolyline(): List<LatLng> {
        if (!TierManager.isPro()) {
            return emptyList()
        }
        return _currentHerePolyline.value
    }
    
    // ==================== TRUCK POI STATE (MP-025) ====================
    
    private val _truckPoiState = MutableStateFlow<TruckPoiState>(TruckPoiState.Idle)
    val truckPoiState: StateFlow<TruckPoiState> = _truckPoiState
    
    private var pendingTruckPoi: TruckPoi? = null
    
    /**
     * MP-022: Get active route polyline for along-route POI filtering.
     * 
     * PLUS TIER ONLY - Returns empty list for Free/Pro tiers.
     * - Free: Cannot use in-app maps or polylines
     * - Pro: Uses HERE SDK (Google data forbidden)
     */
    fun getActiveRoutePolyline(): List<LatLng> {
        // PLUS ONLY - Pro must not access Google polyline data
        if (!FeatureGate.areInAppMapsEnabled() || TierManager.isPro()) {
            return emptyList()
        }
        return _currentGooglePolyline.value
    }
    
    // ==================== PROVIDER REGISTRATION (MP-022/MP-025) ====================
    
    init {
        // Register polyline provider for along-route POI filtering
        RouteDetailsViewModelProvider.registerPolylineProvider { getActiveRoutePolyline() }
        RouteDetailsViewModelProvider.registerNavigationStateProvider { _isNavigating.value }
        // MP-023: Register POI selection handler for detour calculation
        RouteDetailsViewModelProvider.registerPoiSelectionHandler { poi -> onPoiSelected(poi) }
        // MP-025: Register truck POI handler and HERE polyline provider for Pro tier
        RouteDetailsViewModelProvider.registerTruckPoiSelectionHandler { poi -> onTruckPoiSelected(poi) }
        RouteDetailsViewModelProvider.registerHerePolylineProvider { getActiveHereRoutePolyline() }
        Log.d(TAG, "Registered with RouteDetailsViewModelProvider")
    }
    
    override fun onCleared() {
        super.onCleared()
        RouteDetailsViewModelProvider.unregister()
        navigationEngine.stopNavigation()
        Log.d(TAG, "Unregistered from RouteDetailsViewModelProvider")
    }
    
    // ==================== DETOUR CALCULATION (MP-023) ====================
    
    private val _detourState = MutableStateFlow<DetourState>(DetourState.Idle)
    val detourState: StateFlow<DetourState> = _detourState
    
    private var pendingPoiForDetour: SelectedPoi? = null
    
    /**
     * MP-023: Handle POI selection for detour calculation.
     * 
     * PLUS TIER ONLY - Triggers detour cost calculation for the selected POI.
     */
    fun onPoiSelected(poi: SelectedPoi) {
        // SafeMode check
        if (SafeModeManager.isSafeModeEnabled()) {
            Log.w(TAG, "POI selection blocked - SafeMode active")
            _detourState.value = DetourState.Blocked("Safe mode active - AI features disabled")
            return
        }
        
        // Tier check - PLUS only
        if (!FeatureGate.areInAppMapsEnabled()) {
            Log.w(TAG, "POI selection blocked - Free tier")
            _detourState.value = DetourState.Blocked("Detour calculation requires Plus subscription")
            // MP-024: Voice feedback for upgrade required
            RouteDetailsViewModelProvider.emitVoiceEvent(
                AiVoiceEvent.UpgradeRequired("Plus", "detour calculation")
            )
            return
        }
        
        if (TierManager.isPro()) {
            Log.w(TAG, "POI selection blocked - Pro tier uses HERE")
            _detourState.value = DetourState.Blocked("Truck-specific POI coming soon")
            // MP-024: Voice feedback for Pro tier
            RouteDetailsViewModelProvider.emitVoiceEvent(
                AiVoiceEvent.UpgradeRequired("Pro with HERE SDK", "truck-specific POI search")
            )
            return
        }
        
        pendingPoiForDetour = poi
        _detourState.value = DetourState.Calculating
        
        viewModelScope.launch {
            val detourInfo = calculateDetourInfoForPoi(poi.latLng)
            if (detourInfo != null) {
                _detourState.value = DetourState.Ready(poi, detourInfo)
                Log.i(TAG, "Detour calculated: ${detourInfo.formatDetour()}")
                // MP-024: Voice feedback for detour summary
                RouteDetailsViewModelProvider.emitVoiceEvent(
                    AiVoiceEvent.DetourSummary(
                        poiName = poi.name,
                        addedMinutes = detourInfo.extraDurationMinutes,
                        addedMiles = detourInfo.extraDistanceMiles
                    )
                )
            } else {
                _detourState.value = DetourState.Error("Could not calculate detour")
                // MP-024: Voice feedback for error
                RouteDetailsViewModelProvider.emitVoiceEvent(AiVoiceEvent.GenericError)
            }
        }
    }
    
    /**
     * MP-023: Calculate detour cost for adding a POI as intermediate stop.
     * 
     * PLUS TIER ONLY - Returns null for Free/Pro tiers or if no active route.
     */
    private suspend fun calculateDetourInfoForPoi(poiLatLng: LatLng): DetourInfo? {
        // Safety checks
        if (SafeModeManager.isSafeModeEnabled()) {
            Log.w(TAG, "Detour calculation blocked - SafeMode active")
            return null
        }
        
        if (!FeatureGate.areInAppMapsEnabled() || TierManager.isPro()) {
            Log.w(TAG, "Detour calculation blocked - tier not PLUS")
            return null
        }
        
        // Get base route info
        val baseRouteState = _googleRouteState.value
        if (baseRouteState !is GoogleRouteState.Success) {
            Log.w(TAG, "No active route for detour calculation")
            return null
        }
        
        val baseDistanceMeters = baseRouteState.distanceMeters
        val baseDurationSeconds = baseRouteState.durationSeconds
        
        // Get current location and destination
        val origin = _currentUserLocation.value ?: return null
        val dest = _destination.value ?: return null
        
        return withContext(Dispatchers.IO) {
            try {
                // Request route with POI as waypoint
                val detourResult = DirectionsApiClient.getRouteWithWaypoint(
                    origin = origin,
                    destination = LatLng(dest.latitude, dest.longitude),
                    waypoint = poiLatLng
                )
                
                when (detourResult) {
                    is DirectionsResult.Success -> {
                        val detourDistanceMeters = detourResult.distanceMeters
                        val detourDurationSeconds = detourResult.durationSeconds
                        
                        DetourInfo(
                            extraDistanceMeters = detourDistanceMeters - baseDistanceMeters,
                            extraDurationSeconds = detourDurationSeconds - baseDurationSeconds,
                            baseDistanceMeters = baseDistanceMeters,
                            baseDurationSeconds = baseDurationSeconds
                        )
                    }
                    is DirectionsResult.Failure -> {
                        Log.w(TAG, "Detour route request failed: ${detourResult.reason}")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Detour calculation error", e)
                null
            }
        }
    }
    
    /**
     * MP-023: Confirm adding POI as stop and start navigation.
     * 
     * PLUS TIER ONLY - Replaces current route with detour route.
     */
    fun onAddStopConfirmed() {
        val currentState = _detourState.value
        if (currentState !is DetourState.Ready) {
            Log.w(TAG, "Cannot add stop - no detour ready")
            return
        }
        
        // Safety checks
        if (SafeModeManager.isSafeModeEnabled() || !FeatureGate.areInAppMapsEnabled() || TierManager.isPro()) {
            Log.w(TAG, "Add stop blocked - tier/safety check failed")
            return
        }
        
        val poi = currentState.poi
        Log.i(TAG, "Adding stop: ${poi.name} at ${poi.latLng}")
        
        // MP-024: Voice feedback for stop added
        RouteDetailsViewModelProvider.emitVoiceEvent(
            AiVoiceEvent.StopAdded(poi.name)
        )
        
        // Add POI to waypoints
        val currentWaypoints = _waypoints.value.toMutableList()
        val poiDestination = Destination(
            id = System.currentTimeMillis().toString(),
            name = poi.name,
            address = poi.address ?: "",
            latitude = poi.latLng.latitude,
            longitude = poi.latLng.longitude
        )
        currentWaypoints.add(poiDestination)
        _waypoints.value = currentWaypoints
        
        // Recalculate route with new waypoint
        viewModelScope.launch {
            calculateRouteWithWaypoints()
        }
        
        // Clear detour state
        _detourState.value = DetourState.Idle
        pendingPoiForDetour = null
    }
    
    /**
     * MP-023: Dismiss the current detour suggestion.
     */
    fun onDetourDismissed() {
        _detourState.value = DetourState.Idle
        pendingPoiForDetour = null
        Log.d(TAG, "Detour suggestion dismissed")
    }
    
    // ==================== TRUCK POI SEARCH (MP-025) ====================
    
    /**
     * MP-025: Search for truck POIs along the current HERE route.
     * 
     * PRO TIER ONLY - Searches using HERE Places API.
     */
    fun findTruckPois(type: TruckPoiType) {
        // SafeMode check
        if (SafeModeManager.isSafeModeEnabled()) {
            _truckPoiState.value = TruckPoiState.Blocked("Safe Mode is active")
            RouteDetailsViewModelProvider.emitVoiceEvent(
                AiVoiceEvent.GenericError
            )
            return
        }
        
        // Tier check - PRO only
        if (!TierManager.isPro()) {
            val requiredTier = if (TierManager.isPlus()) "Pro" else "Plus"
            _truckPoiState.value = TruckPoiState.Blocked("Truck POIs require Pro subscription")
            RouteDetailsViewModelProvider.emitVoiceEvent(
                AiVoiceEvent.UpgradeRequired(requiredTier, "truck-specific POI search")
            )
            return
        }
        
        // Check for active HERE route
        val routePolyline = getActiveHereRoutePolyline()
        if (routePolyline.size < 2) {
            _truckPoiState.value = TruckPoiState.Error("No active truck route")
            RouteDetailsViewModelProvider.emitVoiceEvent(AiVoiceEvent.GenericError)
            return
        }
        
        _truckPoiState.value = TruckPoiState.Searching(type)
        Log.d(TAG, "Searching for ${type.displayName} along route")
        
        viewModelScope.launch {
            try {
                val results = HereTruckPoiClient.fetchTruckPoisAlongRoute(
                    routePolyline = routePolyline,
                    types = listOf(type),
                    corridorMeters = 3000.0
                )
                
                val result = results.firstOrNull()
                if (result != null && result.pois.isNotEmpty()) {
                    _truckPoiState.value = TruckPoiState.Found(result)
                    Log.i(TAG, "Found ${result.pois.size} ${type.displayName} locations")
                    
                    // Voice feedback
                    RouteDetailsViewModelProvider.emitVoiceEvent(
                        AiVoiceEvent.PoiFound(
                            poiName = result.pois.first().name,
                            poiType = type.displayName,
                            distanceAheadMiles = (result.pois.first().distanceMeters ?: 0) / 1609.34,
                            totalResults = result.pois.size
                        )
                    )
                    
                    // Auto-select first POI for detour calculation
                    val firstPoi = result.pois.first()
                    onTruckPoiSelected(firstPoi)
                } else {
                    _truckPoiState.value = TruckPoiState.NotFound(type)
                    Log.i(TAG, "No ${type.displayName} found along route")
                    RouteDetailsViewModelProvider.emitVoiceEvent(
                        AiVoiceEvent.NoPoisFound(type.displayName)
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Truck POI search error", e)
                _truckPoiState.value = TruckPoiState.Error(e.message ?: "Search failed")
                RouteDetailsViewModelProvider.emitVoiceEvent(AiVoiceEvent.GenericError)
            }
        }
    }
    
    /**
     * MP-025: Handle truck POI selection for detour calculation.
     * 
     * PRO TIER ONLY - Converts TruckPoi to SelectedPoi and uses shared detour flow.
     */
    fun onTruckPoiSelected(poi: TruckPoi) {
        // Safety/tier checks
        if (SafeModeManager.isSafeModeEnabled() || !TierManager.isPro()) {
            Log.w(TAG, "Truck POI selection blocked")
            return
        }
        
        Log.d(TAG, "Truck POI selected: ${poi.name}")
        pendingTruckPoi = poi
        
        // Convert to SelectedPoi and use shared detour calculation
        val selectedPoi = SelectedPoi(
            name = poi.name,
            latLng = poi.latLng,
            address = poi.address,
            placeId = poi.id,
            source = "here_truck"
        )
        
        // Trigger detour calculation
        calculateDetourForTruckPoi(selectedPoi)
    }
    
    /**
     * MP-025: Calculate detour cost for truck POI using HERE routing.
     * 
     * Uses HERE SDK for truck-legal route calculation.
     */
    private fun calculateDetourForTruckPoi(poi: SelectedPoi) {
        _detourState.value = DetourState.Calculating
        
        viewModelScope.launch {
            try {
                val origin = _currentUserLocation.value
                val dest = _destination.value
                
                if (origin == null || dest == null) {
                    _detourState.value = DetourState.Error("Missing origin or destination")
                    return@launch
                }
                
                val destLatLng = LatLng(dest.latitude, dest.longitude)
                val truckConfig = getCurrentTruckConfig()
                
                // Get base route (current route without detour)
                val baseResult = HereShim.requestTruckRoute(origin, destLatLng, truckConfig)
                
                // Get detour route (via POI)
                val detourResult = HereShim.requestTruckRouteWithWaypoint(
                    start = origin,
                    waypoint = poi.latLng,
                    end = destLatLng,
                    truckConfig = truckConfig
                )
                
                // Calculate detour cost
                if (baseResult is TruckRouteResult.Success && detourResult is TruckRouteResult.Success) {
                    val detourInfo = DetourInfo(
                        extraDistanceMeters = (detourResult.distanceMeters - baseResult.distanceMeters).toInt(),
                        extraDurationSeconds = (detourResult.durationSeconds - baseResult.durationSeconds).toInt(),
                        baseDistanceMeters = baseResult.distanceMeters.toInt(),
                        baseDurationSeconds = baseResult.durationSeconds.toInt()
                    )
                    
                    _detourState.value = DetourState.Ready(poi, detourInfo)
                    Log.i(TAG, "Truck detour calculated: ${detourInfo.formatDetour()}")
                    
                    // Voice feedback
                    RouteDetailsViewModelProvider.emitVoiceEvent(
                        AiVoiceEvent.DetourSummary(
                            poiName = poi.name,
                            addedMinutes = detourInfo.extraDurationSeconds / 60,
                            addedMiles = detourInfo.extraDistanceMeters / 1609.34,
                            distanceOffRouteMiles = 0.0 // Not calculated for truck routes
                        )
                    )
                } else {
                    val errorMsg = when {
                        baseResult is TruckRouteResult.Failure -> baseResult.errorMessage
                        detourResult is TruckRouteResult.Failure -> detourResult.errorMessage
                        else -> "Route calculation failed"
                    }
                    _detourState.value = DetourState.Error(errorMsg)
                    RouteDetailsViewModelProvider.emitVoiceEvent(AiVoiceEvent.GenericError)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Truck detour calculation error", e)
                _detourState.value = DetourState.Error(e.message ?: "Detour calculation failed")
                RouteDetailsViewModelProvider.emitVoiceEvent(AiVoiceEvent.GenericError)
            }
        }
    }
    
    /**
     * MP-025: Confirm adding truck POI as stop on HERE route.
     */
    fun onTruckStopConfirmed() {
        val currentState = _detourState.value
        if (currentState !is DetourState.Ready) {
            Log.w(TAG, "Cannot add truck stop - no detour ready")
            return
        }
        
        if (!TierManager.isPro()) {
            Log.w(TAG, "Truck stop blocked - not Pro tier")
            return
        }
        
        val poi = currentState.poi
        Log.i(TAG, "Adding truck stop: ${poi.name}")
        
        // Voice feedback
        RouteDetailsViewModelProvider.emitVoiceEvent(
            AiVoiceEvent.StopAdded(poi.name)
        )
        
        // Add to truck waypoints and recalculate HERE route
        viewModelScope.launch {
            recalculateTruckRouteWithStop(poi.latLng)
        }
        
        // Clear states
        _detourState.value = DetourState.Idle
        _truckPoiState.value = TruckPoiState.Idle
        pendingTruckPoi = null
    }
    
    /**
     * MP-025: Dismiss truck POI state.
     */
    fun onTruckPoiDismissed() {
        _truckPoiState.value = TruckPoiState.Idle
        pendingTruckPoi = null
        Log.d(TAG, "Truck POI dismissed")
    }
    
    /**
     * MP-025: Recalculate truck route including the new stop.
     */
    private suspend fun recalculateTruckRouteWithStop(stopLatLng: LatLng) {
        val origin = _currentUserLocation.value ?: return
        val dest = _destination.value ?: return
        val truckConfig = getCurrentTruckConfig()
        
        withContext(Dispatchers.IO) {
            val result = HereShim.requestTruckRouteWithWaypoint(
                start = origin,
                waypoint = stopLatLng,
                end = LatLng(dest.latitude, dest.longitude),
                truckConfig = truckConfig
            )
            
            when (result) {
                is TruckRouteResult.Success -> {
                    _currentHerePolyline.value = result.polyline
                    _routeState.value = RouteState.Success(
                        RouteInfo(
                            distanceMeters = result.distanceMeters,
                            durationSeconds = result.durationSeconds,
                            polyline = result.polyline,
                            restrictions = result.restrictions
                        )
                    )
                    Log.i(TAG, "Truck route with stop calculated")
                }
                is TruckRouteResult.Failure -> {
                    Log.e(TAG, "Truck route recalculation failed: ${result.errorMessage}")
                }
            }
        }
    }
    
    /**
     * MP-025: Get current truck configuration.
     */
    private fun getCurrentTruckConfig(): TruckConfig {
        // TODO: Get from user settings or vehicle profile
        return TruckConfig(
            heightMeters = 4.0,
            widthMeters = 2.5,
            lengthMeters = 16.0,
            weightKg = 36000,
            axleCount = 5,
            trailerCount = 1,
            hazmatType = null
        )
    }
    
    /**
     * MP-023: Recalculate route including current waypoints.
     */
    private suspend fun calculateRouteWithWaypoints() {
        val origin = _currentUserLocation.value ?: return
        val dest = _destination.value ?: return
        val waypoints = _waypoints.value
        
        if (waypoints.isEmpty()) {
            // No waypoints, use standard route calculation
            calculateGoogleRoute(origin, LatLng(dest.latitude, dest.longitude))
            return
        }
        
        _googleRouteState.value = GoogleRouteState.Loading
        
        withContext(Dispatchers.IO) {
            try {
                val waypointLatLngs = waypoints.map { LatLng(it.latitude, it.longitude) }
                val result = DirectionsApiClient.getRouteWithMultipleWaypoints(
                    origin = origin,
                    destination = LatLng(dest.latitude, dest.longitude),
                    waypoints = waypointLatLngs
                )
                
                when (result) {
                    is DirectionsResult.Success -> {
                        _googleRouteState.value = GoogleRouteState.Success(
                            polyline = result.polyline,
                            distanceMeters = result.distanceMeters,
                            durationSeconds = result.durationSeconds
                        )
                        _currentGooglePolyline.value = result.polyline
                        Log.i(TAG, "Route with ${waypoints.size} waypoints calculated")
                    }
                    is DirectionsResult.Failure -> {
                        _googleRouteState.value = GoogleRouteState.Error(result.reason)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Route calculation error", e)
                _googleRouteState.value = GoogleRouteState.Error(e.message ?: "Route calculation failed")
            }
        }
    }

    /**
     * Update current user location from LocationViewModel.
     * Called by UI when location changes.
     */
    fun updateUserLocation(location: LatLng?) {
        _currentUserLocation.value = location
        
        // Feed location to navigation engine if navigating
        if (_isNavigating.value && location != null) {
            navigationEngine.updateLocation(location)
        }
    }
    
    /**
     * Start turn-by-turn navigation.
     * Requires Plus/Pro tier and valid route.
     */
    fun startNavigation() {
        // SafeMode check
        if (SafeModeManager.isSafeModeEnabled()) {
            Log.w(TAG, "Navigation blocked - SafeMode active")
            return
        }
        
        // Tier check
        if (!FeatureGate.areInAppMapsEnabled()) {
            Log.w(TAG, "Navigation blocked - Free tier")
            return
        }
        
        // Get route to navigate
        val navRoute = _currentNavRoute.value
        if (navRoute == null || navRoute.steps.isEmpty()) {
            Log.w(TAG, "Cannot start navigation - no valid route")
            return
        }
        
        val started = navigationEngine.startNavigation(navRoute)
        if (started) {
            _isNavigating.value = true
            Log.i(TAG, "Navigation started with ${navRoute.steps.size} steps")
        }
    }
    
    /**
     * Stop navigation.
     */
    fun stopNavigation() {
        navigationEngine.stopNavigation()
        _isNavigating.value = false
        Log.i(TAG, "Navigation stopped")
    }
    
    /**
     * Handle off-route detection - request recalculation.
     */
    fun onOffRoute() {
        Log.w(TAG, "Off route - requesting recalculation")
        navigationEngine.requestRecalc()
        
        // Trigger route recalculation
        viewModelScope.launch {
            val location = _currentUserLocation.value ?: return@launch
            val dest = _destination.value ?: return@launch
            
            // Update origin to current location
            _origin.value = Destination(
                id = "recalc_origin",
                name = "Current Location",
                address = "",
                latitude = location.latitude,
                longitude = location.longitude
            )
            
            // Recalculate route
            calculateRouteWithNavigation()
        }
    }
    
    /**
     * Calculate route and prepare for navigation.
     */
    private fun calculateRouteWithNavigation() {
        val orig = _origin.value
        val dest = _destination.value
        
        if (orig == null || dest == null) {
            _routeState.value = RouteState.Error("Please set origin and destination")
            return
        }
        
        _routeState.value = RouteState.Loading
        
        viewModelScope.launch {
            try {
                if (_isTruckMode.value && FeatureGate.areCommercialRoutingFeaturesEnabled()) {
                    // Truck routing with navigation
                    val result = withContext(Dispatchers.IO) {
                        HereShim.requestTruckRoute(
                            start = LatLng(orig.latitude, orig.longitude),
                            end = LatLng(dest.latitude, dest.longitude),
                            truckConfig = _currentTruckConfig.value
                        )
                    }
                    
                    when (result) {
                        is TruckRouteResult.Success -> {
                            val navRoute = HereShim.createNavRoute(result.route)
                            _currentNavRoute.value = navRoute
                            
                            // If navigation was active, update the route
                            if (_isNavigating.value) {
                                navigationEngine.updateRoute(navRoute)
                            }
                            
                            _routeState.value = RouteState.Success(RouteInfo(
                                distanceMeters = result.route.distanceMeters,
                                durationSeconds = result.route.durationSeconds,
                                isTruckRoute = true,
                                isFallback = result.route.isFallback,
                                warnings = result.route.warnings.map { it.description },
                                steps = navRoute.steps.map { it.instruction }
                            ))
                        }
                        is TruckRouteResult.Failure -> {
                            _routeState.value = RouteState.Error(result.errorMessage)
                        }
                    }
                } else {
                    // Car routing (Google) - navigation steps not yet implemented
                    val route = calculateCarRoute(orig, dest)
                    if (route != null) {
                        _routeState.value = RouteState.Success(route)
                        // TODO MP-019: Create NavRoute from Google route
                    } else {
                        _routeState.value = RouteState.Error("Could not calculate route")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Route calculation failed", e)
                _routeState.value = RouteState.Error("Route calculation failed")
            }
        }
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
    private suspend fun calculateTruckRoute(origin: Destination, dest: Destination): RouteInfo? {
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
     * Calculate car route using Google Directions API.
     * MP-019: Full implementation for Plus tier turn-by-turn.
     * Gated by FeatureGate.areInAppMapsEnabled()
     */
    private suspend fun calculateCarRoute(origin: Destination, dest: Destination): RouteInfo? {
        if (!FeatureGate.areInAppMapsEnabled()) {
            Log.d(TAG, "In-app routing blocked - would trigger Maps intent")
            return null
        }
        
        Log.d(TAG, "Calculating car route via Google Directions: ${origin.name} -> ${dest.name}")
        
        val waypoints = _waypoints.value.map { LatLng(it.latitude, it.longitude) }
        
        val result = DirectionsApiClient.getRoute(
            origin = LatLng(origin.latitude, origin.longitude),
            destination = LatLng(dest.latitude, dest.longitude),
            waypoints = waypoints
        )
        
        return when (result) {
            is DirectionsResult.Success -> {
                // Store polyline for map rendering
                _currentGooglePolyline.value = result.polylineCoordinates
                _googleRouteState.value = GoogleRouteState.Success(
                    polyline = result.polylineCoordinates,
                    distanceMeters = result.totalDistanceMeters,
                    durationSeconds = result.totalDurationSeconds
                )
                
                // Create NavRoute for navigation
                val navRoute = MapsShim.createNavRoute(result)
                _currentNavRoute.value = navRoute
                
                Log.i(TAG, "Google route success: ${result.totalDistanceMeters}m, ${navRoute.steps.size} steps")
                
                RouteInfo(
                    distanceMeters = result.totalDistanceMeters.toLong(),
                    durationSeconds = result.totalDurationSeconds.toLong(),
                    isTruckRoute = false,
                    isFallback = false,
                    warnings = result.route.warnings,
                    steps = navRoute.steps.map { it.instruction }
                )
            }
            is DirectionsResult.Failure -> {
                Log.w(TAG, "Google route failed: ${result.errorMessage}")
                _googleRouteState.value = GoogleRouteState.Error(result.errorMessage)
                null
            }
        }
    }
    
    /**
     * Request Google route (Plus tier).
     * Public entry point for requesting Google Directions route.
     */
    fun requestGoogleRoute() {
        val orig = _origin.value
        val dest = _destination.value
        
        if (orig == null || dest == null) {
            _googleRouteState.value = GoogleRouteState.Error("Please set origin and destination")
            return
        }
        
        _googleRouteState.value = GoogleRouteState.Loading
        
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                DirectionsApiClient.getRoute(
                    origin = LatLng(orig.latitude, orig.longitude),
                    destination = LatLng(dest.latitude, dest.longitude),
                    waypoints = _waypoints.value.map { LatLng(it.latitude, it.longitude) }
                )
            }
            
            when (result) {
                is DirectionsResult.Success -> {
                    _currentGooglePolyline.value = result.polylineCoordinates
                    _googleRouteState.value = GoogleRouteState.Success(
                        polyline = result.polylineCoordinates,
                        distanceMeters = result.totalDistanceMeters,
                        durationSeconds = result.totalDurationSeconds
                    )
                    
                    val navRoute = MapsShim.createNavRoute(result)
                    _currentNavRoute.value = navRoute
                    
                    _routeState.value = RouteState.Success(RouteInfo(
                        distanceMeters = result.totalDistanceMeters.toLong(),
                        durationSeconds = result.totalDurationSeconds.toLong(),
                        isTruckRoute = false,
                        isFallback = false,
                        warnings = result.route.warnings,
                        steps = navRoute.steps.map { it.instruction }
                    ))
                    
                    Log.i(TAG, "Google route requested: ${result.totalDistanceMeters}m")
                }
                is DirectionsResult.Failure -> {
                    _googleRouteState.value = GoogleRouteState.Error(result.errorMessage)
                    _routeState.value = RouteState.Error(result.errorMessage)
                }
            }
        }
    }
    
    /**
     * Clear Google route state.
     */
    fun clearGoogleRoute() {
        _googleRouteState.value = GoogleRouteState.Idle
        _currentGooglePolyline.value = emptyList()
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
     * MP-020: Handle resolved intent from AI intent pipeline.
     * This is the entry point for the new multi-step reasoning system.
     * 
     * Pipeline:
     * - If truck mode → HereShim
     * - If car mode → Google Directions
     * - Triggers map, steps, and nav state automatically
     */
    fun handleResolvedIntent(request: AiRouteRequest) {
        Log.i(TAG, "Handling resolved intent: ${request.rawQuery}")
        
        viewModelScope.launch {
            _aiRouteState.value = AiRouteState.Loading
            
            try {
                // Get route suggestion from Gemini
                val result = GeminiShim.getRouteSuggestion(request)
                
                when (result) {
                    is AiRouteResult.Success -> {
                        Log.i(TAG, "Resolved intent success: ${result.suggestion.destinationName}")
                        _aiRouteState.value = AiRouteState.Success(result.suggestion)
                        applyAiRouteSuggestion(result.suggestion)
                    }
                    is AiRouteResult.Failure -> {
                        Log.w(TAG, "Resolved intent failed: ${result.reason}")
                        _aiRouteState.value = AiRouteState.Error(result.reason)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Resolved intent exception", e)
                _aiRouteState.value = AiRouteState.Error("Route calculation failed: ${e.message}")
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
