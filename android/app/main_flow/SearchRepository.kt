package com.gemnav.android.app.main_flow

import com.gemnav.android.app.api.PlacesApiClient
import com.gemnav.android.app.api.PlacePrediction
import com.gemnav.android.app.api.PlaceDetails
import com.gemnav.android.app.main_flow.database.SearchHistoryDao
import com.gemnav.android.app.main_flow.database.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepository @Inject constructor(
    private val searchHistoryDao: SearchHistoryDao,
    private val placesClient: PlacesApiClient
) {
    
    /**
     * Search for places using Google Places API.
     * Returns autocomplete predictions.
     * 
     * TIER: Plus/Pro only (Free tier uses intents)
     */
    suspend fun searchPlaces(
        query: String,
        latitude: Double? = null,
        longitude: Double? = null
    ): List<PlacePrediction> {
        return try {
            placesClient.searchPlaces(query, latitude, longitude)
        } catch (e: Exception) {
            // Log error and return empty list
            emptyList()
        }
    }
    
    /**
     * Get detailed information for a place.
     * 
     * TIER: Plus/Pro only
     */
    suspend fun getPlaceDetails(placeId: String): PlaceDetails? {
        return try {
            placesClient.getPlaceDetails(placeId)
        } catch (e: Exception) {
            // Log error and return null
            null
        }
    }
    
    /**
     * Get recent search history from local database.
     */
    fun getRecentSearches(limit: Int = 20): Flow<List<SearchHistoryEntity>> {
        return searchHistoryDao.getRecentSearches(limit)
    }
    
    /**
     * Search local history by prefix for autocomplete.
     */
    suspend fun searchByPrefix(prefix: String, limit: Int = 5): List<String> {
        return searchHistoryDao.searchByPrefix(prefix, limit)
    }
    
    /**
     * Save search to history after user selects a result.
     */
    suspend fun saveSearch(
        query: String,
        resultCount: Int = 0,
        selectedPlaceId: String? = null
    ): Long {
        return searchHistoryDao.insert(
            SearchHistoryEntity(
                query = query,
                resultCount = resultCount,
                selectedPlaceId = selectedPlaceId
            )
        )
    }
    
    /**
     * Clear old search history entries.
     */
    suspend fun clearOldSearches(daysToKeep: Int = 30) {
        val cutoffTimestamp = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        searchHistoryDao.deleteOlderThan(cutoffTimestamp)
    }
    
    /**
     * Clear all search history.
     */
    suspend fun clearAllSearches() {
        searchHistoryDao.clearAll()
    }
    
    /**
     * Clear Places API session token.
     * Call when user cancels search or switches context.
     */
    fun clearPlacesSession() {
        placesClient.clearSession()
    }
}
