package com.gemnav.data.models

data class RouteResult(
    val polyline: String,
    val distanceMeters: Int,
    val durationSeconds: Int,
    val restrictions: List<RouteRestriction> = emptyList()
)
