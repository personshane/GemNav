package com.gemnav.app.truck.domain

import com.gemnav.app.truck.model.HazmatClass
import com.gemnav.app.truck.model.TruckType

/**
 * Routing constraints derived from a TruckProfile.
 * The routing engine should map these to HERE / Google specific options.
 */
data class TruckRoutingConstraints(
    val heightMeters: Double,
    val widthMeters: Double,
    val lengthMeters: Double,
    val weightTons: Double,
    val axleCount: Int,
    val hazmatClass: HazmatClass,
    val truckType: TruckType,
    val avoidTolls: Boolean,
    val avoidFerries: Boolean,
    val avoidLowBridges: Boolean
)
