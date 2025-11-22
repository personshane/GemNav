package com.gemnav.android.app.main_flow.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RouteOptions(
    val avoidTolls: Boolean = false,
    val avoidHighways: Boolean = false,
    val avoidFerries: Boolean = false,
    val routeType: RouteType = RouteType.FASTEST,
    val includeTraffic: Boolean = true,
    val alternativeRoutes: Int = 0
) : Parcelable

@Parcelize
data class VehicleProfile(
    val type: VehicleType,
    val heightMeters: Double? = null,
    val widthMeters: Double? = null,
    val lengthMeters: Double? = null,
    val weightKg: Double? = null,
    val axleCount: Int? = null,
    val hazmatTypes: List<String> = emptyList()
) : Parcelable

enum class VehicleType {
    CAR,
    TRUCK,
    MOTORCYCLE,
    BICYCLE
}
