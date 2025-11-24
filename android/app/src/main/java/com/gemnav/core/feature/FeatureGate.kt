package com.gemnav.core.feature

import android.util.Log
import com.gemnav.core.shim.GeminiShim
import com.gemnav.core.shim.HereShim
import com.gemnav.core.shim.MapsShim
import com.gemnav.core.shim.SafeModeManager
import com.gemnav.core.subscription.Tier
import com.gemnav.core.subscription.TierManager

/**
 * FeatureGate - Unified feature-gating system for GemNav.
 * Prevents ViewModels from calling Gemini or HERE when SafeMode is active
 * or when subscription tier doesn't allow the feature.
 * 
 * Now integrated with TierManager for real subscription state.
 */
object FeatureGate {
    private const val TAG = "FeatureGate"
    
    // ========== TIER ACCESS ==========
    
    /**
     * Get the current subscription tier from TierManager.
     */
    fun getCurrentTier(): Tier = TierManager.getCurrentTier()
    
    /**
     * Check if user has Plus tier or higher.
     */
    fun isPlus(): Boolean = TierManager.isPlus()
    
    /**
     * Check if user has Pro tier.
     */
    fun isPro(): Boolean = TierManager.isPro()
    
    /**
     * Check if user is on Free tier.
     */
    fun isFree(): Boolean = TierManager.isFree()
    
    // ========== CORE FEATURE GATES ==========
    
    /**
     * Check if advanced features are enabled.
     * Returns FALSE if SafeMode is active.
     */
    fun areAdvancedFeaturesEnabled(): Boolean {
        if (SafeModeManager.isSafeModeEnabled()) {
            logBlocked("Advanced features", "SafeMode active")
            return false
        }
        return true
    }
    
    /**
     * Check if AI features (Gemini) are enabled.
     * Requires: Not in SafeMode + appropriate tier.
     * Note: Free tier uses Nano (on-device), Plus/Pro use Cloud.
     */
    fun areAIFeaturesEnabled(): Boolean {
        if (SafeModeManager.isSafeModeEnabled()) {
            logBlocked("AI features", "SafeMode active")
            return false
        }
        
        if (!GeminiShim.isAvailable()) {
            logBlocked("AI features", "GeminiShim unavailable")
            return false
        }
        
        // AI is available at all tiers (Nano for Free, Cloud for Plus/Pro)
        return true
    }
    
    /**
     * Check if cloud AI features (Gemini Cloud) are enabled.
     * Requires: Plus or Pro tier + not in SafeMode.
     */
    fun areCloudAIFeaturesEnabled(): Boolean {
        if (!areAIFeaturesEnabled()) {
            return false
        }
        
        val tier = getCurrentTier()
        return when (tier) {
            Tier.FREE -> {
                logBlocked("Cloud AI", "Free tier - Nano only")
                false
            }
            Tier.PLUS, Tier.PRO -> true
        }
    }
    
    /**
     * Check if commercial routing features (HERE SDK) are enabled.
     * Requires: Pro tier + not in SafeMode + HERE available.
     */
    fun areCommercialRoutingFeaturesEnabled(): Boolean {
        if (SafeModeManager.isSafeModeEnabled()) {
            logBlocked("Commercial routing", "SafeMode active")
            return false
        }
        
        if (!HereShim.isAvailable()) {
            logBlocked("Commercial routing", "HereShim unavailable")
            return false
        }
        
        val tier = getCurrentTier()
        return when (tier) {
            Tier.FREE, Tier.PLUS -> {
                logBlocked("Commercial routing", "${tier.name} tier - Pro required")
                false
            }
            Tier.PRO -> true
        }
    }
    
    /**
     * Check if in-app maps (Google Maps SDK) are enabled.
     * Requires: Plus or Pro tier + not in SafeMode.
     */
    fun areInAppMapsEnabled(): Boolean {
        if (SafeModeManager.isSafeModeEnabled()) {
            logBlocked("In-app maps", "SafeMode active")
            return false
        }
        
        if (!MapsShim.isAvailable()) {
            logBlocked("In-app maps", "MapsShim unavailable")
            return false
        }
        
        val tier = getCurrentTier()
        return when (tier) {
            Tier.FREE -> {
                logBlocked("In-app maps", "Free tier - intents only")
                false
            }
            Tier.PLUS, Tier.PRO -> true
        }
    }
    
    /**
     * Check if advanced voice command features are enabled.
     * Basic voice available to all; advanced voice requires Plus/Pro.
     */
    fun areAdvancedVoiceCommandsEnabled(): Boolean {
        if (SafeModeManager.isSafeModeEnabled()) {
            logBlocked("Advanced voice", "SafeMode active")
            return false
        }
        
        val tier = getCurrentTier()
        return when (tier) {
            Tier.FREE -> {
                logBlocked("Advanced voice", "Free tier - basic only")
                false
            }
            Tier.PLUS, Tier.PRO -> true
        }
    }
    
    /**
     * Check if multi-waypoint routing is enabled.
     * Requires Plus or Pro tier.
     */
    fun areMultiWaypointEnabled(): Boolean {
        if (SafeModeManager.isSafeModeEnabled()) {
            logBlocked("Multi-waypoint", "SafeMode active")
            return false
        }
        
        val tier = getCurrentTier()
        return when (tier) {
            Tier.FREE -> {
                logBlocked("Multi-waypoint", "Free tier - single destination only")
                false
            }
            Tier.PLUS, Tier.PRO -> true
        }
    }
    
    /**
     * Get maximum allowed waypoints based on tier.
     */
    fun getMaxWaypoints(): Int {
        return when (getCurrentTier()) {
            Tier.FREE -> 1
            Tier.PLUS -> 10
            Tier.PRO -> 25
        }
    }
    
    // ========== HELPER FUNCTIONS ==========
    
    /**
     * Log when a feature is blocked.
     */
    private fun logBlocked(feature: String, reason: String) {
        Log.d(TAG, "Feature blocked: $feature - $reason")
    }
    
    /**
     * Get a summary of available features for the current state.
     */
    fun getFeatureSummary(): FeatureSummary {
        return FeatureSummary(
            tier = getCurrentTier(),
            isSafeModeActive = SafeModeManager.isSafeModeEnabled(),
            advancedFeatures = areAdvancedFeaturesEnabled(),
            aiFeatures = areAIFeaturesEnabled(),
            cloudAI = areCloudAIFeaturesEnabled(),
            commercialRouting = areCommercialRoutingFeaturesEnabled(),
            inAppMaps = areInAppMapsEnabled(),
            advancedVoice = areAdvancedVoiceCommandsEnabled(),
            multiWaypoint = areMultiWaypointEnabled(),
            maxWaypoints = getMaxWaypoints()
        )
    }
    
    /**
     * Summary of all feature availability.
     */
    data class FeatureSummary(
        val tier: Tier,
        val isSafeModeActive: Boolean,
        val advancedFeatures: Boolean,
        val aiFeatures: Boolean,
        val cloudAI: Boolean,
        val commercialRouting: Boolean,
        val inAppMaps: Boolean,
        val advancedVoice: Boolean,
        val multiWaypoint: Boolean,
        val maxWaypoints: Int = 1
    )
    
    // TODO: Add caching for frequently checked features
    // TODO: Add error screen handling when features fail
    // TODO: Add analytics tracking for feature access
}
