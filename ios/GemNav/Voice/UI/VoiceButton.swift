import SwiftUI

/// Voice input button with animated states for iOS
/// 
/// States:
/// - Idle: Gray microphone icon
/// - Listening: Pulsing red animation
/// - Processing: Spinner with mic icon
/// - Error: Red background with shake animation
struct VoiceButton: View {
    let state: VoiceButtonState
    let enabled: Bool
    let action: () -> Void
    
    @State private var scale: CGFloat = 1.0
    @State private var rotation: Double = 0.0
    
    init(
        state: VoiceButtonState,
        enabled: Bool = true,
        action: @escaping () -> Void
    ) {
        self.state = state
        self.enabled = enabled
        self.action = action
    }
    
    var body: some View {
        Button(action: action) {
            ZStack {
                Circle()
                    .fill(backgroundColor)
                    .frame(width: 64, height: 64)
                    .opacity(enabled ? 1.0 : 0.5)
                
                if state == .processing {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: iconColor))
                        .scaleEffect(1.5)
                } else {
                    Image(systemName: "mic.fill")
                        .font(.system(size: 28))
                        .foregroundColor(iconColor)
                }
            }
            .scaleEffect(scale)
            .rotationEffect(.degrees(rotation))
        }
        .disabled(!enabled)
        .buttonStyle(PlainButtonStyle())
        .onAppear {
            if state == .listening {
                startPulseAnimation()
            }
        }
        .onChange(of: state) { newState in
            switch newState {
            case .listening:
                startPulseAnimation()
            case .error:
                startShakeAnimation()
            default:
                scale = 1.0
                rotation = 0.0
            }
        }
    }
    
    private var backgroundColor: Color {
        switch state {
        case .idle:
            return Color(.systemGray5)
        case .listening:
            return Color.red
        case .processing:
            return Color.blue
        case .error:
            return Color.red
        }
    }
    
    private var iconColor: Color {
        switch state {
        case .idle:
            return Color(.systemGray)
        default:
            return Color.white
        }
    }
    
    private func startPulseAnimation() {
        withAnimation(
            Animation.easeInOut(duration: 0.6)
                .repeatForever(autoreverses: true)
        ) {
            scale = 1.2
        }
    }
    
    private func startShakeAnimation() {
        withAnimation(Animation.default.repeatCount(3, autoreverses: true)) {
            rotation = 5
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) {
            rotation = 0
        }
    }
}

/// Voice button states for iOS
enum VoiceButtonState {
    case idle       // Ready for input
    case listening  // Currently listening to user
    case processing // Processing voice command
    case error      // Error occurred
}
