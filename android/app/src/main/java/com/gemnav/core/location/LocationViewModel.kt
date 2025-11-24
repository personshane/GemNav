package com.gemnav.core.location

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.gemnav.core.feature.FeatureGate
import com.gemnav.core.shim.SafeModeManager
import com.gemnav.data.route.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * LocationViewModel - Exposes device location state to UI.
 * Manages LocationService lifecycle with SafeMode awareness.
 */
class LocationViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "LocationViewModel"
    }
    
    /**
     * Location tracking status.
     */
    sealed class LocationStatus {
        object Idle : LocationStatus()
        object Active : LocationStatus()
        object Searching : LocationStatus()
        data class Error(val message: String) : LocationStatus()
        object PermissionDenied : LocationStatus()
    }
    
    private val locationService: LocationService by lazy {
        LocationService(getApplication<Application>().applicationContext).apply {
            setListener(createLocationListener())
        }
    }
    
    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation
    
    private val _lastKnownLocation = MutableStateFlow<LatLng?>(null)
    val lastKnownLocation: StateFlow<LatLng?> = _lastKnownLocation
    
    private val _locationStatus = MutableStateFlow<LocationStatus>(LocationStatus.Idle)
    val locationStatus: StateFlow<LocationStatus> = _locationStatus
    
    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission
    
    init {
        checkPermission()
    }
    
    private fun createLocationListener(): LocationService.LocationListener {
        return object : LocationService.LocationListener {
            override fun onLocationChanged(location: LatLng) {
                Log.d(TAG, "Location update: ${location.latitude}, ${location.longitude}")
                _currentLocation.value = location
                _lastKnownLocation.value = location
                _locationStatus.value = LocationStatus.Active
            }
            
            override fun onLocationError(error: String) {
                Log.w(TAG, "Location error: $error")
                _locationStatus.value = LocationStatus.Error(error)
            }
        }
    }
    
    /**
     * Check location permission status.
     */
    fun checkPermission(): Boolean {
        _hasPermission.value = locationService.hasPermission()
        return _hasPermission.value
    }
    
    /**
     * Start location tracking.
     * Enforces SafeMode and FeatureGate.
     */
    fun startTracking() {
        // Permission check
        if (!checkPermission()) {
            Log.d(TAG, "Tracking blocked - permission not granted")
            _locationStatus.value = LocationStatus.PermissionDenied
            return
        }
        
        // SafeMode check
        if (SafeModeManager.isSafeModeEnabled()) {
            Log.d(TAG, "Tracking blocked - SafeMode active")
            _locationStatus.value = LocationStatus.Error("Safe Mode is active")
            return
        }
        
        // FeatureGate check
        if (!FeatureGate.areInAppMapsEnabled()) {
            Log.d(TAG, "Tracking blocked - feature not enabled")
            _locationStatus.value = LocationStatus.Error("Location requires Plus or Pro")
            return
        }
        
        Log.i(TAG, "Starting location tracking")
        _locationStatus.value = LocationStatus.Searching
        
        val success = locationService.startLocationUpdates()
        if (!success) {
            Log.w(TAG, "Failed to start location updates")
            // Error already set by service via listener
        }
    }
    
    /**
     * Stop location tracking.
     */
    fun stopTracking() {
        Log.i(TAG, "Stopping location tracking")
        locationService.stopLocationUpdates()
        _locationStatus.value = LocationStatus.Idle
    }
    
    /**
     * Refresh last known location (one-shot).
     */
    fun refreshLastLocation() {
        if (!checkPermission()) {
            _locationStatus.value = LocationStatus.PermissionDenied
            return
        }
        
        if (SafeModeManager.isSafeModeEnabled()) {
            return
        }
        
        locationService.getLastKnownLocation { location ->
            if (location != null) {
                _lastKnownLocation.value = location
                if (_currentLocation.value == null) {
                    _currentLocation.value = location
                }
                Log.d(TAG, "Last known: ${location.latitude}, ${location.longitude}")
            }
        }
    }
    
    /**
     * Check if currently tracking.
     */
    fun isTracking(): Boolean = locationService.isTracking()
    
    /**
     * Called after permission granted.
     */
    fun onPermissionGranted() {
        _hasPermission.value = true
        _locationStatus.value = LocationStatus.Idle
        refreshLastLocation()
    }
    
    /**
     * Called when permission denied.
     */
    fun onPermissionDenied() {
        _hasPermission.value = false
        _locationStatus.value = LocationStatus.PermissionDenied
    }
    
    override fun onCleared() {
        super.onCleared()
        locationService.destroy()
        Log.d(TAG, "LocationViewModel cleared")
    }
}
