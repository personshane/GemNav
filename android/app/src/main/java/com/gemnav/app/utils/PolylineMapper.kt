package com.gemnav.app.utils

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions

object PolylineMapper {

    fun toPolylineOptions(points: List<LatLng>): PolylineOptions {
        return PolylineOptions()
            .addAll(points)
            .width(8f)
            .color(0xFF007AFF.toInt()) // iOS Blue, looks good on map
            .geodesic(true)
    }
}
