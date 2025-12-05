package com.gemnav.utils

import com.google.android.gms.maps.model.LatLng

object PolylineDecoder {

    fun decode(encoded: String): List<LatLng> {
        if (encoded.isEmpty()) return emptyList()

        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0

            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1F) shl shift)
                shift += 5
            } while (b >= 0x20)

            val dLat = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
            lat += dLat

            shift = 0
            result = 0

            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1F) shl shift)
                shift += 5
            } while (b >= 0x20)

            val dLng = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
            lng += dLng

            val finalLat = lat / 1E5
            val finalLng = lng / 1E5
            poly.add(LatLng(finalLat, finalLng))
        }

        return poly
    }
}
