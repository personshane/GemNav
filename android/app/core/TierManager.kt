package com.gemnav.app.core

/**
 * User subscription tier enum.
 * Unified tier representation used throughout the app.
 */
enum class SubscriptionTier {
    FREE,
    PLUS,
    PRO;
    
    val isPlus: Boolean get() = this == PLUS || this == PRO
    val isPro: Boolean get() = this == PRO
    val isFree: Boolean get() = this == FREE
}

/**
 * Feature enum for tier-based gating.
 */
enum class Feature {
    CLOUD_AI,
    IN_APP_NAVIGATION,
    MULTI_WAYPOINT,
    VOICE_WAKE_WORD,
    OFFLINE_MAPS,
    TRUCK_ROUTING,
    HAZMAT_RESTRICTIONS,
    HOS_TRACKING
}

/**
 * Manages user subscription tier and feature access.
 */
interface TierManager {
    fun getCurrentTier(): SubscriptionTier
    fun hasFeature(feature: Feature): Boolean
    suspend fun updateTier(newTier: SubscriptionTier)
}

/**
 * Default implementation of TierManager.
 */
class DefaultTierManager : TierManager {
    // TODO: Load from SharedPreferences or billing system
    private var currentTier: SubscriptionTier = SubscriptionTier.FREE
    
    override fun getCurrentTier(): SubscriptionTier = currentTier
    
    override fun hasFeature(feature: Feature): Boolean {
        return when (feature) {
            Feature.CLOUD_AI -> currentTier.isPlus
            Feature.IN_APP_NAVIGATION -> currentTier.isPlus
            Feature.MULTI_WAYPOINT -> currentTier.isPlus
            Feature.VOICE_WAKE_WORD -> currentTier.isPlus
            Feature.OFFLINE_MAPS -> currentTier.isPlus
            Feature.TRUCK_ROUTING -> currentTier.isPro
            Feature.HAZMAT_RESTRICTIONS -> currentTier.isPro
            Feature.HOS_TRACKING -> currentTier.isPro
        }
    }
    
    override suspend fun updateTier(newTier: SubscriptionTier) {
        currentTier = newTier
        // TODO: Persist to SharedPreferences and notify observers
    }
}
