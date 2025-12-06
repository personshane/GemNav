package com.gemnav.routing.here

import com.gemnav.routing.domain.Maneuver
import com.gemnav.routing.domain.ManeuverType

/**
 * Maps HERE SDK maneuver types to GemNav's ManeuverType.
 *
 * You must wire this to the actual HERE maneuver type enum/class.
 */
class HereManeuverMapper {

    fun map(hereManeuver: Any): Maneuver {
        // TODO: Replace 'Any' with actual HERE maneuver type and map it to ManeuverType.

        // Placeholder maps everything to UNKNOWN.
        return Maneuver(
            type = ManeuverType.UNKNOWN,
            streetName = null,
            exitNumber = null
        )
    }
}
