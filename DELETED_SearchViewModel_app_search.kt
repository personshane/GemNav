package com.gemnav.android.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// MARK: - Data Models

data class SearchUiState(
    val results: List<SearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class SearchResult(
    val name: String,
    val address: String,
    val location: LatLng,
    val distance: Double,
    val rating: Float? = null,
    val placeId: String? = null
)

data class SearchFilters(
    val openNow: Boolean = false,
    val maxDistance: Double? = null,
    val minRating: Float? = null
)

enum class TruckPOIType(val displayName: String) {
    TRUCK_STOP("truck stops"),
    REST_AREA("rest areas"),
    TRUCK_PARKING("truck parking"),
    WEIGH_STATION("weigh stations"),
    TRUCK_WASH("truck washes"),
    TRUCK_REPAIR("truck repair shops"),
    FUEL_STATION("fuel stations")
}

enum class SubscriptionTier {
    FREE, PLUS, PRO;
    
    fun allowsAdvancedSearch(): Boolean {
        return this == PLUS || this == PRO
    }
}

// MARK: - Repository Interfaces

interface PlacesRepository {
    suspend fun searchAlongRoute(
        query: String,
        routePoints: List<LatLng>,
        filters: SearchFilters
    ): List<SearchResult>
    
    suspend fun searchTruckPOI(
        type: TruckPOIType,
        routePoints: List<LatLng>
    ): List<SearchResult>
}

interface RouteRepository {
    suspend fun getActiveRoutePoints(): List<LatLng>
}

// MARK: - SearchViewModel

class SearchViewModel @Inject constructor(
    private val placesRepository: PlacesRepository,
    private val routeRepository: RouteRepository,
    private val tier: SubscriptionTier
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    /**
     * Search for places along the active route
     * Requires Plus or Pro tier
     */
    suspend fun searchAlongRoute(
        query: String,
        filters: SearchFilters = SearchFilters()
    ): List<SearchResult> {
        if (!tier.allowsAdvancedSearch()) {
            return emptyList()
        }
        
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        return try {
            val routePoints = routeRepository.getActiveRoutePoints()
            if (routePoints.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "No active route"
                )
                return emptyList()
            }
            
            val results = placesRepository.searchAlongRoute(
                query = query,
                routePoints = routePoints,
                filters = filters
            )
            
            _uiState.value = _uiState.value.copy(
                results = results,
                isLoading = false
            )
            
            results
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Search failed: ${e.message}"
            )
            emptyList()
        }
    }
    
    /**
     * Find truck-specific POIs along route
     * Requires Pro tier
     */
    suspend fun findTruckPOI(type: TruckPOIType): List<SearchResult> {
        if (tier != SubscriptionTier.PRO) {
            return emptyList()
        }
        
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        return try {
            val routePoints = routeRepository.getActiveRoutePoints()
            if (routePoints.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "No active route"
                )
                return emptyList()
            }
            
            val results = placesRepository.searchTruckPOI(
                type = type,
                routePoints = routePoints
            )
            
            _uiState.value = _uiState.value.copy(
                results = results,
                isLoading = false
            )
            
            results
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Search failed: ${e.message}"
            )
            emptyList()
        }
    }
    
    fun clearResults() {
        _uiState.value = _uiState.value.copy(results = emptyList())
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
