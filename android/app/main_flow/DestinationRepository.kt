package com.gemnav.android.app.main_flow

import com.gemnav.android.app.main_flow.database.DestinationDao
import com.gemnav.android.app.main_flow.database.toDomain
import com.gemnav.android.app.main_flow.models.Destination
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DestinationRepository @Inject constructor(
    private val destinationDao: DestinationDao
) {
    
    fun getRecentDestinations(limit: Int = 10): Flow<List<Destination>> {
        return destinationDao.getRecentDestinations(limit)
            .map { entities -> entities.map { it.toDomain() } }
    }
    
    fun getFavorites(): Flow<List<Destination>> {
        return destinationDao.getFavorites()
            .map { entities -> entities.map { it.toDomain() } }
    }
    
    suspend fun getHome(): Destination? {
        return destinationDao.getHome()?.toDomain()
    }
    
    suspend fun getWork(): Destination? {
        return destinationDao.getWork()?.toDomain()
    }
    
    suspend fun saveDestination(destination: Destination): Long {
        return destinationDao.insert(destination.toEntity())
    }
    
    suspend fun updateDestination(destination: Destination) {
        destinationDao.update(destination.toEntity())
    }
    
    suspend fun deleteDestination(destination: Destination) {
        destinationDao.delete(destination.toEntity())
    }
    
    suspend fun toggleFavorite(destination: Destination) {
        val updated = destination.copy(isFavorite = !destination.isFavorite)
        destinationDao.update(updated.toEntity())
    }
    
    suspend fun setAsHome(destination: Destination) {
        val currentHome = destinationDao.getHome()
        currentHome?.let {
            destinationDao.update(it.copy(isHome = false))
        }
        val updated = destination.copy(isHome = true, isFavorite = true)
        destinationDao.update(updated.toEntity())
    }
    
    suspend fun setAsWork(destination: Destination) {
        val currentWork = destinationDao.getWork()
        currentWork?.let {
            destinationDao.update(it.copy(isWork = false))
        }
        val updated = destination.copy(isWork = true, isFavorite = true)
        destinationDao.update(updated.toEntity())
    }
}
