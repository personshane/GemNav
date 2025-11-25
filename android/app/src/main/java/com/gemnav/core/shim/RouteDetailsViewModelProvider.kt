package com.gemnav.core.shim

import com.gemnav.data.route.LatLng

/**
 * MP-022: Service locator for RouteDetailsViewModel access from shim layer.
 * 
 * This allows GeminiShim to access the active route polyline without
 * direct dependency on the ViewModel (which lives in the UI layer).
 * 
 * PLUS TIER ONLY - Provider returns empty data for Free/Pro tiers.
 */
object RouteDetailsViewModelProvider {
    
    private var polylineProvider: (() -> List<LatLng>)? = null
    private var isNavigatingProvider: (() -> Boolean)? = null
    
    /**
     * Register the polyline provider (called from UI layer initialization).
     */
    fun registerPolylineProvider(provider: () -> List<LatLng>) {
        polylineProvider = provider
    }
    
    /**
     * Register the navigation state provider.
     */
    fun registerNavigationStateProvider(provider: () -> Boolean) {
        isNavigatingProvider = provider
    }
    
    /**
     * Unregister providers (called on cleanup).
     */
    fun unregister() {
        polylineProvider = null
        isNavigatingProvider = null
    }
    
    /**
     * Get the active route polyline for along-route POI filtering.
     * Returns empty list if no provider registered or no active route.
     */
    fun getActiveRoutePolyline(): List<LatLng> {
        return polylineProvider?.invoke() ?: emptyList()
    }
    
    /**
     * Check if currently navigating.
     */
    fun isNavigating(): Boolean {
        return isNavigatingProvider?.invoke() ?: false
    }
}
