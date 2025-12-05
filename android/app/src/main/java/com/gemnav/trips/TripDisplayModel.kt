package com.gemnav.trips

import com.google.android.gms.maps.model.LatLng

data class TripDisplayModel(
    val id: Long,
    val startTimeText: String,
    val endTimeText: String,
    val distanceText: String,
    val path: List<LatLng>
)
