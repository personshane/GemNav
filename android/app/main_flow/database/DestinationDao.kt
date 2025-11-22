package com.gemnav.android.app.main_flow.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DestinationDao {
    
    @Query("SELECT * FROM destinations ORDER BY lastUsedTimestamp DESC LIMIT :limit")
    fun getRecentDestinations(limit: Int = 10): Flow<List<DestinationEntity>>
    
    @Query("SELECT * FROM destinations WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavorites(): Flow<List<DestinationEntity>>
    
    @Query("SELECT * FROM destinations WHERE isHome = 1 LIMIT 1")
    suspend fun getHome(): DestinationEntity?
    
    @Query("SELECT * FROM destinations WHERE isWork = 1 LIMIT 1")
    suspend fun getWork(): DestinationEntity?
    
    @Query("SELECT * FROM destinations WHERE id = :id")
    suspend fun getById(id: Long): DestinationEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(destination: DestinationEntity): Long
    
    @Update
    suspend fun update(destination: DestinationEntity)
    
    @Delete
    suspend fun delete(destination: DestinationEntity)
    
    @Query("DELETE FROM destinations WHERE id = :id")
    suspend fun deleteById(id: Long)
