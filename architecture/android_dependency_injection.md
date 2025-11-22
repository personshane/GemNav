# Android Dependency Injection with Hilt

**Version**: 1.0  
**Framework**: Hilt (Dagger)  
**Platform**: Android

---

## Hilt Setup

### Application Class

```kotlin
package com.gemnav.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GemNavApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize HERE SDK (Pro tier only)
        if (BuildConfig.TIER_PRO) {
            initializeHERESDK()
        }
        
        // Initialize Firebase
        // Initialize Crashlytics
        // Initialize Analytics
    }
    
    private fun initializeHERESDK() {
        // HERE SDK initialization
    }
}
```

---

## AppModule.kt

```kotlin
package com.gemnav.app.di

import android.content.Context
import com.gemnav.app.util.PermissionManager
import com.gemnav.app.util.NetworkMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context {
        return context
    }
    
    @Provides
    @Singleton
    fun providePermissionManager(@ApplicationContext context: Context): PermissionManager {
        return PermissionManager(context)
    }
    
    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
        return NetworkMonitor(context)
    }
    
    @IoDispatcher
    @Provides
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
    
    @MainDispatcher
    @Provides
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
    
    @DefaultDispatcher
    @Provides
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher
```

---

## NetworkModule.kt

```kotlin
package com.gemnav.app.di

import com.gemnav.app.BuildConfig
import com.gemnav.app.data.remote.api.GeminiApi
import com.gemnav.app.data.remote.api.NavigationApi
import com.gemnav.app.data.remote.api.UserApi
import com.gemnav.app.data.remote.interceptor.AuthInterceptor
import com.gemnav.app.data.remote.interceptor.LoggingInterceptor
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    private const val TIMEOUT_SECONDS = 30L
    
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            .create()
    }
    
    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        loggingInterceptor: LoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideAuthInterceptor(): AuthInterceptor {
        return AuthInterceptor()
    }
    
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): LoggingInterceptor {
        return LoggingInterceptor().apply {
            level = if (BuildConfig.ENABLE_LOGGING) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }
    
    @ApiRetrofit
    @Provides
    @Singleton
    fun provideApiRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    @GeminiRetrofit
    @Provides
    @Singleton
    fun provideGeminiRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    @Provides
    @Singleton
    fun provideNavigationApi(@ApiRetrofit retrofit: Retrofit): NavigationApi {
        return retrofit.create(NavigationApi::class.java)
    }
    
    @Provides
    @Singleton
    fun provideGeminiApi(@GeminiRetrofit retrofit: Retrofit): GeminiApi {
        return retrofit.create(GeminiApi::class.java)
    }
    
    @Provides
    @Singleton
    fun provideUserApi(@ApiRetrofit retrofit: Retrofit): UserApi {
        return retrofit.create(UserApi::class.java)
    }
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApiRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GeminiRetrofit
```

---

## DatabaseModule.kt

```kotlin
package com.gemnav.app.di

import android.content.Context
import androidx.room.Room
import com.gemnav.app.data.local.database.GemNavDatabase
import com.gemnav.app.data.local.database.dao.MessageDao
import com.gemnav.app.data.local.database.dao.RouteDao
import com.gemnav.app.data.local.database.dao.LocationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    private const val DATABASE_NAME = "gemnav_database"
    
    @Provides
    @Singleton
    fun provideGemNavDatabase(
        @ApplicationContext context: Context
    ): GemNavDatabase {
        return Room.databaseBuilder(
            context,
            GemNavDatabase::class.java,
            DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    @Singleton
    fun provideRouteDao(database: GemNavDatabase): RouteDao {
        return database.routeDao()
    }
    
    @Provides
    @Singleton
    fun provideMessageDao(database: GemNavDatabase): MessageDao {
        return database.messageDao()
    }
    
    @Provides
    @Singleton
    fun provideLocationDao(database: GemNavDatabase): LocationDao {
        return database.locationDao()
    }
}
```

---

## RepositoryModule.kt

```kotlin
package com.gemnav.app.di

import com.gemnav.app.data.repository.ChatRepositoryImpl
import com.gemnav.app.data.repository.NavigationRepositoryImpl
import com.gemnav.app.data.repository.UserRepositoryImpl
import com.gemnav.app.domain.repository.ChatRepository
import com.gemnav.app.domain.repository.NavigationRepository
import com.gemnav.app.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindNavigationRepository(
        impl: NavigationRepositoryImpl
    ): NavigationRepository
    
    @Binds
    @Singleton
    abstract fun bindChatRepository(
        impl: ChatRepositoryImpl
    ): ChatRepository
    
    @Binds
    @Singleton
    abstract fun bindUserRepository(
        impl: UserRepositoryImpl
    ): UserRepository
}
```

---

## NavigationModule.kt (Tier-Specific)

```kotlin
package com.gemnav.app.di

import com.gemnav.app.BuildConfig
import com.gemnav.app.data.navigation.GoogleMapsNavigator
import com.gemnav.app.data.navigation.HERENavigator
import com.gemnav.app.domain.navigation.Navigator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NavigationModule {
    
    @Provides
    @Singleton
    fun provideNavigator(): Navigator {
        return when {
            BuildConfig.TIER_PRO -> {
                // Pro tier uses HERE SDK with Google Maps toggle
                HERENavigator()
            }
            BuildConfig.TIER_PLUS -> {
                // Plus tier uses Google Maps SDK
                GoogleMapsNavigator()
            }
            else -> {
                // Free tier uses intents (no SDK)
                GoogleMapsNavigator(intentMode = true)
            }
        }
    }
}
```

---

## LocationModule.kt

```kotlin
package com.gemnav.app.di

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.gemnav.app.data.location.LocationDataSource
import com.gemnav.app.data.location.LocationDataSourceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocationModule {
    
    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }
    
    @Provides
    @Singleton
    fun provideLocationDataSource(
        fusedLocationClient: FusedLocationProviderClient
    ): LocationDataSource {
        return LocationDataSourceImpl(fusedLocationClient)
    }
}
```

---

## GeminiModule.kt (Tier-Specific)

```kotlin
package com.gemnav.app.di

import android.content.Context
import com.gemnav.app.BuildConfig
import com.gemnav.app.data.ai.GeminiCloudService
import com.gemnav.app.data.ai.GeminiNanoService
import com.gemnav.app.domain.ai.GeminiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GeminiModule {
    
    @Provides
    @Singleton
    fun provideGeminiService(
        @ApplicationContext context: Context
    ): GeminiService {
        return if (BuildConfig.TIER_FREE) {
            // Free tier uses on-device Gemini Nano
            GeminiNanoService(context)
        } else {
            // Plus and Pro use Gemini Cloud API
            GeminiCloudService()
        }
    }
}
```

---

## PreferencesModule.kt

```kotlin
package com.gemnav.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.gemnav.app.data.local.preferences.UserPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {
    
    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }
    
    @Provides
    @Singleton
    fun provideUserPreferences(
        dataStore: DataStore<Preferences>
    ): UserPreferences {
        return UserPreferences(dataStore)
    }
}
```

---

## UseCaseModule.kt

```kotlin
package com.gemnav.app.di

import com.gemnav.app.domain.repository.ChatRepository
import com.gemnav.app.domain.repository.NavigationRepository
import com.gemnav.app.domain.usecase.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object UseCaseModule {
    
    @Provides
    @ViewModelScoped
    fun provideCalculateRouteUseCase(
        repository: NavigationRepository
    ): CalculateRouteUseCase {
        return CalculateRouteUseCase(repository)
    }
    
    @Provides
    @ViewModelScoped
    fun provideSendMessageUseCase(
        repository: ChatRepository
    ): SendMessageUseCase {
        return SendMessageUseCase(repository)
    }
    
    @Provides
    @ViewModelScoped
    fun provideGetNavigationStateUseCase(
        repository: NavigationRepository
    ): GetNavigationStateUseCase {
        return GetNavigationStateUseCase(repository)
    }
}
```

---

## ServiceModule.kt

```kotlin
package com.gemnav.app.di

import android.content.Context
import com.gemnav.app.service.LocationService
import com.gemnav.app.service.NavigationService
import com.gemnav.app.service.VoiceCommandService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {
    
    @Provides
    @Singleton
    fun provideLocationService(
        @ApplicationContext context: Context
    ): LocationService {
        return LocationService()
    }
    
    @Provides
    @Singleton
    fun provideNavigationService(
        @ApplicationContext context: Context
    ): NavigationService {
        return NavigationService()
    }
    
    @Provides
    @Singleton
    fun provideVoiceCommandService(
        @ApplicationContext context: Context
    ): VoiceCommandService {
        return VoiceCommandService()
    }
}
```

---

## FirebaseModule.kt

```kotlin
package com.gemnav.app.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }
    
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }
    
    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }
}
```

---

## Hilt Entry Points

### For Services

```kotlin
package com.gemnav.app.service

import android.app.Service
import com.gemnav.app.domain.repository.NavigationRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NavigationServiceEntryPoint {
    fun navigationRepository(): NavigationRepository
}

class NavigationService : Service() {
    
    private lateinit var navigationRepository: NavigationRepository
    
    override fun onCreate() {
        super.onCreate()
        
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            NavigationServiceEntryPoint::class.java
        )
        navigationRepository = entryPoint.navigationRepository()
    }
    
    // Service implementation
}
```

---

## Dependency Graph

```
┌─────────────────────────────────────┐
│      Application Component          │
│         (Singleton)                 │
└─────────────────────────────────────┘
              │
              ├─── AppModule
              │    ├─ Context
              │    ├─ PermissionManager
              │    ├─ NetworkMonitor
              │    └─ Dispatchers
              │
              ├─── NetworkModule
              │    ├─ OkHttpClient
              │    ├─ Retrofit (API)
              │    ├─ Retrofit (Gemini)
              │    └─ API Interfaces
              │
              ├─── DatabaseModule
              │    ├─ Room Database
              │    └─ DAOs
              │
              ├─── RepositoryModule
              │    ├─ NavigationRepository
              │    ├─ ChatRepository
              │    └─ UserRepository
              │
              ├─── NavigationModule (Tier-Specific)
              │    └─ Navigator (HERE/Google Maps)
              │
              ├─── LocationModule
              │    ├─ FusedLocationProvider
              │    └─ LocationDataSource
              │
              ├─── GeminiModule (Tier-Specific)
              │    └─ GeminiService (Nano/Cloud)
              │
              ├─── PreferencesModule
              │    └─ UserPreferences
              │
              └─── FirebaseModule
                   ├─ FirebaseAuth
                   ├─ Firestore
                   └─ Storage

┌─────────────────────────────────────┐
│      ViewModel Component            │
│       (ViewModelScoped)             │
└─────────────────────────────────────┘
              │
              └─── UseCaseModule
                   ├─ CalculateRouteUseCase
                   ├─ SendMessageUseCase
                   └─ GetNavigationStateUseCase
```

---

## Testing with Hilt

### HiltTestApplication

```kotlin
package com.gemnav.app

import dagger.hilt.android.testing.HiltAndroidApp

@HiltAndroidApp
class HiltTestApplication : GemNavApplication()
```

### Test Module Override

```kotlin
package com.gemnav.app.di

import com.gemnav.app.data.repository.FakeNavigationRepository
import com.gemnav.app.domain.repository.NavigationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RepositoryModule::class]
)
object TestRepositoryModule {
    
    @Provides
    @Singleton
    fun provideNavigationRepository(): NavigationRepository {
        return FakeNavigationRepository()
    }
}
```

---

## Best Practices

### 1. Scope Management
- Use `@Singleton` for app-wide dependencies
- Use `@ViewModelScoped` for ViewModel dependencies
- Use `@ActivityScoped` for Activity dependencies

### 2. Module Organization
- Group related dependencies
- Use separate modules for different layers
- Keep modules focused and cohesive

### 3. Qualifiers
- Use qualifiers for multiple instances of same type
- Custom annotations for clarity
- Document usage in KDoc

### 4. Testing
- Provide test doubles via `@TestInstallIn`
- Use `@UninstallModules` to remove production modules
- Create fake implementations for repositories

### 5. Tier-Specific Dependencies
- Use `BuildConfig` flags for conditional provisioning
- Separate modules for tier-specific features
- Keep tier logic isolated in DI layer
