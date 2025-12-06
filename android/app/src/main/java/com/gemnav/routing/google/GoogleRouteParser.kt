package com.gemnav.routing.google

import android.util.Log
import com.gemnav.routing.domain.*
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName

/**
 * Parses Google Directions API JSON response into Route domain model.
 *
 * Handles:
 * - Route polyline extraction via GooglePolylineDecoder
 * - Distance/duration aggregation
 * - Step-by-step maneuver mapping via GoogleManeuverMapper
 */
class GoogleRouteParser(
    private val maneuverMapper: GoogleManeuverMapper = GoogleManeuverMapper(),
    private val polylineDecoder: GooglePolylineDecoder = GooglePolylineDecoder,
    private val gson: Gson = Gson()
) {

    /**
     * Parses raw Google Directions JSON into Route.
     * Returns RouteResult for consistency with engine contract.
     */
    fun parse(jsonResponse: String, request: RouteRequest): RouteResult {
        return try {
            val response = gson.fromJson(jsonResponse, GoogleDirectionsResponse::class.java)
            
            // Check API status
            if (response.status != "OK") {
                val errorMessage = when (response.status) {
                    "ZERO_RESULTS" -> "No route found between origin and destination"
                    "NOT_FOUND" -> "One or more locations could not be geocoded"
                    "INVALID_REQUEST" -> "Invalid request parameters"
                    "REQUEST_DENIED" -> "API key issue or request denied"
                    "OVER_QUERY_LIMIT" -> "Query limit exceeded"
                    else -> "Google API returned status: ${response.status}"
                }
                Log.e(TAG, "Parse failed: $errorMessage")
                return RouteResult.Failure(
                    engineName = "GOOGLE",
                    message = errorMessage
                )
            }
            
            val route = response.routes.firstOrNull()
            if (route == null) {
                Log.e(TAG, "No routes in response despite OK status")
                return RouteResult.Failure(
                    engineName = "GOOGLE",
                    message = "No routes found in response"
                )
            }
            
            // Parse polyline for map visualization
            val polylinePoints = try {
                polylineDecoder.decode(route.overviewPolyline.points)
            } catch (e: Exception) {
                Log.e(TAG, "Polyline decode failed", e)
                emptyList()
            }
            
            // Parse legs (Google typically returns 1 leg for simple A->B routing)
            val legs = route.legs.map { googleLeg ->
                val steps = googleLeg.steps.map { googleStep ->
                    RouteStep(
                        instruction = stripHtml(googleStep.htmlInstructions),
                        maneuver = maneuverMapper.map(googleStep.maneuver),
                        distanceMeters = googleStep.distance.value.toLong(),
                        durationSeconds = googleStep.duration.value.toLong(),
                        startLocation = LatLng(
                            googleStep.startLocation.lat,
                            googleStep.startLocation.lng
                        ),
                        endLocation = LatLng(
                            googleStep.endLocation.lat,
                            googleStep.endLocation.lng
                        )
                    )
                }
                
                RouteLeg(
                    steps = steps,
                    distanceMeters = googleLeg.distance.value.toLong(),
                    durationSeconds = googleLeg.duration.value.toLong()
                )
            }
            
            val totalDistance = legs.sumOf { it.distanceMeters }
            val totalDuration = legs.sumOf { it.durationSeconds }
            
            Log.d(TAG, "Parse success: ${legs.size} legs, ${totalDistance}m, ${totalDuration}s, ${polylinePoints.size} polyline points")
            
            RouteResult.Success(
                Route(
                    engineName = "GOOGLE",
                    legs = legs,
                    distanceMeters = totalDistance,
                    durationSeconds = totalDuration,
                    polylinePoints = polylinePoints
                )
            )
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "JSON parse error", e)
            RouteResult.Failure(
                engineName = "GOOGLE",
                message = "Failed to parse Google Directions response: ${e.message}"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected parse error", e)
            RouteResult.Failure(
                engineName = "GOOGLE",
                message = "Route parsing failed: ${e.message}"
            )
        }
    }
    
    /**
     * Removes HTML tags from Google's html_instructions field.
     */
    private fun stripHtml(html: String): String {
        return html
            .replace("<[^>]*>".toRegex(), "")
            .replace("&nbsp;", " ")
            .trim()
    }
    
    companion object {
        private const val TAG = "GoogleRouteParser"
    }
}

// Google Directions API response DTOs
private data class GoogleDirectionsResponse(
    val status: String,
    val routes: List<GoogleRoute>,
    @SerializedName("error_message") val errorMessage: String? = null
)

private data class GoogleRoute(
    val legs: List<GoogleLeg>,
    @SerializedName("overview_polyline") val overviewPolyline: GooglePolyline
)

private data class GoogleLeg(
    val steps: List<GoogleStep>,
    val distance: GoogleDistance,
    val duration: GoogleDuration
)

private data class GoogleStep(
    @SerializedName("html_instructions") val htmlInstructions: String,
    val maneuver: String? = null,
    val distance: GoogleDistance,
    val duration: GoogleDuration,
    @SerializedName("start_location") val startLocation: GoogleLatLng,
    @SerializedName("end_location") val endLocation: GoogleLatLng,
    val polyline: GooglePolyline
)

private data class GooglePolyline(
    val points: String
)

private data class GoogleDistance(
    val value: Int,
    val text: String
)

private data class GoogleDuration(
    val value: Int,
    val text: String
)

private data class GoogleLatLng(
    val lat: Double,
    val lng: Double
)
