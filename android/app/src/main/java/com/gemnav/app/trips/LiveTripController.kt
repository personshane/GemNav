package com.gemnav.app.trips

import android.content.Context
import android.location.Location
import com.gemnav.location.LocationRepository
import com.gemnav.app.utils.PolylineMapper
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LiveTripController(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main)
    private val locationRepo = LocationRepository(context)

    private val path = mutableListOf<LatLng>()
    private var distanceMeters = 0.0

    private var trackingJob: Job? = null

    fun start(onUpdate: (PolylineOptions, Double) -> Unit) {
        if (trackingJob != null) return

        trackingJob = scope.launch {
            locationRepo.streamLocation().collectLatest { loc ->
                handleNewLocation(loc, onUpdate)
            }
        }
    }

    fun stop() {
        trackingJob?.cancel()
        trackingJob = null
    }

    private fun handleNewLocation(
        location: Location,
        onUpdate: (PolylineOptions, Double) -> Unit
    ) {
        val point = LatLng(location.latitude, location.longitude)

        if (path.isNotEmpty()) {
            val last = path.last()
            val tempLoc = Location("").apply {
                latitude = last.latitude
                longitude = last.longitude
            }
            distanceMeters += tempLoc.distanceTo(location)
        }

        path.add(point)

        // Build live polyline
        val poly = PolylineMapper.toPolylineOptions(path)

        onUpdate(poly, distanceMeters)
    }

    fun getPath(): List<LatLng> = path.toList()

    fun getDistanceMeters(): Double = distanceMeters
}
