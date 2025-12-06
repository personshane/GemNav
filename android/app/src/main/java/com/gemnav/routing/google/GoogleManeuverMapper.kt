package com.gemnav.routing.google

import com.gemnav.routing.domain.Maneuver
import com.gemnav.routing.domain.ManeuverType

/**
 * Maps Google Directions maneuver strings to GemNav ManeuverType enum.
 *
 * Google maneuver values include: turn-left, turn-right, turn-slight-left,
 * turn-slight-right, turn-sharp-left, turn-sharp-right, uturn-left, uturn-right,
 * ramp-left, ramp-right, merge, fork-left, fork-right, roundabout-left,
 * roundabout-right, etc.
 */
class GoogleManeuverMapper {

    /**
     * Maps a Google maneuver string to a Maneuver domain object.
     * If maneuver is null (e.g., first or last step), returns CONTINUE or appropriate default.
     */
    fun map(googleManeuver: String?): Maneuver {
        val type = when (googleManeuver?.lowercase()) {
            "turn-left" -> ManeuverType.TURN_LEFT
            "turn-right" -> ManeuverType.TURN_RIGHT
            "turn-slight-left" -> ManeuverType.SLIGHT_LEFT
            "turn-slight-right" -> ManeuverType.SLIGHT_RIGHT
            "turn-sharp-left" -> ManeuverType.SHARP_LEFT
            "turn-sharp-right" -> ManeuverType.SHARP_RIGHT
            "uturn-left", "uturn-right" -> ManeuverType.U_TURN
            "ramp-left", "ramp-right", "merge" -> ManeuverType.ENTER_HIGHWAY
            "fork-left", "fork-right" -> ManeuverType.CONTINUE
            "roundabout-left", "roundabout-right" -> ManeuverType.ROUNDABOUT_ENTER
            "straight" -> ManeuverType.CONTINUE
            null -> ManeuverType.CONTINUE // No explicit maneuver = continue straight
            else -> ManeuverType.UNKNOWN
        }
        
        return Maneuver(
            type = type,
            streetName = null, // Google puts street info in html_instructions, not maneuver
            exitNumber = null
        )
    }
}
