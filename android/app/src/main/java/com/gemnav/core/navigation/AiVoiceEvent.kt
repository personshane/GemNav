package com.gemnav.core.navigation

/**
 * MP-024: Voice event types for POI and detour feedback.
 * 
 * These events are emitted by RouteDetailsViewModel and handled
 * by VoiceFeedbackManager to provide spoken feedback to users.
 */
sealed class AiVoiceEvent {
    
    /**
     * Spoken when detour calculation completes successfully.
     * Example: "Detour found to Pilot Travel Center. About 5 minutes extra and 2 miles added to your trip."
     */
    data class DetourSummary(
        val poiName: String,
        val addedMinutes: Int?,
        val addedMiles: Double?,
        val distanceOffRouteMiles: Double? = null
    ) : AiVoiceEvent()
    
    /**
     * Spoken when a feature requires a higher subscription tier.
     * Example: "This feature requires a Plus subscription to use detour calculation."
     */
    data class UpgradeRequired(
        val requiredTierName: String,
        val featureName: String
    ) : AiVoiceEvent()
    
    /**
     * Spoken when a POI is successfully added as a stop.
     * Example: "Added Pilot Travel Center as a stop on your route."
     */
    data class StopAdded(
        val poiName: String
    ) : AiVoiceEvent()
    
    /**
     * Spoken when POI search finds results along the route.
     * Example: "Found 3 gas stations along your route. The closest is Shell, 2 miles ahead."
     */
    data class PoiFound(
        val poiName: String,
        val poiType: String,
        val distanceAheadMiles: Double? = null,
        val totalResults: Int = 1
    ) : AiVoiceEvent()
    
    /**
     * Spoken when no POIs are found matching the search.
     * Example: "No truck stops found along your route."
     */
    data class NoPoisFound(
        val poiType: String
    ) : AiVoiceEvent()
    
    /**
     * Spoken on generic errors during POI or detour operations.
     * Example: "Sorry, I couldn't calculate a detour right now."
     */
    object GenericError : AiVoiceEvent()
}
