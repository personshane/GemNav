package com.gemnav.app.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central coordinator for voice command functionality.
 * Manages speech recognition, command parsing, execution, and TTS responses.
 */
@Singleton
class VoiceCommandManager @Inject constructor(
    private val speechRecognitionService: SpeechRecognitionService,
    private val commandParser: CommandParser,
    private val commandExecutor: CommandExecutor,
    private val voiceResponseService: VoiceResponseService,
    private val wakeWordDetector: WakeWordDetector?,
    private val tier: SubscriptionTier,
    private val coroutineScope: CoroutineScope
) {
    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()
    
    private val _lastTranscript = MutableStateFlow<String?>(null)
    val lastTranscript: StateFlow<String?> = _lastTranscript.asStateFlow()
    
    private val conversationContext = mutableListOf<ConversationTurn>()
    private var isWakeWordEnabled = false
    
    init {
        setupSpeechRecognition()
        if (tier.allowsWakeWord()) {
            setupWakeWord()
        }
    }
    
    private fun setupSpeechRecognition() {
        speechRecognitionService.setOnTranscriptListener { transcript ->
            coroutineScope.launch {
                handleTranscript(transcript)
            }
        }
        
        speechRecognitionService.setOnErrorListener { error ->
            handleRecognitionError(error)
        }
    }
    
    private fun setupWakeWord() {
        wakeWordDetector?.setOnWakeWordDetectedListener {
            if (isWakeWordEnabled && _voiceState.value == VoiceState.Idle) {
                startListening(VoiceTrigger.WakeWord)
            }
        }
    }
    
    /**
     * Start listening for voice input
     */
    fun startListening(trigger: VoiceTrigger = VoiceTrigger.Manual) {
        if (_voiceState.value == VoiceState.Listening) return
        
        _voiceState.value = VoiceState.Listening
        speechRecognitionService.startListening()
    }
    
    /**
     * Stop listening for voice input
     */
    fun stopListening() {
        if (_voiceState.value != VoiceState.Listening) return
        
        speechRecognitionService.stopListening()
        _voiceState.value = VoiceState.Idle
    }
    
    /**
     * Process voice transcript
     */
    private suspend fun handleTranscript(transcript: String) {
        _lastTranscript.value = transcript
        _voiceState.value = VoiceState.Processing
        
        try {
            // Add to conversation context if Plus/Pro
            if (tier.allowsAdvancedVoice()) {
                conversationContext.add(
                    ConversationTurn(role = "user", content = transcript)
                )
            }
            
            // Parse command using Gemini
            val command = commandParser.parse(transcript, conversationContext)
            
            // Execute command
            val result = commandExecutor.execute(command)
            
            // Handle result
            when (result) {
                is CommandResult.Success -> {
                    speak(result.message, interrupt = true)
                    if (tier.allowsAdvancedVoice()) {
                        conversationContext.add(
                            ConversationTurn(role = "assistant", content = result.message)
                        )
                    }
                }
                
                is CommandResult.Error -> {
                    speak(result.message, interrupt = true)
                    _voiceState.value = VoiceState.Error(result.message)
                }
                
                is CommandResult.TierRestricted -> {
                    speak(result.message, interrupt = true)
                    _voiceState.value = VoiceState.Error(result.message)
                }
                
                is CommandResult.NeedsClarification -> {
                    speak(result.question, interrupt = true)
                    // Keep listening for follow-up
                    startListening(VoiceTrigger.Continuation)
                }
            }
            
        } catch (e: Exception) {
            val errorMessage = "Sorry, I couldn't process that command"
            _voiceState.value = VoiceState.Error(errorMessage)
            speak(errorMessage, interrupt = true)
        }
    }
    
    /**
     * Speak text via TTS
     */
    fun speak(text: String, interrupt: Boolean = false) {
        _voiceState.value = VoiceState.Speaking(text)
        voiceResponseService.speak(text, interrupt)
        
        // Auto-return to idle after speaking
        coroutineScope.launch {
            kotlinx.coroutines.delay(text.length * 50L) // Rough estimate
            if (_voiceState.value is VoiceState.Speaking) {
                _voiceState.value = VoiceState.Idle
            }
        }
    }
    
    /**
     * Cancel current speech
     */
    fun cancelSpeech() {
        voiceResponseService.stop()
        if (_voiceState.value is VoiceState.Speaking) {
            _voiceState.value = VoiceState.Idle
        }
    }
    
    /**
     * Enable/disable wake word detection (Plus/Pro only)
     */
    fun setWakeWordEnabled(enabled: Boolean) {
        if (!tier.allowsWakeWord()) {
            return
        }
        
        isWakeWordEnabled = enabled
        if (enabled) {
            wakeWordDetector?.start()
        } else {
            wakeWordDetector?.stop()
        }
    }
    
    /**
     * Clear conversation context
     */
    fun clearConversationContext() {
        conversationContext.clear()
    }
    
    /**
     * Handle recognition errors
     */
    private fun handleRecognitionError(error: SpeechRecognitionError) {
        val message = when (error) {
            SpeechRecognitionError.NO_MATCH -> "I didn't catch that. Please try again."
            SpeechRecognitionError.NETWORK_ERROR -> "Network error. Voice recognition unavailable."
            SpeechRecognitionError.PERMISSION_DENIED -> "Microphone permission required for voice commands."
            SpeechRecognitionError.INSUFFICIENT_PERMISSIONS -> "Microphone permission required for voice commands."
            SpeechRecognitionError.SERVICE_UNAVAILABLE -> "Speech recognition service unavailable."
            else -> "Voice recognition error. Please try again."
        }
        
        _voiceState.value = VoiceState.Error(message)
        speak(message, interrupt = true)
    }
    
    /**
     * Cleanup resources
     */
    fun shutdown() {
        stopListening()
        wakeWordDetector?.stop()
        voiceResponseService.shutdown()
    }
}

// Voice states
sealed class VoiceState {
    object Idle : VoiceState()
    object Listening : VoiceState()
    object Processing : VoiceState()
    data class Speaking(val text: String) : VoiceState()
    data class Error(val message: String) : VoiceState()
}

// Voice trigger types
enum class VoiceTrigger {
    Manual,      // User tapped microphone button
    WakeWord,    // Wake word detected
    Continuation // Continuation of multi-turn conversation
}

// Conversation context for multi-turn dialogue
data class ConversationTurn(
    val role: String,  // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

// Subscription tier extensions
fun SubscriptionTier.allowsWakeWord(): Boolean {
    return this == SubscriptionTier.PLUS || this == SubscriptionTier.PRO
}

fun SubscriptionTier.allowsAdvancedVoice(): Boolean {
    return this == SubscriptionTier.PLUS || this == SubscriptionTier.PRO
}
