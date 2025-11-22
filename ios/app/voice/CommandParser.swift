import Foundation

class CommandParser {
    private let geminiService: GeminiService
    private let tier: SubscriptionTier
    
    init(geminiService: GeminiService, tier: SubscriptionTier) {
        self.geminiService = geminiService
        self.tier = tier
    }
    
    // MARK: - Parse Command
    
    func parse(transcript: String, context: [ConversationTurn] = []) async -> VoiceCommand {
        // Free tier: Simple pattern matching for basic commands
        if tier == .free {
            return parseBasicCommand(transcript: transcript)
        }
        
        // Plus/Pro tier: Use Gemini Cloud for advanced parsing
        return await parseAdvancedCommand(transcript: transcript, context: context)
    }
    
    // MARK: - Basic Pattern Matching (Free Tier)
    
    private func parseBasicCommand(transcript: String) -> VoiceCommand {
        let normalized = transcript.lowercased().trimmingCharacters(in: .whitespaces)
        
        // Navigation control
        if normalized.contains("navigate to") || normalized.contains("take me to") {
            let destination = extractDestination(from: normalized)
            return destination.isEmpty ? .unknown : .navigate(destination: destination)
        }
        
        // Audio control
        if normalized == "mute" || normalized.contains("mute voice") {
            return .muteVoice
        }
        if normalized == "unmute" || normalized.contains("unmute voice") {
            return .unmuteVoice
        }
        if normalized.contains("repeat") {
            return .repeatInstruction
        }
        
        // Navigation commands
        if normalized.contains("cancel") {
            return .cancelNavigation
        }
        if normalized.contains("recenter") {
            return .recenterMap
        }
        
        // Information queries
        if normalized.contains("eta") || normalized.contains("when will i arrive") {
            return .getETA
        }
        if normalized.contains("how far") || normalized.contains("distance") {
            return .getDistanceRemaining
        }
        
        return .unknown
    }
    
    // MARK: - Advanced Gemini Parsing (Plus/Pro Tier)
    
    private func parseAdvancedCommand(transcript: String, context: [ConversationTurn]) async -> VoiceCommand {
        let prompt = buildGeminiPrompt(transcript: transcript, context: context)
        
        do {
            let response = try await geminiService.generateContent(prompt: prompt)
            return parseGeminiResponse(response: response)
        } catch {
            // Fallback to basic parsing if Gemini fails
            return parseBasicCommand(transcript: transcript)
        }
    }
    
    // MARK: - Build Gemini Prompt
    
    private func buildGeminiPrompt(transcript: String, context: [ConversationTurn]) -> String {
        let contextString: String
        if !context.isEmpty {
            contextString = context.suffix(5).map { "\($0.role): \($0.content)" }.joined(separator: "\n")
        } else {
            contextString = ""
        }
        
        let tierCapabilities: String
        switch tier {
        case .pro:
            tierCapabilities = "truck_features_enabled"
        case .plus:
            tierCapabilities = "advanced_features_enabled"
        case .free:
            tierCapabilities = "basic_features_only"
        }
        
        return """
        You are a voice command parser for GemNav navigation app.
        
        Tier: \(tierCapabilities)
        
        \(contextString.isEmpty ? "" : "Conversation history:\n\(contextString)\n")
        User said: "\(transcript)"
        
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
        """
    }
    
    // MARK: - Parse Gemini JSON Response
    
    private func parseGeminiResponse(response: String) -> VoiceCommand {
        guard let jsonData = response.trimmingCharacters(in: .whitespacesAndNewlines).data(using: .utf8),
              let json = try? JSONSerialization.jsonObject(with: jsonData) as? [String: Any] else {
            return .unknown
        }
        
        guard let intent = json["intent"] as? String,
              let entities = json["entities"] as? [String: Any],
              let confidence = json["confidence"] as? Double,
              let needsClarification = json["needs_clarification"] as? Bool else {
            return .unknown
        }
        
        // Handle low confidence or clarification needed
        if needsClarification || confidence < 0.6 {
            let question = json["clarification_question"] as? String ?? "Could you please rephrase that?"
            return .clarification(question: question)
        }
        
        // Parse based on intent
        switch intent {
        case "navigate":
            guard let destination = entities["destination"] as? String, !destination.isEmpty else {
                return .unknown
            }
            let waypoints = entities["waypoints"] as? [String] ?? []
            return .navigate(destination: destination, waypoints: waypoints)
            
        case "add_stop":
            guard let location = entities["destination"] as? String, !location.isEmpty else {
                return .unknown
            }
            return .addStop(location: location)
            
        case "search_along_route":
            let query = entities["query"] as? String ?? ""
            let filters = entities["filters"] as? [String: String] ?? [:]
            return .searchAlongRoute(query: query, filters: filters)
            
        case "modify_route":
            if let featureStr = entities["route_feature"] as? String,
               let feature = RouteFeature(rawValue: featureStr) {
                return .avoidRouteFeature(feature: feature)
            }
            if let optimizationStr = entities["optimization"] as? String,
               let criterion = OptimizationCriterion(rawValue: optimizationStr) {
                return .optimizeRoute(criterion: criterion)
            }
            return .showAlternativeRoutes
            
        case "query_info":
            let infoType = entities["info_type"] as? String
            switch infoType {
            case "eta": return .getETA
            case "distance": return .getDistanceRemaining
            case "traffic": return .getTrafficStatus
            default: return .unknown
            }
            
        case "audio_control":
            let action = entities["action"] as? String
            switch action {
            case "mute": return .muteVoice
            case "unmute": return .unmuteVoice
            case "repeat": return .repeatInstruction
            default: return .unknown
            }
            
        case "truck_poi":
            guard tier == .pro else { return .unknown }
            guard let poiTypeStr = entities["poi_type"] as? String,
                  let poiType = TruckPOIType(rawValue: poiTypeStr) else {
                return .unknown
            }
            return .findTruckPOI(type: poiType)
            
        default:
            return .unknown
        }
    }
    
    // MARK: - Extract Destination
    
    private func extractDestination(from transcript: String) -> String {
        let patterns = ["navigate to ", "take me to ", "go to ", "drive to "]
        
        for pattern in patterns {
            if let range = transcript.range(of: pattern) {
                let destination = String(transcript[range.upperBound...])
                return destination.trimmingCharacters(in: .whitespaces)
            }
        }
        
        return ""
    }
}
