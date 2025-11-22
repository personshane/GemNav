import Foundation
import AVFoundation
import Speech
import Combine

/**
 * Manages microphone and speech recognition permissions for voice commands.
 * 
 * Handles:
 * - AVAudioSession microphone permission
 * - SFSpeechRecognizer authorization
 * - Permission state tracking
 * - Settings navigation for denied permissions
 * 
 * Usage:
 * ```swift
 * let manager = MicrophonePermissionManager()
 * 
 * Task {
 *     let granted = await manager.requestPermissions()
 *     if granted {
 *         startVoiceRecording()
 *     } else if manager.permissionState.value == .permanentlyDenied {
 *         manager.navigateToSettings()
 *     }
 * }
 * ```
 */
class MicrophonePermissionManager: ObservableObject {
    @Published var permissionState: PermissionState = .unknown
    
    private let audioSession = AVAudioSession.sharedInstance()
    
    init() {
        updatePermissionState()
    }
    
    /**
     * Check current permission status without requesting.
     */
    func checkPermissions() -> Bool {
        let microphoneGranted = audioSession.recordPermission == .granted
        let speechGranted = SFSpeechRecognizer.authorizationStatus() == .authorized
        
        let granted = microphoneGranted && speechGranted
        
        if granted {
            permissionState = .granted
        } else {
            updatePermissionState()
        }
        
        return granted
    }
    
    /**
     * Request both microphone and speech recognition permissions.
     * Returns true if both are granted.
     */
    func requestPermissions() async -> Bool {
        // Request microphone permission first
        let microphoneGranted = await requestMicrophonePermission()
        guard microphoneGranted else {
            updatePermissionState()
            return false
        }
        
        // Request speech recognition permission
        let speechGranted = await requestSpeechRecognitionPermission()
        guard speechGranted else {
            updatePermissionState()
            return false
        }
        
        permissionState = .granted
        return true
    }
    
    /**
     * Navigate user to app settings to manually grant permissions.
     */
    func navigateToSettings() {
        guard let settingsUrl = URL(string: UIApplication.openSettingsURLString) else {
            return
        }
        
        if UIApplication.shared.canOpenURL(settingsUrl) {
            UIApplication.shared.open(settingsUrl)
        }
    }
    
    /**
     * Check if permissions should be requested (not permanently denied).
     */
    func shouldRequestPermissions() -> Bool {
        return permissionState != .permanentlyDenied
    }
    
    // MARK: - Private Methods
    
    private func requestMicrophonePermission() async -> Bool {
        return await withCheckedContinuation { continuation in
            audioSession.requestRecordPermission { granted in
                continuation.resume(returning: granted)
            }
        }
    }
    
    private func requestSpeechRecognitionPermission() async -> Bool {
        return await withCheckedContinuation { continuation in
            SFSpeechRecognizer.requestAuthorization { status in
                continuation.resume(returning: status == .authorized)
            }
        }
    }
    
    private func updatePermissionState() {
        let microphoneStatus = audioSession.recordPermission
        let speechStatus = SFSpeechRecognizer.authorizationStatus()
        
        // Check if both are granted
        if microphoneStatus == .granted && speechStatus == .authorized {
            permissionState = .granted
            return
        }
        
        // Check if either is permanently denied
        if microphoneStatus == .denied || speechStatus == .denied {
            permissionState = .permanentlyDenied
            return
        }
        
        // Check if restricted (parental controls, MDM, etc.)
        if speechStatus == .restricted {
            permissionState = .permanentlyDenied
            return
        }
        
        // Otherwise, not yet requested or denied (can request again)
        permissionState = .denied
    }
}

/**
 * Permission states for microphone and speech recognition access.
 */
enum PermissionState {
    case unknown            // Initial state, not yet checked
    case granted            // Both permissions granted
    case denied             // One or both denied, can request again
    case permanentlyDenied  // One or both permanently denied, need settings
}