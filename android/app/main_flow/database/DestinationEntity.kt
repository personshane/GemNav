package com.gemnav.android.app.main_flow.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.gemnav.android.app.main_flow.models.Destination

@Entity(tableName = "destinations")
data class DestinationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val placeId: String? = null,
    val isFavorite: Boolean = false,
    val isHome: Boolean = false,
    val isWork: Boolean = false,
    val lastUsedTimestamp: Long = System.currentTimeMillis(),
    val useCount: Int = 1,
    val category: String? = null
)

fun DestinationEntity.toDomain(): Destination {
    return Destination(
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
