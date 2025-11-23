package com.gemnav.app.models

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
}
