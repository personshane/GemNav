package com.gemnav.routing.google

import com.gemnav.routing.domain.Maneuver
import com.gemnav.routing.domain.ManeuverType

/**
 * Maps Google Directions step/maneuver metadata to GemNav ManeuverType.
 *
 * You must adapt this to the actual Google step/maneuver representation once integrated.
 */
class GoogleManeuverMapper {

    fun map(googleStep: Any): Maneuver {
        // TODO: Replace 'Any' with the actual Google step type and map it to ManeuverType.
        return Maneuver(
            type = ManeuverType.UNKNOWN,
            streetName = null,
            exitNumber = null
        )
    }
}
