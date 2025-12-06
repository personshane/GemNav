package com.gemnav.truck.domain

/**
 * Truck-specific routing constraints for truck routing engines.
 * 
 * This is a placeholder implementation. Full truck profile management
 * will be added in a future phase.
 */
data class TruckRoutingConstraints(
    val heightMeters: Double? = null,
    val widthMeters: Double? = null,
    val lengthMeters: Double? = null,
    val weightTons: Double? = null,
    val axleCount: Int? = null,
    val isHazmat: Boolean = false
)
