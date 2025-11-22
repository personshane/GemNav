import SwiftUI

struct VoicePermissionView: View {
    let onRequestPermission: () -> Void
    let onDismiss: () -> Void
    let showRationale: Bool
    
    var body: some View {
        VStack(spacing: 24) {
            Image(systemName: "mic.fill")
                .font(.system(size: 64))
                .foregroundColor(.blue)
            
            Text(showRationale ? "Microphone Permission Required" : "Enable Voice Commands")
                .font(.title2)
                .fontWeight(.bold)
                .multilineTextAlignment(.center)
            
            Text(permissionMessage)
                .font(.body)
                .multilineTextAlignment(.center)
                .foregroundColor(.secondary)
            
            if showRationale {
                Text("Please grant permission in Settings to use this feature.")
                    .font(.callout)
                    .multilineTextAlignment(.center)
                    .foregroundColor(.red)
            }
            
            HStack(spacing: 16) {
                Button(showRationale ? "Not Now" : "Cancel") {
                    onDismiss()
                }
                .buttonStyle(.bordered)
                
                Button(showRationale ? "Open Settings" : "Allow") {
                    onRequestPermission()
                }
                .buttonStyle(.borderedProminent)
            }
        }
        .padding(32)
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(radius: 10)
    }
    
    private var permissionMessage: String {
        if showRationale {
            return "GemNav needs microphone access to enable voice commands during navigation. " +
                   "Your voice is processed locally on your device (Free tier) or securely sent to " +
                   "Google Cloud (Plus/Pro tiers) for enhanced understanding."
        } else {
            return "Use voice commands to navigate hands-free. Say \"Navigate to [destination]\" " +
                   "or \"Find gas stations nearby\" without touching your phone."
        }
    }
}