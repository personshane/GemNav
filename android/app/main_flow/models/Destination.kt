package com.gemnav.android.app.main_flow.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Destination(
    val id: Long = 0,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val placeId: String? = null,
    val isFavorite: Boolean = false,
    val isHome: Boolean = false,
    val isWork: Boolean = false
) : Parcelable {
    
    fun toLatLngString(): String = "$latitude,$longitude"
    
    fun toEntity(): com.gemnav.android.app.main_flow.database.DestinationEntity {
        return com.gemnav.android.app.main_flow.database.DestinationEntity(
            id = id,
            name = name,
            address = address,
            latitude = latitude,
            longitude = longitude,
            placeId = placeId,
            isFavorite = isFavorite,
            isHome = isHome,
            isWork = isWork
        )
    }
}
