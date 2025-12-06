package com.gemnav.routing.domain

/**
 * Core route domain models shared across engines.
 */

data class Route(
    val engineName: String,
    val legs: List<RouteLeg>,
    val distanceMeters: Long,
    val durationSeconds: Long,
    val polylinePoints: List<LatLng>
)

data class RouteLeg(
    val steps: List<RouteStep>,
    val distanceMeters: Long,
    val durationSeconds: Long
)

data class RouteStep(
    val instruction: String,
    val maneuver: Maneuver,
    val distanceMeters: Long,
    val durationSeconds: Long,
    val startLocation: LatLng,
    val endLocation: LatLng
)

data class Maneuver(
    val type: ManeuverType,
    val streetName: String? = null,
    val exitNumber: String? = null
)

enum class ManeuverType {
    UNKNOWN,
    START,
    CONTINUE,
    TURN_LEFT,
    TURN_RIGHT,
    SLIGHT_LEFT,
    SLIGHT_RIGHT,
    SHARP_LEFT,
    SHARP_RIGHT,
    U_TURN,
    ENTER_HIGHWAY,
    EXIT_HIGHWAY,
    ROUNDABOUT_ENTER,
    ROUNDABOUT_EXIT,
    ARRIVE_DESTINATION
}
