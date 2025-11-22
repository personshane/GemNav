package com.gemnav.app.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import javax.inject.Inject

class AndroidSpeechRecognitionService @Inject constructor(
    private val context: Context,
    private val tier: SubscriptionTier
) : SpeechRecognitionService {
    
    private var recognizer: SpeechRecognizer? = null
    private var onTranscriptListener: ((String) -> Unit)? = null
    private var onErrorListener: ((SpeechRecognitionError) -> Unit)? = null
    
    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            // Speech recognition is ready
        }
        
        override fun onBeginningOfSpeech() {
            // User started speaking
        }
        
        override fun onRmsChanged(rmsdB: Float) {
            // Audio level changed (can be used for visual feedback)
        }
        
        override fun onBufferReceived(buffer: ByteArray?) {
            // Partial audio buffer received
        }
        
        override fun onEndOfSpeech() {
            // User stopped speaking
        }
        
        override fun onError(error: Int) {
            val speechError = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH -> SpeechRecognitionError.NO_MATCH
                SpeechRecognizer.ERROR_NETWORK -> SpeechRecognitionError.NETWORK_ERROR
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> SpeechRecognitionError.NETWORK_ERROR
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechRecognitionError.INSUFFICIENT_PERMISSIONS
                SpeechRecognizer.ERROR_SERVER -> SpeechRecognitionError.SERVICE_UNAVAILABLE
                SpeechRecognizer.ERROR_CLIENT -> SpeechRecognitionError.SERVICE_UNAVAILABLE
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SpeechRecognitionError.NO_MATCH
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> SpeechRecognitionError.RECOGNIZER_BUSY
                SpeechRecognizer.ERROR_AUDIO -> SpeechRecognitionError.AUDIO_ERROR
                else -> SpeechRecognitionError.UNKNOWN
            }
            
            onErrorListener?.invoke(speechError)
        }
        
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val transcript = matches?.firstOrNull()
            
            if (transcript != null) {
                onTranscriptListener?.invoke(transcript)
            } else {
                onErrorListener?.invoke(SpeechRecognitionError.NO_MATCH)
            }
        }
        
        override fun onPartialResults(partialResults: Bundle?) {
            // Can be used for real-time transcript display
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partialTranscript = matches?.firstOrNull()
            // Optionally emit partial results for UI feedback
        }
        
        override fun onEvent(eventType: Int, params: Bundle?) {
            // Additional events
        }
    }
    
    override fun startListening() {
        // Check if speech recognition is available
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onErrorListener?.invoke(SpeechRecognitionError.SERVICE_UNAVAILABLE)
            return
        }
        
        // Create recognizer if needed
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer?.setRecognitionListener(recognitionListener)
        }
        
        // Configure recognition intent
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            
            // Plus/Pro tier: Use cloud recognition for better accuracy
            if (tier.allowsAdvancedVoice()) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            } else {
                // Free tier: Prefer on-device when available
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
        }
        
        recognizer?.startListening(intent)
    }
    
    override fun stopListening() {
        recognizer?.stopListening()
    }
    
    override fun setOnTranscriptListener(listener: (String) -> Unit) {
        onTranscriptListener = listener
    }
    
    override fun setOnErrorListener(listener: (SpeechRecognitionError) -> Unit) {
        onErrorListener = listener
    }
    
    /**
     * Clean up resources
     */
    fun destroy() {
        recognizer?.destroy()
        recognizer = null
    }
}
