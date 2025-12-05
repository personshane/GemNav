package com.gemnav.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class LocationService(context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationRequest: LocationRequest =
        LocationRequest.Builder(1000) // 1s interval
            .setMinUpdateIntervalMillis(500) // faster when in motion
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .build()

    @SuppressLint("MissingPermission") // Permission checked by caller
    fun locationUpdates(): Flow<Location> = callbackFlow {

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it).isSuccess }
            }
        }

        fusedClient.requestLocationUpdates(locationRequest, callback, null)

        awaitClose {
            fusedClient.removeLocationUpdates(callback)
        }
    }
}
