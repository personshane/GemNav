package com.gemnav.app.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import com.gemnav.core.feature.FeatureGate
import com.gemnav.core.shim.HereShim
import com.gemnav.core.shim.SafeModeManager
import com.gemnav.core.shim.VersionCheck
import com.gemnav.core.subscription.Tier
import com.gemnav.core.subscription.TierManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * SettingsViewModel - Handles app settings with feature gating.
 */
class SettingsViewModel : ViewModel() {
    
    companion object {
        private const val TAG = "SettingsViewModel"
    }
    
    private val _featureSummary = MutableStateFlow(FeatureGate.getFeatureSummary())
    val featureSummary: StateFlow<FeatureGate.FeatureSummary> = _featureSummary
    
    private val _safeModeStatus = MutableStateFlow(SafeModeManager.getStatusSummary())
    val safeModeStatus: StateFlow<SafeModeManager.SafeModeStatus> = _safeModeStatus
    
    private val _truckSpecs = MutableStateFlow(HereShim.TruckSpecs())
    val truckSpecs: StateFlow<HereShim.TruckSpecs> = _truckSpecs
    
    private val _isTruckModeEnabled = MutableStateFlow(false)
    val isTruckModeEnabled: StateFlow<Boolean> = _isTruckModeEnabled
    
    // UI Settings
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode
    
    private val _isVoiceEnabled = MutableStateFlow(true)
    val isVoiceEnabled: StateFlow<Boolean> = _isVoiceEnabled
    
    private val _distanceUnit = MutableStateFlow(DistanceUnit.MILES)
    val distanceUnit: StateFlow<DistanceUnit> = _distanceUnit
    
    enum class DistanceUnit { MILES, KILOMETERS }
    
    init {
        refreshState()
    }
    
    /**
     * Refresh all state.
     */
    fun refreshState() {
        _featureSummary.value = FeatureGate.getFeatureSummary()
        _safeModeStatus.value = SafeModeManager.getStatusSummary()
        _isTruckModeEnabled.value = HereShim.isTruckModeEnabled()
    }
    
    // ========== TIER-GATED SETTINGS ==========
    
    /**
     * Enable/disable truck mode.
     * Gated by FeatureGate.areCommercialRoutingFeaturesEnabled()
     */
    fun setTruckModeEnabled(enabled: Boolean) {
        if (enabled && !FeatureGate.areCommercialRoutingFeaturesEnabled()) {
            Log.d(TAG, "Truck mode setting blocked - Pro required")
            // TODO: Show upgrade prompt
            return
        }
        
        _isTruckModeEnabled.value = enabled
        HereShim.setTruckMode(enabled)
        Log.d(TAG, "Truck mode set to: $enabled")
    }
    
    /**
     * Update truck specifications.
     * Gated by FeatureGate.areCommercialRoutingFeaturesEnabled()
     */
    fun updateTruckSpecs(specs: HereShim.TruckSpecs) {
        if (!FeatureGate.areCommercialRoutingFeaturesEnabled()) {
            Log.d(TAG, "Truck specs setting blocked - Pro required")
            return
        }
        
        _truckSpecs.value = specs
        HereShim.setTruckSpecs(specs)
        Log.d(TAG, "Truck specs updated: ${specs.heightCm}cm height, ${specs.weightKg}kg")
    }
    
    // ========== GENERAL SETTINGS ==========
    
    /**
     * Toggle dark mode.
     */
    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        // TODO: Persist to preferences
        Log.d(TAG, "Dark mode set to: $enabled")
    }
    
    /**
     * Toggle voice commands.
     */
    fun setVoiceEnabled(enabled: Boolean) {
        _isVoiceEnabled.value = enabled
        // TODO: Persist to preferences
        Log.d(TAG, "Voice enabled set to: $enabled")
    }
    
    /**
     * Set distance unit.
     */
    fun setDistanceUnit(unit: DistanceUnit) {
        _distanceUnit.value = unit
        // TODO: Persist to preferences
        Log.d(TAG, "Distance unit set to: $unit")
    }
    
    // ========== SAFE MODE CONTROLS ==========
    
    /**
     * Manually enable safe mode.
     */
    fun enableSafeMode() {
        SafeModeManager.enableSafeMode()
        refreshState()
        Log.d(TAG, "Safe mode manually enabled")
    }
    
    /**
     * Manually disable safe mode.
     */
    fun disableSafeMode() {
        SafeModeManager.disableSafeMode()
        refreshState()
        Log.d(TAG, "Safe mode manually disabled")
    }
    
    /**
     * Re-run version checks.
     */
    fun rerunVersionChecks() {
        val result = VersionCheck.performAllChecks()
        Log.d(TAG, "Version checks rerun - all safe: ${result.allSafe}")
        refreshState()
    }
    
    // ========== SUBSCRIPTION CONTROLS ==========
    
    /**
     * Simulate tier change (for testing).
     * TODO: Wire into actual billing system
     */
    fun setSubscriptionTier(tier: Tier) {
        TierManager.debugSetTier(tier)
        refreshState()
        Log.d(TAG, "Subscription tier changed to: $tier")
    }
    
    /**
     * Check if a specific feature is available.
     */
    fun isFeatureAvailable(feature: Feature): Boolean {
        return when (feature) {
            Feature.AI_ROUTING -> FeatureGate.areAIFeaturesEnabled()
            Feature.CLOUD_AI -> FeatureGate.areCloudAIFeaturesEnabled()
            Feature.TRUCK_ROUTING -> FeatureGate.areCommercialRoutingFeaturesEnabled()
            Feature.IN_APP_MAPS -> FeatureGate.areInAppMapsEnabled()
            Feature.ADVANCED_VOICE -> FeatureGate.areAdvancedVoiceCommandsEnabled()
            Feature.MULTI_WAYPOINT -> FeatureGate.areMultiWaypointEnabled()
        }
    }
    
    enum class Feature {
        AI_ROUTING,
        CLOUD_AI,
        TRUCK_ROUTING,
        IN_APP_MAPS,
        ADVANCED_VOICE,
        MULTI_WAYPOINT
    }
}
