package com.gemnav.routing.google

import com.gemnav.routing.domain.LatLng

/**
 * Decodes a Google-encoded polyline string into a list of LatLng points.
 *
 * This implementation follows the standard Google polyline algorithm.
 */
object GooglePolylineDecoder {

    fun decode(encodedPath: String): List<LatLng> {
        val len = encodedPath.length
        var index = 0
        var lat = 0
        var lng = 0
        val path = ArrayList<LatLng>()

        while (index < len) {
            var result = 0
            var shift = 0
            var b: Int
            do {
                b = encodedPath[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
            lat += dlat

            result = 0
            shift = 0
            do {
                b = encodedPath[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
            lng += dlng

            val latitude = lat / 1E5
            val longitude = lng / 1E5
            path.add(LatLng(latitude, longitude))
        }

        return path
    }
}
