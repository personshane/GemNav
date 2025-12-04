package com.gemnav.data.navigation

import com.gemnav.data.route.LatLng

/**
 * MP-025: Data models for HERE truck-specific POIs.
 * 
 * PRO TIER ONLY - Used for truck stops, weigh stations, rest areas, and parking.
 */

/**
 * HERE category IDs for truck-specific POIs.
 * Reference: https://developer.here.com/documentation/geocoding-search-api/dev_guide/topics/places-category-system.html
 */
enum class TruckPoiType(val hereCategoryId: String, val displayName: String) {
    TRUCK_STOP("700-7600-0322", "Truck Stop"),
    WEIGH_STATION("700-7600-0116", "Weigh Station"),
    REST_AREA("800-8500-0000", "Rest Area"),
    PARKING("800-8500-0177", "Truck Parking")
}

/**
 * A truck-specific POI from HERE Places API.
 */
data class TruckPoi(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val category: TruckPoiType,
    val address: String? = null,
    val distanceMeters: Int? = null,
    val phone: String? = null
) {
    val latLng: LatLng get() = LatLng(lat, lng)
}

/**
 * Result container for truck POI search.
 */
data class TruckPoiResult(
    val type: TruckPoiType,
    val pois: List<TruckPoi>,
    val totalFound: Int = pois.size
)

/**
 * State for truck POI search operations.
 */
sealed class TruckPoiState {
    object Idle : TruckPoiState()
    data class Searching(val type: TruckPoiType) : TruckPoiState()
    data class Found(val result: TruckPoiResult) : TruckPoiState()
    data class NotFound(val type: TruckPoiType) : TruckPoiState()
    data class Error(val message: String) : TruckPoiState()
    data class Blocked(val reason: String) : TruckPoiState()
}
