package com.gemnav.app.ui.route

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemnav.app.models.Destination
import com.gemnav.core.feature.FeatureGate
import com.gemnav.core.shim.HereShim
import com.gemnav.core.shim.MapsShim
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
}
