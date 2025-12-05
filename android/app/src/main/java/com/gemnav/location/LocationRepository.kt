package com.gemnav.location

import android.content.Context
import android.location.Location
import kotlinx.coroutines.flow.Flow

class LocationRepository(context: Context) {

    private val service = LocationService(context)

    fun streamLocation(): Flow<Location> {
        return service.locationUpdates()
    }
}
