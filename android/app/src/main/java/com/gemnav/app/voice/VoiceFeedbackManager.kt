package com.gemnav.app.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.gemnav.core.navigation.AiVoiceEvent
import java.util.Locale

/**
 * MP-024: Voice feedback manager using Android TextToSpeech.
 * 
 * Provides spoken feedback for POI searches, detour calculations,
 * tier blocks, and stop confirmations. No external APIs required.
 */
class VoiceFeedbackManager(context: Context) : TextToSpeech.OnInitListener {
    
    companion object {
        private const val TAG = "VoiceFeedbackManager"
        private const val UTTERANCE_ID = "gemnav-voice"
    }
    
    /** Enable/disable voice feedback */
    @Volatile
    var enabled: Boolean = true
    
    private val tts = TextToSpeech(context.applicationContext, this)
    
    @Volatile
    private var ready = false
    
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.getDefault())
            ready = result != TextToSpeech.LANG_MISSING_DATA && 
                    result != TextToSpeech.LANG_NOT_SUPPORTED
            
            if (ready) {
                Log.i(TAG, "TTS initialized successfully")
            } else {
                Log.w(TAG, "TTS language not supported: ${Locale.getDefault()}")
            }
        } else {
            Log.e(TAG, "TTS initialization failed with status: $status")
        }
    }
    
    /**
     * Handle a voice event and speak the appropriate message.
     */
    fun handleEvent(event: AiVoiceEvent) {
        if (!enabled || !ready) {
            Log.d(TAG, "Voice disabled or not ready, skipping event: ${event::class.simpleName}")
            return
        }
        
        val text = formatEventText(event)
        Log.d(TAG, "Speaking: $text")
        speak(text)
    }
    
    /**
     * Format voice event into spoken text.
     */
    private fun formatEventText(event: AiVoiceEvent): String {
        return when (event) {
            is AiVoiceEvent.DetourSummary -> {
                val minutes = event.addedMinutes?.let { "$it minutes" } ?: "a few minutes"
                val miles = event.addedMiles?.let { 
                    String.format("%.1f miles", it) 
                } ?: ""
                val offRoute = event.distanceOffRouteMiles?.let { 
                    ", about ${String.format("%.1f", it)} miles off your route" 
                } ?: ""
                
                if (miles.isNotEmpty()) {
                    "Detour found to ${event.poiName}. About $minutes extra and $miles added to your trip$offRoute."
                } else {
                    "Detour found to ${event.poiName}. About $minutes extra$offRoute."
                }
            }
            
            is AiVoiceEvent.UpgradeRequired -> {
                "This feature requires a ${event.requiredTierName} subscription to use ${event.featureName}."
            }
            
            is AiVoiceEvent.StopAdded -> {
                "Added ${event.poiName} as a stop on your route."
            }
            
            is AiVoiceEvent.PoiFound -> {
                val ahead = event.distanceAheadMiles?.let {
                    ", ${String.format("%.1f", it)} miles ahead"
                } ?: ""
                
                if (event.totalResults > 1) {
                    "Found ${event.totalResults} ${event.poiType}s along your route. The closest is ${event.poiName}$ahead."
                } else {
                    "Found ${event.poiName}$ahead."
                }
            }
            
            is AiVoiceEvent.NoPoisFound -> {
                "No ${event.poiType}s found along your route."
            }
            
            AiVoiceEvent.GenericError -> {
                "Sorry, I couldn't calculate a detour right now."
            }
        }
    }
    
    /**
     * Speak text using TTS.
     */
    private fun speak(text: String) {
        if (!ready) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }
    
    /**
     * Stop any ongoing speech.
     */
    fun stop() {
        tts.stop()
    }
    
    /**
     * Release TTS resources.
     */
    fun shutdown() {
        tts.stop()
        tts.shutdown()
        Log.d(TAG, "TTS shutdown")
    }
}
