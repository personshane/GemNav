package com.gemnav.app.utils

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions

object PolylineMapper {

    fun toPolylineOptions(points: List<LatLng>): PolylineOptions {
        return PolylineOptions()
            .addAll(points)
            .width(12f)
            .color(0xFF007AFF.toInt()) // bright blue
            .geodesic(true)
            .startCap(com.google.android.gms.maps.model.RoundCap())
            .endCap(com.google.android.gms.maps.model.RoundCap())
            .jointType(com.google.android.gms.maps.model.JointType.ROUND)
    }

    fun dashed(points: List<LatLng>): PolylineOptions {
        return PolylineOptions()
            .addAll(points)
            .width(12f)
            .color(0xFF888888.toInt()) // gray dashed for "in progress"
            .geodesic(true)
            .pattern(listOf(com.google.android.gms.maps.model.Dash(40f), com.google.android.gms.maps.model.Gap(20f)))
    }
}
