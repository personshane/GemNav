package com.gemnav.trips

import android.content.Context
import android.location.Location
import com.gemnav.data.db.DatabaseProvider
import com.gemnav.data.db.entities.TripLogEntity
import com.gemnav.location.LocationRepository
import com.gemnav.utils.PolylineEncoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TripLogger(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val repo = LocationRepository(context)
    private val db = DatabaseProvider.getDatabase(context)

    private var tripJob: Job? = null
    private var isTripActive = false

    private val pathPoints = mutableListOf<Location>()
    private var distanceMeters: Double = 0.0
    private var tripStartTime: Long = 0L

    fun startTrip() {
        if (isTripActive) return
        isTripActive = true
        tripStartTime = System.currentTimeMillis()

        tripJob = scope.launch {
            repo.streamLocation().collectLatest { loc ->
                handleNewLocation(loc)
            }
        }
    }

    private fun handleNewLocation(location: Location) {
        if (pathPoints.isNotEmpty()) {
            val last = pathPoints.last()
            distanceMeters += last.distanceTo(location)
        }
        pathPoints.add(location)
    }

    fun stopTrip() {
        if (!isTripActive) return
        isTripActive = false

        tripJob?.cancel()
        tripJob = null

        val endTime = System.currentTimeMillis()

        val encoded = PolylineEncoder.encodePath(pathPoints)

        scope.launch {
            db.tripLogDao().insertTrip(
                TripLogEntity(
                    startTimestamp = tripStartTime,
                    endTimestamp = endTime,
                    distanceMeters = distanceMeters,
                    encodedPath = encoded
                )
            )
        }

        // reset for next trip
        pathPoints.clear()
        distanceMeters = 0.0
        tripStartTime = 0L
    }
}
