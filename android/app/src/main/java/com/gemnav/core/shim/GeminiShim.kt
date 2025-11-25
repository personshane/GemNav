package com.gemnav.core.shim

import android.util.Log
import com.gemnav.app.BuildConfig
import com.gemnav.core.feature.FeatureGate
import com.gemnav.core.subscription.TierManager
import com.gemnav.data.ai.*
import com.gemnav.data.route.LatLng
import com.gemnav.core.places.PlacesApiClient
import com.gemnav.core.places.PlacesResult
import com.gemnav.core.places.PoiTypeMapper
import com.gemnav.core.navigation.RouteCorridor
import com.gemnav.core.navigation.SelectedPoi
import com.gemnav.core.navigation.AiVoiceEvent
import com.gemnav.core.navigation.TruckPoi
import com.gemnav.core.navigation.TruckPoiType
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import org.json.JSONObject
import org.json.JSONException

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
    suspend fun parseNavigationIntent(input: String): LegacyNavigationIntent? {
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
    
    // ==================== INTENT CLASSIFICATION (MP-020) ====================
    
    /**
     * Classify user input into a structured NavigationIntent.
     * This is the entry point for the AI intent system.
     * 
     * @param rawInput User's natural language query
     * @return IntentClassificationResult with parsed intent or failure
     */
    suspend fun classifyIntent(rawInput: String): IntentClassificationResult {
        val startTime = System.currentTimeMillis()
        
        // SafeMode check - return Unknown intent
        if (SafeModeManager.isSafeModeEnabled()) {
            logWarning("Intent classification blocked - SafeMode active")
            return IntentClassificationResult.Failure(
                reason = "Safe mode active - AI features disabled",
                rawText = rawInput
            )
        }
        
        // Feature gate check - Free tier blocked
        if (!FeatureGate.areAIFeaturesEnabled()) {
            logWarning("Intent classification blocked - AI not enabled for tier")
            return IntentClassificationResult.Failure(
                reason = "AI features not available for your subscription tier",
                rawText = rawInput
            )
        }
        
        if (rawInput.isBlank()) {
            return IntentClassificationResult.Failure(
                reason = "Empty input",
                rawText = rawInput
            )
        }
        
        logInfo("Classifying intent: ${rawInput.take(50)}...")
        
        return try {
            // TODO: Replace with actual Gemini API call
            // Build classification prompt
            val classificationPrompt = buildClassificationPrompt(rawInput)
            
            // TODO: Send to Gemini API
            // val response = geminiApiClient.generate(classificationPrompt)
            
            // Stub: Use heuristic classification until API is connected
            val intent = classifyIntentHeuristic(rawInput)
            val processingTime = System.currentTimeMillis() - startTime
            
            logInfo("Intent classified in ${processingTime}ms: ${intent::class.simpleName}")
            IntentClassificationResult.Success(intent, processingTime)
            
        } catch (e: Exception) {
            logError("Intent classification failed", e)
            SafeModeManager.reportFailure("GeminiShim.classifyIntent", e)
            IntentClassificationResult.Failure(
                reason = e.message ?: "Classification failed",
                rawText = rawInput
            )
        }
    }
    
    /**
     * Resolve a classified intent into an actionable AiRouteRequest.
     * 
     * @param intent The classified NavigationIntent
     * @param currentLocation User's current location (if available)
     * @return IntentResolutionResult with route request or status
     */
    suspend fun resolveIntent(
        intent: NavigationIntent,
        currentLocation: LatLng? = null
    ): IntentResolutionResult {
        // SafeMode check
        if (SafeModeManager.isSafeModeEnabled()) {
            logWarning("Intent resolution blocked - SafeMode active")
            return IntentResolutionResult.Failure("Safe mode active - AI features disabled")
        }
        
        // Feature gate check
        if (!FeatureGate.areAIFeaturesEnabled()) {
            logWarning("Intent resolution blocked - AI not enabled for tier")
            return IntentResolutionResult.Failure("AI features not available for your subscription tier")
        }
        
        logInfo("Resolving intent: ${intent::class.simpleName}")
        
        return when (intent) {
            is NavigationIntent.NavigateTo -> resolveNavigateTo(intent, currentLocation)
            is NavigationIntent.FindPOI -> resolveFindPOI(intent, currentLocation)
            is NavigationIntent.AddStop -> resolveAddStop(intent, currentLocation)
            is NavigationIntent.RoutePreferences -> resolveRoutePreferences(intent, currentLocation)
            is NavigationIntent.Question -> IntentResolutionResult.NotSupported(
                "Questions are not yet supported. Try a navigation command instead."
            )
            is NavigationIntent.Unknown -> IntentResolutionResult.Failure(
                intent.reason
            )
        }
    }
    
    /**
     * Build the classification prompt for Gemini.
     */
    private fun buildClassificationPrompt(userInput: String): String {
        return """
            |You are a navigation intent classifier. Analyze the user's query and output ONLY a JSON object.
            |
            |User query: "$userInput"
            |
            |Classify into one of these types:
            |1. "navigate_to" - Direct navigation (e.g., "Take me to Phoenix")
            |2. "find_poi" - Find POI (e.g., "Find truck stop with showers")
            |3. "add_stop" - Add waypoint (e.g., "Add Walmart on the way")
            |4. "route_preferences" - Change settings (e.g., "Avoid tolls")
            |5. "question" - Information query (e.g., "How far to Denver?")
            |6. "unknown" - Cannot classify
            |
            |POI types: truck_stop, gas_station, diesel, rest_area, hotel, motel, 
            |restaurant, fast_food, parking, truck_parking, walmart, grocery, 
            |weigh_station, repair_shop, hospital, pharmacy, atm, other
            |
            |Output JSON format:
            |{
            |  "type": "navigate_to|find_poi|add_stop|route_preferences|question|unknown",
            |  "destination": "string or null",
            |  "poi_type": "string or null",
            |  "filters": { "has_showers": bool, "has_truck_parking": bool, etc },
            |  "settings": { "avoid_tolls": bool, "avoid_highways": bool, etc },
            |  "near_location": "string or null",
            |  "confidence": 0.0-1.0
            |}
            |
            |Output ONLY valid JSON, no explanation.
        """.trimMargin()
    }
    
    /**
     * Heuristic intent classification (stub until Gemini API connected).
     * TODO: Replace with parsed Gemini response
     */
    private fun classifyIntentHeuristic(input: String): NavigationIntent {
        val lowerInput = input.lowercase().trim()
        
        // Check for POI search patterns
        val poiResult = checkForPOIIntent(lowerInput)
        if (poiResult != null) return poiResult
        
        // Check for route preferences
        val prefsResult = checkForPreferencesIntent(lowerInput)
        if (prefsResult != null) return prefsResult
        
        // Check for add stop
        val stopResult = checkForAddStopIntent(lowerInput)
        if (stopResult != null) return stopResult
        
        // Check for questions
        val questionResult = checkForQuestionIntent(lowerInput)
        if (questionResult != null) return questionResult
        
        // Check for direct navigation
        val navResult = checkForNavigateIntent(lowerInput)
        if (navResult != null) return navResult
        
        // Default: try to extract as navigation
        return NavigationIntent.NavigateTo(
            destinationText = input.trim(),
            confidence = 0.3f
        )
    }
    
    private fun checkForPOIIntent(input: String): NavigationIntent.FindPOI? {
        val poiKeywords = mapOf(
            POIType.TRUCK_STOP to listOf("truck stop", "truckstop", "flying j", "pilot", "loves"),
            POIType.DIESEL to listOf("diesel", "fuel"),
            POIType.GAS_STATION to listOf("gas station", "gas", "fill up"),
            POIType.REST_AREA to listOf("rest area", "rest stop"),
            POIType.HOTEL to listOf("hotel", "inn", "suites"),
            POIType.MOTEL to listOf("motel"),
            POIType.RESTAURANT to listOf("restaurant", "food", "eat"),
            POIType.FAST_FOOD to listOf("fast food", "drive thru", "drive through"),
            POIType.PARKING to listOf("parking"),
            POIType.TRUCK_PARKING to listOf("truck parking", "overnight parking"),
            POIType.WALMART to listOf("walmart", "wal-mart"),
            POIType.GROCERY to listOf("grocery", "supermarket"),
            POIType.REPAIR_SHOP to listOf("mechanic", "repair", "tire shop"),
            POIType.HOSPITAL to listOf("hospital", "emergency room", "er"),
            POIType.PHARMACY to listOf("pharmacy", "drugstore")
        )
        
        // Check for "find" or "nearest" patterns
        val findPatterns = listOf("find", "nearest", "closest", "where is", "looking for")
        val hasFindPattern = findPatterns.any { input.contains(it) }
        
        for ((poiType, keywords) in poiKeywords) {
            if (keywords.any { input.contains(it) }) {
                val filters = extractPOIFilters(input)
                val nearLocation = extractNearLocation(input)
                
                return NavigationIntent.FindPOI(
                    poiType = poiType,
                    filters = filters,
                    nearLocation = nearLocation,
                    confidence = if (hasFindPattern) 0.85f else 0.6f
                )
            }
        }
        
        return null
    }
    
    private fun extractPOIFilters(input: String): POIFilters {
        return POIFilters(
            hasShowers = input.contains("shower"),
            hasOvernightParking = input.contains("overnight") || input.contains("sleep"),
            hasTruckParking = input.contains("truck parking"),
            hasDiesel = input.contains("diesel"),
            isOpen24Hours = input.contains("24 hour") || input.contains("open now")
        )
    }
    
    private fun extractNearLocation(input: String): String? {
        val nearPatterns = listOf("near ", "around ", "by ", "close to ", "in ")
        for (pattern in nearPatterns) {
            val idx = input.indexOf(pattern)
            if (idx >= 0) {
                val afterPattern = input.substring(idx + pattern.length)
                // Extract until end or next keyword
                val endIdx = afterPattern.indexOfAny(charArrayOf(',', '.', '?', '!'))
                return if (endIdx > 0) afterPattern.substring(0, endIdx).trim()
                       else afterPattern.trim()
            }
        }
        return null
    }
    
    private fun checkForPreferencesIntent(input: String): NavigationIntent.RoutePreferences? {
        val settings = RouteSettings(
            avoidTolls = input.contains("avoid toll") || input.contains("no toll"),
            avoidHighways = input.contains("avoid highway") || input.contains("no highway"),
            avoidMountains = input.contains("avoid mountain") || input.contains("no mountain"),
            avoidSnow = input.contains("avoid snow") || input.contains("no snow"),
            preferFastest = input.contains("fastest"),
            preferShortest = input.contains("shortest"),
            preferScenic = input.contains("scenic")
        )
        
        // Check if any preference was set
        val hasPreference = listOf(
            settings.avoidTolls, settings.avoidHighways, settings.avoidMountains,
            settings.avoidSnow, settings.preferFastest, settings.preferShortest,
            settings.preferScenic
        ).any { it == true }
        
        return if (hasPreference) {
            NavigationIntent.RoutePreferences(settings = settings, confidence = 0.8f)
        } else null
    }
    
    private fun checkForAddStopIntent(input: String): NavigationIntent.AddStop? {
        val addPatterns = listOf("add a stop", "add stop", "stop at", "add ", "on the way")
        
        for (pattern in addPatterns) {
            if (input.contains(pattern)) {
                val afterPattern = input.substringAfter(pattern).trim()
                
                // Check if it's a POI type
                val poiType = detectPOIType(afterPattern)
                
                return NavigationIntent.AddStop(
                    stopType = if (poiType != null) StopType.POI_TYPE else StopType.SPECIFIC_DESTINATION,
                    destinationText = if (poiType == null) afterPattern else null,
                    poiType = poiType,
                    confidence = 0.75f
                )
            }
        }
        
        return null
    }
    
    private fun detectPOIType(input: String): POIType? {
        val lower = input.lowercase()
        return when {
            lower.contains("truck stop") -> POIType.TRUCK_STOP
            lower.contains("gas") || lower.contains("fuel") -> POIType.GAS_STATION
            lower.contains("walmart") -> POIType.WALMART
            lower.contains("rest area") -> POIType.REST_AREA
            lower.contains("food") || lower.contains("restaurant") -> POIType.RESTAURANT
            else -> null
        }
    }
    
    private fun checkForQuestionIntent(input: String): NavigationIntent.Question? {
        val questionPatterns = listOf("how far", "how long", "what time", "when will", "what's traffic", "is there")
        
        for (pattern in questionPatterns) {
            if (input.contains(pattern)) {
                return NavigationIntent.Question(query = input, confidence = 0.7f)
            }
        }
        
        return null
    }
    
    private fun checkForNavigateIntent(input: String): NavigationIntent.NavigateTo? {
        val navPrefixes = listOf(
            "navigate to", "take me to", "go to", "route to",
            "directions to", "drive to", "head to"
        )
        
        for (prefix in navPrefixes) {
            if (input.contains(prefix)) {
                val destination = input.substringAfter(prefix).trim()
                    .replaceFirstChar { it.uppercase() }
                return NavigationIntent.NavigateTo(
                    destinationText = destination,
                    confidence = 0.85f
                )
            }
        }
        
        return null
    }
    
    // ==================== INTENT RESOLUTION ====================
    
    private suspend fun resolveNavigateTo(
        intent: NavigationIntent.NavigateTo,
        currentLocation: LatLng?
    ): IntentResolutionResult {
        val origin = currentLocation ?: LatLng(33.4484, -112.0740) // Phoenix default
        
        // TODO: Geocode destination to coordinates
        // For now, generate stub coordinates
        val destCoords = intent.destinationCoords ?: LatLng(
            origin.latitude + 0.05,
            origin.longitude + 0.05
        )
        
        val isTruck = TierManager.isPro() // Pro tier can use truck mode
        
        val request = AiRouteRequest(
            rawQuery = "Navigate to ${intent.destinationText}",
            currentLocation = origin,
            destinationHint = intent.destinationText,
            tier = TierManager.getCurrentTier(),
            isTruck = isTruck,
            maxStops = FeatureGate.getMaxWaypoints()
        )
        
        return IntentResolutionResult.RouteRequest(
            request = request,
            explanation = "Routing to ${intent.destinationText}"
        )
    }
    
    private suspend fun resolveFindPOI(
        intent: NavigationIntent.FindPOI,
        currentLocation: LatLng?
    ): IntentResolutionResult {
        val origin = currentLocation ?: LatLng(33.4484, -112.0740) // Phoenix default
        
        // MP-021: Proper tier enforcement for Places API
        // FREE: Uses AI but cannot use Places API
        if (TierManager.isFree()) {
            logWarning("FindPOI blocked - Free tier cannot use Places API")
            return IntentResolutionResult.Failure(
                "POI search requires Plus subscription. Upgrade to search for places like truck stops, restaurants, and more."
            )
        }
        
        // PRO: Uses HERE SDK for truck POIs - MP-025
        if (TierManager.isPro()) {
            logInfo("FindPOI redirecting to truck POI search for Pro tier")
            return resolveFindTruckPOI(intent, currentLocation)
        }
        
        // PLUS: Use Google Places REST API
        // MP-022: Along-route POI search support
        val poiDescription = PoiTypeMapper.getPoiDescription(intent.poiType)
        val rawQuery = intent.nearLocation ?: ""
        val isAlongRoute = RouteCorridor.containsAlongRouteKeywords(rawQuery)
        val isNearMe = rawQuery.lowercase().let { 
            it.contains("near me") || it.contains("nearby") 
        }
        
        // Along-route search uses wider radius (50km) to find POIs ahead
        val searchRadius = if (isAlongRoute) {
            50_000 // 50km for along-route
        } else {
            PlacesApiClient.getSearchRadius(isNearMe)
        }
        
        logInfo("Plus tier FindPOI: ${intent.poiType} radius=${searchRadius}m filters=${intent.filters} alongRoute=$isAlongRoute")
        
        val placesResult = PlacesApiClient.searchNearby(
            location = origin,
            radiusMeters = searchRadius,
            poiType = intent.poiType,
            filters = intent.filters
        )
        
        return when (placesResult) {
            is PlacesResult.Success -> {
                if (placesResult.places.isEmpty()) {
                    IntentResolutionResult.Failure(
                        "No $poiDescription found nearby. Try a different location or remove filters."
                    )
                } else {
                    // MP-022: Apply along-route filtering if requested
                    val filteredPlaces = if (isAlongRoute) {
                        val routePolyline = RouteDetailsViewModelProvider.getActiveRoutePolyline()
                        if (routePolyline.size >= 2) {
                            val alongRoutePlaces = RouteCorridor.filterPlacesAlongRoute(
                                places = placesResult.places,
                                routePolyline = routePolyline,
                                toleranceMeters = RouteCorridor.DEFAULT_CORRIDOR_METERS
                            )
                            if (alongRoutePlaces.isEmpty()) {
                                logWarning("No POIs found along route, falling back to all results")
                                placesResult.places
                            } else {
                                logInfo("Found ${alongRoutePlaces.size} POIs along route")
                                alongRoutePlaces
                            }
                        } else {
                            logWarning("No active route for along-route search, using all results")
                            placesResult.places
                        }
                    } else {
                        placesResult.places
                    }
                    
                    // Pick best match (first result after filtering)
                    val bestMatch = filteredPlaces.first()
                    logInfo("Found POI: ${bestMatch.name} at ${bestMatch.lat},${bestMatch.lng}")
                    
                    // MP-023: Trigger detour calculation for along-route POIs
                    if (isAlongRoute) {
                        val selectedPoi = SelectedPoi.fromPlaceResult(bestMatch)
                        RouteDetailsViewModelProvider.selectPoiForDetour(selectedPoi)
                        logInfo("Triggered detour calculation for ${bestMatch.name}")
                    }
                    
                    val request = AiRouteRequest(
                        rawQuery = "Navigate to ${bestMatch.name}",
                        currentLocation = origin,
                        destinationHint = bestMatch.name,
                        tier = TierManager.getCurrentTier(),
                        isTruck = false, // Plus tier = car only
                        maxStops = FeatureGate.getMaxWaypoints()
                    )
                    
                    val explanation = if (isAlongRoute && filteredPlaces.size < placesResult.places.size) {
                        "Found ${bestMatch.name} along your route${bestMatch.address?.let { " at $it" } ?: ""}"
                    } else {
                        "Found ${bestMatch.name}${bestMatch.address?.let { " at $it" } ?: ""}"
                    }
                    
                    IntentResolutionResult.RouteRequest(
                        request = request,
                        explanation = explanation
                    )
                }
            }
            is PlacesResult.Failure -> {
                logWarning("Places search failed: ${placesResult.reason}")
                IntentResolutionResult.Failure(placesResult.reason)
            }
        }
    }
    
    private suspend fun resolveAddStop(
        intent: NavigationIntent.AddStop,
        currentLocation: LatLng?
    ): IntentResolutionResult {
        val origin = currentLocation ?: LatLng(33.4484, -112.0740)
        
        // TODO: Get existing route and merge new waypoint
        val stopDescription = intent.destinationText 
            ?: intent.poiType?.name?.lowercase()?.replace("_", " ")
            ?: "stop"
        
        logInfo("TODO: Merge stop into existing route: $stopDescription")
        
        val request = AiRouteRequest(
            rawQuery = "Add stop at $stopDescription",
            currentLocation = origin,
            destinationHint = stopDescription,
            tier = TierManager.getCurrentTier(),
            isTruck = TierManager.isPro(),
            maxStops = FeatureGate.getMaxWaypoints()
        )
        
        return IntentResolutionResult.RouteRequest(
            request = request,
            explanation = "Adding $stopDescription to route"
        )
    }
    
    private suspend fun resolveRoutePreferences(
        intent: NavigationIntent.RoutePreferences,
        currentLocation: LatLng?
    ): IntentResolutionResult {
        val origin = currentLocation ?: LatLng(33.4484, -112.0740)
        
        // TODO: Apply preferences to current route and recalculate
        val settings = intent.settings
        val prefsDescription = buildList {
            if (settings.avoidTolls == true) add("avoid tolls")
            if (settings.avoidHighways == true) add("avoid highways")
            if (settings.avoidMountains == true) add("avoid mountains")
            if (settings.preferFastest == true) add("fastest route")
        }.joinToString(", ")
        
        logInfo("TODO: Apply route preferences: $prefsDescription")
        
        val request = AiRouteRequest(
            rawQuery = "Update route: $prefsDescription",
            currentLocation = origin,
            destinationHint = null,
            tier = TierManager.getCurrentTier(),
            isTruck = settings.truckMode ?: TierManager.isPro(),
            maxStops = FeatureGate.getMaxWaypoints()
        )
        
        return IntentResolutionResult.RouteRequest(
            request = request,
            explanation = "Applying preferences: $prefsDescription"
        )
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
     * Legacy navigation intent (for backward compatibility).
     * @deprecated Use NavigationIntent sealed class from IntentModel.kt
     */
    @Deprecated("Use NavigationIntent sealed class from IntentModel.kt")
    data class LegacyNavigationIntent(
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
