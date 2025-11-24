package com.gemnav.app

import android.app.Application
import android.util.Log
import com.gemnav.core.shim.GeminiShim
import com.gemnav.core.shim.HereShim
import com.gemnav.core.shim.MapsShim
import com.gemnav.core.shim.SafeModeManager
import com.gemnav.core.shim.VersionCheck
import com.gemnav.core.subscription.BillingClientManager
import com.gemnav.core.subscription.TierManager
import dagger.hilt.android.HiltAndroidApp

/**
 * GemNavApplication - Main Application class for GemNav.
 * Initializes the SDK Shield Layer before any other components.
 */
@HiltAndroidApp
class GemNavApplication : Application() {
    
    companion object {
        private const val TAG = "GemNavApplication"
    }
    
    override fun onCreate() {
        super.onCreate()
        initializeSubscriptionSystem()
        initializeShieldLayer()
    }
    
    /**
     * Initialize subscription and billing system.
     * Must run early to determine feature availability.
     */
    private fun initializeSubscriptionSystem() {
        Log.i(TAG, "Initializing subscription system...")
        
        // Initialize TierManager first (loads cached tier)
        TierManager.initialize(this)
        Log.i(TAG, "TierManager initialized with tier: ${TierManager.getCurrentTier()}")
        
        // Initialize BillingClient (connects to Google Play)
        BillingClientManager.initialize(this)
        Log.i(TAG, "BillingClientManager initialized")
    }
    
    /**
     * Initialize the SDK Shield Layer.
     * This MUST run before any Maps, Gemini, or HERE components are touched.
     */
    private fun initializeShieldLayer() {
        Log.i(TAG, "Initializing SDK Shield Layer...")
        
        // Step 1: Perform version checks
        val versionResult = VersionCheck.performAllChecks()
        logVersionCheckResult(versionResult)
        
        // Step 2: Set safe mode based on version checks
        if (!versionResult.allSafe) {
            Log.w(TAG, "Version checks failed - enabling Safe Mode")
            SafeModeManager.enableSafeMode()
        } else {
            Log.i(TAG, "All version checks passed - Safe Mode disabled")
            SafeModeManager.disableSafeMode()
        }
        
        // Step 3: Initialize shims (only if not in safe mode)
        if (!SafeModeManager.isSafeModeEnabled()) {
            initializeShims()
        } else {
            Log.w(TAG, "Skipping shim initialization due to Safe Mode")
        }
        
        // Step 4: Set up safe mode listener for app-wide state changes
        setupSafeModeListener()
        
        // Log final status
        logShieldLayerStatus()
    }
    
    /**
     * Initialize all SDK shims.
     */
    private fun initializeShims() {
        Log.i(TAG, "Initializing SDK shims...")
        
        // Initialize Maps shim
        val mapsInitialized = MapsShim.initialize()
        if (!mapsInitialized) {
            Log.w(TAG, "MapsShim initialization failed")
        }
        
        // Initialize Gemini shim (default to Nano for Free tier)
        val geminiInitialized = GeminiShim.initialize(GeminiShim.GeminiMode.NANO)
        if (!geminiInitialized) {
            Log.w(TAG, "GeminiShim initialization failed")
        }
        
        // Initialize HERE shim (Pro tier only, but safe to init)
        val hereInitialized = HereShim.initialize()
        if (!hereInitialized) {
            Log.w(TAG, "HereShim initialization failed")
        }
        
        Log.i(TAG, "Shim initialization complete: Maps=$mapsInitialized, Gemini=$geminiInitialized, HERE=$hereInitialized")
    }
    
    /**
     * Set up listener for safe mode state changes.
     */
    private fun setupSafeModeListener() {
        SafeModeManager.setSafeModeListener(object : SafeModeManager.SafeModeListener {
            override fun onSafeModeChanged(enabled: Boolean) {
                Log.i(TAG, "Safe Mode changed: enabled=$enabled")
                // TODO: Notify UI components of safe mode change
                // Could use a broadcast, event bus, or StateFlow
            }
            
            override fun onFailureReported(component: String, exception: Exception?) {
                Log.w(TAG, "Component failure: $component - ${exception?.message}")
                // TODO: Track failures for analytics/crashlytics
            }
        })
    }
    
    /**
     * Log version check results.
     */
    private fun logVersionCheckResult(result: VersionCheck.VersionCheckResult) {
        Log.i(TAG, "Version Check Results:")
        Log.i(TAG, "  Maps: ${result.mapsCheck.detectedVersion ?: "unknown"} (safe: ${result.mapsCheck.isSafe})")
        Log.i(TAG, "  HERE: ${result.hereCheck.detectedVersion ?: "unknown"} (safe: ${result.hereCheck.isSafe})")
        Log.i(TAG, "  Gemini: ${result.geminiCheck.detectedVersion ?: "unknown"} (safe: ${result.geminiCheck.isSafe})")
        Log.i(TAG, "  All Safe: ${result.allSafe}")
    }
    
    /**
     * Log final shield layer status.
     */
    private fun logShieldLayerStatus() {
        val status = SafeModeManager.getStatusSummary()
        Log.i(TAG, "Shield Layer Status:")
        Log.i(TAG, "  Safe Mode: ${status.isSafeModeEnabled}")
        Log.i(TAG, "  Version Warning: ${status.hasVersionWarning}")
        Log.i(TAG, "  Recent Failures: ${status.recentFailureCount}/${status.failureThreshold}")
    }
}
