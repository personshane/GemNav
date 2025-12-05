package com.gemnav.app.truck.domain

import com.gemnav.app.truck.model.TruckProfile

/**
 * Converts a TruckProfile into TruckRoutingConstraints for use in the routing layer.
 */
object TruckRoutingConstraintBuilder {

    fun fromProfile(profile: TruckProfile): TruckRoutingConstraints {
        return TruckRoutingConstraints(
            heightMeters = profile.heightMeters,
            widthMeters = profile.widthMeters,
            lengthMeters = profile.lengthMeters,
            weightTons = profile.weightTons,
            axleCount = profile.axleCount,
            hazmatClass = profile.hazmatClass,
            truckType = profile.truckType,
            avoidTolls = profile.avoidTolls,
            avoidFerries = profile.avoidFerries,
            avoidLowBridges = profile.avoidLowBridges
        )
    }
}
