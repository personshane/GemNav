package com.gemnav.app.ui.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemnav.app.models.Destination
import com.gemnav.core.feature.FeatureGate
import com.gemnav.core.shim.GeminiShim
import com.gemnav.core.shim.MapsShim
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * SearchViewModel - Handles search functionality with feature gating.
 */
class SearchViewModel : ViewModel() {
    
    companion object {
        private const val TAG = "SearchViewModel"
        private const val DEBOUNCE_MS = 300L
    }
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery
    
    private val _searchResults = MutableStateFlow<List<Destination>>(emptyList())
    val searchResults: StateFlow<List<Destination>> = _searchResults
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    
    private val _featureSummary = MutableStateFlow(FeatureGate.getFeatureSummary())
    val featureSummary: StateFlow<FeatureGate.FeatureSummary> = _featureSummary
    
    private var searchJob: Job? = null
    
    /**
     * Update search query with debounce.
     */
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _errorMessage.value = null
        
        // Cancel previous search
        searchJob?.cancel()
        
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        
        // Debounce search
        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            performSearch(query)
        }
    }
    
    /**
     * Perform search with feature gating.
     */
    private suspend fun performSearch(query: String) {
        _isLoading.value = true
        
        try {
            // Try AI-powered search first if enabled
            if (FeatureGate.areAIFeaturesEnabled()) {
                val aiResults = performAISearch(query)
                if (aiResults.isNotEmpty()) {
                    _searchResults.value = aiResults
                    _isLoading.value = false
                    return
                }
            }
            
            // Fall back to Maps SDK search if enabled
            if (FeatureGate.areInAppMapsEnabled()) {
                val mapsResults = performMapsSearch(query)
                _searchResults.value = mapsResults
            } else {
                // Free tier fallback - trigger external intent
                Log.d(TAG, "Search blocked - would trigger Maps intent for: $query")
                _errorMessage.value = "Opening Google Maps..."
                // TODO: Trigger Google Maps intent
                _searchResults.value = emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Search failed", e)
            _errorMessage.value = "Search failed. Please try again."
            _searchResults.value = emptyList()
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * AI-powered search using Gemini.
     * Gated by FeatureGate.areAIFeaturesEnabled()
     */
    private suspend fun performAISearch(query: String): List<Destination> {
        if (!FeatureGate.areAIFeaturesEnabled()) {
            Log.d(TAG, "AI search blocked - feature not enabled")
            return emptyList()
        }
        
        return try {
            Log.d(TAG, "Performing AI search for: $query")
            val intent = GeminiShim.parseNavigationIntent(query)
            if (intent != null) {
                // TODO: Convert NavigationIntent to Destination list
                Log.d(TAG, "AI parsed destination: ${intent.destination}")
            }
            // TODO: Implement actual conversion
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "AI search failed", e)
            emptyList()
        }
    }
    
    /**
     * Maps SDK search.
     * Gated by FeatureGate.areInAppMapsEnabled()
     */
    private fun performMapsSearch(query: String): List<Destination> {
        if (!FeatureGate.areInAppMapsEnabled()) {
            Log.d(TAG, "Maps search blocked - feature not enabled")
            return emptyList()
        }
        
        return try {
            Log.d(TAG, "Performing Maps search for: $query")
            val places = MapsShim.searchPlaces(query)
            places.map { place ->
                Destination(
                    id = place.placeId,
                    name = place.name,
                    address = place.address,
                    latitude = place.latitude,
                    longitude = place.longitude
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Maps search failed", e)
            emptyList()
        }
    }
    
    /**
     * Clear search results.
     */
    fun clearSearch() {
        searchJob?.cancel()
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _errorMessage.value = null
    }
    
    /**
     * Refresh feature state.
     */
    fun refreshFeatureState() {
        _featureSummary.value = FeatureGate.getFeatureSummary()
    }
}
