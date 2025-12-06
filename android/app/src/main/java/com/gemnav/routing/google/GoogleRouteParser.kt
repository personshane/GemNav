package com.gemnav.routing.google

import com.gemnav.routing.domain.*

/**
 * Parses a Google Directions API response into the shared Route domain model.
 *
 * NOTE:
 * - 'googleResponse' is typed as Any to avoid coupling to a specific client library.
 * - You must adapt this to your actual Google response model if/when you wire up real HTTP calls.
 */
class GoogleRouteParser(
    private val maneuverMapper: GoogleManeuverMapper = GoogleManeuverMapper()
) {

    fun parse(googleResponse: Any): Route {
        // TODO: Replace 'Any' with the actual Google response type and implement real parsing.
        // For now, return an empty placeholder route to allow compilation.
        return Route(
            engineName = "GOOGLE",
            legs = emptyList(),
            distanceMeters = 0L,
            durationSeconds = 0L,
            polylinePoints = emptyList()
        )
    }
}
