package com.gemnav.android.app.main_flow.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val query: String,
    val timestamp: Long = System.currentTimeMillis(),
    val resultCount: Int = 0,
    val selectedPlaceId: String? = null
)
