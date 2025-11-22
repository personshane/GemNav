package com.gemnav.android.voice.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages microphone permission for voice commands.
 * 
 * Handles:
 * - Permission status checking
 * - Runtime permission requests
 * - Settings navigation for denied permissions
 * - Permission state updates
 * 
 * Usage:
 * ```
 * val manager = MicrophonePermissionManager(activity)
 * manager.initialize(activity)
 * 
 * when (manager.permissionState.value) {
 *     PermissionState.Granted -> startVoiceRecording()
 *     PermissionState.Denied -> manager.requestPermission()
 *     PermissionState.PermanentlyDenied -> manager.navigateToSettings(activity)
 * }
 * ```
 */
class MicrophonePermissionManager(
    private val context: Context
) {
    private val _permissionState = MutableStateFlow(PermissionState.UNKNOWN)
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()
    
    private var permissionLauncher: ActivityResultLauncher<String>? = null
    private var shouldShowRationale: Boolean = false
    
    /**
     * Initialize permission launcher. Must be called in onCreate before any permission requests.
     * 
     * @param activity ComponentActivity that hosts the permission request
     */
    fun initialize(activity: ComponentActivity) {
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            handlePermissionResult(isGranted)
        }
        
        updatePermissionState(activity)
    }
    
    /**
     * Check current microphone permission status.
     */
    fun checkPermission(): Boolean {
        val isGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        
        _permissionState.value = if (isGranted) {
            PermissionState.GRANTED
        } else {
            PermissionState.DENIED
        }
        
        return isGranted
    }
    
    /**
     * Request microphone permission. Shows system permission dialog.
     * 
     * @throws IllegalStateException if initialize() was not called first
     */
    fun requestPermission() {
        requireNotNull(permissionLauncher) {
            "Permission launcher not initialized. Call initialize() first."
        }
        
        if (checkPermission()) {
            _permissionState.value = PermissionState.GRANTED
            return
        }
        
        permissionLauncher?.launch(Manifest.permission.RECORD_AUDIO)
    }
    
    /**
     * Navigate user to app settings to manually grant permission.
     * Use when permission is permanently denied.
     */
    fun navigateToSettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activity.startActivity(intent)
    }
    
    /**
     * Determine if permission rationale should be shown.
     * True when user denied permission once but not permanently.
     */
    fun shouldShowRationale(activity: Activity): Boolean {
        return activity.shouldShowRequestPermissionRationale(
            Manifest.permission.RECORD_AUDIO
        )
    }
    
    private fun updatePermissionState(activity: Activity) {
        val isGranted = checkPermission()
        shouldShowRationale = shouldShowRationale(activity)
        
        _permissionState.value = when {
            isGranted -> PermissionState.GRANTED
            shouldShowRationale -> PermissionState.DENIED
            else -> {
                // Permission denied + no rationale = permanently denied
                val hasRequestedBefore = context.getSharedPreferences(
                    "voice_permissions",
                    Context.MODE_PRIVATE
                ).getBoolean("microphone_requested", false)
                
                if (hasRequestedBefore) {
                    PermissionState.PERMANENTLY_DENIED
                } else {
                    PermissionState.DENIED
                }
            }
        }
    }
    
    private fun handlePermissionResult(isGranted: Boolean) {
        // Mark permission as requested
        context.getSharedPreferences("voice_permissions", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("microphone_requested", true)
            .apply()
        
        _permissionState.value = if (isGranted) {
            PermissionState.GRANTED
        } else {
            if (shouldShowRationale) {
                PermissionState.DENIED
            } else {
                PermissionState.PERMANENTLY_DENIED
            }
        }
    }
}

/**
 * Permission states for microphone access.
 */
enum class PermissionState {
    UNKNOWN,           // Initial state, not yet checked
    GRANTED,           // Permission granted
    DENIED,            // Permission denied, can request again
    PERMANENTLY_DENIED // Permission denied permanently, need settings navigation
}