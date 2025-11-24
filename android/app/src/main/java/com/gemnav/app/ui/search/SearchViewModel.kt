package com.gemnav.app.ui.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemnav.app.models.Destination
import com.gemnav.core.feature.FeatureGate
import com.gemnav.core.shim.GeminiShim
import com.gemnav.core.shim.MapsShim
import com.gemnav.core.subscription.TierManager
import com.gemnav.data.ai.*
import com.gemnav.data.route.LatLng
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
    
    private val _aiRouteState = MutableStateFlow<AiRouteState>(AiRouteState.Idle)
    val aiRouteState: StateFlow<AiRouteState> = _aiRouteState
    
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
        _aiRouteState.value = AiRouteState.Idle
    }
    
    // ==================== AI ROUTING (MP-016) ====================
    
    /**
     * Request AI-generated route suggestion for query.
     * Gated by FeatureGate.areAIFeaturesEnabled()
     */
    fun onAiRouteRequested(query: String) {
        if (!FeatureGate.areAIFeaturesEnabled()) {
            Log.d(TAG, "AI routing blocked - feature not enabled")
            _aiRouteState.value = AiRouteState.Error("AI routing not available for your subscription tier")
            return
        }
        
        if (query.isBlank()) {
            _aiRouteState.value = AiRouteState.Error("Please enter a destination")
            return
        }
        
        _aiRouteState.value = AiRouteState.Loading
        
        viewModelScope.launch {
            try {
                val request = AiRouteRequest(
                    rawQuery = query,
                    currentLocation = null, // TODO: Hook up location provider
                    destinationHint = query,
                    tier = TierManager.getCurrentTier(),
                    isTruck = TierManager.isPro(), // Pro tier defaults to truck mode option
                    maxStops = FeatureGate.getMaxWaypoints()
                )
                
                val result = GeminiShim.getRouteSuggestion(request)
                
                _aiRouteState.value = when (result) {
                    is AiRouteResult.Success -> {
                        Log.i(TAG, "AI route success: ${result.suggestion.destinationName}")
                        AiRouteState.Success(result.suggestion)
                    }
                    is AiRouteResult.Failure -> {
                        Log.w(TAG, "AI route failed: ${result.reason}")
                        AiRouteState.Error(result.reason)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "AI routing exception", e)
                _aiRouteState.value = AiRouteState.Error("AI routing failed. Please try again.")
            }
        }
    }
    
    /**
     * Clear AI route state.
     */
    fun clearAiRouteState() {
        _aiRouteState.value = AiRouteState.Idle
    }
    
    /**
     * Refresh feature state.
     */
    fun refreshFeatureState() {
        _featureSummary.value = FeatureGate.getFeatureSummary()
    }
}
