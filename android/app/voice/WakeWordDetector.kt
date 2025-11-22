package com.gemnav.app.voice

import android.content.Context
import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback
import com.gemnav.BuildConfig
import javax.inject.Inject

/**
 * Wake word detector for "Hey GemNav" using Porcupine SDK
 * Only available for Plus/Pro tiers
 * 
 * Note: Requires Porcupine SDK dependency and access key in BuildConfig
 */
class WakeWordDetector @Inject constructor(
    private val context: Context
) {
    private var porcupineManager: PorcupineManager? = null
    private var onWakeWordDetected: (() -> Unit)? = null
    private var isRunning = false
    
    /**
     * Set callback for wake word detection
     */
    fun setOnWakeWordDetectedListener(listener: () -> Unit) {
        onWakeWordDetected = listener
    }
    
    /**
     * Start listening for wake word
     */
    fun start() {
        if (isRunning) return
        
        try {
            val callback = PorcupineManagerCallback { keywordIndex ->
                // Wake word detected
                onWakeWordDetected?.invoke()
            }
            
            // Initialize Porcupine with custom wake word
            // Note: Custom wake word model would be trained and included in assets
            // For MVP, using built-in keyword as placeholder
            porcupineManager = PorcupineManager.Builder()
                .setAccessKey(BuildConfig.PORCUPINE_ACCESS_KEY)
                .setKeywordPath("path/to/custom/hey_gemnav.ppn") // Custom wake word model
                .setSensitivity(0.7f) // Sensitivity: 0.0 (least sensitive) to 1.0 (most sensitive)
                .setErrorCallback { error ->
                    // Handle error
                    stop()
                }
                .build(context, callback)
            
            porcupineManager?.start()
            isRunning = true
            
        } catch (e: Exception) {
            // Wake word detection unavailable
            // Fall back to manual activation
            isRunning = false
        }
    }
    
    /**
     * Stop listening for wake word
     */
    fun stop() {
        if (!isRunning) return
        
        try {
            porcupineManager?.stop()
            porcupineManager?.delete()
            porcupineManager = null
            isRunning = false
        } catch (e: Exception) {
            // Cleanup error
        }
    }
    
    /**
     * Check if wake word detection is running
     */
    fun isActive(): Boolean = isRunning
}

/**
 * Alternative wake word detector using on-device model
 * For environments where Porcupine SDK is not available
 */
class SimpleWakeWordDetector @Inject constructor(
    private val context: Context
) {
    private var onWakeWordDetected: (() -> Unit)? = null
    private var isRunning = false
    
    // Placeholder for simpler wake word detection
    // In production, would use TensorFlow Lite or similar
    
    fun setOnWakeWordDetectedListener(listener: () -> Unit) {
        onWakeWordDetected = listener
    }
    
    fun start() {
        if (isRunning) return
        // Initialize simple wake word model
        isRunning = true
    }
    
    fun stop() {
        if (!isRunning) return
        // Stop wake word detection
        isRunning = false
    }
    
    fun isActive(): Boolean = isRunning
}
