package com.gemnav.core.feature

import android.util.Log
import com.gemnav.core.shim.GeminiShim
import com.gemnav.core.shim.HereShim
import com.gemnav.core.shim.MapsShim
import com.gemnav.core.shim.SafeModeManager

/**
 * FeatureGate - Unified feature-gating system for GemNav.
 * Prevents ViewModels from calling Gemini or HERE when SafeMode is active
 * or when subscription tier doesn't allow the feature.
 */
object FeatureGate {
    private const val TAG = "FeatureGate"
    
    // TODO: Replace with actual subscription tier from billing/auth
    private var currentTier: SubscriptionTier = SubscriptionTier.FREE
    
    /**
     * Subscription tiers for GemNav.
     */
    enum class SubscriptionTier {
        FREE,   // Gemini Nano + Google Maps intents only
        PLUS,   // Gemini Cloud + Google Maps SDK
        PRO     // Gemini Cloud + HERE SDK (truck routing)
    }
    
    /**
     * Set the current subscription tier.
     * TODO: Wire into billing/subscription system
     */
    fun setSubscriptionTier(tier: SubscriptionTier) {
        Log.i(TAG, "Subscription tier changed: $currentTier -> $tier")
        currentTier = tier
    }
    
    /**
     * Get the current subscription tier.
     */
    fun getCurrentTier(): SubscriptionTier = currentTier
    
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
        
        // TODO: Free tier uses Nano (on-device), Plus/Pro use Cloud
        // For now, AI is available at all tiers (Nano for Free)
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
        
        // TODO: Free tier → no cloud AI, only Nano
        // TODO: Plus tier → cloud AI enabled
        // TODO: Pro tier → cloud AI enabled
        return when (currentTier) {
            SubscriptionTier.FREE -> {
                logBlocked("Cloud AI", "Free tier - Nano only")
                false
            }
            SubscriptionTier.PLUS, SubscriptionTier.PRO -> true
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
        
        // TODO: Free tier → no HERE
        // TODO: Plus tier → no HERE
        // TODO: Pro tier → HERE enabled
        return when (currentTier) {
            SubscriptionTier.FREE, SubscriptionTier.PLUS -> {
                logBlocked("Commercial routing", "${currentTier.name} tier - Pro required")
                false
            }
            SubscriptionTier.PRO -> true
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
        
        // TODO: Free tier → Google Maps app via intents only
        // TODO: Plus/Pro tier → in-app Google Maps SDK
        return when (currentTier) {
            SubscriptionTier.FREE -> {
                logBlocked("In-app maps", "Free tier - intents only")
                false
            }
            SubscriptionTier.PLUS, SubscriptionTier.PRO -> true
        }
    }
    
    /**
     * Check if voice command features are enabled.
     * Basic voice available to all; advanced voice requires Plus/Pro.
     */
    fun areAdvancedVoiceCommandsEnabled(): Boolean {
        if (SafeModeManager.isSafeModeEnabled()) {
            logBlocked("Advanced voice", "SafeMode active")
            return false
        }
        
        return when (currentTier) {
            SubscriptionTier.FREE -> {
                logBlocked("Advanced voice", "Free tier - basic only")
                false
            }
            SubscriptionTier.PLUS, SubscriptionTier.PRO -> true
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
        
        return when (currentTier) {
            SubscriptionTier.FREE -> {
                logBlocked("Multi-waypoint", "Free tier - single destination only")
                false
            }
            SubscriptionTier.PLUS, SubscriptionTier.PRO -> true
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
            tier = currentTier,
            isSafeModeActive = SafeModeManager.isSafeModeEnabled(),
            advancedFeatures = areAdvancedFeaturesEnabled(),
            aiFeatures = areAIFeaturesEnabled(),
            cloudAI = areCloudAIFeaturesEnabled(),
            commercialRouting = areCommercialRoutingFeaturesEnabled(),
            inAppMaps = areInAppMapsEnabled(),
            advancedVoice = areAdvancedVoiceCommandsEnabled(),
            multiWaypoint = areMultiWaypointEnabled()
        )
    }
    
    /**
     * Summary of all feature availability.
     */
    data class FeatureSummary(
        val tier: SubscriptionTier,
        val isSafeModeActive: Boolean,
        val advancedFeatures: Boolean,
        val aiFeatures: Boolean,
        val cloudAI: Boolean,
        val commercialRouting: Boolean,
        val inAppMaps: Boolean,
        val advancedVoice: Boolean,
        val multiWaypoint: Boolean
    )
}
