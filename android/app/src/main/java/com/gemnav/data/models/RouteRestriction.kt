package com.gemnav.data.models

data class RouteRestriction(
    val type: RestrictionType,
    val location: LatLng,
    val description: String
)
