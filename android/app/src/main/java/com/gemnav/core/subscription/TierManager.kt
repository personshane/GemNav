package com.gemnav.core.subscription

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * TierManager - Central manager for subscription tier state.
 * Holds current tier and provides helper methods for feature checks.
 */
object TierManager {
    
    private const val TAG = "TierManager"
    private const val PREFS_NAME = "gemnav_tier_prefs"
    private const val KEY_CACHED_TIER = "cached_tier"
    private const val KEY_LAST_VERIFIED = "last_verified"
    
    private val _currentTier = MutableStateFlow(Tier.FREE)
    val currentTier: StateFlow<Tier> = _currentTier.asStateFlow()
    
    private var prefs: SharedPreferences? = null
    private var isInitialized = false
    
    /**
     * Initialize TierManager with context.
     * Should be called from Application.onCreate()
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Load cached tier on startup
        val cachedTierName = prefs?.getString(KEY_CACHED_TIER, Tier.FREE.name) ?: Tier.FREE.name
        val cachedTier = try {
            Tier.valueOf(cachedTierName)
        } catch (e: Exception) {
            Tier.FREE
        }
        
        _currentTier.value = cachedTier
        isInitialized = true
        Log.d(TAG, "TierManager initialized with tier: $cachedTier")
        
        // TODO: Verify with BillingClient on startup
        // TODO: Check for remote config trial handling
    }
    
    /**
     * Get current subscription tier.
     */
    fun getCurrentTier(): Tier = _currentTier.value
    
    /**
     * Check if user has Plus tier or higher.
     */
    fun isPlus(): Boolean = _currentTier.value >= Tier.PLUS
    
    /**
     * Check if user has Pro tier.
     */
    fun isPro(): Boolean = _currentTier.value == Tier.PRO
    
    /**
     * Check if user is on Free tier.
     */
    fun isFree(): Boolean = _currentTier.value == Tier.FREE
    
    /**
     * Update tier from billing result.
     * Called by BillingClientManager when purchases are verified.
     */
    fun updateTier(newTier: Tier) {
        val oldTier = _currentTier.value
        if (oldTier != newTier) {
            _currentTier.value = newTier
            cacheTier(newTier)
            Log.i(TAG, "Tier updated: $oldTier -> $newTier")
            
            // TODO: Notify analytics of tier change
            // TODO: Enable/disable features based on new tier
        }
    }
    
    /**
     * Handle successful purchase of a SKU.
     */
    fun onPurchaseCompleted(sku: String) {
        val newTier = Tier.fromSku(sku)
        updateTier(newTier)
        Log.i(TAG, "Purchase completed: $sku -> tier $newTier")
    }
    
    /**
     * Handle subscription expiration or cancellation.
     */
    fun onSubscriptionExpired() {
        updateTier(Tier.FREE)
        Log.i(TAG, "Subscription expired, reverted to FREE")
        
        // TODO: Show grace period UI if applicable
        // TODO: Handle account hold state
    }
    
    /**
     * Restore purchases from Google Play.
     * TODO: Implement full restore flow
     */
    fun restorePurchases() {
        Log.d(TAG, "TODO: Restore purchases from BillingClient")
        // TODO: Query BillingClient for existing purchases
        // TODO: Update tier based on restored purchases
    }
    
    /**
     * Cache tier locally for offline access.
     */
    private fun cacheTier(tier: Tier) {
        prefs?.edit()?.apply {
            putString(KEY_CACHED_TIER, tier.name)
            putLong(KEY_LAST_VERIFIED, System.currentTimeMillis())
            apply()
        }
    }
    
    /**
     * Check if cached tier is stale (older than 24 hours).
     */
    fun isCacheStale(): Boolean {
        val lastVerified = prefs?.getLong(KEY_LAST_VERIFIED, 0L) ?: 0L
        val staleThreshold = 24 * 60 * 60 * 1000L // 24 hours
        return System.currentTimeMillis() - lastVerified > staleThreshold
    }
    
    /**
     * Force refresh tier from BillingClient.
     * TODO: Implement when BillingClient is wired
     */
    fun forceRefresh() {
        Log.d(TAG, "TODO: Force refresh tier from BillingClient")
        // TODO: Query purchases and update tier
    }
    
    // ============================================================
    // TEMPORARY: Debug/testing methods (remove in production)
    // ============================================================
    
    /**
     * DEBUG ONLY: Set tier directly for testing.
     * This bypasses billing and should never be used in production.
     */
    fun debugSetTier(tier: Tier) {
        Log.w(TAG, "DEBUG: Manually setting tier to $tier")
        updateTier(tier)
    }
}
