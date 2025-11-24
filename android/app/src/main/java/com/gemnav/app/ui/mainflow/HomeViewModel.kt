package com.gemnav.app.ui.mainflow

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemnav.app.models.Destination
import com.gemnav.core.feature.FeatureGate
import com.gemnav.core.shim.GeminiShim
import com.gemnav.core.shim.MapsShim
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class HomeViewModel : ViewModel() {
    
    companion object {
        private const val TAG = "HomeViewModel"
    }
    
    private val _favorites = MutableStateFlow<List<Destination>>(emptyList())
    val favorites: StateFlow<List<Destination>> = _favorites

    private val _recent = MutableStateFlow<List<Destination>>(emptyList())
    val recent: StateFlow<List<Destination>> = _recent

    private val _home = MutableStateFlow<Destination?>(null)
    val home: StateFlow<Destination?> = _home

    private val _work = MutableStateFlow<Destination?>(null)
    val work: StateFlow<Destination?> = _work
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _featureSummary = MutableStateFlow(FeatureGate.getFeatureSummary())
    val featureSummary: StateFlow<FeatureGate.FeatureSummary> = _featureSummary

    init {
        refreshFeatureState()
    }
    
    /**
     * Refresh the feature gate state.
     */
    fun refreshFeatureState() {
        _featureSummary.value = FeatureGate.getFeatureSummary()
    }
    
    /**
     * Get AI-powered destination suggestions.
     * Gated by FeatureGate.areAIFeaturesEnabled()
     */
    fun getAISuggestions(query: String) {
        if (!FeatureGate.areAIFeaturesEnabled()) {
            Log.d(TAG, "AI suggestions blocked - feature not enabled")
            // TODO: Show fallback suggestions (recent/favorites)
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // TODO: Implement actual AI suggestion logic
                val response = GeminiShim.parseNavigationIntent(query)
                if (response != null) {
                    Log.d(TAG, "AI parsed intent: ${response.destination}")
                    // TODO: Convert intent to suggestions
                }
            } catch (e: Exception) {
                Log.e(TAG, "AI suggestions failed", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Search for places using Maps SDK.
     * Gated by FeatureGate.areInAppMapsEnabled() for SDK, falls back to intents.
     */
    fun searchPlaces(query: String) {
        if (!FeatureGate.areInAppMapsEnabled()) {
            Log.d(TAG, "In-app maps blocked - using intent fallback")
            // TODO: Trigger Google Maps app via intent
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val results = MapsShim.searchPlaces(query)
                Log.d(TAG, "Found ${results.size} places")
                // TODO: Convert to destinations and update state
            } catch (e: Exception) {
                Log.e(TAG, "Place search failed", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMockData() {
        _favorites.value = listOf(
            Destination(
                id = UUID.randomUUID().toString(),
                name = "Favorite 1",
                address = "123 Main St",
                latitude = 33.4484,
                longitude = -112.0740
            ),
            Destination(
                id = UUID.randomUUID().toString(),
                name = "Favorite 2",
                address = "555 Center Ave",
                latitude = 33.5484,
                longitude = -112.1740
            )
        )
        _recent.value = listOf(
            Destination(
                id = UUID.randomUUID().toString(),
                name = "Truck Stop",
                address = "AZ-95 Exit 12",
                latitude = 34.0484,
                longitude = -113.0740
            ),
            Destination(
                id = UUID.randomUUID().toString(),
                name = "Warehouse 32A",
                address = "Industrial Rd",
                latitude = 33.3484,
                longitude = -112.2740
            )
        )
        _home.value = Destination(
            id = UUID.randomUUID().toString(),
            name = "Home",
            address = "My House",
            latitude = 33.4484,
            longitude = -112.0740
        )
        _work.value = Destination(
            id = UUID.randomUUID().toString(),
            name = "Work",
            address = "Distribution Center",
            latitude = 33.5484,
            longitude = -112.1740
        )
    }
}
