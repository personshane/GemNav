import SwiftUI

struct VoiceButton: View {
    let state: VoiceState
    let action: () -> Void
    var isEnabled: Bool = true
    
    @State private var pulseScale: CGFloat = 1.0
    
    var body: some View {
        Button(action: {
            if isEnabled {
                action()
            }
        }) {
            ZStack {
                Circle()
                    .fill(buttonColor)
                    .frame(width: 56, height: 56)
                    .scaleEffect(state == .listening ? pulseScale : 1.0)
                
                if state == .processing {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                } else {
                    Image(systemName: "mic.fill")
                        .font(.system(size: 24))
                        .foregroundColor(.white)
                }
            }
        }
        .disabled(!isEnabled)
        .onAppear {
            if state == .listening {
                withAnimation(
                    Animation.easeInOut(duration: 0.8)
                        .repeatForever(autoreverses: true)
                ) {
                    pulseScale = 1.15
                }
            }
        }
        .onChange(of: state) { newState in
            if newState == .listening {
                withAnimation(
                    Animation.easeInOut(duration: 0.8)
                        .repeatForever(autoreverses: true)
                ) {
                    pulseScale = 1.15
                }
            } else {
                pulseScale = 1.0
            }
        }
    }
    
    private var buttonColor: Color {
        switch state {
        case .idle:
            return .blue
        case .listening:
            return Color(red: 0.9, green: 0.22, blue: 0.21)
        case .processing:
            return .purple
        case .speaking:
            return .orange
        case .error:
            return .red
        }
    }
}