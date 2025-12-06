package com.gemnav.routing.here

import com.gemnav.routing.domain.LatLng

/**
 * Decodes HERE SDK polyline/geometry into a list of LatLng domain points.
 *
 * You must adapt this to your actual HERE route geometry representation.
 */
object HerePolylineDecoder {

    fun decode(hereGeometry: Any): List<LatLng> {
        // TODO: Replace 'Any' with actual HERE geometry type and decode into LatLng list.
        return emptyList()
    }
}
