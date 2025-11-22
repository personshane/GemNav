import Foundation

// MARK: - Voice Commands

enum VoiceCommand {
    // Navigation commands
    case navigate(destination: String, waypoints: [String] = [])
    case addStop(location: String)
    case cancelNavigation
    case recenterMap
    
    // Audio commands
    case muteVoice
    case unmuteVoice
    case repeatInstruction
    
    // Information queries
    case getETA
    case getDistanceRemaining
    case getTrafficStatus
    case searchAlongRoute(query: String, filters: [String: String] = [:])
    
    // Route modifications
    case avoidRouteFeature(feature: RouteFeature)
    case showAlternativeRoutes
    case optimizeRoute(criterion: OptimizationCriterion)
    
    // Truck-specific (Pro tier)
    case findTruckPOI(type: TruckPOIType)
    case checkBridgeClearances
    case checkHeightRestrictions
    case checkWeightLimits
    case findWeighStations
    
    // Conversation
    case clarification(question: String)
    case unknown
}

// MARK: - Route Features

enum RouteFeature: String {
    case tolls
    case highways
    case ferries
    case unpavedRoads
}

// MARK: - Optimization Criteria

enum OptimizationCriterion: String {
    case fastest
    case shortest
    case ecoFriendly
    case avoidTraffic
}

// MARK: - Truck POI Types

enum TruckPOIType: String {
    case truckStop
    case weighStation
    case restArea
    case dieselStation
    case truckWash
    case repairShop
    
    var displayName: String {
        switch self {
        case .truckStop: return "truck stops"
        case .weighStation: return "weigh stations"
        case .restArea: return "rest areas with truck parking"
        case .dieselStation: return "diesel fuel stations"
        case .truckWash: return "truck washes"
        case .repairShop: return "truck repair shops"
        }
    }
}

// MARK: - Command Result

enum CommandResult {
    case success(message: String, data: Any? = nil)
    case error(message: String)
    case tierRestricted(message: String)
    case needsClarification(question: String)
}

// MARK: - Voice State

enum VoiceState: Equatable {
    case idle
    case listening
    case processing
    case speaking(text: String)
    case error(message: String)
    
    static func == (lhs: VoiceState, rhs: VoiceState) -> Bool {
        switch (lhs, rhs) {
        case (.idle, .idle),
             (.listening, .listening),
             (.processing, .processing):
            return true
        case (.speaking(let lhsText), .speaking(let rhsText)):
            return lhsText == rhsText
        case (.error(let lhsMsg), .error(let rhsMsg)):
            return lhsMsg == rhsMsg
        default:
            return false
        }
    }
}

// MARK: - Voice Trigger

enum VoiceTrigger {
    case manual      // User tapped microphone button
    case wakeWord    // Wake word detected
    case continuation // Continuation of multi-turn conversation
}

// MARK: - Conversation Turn

struct ConversationTurn {
    let role: String  // "user" or "assistant"
    let content: String
    let timestamp: Date
    
    init(role: String, content: String, timestamp: Date = Date()) {
        self.role = role
        self.content = content
        self.timestamp = timestamp
    }
}

// MARK: - Subscription Tier Extensions

extension SubscriptionTier {
    var allowsWakeWord: Bool {
        return self == .plus || self == .pro
    }
    
    var allowsAdvancedVoice: Bool {
        return self == .plus || self == .pro
    }
}
