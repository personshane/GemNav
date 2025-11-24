package com.gemnav.core.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.gemnav.core.shim.SafeModeManager

/**
 * SpeechRecognizerManager - Wrapper for Android's SpeechRecognizer API.
 * Provides clean callbacks and handles all edge cases gracefully.
 */
class SpeechRecognizerManager(private val context: Context) {
    
    companion object {
        private const val TAG = "SpeechRecognizerManager"
        private const val DEFAULT_LANGUAGE = "en-US"
    }
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var listener: SpeechRecognizerListener? = null
    private var isListeningInternal = false
    
    /**
     * Check if speech recognition is available on this device.
     */
    fun isAvailable(): Boolean {
        return try {
            SpeechRecognizer.isRecognitionAvailable(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking speech recognition availability", e)
            false
        }
    }
    
    /**
     * Set the listener for speech recognition events.
     */
    fun setListener(listener: SpeechRecognizerListener?) {
        this.listener = listener
    }
    
    /**
     * Start listening for speech input.
     * @param languageCode BCP-47 language code (e.g., "en-US", "es-ES")
     * @return true if listening started successfully
     */
    fun startListening(languageCode: String? = null): Boolean {
        // SafeMode check - defense in depth
        if (SafeModeManager.isSafeModeEnabled()) {
            Log.w(TAG, "Speech recognition blocked - SafeMode active")
            listener?.onError(SpeechError.NOT_AVAILABLE)
            return false
        }
        
        if (isListeningInternal) {
            Log.d(TAG, "Already listening, ignoring start request")
            return true
        }
        
        if (!isAvailable()) {
            Log.w(TAG, "Speech recognition not available on this device")
            listener?.onError(SpeechError.NOT_AVAILABLE)
            return false
        }
        
        return try {
            // Create or recreate recognizer
            destroyRecognizer()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(createRecognitionListener())
            
            // Configure intent
            val intent = createRecognizerIntent(languageCode ?: DEFAULT_LANGUAGE)
            
            // Start listening
            speechRecognizer?.startListening(intent)
            isListeningInternal = true
            Log.d(TAG, "Started listening with language: ${languageCode ?: DEFAULT_LANGUAGE}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition", e)
            listener?.onError(SpeechError.UNKNOWN)
            isListeningInternal = false
            false
        }
    }
    
    /**
     * Stop listening for speech input.
     */
    fun stopListening() {
        if (!isListeningInternal) {
            return
        }
        
        try {
            speechRecognizer?.stopListening()
            Log.d(TAG, "Stopped listening")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping speech recognition", e)
        } finally {
            isListeningInternal = false
            listener?.onListeningStateChanged(false)
        }
    }
    
    /**
     * Cancel current recognition session.
     */
    fun cancel() {
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling speech recognition", e)
        } finally {
            isListeningInternal = false
            listener?.onListeningStateChanged(false)
        }
    }
    
    /**
     * Release all resources. Must be called when done.
     */
    fun destroy() {
        destroyRecognizer()
        listener = null
        Log.d(TAG, "SpeechRecognizerManager destroyed")
    }
    
    /**
     * Check if currently listening.
     */
    fun isListening(): Boolean = isListeningInternal
    
    private fun destroyRecognizer() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying speech recognizer", e)
        }
        speechRecognizer = null
        isListeningInternal = false
    }
    
    private fun createRecognizerIntent(languageCode: String): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            // TODO: Add EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS for custom timeout
            // TODO: Add hotword detection configuration
        }
    }
    
    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
                listener?.onListeningStateChanged(true)
            }
            
            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Beginning of speech detected")
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                // Audio level changed - can be used for visual feedback
                listener?.onAudioLevelChanged(rmsdB)
            }
            
            override fun onBufferReceived(buffer: ByteArray?) {
                // Raw audio buffer - not typically needed
            }
            
            override fun onEndOfSpeech() {
                Log.d(TAG, "End of speech detected")
            }
            
            override fun onError(error: Int) {
                val speechError = mapErrorCode(error)
                Log.w(TAG, "Speech recognition error: $speechError (code: $error)")
                isListeningInternal = false
                listener?.onListeningStateChanged(false)
                listener?.onError(speechError)
            }
            
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val finalText = matches?.firstOrNull() ?: ""
                Log.d(TAG, "Final result: $finalText")
                isListeningInternal = false
                listener?.onListeningStateChanged(false)
                listener?.onFinalResult(finalText)
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partialText = matches?.firstOrNull() ?: ""
                if (partialText.isNotEmpty()) {
                    Log.d(TAG, "Partial result: $partialText")
                    listener?.onPartialResult(partialText)
                }
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {
                Log.d(TAG, "Speech event: $eventType")
            }
        }
    }
    
    private fun mapErrorCode(errorCode: Int): SpeechError {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> SpeechError.AUDIO_ERROR
            SpeechRecognizer.ERROR_CLIENT -> SpeechError.CLIENT_ERROR
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechError.PERMISSION_DENIED
            SpeechRecognizer.ERROR_NETWORK -> SpeechError.NETWORK_ERROR
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> SpeechError.NETWORK_TIMEOUT
            SpeechRecognizer.ERROR_NO_MATCH -> SpeechError.NO_MATCH
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> SpeechError.RECOGNIZER_BUSY
            SpeechRecognizer.ERROR_SERVER -> SpeechError.SERVER_ERROR
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SpeechError.SPEECH_TIMEOUT
            else -> SpeechError.UNKNOWN
        }
    }
    
    /**
     * Listener interface for speech recognition events.
     */
    interface SpeechRecognizerListener {
        fun onPartialResult(text: String)
        fun onFinalResult(text: String)
        fun onError(error: SpeechError)
        fun onListeningStateChanged(isListening: Boolean)
        fun onAudioLevelChanged(rmsdB: Float) {}
    }
    
    /**
     * Speech recognition error types.
     */
    enum class SpeechError {
        NOT_AVAILABLE,
        PERMISSION_DENIED,
        AUDIO_ERROR,
        CLIENT_ERROR,
        NETWORK_ERROR,
        NETWORK_TIMEOUT,
        NO_MATCH,
        RECOGNIZER_BUSY,
        SERVER_ERROR,
        SPEECH_TIMEOUT,
        UNKNOWN
    }
}
