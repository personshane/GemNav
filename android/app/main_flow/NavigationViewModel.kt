package com.gemnav.android.app.main_flow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemnav.android.app.main_flow.models.Route
import com.gemnav.android.core.TierManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val tierManager: TierManager
) : ViewModel() {
    
    private val _navigationState = MutableStateFlow<NavigationState>(NavigationState.Idle)
    val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()
    
    private val _currentRoute = MutableStateFlow<Route?>(null)
    val currentRoute: StateFlow<Route?> = _currentRoute.asStateFlow()
    
    fun startNavigation(route: Route) {
        _currentRoute.value = route
        
        when (tierManager.currentTier) {
            TierManager.Tier.FREE -> startFreeNavigation(route)
            TierManager.Tier.PLUS -> startPlusNavigation(route)
            TierManager.Tier.PRO -> startProNavigation(route)
        }
    }
    
    private fun startFreeNavigation(route: Route) {
        // Launch Google Maps app with intent
        _navigationState.value = NavigationState.LaunchingExternalApp(route)
    }
    
    private fun startPlusNavigation(route: Route) {
        // Start in-app navigation with Google Maps SDK
        _navigationState.value = NavigationState.Navigating(route)
    }
    
    private fun startProNavigation(route: Route) {
        // Start navigation with appropriate SDK (HERE or Google)
        _navigationState.value = NavigationState.Navigating(route)
    }
    
    fun stopNavigation() {
        _navigationState.value = NavigationState.Idle
        _currentRoute.value = null
    }
    
    fun pauseNavigation() {
        _navigationState.value = NavigationState.Paused
    }
    
    fun resumeNavigation() {
        _currentRoute.value?.let { route ->
            _navigationState.value = NavigationState.Navigating(route)
        }
    }
}

sealed class NavigationState {
    object Idle : NavigationState()
    data class LaunchingExternalApp(val route: Route) : NavigationState()
    data class Navigating(val route: Route) : NavigationState()
    object Paused : NavigationState()
}
