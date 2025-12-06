package com.gemnav.routing.here

import android.content.Context
import com.gemnav.routing.domain.RouteRequest
import com.gemnav.truck.domain.TruckRoutingConstraints

/**
 * Builds HERE SDK routing options from our RouteRequest and TruckRoutingConstraints.
 *
 * You must replace the placeholder types with the actual HERE SDK routing option types.
 */
class HereRouteOptionsBuilder {

    fun buildOptions(
        context: Context,
        request: RouteRequest
        // ): HereRouteOptionsType
    ): Any {
        val constraints: TruckRoutingConstraints? = request.truckConstraints

        // TODO: Replace Any with the actual HERE route options type and map fields properly.
        // Pseudo-mapping:
        // val options = HereRouteOptions()
        // options.truckHeight = constraints?.heightMeters
        // options.truckWeight = constraints?.weightTons
        // options.avoidTolls = request.avoidTolls
        // options.avoidFerries = request.avoidFerries
        // ...

        return Any()
    }
}
