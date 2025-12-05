package com.gemnav.app.truck.model

data class TruckProfile(
    val name: String = "Default truck",
    val heightMeters: Double = 4.1,
    val widthMeters: Double = 2.6,
    val lengthMeters: Double = 18.0,
    val weightTons: Double = 36.0,
    val axleCount: Int = 5,
    val hazmatClass: HazmatClass = HazmatClass.NONE,
    val truckType: TruckType = TruckType.SEMI_TRACTOR_TRAILER,
    val avoidTolls: Boolean = false,
    val avoidFerries: Boolean = false,
    val avoidLowBridges: Boolean = true
)
