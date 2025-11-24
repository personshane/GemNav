package com.gemnav.app.ui.voice

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gemnav.core.feature.FeatureGate
import com.gemnav.core.shim.GeminiShim
import com.gemnav.core.shim.SafeModeManager
import com.gemnav.core.subscription.TierManager
import com.gemnav.core.voice.SpeechRecognizerManager
import com.gemnav.data.ai.*
import com.gemnav.data.route.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * VoiceViewModel - Handles voice command functionality with SpeechRecognizer,
 * SafeMode awareness, and feature gating.
 */
class VoiceViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "VoiceViewModel"
    }
    
    sealed class VoiceState {
        object Idle : VoiceState()
        object Listening : VoiceState()
        object Processing : VoiceState()
        data class Result(val text: String, val intent: GeminiShim.LegacyNavigationIntent?) : VoiceState()
        data class Error(val message: String) : VoiceState()
    }
    
    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState: StateFlow<VoiceState> = _voiceState
    
    private val _transcribedText = MutableStateFlow("")
    val transcribedText: StateFlow<String> = _transcribedText
    
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening
    
    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel
    
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError
    
    private val _featureSummary = MutableStateFlow(FeatureGate.getFeatureSummary())
    val featureSummary: StateFlow<FeatureSummary> = _featureSummary
    
    private val _needsPermission = MutableStateFlow(false)
    val needsPermission: StateFlow<Boolean> = _needsPermission
    
    private val _voiceAiRouteState = MutableStateFlow<VoiceAiRouteState>(VoiceAiRouteState.Idle)
    val voiceAiRouteState: StateFlow<VoiceAiRouteState> = _voiceAiRouteState
    
    // MP-020: AI Intent State
    private val _aiIntentState = MutableStateFlow<AiIntentState>(AiIntentState.Idle)
    val aiIntentState: StateFlow<AiIntentState> = _aiIntentState
    
    private val _classifiedIntent = MutableStateFlow<NavigationIntent?>(null)
    val classifiedIntent: StateFlow<NavigationIntent?> = _classifiedIntent
    
    private val speechRecognizerManager: SpeechRecognizerManager by lazy {
        SpeechRecognizerManager(getApplication<Application>().applicationContext).apply {
            setListener(createSpeechListener())
        }
    }
    
    private fun createSpeechListener(): SpeechRecognizerManager.SpeechRecognizerListener {
        return object : SpeechRecognizerManager.SpeechRecognizerListener {
            override fun onPartialResult(text: String) {
                Log.d(TAG, "Partial: $text")
                _transcribedText.value = text
            }
            
            override fun onFinalResult(text: String) {
                Log.d(TAG, "Final: $text")
                _transcribedText.value = text
                _isListening.value = false
                
                if (text.isNotBlank()) {
                    processVoiceCommand(text)
                } else {
                    _voiceState.value = VoiceState.Idle
                }
            }
            
            override fun onError(error: SpeechRecognizerManager.SpeechError) {
                Log.w(TAG, "Speech error: $error")
                _isListening.value = false
                _lastError.value = error.name
                
                val message = when (error) {
                    SpeechRecognizerManager.SpeechError.NOT_AVAILABLE -> 
                        "Speech recognition not available"
                    SpeechRecognizerManager.SpeechError.PERMISSION_DENIED -> {
                        _needsPermission.value = true
                        "Microphone permission required"
                    }
                    SpeechRecognizerManager.SpeechError.NO_MATCH -> 
                        "Didn't catch that. Please try again."
                    SpeechRecognizerManager.SpeechError.SPEECH_TIMEOUT -> 
                        "No speech detected"
                    SpeechRecognizerManager.SpeechError.NETWORK_ERROR,
                    SpeechRecognizerManager.SpeechError.NETWORK_TIMEOUT -> 
                        "Network error. Check connection."
                    else -> "Voice input failed"
                }
                _voiceState.value = VoiceState.Error(message)
            }
            
            override fun onListeningStateChanged(isListening: Boolean) {
                _isListening.value = isListening
                if (isListening) {
                    _voiceState.value = VoiceState.Listening
                }
            }
            
            override fun onAudioLevelChanged(rmsdB: Float) {
                _audioLevel.value = rmsdB
            }
        }
    }
    
    /**
     * Toggle voice listening on button press.
     */
    fun onVoiceButtonPressed() {
        if (_isListening.value) {
            stopListening()
        } else {
            startListening()
        }
    }
    
    /**
     * Start listening for voice input.
     */
    fun startListening() {
        // Check SafeMode
        if (SafeModeManager.isSafeModeEnabled()) {
            Log.d(TAG, "Voice blocked - SafeMode active")
            _voiceState.value = VoiceState.Error("Voice commands unavailable in Safe Mode")
            return
        }
        
        // Check feature gate
        if (!FeatureGate.areAdvancedFeaturesEnabled()) {
            Log.d(TAG, "Voice blocked - Advanced features disabled")
            _voiceState.value = VoiceState.Error("Voice features temporarily unavailable")
            return
        }
        
        // Check permission
        if (!hasRecordAudioPermission()) {
            Log.d(TAG, "Voice blocked - Permission not granted")
            _needsPermission.value = true
            _voiceState.value = VoiceState.Error("Microphone permission required")
            // TODO: Trigger permission request via UI layer
            return
        }
        
        // Check availability
        if (!speechRecognizerManager.isAvailable()) {
            Log.d(TAG, "Voice blocked - SpeechRecognizer not available")
            _voiceState.value = VoiceState.Error("Speech recognition not available on this device")
            return
        }
        
        // Clear previous state
        _transcribedText.value = ""
        _lastError.value = null
        _voiceState.value = VoiceState.Listening
        
        // Start recognition
        // TODO: Get language from Settings
        val success = speechRecognizerManager.startListening(null)
        if (!success) {
            _voiceState.value = VoiceState.Error("Could not start voice input")
        }
        
        Log.d(TAG, "Voice listening started: $success")
    }
    
    /**
     * Stop listening and process the voice input.
     */
    fun stopListening() {
        speechRecognizerManager.stopListening()
        _isListening.value = false
        Log.d(TAG, "Voice listening stopped")
    }
    
    /**
     * Process voice command with AI.
     */
    private fun processVoiceCommand(text: String) {
        _voiceState.value = VoiceState.Processing
        
        viewModelScope.launch {
            try {
                if (FeatureGate.areAdvancedVoiceCommandsEnabled()) {
                    processWithAI(text)
                } else {
                    processBasicCommand(text)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Voice processing failed", e)
                _voiceState.value = VoiceState.Error("Processing failed. Please try again.")
            }
        }
    }
    
    /**
     * Process command with Gemini AI (Plus/Pro tier).
     */
    private suspend fun processWithAI(text: String) {
        if (!FeatureGate.areAIFeaturesEnabled()) {
            Log.d(TAG, "AI processing blocked - falling back to basic")
            processBasicCommand(text)
            return
        }
        
        Log.d(TAG, "Processing with AI: $text")
        
        // Check if this is a navigation query - use full intent pipeline
        if (GeminiShim.isNavigationQuery(text)) {
            processNavigationWithAI(text)
        } else {
            // Non-navigation query - still try intent classification
            val result = GeminiShim.classifyIntent(text)
            when (result) {
                is IntentClassificationResult.Success -> {
                    _classifiedIntent.value = result.intent
                    val legacyIntent = convertToLegacyIntent(result.intent)
                    Log.d(TAG, "AI parsed intent: ${result.intent::class.simpleName}")
                    _voiceState.value = VoiceState.Result(text, legacyIntent)
                }
                is IntentClassificationResult.Failure -> {
                    Log.d(TAG, "AI could not parse intent")
                    _voiceState.value = VoiceState.Result(text, null)
                }
            }
        }
    }
    
    /**
     * Process navigation request with AI routing (MP-020).
     * Now uses full intent classification pipeline.
     */
    private suspend fun processNavigationWithAI(text: String) {
        _voiceAiRouteState.value = VoiceAiRouteState.AiRouting
        _aiIntentState.value = AiIntentState.Classifying
        
        try {
            // Step 1: Classify intent
            val classificationResult = GeminiShim.classifyIntent(text)
            
            when (classificationResult) {
                is IntentClassificationResult.Success -> {
                    val intent = classificationResult.intent
                    _classifiedIntent.value = intent
                    Log.i(TAG, "Voice intent classified: ${intent::class.simpleName}")
                    
                    // Update voice state with intent info
                    _voiceState.value = VoiceState.Result(
                        text = text,
                        intent = convertToLegacyIntent(intent)
                    )
                    
                    // Step 2: Resolve intent
                    _aiIntentState.value = AiIntentState.Reasoning(
                        intent = intent,
                        statusMessage = "Understanding request..."
                    )
                    
                    val resolutionResult = GeminiShim.resolveIntent(intent, null)
                    
                    when (resolutionResult) {
                        is IntentResolutionResult.RouteRequest -> {
                            // Step 3: Get route suggestion
                            _aiIntentState.value = AiIntentState.Suggesting(
                                intent = intent,
                                statusMessage = resolutionResult.explanation ?: "Finding route..."
                            )
                            
                            val routeResult = GeminiShim.getRouteSuggestion(resolutionResult.request)
                            
                            when (routeResult) {
                                is AiRouteResult.Success -> {
                                    Log.i(TAG, "Voice AI route success: ${routeResult.suggestion.destinationName}")
                                    _voiceAiRouteState.value = VoiceAiRouteState.Success(routeResult.suggestion)
                                    _aiIntentState.value = AiIntentState.Success(
                                        intent = intent,
                                        suggestion = routeResult.suggestion
                                    )
                                }
                                is AiRouteResult.Failure -> {
                                    Log.w(TAG, "Voice AI route failed: ${routeResult.reason}")
                                    _voiceAiRouteState.value = VoiceAiRouteState.Error(routeResult.reason)
                                    _aiIntentState.value = AiIntentState.Error(
                                        message = routeResult.reason,
                                        intent = intent
                                    )
                                    _voiceState.value = VoiceState.Error(routeResult.reason)
                                }
                            }
                        }
                        is IntentResolutionResult.NeedsMoreInfo -> {
                            _voiceAiRouteState.value = VoiceAiRouteState.Error(resolutionResult.prompt)
                            _aiIntentState.value = AiIntentState.Error(
                                message = resolutionResult.prompt,
                                intent = intent
                            )
                            _voiceState.value = VoiceState.Error(resolutionResult.prompt)
                        }
                        is IntentResolutionResult.NotSupported -> {
                            _voiceAiRouteState.value = VoiceAiRouteState.Error(resolutionResult.reason)
                            _aiIntentState.value = AiIntentState.Error(
                                message = resolutionResult.reason,
                                intent = intent
                            )
                            _voiceState.value = VoiceState.Error(resolutionResult.reason)
                        }
                        is IntentResolutionResult.Failure -> {
                            _voiceAiRouteState.value = VoiceAiRouteState.Error(resolutionResult.reason)
                            _aiIntentState.value = AiIntentState.Error(
                                message = resolutionResult.reason,
                                intent = intent
                            )
                            _voiceState.value = VoiceState.Error(resolutionResult.reason)
                        }
                    }
                }
                is IntentClassificationResult.Failure -> {
                    Log.w(TAG, "Voice intent classification failed: ${classificationResult.reason}")
                    _voiceAiRouteState.value = VoiceAiRouteState.Error(classificationResult.reason)
                    _aiIntentState.value = AiIntentState.Error(classificationResult.reason)
                    _voiceState.value = VoiceState.Error("I didn't understand that")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Voice AI routing exception", e)
            _voiceAiRouteState.value = VoiceAiRouteState.Error("AI routing failed")
            _aiIntentState.value = AiIntentState.Error("Processing failed: ${e.message}")
            _voiceState.value = VoiceState.Error("Voice command processing failed")
        }
    }
    
    /**
     * Convert new NavigationIntent to legacy format for backward compatibility.
     */
    private fun convertToLegacyIntent(intent: NavigationIntent): GeminiShim.LegacyNavigationIntent? {
        return when (intent) {
            is NavigationIntent.NavigateTo -> GeminiShim.LegacyNavigationIntent(
                destination = intent.destinationText
            )
            is NavigationIntent.FindPOI -> GeminiShim.LegacyNavigationIntent(
                destination = intent.poiType.name.lowercase().replace("_", " ")
            )
            is NavigationIntent.AddStop -> GeminiShim.LegacyNavigationIntent(
                destination = intent.destinationText ?: intent.poiType?.name?.lowercase()?.replace("_", " ") ?: ""
            )
            else -> null
        }
    }
    
    /**
     * Basic command processing (Free tier).
     */
    private fun processBasicCommand(text: String) {
        Log.d(TAG, "Processing basic command: $text")
        
        val lowerText = text.lowercase()
        val basicIntent = when {
            lowerText.contains("home") -> {
                GeminiShim.LegacyNavigationIntent(destination = "home")
            }
            lowerText.contains("work") -> {
                GeminiShim.LegacyNavigationIntent(destination = "work")
            }
            lowerText.startsWith("navigate to") || lowerText.startsWith("go to") -> {
                val destination = lowerText
                    .removePrefix("navigate to")
                    .removePrefix("go to")
                    .trim()
                GeminiShim.LegacyNavigationIntent(destination = destination)
            }
            else -> null
        }
        
        _voiceState.value = VoiceState.Result(text, basicIntent)
    }
    
    /**
     * Cancel current voice operation.
     */
    fun cancel() {
        speechRecognizerManager.cancel()
        _isListening.value = false
        _voiceState.value = VoiceState.Idle
        _voiceAiRouteState.value = VoiceAiRouteState.Idle
        _aiIntentState.value = AiIntentState.Idle
        _classifiedIntent.value = null
        _transcribedText.value = ""
    }
    
    /**
     * Clear AI route state.
     */
    fun clearAiRouteState() {
        _voiceAiRouteState.value = VoiceAiRouteState.Idle
        _aiIntentState.value = AiIntentState.Idle
        _classifiedIntent.value = null
    }
    
    /**
     * Check if RECORD_AUDIO permission is granted.
     */
    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Call after permission is granted.
     */
    fun onPermissionGranted() {
        _needsPermission.value = false
        startListening()
    }
    
    /**
     * Refresh feature state.
     */
    fun refreshFeatureState() {
        _featureSummary.value = FeatureGate.getFeatureSummary()
    }
    
    override fun onCleared() {
        super.onCleared()
        speechRecognizerManager.destroy()
        Log.d(TAG, "VoiceViewModel cleared, SpeechRecognizer destroyed")
    }
}

// Type alias for cleaner imports
typealias FeatureSummary = FeatureGate.FeatureSummary
