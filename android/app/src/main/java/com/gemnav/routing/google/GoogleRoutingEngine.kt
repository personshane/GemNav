package com.gemnav.routing.google

import android.util.Log
import com.gemnav.routing.domain.RouteRequest
import com.gemnav.routing.domain.RouteResult

/**
 * Google-based routing engine for GemNav Free/Basic tiers.
 *
 * Uses Google Directions REST API via GoogleDirectionsClient and parses
 * responses with GoogleRouteParser to produce RouteResult.
 */
class GoogleRoutingEngine(
    private val client: GoogleDirectionsClient = GoogleDirectionsClient(),
    private val parser: GoogleRouteParser = GoogleRouteParser()
) {

    suspend fun calculateRoute(request: RouteRequest): RouteResult {
        Log.d(TAG, "calculateRoute called for origin=${request.origin}, dest=${request.destination}")
        
        // Request directions from Google API
        val responseResult = client.requestDirections(request)
        
        return responseResult.fold(
            onSuccess = { json ->
                // Parse JSON response into RouteResult
                val parseResult = parser.parse(json, request)
                
                when (parseResult) {
                    is RouteResult.Success -> {
                        Log.d(TAG, "SUCCESS - route with ${parseResult.route.legs.size} legs, " +
                                "${parseResult.route.distanceMeters}m, " +
                                "${parseResult.route.durationSeconds}s")
                    }
                    is RouteResult.Failure -> {
                        Log.d(TAG, "FAILURE - ${parseResult.message}")
                    }
                }
                
                parseResult
            },
            onFailure = { throwable ->
                val message = "Google Directions request failed: ${throwable.message}"
                Log.e(TAG, message)
                RouteResult.Failure(
                    engineName = "GOOGLE",
                    message = message
                )
            }
        )
    }
    
    companion object {
        private const val TAG = "GoogleRoutingEngine"
    }
}
