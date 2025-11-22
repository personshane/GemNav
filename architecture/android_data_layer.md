# Android Data Layer Architecture

**Version**: 1.0  
**Platform**: Android  
**Patterns**: Repository, Room, Retrofit

---

## Data Layer Overview

```
Domain Layer (Interfaces)
         ↓
Repository Implementations
         ↓
    ┌────────┴────────┐
Local Source      Remote Source
(Room DB)         (Retrofit API)
```

---

## Domain Models

### Location.kt

```kotlin
package com.gemnav.app.domain.model

data class Location(
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
```

### Route.kt

```kotlin
package com.gemnav.app.domain.model

data class Route(
    val id: String,
    val origin: Location,
    val destination: Location,
    val distance: Int,  // meters
    val duration: Int,  // seconds
    val polyline: String,
    val steps: List<RouteStep>,
    val warnings: List<String> = emptyList(),
    val tollCost: Double? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class RouteStep(
    val instruction: String,
    val distance: Int,
    val duration: Int,
    val polyline: String,
    val maneuver: String
)
```

### ChatMessage.kt

```kotlin
package com.gemnav.app.domain.model

sealed class ChatMessage {
    abstract val text: String
    abstract val timestamp: Long
    
    data class User(
        override val text: String,
        override val timestamp: Long
    ) : ChatMessage()
    
    data class Assistant(
        override val text: String,
        override val timestamp: Long,
        val action: NavigationAction? = null
    ) : ChatMessage()
    
    data class System(
        override val text: String,
        override val timestamp: Long
    ) : ChatMessage()
}

sealed class NavigationAction {
    data class Navigate(val destination: Location) : NavigationAction()
    data class AddWaypoint(val location: Location) : NavigationAction()
    object CancelNavigation : NavigationAction()
}
```

### NavigationState.kt

```kotlin
package com.gemnav.app.domain.model

sealed class NavigationState {
    object Idle : NavigationState()
    data class RouteCalculating(val destination: Location) : NavigationState()
    data class RouteReady(val route: Route) : NavigationState()
    data class Navigating(
        val route: Route,
        val currentLocation: Location,
        val nextStep: RouteStep?,
        val distanceRemaining: Int,
        val timeRemaining: Int
    ) : NavigationState()
    object Arrived : NavigationState()
}
```

---

## Repository Interfaces

### NavigationRepository.kt

```kotlin
package com.gemnav.app.domain.repository

import com.gemnav.app.domain.model.Location
import com.gemnav.app.domain.model.NavigationState
import com.gemnav.app.domain.model.Route
import kotlinx.coroutines.flow.Flow

interface NavigationRepository {
    
    suspend fun calculateRoute(
        origin: Location,
        destination: Location,
        waypoints: List<Location> = emptyList()
    ): Result<Route>
    
    suspend fun saveRoute(route: Route): Result<Unit>
    
    suspend fun getRecentRoutes(limit: Int = 10): Result<List<Route>>
    
    suspend fun deleteRoute(routeId: String): Result<Unit>
    
    fun observeNavigationState(): Flow<NavigationState>
    
    suspend fun startNavigation(route: Route): Result<Unit>
    
    suspend fun stopNavigation(): Result<Unit>
    
    suspend fun updateCurrentLocation(location: Location): Result<Unit>
}
```

### ChatRepository.kt

```kotlin
package com.gemnav.app.domain.repository

import com.gemnav.app.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    
    suspend fun sendMessage(text: String): Result<String>
    
    suspend fun saveMessage(message: ChatMessage): Result<Unit>
    
    fun observeMessages(): Flow<List<ChatMessage>>
    
    suspend fun getMessageHistory(limit: Int = 50): Result<List<ChatMessage>>
    
    suspend fun clearHistory(): Result<Unit>
}
```

### UserRepository.kt

```kotlin
package com.gemnav.app.domain.repository

import com.gemnav.app.domain.model.User
import com.gemnav.app.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    
    suspend fun getCurrentUser(): Result<User?>
    
    suspend fun saveUser(user: User): Result<Unit>
    
    fun observeUserPreferences(): Flow<UserPreferences>
    
    suspend fun updatePreferences(preferences: UserPreferences): Result<Unit>
    
    suspend fun logout(): Result<Unit>
}
```

---

## Room Database

### GemNavDatabase.kt

```kotlin
package com.gemnav.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.gemnav.app.data.local.database.converter.Converters
import com.gemnav.app.data.local.database.dao.LocationDao
import com.gemnav.app.data.local.database.dao.MessageDao
import com.gemnav.app.data.local.database.dao.RouteDao
import com.gemnav.app.data.local.database.entity.LocationEntity
import com.gemnav.app.data.local.database.entity.MessageEntity
import com.gemnav.app.data.local.database.entity.RouteEntity

@Database(
    entities = [
        RouteEntity::class,
        MessageEntity::class,
        LocationEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class GemNavDatabase : RoomDatabase() {
    abstract fun routeDao(): RouteDao
    abstract fun messageDao(): MessageDao
    abstract fun locationDao(): LocationDao
}
```

### Converters.kt

```kotlin
package com.gemnav.app.data.local.database.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    
    private val gson = Gson()
    
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type)
    }
    
    @TypeConverter
    fun fromRouteStepList(value: List<RouteStepJson>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toRouteStepList(value: String): List<RouteStepJson> {
        val type = object : TypeToken<List<RouteStepJson>>() {}.type
        return gson.fromJson(value, type)
    }
}

data class RouteStepJson(
    val instruction: String,
    val distance: Int,
    val duration: Int,
    val polyline: String,
    val maneuver: String
)
```

---

## Database Entities

### RouteEntity.kt

```kotlin
package com.gemnav.app.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey
    val id: String,
    val originLat: Double,
    val originLng: Double,
    val originAddress: String?,
    val destLat: Double,
    val destLng: Double,
    val destAddress: String?,
    val distance: Int,
    val duration: Int,
    val polyline: String,
    val steps: String,  // JSON
    val warnings: String,  // JSON
    val tollCost: Double?,
    val timestamp: Long
)
```

### MessageEntity.kt

```kotlin
package com.gemnav.app.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long,
    val actionType: String?,  // null, "navigate", "waypoint", "cancel"
    val actionData: String?   // JSON for navigation action
)
```

### LocationEntity.kt

```kotlin
package com.gemnav.app.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val address: String?,
    val timestamp: Long,
    val isFavorite: Boolean = false,
    val name: String?
)
```

---

## DAOs

### RouteDao.kt

```kotlin
package com.gemnav.app.data.local.database.dao

import androidx.room.*
import com.gemnav.app.data.local.database.entity.RouteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {
    
    @Query("SELECT * FROM routes ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentRoutes(limit: Int): List<RouteEntity>
    
    @Query("SELECT * FROM routes ORDER BY timestamp DESC")
    fun observeRoutes(): Flow<List<RouteEntity>>
    
    @Query("SELECT * FROM routes WHERE id = :routeId")
    suspend fun getRouteById(routeId: String): RouteEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: RouteEntity)
    
    @Delete
    suspend fun deleteRoute(route: RouteEntity)
    
    @Query("DELETE FROM routes WHERE id = :routeId")
    suspend fun deleteRouteById(routeId: String)
    
    @Query("DELETE FROM routes")
    suspend fun clearAllRoutes()
}
```

### MessageDao.kt

```kotlin
package com.gemnav.app.data.local.database.dao

import androidx.room.*
import com.gemnav.app.data.local.database.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    
    @Query("SELECT * FROM messages ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getRecentMessages(limit: Int): List<MessageEntity>
    
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun observeMessages(): Flow<List<MessageEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)
    
    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()
    
    @Query("DELETE FROM messages WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOldMessages(beforeTimestamp: Long)
}
```

### LocationDao.kt

```kotlin
package com.gemnav.app.data.local.database.dao

import androidx.room.*
import com.gemnav.app.data.local.database.entity.LocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    
    @Query("SELECT * FROM locations WHERE isFavorite = 1 ORDER BY name ASC")
    fun observeFavoriteLocations(): Flow<List<LocationEntity>>
    
    @Query("SELECT * FROM locations ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentLocations(limit: Int): List<LocationEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity)
    
    @Update
    suspend fun updateLocation(location: LocationEntity)
    
    @Delete
    suspend fun deleteLocation(location: LocationEntity)
    
    @Query("DELETE FROM locations WHERE isFavorite = 0 AND timestamp < :beforeTimestamp")
    suspend fun deleteOldNonFavorites(beforeTimestamp: Long)
}
```

---

## Repository Implementations

### NavigationRepositoryImpl.kt

```kotlin
package com.gemnav.app.data.repository

import com.gemnav.app.data.local.database.dao.RouteDao
import com.gemnav.app.data.mapper.RouteMapper
import com.gemnav.app.data.remote.api.NavigationApi
import com.gemnav.app.domain.model.Location
import com.gemnav.app.domain.model.NavigationState
import com.gemnav.app.domain.model.Route
import com.gemnav.app.domain.repository.NavigationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class NavigationRepositoryImpl @Inject constructor(
    private val navigationApi: NavigationApi,
    private val routeDao: RouteDao,
    private val routeMapper: RouteMapper
) : NavigationRepository {
    
    private val _navigationState = MutableStateFlow<NavigationState>(NavigationState.Idle)
    
    override suspend fun calculateRoute(
        origin: Location,
        destination: Location,
        waypoints: List<Location>
    ): Result<Route> {
        return try {
            val response = navigationApi.calculateRoute(
                originLat = origin.latitude,
                originLng = origin.longitude,
                destLat = destination.latitude,
                destLng = destination.longitude,
                waypoints = waypoints.map { "${it.latitude},${it.longitude}" }
            )
            
            val route = routeMapper.mapFromDto(response)
            Result.success(route)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun saveRoute(route: Route): Result<Unit> {
        return try {
            val entity = routeMapper.mapToEntity(route)
            routeDao.insertRoute(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getRecentRoutes(limit: Int): Result<List<Route>> {
        return try {
            val entities = routeDao.getRecentRoutes(limit)
            val routes = entities.map { routeMapper.mapFromEntity(it) }
            Result.success(routes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteRoute(routeId: String): Result<Unit> {
        return try {
            routeDao.deleteRouteById(routeId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun observeNavigationState(): Flow<NavigationState> {
        return _navigationState.asStateFlow()
    }
    
    override suspend fun startNavigation(route: Route): Result<Unit> {
        return try {
            _navigationState.value = NavigationState.RouteReady(route)
            // Start location tracking service
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun stopNavigation(): Result<Unit> {
        return try {
            _navigationState.value = NavigationState.Idle
            // Stop location tracking service
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateCurrentLocation(location: Location): Result<Unit> {
        return try {
            val currentState = _navigationState.value
            if (currentState is NavigationState.Navigating) {
                // Update navigation state with new location
                // Calculate remaining distance/time
                // Check if arrived
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### ChatRepositoryImpl.kt

```kotlin
package com.gemnav.app.data.repository

import com.gemnav.app.data.local.database.dao.MessageDao
import com.gemnav.app.data.local.database.entity.MessageEntity
import com.gemnav.app.data.mapper.MessageMapper
import com.gemnav.app.data.remote.api.GeminiApi
import com.gemnav.app.domain.model.ChatMessage
import com.gemnav.app.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val geminiApi: GeminiApi,
    private val messageDao: MessageDao,
    private val messageMapper: MessageMapper
) : ChatRepository {
    
    override suspend fun sendMessage(text: String): Result<String> {
        return try {
            val response = geminiApi.sendMessage(text)
            val reply = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "Sorry, I couldn't generate a response."
            
            Result.success(reply)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun saveMessage(message: ChatMessage): Result<Unit> {
        return try {
            val entity = messageMapper.mapToEntity(message)
            messageDao.insertMessage(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun observeMessages(): Flow<List<ChatMessage>> {
        return messageDao.observeMessages().map { entities ->
            entities.map { messageMapper.mapFromEntity(it) }
        }
    }
    
    override suspend fun getMessageHistory(limit: Int): Result<List<ChatMessage>> {
        return try {
            val entities = messageDao.getRecentMessages(limit)
            val messages = entities.map { messageMapper.mapFromEntity(it) }
            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun clearHistory(): Result<Unit> {
        return try {
            messageDao.clearAllMessages()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## Data Mappers

### RouteMapper.kt

```kotlin
package com.gemnav.app.data.mapper

import com.gemnav.app.data.local.database.entity.RouteEntity
import com.gemnav.app.data.remote.dto.RouteResponse
import com.gemnav.app.domain.model.Location
import com.gemnav.app.domain.model.Route
import com.gemnav.app.domain.model.RouteStep
import com.google.gson.Gson
import javax.inject.Inject

class RouteMapper @Inject constructor(
    private val gson: Gson
) {
    
    fun mapFromDto(dto: RouteResponse): Route {
        return Route(
            id = dto.id,
            origin = Location(dto.origin.lat, dto.origin.lng, dto.origin.address),
            destination = Location(dto.destination.lat, dto.destination.lng, dto.destination.address),
            distance = dto.distance,
            duration = dto.duration,
            polyline = dto.polyline,
            steps = dto.steps.map { step ->
                RouteStep(
                    instruction = step.instruction,
                    distance = step.distance,
                    duration = step.duration,
                    polyline = step.polyline,
                    maneuver = step.maneuver
                )
            },
            warnings = dto.warnings,
            tollCost = dto.tollCost
        )
    }
    
    fun mapToEntity(route: Route): RouteEntity {
        return RouteEntity(
            id = route.id,
            originLat = route.origin.latitude,
            originLng = route.origin.longitude,
            originAddress = route.origin.address,
            destLat = route.destination.latitude,
            destLng = route.destination.longitude,
            destAddress = route.destination.address,
            distance = route.distance,
            duration = route.duration,
            polyline = route.polyline,
            steps = gson.toJson(route.steps),
            warnings = gson.toJson(route.warnings),
            tollCost = route.tollCost,
            timestamp = route.timestamp
        )
    }
    
    fun mapFromEntity(entity: RouteEntity): Route {
        val steps = gson.fromJson(entity.steps, Array<RouteStep>::class.java).toList()
        val warnings = gson.fromJson(entity.warnings, Array<String>::class.java).toList()
        
        return Route(
            id = entity.id,
            origin = Location(entity.originLat, entity.originLng, entity.originAddress),
            destination = Location(entity.destLat, entity.destLng, entity.destAddress),
            distance = entity.distance,
            duration = entity.duration,
            polyline = entity.polyline,
            steps = steps,
            warnings = warnings,
            tollCost = entity.tollCost,
            timestamp = entity.timestamp
        )
    }
}
```

---

## Use Cases

### CalculateRouteUseCase.kt

```kotlin
package com.gemnav.app.domain.usecase

import com.gemnav.app.domain.model.Location
import com.gemnav.app.domain.model.Route
import com.gemnav.app.domain.repository.NavigationRepository
import javax.inject.Inject

class CalculateRouteUseCase @Inject constructor(
    private val navigationRepository: NavigationRepository
) {
    
    suspend operator fun invoke(
        origin: Location,
        destination: Location,
        waypoints: List<Location> = emptyList()
    ): Result<Route> {
        // Validate inputs
        if (origin == destination) {
            return Result.failure(Exception("Origin and destination cannot be the same"))
        }
        
        // Calculate route
        val result = navigationRepository.calculateRoute(origin, destination, waypoints)
        
        // Save route to history if successful
        result.onSuccess { route ->
            navigationRepository.saveRoute(route)
        }
        
        return result
    }
}
```

### SendMessageUseCase.kt

```kotlin
package com.gemnav.app.domain.usecase

import com.gemnav.app.domain.model.ChatMessage
import com.gemnav.app.domain.repository.ChatRepository
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    
    suspend operator fun invoke(text: String): Result<String> {
        if (text.isBlank()) {
            return Result.failure(Exception("Message cannot be empty"))
        }
        
        // Save user message
        val userMessage = ChatMessage.User(text, System.currentTimeMillis())
        chatRepository.saveMessage(userMessage)
        
        // Send to Gemini
        val result = chatRepository.sendMessage(text)
        
        // Save assistant response
        result.onSuccess { response ->
            val assistantMessage = ChatMessage.Assistant(response, System.currentTimeMillis())
            chatRepository.saveMessage(assistantMessage)
        }
        
        return result
    }
}
```

---

## DataStore Preferences

### UserPreferences.kt

```kotlin
package com.gemnav.app.data.local.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

class UserPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    
    private object Keys {
        val VOICE_ENABLED = booleanPreferencesKey("voice_enabled")
        val UNITS_METRIC = booleanPreferencesKey("units_metric")
        val AUTO_ZOOM = booleanPreferencesKey("auto_zoom")
        val TRAFFIC_ALERTS = booleanPreferencesKey("traffic_alerts")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
    }
    
    val voiceEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { it[Keys.VOICE_ENABLED] ?: true }
    
    suspend fun setVoiceEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.VOICE_ENABLED] = enabled }
    }
    
    val unitsMetric: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.UNITS_METRIC] ?: true }
    
    suspend fun setUnitsMetric(metric: Boolean) {
        dataStore.edit { it[Keys.UNITS_METRIC] = metric }
    }
}
```

---

## Data Layer Best Practices

### 1. Single Source of Truth
- Database is authoritative for local data
- Network is source for fresh data
- Repository coordinates between sources

### 2. Error Handling
- Use Result<T> for clear success/failure
- Catch exceptions at repository level
- Provide meaningful error messages

### 3. Data Flow
- Repository observes local data via Flow
- UI observes via StateFlow in ViewModel
- One-way data flow from source to UI

### 4. Caching Strategy
- Cache network responses in database
- Serve cached data immediately
- Fetch fresh data in background
- Update UI when fresh data arrives

### 5. Testing
- Mock repository interfaces in tests
- Use in-memory database for DAO tests
- MockWebServer for API tests
- Fake implementations for integration tests
