package com.gemnav.app.api

import android.content.Context
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PlaceTypes
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Client for Google Places API integration.
 * 
 * TIER RESTRICTIONS:
 * - Free tier: Not available (uses intents to Maps app)
 * - Plus tier: Full access
 * - Pro tier: Full access
 * 
 * Features:
 * - Autocomplete predictions for destination search
 * - Place details fetching (name, address, coordinates)
 * - Session token management for billing optimization
 */
class PlacesApiClient(context: Context) {
    
    private val placesClient: PlacesClient = Places.createClient(context)
    private var sessionToken: AutocompleteSessionToken? = null
    
    /**
     * Search for place predictions based on user input.
     * 
     * @param query User's search query
     * @param latitude Optional user latitude for location bias
     * @param longitude Optional user longitude for location bias
     * @return List of autocomplete predictions
     */
    suspend fun searchPlaces(
        query: String,
        latitude: Double? = null,
        longitude: Double? = null
    ): List<PlacePrediction> = suspendCancellableCoroutine { continuation ->
        
        // Create or reuse session token for billing optimization
        val token = sessionToken ?: AutocompleteSessionToken.newInstance().also {
            sessionToken = it
        }
        
        val request = FindAutocompletePredictionsRequest.builder()
            .setSessionToken(token)
            .setQuery(query)
            .apply {
                if (latitude != null && longitude != null) {
                    // Location bias would be set here if needed
                    // setOrigin() or setLocationBias() based on SDK version
                }
            }
            .build()
        
        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                val predictions = response.autocompletePredictions.map { prediction ->
                    PlacePrediction(
                        placeId = prediction.placeId,
                        primaryText = prediction.getPrimaryText(null).toString(),
                        secondaryText = prediction.getSecondaryText(null)?.toString() ?: "",
                        fullText = prediction.getFullText(null).toString()
                    )
                }
                continuation.resume(predictions)
            }
            .addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
        
        continuation.invokeOnCancellation {
            // Cleanup if needed
        }
    }
    
    /**
     * Fetch detailed information about a place.
     * 
     * @param placeId Google Place ID
     * @return Place details including coordinates, address, name
     */
    suspend fun getPlaceDetails(placeId: String): PlaceDetails = suspendCancellableCoroutine { continuation ->
        
        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG,
            Place.Field.TYPES,
            Place.Field.PHONE_NUMBER,
            Place.Field.RATING,
            Place.Field.USER_RATINGS_TOTAL
        )
        
        val request = FetchPlaceRequest.builder(placeId, placeFields)
            .setSessionToken(sessionToken)
            .build()
        
        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                val place = response.place
                val details = PlaceDetails(
                    placeId = place.id ?: "",
                    name = place.name ?: "",
                    address = place.address ?: "",
                    latitude = place.latLng?.latitude ?: 0.0,
                    longitude = place.latLng?.longitude ?: 0.0,
                    types = place.placeTypes?.map { it.toString() } ?: emptyList(),
                    phoneNumber = place.phoneNumber,
                    rating = place.rating,
                    userRatingsTotal = place.userRatingsTotal
                )
                
                // Clear session token after place details fetch (billing optimization)
                sessionToken = null
                
                continuation.resume(details)
            }
            .addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
        
        continuation.invokeOnCancellation {
            sessionToken = null
        }
    }
    
    /**
     * Search for specific place types near a location.
     * Used for gas stations, parking, restaurants, etc.
     * 
     * @param latitude Center latitude
     * @param longitude Center longitude
     * @param placeType Type of place to search for (e.g., "gas_station", "parking")
     * @param radius Search radius in meters
     * @return List of nearby places
     */
    suspend fun searchNearby(
        latitude: Double,
        longitude: Double,
        placeType: String,
        radius: Int = 5000
    ): List<PlaceDetails> {
        // Note: Nearby search requires Places API (New) or separate implementation
        // This is a placeholder for future implementation with Nearby Search API
        // For MVP, we can use autocomplete with type filters
        throw NotImplementedError("Nearby search will be implemented with Places API (New)")
    }
    
    /**
     * Clear the current session token.
     * Useful when user cancels a search or switches context.
     */
    fun clearSession() {
        sessionToken = null
    }
}

/**
 * Autocomplete prediction data class.
 */
data class PlacePrediction(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String,
    val fullText: String
)

/**
 * Place details data class.
 */
data class PlaceDetails(
    val placeId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val types: List<String>,
    val phoneNumber: String? = null,
    val rating: Double? = null,
    val userRatingsTotal: Int? = null
)