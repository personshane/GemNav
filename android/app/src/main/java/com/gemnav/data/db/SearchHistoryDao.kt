package com.gemnav.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gemnav.data.db.entities.SearchHistoryEntity

@Dao
interface SearchHistoryDao {

    @Insert
    suspend fun insertQuery(entry: SearchHistoryEntity): Long

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentSearches(limit: Int = 20): List<SearchHistoryEntity>
}
