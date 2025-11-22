import SwiftUI

/// Permission request view for voice input on iOS
/// Shows rationale and guides user through permission flow
struct VoicePermissionView: View {
    let permissionState: VoicePermissionState
    let onRequestPermission: () -> Void
    let onOpenSettings: () -> Void
    let onDismiss: () -> Void
    
    var body: some View {
        VStack(spacing: 24) {
            // Icon
            Image(systemName: "mic.fill")
                .font(.system(size: 64))
                .foregroundColor(.blue)
            
            // Title
            Text(titleText)
                .font(.title2)
                .fontWeight(.semibold)
                .multilineTextAlignment(.center)
            
            // Message
            Text(messageText)
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
            
            Spacer()
            
            // Action buttons
            VStack(spacing: 12) {
                Button(action: {
                    if permissionState == .permanentlyDenied {
                        onOpenSettings()
                    } else {
                        onRequestPermission()
                    }
                }) {
                    Text(buttonText)
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.blue)
                        .cornerRadius(12)
                }
                
                Button(action: onDismiss) {
                    Text("Not Now")
                        .font(.headline)
                        .foregroundColor(.blue)
                        .frame(maxWidth: .infinity)
                        .padding()
                }
            }
            .padding(.horizontal)
        }
        .padding()
    }
    
    private var titleText: String {
        switch permissionState {
        case .notRequested:
            return "Enable Voice Commands"
        case .denied:
            return "Microphone Access Required"
        case .permanentlyDenied:
            return "Enable in Settings"
        case .granted:
            return "Voice Commands"
        }
    }
    
    private var messageText: String {
        switch permissionState {
        case .notRequested:
            return "GemNav uses your microphone to enable hands-free voice commands during navigation. Your voice data is processed securely and never stored without your permission."
        case .denied:
            return "Voice commands require microphone access. Please grant permission to use this feature. You can always disable voice commands in settings."
        case .permanentlyDenied:
            return "Microphone permission was denied. To enable voice commands, please go to Settings > GemNav > Permissions and enable Microphone access."
        case .granted:
            return "Voice commands are enabled."
        }
    }
    
    private var buttonText: String {
        switch permissionState {
        case .permanentlyDenied:
            return "Open Settings"
        default:
            return "Grant Permission"
        }
    }
}

/// Voice permission states for iOS
enum VoicePermissionState {
    case notRequested       // Permission not yet requested
    case granted            // Permission granted
    case denied             // Permission denied but can be requested again
    case permanentlyDenied  // Permission permanently denied (user must go to settings)
}
