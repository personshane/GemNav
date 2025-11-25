package com.gemnav.core.maps.google

import android.util.Log
import com.gemnav.app.BuildConfig
import com.gemnav.core.feature.FeatureGate
import com.gemnav.core.shim.SafeModeManager
import com.gemnav.data.route.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Google Directions API Client.
 * MP-019: Plus tier turn-by-turn routing.
 * 
 * Makes HTTP requests to Google Directions API and parses responses.
 */
object DirectionsApiClient {
    
    private const val TAG = "DirectionsApiClient"
    private const val BASE_URL = "https://maps.googleapis.com/maps/api/directions/json"
    private const val TIMEOUT_MS = 15000
    
    /**
     * Get driving route from Google Directions API.
     * 
     * @param origin Starting point
     * @param destination End point
     * @param waypoints Optional intermediate stops
     * @return DirectionsResult (Success or Failure)
     */
    suspend fun getRoute(
        origin: LatLng,
        destination: LatLng,
        waypoints: List<LatLng> = emptyList()
    ): DirectionsResult = withContext(Dispatchers.IO) {
        
        // SafeMode check
        if (SafeModeManager.isSafeModeEnabled()) {
            Log.w(TAG, "Directions API blocked - SafeMode active")
            return@withContext DirectionsResult.Failure(
                errorMessage = "Safe Mode is active",
                errorCode = DirectionsError.SAFE_MODE_ACTIVE
            )
        }
        
        // FeatureGate check - Plus tier minimum
        if (!FeatureGate.areInAppMapsEnabled()) {
            Log.w(TAG, "Directions API blocked - Free tier")
            return@withContext DirectionsResult.Failure(
                errorMessage = "Plus subscription required for routing",
                errorCode = DirectionsError.FEATURE_NOT_ENABLED
            )
        }
        
        // API key check
        val apiKey = BuildConfig.GOOGLE_MAPS_API_KEY
        if (apiKey.isBlank()) {
            Log.e(TAG, "Google Maps API key not configured")
            return@withContext DirectionsResult.Failure(
                errorMessage = "API key not configured",
                errorCode = DirectionsError.REQUEST_DENIED
            )
        }
        
        try {
            val url = buildUrl(origin, destination, waypoints, apiKey)
            Log.d(TAG, "Requesting route: ${origin.latitude},${origin.longitude} -> ${destination.latitude},${destination.longitude}")
            
            val response = makeRequest(url)
            parseResponse(response)
        } catch (e: Exception) {
            Log.e(TAG, "Directions API request failed", e)
            DirectionsResult.Failure(
                errorMessage = e.message ?: "Network error",
                errorCode = DirectionsError.NETWORK_ERROR
            )
        }
    }
    
    /**
     * Build the Directions API URL with parameters.
     */
    private fun buildUrl(
        origin: LatLng,
        destination: LatLng,
        waypoints: List<LatLng>,
        apiKey: String
    ): String {
        val originStr = "${origin.latitude},${origin.longitude}"
        val destStr = "${destination.latitude},${destination.longitude}"
        
        val params = mutableListOf(
            "origin=${URLEncoder.encode(originStr, "UTF-8")}",
            "destination=${URLEncoder.encode(destStr, "UTF-8")}",
            "mode=driving",
            "key=$apiKey"
        )
        
        // Add waypoints if present
        if (waypoints.isNotEmpty()) {
            val waypointsStr = waypoints.joinToString("|") { "${it.latitude},${it.longitude}" }
            params.add("waypoints=${URLEncoder.encode(waypointsStr, "UTF-8")}")
        }
        
        // TODO: Add traffic model for better ETA
        // params.add("departure_time=now")
        // params.add("traffic_model=best_guess")
        
        // TODO: Request alternative routes
        // params.add("alternatives=true")
        
        return "$BASE_URL?${params.joinToString("&")}"
    }

    
    /**
     * Make HTTP GET request to Directions API.
     */
    private fun makeRequest(urlString: String): String {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/json")
            
            val responseCode = connection.responseCode
            Log.d(TAG, "Directions API response code: $responseCode")
            
            return if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                throw Exception("HTTP $responseCode: $errorStream")
            }
        } finally {
            connection.disconnect()
        }
    }
    
    /**
     * Parse Directions API JSON response.
     */
    private fun parseResponse(jsonString: String): DirectionsResult {
        return try {
            val json = JSONObject(jsonString)
            val status = json.getString("status")
            
            Log.d(TAG, "Directions API status: $status")
            
            when (status) {
                "OK" -> parseSuccessResponse(json)
                "ZERO_RESULTS" -> DirectionsResult.Failure(
                    errorMessage = "No route found between locations",
                    errorCode = DirectionsError.ZERO_RESULTS
                )
                "NOT_FOUND" -> DirectionsResult.Failure(
                    errorMessage = "Location not found",
                    errorCode = DirectionsError.NOT_FOUND
                )
                "MAX_WAYPOINTS_EXCEEDED" -> DirectionsResult.Failure(
                    errorMessage = "Too many waypoints",
                    errorCode = DirectionsError.MAX_WAYPOINTS_EXCEEDED
                )
                "OVER_DAILY_LIMIT" -> DirectionsResult.Failure(
                    errorMessage = "API daily limit exceeded",
                    errorCode = DirectionsError.OVER_DAILY_LIMIT
                )
                "OVER_QUERY_LIMIT" -> DirectionsResult.Failure(
                    errorMessage = "API query limit exceeded",
                    errorCode = DirectionsError.OVER_QUERY_LIMIT
                )
                "REQUEST_DENIED" -> DirectionsResult.Failure(
                    errorMessage = json.optString("error_message", "Request denied"),
                    errorCode = DirectionsError.REQUEST_DENIED
                )
                "INVALID_REQUEST" -> DirectionsResult.Failure(
                    errorMessage = json.optString("error_message", "Invalid request"),
                    errorCode = DirectionsError.INVALID_REQUEST
                )
                else -> DirectionsResult.Failure(
                    errorMessage = "Unknown status: $status",
                    errorCode = DirectionsError.UNKNOWN_ERROR
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Directions response", e)
            DirectionsResult.Failure(
                errorMessage = "Failed to parse response: ${e.message}",
                errorCode = DirectionsError.PARSE_ERROR
            )
        }
    }
    
    /**
     * Parse successful Directions API response.
     */
    private fun parseSuccessResponse(json: JSONObject): DirectionsResult {
        val routes = json.getJSONArray("routes")
        if (routes.length() == 0) {
            return DirectionsResult.Failure(
                errorMessage = "No routes in response",
                errorCode = DirectionsError.ZERO_RESULTS
            )
        }
        
        val routeJson = routes.getJSONObject(0)
        val route = parseRoute(routeJson)
        
        // Decode overview polyline
        val polylineEncoded = routeJson.getJSONObject("overview_polyline").getString("points")
        val polylineCoordinates = PolylineDecoder.decode(polylineEncoded)
        
        // Calculate totals from legs
        var totalDistance = 0
        var totalDuration = 0
        val legs = routeJson.getJSONArray("legs")
        for (i in 0 until legs.length()) {
            val leg = legs.getJSONObject(i)
            totalDistance += leg.getJSONObject("distance").getInt("value")
            totalDuration += leg.getJSONObject("duration").getInt("value")
        }
        
        Log.i(TAG, "Route parsed: ${totalDistance}m, ${totalDuration}s, ${polylineCoordinates.size} points")
        
        return DirectionsResult.Success(
            route = route,
            polylineCoordinates = polylineCoordinates,
            totalDistanceMeters = totalDistance,
            totalDurationSeconds = totalDuration
        )
    }
    
    /**
     * Parse a single route from JSON.
     */
    private fun parseRoute(routeJson: JSONObject): DirectionsRoute {
        val legs = mutableListOf<DirectionsLeg>()
        val legsJson = routeJson.getJSONArray("legs")
        
        for (i in 0 until legsJson.length()) {
            legs.add(parseLeg(legsJson.getJSONObject(i)))
        }
        
        val warnings = mutableListOf<String>()
        if (routeJson.has("warnings")) {
            val warningsJson = routeJson.getJSONArray("warnings")
            for (i in 0 until warningsJson.length()) {
                warnings.add(warningsJson.getString(i))
            }
        }
        
        return DirectionsRoute(
            summary = routeJson.optString("summary", ""),
            legs = legs,
            overviewPolyline = DirectionsPolyline(
                routeJson.getJSONObject("overview_polyline").getString("points")
            ),
            copyrights = routeJson.optString("copyrights"),
            warnings = warnings
        )
    }

    
    /**
     * Parse a leg from JSON.
     */
    private fun parseLeg(legJson: JSONObject): DirectionsLeg {
        val steps = mutableListOf<DirectionsStep>()
        val stepsJson = legJson.getJSONArray("steps")
        
        for (i in 0 until stepsJson.length()) {
            steps.add(parseStep(stepsJson.getJSONObject(i)))
        }
        
        return DirectionsLeg(
            startAddress = legJson.optString("start_address", ""),
            endAddress = legJson.optString("end_address", ""),
            startLocation = parseLatLng(legJson.getJSONObject("start_location")),
            endLocation = parseLatLng(legJson.getJSONObject("end_location")),
            distance = parseValue(legJson.getJSONObject("distance")),
            duration = parseValue(legJson.getJSONObject("duration")),
            durationInTraffic = legJson.optJSONObject("duration_in_traffic")?.let { parseValue(it) },
            steps = steps
        )
    }
    
    /**
     * Parse a step from JSON.
     */
    private fun parseStep(stepJson: JSONObject): DirectionsStep {
        return DirectionsStep(
            htmlInstructions = stepJson.optString("html_instructions", ""),
            distance = parseValue(stepJson.getJSONObject("distance")),
            duration = parseValue(stepJson.getJSONObject("duration")),
            startLocation = parseLatLng(stepJson.getJSONObject("start_location")),
            endLocation = parseLatLng(stepJson.getJSONObject("end_location")),
            polyline = DirectionsPolyline(
                stepJson.getJSONObject("polyline").getString("points")
            ),
            maneuver = stepJson.optString("maneuver", null),
            travelMode = stepJson.optString("travel_mode", "DRIVING")
        )
    }
    
    /**
     * Parse lat/lng from JSON.
     */
    private fun parseLatLng(json: JSONObject): DirectionsLatLng {
        return DirectionsLatLng(
            lat = json.getDouble("lat"),
            lng = json.getDouble("lng")
        )
    }
    
    /**
     * Parse distance/duration value from JSON.
     */
    private fun parseValue(json: JSONObject): DirectionsValue {
        return DirectionsValue(
            text = json.getString("text"),
            value = json.getInt("value")
        )
    }
    
    /**
     * Strip HTML tags from instruction string.
     */
    fun stripHtml(html: String): String {
        return html
            .replace(Regex("<[^>]*>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .trim()
    }
    
    // ==================== MP-023: WAYPOINT HELPERS ====================
    
    /**
     * Get route with a single waypoint (for detour calculation).
     * 
     * @param origin Starting point
     * @param destination Final destination
     * @param waypoint Intermediate stop (POI)
     * @return DirectionsResult (Success or Failure)
     */
    suspend fun getRouteWithWaypoint(
        origin: LatLng,
        destination: LatLng,
        waypoint: LatLng
    ): DirectionsResult {
        return getRoute(origin, destination, listOf(waypoint))
    }
    
    /**
     * Get route with multiple waypoints.
     * 
     * @param origin Starting point
     * @param destination Final destination
     * @param waypoints Intermediate stops in order
     * @return DirectionsResult (Success or Failure)
     */
    suspend fun getRouteWithMultipleWaypoints(
        origin: LatLng,
        destination: LatLng,
        waypoints: List<LatLng>
    ): DirectionsResult {
        return getRoute(origin, destination, waypoints)
    }
}
