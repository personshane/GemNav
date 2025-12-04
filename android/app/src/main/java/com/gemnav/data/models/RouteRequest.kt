package com.gemnav.data.models

data class RouteRequest(
    val origin: LatLng,
    val destination: LatLng,
    val waypoints: List<LatLng> = emptyList(),
    val avoidTolls: Boolean = false,
    val avoidHighways: Boolean = false,
    val useTruckRouting: Boolean = false
)
