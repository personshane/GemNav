package com.gemnav.data.models

data class TruckProfile(
    val heightMeters: Double,
    val lengthMeters: Double,
    val widthMeters: Double,
    val grossWeightKg: Double,
    val axleCount: Int,
    val hazmat: Boolean,
    val trailerCount: Int
)
