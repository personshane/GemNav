package com.gemnav.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.gemnav.core.feature.FeatureGate
import com.gemnav.core.safety.SafeModeManager
import com.gemnav.data.route.LatLng
import com.google.android.gms.location.*

/**
 * LocationService - Wrapper for FusedLocationProviderClient.
 * Provides GPS location with SafeMode + FeatureGate enforcement.
 */
class LocationService(private val context: Context) {
    
    companion object {
        private const val TAG = "LocationService"
        private const val DEFAULT_INTERVAL_MS = 1000L
        private const val DEFAULT_FASTEST_INTERVAL_MS = 500L
    }
    
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }
    
    private var locationCallback: LocationCallback? = null
    private var isTracking = false
    
    // Listener interface
    interface LocationListener {
        fun onLocationChanged(location: LatLng)
        fun onLocationError(error: String)
    }
    
    private var listener: LocationListener? = null
    
    fun setListener(listener: LocationListener) {
        this.listener = listener
    }
    
    /**
     * Check if location permissions are granted.
     */
    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Start location updates.
     * Enforces SafeMode and FeatureGate.
     */
    @SuppressLint("MissingPermission")
    fun startLocationUpdates(
        intervalMs: Long = DEFAULT_INTERVAL_MS,
        fastestIntervalMs: Long = DEFAULT_FASTEST_INTERVAL_MS
    ): Boolean {
        // SafeMode check
        if (SafeModeManager.isSafeModeEnabled()) {
            Log.w(TAG, "Location blocked - SafeMode active")
            listener?.onLocationError("Safe Mode is active")
            return false
        }
        
        // FeatureGate check (in-app maps required for live location)
        if (!FeatureGate.areInAppMapsEnabled()) {
            Log.w(TAG, "Location blocked - in-app maps not enabled for tier")
            listener?.onLocationError("Location requires Plus or Pro subscription")
            return false
        }
        
        // Permission check
        if (!hasPermission()) {
            Log.w(TAG, "Location blocked - permission not granted")
            listener?.onLocationError("Location permission required")
            return false
        }
        
        if (isTracking) {
            Log.d(TAG, "Already tracking location")
            return true
        }
        
        Log.i(TAG, "Starting location updates (interval: ${intervalMs}ms)")
        
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            intervalMs
        ).apply {
            setMinUpdateIntervalMillis(fastestIntervalMs)
            setWaitForAccurateLocation(false)
        }.build()
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    val latLng = LatLng(location.latitude, location.longitude)
                    Log.d(TAG, "Location: ${latLng.latitude}, ${latLng.longitude}")
                    listener?.onLocationChanged(latLng)
                }
            }
            
            override fun onLocationAvailability(availability: LocationAvailability) {
                if (!availability.isLocationAvailable) {
                    Log.w(TAG, "Location not available")
                    listener?.onLocationError("GPS signal lost")
                }
            }
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            isTracking = true
            return true
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception starting location", e)
            listener?.onLocationError("Location permission denied")
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start location updates", e)
            listener?.onLocationError("Failed to start GPS")
            return false
        }
    }
    
    /**
     * Stop location updates.
     */
    fun stopLocationUpdates() {
        if (!isTracking) return
        
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            Log.i(TAG, "Stopped location updates")
        }
        locationCallback = null
        isTracking = false
    }
    
    /**
     * Get last known location (cached).
     */
    @SuppressLint("MissingPermission")
    fun getLastKnownLocation(callback: (LatLng?) -> Unit) {
        if (!hasPermission()) {
            callback(null)
            return
        }
        
        if (SafeModeManager.isSafeModeEnabled()) {
            callback(null)
            return
        }
        
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    callback(LatLng(location.latitude, location.longitude))
                } else {
                    callback(null)
                }
            }.addOnFailureListener {
                Log.w(TAG, "Failed to get last location", it)
                callback(null)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception getting last location", e)
            callback(null)
        }
    }
    
    /**
     * Check if currently tracking.
     */
    fun isTracking(): Boolean = isTracking
    
    /**
     * Cleanup resources.
     */
    fun destroy() {
        stopLocationUpdates()
        listener = null
    }
}
