package com.gemnav.core.shim

import android.util.Log

/**
 * VersionCheck - Runtime safety layer for SDK version detection.
 * Compares running SDK versions against known-safe versions and
 * triggers SafeMode when incompatible versions are detected.
 */
object VersionCheck {
    private const val TAG = "VersionCheck"
    
    // Known-safe SDK versions - update as new versions are verified
    private val KNOWN_SAFE_MAPS_VERSIONS = listOf(
        "18.1.0", "18.2.0", "19.0.0"
        // TODO: Add verified Maps SDK versions
    )
    
    private val KNOWN_SAFE_HERE_VERSIONS = listOf(
        "4.17.0", "4.18.0", "4.19.0"
        // TODO: Add verified HERE SDK versions
    )
    
    private val KNOWN_SAFE_GEMINI_VERSIONS = listOf(
        "1.0.0", "1.1.0"
        // TODO: Add verified Gemini SDK versions
    )
    
    private var detectedMapsVersion: String? = null
    private var detectedHereVersion: String? = null
    private var detectedGeminiVersion: String? = null
    
    private var mapsVersionSafe = true
    private var hereVersionSafe = true
    private var geminiVersionSafe = true
    
    /**
     * Perform all version checks at app startup.
     * @return VersionCheckResult with status of all SDKs
     */
    fun performAllChecks(): VersionCheckResult {
        logInfo("Starting SDK version checks...")
        
        val mapsResult = checkMapsVersion()
        val hereResult = checkHereVersion()
        val geminiResult = checkGeminiVersion()
        
        val allSafe = mapsResult.isSafe && hereResult.isSafe && geminiResult.isSafe
        
        if (!allSafe) {
            logWarning("One or more SDK versions are not in known-safe list")
            // TODO: Hook into SafeModeManager
            SafeModeManager.setVersionWarning(!allSafe)
        }
        
        return VersionCheckResult(
            mapsCheck = mapsResult,
            hereCheck = hereResult,
            geminiCheck = geminiResult,
            allSafe = allSafe
        )
    }
    
    /**
     * Detect and verify Google Maps SDK version.
     */
    fun checkMapsVersion(): SdkVersionCheck {
        return try {
            // TODO: Implement actual Maps SDK version detection
            // Example: com.google.android.gms.maps.MapsInitializer.getVersion()
            val version = detectMapsVersion()
            detectedMapsVersion = version
            
            val isSafe = version == null || KNOWN_SAFE_MAPS_VERSIONS.contains(version)
            mapsVersionSafe = isSafe
            
            if (version != null) {
                logInfo("Maps SDK version: $version (safe: $isSafe)")
            } else {
                logWarning("Could not detect Maps SDK version")
            }
            
            SdkVersionCheck(
                sdkName = "Google Maps",
                detectedVersion = version,
                knownSafeVersions = KNOWN_SAFE_MAPS_VERSIONS,
                isSafe = isSafe,
                recommendation = if (isSafe) null else "Update to a verified Maps SDK version"
            )
        } catch (e: Exception) {
            logError("Error checking Maps version", e)
            SdkVersionCheck(
                sdkName = "Google Maps",
                detectedVersion = null,
                knownSafeVersions = KNOWN_SAFE_MAPS_VERSIONS,
                isSafe = false,
                recommendation = "Could not verify Maps SDK version"
            )
        }
    }
    
    /**
     * Detect and verify HERE SDK version.
     */
    fun checkHereVersion(): SdkVersionCheck {
        return try {
            // TODO: Implement actual HERE SDK version detection
            val version = detectHereVersion()
            detectedHereVersion = version
            
            val isSafe = version == null || KNOWN_SAFE_HERE_VERSIONS.contains(version)
            hereVersionSafe = isSafe
            
            if (version != null) {
                logInfo("HERE SDK version: $version (safe: $isSafe)")
            } else {
                logInfo("HERE SDK not installed or not detected")
            }
            
            SdkVersionCheck(
                sdkName = "HERE",
                detectedVersion = version,
                knownSafeVersions = KNOWN_SAFE_HERE_VERSIONS,
                isSafe = isSafe,
                recommendation = if (isSafe) null else "Update to a verified HERE SDK version"
            )
        } catch (e: Exception) {
            logError("Error checking HERE version", e)
            SdkVersionCheck(
                sdkName = "HERE",
                detectedVersion = null,
                knownSafeVersions = KNOWN_SAFE_HERE_VERSIONS,
                isSafe = false,
                recommendation = "Could not verify HERE SDK version"
            )
        }
    }
    
    /**
     * Detect and verify Gemini SDK version.
     */
    fun checkGeminiVersion(): SdkVersionCheck {
        return try {
            // TODO: Implement actual Gemini SDK version detection
            val version = detectGeminiVersion()
            detectedGeminiVersion = version
            
            val isSafe = version == null || KNOWN_SAFE_GEMINI_VERSIONS.contains(version)
            geminiVersionSafe = isSafe
            
            if (version != null) {
                logInfo("Gemini SDK version: $version (safe: $isSafe)")
            } else {
                logWarning("Could not detect Gemini SDK version")
            }
            
            SdkVersionCheck(
                sdkName = "Gemini",
                detectedVersion = version,
                knownSafeVersions = KNOWN_SAFE_GEMINI_VERSIONS,
                isSafe = isSafe,
                recommendation = if (isSafe) null else "Update to a verified Gemini SDK version"
            )
        } catch (e: Exception) {
            logError("Error checking Gemini version", e)
            SdkVersionCheck(
                sdkName = "Gemini",
                detectedVersion = null,
                knownSafeVersions = KNOWN_SAFE_GEMINI_VERSIONS,
                isSafe = false,
                recommendation = "Could not verify Gemini SDK version"
            )
        }
    }
    
    // Stub detection methods - TODO: implement actual detection
    private fun detectMapsVersion(): String? {
        // TODO: Use reflection or SDK API to get actual version
        return null
    }
    
    private fun detectHereVersion(): String? {
        // TODO: Use reflection or SDK API to get actual version
        return null
    }
    
    private fun detectGeminiVersion(): String? {
        // TODO: Use reflection or SDK API to get actual version
        return null
    }
    
    // Getters for detected versions
    fun getDetectedMapsVersion(): String? = detectedMapsVersion
    fun getDetectedHereVersion(): String? = detectedHereVersion
    fun getDetectedGeminiVersion(): String? = detectedGeminiVersion
    
    fun isMapsVersionSafe(): Boolean = mapsVersionSafe
    fun isHereVersionSafe(): Boolean = hereVersionSafe
    fun isGeminiVersionSafe(): Boolean = geminiVersionSafe
    
    private fun logInfo(message: String) = Log.i(TAG, message)
    private fun logWarning(message: String) = Log.w(TAG, message)
    private fun logError(message: String, e: Exception) = Log.e(TAG, message, e)
    
    /**
     * Result of checking a single SDK version.
     */
    data class SdkVersionCheck(
        val sdkName: String,
        val detectedVersion: String?,
        val knownSafeVersions: List<String>,
        val isSafe: Boolean,
        val recommendation: String?
    )
    
    /**
     * Combined result of all SDK version checks.
     */
    data class VersionCheckResult(
        val mapsCheck: SdkVersionCheck,
        val hereCheck: SdkVersionCheck,
        val geminiCheck: SdkVersionCheck,
        val allSafe: Boolean
    )
}
