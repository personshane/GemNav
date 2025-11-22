package com.gemnav.app.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import javax.inject.Inject

class AndroidVoiceResponseService @Inject constructor(
    private val context: Context
) : VoiceResponseService {
    
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val utteranceListeners = mutableListOf<(Boolean) -> Unit>()
    
    init {
        initializeTTS()
    }
    
    private fun initializeTTS() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.let { engine ->
                    // Set language to device default
                    val result = engine.setLanguage(Locale.getDefault())
                    
                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        // Fallback to US English if default language not available
                        engine.setLanguage(Locale.US)
                    }
                    
                    // Set speech rate and pitch (can be customized)
                    engine.setSpeechRate(1.0f)
                    engine.setPitch(1.0f)
                    
                    // Set utterance progress listener
                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            // TTS started speaking
                        }
                        
                        override fun onDone(utteranceId: String?) {
                            // TTS finished speaking
                            utteranceListeners.forEach { it(true) }
                        }
                        
                        override fun onError(utteranceId: String?) {
                            // TTS error
                            utteranceListeners.forEach { it(false) }
                        }
                    })
                    
                    isInitialized = true
                }
            }
        }
    }
    
    override fun speak(text: String, interrupt: Boolean) {
        if (!isInitialized || text.isBlank()) return
        
        val queueMode = if (interrupt) {
            TextToSpeech.QUEUE_FLUSH  // Stop current speech and start new
        } else {
            TextToSpeech.QUEUE_ADD    // Add to queue after current speech
        }
        
        val utteranceId = System.currentTimeMillis().toString()
        
        tts?.speak(text, queueMode, null, utteranceId)
    }
    
    override fun stop() {
        tts?.stop()
    }
    
    /**
     * Add listener for TTS completion
     */
    fun addUtteranceListener(listener: (Boolean) -> Unit) {
        utteranceListeners.add(listener)
    }
    
    /**
     * Remove utterance listener
     */
    fun removeUtteranceListener(listener: (Boolean) -> Unit) {
        utteranceListeners.remove(listener)
    }
    
    override fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        utteranceListeners.clear()
    }
}
