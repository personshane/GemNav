import Foundation

/// User subscription tier enum.
/// Unified tier representation used throughout the app.
enum SubscriptionTier {
    case free
    case plus
    case pro
    
    var isPlus: Bool { self == .plus || self == .pro }
    var isPro: Bool { self == .pro }
    var isFree: Bool { self == .free }
}

/// Feature enum for tier-based gating.
enum Feature {
    case cloudAI
    case inAppNavigation
    case multiWaypoint
    case voiceWakeWord
    case offlineMaps
    case truckRouting
    case hazmatRestrictions
    case hosTracking
}

/// Manages user subscription tier and feature access.
protocol TierManager {
    func getCurrentTier() -> SubscriptionTier
    func hasFeature(_ feature: Feature) -> Bool
    func updateTier(_ newTier: SubscriptionTier) async
}

/// Default implementation of TierManager.
class DefaultTierManager: TierManager {
    // TODO: Load from UserDefaults or billing system
    private var currentTier: SubscriptionTier = .free
    
    func getCurrentTier() -> SubscriptionTier {
        return currentTier
    }
    
    func hasFeature(_ feature: Feature) -> Bool {
        switch feature {
        case .cloudAI, .inAppNavigation, .multiWaypoint, .voiceWakeWord, .offlineMaps:
            return currentTier.isPlus
        case .truckRouting, .hazmatRestrictions, .hosTracking:
            return currentTier.isPro
        }
    }
    
    func updateTier(_ newTier: SubscriptionTier) async {
        currentTier = newTier
        // TODO: Persist to UserDefaults and notify observers
    }
}
