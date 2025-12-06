package com.gemnav.routing.here

import com.gemnav.routing.domain.*

/**
 * Parses HERE route objects into our shared Route domain model.
 *
 * You must adjust the HERE types and mapping based on the actual SDK usage in GemNav.
 */
class HereRouteParser(
    private val maneuverMapper: HereManeuverMapper = HereManeuverMapper()
) {

    fun parse(hereRoute: Any): Route {
        // TODO: Replace 'Any' with actual HERE route type and map fields properly.

        // Placeholder skeleton:
        val legs = listOf<RouteLeg>() // Fill with parsed legs and steps
        return Route(
            engineName = "HERE",
            legs = legs,
            distanceMeters = 0L,
            durationSeconds = 0L,
            polylinePoints = emptyList()
        )
    }
}
