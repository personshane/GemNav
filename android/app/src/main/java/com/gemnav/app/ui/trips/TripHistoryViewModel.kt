package com.gemnav.app.ui.trips

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gemnav.trips.TripDisplayMapper
import com.gemnav.trips.TripSummaryProvider
import com.gemnav.trips.TripDisplayModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TripHistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val provider = TripSummaryProvider(application)

    private val _trips = MutableStateFlow<List<TripDisplayModel>>(emptyList())
    val trips: StateFlow<List<TripDisplayModel>> = _trips

    init {
        viewModelScope.launch {
            provider.recentTrips().collect { summaries ->
                val uiList = TripDisplayMapper.map(getApplication(), summaries)
                _trips.value = uiList
            }
        }
    }
}
