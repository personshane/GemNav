package com.gemnav.core.safety

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * SafeModeManager - Controls app-wide "Safe Mode" when providers fail.
 * Allows navigation to continue gracefully even when SDKs have issues.
 */
object SafeModeManager {
    private const val TAG = "SafeModeManager"
    private const val MAX_FAILURES_BEFORE_SAFE_MODE = 3
    private const val FAILURE_WINDOW_MS = 60_000L // 1 minute
    
    private val isSafeModeEnabled = AtomicBoolean(false)
    private val hasVersionWarning = AtomicBoolean(false)
    private val failureCount = AtomicInteger(0)
    private val failures = ConcurrentHashMap<String, MutableList<FailureRecord>>()
    private var safeModeListener: SafeModeListener? = null
    
    /**
     * Enable safe mode manually.
     * Useful for testing or user-initiated safe mode.
     */
    fun enableSafeMode() {
        if (!isSafeModeEnabled.getAndSet(true)) {
            logWarning("Safe Mode ENABLED - limited functionality active")
            safeModeListener?.onSafeModeChanged(true)
        }
    }
    
    /**
     * Disable safe mode and attempt normal operation.
     */
    fun disableSafeMode() {
        if (isSafeModeEnabled.getAndSet(false)) {
            logInfo("Safe Mode DISABLED - normal operation resumed")
            clearAllFailures()
            safeModeListener?.onSafeModeChanged(false)
        }
    }
    
    /**
     * Check if safe mode is currently enabled.
     */
    fun isSafeModeEnabled(): Boolean = isSafeModeEnabled.get()
    
    /**
     * Set version warning flag from VersionCheck.
     */
    fun setVersionWarning(hasWarning: Boolean) {
        hasVersionWarning.set(hasWarning)
        if (hasWarning) {
            logWarning("Version compatibility warning active")
        }
    }
    
    /**
     * Check if there's a version compatibility warning.
     */
    fun hasVersionWarning(): Boolean = hasVersionWarning.get()
    
    /**
     * Report a failure from any shim component.
     * Tracks failures and auto-enables safe mode if threshold exceeded.
     * 
     * @param component Name of the failing component
     * @param exception The exception that occurred (nullable)
     */
    fun reportFailure(component: String, exception: Exception?) {
        try {
            val record = FailureRecord(
                component = component,
                timestamp = System.currentTimeMillis(),
                message = exception?.message ?: "Unknown error",
                exceptionType = exception?.javaClass?.simpleName ?: "Unknown"
            )
            
            // Add to component-specific failure list
            failures.getOrPut(component) { mutableListOf() }.add(record)
            
            // Clean old failures outside the window
            cleanOldFailures()
            
            // Count recent failures
            val recentFailureCount = countRecentFailures()
            failureCount.set(recentFailureCount)
            
            logWarning("Failure reported: $component - ${exception?.message}")
            logInfo("Total recent failures: $recentFailureCount / $MAX_FAILURES_BEFORE_SAFE_MODE")
            
            // Auto-enable safe mode if threshold exceeded
            if (recentFailureCount >= MAX_FAILURES_BEFORE_SAFE_MODE) {
                enableSafeMode()
            }
            
            // Notify listener
            safeModeListener?.onFailureReported(component, exception)
            
        } catch (e: Exception) {
            // Don't let failure reporting itself cause issues
            Log.e(TAG, "Error in reportFailure", e)
        }
    }
    
    /**
     * Count failures within the recent time window.
     */
    private fun countRecentFailures(): Int {
        val cutoff = System.currentTimeMillis() - FAILURE_WINDOW_MS
        return failures.values.sumOf { list ->
            list.count { it.timestamp >= cutoff }
        }
    }
    
    /**
     * Remove failures older than the time window.
     */
    private fun cleanOldFailures() {
        val cutoff = System.currentTimeMillis() - FAILURE_WINDOW_MS
        failures.values.forEach { list ->
            list.removeAll { it.timestamp < cutoff }
        }
    }
    
    /**
     * Clear all failure records.
     */
    fun clearAllFailures() {
        failures.clear()
        failureCount.set(0)
        logInfo("All failure records cleared")
    }
    
    /**
     * Get recent failure count.
     */
    fun getRecentFailureCount(): Int = failureCount.get()
    
    /**
     * Get failures for a specific component.
     */
    fun getFailuresForComponent(component: String): List<FailureRecord> {
        return failures[component]?.toList() ?: emptyList()
    }
    
    /**
     * Get all recent failures.
     */
    fun getAllRecentFailures(): List<FailureRecord> {
        val cutoff = System.currentTimeMillis() - FAILURE_WINDOW_MS
        return failures.values.flatten().filter { it.timestamp >= cutoff }
    }
    
    /**
     * Set a listener for safe mode changes.
     */
    fun setSafeModeListener(listener: SafeModeListener?) {
        safeModeListener = listener
    }
    
    /**
     * Get current status summary.
     */
    fun getStatusSummary(): SafeModeStatus {
        return SafeModeStatus(
            isSafeModeEnabled = isSafeModeEnabled.get(),
            hasVersionWarning = hasVersionWarning.get(),
            recentFailureCount = failureCount.get(),
            failureThreshold = MAX_FAILURES_BEFORE_SAFE_MODE,
            componentFailures = failures.mapValues { it.value.size }
        )
    }
    
    /**
     * Execute an operation with safe mode fallback.
     * If operation fails and safe mode is enabled, returns fallback value.
     */
    fun <T> safeExecute(
        component: String,
        fallback: T,
        operation: () -> T
    ): T {
        return try {
            operation()
        } catch (e: Exception) {
            reportFailure(component, e)
            if (isSafeModeEnabled.get()) {
                logInfo("Safe mode active - returning fallback for $component")
            }
            fallback
        }
    }
    
    private fun logInfo(message: String) = Log.i(TAG, message)
    private fun logWarning(message: String) = Log.w(TAG, message)
    
    /**
     * Record of a single failure event.
     */
    data class FailureRecord(
        val component: String,
        val timestamp: Long,
        val message: String,
        val exceptionType: String
    )
    
    /**
     * Summary of safe mode status.
     */
    data class SafeModeStatus(
        val isSafeModeEnabled: Boolean,
        val hasVersionWarning: Boolean,
        val recentFailureCount: Int,
        val failureThreshold: Int,
        val componentFailures: Map<String, Int>
    )
    
    /**
     * Listener for safe mode events.
     */
    interface SafeModeListener {
        fun onSafeModeChanged(enabled: Boolean)
        fun onFailureReported(component: String, exception: Exception?)
    }
}
