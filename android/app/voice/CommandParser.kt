package com.gemnav.app.voice

import com.gemnav.app.ai.GeminiService
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Parses natural language transcripts into VoiceCommand objects using Gemini AI
 */
class CommandParser @Inject constructor(
    private val geminiService: GeminiService,
    private val tier: SubscriptionTier
) {
    /**
     * Parse voice transcript into structured command
     */
    suspend fun parse(
        transcript: String,
        context: List<ConversationTurn> = emptyList()
    ): VoiceCommand = withContext(Dispatchers.IO) {
        
        // Free tier: Simple pattern matching for basic commands
        if (tier == SubscriptionTier.FREE) {
            return@withContext parseBasicCommand(transcript)
        }
        
        // Plus/Pro tier: Use Gemini Cloud for advanced parsing
        return@withContext parseAdvancedCommand(transcript, context)
    }
    
    /**
     * Basic pattern matching for Free tier
     */
    private fun parseBasicCommand(transcript: String): VoiceCommand {
        val normalized = transcript.lowercase().trim()
        
        return when {
            // Navigation control
            normalized.contains("navigate to") || normalized.contains("take me to") -> {
                val destination = extractDestination(normalized)
                if (destination.isNotEmpty()) {
                    VoiceCommand.Navigate(destination)
                } else {
                    VoiceCommand.Unknown
                }
            }
            
            // Audio control
            normalized == "mute" || normalized.contains("mute voice") -> VoiceCommand.MuteVoice
            normalized == "unmute" || normalized.contains("unmute voice") -> VoiceCommand.UnmuteVoice
            normalized.contains("repeat") -> VoiceCommand.RepeatInstruction
            
            // Navigation commands
            normalized.contains("cancel") -> VoiceCommand.CancelNavigation
            normalized.contains("recenter") -> VoiceCommand.RecenterMap
            
            // Information queries
            normalized.contains("eta") || normalized.contains("when will i arrive") -> VoiceCommand.GetETA
            normalized.contains("how far") || normalized.contains("distance") -> VoiceCommand.GetDistanceRemaining
            
            else -> VoiceCommand.Unknown
        }
    }
    
    /**
     * Advanced Gemini-based parsing for Plus/Pro tiers
     */
    private suspend fun parseAdvancedCommand(
        transcript: String,
        context: List<ConversationTurn>
    ): VoiceCommand {
        val prompt = buildGeminiPrompt(transcript, context)
        
        try {
            val response = geminiService.generateContent(prompt)
            return parseGeminiResponse(response)
        } catch (e: Exception) {
            // Fallback to basic parsing if Gemini fails
            return parseBasicCommand(transcript)
        }
    }
    
    /**
     * Build Gemini prompt for command parsing
     */
    private fun buildGeminiPrompt(
        transcript: String,
        context: List<ConversationTurn>
    ): String {
        val contextString = if (context.isNotEmpty()) {
            context.takeLast(5).joinToString("\n") { turn ->
                "${turn.role}: ${turn.content}"
            }
        } else {
            ""
        }
        
        val tierCapabilities = when (tier) {
            SubscriptionTier.PRO -> "truck_features_enabled"
            SubscriptionTier.PLUS -> "advanced_features_enabled"
            else -> "basic_features_only"
        }
        
        return """
You are a voice command parser for GemNav navigation app.

Tier: $tierCapabilities

${if (contextString.isNotEmpty()) "Conversation history:\n$contextString\n" else ""}
User said: "$transcript"

Extract structured command as JSON:
{
  "intent": "navigate|add_stop|search_along_route|modify_route|query_info|audio_control|truck_poi|unknown",
  "entities": {
    "destination": "string or null",
    "waypoints": ["list of waypoints"] or null,
    "query": "search query" or null,
    "filters": {"filter_key": "filter_value"} or null,
    "route_feature": "tolls|highways|ferries|unpaved" or null,
    "optimization": "fastest|shortest|eco|avoid_traffic" or null,
    "poi_type": "truck_stop|weigh_station|rest_area|diesel|truck_wash|repair" or null
  },
  "confidence": 0.0-1.0,
  "needs_clarification": true|false,
  "clarification_question": "string or null"
}

Rules:
- For navigation: extract clear destination name
- For searches: extract query and any filters (price, type, rating, etc.)
- For route mods: identify what to avoid or optimize for
- For truck commands (PRO only): extract POI type or compliance check
- If ambiguous or confidence < 0.6: set needs_clarification=true
- Return ONLY valid JSON, no markdown or explanation

JSON response:
""".trimIndent()
    }
    
    /**
     * Parse Gemini JSON response into VoiceCommand
     */
    private fun parseGeminiResponse(response: String): VoiceCommand {
        return try {
            val json = JSONObject(response.trim())
            val intent = json.getString("intent")
            val entities = json.getJSONObject("entities")
            val confidence = json.getDouble("confidence")
            val needsClarification = json.getBoolean("needs_clarification")
            
            // Handle low confidence or clarification needed
            if (needsClarification || confidence < 0.6) {
                val question = json.optString("clarification_question", "Could you please rephrase that?")
                return VoiceCommand.Clarification(question)
            }
            
            // Parse based on intent
            when (intent) {
                "navigate" -> {
                    val destination = entities.optString("destination")
                    if (destination.isNotEmpty()) {
                        val waypointsArray = entities.optJSONArray("waypoints")
                        val waypoints = waypointsArray?.let { arr ->
                            (0 until arr.length()).map { arr.getString(it) }
                        } ?: emptyList()
                        VoiceCommand.Navigate(destination, waypoints)
                    } else {
                        VoiceCommand.Unknown
                    }
                }
                
                "add_stop" -> {
                    val location = entities.optString("destination")
                    if (location.isNotEmpty()) {
                        VoiceCommand.AddStop(location)
                    } else {
                        VoiceCommand.Unknown
                    }
                }
                
                "search_along_route" -> {
                    val query = entities.optString("query")
                    val filtersObj = entities.optJSONObject("filters")
                    val filters = filtersObj?.let { obj ->
                        obj.keys().asSequence().associateWith { obj.getString(it) }
                    } ?: emptyMap()
                    VoiceCommand.SearchAlongRoute(query, filters)
                }
                
                "modify_route" -> {
                    val feature = entities.optString("route_feature")
                    val optimization = entities.optString("optimization")
                    
                    when {
                        feature.isNotEmpty() -> {
                            val routeFeature = when (feature) {
                                "tolls" -> RouteFeature.TOLLS
                                "highways" -> RouteFeature.HIGHWAYS
                                "ferries" -> RouteFeature.FERRIES
                                "unpaved" -> RouteFeature.UNPAVED_ROADS
                                else -> return VoiceCommand.Unknown
                            }
                            VoiceCommand.AvoidRouteFeature(routeFeature)
                        }
                        optimization.isNotEmpty() -> {
                            val criterion = when (optimization) {
                                "fastest" -> OptimizationCriterion.FASTEST
                                "shortest" -> OptimizationCriterion.SHORTEST
                                "eco" -> OptimizationCriterion.ECO_FRIENDLY
                                "avoid_traffic" -> OptimizationCriterion.AVOID_TRAFFIC
                                else -> return VoiceCommand.Unknown
                            }
                            VoiceCommand.OptimizeRoute(criterion)
                        }
                        else -> VoiceCommand.ShowAlternativeRoutes
                    }
                }
                
                "query_info" -> {
                    when (entities.optString("info_type")) {
                        "eta" -> VoiceCommand.GetETA
                        "distance" -> VoiceCommand.GetDistanceRemaining
                        "traffic" -> VoiceCommand.GetTrafficStatus
                        else -> VoiceCommand.Unknown
                    }
                }
                
                "audio_control" -> {
                    when (entities.optString("action")) {
                        "mute" -> VoiceCommand.MuteVoice
                        "unmute" -> VoiceCommand.UnmuteVoice
                        "repeat" -> VoiceCommand.RepeatInstruction
                        else -> VoiceCommand.Unknown
                    }
                }
                
                "truck_poi" -> {
                    if (tier != SubscriptionTier.PRO) {
                        return VoiceCommand.Unknown
                    }
                    
                    val poiType = when (entities.optString("poi_type")) {
                        "truck_stop" -> TruckPOIType.TRUCK_STOP
                        "weigh_station" -> TruckPOIType.WEIGH_STATION
                        "rest_area" -> TruckPOIType.REST_AREA
                        "diesel" -> TruckPOIType.DIESEL_STATION
                        "truck_wash" -> TruckPOIType.TRUCK_WASH
                        "repair" -> TruckPOIType.REPAIR_SHOP
                        else -> return VoiceCommand.Unknown
                    }
                    VoiceCommand.FindTruckPOI(poiType)
                }
                
                else -> VoiceCommand.Unknown
            }
        } catch (e: Exception) {
            VoiceCommand.Unknown
        }
    }
    
    /**
     * Extract destination from basic navigation command
     */
    private fun extractDestination(transcript: String): String {
        val patterns = listOf(
            "navigate to ",
            "take me to ",
            "go to ",
            "drive to "
        )
        
        for (pattern in patterns) {
            if (transcript.contains(pattern)) {
                return transcript.substringAfter(pattern).trim()
            }
        }
        
        return ""
    }
}
