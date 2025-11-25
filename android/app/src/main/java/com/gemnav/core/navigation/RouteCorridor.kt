package com.gemnav.core.navigation

import com.gemnav.data.route.LatLng
import com.gemnav.core.places.PlaceResult
import kotlin.math.*

/**
 * MP-022: Route corridor utilities for along-route POI filtering.
 * 
 * PLUS TIER ONLY - Never called by Free or Pro tiers.
 * - Free: Cannot use Places API or route polylines
 * - Pro: Uses HERE SDK (Google data forbidden)
 */
object RouteCorridor {
    
    private const val TAG = "RouteCorridor"
    private const val EARTH_RADIUS_METERS = 6_371_000.0
    
    /**
     * Default corridor width for along-route searches (2km from route).
     */
    const val DEFAULT_CORRIDOR_METERS = 2000.0
    
    /**
     * Check if a POI lies within the route corridor.
     * 
     * @param poiLatLng The POI location
     * @param routePolyline Decoded polyline points from Google Directions
     * @param toleranceMeters Max distance from route (default 2km)
     * @return true if POI is within corridor
     */
    fun isPointAlongRoute(
        poiLatLng: LatLng,
        routePolyline: List<LatLng>,
        toleranceMeters: Double = DEFAULT_CORRIDOR_METERS
    ): Boolean {
        if (routePolyline.size < 2) return false
        
        // Check distance to each segment
        for (i in 0 until routePolyline.size - 1) {
            val segmentStart = routePolyline[i]
            val segmentEnd = routePolyline[i + 1]
            
            val distance = pointToSegmentDistance(poiLatLng, segmentStart, segmentEnd)
            if (distance <= toleranceMeters) {
                return true
            }
        }
        return false
    }
    
    /**
     * Filter places to only those within the route corridor.
     * Results are sorted by approximate distance along route (by segment index).
     * 
     * @param places List of PlaceResult from Places API
     * @param routePolyline Decoded polyline points
     * @param toleranceMeters Max distance from route
     * @return Filtered and sorted list of places along route
     */
    fun filterPlacesAlongRoute(
        places: List<PlaceResult>,
        routePolyline: List<LatLng>,
        toleranceMeters: Double = DEFAULT_CORRIDOR_METERS
    ): List<PlaceResult> {
        if (routePolyline.size < 2) return emptyList()
        
        // Map each place to (place, nearestSegmentIndex, distanceFromRoute)
        val placesWithPosition = places.mapNotNull { place ->
            val poiLatLng = LatLng(place.lat, place.lng)
            val (segmentIndex, distance) = findNearestSegment(poiLatLng, routePolyline)
            
            if (distance <= toleranceMeters) {
                Triple(place, segmentIndex, distance)
            } else {
                null
            }
        }
        
        // Sort by segment index (approximate route progress)
        return placesWithPosition
            .sortedBy { it.second }
            .map { it.first }
    }
    
    /**
     * Find the nearest segment and distance to a point.
     * 
     * @return Pair of (segmentIndex, distanceMeters)
     */
    private fun findNearestSegment(
        point: LatLng,
        polyline: List<LatLng>
    ): Pair<Int, Double> {
        var minDistance = Double.MAX_VALUE
        var nearestSegment = 0
        
        for (i in 0 until polyline.size - 1) {
            val distance = pointToSegmentDistance(point, polyline[i], polyline[i + 1])
            if (distance < minDistance) {
                minDistance = distance
                nearestSegment = i
            }
        }
        
        return Pair(nearestSegment, minDistance)
    }
    
    /**
     * Calculate minimum distance from a point to a line segment (Haversine-based).
     * 
     * Uses vector projection to find closest point on segment,
     * then calculates Haversine distance.
     */
    private fun pointToSegmentDistance(
        point: LatLng,
        segmentStart: LatLng,
        segmentEnd: LatLng
    ): Double {
        // Convert to radians
        val pLat = Math.toRadians(point.latitude)
        val pLon = Math.toRadians(point.longitude)
        val aLat = Math.toRadians(segmentStart.latitude)
        val aLon = Math.toRadians(segmentStart.longitude)
        val bLat = Math.toRadians(segmentEnd.latitude)
        val bLon = Math.toRadians(segmentEnd.longitude)
        
        // Vector from A to B
        val abLat = bLat - aLat
        val abLon = bLon - aLon
        
        // Vector from A to P
        val apLat = pLat - aLat
        val apLon = pLon - aLon
        
        // Project P onto line AB
        val ab2 = abLat * abLat + abLon * abLon
        if (ab2 == 0.0) {
            // Segment is a point
            return haversineDistance(point, segmentStart)
        }
        
        val t = maxOf(0.0, minOf(1.0, (apLat * abLat + apLon * abLon) / ab2))
        
        // Closest point on segment
        val closestLat = aLat + t * abLat
        val closestLon = aLon + t * abLon
        val closestPoint = LatLng(Math.toDegrees(closestLat), Math.toDegrees(closestLon))
        
        return haversineDistance(point, closestPoint)
    }
    
    /**
     * Haversine distance between two points in meters.
     */
    private fun haversineDistance(p1: LatLng, p2: LatLng): Double {
        val lat1 = Math.toRadians(p1.latitude)
        val lat2 = Math.toRadians(p2.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(p2.longitude - p1.longitude)
        
        val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return EARTH_RADIUS_METERS * c
    }
    
    /**
     * Check if user query contains along-route keywords.
     * 
     * Keywords: "along my route", "on my way", "on the way", "next", "upcoming"
     */
    fun containsAlongRouteKeywords(query: String): Boolean {
        val lowerQuery = query.lowercase()
        val keywords = listOf(
            "along my route",
            "along the route",
            "on my way",
            "on the way",
            "next ",       // "next gas station"
            "upcoming",
            "ahead",
            "coming up",
            "before i arrive",
            "before i get there"
        )
        return keywords.any { lowerQuery.contains(it) }
    }
}
