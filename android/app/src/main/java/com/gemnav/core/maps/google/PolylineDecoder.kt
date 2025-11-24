package com.gemnav.core.maps.google

import com.gemnav.data.route.LatLng

/**
 * Google Polyline Decoder.
 * MP-019: Decodes encoded polyline strings from Directions API.
 * 
 * Implementation of Google's Encoded Polyline Algorithm:
 * https://developers.google.com/maps/documentation/utilities/polylinealgorithm
 */
object PolylineDecoder {
    
    /**
     * Decode an encoded polyline string into a list of LatLng coordinates.
     * 
     * @param encoded The encoded polyline string from Google Directions API
     * @return List of LatLng coordinates representing the polyline
     */
    fun decode(encoded: String): List<LatLng> {
        val coordinates = mutableListOf<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        
        while (index < len) {
            // Decode latitude
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
            lat += dlat
            
            // Decode longitude
            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
            lng += dlng
            
            // Convert to decimal degrees (precision: 5 decimal places)
            coordinates.add(LatLng(lat / 1e5, lng / 1e5))
        }
        
        return coordinates
    }
    
    /**
     * Encode a list of LatLng coordinates into an encoded polyline string.
     * Useful for debugging and testing.
     * 
     * @param coordinates List of LatLng coordinates
     * @return Encoded polyline string
     */
    fun encode(coordinates: List<LatLng>): String {
        val result = StringBuilder()
        var prevLat = 0
        var prevLng = 0
        
        for (coord in coordinates) {
            val lat = (coord.latitude * 1e5).toInt()
            val lng = (coord.longitude * 1e5).toInt()
            
            result.append(encodeValue(lat - prevLat))
            result.append(encodeValue(lng - prevLng))
            
            prevLat = lat
            prevLng = lng
        }
        
        return result.toString()
    }
    
    /**
     * Encode a single value for polyline encoding.
     */
    private fun encodeValue(value: Int): String {
        var v = if (value < 0) (value shl 1).inv() else (value shl 1)
        val result = StringBuilder()
        
        while (v >= 0x20) {
            result.append(((0x20 or (v and 0x1f)) + 63).toChar())
            v = v shr 5
        }
        result.append((v + 63).toChar())
        
        return result.toString()
    }
    
    /**
     * Simplify a polyline by removing points that don't significantly 
     * change the path (Douglas-Peucker algorithm).
     * 
     * @param coordinates Original polyline
     * @param tolerance Maximum deviation in meters
     * @return Simplified polyline
     */
    fun simplify(coordinates: List<LatLng>, tolerance: Double = 10.0): List<LatLng> {
        if (coordinates.size < 3) return coordinates
        
        // Find the point with maximum distance from the line
        var maxDistance = 0.0
        var maxIndex = 0
        val first = coordinates.first()
        val last = coordinates.last()
        
        for (i in 1 until coordinates.size - 1) {
            val distance = perpendicularDistance(coordinates[i], first, last)
            if (distance > maxDistance) {
                maxDistance = distance
                maxIndex = i
            }
        }
        
        // If max distance is greater than tolerance, recursively simplify
        return if (maxDistance > tolerance) {
            val left = simplify(coordinates.subList(0, maxIndex + 1), tolerance)
            val right = simplify(coordinates.subList(maxIndex, coordinates.size), tolerance)
            left.dropLast(1) + right
        } else {
            listOf(first, last)
        }
    }
    
    /**
     * Calculate perpendicular distance from point to line segment.
     * Uses simplified approximation for small distances.
     */
    private fun perpendicularDistance(point: LatLng, lineStart: LatLng, lineEnd: LatLng): Double {
        val dx = lineEnd.longitude - lineStart.longitude
        val dy = lineEnd.latitude - lineStart.latitude
        
        if (dx == 0.0 && dy == 0.0) {
            return haversineDistance(point, lineStart)
        }
        
        val t = ((point.longitude - lineStart.longitude) * dx + 
                 (point.latitude - lineStart.latitude) * dy) / (dx * dx + dy * dy)
        val tClamped = t.coerceIn(0.0, 1.0)
        
        val nearestLat = lineStart.latitude + tClamped * dy
        val nearestLng = lineStart.longitude + tClamped * dx
        
        return haversineDistance(point, LatLng(nearestLat, nearestLng))
    }
    
    /**
     * Calculate Haversine distance between two points in meters.
     */
    private fun haversineDistance(p1: LatLng, p2: LatLng): Double {
        val earthRadius = 6371000.0 // meters
        val lat1 = Math.toRadians(p1.latitude)
        val lat2 = Math.toRadians(p2.latitude)
        val dLat = Math.toRadians(p2.latitude - p1.latitude)
        val dLng = Math.toRadians(p2.longitude - p1.longitude)
        
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(lat1) * kotlin.math.cos(lat2) *
                kotlin.math.sin(dLng / 2) * kotlin.math.sin(dLng / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        
        return earthRadius * c
    }
}
