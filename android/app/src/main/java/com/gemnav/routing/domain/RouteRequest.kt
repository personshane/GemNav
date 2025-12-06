package com.gemnav.routing.domain

import com.gemnav.truck.domain.TruckRoutingConstraints

/**
 * Engine-agnostic route request used by all routing engines (HERE, Google, etc.).
 */
data class RouteRequest(
    val origin: LatLng,
    val destination: LatLng,
    val waypoints: List<LatLng> = emptyList(),
    val avoidTolls: Boolean = false,
    val avoidFerries: Boolean = false,
    val preferHighways: Boolean = true,
    val truckConstraints: TruckRoutingConstraints? = null
)
