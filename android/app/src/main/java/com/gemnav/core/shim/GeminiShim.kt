package com.gemnav.core.shim

import android.util.Log
import com.gemnav.app.BuildConfig
import com.gemnav.core.feature.FeatureGate
import com.gemnav.data.ai.*
import com.gemnav.data.route.LatLng
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
    
    // ==================== AI ROUTING (MP-016) ====================
    
    /**
     * Get AI-generated route suggestion from Gemini.
     * Enforces tier gating, SafeMode, and API key validation.
     * 
     * @param request AI route request containing query and context
     * @return AiRouteResult with suggestion or failure reason
     */
    suspend fun getRouteSuggestion(request: AiRouteRequest): AiRouteResult {
        // SafeMode check
        if (SafeModeManager.isSafeModeEnabled()) {
            logWarning("AI routing blocked - SafeMode active")
            return AiRouteResult.Failure("Safe mode active - AI routing disabled")
        }
        
        // Feature gate check
        if (!FeatureGate.areAIFeaturesEnabled()) {
            logWarning("AI routing blocked - feature not enabled for tier")
            return AiRouteResult.Failure("AI features not available for your subscription tier")
        }
        
        // API key check
        if (BuildConfig.GEMINI_API_KEY.isBlank()) {
            logWarning("AI routing blocked - Gemini API key not configured")
            return AiRouteResult.Failure("Gemini API key not configured")
        }
        
        // Validate request
        if (request.rawQuery.isBlank()) {
            return AiRouteResult.Failure("Empty query")
        }
        
        logInfo("Processing AI route request: ${request.rawQuery.take(50)}...")
        
        return try {
            // TODO: Implement real Gemini HTTP client call
            // For now, return stub response with parsed destination
            val stubSuggestion = parseStubRouteSuggestion(request)
            
            if (stubSuggestion != null) {
                logInfo("AI route suggestion generated: ${stubSuggestion.destinationName}")
                AiRouteResult.Success(stubSuggestion)
            } else {
                AiRouteResult.Failure("Could not parse navigation request")
            }
        } catch (e: Exception) {
            logError("AI routing failed", e)
            SafeModeManager.reportFailure("GeminiShim.getRouteSuggestion", e)
            AiRouteResult.Failure(e.message ?: "AI routing failed")
        }
    }
    
    /**
     * Stub implementation for route parsing.
     * TODO: Replace with actual Gemini API call.
     */
    private fun parseStubRouteSuggestion(request: AiRouteRequest): AiRouteSuggestion? {
        val query = request.rawQuery.lowercase()
        
        // Simple heuristic parsing (TODO: real Gemini call)
        val destination = when {
            query.contains("home") -> "Home"
            query.contains("work") -> "Work"
            query.contains("airport") -> "Airport"
            query.contains("hospital") -> "Hospital"
            else -> extractDestinationFromQuery(query)
        }
        
        if (destination.isBlank()) return null
        
        // Use current location as origin or default
        val origin = request.currentLocation ?: LatLng(33.4484, -112.0740) // Phoenix default
        
        // Generate stub destination coordinates (TODO: real geocoding)
        val destCoords = LatLng(
            origin.latitude + 0.05,
            origin.longitude + 0.05
        )
        
        return AiRouteSuggestion(
            origin = origin,
            destination = destCoords,
            waypoints = emptyList(),
            notes = "AI-generated route to $destination",
            mode = if (request.isTruck) AiRouteMode.TRUCK else AiRouteMode.CAR,
            destinationName = destination,
            estimatedDurationMinutes = 15
        )
    }
    
    /**
     * Extract destination from navigation query.
     */
    private fun extractDestinationFromQuery(query: String): String {
        val prefixes = listOf(
            "navigate to", "take me to", "go to", "route to",
            "directions to", "drive to", "find"
        )
        
        for (prefix in prefixes) {
            if (query.contains(prefix)) {
                return query.substringAfter(prefix).trim()
                    .replaceFirstChar { it.uppercase() }
            }
        }
        
        return query.trim().replaceFirstChar { it.uppercase() }
    }
    
    /**
     * Check if a query looks like a navigation request.
     */
    fun isNavigationQuery(query: String): Boolean {
        val lowerQuery = query.lowercase()
        val navigationKeywords = listOf(
            "navigate", "route", "take me", "go to", "directions",
            "drive to", "how do i get", "find", "where is"
        )
        return navigationKeywords.any { lowerQuery.contains(it) }
    }
}
