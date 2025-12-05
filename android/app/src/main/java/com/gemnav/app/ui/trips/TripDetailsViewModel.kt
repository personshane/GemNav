package com.gemnav.app.ui.trips

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gemnav.data.db.DatabaseProvider
import com.gemnav.trips.TripDisplayModel
import com.gemnav.trips.TripSummary
import com.gemnav.trips.TripDisplayMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TripDetailsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val db = DatabaseProvider.getDatabase(application)

    private val _trip = MutableStateFlow<TripDisplayModel?>(null)
    val trip: StateFlow<TripDisplayModel?> = _trip

    fun loadTrip(id: Long) {
        viewModelScope.launch {
            val entity = db.tripLogDao().getTripById(id)
            if (entity != null) {
                val summary = TripSummary(
                    id = entity.id,
                    startTimestamp = entity.startTimestamp,
                    endTimestamp = entity.endTimestamp,
                    distanceMeters = entity.distanceMeters,
                    encodedPath = entity.encodedPath
                )
                val list = listOf(summary)
                val mapped = TripDisplayMapper.map(getApplication(), list)
                _trip.value = mapped.firstOrNull()
            }
        }
    }
}
