import SwiftUI

/// Visual indicator for wake word detection status on iOS
/// Shows "Hey GemNav" listening status for Plus/Pro tiers
struct WakeWordIndicator: View {
    let isActive: Bool
    
    var body: some View {
        if isActive {
            HStack(spacing: 8) {
                Image(systemName: "waveform")
                    .font(.system(size: 16))
                    .foregroundColor(.blue)
                
                Text("\"Hey GemNav\" listening")
                    .font(.caption)
                    .foregroundColor(.blue)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(
                Capsule()
                    .fill(Color.blue.opacity(0.1))
            )
            .transition(.opacity.combined(with: .scale))
        }
    }
}
