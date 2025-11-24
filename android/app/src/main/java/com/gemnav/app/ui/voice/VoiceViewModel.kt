package com.gemnav.app.ui.voice

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemnav.core.feature.FeatureGate
import com.gemnav.core.shim.GeminiShim
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * VoiceViewModel - Handles voice command functionality with feature gating.
 */
class VoiceViewModel : ViewModel() {
    
    companion object {
        private const val TAG = "VoiceViewModel"
    }
    
    sealed class VoiceState {
        object Idle : VoiceState()
        object Listening : VoiceState()
        object Processing : VoiceState()
        data class Result(val text: String, val intent: GeminiShim.NavigationIntent?) : VoiceState()
        data class Error(val message: String) : VoiceState()
    }
    
    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState: StateFlow<VoiceState> = _voiceState
    
    private val _transcribedText = MutableStateFlow("")
    val transcribedText: StateFlow<String> = _transcribedText
    
    private val _featureSummary = MutableStateFlow(FeatureGate.getFeatureSummary())
    val featureSummary: StateFlow<FeatureGate.FeatureSummary> = _featureSummary
    
    /**
     * Start listening for voice input.
     * Basic voice available to all tiers; advanced processing requires Plus/Pro.
     */
    fun startListening() {
        if (!FeatureGate.areAdvancedFeaturesEnabled()) {
            Log.d(TAG, "Voice blocked - SafeMode active")
            _voiceState.value = VoiceState.Error("Voice commands unavailable in Safe Mode")
            return
        }
        
        _voiceState.value = VoiceState.Listening
        _transcribedText.value = ""
        
        // TODO: Start actual speech recognition
        Log.d(TAG, "Voice listening started")
    }
    
    /**
     * Stop listening and process the voice input.
     */
    fun stopListening() {
        if (_voiceState.value != VoiceState.Listening) {
            return
        }
        
        // TODO: Stop actual speech recognition
        Log.d(TAG, "Voice listening stopped")
        
        // Process the transcribed text
        val text = _transcribedText.value
        if (text.isNotBlank()) {
            processVoiceCommand(text)
        } else {
            _voiceState.value = VoiceState.Idle
        }
    }
    
    /**
     * Handle transcription result from speech recognizer.
     */
    fun onTranscriptionResult(text: String) {
        _transcribedText.value = text
    }
    
    /**
     * Process voice command with AI.
     * Advanced processing gated by FeatureGate.areAdvancedVoiceCommandsEnabled()
     */
    private fun processVoiceCommand(text: String) {
        _voiceState.value = VoiceState.Processing
        
        viewModelScope.launch {
            try {
                if (FeatureGate.areAdvancedVoiceCommandsEnabled()) {
                    // Advanced AI processing for Plus/Pro tiers
                    processWithAI(text)
                } else {
                    // Basic processing for Free tier
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
        val intent = GeminiShim.parseNavigationIntent(text)
        
        if (intent != null) {
            Log.d(TAG, "AI parsed intent: ${intent.destination}")
            _voiceState.value = VoiceState.Result(text, intent)
        } else {
            Log.d(TAG, "AI could not parse intent")
            _voiceState.value = VoiceState.Result(text, null)
        }
    }
    
    /**
     * Basic command processing (Free tier).
     * Simple keyword matching without AI.
     */
    private fun processBasicCommand(text: String) {
        Log.d(TAG, "Processing basic command: $text")
        
        // TODO: Implement basic keyword matching
        // Examples: "navigate to [place]", "go home", "go to work"
        val lowerText = text.lowercase()
        
        val basicIntent = when {
            lowerText.contains("home") -> {
                GeminiShim.NavigationIntent(destination = "home")
            }
            lowerText.contains("work") -> {
                GeminiShim.NavigationIntent(destination = "work")
            }
            lowerText.startsWith("navigate to") || lowerText.startsWith("go to") -> {
                val destination = lowerText
                    .removePrefix("navigate to")
                    .removePrefix("go to")
                    .trim()
                GeminiShim.NavigationIntent(destination = destination)
            }
            else -> null
        }
        
        _voiceState.value = VoiceState.Result(text, basicIntent)
    }
    
    /**
     * Cancel current voice operation.
     */
    fun cancel() {
        // TODO: Cancel speech recognition if active
        _voiceState.value = VoiceState.Idle
        _transcribedText.value = ""
    }
    
    /**
     * Refresh feature state.
     */
    fun refreshFeatureState() {
        _featureSummary.value = FeatureGate.getFeatureSummary()
    }
}
