import SwiftUI

/// Full-screen overlay for voice feedback on iOS
/// Shows listening/processing/speaking/error states with animations
struct VoiceFeedbackOverlay: View {
    let state: VoiceFeedbackState
    let transcript: String
    let response: String
    let onDismiss: () -> Void
    
    var body: some View {
        ZStack {
            // Semi-transparent background
            Color.black.opacity(0.7)
                .ignoresSafeArea()
                .onTapGesture {
                    onDismiss()
                }
            
            // Content card
            VStack(spacing: 24) {
                // State-specific icon and animation
                stateContent
                
                // Transcript display
                if !transcript.isEmpty {
                    Divider()
                    VStack(spacing: 8) {
                        Text("You said:")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text(transcript)
                            .font(.body)
                            .multilineTextAlignment(.center)
                    }
                }
                
                // Response display
                if !response.isEmpty {
                    Divider()
                    Text(response)
                        .font(.body)
                        .multilineTextAlignment(.center)
                }
            }
            .padding(32)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(Color(.systemBackground))
            )
            .padding(32)
        }
    }
    
    @ViewBuilder
    private var stateContent: some View {
        switch state {
        case .listening:
            VStack(spacing: 16) {
                PulsingMicIcon()
                Text("Listening...")
                    .font(.title2)
                    .foregroundColor(.blue)
            }
        case .processing:
            VStack(spacing: 16) {
                ProgressView()
                    .scaleEffect(1.5)
                Text("Processing command...")
                    .font(.title2)
            }
        case .speaking:
            VStack(spacing: 16) {
                SoundWaveAnimation()
                Text("GemNav")
                    .font(.title2)
                    .foregroundColor(.blue)
            }
        case .error(let message):
            VStack(spacing: 16) {
                Image(systemName: "exclamationmark.triangle.fill")
                    .font(.system(size: 56))
                    .foregroundColor(.red)
                Text("Error")
                    .font(.title2)
                    .foregroundColor(.red)
                Text(message)
                    .font(.body)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
        }
    }
}

/// Pulsing microphone icon animation for iOS
struct PulsingMicIcon: View {
    @State private var scale: CGFloat = 1.0
    
    var body: some View {
        Image(systemName: "mic.fill")
            .font(.system(size: 56))
            .foregroundColor(.blue)
            .scaleEffect(scale)
            .onAppear {
                withAnimation(
                    Animation.easeInOut(duration: 0.8)
                        .repeatForever(autoreverses: true)
                ) {
                    scale = 1.3
                }
            }
    }
}

/// Sound wave animation for speaking state on iOS
struct SoundWaveAnimation: View {
    @State private var heights: [CGFloat] = [20, 30, 40, 30, 20]
    
    var body: some View {
        HStack(spacing: 4) {
            ForEach(0..<5) { index in
                RoundedRectangle(cornerRadius: 4)
                    .fill(Color.blue)
                    .frame(width: 8, height: heights[index])
                    .animation(
                        Animation.easeInOut(duration: 0.6)
                            .repeatForever(autoreverses: true)
                            .delay(Double(index) * 0.1),
                        value: heights[index]
                    )
            }
        }
        .frame(height: 56)
        .onAppear {
            for i in 0..<5 {
                heights[i] = CGFloat.random(in: 20...56)
            }
        }
    }
}

/// Voice feedback states for iOS
enum VoiceFeedbackState {
    case listening
    case processing
    case speaking
    case error(String)
}
