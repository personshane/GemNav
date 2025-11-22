package com.gemnav.android.app.main_flow.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
    
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSearches(limit: Int = 20): Flow<List<SearchHistoryEntity>>
    
    @Query("SELECT DISTINCT query FROM search_history WHERE query LIKE :prefix || '%' ORDER BY timestamp DESC LIMIT :limit")
    suspend fun searchByPrefix(prefix: String, limit: Int = 5): List<String>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(searchHistory: SearchHistoryEntity): Long
    
    @Query("DELETE FROM search_history WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long)
    
    @Query("DELETE FROM search_history")
    suspend fun clearAll()
}
