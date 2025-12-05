package com.gemnav.utils

import android.location.Location
import kotlin.math.floor

object PolylineEncoder {

    fun encodePath(points: List<Location>): String {
        if (points.isEmpty()) return ""

        val encoded = StringBuilder()
        var lastLat = 0
        var lastLng = 0

        for (loc in points) {
            val lat = floor(loc.latitude * 1e5).toInt()
            val lng = floor(loc.longitude * 1e5).toInt()

            val dLat = lat - lastLat
            val dLng = lng - lastLng

            encodeValue(dLat, encoded)
            encodeValue(dLng, encoded)

            lastLat = lat
            lastLng = lng
        }

        return encoded.toString()
    }

    private fun encodeValue(v: Int, result: StringBuilder) {
        var value = v shl 1
        if (v < 0) {
            value = value.inv()
        }

        while (value >= 0x20) {
            val next = (0x20 or (value and 0x1F)) + 63
            result.append(next.toChar())
            value = value shr 5
        }

        result.append((value + 63).toChar())
    }
}
