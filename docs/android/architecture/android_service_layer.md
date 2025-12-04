# Android Service Layer Implementation

**Micro-Project**: MP-011  
**Platform**: Android  
**Last Updated**: 2025-11-21

---

## Overview

Service layer for GemNav Android app handling GPS tracking, navigation guidance, voice commands, and background execution.

---

## Architecture

```
app/src/main/java/com/gemnav/android/
├── services/
│   ├── location/
│   │   ├── LocationService.kt
│   │   ├── LocationServiceImpl.kt
│   │   └── LocationServiceBinder.kt
│   ├── navigation/
│   │   ├── NavigationService.kt
│   │   ├── NavigationServiceImpl.kt
│   │   └── TurnByTurnManager.kt
│   ├── voice/
│   │   ├── VoiceCommandService.kt
│   │   ├── VoiceCommandServiceImpl.kt
│   │   └── SpeechRecognitionManager.kt
│   └── background/
│       ├── ForegroundServiceManager.kt
│       └── NotificationChannelManager.kt
```

---

## 1. LocationService

### Interface

```kotlin
// services/location/LocationService.kt
package com.gemnav.android.services.location

import android.location.Location
import kotlinx.coroutines.flow.Flow

interface LocationService {
    /**
     * Start location tracking
     * @param highAccuracy Use GPS (true) or network (false)
     * @param updateIntervalMs Update frequency in milliseconds
     */
    suspend fun startTracking(
        highAccuracy: Boolean = true,
        updateIntervalMs: Long = 1000L
    ): Result<Unit>
    
    /**
     * Stop location tracking
     */
    suspend fun stopTracking(): Result<Unit>
    
    /**
     * Get current location (one-time)
     */
    suspend fun getCurrentLocation(): Result<Location>
    
    /**
     * Flow of location updates
     */
    fun locationUpdates(): Flow<Location>
    
    /**
     * Check if tracking is active
     */
    fun isTracking(): Boolean
    
    /**
     * Get last known location (may be cached)
     */
    fun getLastKnownLocation(): Location?
}
```

### Implementation

```kotlin
// services/location/LocationServiceImpl.kt
package com.gemnav.android.services.location

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.IBinder
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.gemnav.android.services.background.ForegroundServiceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocationServiceImpl @Inject constructor(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val foregroundServiceManager: ForegroundServiceManager
) : LocationService {
    
    private var isTrackingActive = false
    private val _locationFlow = MutableStateFlow<Location?>(null)
    private var locationCallback: LocationCallback? = null
    private var lastLocation: Location? = null
    
    override suspend fun startTracking(
        highAccuracy: Boolean,
        updateIntervalMs: Long
    ): Result<Unit> = runCatching {
        if (!hasLocationPermission()) {
            throw SecurityException("Location permission not granted")
        }
        
        if (isTrackingActive) {
            return@runCatching
        }
        
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            updateIntervalMs
        ).apply {
            setMinUpdateIntervalMillis(updateIntervalMs / 2)
            setWaitForAccurateLocation(highAccuracy)
        }.build()
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    lastLocation = location
                    _locationFlow.value = location
                }
            }
        }
        
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )
        
        isTrackingActive = true
    }
    
    override suspend fun stopTracking(): Result<Unit> = runCatching {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
        isTrackingActive = false
    }
    
    override suspend fun getCurrentLocation(): Result<Location> = 
        suspendCancellableCoroutine { continuation ->
            if (!hasLocationPermission()) {
                continuation.resume(
                    Result.failure(SecurityException("Location permission not granted"))
                )
                return@suspendCancellableCoroutine
            }
            
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).addOnSuccessListener { location ->
                if (location != null) {
                    lastLocation = location
                    continuation.resume(Result.success(location))
                } else {
                    continuation.resume(
                        Result.failure(Exception("Unable to get current location"))
                    )
                }
            }.addOnFailureListener { exception ->
                continuation.resume(Result.failure(exception))
            }
        }
    
    override fun locationUpdates(): Flow<Location> = callbackFlow {
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it) }
            }
        }
        
        if (hasLocationPermission()) {
            val request = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                1000L
            ).build()
            
            fusedLocationClient.requestLocationUpdates(
                request,
                callback,
                Looper.getMainLooper()
            )
        }
        
        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }
    
    override fun isTracking(): Boolean = isTrackingActive
    
    override fun getLastKnownLocation(): Location? = lastLocation
    
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
```

### Foreground Service Wrapper

```kotlin
// services/location/LocationServiceBinder.kt
package com.gemnav.android.services.location

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.gemnav.android.services.background.ForegroundServiceManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LocationForegroundService : Service() {
    
    @Inject
    lateinit var locationService: LocationService
    
    @Inject
    lateinit var foregroundServiceManager: ForegroundServiceManager
    
    private val binder = LocationBinder()
    
    inner class LocationBinder : Binder() {
        fun getService(): LocationService = locationService
    }
    
    override fun onCreate() {
        super.onCreate()
        foregroundServiceManager.startForeground(this, "Location tracking active")
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onDestroy() {
        foregroundServiceManager.stopForeground(this)
        super.onDestroy()
    }
}
```

---

## 2. NavigationService

### Interface

```kotlin
// services/navigation/NavigationService.kt
package com.gemnav.android.services.navigation

import com.gemnav.android.domain.model.Route
import com.gemnav.android.domain.model.TurnInstruction
import kotlinx.coroutines.flow.Flow

interface NavigationService {
    /**
     * Start turn-by-turn navigation
     */
    suspend fun startNavigation(route: Route): Result<Unit>
    
    /**
     * Stop navigation
     */
    suspend fun stopNavigation(): Result<Unit>
    
    /**
     * Flow of turn instructions
     */
    fun turnInstructions(): Flow<TurnInstruction>
    
    /**
     * Check if navigation is active
     */
    fun isNavigating(): Boolean
    
    /**
     * Get next turn instruction
     */
    fun getNextTurn(): TurnInstruction?
    
    /**
     * Simulate navigation (testing mode)
     */
    suspend fun simulateNavigation(route: Route, speedMultiplier: Float = 1f): Result<Unit>
}
```

### Implementation

```kotlin
// services/navigation/NavigationServiceImpl.kt
package com.gemnav.android.services.navigation

import android.location.Location
import com.gemnav.android.domain.model.Route
import com.gemnav.android.domain.model.TurnInstruction
import com.gemnav.android.services.location.LocationService
import com.gemnav.android.services.voice.VoiceCommandService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NavigationServiceImpl @Inject constructor(
    private val locationService: LocationService,
    private val voiceCommandService: VoiceCommandService,
    private val turnByTurnManager: TurnByTurnManager
) : NavigationService {
    
    private var isActive = false
    private var currentRoute: Route? = null
    private val _turnFlow = MutableSharedFlow<TurnInstruction>(replay = 1)
    private var navigationJob: Job? = null
    
    override suspend fun startNavigation(route: Route): Result<Unit> = runCatching {
        if (isActive) {
            stopNavigation()
        }
        
        currentRoute = route
        isActive = true
        
        // Start location tracking
        locationService.startTracking(highAccuracy = true, updateIntervalMs = 500L)
        
        // Monitor location and provide turn instructions
        navigationJob = CoroutineScope(Dispatchers.Default).launch {
            locationService.locationUpdates()
                .collect { location ->
                    processLocationUpdate(location)
                }
        }
    }
    
    override suspend fun stopNavigation(): Result<Unit> = runCatching {
        navigationJob?.cancel()
        navigationJob = null
        isActive = false
        currentRoute = null
        locationService.stopTracking()
    }
    
    override fun turnInstructions(): Flow<TurnInstruction> = _turnFlow.asSharedFlow()
    
    override fun isNavigating(): Boolean = isActive
    
    override fun getNextTurn(): TurnInstruction? = 
        turnByTurnManager.getNextInstruction(currentRoute, locationService.getLastKnownLocation())
    
    override suspend fun simulateNavigation(
        route: Route,
        speedMultiplier: Float
    ): Result<Unit> = runCatching {
        // Simulation mode for testing without actual GPS movement
        currentRoute = route
        isActive = true
        
        navigationJob = CoroutineScope(Dispatchers.Default).launch {
            turnByTurnManager.simulateRoute(route, speedMultiplier)
                .collect { instruction ->
                    _turnFlow.emit(instruction)
                    voiceCommandService.speak(instruction.text)
                }
        }
    }
    
    private suspend fun processLocationUpdate(location: Location) {
        currentRoute?.let { route ->
            val instruction = turnByTurnManager.calculateNextInstruction(route, location)
            instruction?.let {
                _turnFlow.emit(it)
                
                // Speak instruction if close enough
                if (it.distanceMeters < 200) {
                    voiceCommandService.speak(it.text)
                }
            }
        }
    }
}
```

### Turn-by-Turn Manager

```kotlin
// services/navigation/TurnByTurnManager.kt
package com.gemnav.android.services.navigation

import android.location.Location
import com.gemnav.android.domain.model.Route
import com.gemnav.android.domain.model.TurnInstruction
import com.gemnav.android.domain.model.TurnType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Singleton
class TurnByTurnManager @Inject constructor() {
    
    private var currentStepIndex = 0
    
    fun calculateNextInstruction(route: Route, location: Location): TurnInstruction? {
        if (currentStepIndex >= route.steps.size) return null
        
        val currentStep = route.steps[currentStepIndex]
        val distance = calculateDistance(
            location.latitude,
            location.longitude,
            currentStep.endLocation.latitude,
            currentStep.endLocation.longitude
        )
        
        // Move to next step if we're close to the end
        if (distance < 20.0) {
            currentStepIndex++
            if (currentStepIndex >= route.steps.size) {
                return TurnInstruction(
                    text = "You have arrived at your destination",
                    type = TurnType.ARRIVE,
                    distanceMeters = 0,
                    streetName = ""
                )
            }
        }
        
        val nextStep = route.steps.getOrNull(currentStepIndex)
        return nextStep?.let {
            TurnInstruction(
                text = generateInstructionText(it, distance.toInt()),
                type = it.turnType,
                distanceMeters = distance.toInt(),
                streetName = it.streetName ?: ""
            )
        }
    }
    
    fun getNextInstruction(route: Route?, location: Location?): TurnInstruction? {
        if (route == null || location == null) return null
        return calculateNextInstruction(route, location)
    }
    
    fun simulateRoute(route: Route, speedMultiplier: Float): Flow<TurnInstruction> = flow {
        for ((index, step) in route.steps.withIndex()) {
            val distance = step.distanceMeters
            val duration = (step.durationSeconds * 1000 / speedMultiplier).toLong()
            
            emit(TurnInstruction(
                text = generateInstructionText(step, distance),
                type = step.turnType,
                distanceMeters = distance,
                streetName = step.streetName ?: ""
            ))
            
            delay(duration)
        }
        
        emit(TurnInstruction(
            text = "You have arrived at your destination",
            type = TurnType.ARRIVE,
            distanceMeters = 0,
            streetName = ""
        ))
    }
    
    private fun generateInstructionText(step: Route.Step, distanceMeters: Int): String {
        val action = when (step.turnType) {
            TurnType.LEFT -> "Turn left"
            TurnType.RIGHT -> "Turn right"
            TurnType.SLIGHT_LEFT -> "Slight left"
            TurnType.SLIGHT_RIGHT -> "Slight right"
            TurnType.SHARP_LEFT -> "Sharp left"
            TurnType.SHARP_RIGHT -> "Sharp right"
            TurnType.STRAIGHT -> "Continue straight"
            TurnType.U_TURN -> "Make a U-turn"
            TurnType.ARRIVE -> "Arrive"
        }
        
        val distance = if (distanceMeters < 1000) {
            "in $distanceMeters meters"
        } else {
            "in ${distanceMeters / 1000} kilometers"
        }
        
        return if (step.streetName.isNullOrEmpty()) {
            "$action $distance"
        } else {
            "$action onto ${step.streetName} $distance"
        }
    }
    
    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
```

---

## 3. VoiceCommandService

### Interface

```kotlin
// services/voice/VoiceCommandService.kt
package com.gemnav.android.services.voice

import kotlinx.coroutines.flow.Flow

interface VoiceCommandService {
    /**
     * Start listening for voice commands
     */
    suspend fun startListening(): Result<Unit>
    
    /**
     * Stop listening
     */
    suspend fun stopListening(): Result<Unit>
    
    /**
     * Flow of recognized speech
     */
    fun recognizedSpeech(): Flow<String>
    
    /**
     * Text-to-speech output
     */
    suspend fun speak(text: String): Result<Unit>
    
    /**
     * Check if listening is active
     */
    fun isListening(): Boolean
    
    /**
     * Set language for recognition
     */
    fun setLanguage(languageCode: String)
}
```

### Implementation

```kotlin
// services/voice/VoiceCommandServiceImpl.kt
package com.gemnav.android.services.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class VoiceCommandServiceImpl @Inject constructor(
    private val context: Context,
    private val speechRecognitionManager: SpeechRecognitionManager
) : VoiceCommandService {
    
    private var textToSpeech: TextToSpeech? = null
    private var ttsInitialized = false
    private var currentLanguage = Locale.US
    
    init {
        initializeTextToSpeech()
    }
    
    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = currentLanguage
                ttsInitialized = true
            }
        }
    }
    
    override suspend fun startListening(): Result<Unit> = runCatching {
        speechRecognitionManager.startListening()
    }
    
    override suspend fun stopListening(): Result<Unit> = runCatching {
        speechRecognitionManager.stopListening()
    }
    
    override fun recognizedSpeech(): Flow<String> = 
        speechRecognitionManager.recognitionResults()
    
    override suspend fun speak(text: String): Result<Unit> = 
        suspendCancellableCoroutine { continuation ->
            if (!ttsInitialized) {
                continuation.resume(Result.failure(Exception("TTS not initialized")))
                return@suspendCancellableCoroutine
            }
            
            val utteranceId = UUID.randomUUID().toString()
            
            textToSpeech?.setOnUtteranceProgressListener(
                object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    
                    override fun onDone(utteranceId: String?) {
                        continuation.resume(Result.success(Unit))
                    }
                    
                    override fun onError(utteranceId: String?) {
                        continuation.resume(
                            Result.failure(Exception("TTS error"))
                        )
                    }
                }
            )
            
            val result = textToSpeech?.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                utteranceId
            )
            
            if (result != TextToSpeech.SUCCESS) {
                continuation.resume(Result.failure(Exception("TTS speak failed")))
            }
        }
    
    override fun isListening(): Boolean = speechRecognitionManager.isListening()
    
    override fun setLanguage(languageCode: String) {
        currentLanguage = Locale.forLanguageTag(languageCode)
        textToSpeech?.language = currentLanguage
        speechRecognitionManager.setLanguage(languageCode)
    }
    
    fun cleanup() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        speechRecognitionManager.cleanup()
    }
}
```

### Speech Recognition Manager

```kotlin
// services/voice/SpeechRecognitionManager.kt
package com.gemnav.android.services.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeechRecognitionManager @Inject constructor(
    private val context: Context
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var listening = false
    private var currentLanguage = "en-US"
    private val _recognitionFlow = MutableSharedFlow<String>(replay = 0)
    
    fun startListening() {
        if (listening) return
        
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        }
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguage)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                listening = true
            }
            
            override fun onBeginningOfSpeech() {}
            
            override fun onRmsChanged(rmsdB: Float) {}
            
            override fun onBufferReceived(buffer: ByteArray?) {}
            
            override fun onEndOfSpeech() {
                listening = false
            }
            
            override fun onError(error: Int) {
                listening = false
            }
            
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { text ->
                    _recognitionFlow.tryEmit(text)
                }
                listening = false
            }
            
            override fun onPartialResults(partialResults: Bundle?) {}
            
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        
        speechRecognizer?.startListening(intent)
    }
    
    fun stopListening() {
        speechRecognizer?.stopListening()
        listening = false
    }
    
    fun recognitionResults(): Flow<String> = _recognitionFlow
    
    fun isListening(): Boolean = listening
    
    fun setLanguage(languageCode: String) {
        currentLanguage = languageCode
    }
    
    fun cleanup() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        listening = false
    }
}
```

---

## 4. Background Service Management

### Foreground Service Manager

```kotlin
// services/background/ForegroundServiceManager.kt
package com.gemnav.android.services.background

import android.app.*
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.gemnav.android.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ForegroundServiceManager @Inject constructor(
    private val context: Context,
    private val notificationChannelManager: NotificationChannelManager
) {
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "gemnav_navigation"
    }
    
    fun startForeground(service: Service, contentText: String) {
        notificationChannelManager.createNavigationChannel()
        
        val notification = createNotification(contentText)
        service.startForeground(NOTIFICATION_ID, notification)
    }
    
    fun updateNotification(service: Service, contentText: String) {
        val notification = createNotification(contentText)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    fun stopForeground(service: Service) {
        service.stopForeground(Service.STOP_FOREGROUND_REMOVE)
    }
    
    private fun createNotification(contentText: String): Notification {
        // Intent to open main activity when notification is tapped
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            context.packageManager.getLaunchIntentForPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("GemNav Navigation")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_navigation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .build()
    }
}
```

### Notification Channel Manager

```kotlin
// services/background/NotificationChannelManager.kt
package com.gemnav.android.services.background

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationChannelManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val NAVIGATION_CHANNEL_ID = "gemnav_navigation"
        private const val NAVIGATION_CHANNEL_NAME = "Navigation"
        private const val NAVIGATION_CHANNEL_DESCRIPTION = "Navigation and location tracking"
        
        private const val ALERTS_CHANNEL_ID = "gemnav_alerts"
        private const val ALERTS_CHANNEL_NAME = "Alerts"
        private const val ALERTS_CHANNEL_DESCRIPTION = "Traffic alerts and warnings"
    }
    
    fun createNavigationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NAVIGATION_CHANNEL_ID,
                NAVIGATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = NAVIGATION_CHANNEL_DESCRIPTION
                setShowBadge(false)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun createAlertsChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALERTS_CHANNEL_ID,
                ALERTS_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = ALERTS_CHANNEL_DESCRIPTION
                setShowBadge(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun createAllChannels() {
        createNavigationChannel()
        createAlertsChannel()
    }
}
```

---

## 5. Dependency Injection Module

```kotlin
// di/ServiceModule.kt
package com.gemnav.android.di

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.gemnav.android.services.background.ForegroundServiceManager
import com.gemnav.android.services.background.NotificationChannelManager
import com.gemnav.android.services.location.LocationService
import com.gemnav.android.services.location.LocationServiceImpl
import com.gemnav.android.services.navigation.NavigationService
import com.gemnav.android.services.navigation.NavigationServiceImpl
import com.gemnav.android.services.navigation.TurnByTurnManager
import com.gemnav.android.services.voice.SpeechRecognitionManager
import com.gemnav.android.services.voice.VoiceCommandService
import com.gemnav.android.services.voice.VoiceCommandServiceImpl
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
    fun provideFusedLocationProviderClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }
    
    @Provides
    @Singleton
    fun provideNotificationChannelManager(
        @ApplicationContext context: Context
    ): NotificationChannelManager {
        return NotificationChannelManager(context)
    }
    
    @Provides
    @Singleton
    fun provideForegroundServiceManager(
        @ApplicationContext context: Context,
        notificationChannelManager: NotificationChannelManager
    ): ForegroundServiceManager {
        return ForegroundServiceManager(context, notificationChannelManager)
    }
    
    @Provides
    @Singleton
    fun provideLocationService(
        @ApplicationContext context: Context,
        fusedLocationClient: FusedLocationProviderClient,
        foregroundServiceManager: ForegroundServiceManager
    ): LocationService {
        return LocationServiceImpl(context, fusedLocationClient, foregroundServiceManager)
    }
    
    @Provides
    @Singleton
    fun provideTurnByTurnManager(): TurnByTurnManager {
        return TurnByTurnManager()
    }
    
    @Provides
    @Singleton
    fun provideSpeechRecognitionManager(
        @ApplicationContext context: Context
    ): SpeechRecognitionManager {
        return SpeechRecognitionManager(context)
    }
    
    @Provides
    @Singleton
    fun provideVoiceCommandService(
        @ApplicationContext context: Context,
        speechRecognitionManager: SpeechRecognitionManager
    ): VoiceCommandService {
        return VoiceCommandServiceImpl(context, speechRecognitionManager)
    }
    
    @Provides
    @Singleton
    fun provideNavigationService(
        locationService: LocationService,
        voiceCommandService: VoiceCommandService,
        turnByTurnManager: TurnByTurnManager
    ): NavigationService {
        return NavigationServiceImpl(locationService, voiceCommandService, turnByTurnManager)
    }
}
```

---

## 6. AndroidManifest.xml Additions

```xml
<!-- Required permissions -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />

<!-- Services -->
<service
    android:name=".services.location.LocationForegroundService"
    android:enabled="true"
    android:exported="false"
    android:foregroundServiceType="location" />
```

---

## Testing Strategy

### Unit Tests
- LocationService mock tests
- NavigationService turn calculation tests
- VoiceCommandService TTS/STT mocks
- TurnByTurnManager distance calculation tests

### Integration Tests
- End-to-end navigation flow
- Location update handling
- Voice command recognition pipeline

---

## Usage Example

```kotlin
// In ViewModel
class NavigationViewModel @Inject constructor(
    private val locationService: LocationService,
    private val navigationService: NavigationService,
    private val voiceCommandService: VoiceCommandService
) : ViewModel() {
    
    val currentLocation = locationService.locationUpdates()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)
    
    val turnInstructions = navigationService.turnInstructions()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)
    
    fun startNavigation(route: Route) {
        viewModelScope.launch {
            navigationService.startNavigation(route)
        }
    }
    
    fun startVoiceCommand() {
        viewModelScope.launch {
            voiceCommandService.startListening()
        }
    }
}
```
