package com.gemnav.core.shim

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException

/**
 * GeminiShim - Safe wrapper for all Gemini AI interactions.
 * Handles switching between Nano (on-device) and Cloud modes,
 * timeout/retry logic, and response sanitization.
 */
object GeminiShim {
    private const val TAG = "GeminiShim"
    private const val DEFAULT_TIMEOUT_MS = 30_000L
    private const val MAX_RETRIES = 3
    private const val RETRY_DELAY_MS = 1000L
    
    private var isInitialized = false
    private var currentMode: GeminiMode = GeminiMode.NANO
    private var lastError: Exception? = null
    
    enum class GeminiMode {
        NANO,   // On-device (Free tier)
        CLOUD   // Cloud API (Plus/Pro tier)
    }
    
    /**
     * Initialize the Gemini shim layer.
     * @param mode The Gemini mode to use (NANO or CLOUD)
     * @return true if initialization successful, false otherwise
     */
    fun initialize(mode: GeminiMode = GeminiMode.NANO): Boolean {
        return try {
            currentMode = mode
            // TODO: Initialize actual Gemini SDK based on mode
            logInfo("GeminiShim initialized in $mode mode")
            isInitialized = true
            true
        } catch (e: Exception) {
            logError("Failed to initialize GeminiShim", e)
            lastError = e
            SafeModeManager.reportFailure("GeminiShim", e)
            false
        }
    }
    
    /**
     * Switch between Nano and Cloud modes.
     * @param mode The new mode to switch to
     * @return true if switch successful
     */
    fun switchMode(mode: GeminiMode): Boolean {
        return try {
            logInfo("Switching Gemini mode from $currentMode to $mode")
            currentMode = mode
            // TODO: Reinitialize SDK for new mode
            true
        } catch (e: Exception) {
            logError("Failed to switch Gemini mode", e)
            lastError = e
            false
        }
    }
    
    /**
     * Send a prompt to Gemini with timeout and retry logic.
     * @param prompt The user prompt
     * @param timeoutMs Timeout in milliseconds
     * @return Response text or null if failed
     */
    suspend fun sendPrompt(
        prompt: String,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): GeminiResponse {
        if (!isInitialized) {
            logWarning("GeminiShim not initialized")
            return GeminiResponse.Error("GeminiShim not initialized")
        }
        
        if (prompt.isBlank()) {
            return GeminiResponse.Error("Empty prompt")
        }
        
        var lastException: Exception? = null
        
        repeat(MAX_RETRIES) { attempt ->
            try {
                return withTimeout(timeoutMs) {
                    // TODO: Implement actual Gemini API call
                    // Stub: Return placeholder response
                    val sanitizedPrompt = sanitizeInput(prompt)
                    logInfo("Sending prompt (attempt ${attempt + 1}): ${sanitizedPrompt.take(50)}...")
                    
                    // Simulate API call - replace with real implementation
                    GeminiResponse.Success(
                        text = "",  // TODO: Real response
                        mode = currentMode,
                        processingTimeMs = 0
                    )
                }
            } catch (e: TimeoutCancellationException) {
                logWarning("Gemini request timed out (attempt ${attempt + 1})")
                lastException = e
                if (attempt < MAX_RETRIES - 1) {
                    delay(RETRY_DELAY_MS * (attempt + 1))
                }
            } catch (e: Exception) {
                logError("Gemini request failed (attempt ${attempt + 1})", e)
                lastException = e
                if (attempt < MAX_RETRIES - 1) {
                    delay(RETRY_DELAY_MS * (attempt + 1))
                }
            }
        }
        
        lastError = lastException
        SafeModeManager.reportFailure("GeminiShim.sendPrompt", lastException)
        return GeminiResponse.Error(lastException?.message ?: "Unknown error after $MAX_RETRIES retries")
    }
    
    /**
     * Parse navigation intent from natural language.
     * @param input User's natural language input
     * @return Parsed navigation intent or null
     */
    suspend fun parseNavigationIntent(input: String): NavigationIntent? {
        return try {
            if (input.isBlank()) return null
            
            // TODO: Implement actual Gemini parsing logic
            // Stub: Return null until implemented
            logInfo("Parsing navigation intent: ${input.take(50)}...")
            null
        } catch (e: Exception) {
            logError("Error parsing navigation intent", e)
            SafeModeManager.reportFailure("GeminiShim.parseNavigationIntent", e)
            null
        }
    }
    
    /**
     * Sanitize user input before sending to AI.
     */
    private fun sanitizeInput(input: String): String {
        return input
            .trim()
            .replace(Regex("[\\x00-\\x1F]"), "") // Remove control characters
            .take(4096) // Limit length
    }
    
    /**
     * Sanitize AI response before returning to app.
     */
    private fun sanitizeOutput(output: String): String {
        return output
            .trim()
            .replace(Regex("[\\x00-\\x1F]"), "") // Remove control characters
    }
    
    fun getCurrentMode(): GeminiMode = currentMode
    fun isAvailable(): Boolean = isInitialized && lastError == null
    fun getLastError(): Exception? = lastError
    fun clearError() { lastError = null }
    
    private fun logInfo(message: String) = Log.i(TAG, message)
    private fun logWarning(message: String) = Log.w(TAG, message)
    private fun logError(message: String, e: Exception) = Log.e(TAG, message, e)
    
    /**
     * Response wrapper for Gemini API calls.
     */
    sealed class GeminiResponse {
        data class Success(
            val text: String,
            val mode: GeminiMode,
            val processingTimeMs: Long
        ) : GeminiResponse()
        
        data class Error(val message: String) : GeminiResponse()
    }
    
    /**
     * Parsed navigation intent from natural language.
     * TODO: Expand with full intent structure
     */
    data class NavigationIntent(
        val destination: String = "",
        val origin: String? = null,
        val waypoints: List<String> = emptyList(),
        val avoidTolls: Boolean = false,
        val avoidHighways: Boolean = false
    )
}
