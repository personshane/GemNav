package com.gemnav.core.shim

import com.gemnav.core.navigation.AiVoiceEvent
import com.gemnav.core.navigation.SelectedPoi
import com.gemnav.core.navigation.TruckPoi
import com.gemnav.data.route.LatLng

/**
 * MP-022/MP-023/MP-024/MP-025: Service locator for RouteDetailsViewModel access from shim layer.
 * 
 * This allows GeminiShim to access the active route polyline without
 * direct dependency on the ViewModel (which lives in the UI layer).
 * 
 * PLUS TIER ONLY for Google POIs, PRO TIER for truck POIs.
 */
object RouteDetailsViewModelProvider {
    
    private var polylineProvider: (() -> List<LatLng>)? = null
    private var isNavigatingProvider: (() -> Boolean)? = null
    private var poiSelectionHandler: ((SelectedPoi) -> Unit)? = null
    
    /** MP-025: Truck POI selection handler for Pro tier */
    private var truckPoiSelectionHandler: ((TruckPoi) -> Unit)? = null
    
    /** MP-025: HERE route polyline provider for Pro tier */
    private var herePolylineProvider: (() -> List<LatLng>)? = null
    
    /** MP-024: Voice event handler for spoken feedback */
    @Volatile
    var voiceEventHandler: ((AiVoiceEvent) -> Unit)? = null
    
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
     * MP-023: Register the POI selection handler for detour calculation.
     */
    fun registerPoiSelectionHandler(handler: (SelectedPoi) -> Unit) {
        poiSelectionHandler = handler
    }
    
    /**
     * MP-025: Register the truck POI selection handler for Pro tier detour calculation.
     */
    fun registerTruckPoiSelectionHandler(handler: (TruckPoi) -> Unit) {
        truckPoiSelectionHandler = handler
    }
    
    /**
     * MP-025: Register the HERE route polyline provider for Pro tier.
     */
    fun registerHerePolylineProvider(provider: () -> List<LatLng>) {
        herePolylineProvider = provider
    }

    /**
     * Unregister providers (called on cleanup).
     */
    fun unregister() {
        polylineProvider = null
        isNavigatingProvider = null
        poiSelectionHandler = null
        truckPoiSelectionHandler = null
        herePolylineProvider = null
        // Note: voiceEventHandler is managed by RouteDetailsScreen lifecycle
    }
    
    /**
     * MP-024: Emit a voice event for spoken feedback.
     */
    fun emitVoiceEvent(event: AiVoiceEvent) {
        voiceEventHandler?.invoke(event)
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
    
    /**
     * MP-023: Trigger POI selection for detour calculation.
     * Returns true if handler was called, false if no handler registered.
     */
    fun selectPoiForDetour(poi: SelectedPoi): Boolean {
        poiSelectionHandler?.invoke(poi) ?: return false
        return true
    }
    
    /**
     * MP-025: Trigger truck POI selection for Pro tier detour calculation.
     * Returns true if handler was called, false if no handler registered.
     */
    fun selectTruckPoiForDetour(poi: TruckPoi): Boolean {
        truckPoiSelectionHandler?.invoke(poi) ?: return false
        return true
    }
    
    /**
     * MP-025: Get HERE route polyline for Pro tier along-route POI filtering.
     * Returns empty list if no provider registered or no active route.
     */
    fun getHereRoutePolyline(): List<LatLng> {
        return herePolylineProvider?.invoke() ?: emptyList()
    }
}
