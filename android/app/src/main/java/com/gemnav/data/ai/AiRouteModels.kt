package com.gemnav.data.ai

import com.gemnav.core.subscription.Tier
import com.gemnav.data.route.LatLng

/**
 * Request to Gemini AI for route suggestions.
 */
data class AiRouteRequest(
    val rawQuery: String,
    val currentLocation: LatLng?,
    val destinationHint: String?,
    val tier: Tier,
    val isTruck: Boolean,
    val maxStops: Int = 8
)

/**
 * AI-generated route suggestion.
 */
data class AiRouteSuggestion(
    val origin: LatLng,
    val destination: LatLng,
    val waypoints: List<LatLng> = emptyList(),
    val notes: String? = null,
    val mode: AiRouteMode = AiRouteMode.CAR,
    val destinationName: String = "",
    val estimatedDurationMinutes: Int? = null
)

/**
 * Routing mode for AI suggestions.
 */
enum class AiRouteMode {
    CAR,
    TRUCK
}

/**
 * Result of AI route request.
 */
sealed class AiRouteResult {
    data class Success(val suggestion: AiRouteSuggestion) : AiRouteResult()
    data class Failure(val reason: String) : AiRouteResult()
}

/**
 * State for AI routing operations in ViewModels.
 */
sealed class AiRouteState {
    object Idle : AiRouteState()
    object Loading : AiRouteState()
    data class Success(val suggestion: AiRouteSuggestion) : AiRouteState()
    data class Error(val message: String) : AiRouteState()
}

/**
 * Voice-specific AI routing state.
 */
sealed class VoiceAiRouteState {
    object Idle : VoiceAiRouteState()
    object Listening : VoiceAiRouteState()
    object AiRouting : VoiceAiRouteState()
    data class Success(val suggestion: AiRouteSuggestion) : VoiceAiRouteState()
    data class Error(val message: String) : VoiceAiRouteState()
}

/**
 * State for AI intent classification and reasoning pipeline.
 * MP-020: Advanced AI Intent System
 */
sealed class AiIntentState {
    /** No intent processing active. */
    object Idle : AiIntentState()
    
    /** Classifying user input into NavigationIntent. */
    object Classifying : AiIntentState()
    
    /** Resolving intent into actionable route request. */
    data class Reasoning(
        val intent: NavigationIntent,
        val statusMessage: String = "Processing..."
    ) : AiIntentState()
    
    /** Generating route suggestion from resolved intent. */
    data class Suggesting(
        val intent: NavigationIntent,
        val statusMessage: String = "Finding route..."
    ) : AiIntentState()
    
    /** Intent successfully resolved to route. */
    data class Success(
        val intent: NavigationIntent,
        val suggestion: AiRouteSuggestion
    ) : AiIntentState()
    
    /** Intent processing failed. */
    data class Error(
        val message: String,
        val intent: NavigationIntent? = null
    ) : AiIntentState()
}
