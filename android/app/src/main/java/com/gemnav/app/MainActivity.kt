package com.gemnav.app

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.gemnav.app.ui.AppNavHost
import com.gemnav.app.ui.theme.AppTheme
import com.gemnav.core.safety.SafeModeManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
        
        // Expose safe mode state as a StateFlow for Compose
        private val _isSafeModeEnabled = MutableStateFlow(false)
        val isSafeModeEnabled: StateFlow<Boolean> = _isSafeModeEnabled.asStateFlow()
        
        /**
         * Check if advanced features should be disabled.
         * Call this before enabling:
         * - Gemini enhanced routing
         * - AI queries
         * - HERE truck routing
         */
        fun areAdvancedFeaturesEnabled(): Boolean {
            return !SafeModeManager.isSafeModeEnabled()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check safe mode status on activity creation
        checkSafeModeStatus()
        
        setContent {
            // Observe safe mode state
            val isSafeMode by isSafeModeEnabled.collectAsState()
            
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // TODO: Pass isSafeMode to AppNavHost for feature gating
                    // AppNavHost(isSafeMode = isSafeMode)
                    AppNavHost()
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Re-check safe mode on resume in case it changed
        checkSafeModeStatus()
    }
    
    /**
     * Check and log safe mode status.
     * Updates the StateFlow for Compose observation.
     */
    private fun checkSafeModeStatus() {
        val safeMode = SafeModeManager.isSafeModeEnabled()
        _isSafeModeEnabled.value = safeMode
        
        if (safeMode) {
            Log.w(TAG, "Safe Mode is ENABLED - advanced features disabled")
            logDisabledFeatures()
        } else {
            Log.i(TAG, "Safe Mode is DISABLED - all features available")
        }
        
        // Log any version warnings
        if (SafeModeManager.hasVersionWarning()) {
            Log.w(TAG, "Version compatibility warning active")
        }
    }
    
    /**
     * Log which features are disabled in safe mode.
     */
    private fun logDisabledFeatures() {
        Log.w(TAG, "Disabled features in Safe Mode:")
        Log.w(TAG, "  - Gemini enhanced routing")
        Log.w(TAG, "  - AI-powered queries")
        Log.w(TAG, "  - HERE truck routing")
        Log.w(TAG, "  - Advanced voice commands")
        // TODO: Implement actual feature gating in respective ViewModels/screens
    }
}
