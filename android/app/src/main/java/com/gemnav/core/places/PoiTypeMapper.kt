package com.gemnav.core.places

import com.gemnav.data.ai.POIType

/**
 * PoiTypeMapper - Maps GemNav AI POI types to Google Places API types.
 * MP-021: Plus tier Google Places integration.
 * 
 * Google Places uses specific type strings. Some POIs require
 * keyword searches instead of or in addition to type filters.
 */
object PoiTypeMapper {
    
    /**
     * Map GemNav POIType to Google Places type string.
     * Returns empty string if no direct type mapping exists.
     */
    fun mapPoiTypeToPlacesType(poiType: POIType): String {
        return when (poiType) {
            POIType.TRUCK_STOP -> "gas_station"
            POIType.GAS_STATION -> "gas_station"
            POIType.DIESEL -> "gas_station"
            POIType.REST_AREA -> "parking"
            POIType.HOTEL -> "lodging"
            POIType.MOTEL -> "lodging"
            POIType.RESTAURANT -> "restaurant"
            POIType.FAST_FOOD -> "restaurant"
            POIType.PARKING -> "parking"
            POIType.TRUCK_PARKING -> "parking"
            POIType.WALMART -> "" // Use keyword instead
            POIType.GROCERY -> "supermarket"
            POIType.WEIGH_STATION -> "" // Use keyword instead
            POIType.REPAIR_SHOP -> "car_repair"
            POIType.CAR_WASH -> "car_wash"
            POIType.HOSPITAL -> "hospital"
            POIType.PHARMACY -> "pharmacy"
            POIType.ATM -> "atm"
            POIType.OTHER -> ""
        }
    }
    
    /**
     * Get keyword for POI search.
     * Used when Places type alone isn't specific enough.
     */
    fun mapPoiKeyword(poiType: POIType): String? {
        return when (poiType) {
            POIType.TRUCK_STOP -> "truck stop"
            POIType.DIESEL -> "diesel fuel"
            POIType.REST_AREA -> "rest area"
            POIType.MOTEL -> "motel"
            POIType.FAST_FOOD -> "fast food drive thru"
            POIType.TRUCK_PARKING -> "truck parking overnight"
            POIType.WALMART -> "Walmart"
            POIType.WEIGH_STATION -> "weigh station"
            POIType.REPAIR_SHOP -> "truck repair"
            else -> null
        }
    }
    
    /**
     * Get human-readable description for POI type.
     */
    fun getPoiDescription(poiType: POIType): String {
        return when (poiType) {
            POIType.TRUCK_STOP -> "truck stop"
            POIType.GAS_STATION -> "gas station"
            POIType.DIESEL -> "diesel station"
            POIType.REST_AREA -> "rest area"
            POIType.HOTEL -> "hotel"
            POIType.MOTEL -> "motel"
            POIType.RESTAURANT -> "restaurant"
            POIType.FAST_FOOD -> "fast food"
            POIType.PARKING -> "parking"
            POIType.TRUCK_PARKING -> "truck parking"
            POIType.WALMART -> "Walmart"
            POIType.GROCERY -> "grocery store"
            POIType.WEIGH_STATION -> "weigh station"
            POIType.REPAIR_SHOP -> "repair shop"
            POIType.CAR_WASH -> "car wash"
            POIType.HOSPITAL -> "hospital"
            POIType.PHARMACY -> "pharmacy"
            POIType.ATM -> "ATM"
            POIType.OTHER -> "place"
        }
    }
    
    /**
     * Get filter description from POI filters.
     */
    fun getFilterDescription(
        hasShowers: Boolean? = null,
        hasTruckParking: Boolean? = null,
        hasDiesel: Boolean? = null,
        hasOvernightParking: Boolean? = null
    ): String {
        val parts = mutableListOf<String>()
        if (hasShowers == true) parts.add("with showers")
        if (hasTruckParking == true) parts.add("with truck parking")
        if (hasDiesel == true) parts.add("with diesel")
        if (hasOvernightParking == true) parts.add("with overnight parking")
        return parts.joinToString(", ")
    }
}
