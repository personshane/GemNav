package com.gemnav.android.app.main_flow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemnav.android.app.main_flow.models.*
import com.gemnav.android.core.TierManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoutePreviewViewModel @Inject constructor(
    private val routeRepository: RouteRepository,
    private val tierManager: TierManager
) : ViewModel() {
    
    private val _selectedRoute = MutableStateFlow<Route?>(null)
    val selectedRoute: StateFlow<Route?> = _selectedRoute.asStateFlow()
    
    private val _alternativeRoutes = MutableStateFlow<List<Route>>(emptyList())
    val alternativeRoutes: StateFlow<List<Route>> = _alternativeRoutes.asStateFlow()
    
    private val _isCalculating = MutableStateFlow(false)
    val isCalculating: StateFlow<Boolean> = _isCalculating.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _routeOptions = MutableStateFlow(RouteOptions())
    val routeOptions: StateFlow<RouteOptions> = _routeOptions.asStateFlow()
    
    fun calculateRoute(
        origin: Destination,
        destination: Destination,
        waypoints: List<Destination> = emptyList()
    ) {
        viewModelScope.launch {
            _isCalculating.value = true
            _error.value = null
            
            routeRepository.calculateRoute(origin, destination, waypoints, _routeOptions.value)
                .onSuccess { route ->
                    _selectedRoute.value = route
                    // TODO: Calculate alternative routes for Plus/Pro tiers
                }
                .onFailure { throwable ->
                    _error.value = throwable.message ?: "Failed to calculate route"
                }
            
            _isCalculating.value = false
        }
    }
    
    fun selectRoute(route: Route) {
        _selectedRoute.value = route
    }
    
    fun updateRouteOptions(options: RouteOptions) {
        _routeOptions.value = options
    }
    
    fun clearError() {
        _error.value = null
    }
}
