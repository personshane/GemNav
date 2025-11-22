package com.gemnav.android.app.main_flow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemnav.android.app.main_flow.models.Destination
import com.gemnav.android.core.TierManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val destinationRepository: DestinationRepository,
    private val searchRepository: SearchRepository,
    private val tierManager: TierManager
) : ViewModel() {
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _searchResults = MutableStateFlow<List<Destination>>(emptyList())
    val searchResults: StateFlow<List<Destination>> = _searchResults.asStateFlow()
    
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    
    val recentDestinations = destinationRepository.getRecentDestinations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val favorites = destinationRepository.getFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    private val _home = MutableStateFlow<Destination?>(null)
    val home: StateFlow<Destination?> = _home.asStateFlow()
    
    private val _work = MutableStateFlow<Destination?>(null)
    val work: StateFlow<Destination?> = _work.asStateFlow()
    
    init {
        loadQuickActions()
    }
    
    private fun loadQuickActions() {
        viewModelScope.launch {
            _home.value = destinationRepository.getHome()
            _work.value = destinationRepository.getWork()
        }
    }
    
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.length >= 3) {
            performSearch(query)
        } else {
            _searchResults.value = emptyList()
        }
    }
    
    private fun performSearch(query: String) {
        viewModelScope.launch {
            _isSearching.value = true
            // TODO: Implement Places API search for Plus/Pro tiers
            // For now, just search local history
            _searchResults.value = emptyList()
            _isSearching.value = false
        }
    }
    
    fun onDestinationSelected(destination: Destination) {
        viewModelScope.launch {
            destinationRepository.saveDestination(destination)
            searchRepository.saveSearch(destination.name, 1, destination.placeId)
        }
    }
    
    fun toggleFavorite(destination: Destination) {
        viewModelScope.launch {
            destinationRepository.toggleFavorite(destination)
        }
    }
    
    fun setAsHome(destination: Destination) {
        viewModelScope.launch {
            destinationRepository.setAsHome(destination)
            _home.value = destination
        }
    }
    
    fun setAsWork(destination: Destination) {
        viewModelScope.launch {
            destinationRepository.setAsWork(destination)
            _work.value = destination
        }
    }
}
