package com.gemnav.core.places

import android.util.Log
import com.gemnav.app.BuildConfig
import com.gemnav.core.feature.FeatureGate
import com.gemnav.core.shim.SafeModeManager
import com.gemnav.core.subscription.TierManager
import com.gemnav.data.ai.POIFilters
import com.gemnav.data.ai.POIType
import com.gemnav.data.route.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * PlacesApiClient - REST-based Google Places API client.
 * MP-021: Plus tier only, uses REST API (not SDK) for compatibility.
 * 
 * Tier Rules:
 * - FREE: Blocked (uses AI only, no Places)
 * - PLUS: Full access via REST API
 * - PRO: Blocked (truck POI via HERE in future MP)
 */
object PlacesApiClient {
    private const val TAG = "PlacesApiClient"
    private const val BASE_URL = "https://maps.googleapis.com/maps/api/place"
    private const val DEFAULT_RADIUS_METERS = 30000 // 30km default
    private const val NEARBY_RADIUS_METERS = 5000   // 5km for "near me"
    private const val MAX_RESULTS = 10
    
    /**
     * Search for POIs near a location.
     * 
     * @param location Center point for search
     * @param radiusMeters Search radius (default 30km)
     * @param poiType Type of POI to search for
     * @param filters Additional filter criteria
     * @return PlacesResult with matching places or failure
     */
    suspend fun searchNearby(
        location: LatLng,
        radiusMeters: Int = DEFAULT_RADIUS_METERS,
        poiType: POIType,
        filters: POIFilters = POIFilters()
    ): PlacesResult = withContext(Dispatchers.IO) {
        // Tier enforcement
        val tierCheck = checkTierAccess()
        if (tierCheck != null) return@withContext tierCheck
        
        // API key check
        val apiKey = BuildConfig.GOOGLE_PLACES_API_KEY
        if (apiKey.isBlank()) {
            Log.w(TAG, "Google Places API key not configured")
            return@withContext PlacesResult.Failure("Places API key not configured")
        }
        
        try {
            val placesType = PoiTypeMapper.mapPoiTypeToPlacesType(poiType)
            val keyword = PoiTypeMapper.mapPoiKeyword(poiType)
            
            val urlBuilder = StringBuilder()
                .append("$BASE_URL/nearbysearch/json?")
                .append("location=${location.latitude},${location.longitude}")
                .append("&radius=$radiusMeters")
                .append("&key=$apiKey")
            
            if (placesType.isNotBlank()) {
                urlBuilder.append("&type=$placesType")
            }
            if (keyword != null) {
                urlBuilder.append("&keyword=${URLEncoder.encode(keyword, "UTF-8")}")
            }
            
            Log.d(TAG, "Nearby search: $poiType at ${location.latitude},${location.longitude}")
            
            val response = executeRequest(urlBuilder.toString())
            parseSearchResponse(response, filters)
            
        } catch (e: Exception) {
            Log.e(TAG, "Nearby search failed", e)
            PlacesResult.Failure("Search failed: ${e.message}")
        }
    }
    
    /**
     * Text-based place search with location bias.
     * 
     * @param query Search query text
     * @param locationBias Optional location to bias results toward
     * @return PlacesResult with matching places or failure
     */
    suspend fun searchText(
        query: String,
        locationBias: LatLng? = null
    ): PlacesResult = withContext(Dispatchers.IO) {
        // Tier enforcement
        val tierCheck = checkTierAccess()
        if (tierCheck != null) return@withContext tierCheck
        
        // API key check
        val apiKey = BuildConfig.GOOGLE_PLACES_API_KEY
        if (apiKey.isBlank()) {
            Log.w(TAG, "Google Places API key not configured")
            return@withContext PlacesResult.Failure("Places API key not configured")
        }
        
        try {
            val urlBuilder = StringBuilder()
                .append("$BASE_URL/textsearch/json?")
                .append("query=${URLEncoder.encode(query, "UTF-8")}")
                .append("&key=$apiKey")
            
            if (locationBias != null) {
                urlBuilder.append("&location=${locationBias.latitude},${locationBias.longitude}")
                urlBuilder.append("&radius=$DEFAULT_RADIUS_METERS")
            }
            
            Log.d(TAG, "Text search: $query")
            
            val response = executeRequest(urlBuilder.toString())
            parseSearchResponse(response, POIFilters())
            
        } catch (e: Exception) {
            Log.e(TAG, "Text search failed", e)
            PlacesResult.Failure("Search failed: ${e.message}")
        }
    }
    
    /**
     * Check tier access for Places API.
     * Returns null if access allowed, or PlacesResult.Failure if blocked.
     */
    private fun checkTierAccess(): PlacesResult.Failure? {
        // SafeMode check
        if (SafeModeManager.isSafeModeEnabled()) {
            Log.w(TAG, "Places blocked - SafeMode active")
            return PlacesResult.Failure("Safe mode active - Places search disabled")
        }
        
        // Tier checks
        when {
            TierManager.isFree() -> {
                Log.w(TAG, "Places blocked - Free tier")
                return PlacesResult.Failure("POI search requires Plus subscription. Upgrade to search for places.")
            }
            TierManager.isPro() -> {
                Log.w(TAG, "Places blocked - Pro tier uses HERE")
                return PlacesResult.Failure("Truck-specific POI search coming soon. Pro tier uses HERE SDK for truck routing.")
            }
        }
        
        // Plus tier - allowed
        return null
    }
    
    /**
     * Execute HTTP GET request.
     */
    private fun executeRequest(urlString: String): String {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        
        return try {
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().readText()
            } else {
                val error = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                throw Exception("HTTP ${connection.responseCode}: $error")
            }
        } finally {
            connection.disconnect()
        }
    }
    
    /**
     * Parse Places API JSON response.
     */
    private fun parseSearchResponse(json: String, filters: POIFilters): PlacesResult {
        val jsonObject = JSONObject(json)
        val status = jsonObject.optString("status", "UNKNOWN")
        
        if (status != "OK" && status != "ZERO_RESULTS") {
            val errorMessage = jsonObject.optString("error_message", "Places API error: $status")
            Log.w(TAG, "Places API status: $status - $errorMessage")
            return PlacesResult.Failure(errorMessage)
        }
        
        val resultsArray = jsonObject.optJSONArray("results") ?: JSONArray()
        val places = mutableListOf<PlaceResult>()
        
        for (i in 0 until minOf(resultsArray.length(), MAX_RESULTS)) {
            val placeJson = resultsArray.getJSONObject(i)
            val place = parsePlaceResult(placeJson)
            
            // Apply filters
            if (matchesFilters(place, filters)) {
                places.add(place)
            }
        }
        
        Log.d(TAG, "Found ${places.size} places matching filters")
        return PlacesResult.Success(places)
    }
    
    /**
     * Parse individual place result.
     */
    private fun parsePlaceResult(json: JSONObject): PlaceResult {
        val geometry = json.optJSONObject("geometry")
        val location = geometry?.optJSONObject("location")
        val lat = location?.optDouble("lat", 0.0) ?: 0.0
        val lng = location?.optDouble("lng", 0.0) ?: 0.0
        
        val typesArray = json.optJSONArray("types") ?: JSONArray()
        val types = mutableListOf<String>()
        for (i in 0 until typesArray.length()) {
            types.add(typesArray.getString(i))
        }
        
        val name = json.optString("name", "Unknown")
        val attributes = inferAttributes(name, types)
        
        return PlaceResult(
            name = name,
            lat = lat,
            lng = lng,
            placeId = json.optString("place_id", ""),
            address = json.optString("vicinity", json.optString("formatted_address")),
            rating = json.optDouble("rating", -1.0).takeIf { it >= 0 },
            types = types,
            attributes = attributes
        )
    }
    
    /**
     * Infer place attributes from name and types.
     * Uses heuristics since Places API doesn't provide these directly.
     */
    private fun inferAttributes(name: String, types: List<String>): PlaceAttributes {
        val nameLower = name.lowercase()
        val isTruckStop = nameLower.contains("truck") || 
                         nameLower.contains("pilot") || 
                         nameLower.contains("flying j") ||
                         nameLower.contains("loves") ||
                         nameLower.contains("ta ") ||
                         nameLower.contains("petro")
        
        return PlaceAttributes(
            hasShowers = isTruckStop || nameLower.contains("shower"),
            truckParking = isTruckStop || nameLower.contains("truck parking"),
            overnightAllowed = isTruckStop || types.contains("lodging"),
            dieselAvailable = isTruckStop || 
                             nameLower.contains("diesel") || 
                             types.contains("gas_station"),
            hazmatFriendly = isTruckStop && (nameLower.contains("pilot") || 
                                            nameLower.contains("flying j"))
        )
    }
    
    /**
     * Check if place matches filter criteria.
     */
    private fun matchesFilters(place: PlaceResult, filters: POIFilters): Boolean {
        val attrs = place.attributes
        
        // Check each filter if specified
        if (filters.hasShowers == true && !attrs.hasShowers) return false
        if (filters.hasTruckParking == true && !attrs.truckParking) return false
        if (filters.hasOvernightParking == true && !attrs.overnightAllowed) return false
        if (filters.hasDiesel == true && !attrs.dieselAvailable) return false
        if (filters.hasHazmatAccess == true && !attrs.hazmatFriendly) return false
        if (filters.minRating != null && (place.rating ?: 0.0) < filters.minRating) return false
        
        return true
    }
    
    /**
     * Get nearby radius based on context.
     */
    fun getSearchRadius(isNearMe: Boolean): Int {
        return if (isNearMe) NEARBY_RADIUS_METERS else DEFAULT_RADIUS_METERS
    }
}

/**
 * Result of a Places API search.
 */
sealed class PlacesResult {
    data class Success(val places: List<PlaceResult>) : PlacesResult()
    data class Failure(val reason: String) : PlacesResult()
}

/**
 * Individual place result from search.
 */
data class PlaceResult(
    val name: String,
    val lat: Double,
    val lng: Double,
    val placeId: String,
    val address: String?,
    val rating: Double?,
    val types: List<String>,
    val attributes: PlaceAttributes
) {
    fun toLatLng(): LatLng = LatLng(lat, lng)
}

/**
 * Inferred attributes for a place.
 * Based on heuristics since Google Places doesn't provide these directly.
 */
data class PlaceAttributes(
    val hasShowers: Boolean = false,
    val truckParking: Boolean = false,
    val overnightAllowed: Boolean = false,
    val dieselAvailable: Boolean = false,
    val hazmatFriendly: Boolean = false
)
