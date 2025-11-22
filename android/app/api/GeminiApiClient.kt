package com.gemnav.app.api

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.Flow

/**
 * Client for Gemini AI integration (both on-device Nano and Cloud API).
 * 
 * TIER IMPLEMENTATION:
 * - Free tier: Gemini Nano (on-device, privacy-first, no API key needed)
 * - Plus tier: Gemini Cloud API (enhanced reasoning, requires API key)
 * - Pro tier: Gemini Cloud API (same as Plus, enhanced for commercial use)
 * 
 * Features:
 * - Natural language destination parsing
 * - Route preference extraction
 * - Conversational interface
 * - Multi-turn conversation support
 * - Tier-aware model selection
 */
class GeminiApiClient(
    private val context: Context,
    private val apiKey: String? = null,
    private val tier: UserTier
) {
    
    private val model: GenerativeModel by lazy {
        when (tier) {
            UserTier.FREE -> {
                // Gemini Nano on-device model
                // Note: Actual implementation requires Gemini Nano SDK
                // This is a placeholder structure
                throw NotImplementedError(
                    "Gemini Nano on-device implementation pending SDK availability"
                )
            }
            UserTier.PLUS, UserTier.PRO -> {
                // Gemini Cloud API
                requireNotNull(apiKey) { "API key required for Plus/Pro tier" }
                GenerativeModel(
                    modelName = "gemini-1.5-flash", // Fast model for navigation
                    apiKey = apiKey,
                    systemInstruction = content {
                        text(NAVIGATION_SYSTEM_PROMPT)
                    }
                )
            }
        }
    }
    
    /**
     * Parse natural language input into structured navigation request.
     * 
     * @param input User's natural language input (e.g., "Navigate to nearest Starbucks")
     * @param currentLocation Optional current location for context
     * @return Parsed navigation request
     */
    suspend fun parseNavigationInput(
        input: String,
        currentLocation: String? = null
    ): NavigationRequest {
        
        val prompt = buildString {
            append("Parse this navigation request into structured data:\n")
            append("Input: \"$input\"\n")
            if (currentLocation != null) {
                append("Current location: $currentLocation\n")
            }
            append("\nRespond with JSON containing:\n")
            append("- destination: string (place name or address)\n")
            append("- searchQuery: string (for Places API)\n")
            append("- preferences: object (avoid tolls, fastest route, etc.)\n")
            append("- waypoints: array (if multi-stop request)\n")
            append("- intent: string (navigate, search, plan)\n")
        }
        
        val response = model.generateContent(prompt)
        return parseNavigationResponse(response.text ?: "")
    }
    
    /**
     * Generate conversational response for navigation assistance.
     * 
     * @param userMessage User's message
     * @param conversationHistory Previous conversation turns
     * @return AI-generated response
     */
    suspend fun chat(
        userMessage: String,
        conversationHistory: List<ChatMessage> = emptyList()
    ): String {
        
        val chat = model.startChat(
            history = conversationHistory.map { msg ->
                content(msg.role) { text(msg.content) }
            }
        )
        
        val response = chat.sendMessage(userMessage)
        return response.text ?: ""
    }
    
    /**
     * Streaming chat for real-time responses.
     * 
     * @param userMessage User's message
     * @param conversationHistory Previous conversation turns
     * @return Flow of response chunks
     */
    fun chatStream(
        userMessage: String,
        conversationHistory: List<ChatMessage> = emptyList()
    ): Flow<GenerateContentResponse> {
        
        val chat = model.startChat(
            history = conversationHistory.map { msg ->
                content(msg.role) { text(msg.content) }
            }
        )
        
        return chat.sendMessageStream(userMessage)
    }
    
    /**
     * Explain route differences and recommendations.
     * 
     * @param routes List of route options with details
     * @param userPreferences User's stated preferences
     * @return AI-generated explanation and recommendation
     */
    suspend fun explainRoutes(
        routes: List<RouteOption>,
        userPreferences: String? = null
    ): String {
        
        val prompt = buildString {
            append("Explain these route options to the user:\n\n")
            routes.forEachIndexed { index, route ->
                append("Route ${index + 1}:\n")
                append("- Duration: ${route.durationMinutes} minutes\n")
                append("- Distance: ${route.distanceMiles} miles\n")
                append("- Description: ${route.description}\n")
                if (route.hasTolls) append("- Has tolls\n")
                if (route.hasTraffic) append("- Current traffic delays\n")
                append("\n")
            }
            if (userPreferences != null) {
                append("User preferences: $userPreferences\n")
            }
            append("\nProvide a brief (2-3 sentence) recommendation.")
        }
        
        val response = model.generateContent(prompt)
        return response.text ?: ""
    }
    
    /**
     * Parse JSON response into NavigationRequest.
     * Basic implementation - enhance with proper JSON parsing.
     */
    private fun parseNavigationResponse(jsonText: String): NavigationRequest {
        // TODO: Implement proper JSON parsing with Gson/Moshi/kotlinx.serialization
        return NavigationRequest(
            destination = "",
            searchQuery = "",
            intent = "navigate"
        )
    }
    
    companion object {
        private const val NAVIGATION_SYSTEM_PROMPT = """
You are GemNav's navigation assistant. Help users with:
1. Parsing natural language into navigation requests
2. Explaining route options clearly
3. Answering navigation-related questions
4. Multi-stop trip planning

Be concise, friendly, and safety-focused. Always prioritize user safety.
For destination parsing, extract the key location and search terms.
For route explanations, be brief but informative.
        """
    }
}

/**
 * User tier enum.
 */
enum class UserTier {
    FREE,
    PLUS,
    PRO
}

/**
 * Parsed navigation request.
 */
data class NavigationRequest(
    val destination: String,
    val searchQuery: String,
    val intent: String,
    val waypoints: List<String> = emptyList(),
    val preferences: Map<String, Any> = emptyMap()
)

/**
 * Chat message for conversation history.
 */
data class ChatMessage(
    val role: String, // "user" or "model"
    val content: String
)

/**
 * Route option for AI explanation.
 */
data class RouteOption(
    val durationMinutes: Int,
    val distanceMiles: Double,
    val description: String,
    val hasTolls: Boolean = false,
    val hasTraffic: Boolean = false
)
