package com.gemnav.core.shim

import android.util.Log
import com.gemnav.app.BuildConfig
import com.gemnav.core.feature.FeatureGate
import com.gemnav.core.navigation.RouteCorridor
import com.gemnav.core.navigation.TruckPoi
import com.gemnav.core.navigation.TruckPoiResult
import com.gemnav.core.navigation.TruckPoiType
import com.gemnav.data.route.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * MP-025: HERE Places API client for truck-specific POIs.
 * 
 * PRO TIER ONLY - Searches for truck stops, weigh stations, rest areas, and parking.
 * Uses HERE Discover API with category filtering, then applies route corridor filtering.
 */
object HereTruckPoiClient {
    
    private const val TAG = "HereTruckPoiClient"
    private const val DISCOVER_BASE_URL = "https://discover.search.hereapi.com/v1/discover"
    private const val DEFAULT_LIMIT = 20
    private const val DEFAULT_CORRIDOR_METERS = 3000.0
    
    /**
     * Fetch truck POIs along a route corridor.
     * 
     * @param routePolyline Decoded polyline points from HERE routing
     * @param types List of truck POI types to search for
     * @param corridorMeters Max distance from route (default 3km)
     * @return List of TruckPoiResult, one per type requested
     */
    suspend fun fetchTruckPoisAlongRoute(
        routePolyline: List<LatLng>,
        types: List<TruckPoiType>,
        corridorMeters: Double = DEFAULT_CORRIDOR_METERS
    ): List<TruckPoiResult> {
        // Safety checks
        if (SafeModeManager.isSafeModeEnabled()) {
            Log.w(TAG, "Truck POI search blocked - SafeMode active")
            return emptyList()
        }
        
        if (!FeatureGate.areCommercialRoutingFeaturesEnabled()) {
            Log.w(TAG, "Truck POI search blocked - not Pro tier")
            return emptyList()
        }
        
        if (routePolyline.size < 2) {
            Log.w(TAG, "Invalid route polyline for POI search")
            return emptyList()
        }
        
        val apiKey = BuildConfig.HERE_API_KEY
        if (apiKey.isBlank()) {
            Log.e(TAG, "HERE API key not configured")
            return emptyList()
        }
        
        // Compute bounding box from polyline with corridor buffer
        val bbox = computeBoundingBox(routePolyline, corridorMeters)
        
        return withContext(Dispatchers.IO) {
            types.mapNotNull { type ->
                try {
                    val pois = searchPoisForType(type, bbox, apiKey)
                    // Filter to route corridor
                    val filteredPois = filterPoisToRouteCorridor(pois, routePolyline, corridorMeters)
                    TruckPoiResult(type, filteredPois)
                } catch (e: Exception) {
                    Log.e(TAG, "Error searching ${type.displayName}", e)
                    null
                }
            }
        }
    }
    
    /**
     * Search for a single POI type near a location (without route).
     */
    suspend fun searchNearby(
        location: LatLng,
        type: TruckPoiType,
        radiusMeters: Int = 30000
    ): TruckPoiResult {
        if (SafeModeManager.isSafeModeEnabled() || !FeatureGate.areCommercialRoutingFeaturesEnabled()) {
            return TruckPoiResult(type, emptyList())
        }
        
        val apiKey = BuildConfig.HERE_API_KEY
        if (apiKey.isBlank()) {
            return TruckPoiResult(type, emptyList())
        }
        
        return withContext(Dispatchers.IO) {
            try {
                val pois = searchPoisNearLocation(type, location, radiusMeters, apiKey)
                TruckPoiResult(type, pois)
            } catch (e: Exception) {
                Log.e(TAG, "Error searching ${type.displayName} nearby", e)
                TruckPoiResult(type, emptyList())
            }
        }
    }
    
    /**
     * Search HERE Discover API for POIs within bounding box.
     */
    private fun searchPoisForType(
        type: TruckPoiType,
        bbox: BoundingBox,
        apiKey: String
    ): List<TruckPoi> {
        val url = buildDiscoverUrl(
            bbox = bbox,
            categoryId = type.hereCategoryId,
            apiKey = apiKey
        )
        
        Log.d(TAG, "Searching ${type.displayName}: $url")
        
        val response = executeHttpGet(url)
        return parseDiscoverResponse(response, type)
    }
    
    /**
     * Search HERE Discover API for POIs near a location.
     */
    private fun searchPoisNearLocation(
        type: TruckPoiType,
        location: LatLng,
        radiusMeters: Int,
        apiKey: String
    ): List<TruckPoi> {
        val url = buildDiscoverUrlWithCircle(
            center = location,
            radiusMeters = radiusMeters,
            categoryId = type.hereCategoryId,
            apiKey = apiKey
        )
        
        Log.d(TAG, "Searching ${type.displayName} nearby: $url")
        
        val response = executeHttpGet(url)
        return parseDiscoverResponse(response, type)
    }
    
    /**
     * Build HERE Discover URL with bounding box.
     */
    private fun buildDiscoverUrl(
        bbox: BoundingBox,
        categoryId: String,
        apiKey: String
    ): String {
        val bboxParam = "bbox:${bbox.west},${bbox.south},${bbox.east},${bbox.north}"
        return "$DISCOVER_BASE_URL?" +
                "in=${URLEncoder.encode(bboxParam, "UTF-8")}&" +
                "categories=$categoryId&" +
                "limit=$DEFAULT_LIMIT&" +
                "apiKey=$apiKey"
    }
    
    /**
     * Build HERE Discover URL with circle (location + radius).
     */
    private fun buildDiscoverUrlWithCircle(
        center: LatLng,
        radiusMeters: Int,
        categoryId: String,
        apiKey: String
    ): String {
        val circleParam = "circle:${center.latitude},${center.longitude};r=$radiusMeters"
        return "$DISCOVER_BASE_URL?" +
                "in=${URLEncoder.encode(circleParam, "UTF-8")}&" +
                "categories=$categoryId&" +
                "limit=$DEFAULT_LIMIT&" +
                "apiKey=$apiKey"
    }
    
    /**
     * Execute HTTP GET request.
     */
    private fun executeHttpGet(urlString: String): String {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        
        return try {
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                Log.w(TAG, "HTTP ${connection.responseCode}: ${connection.responseMessage}")
                "{}"
            }
        } finally {
            connection.disconnect()
        }
    }
    
    /**
     * Parse HERE Discover API response.
     */
    private fun parseDiscoverResponse(jsonString: String, type: TruckPoiType): List<TruckPoi> {
        val pois = mutableListOf<TruckPoi>()
        
        try {
            val json = JSONObject(jsonString)
            val items = json.optJSONArray("items") ?: return emptyList()
            
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val position = item.optJSONObject("position")
                val address = item.optJSONObject("address")
                
                if (position != null) {
                    pois.add(TruckPoi(
                        id = item.optString("id", "poi_$i"),
                        name = item.optString("title", "Unknown ${type.displayName}"),
                        lat = position.getDouble("lat"),
                        lng = position.getDouble("lng"),
                        category = type,
                        address = address?.optString("label"),
                        distanceMeters = item.optInt("distance", -1).takeIf { it >= 0 },
                        phone = item.optJSONArray("contacts")
                            ?.optJSONObject(0)
                            ?.optJSONArray("phone")
                            ?.optJSONObject(0)
                            ?.optString("value")
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing response", e)
        }
        
        return pois
    }
    
    /**
     * Filter POIs to only those within route corridor.
     */
    private fun filterPoisToRouteCorridor(
        pois: List<TruckPoi>,
        routePolyline: List<LatLng>,
        corridorMeters: Double
    ): List<TruckPoi> {
        return pois.filter { poi ->
            RouteCorridor.isPointAlongRoute(poi.latLng, routePolyline, corridorMeters)
        }
    }
    
    /**
     * Compute bounding box from polyline with buffer.
     */
    private fun computeBoundingBox(polyline: List<LatLng>, bufferMeters: Double): BoundingBox {
        var minLat = Double.MAX_VALUE
        var maxLat = Double.MIN_VALUE
        var minLng = Double.MAX_VALUE
        var maxLng = Double.MIN_VALUE
        
        for (point in polyline) {
            if (point.latitude < minLat) minLat = point.latitude
            if (point.latitude > maxLat) maxLat = point.latitude
            if (point.longitude < minLng) minLng = point.longitude
            if (point.longitude > maxLng) maxLng = point.longitude
        }
        
        // Add buffer (roughly convert meters to degrees)
        val latBuffer = bufferMeters / 111000.0  // ~111km per degree latitude
        val lngBuffer = bufferMeters / (111000.0 * kotlin.math.cos(Math.toRadians((minLat + maxLat) / 2)))
        
        return BoundingBox(
            north = maxLat + latBuffer,
            south = minLat - latBuffer,
            east = maxLng + lngBuffer,
            west = minLng - lngBuffer
        )
    }
    
    /**
     * Bounding box coordinates.
     */
    private data class BoundingBox(
        val north: Double,
        val south: Double,
        val east: Double,
        val west: Double
    )
}
