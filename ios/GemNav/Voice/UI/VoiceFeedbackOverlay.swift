import SwiftUI

struct VoiceFeedbackOverlay: View {
    let state: VoiceState
    let transcript: String
    let onDismiss: () -> Void
    
    var body: some View {
        if state != .idle {
            ZStack {
                Color.black.opacity(0.6)
                    .ignoresSafeArea()
                    .onTapGesture {
                        if case .error = state {
                            onDismiss()
                        }
                    }
                
                VStack(spacing: 24) {
                    stateIcon
                    stateTitle
                    
                    if !transcript.isEmpty {
                        Divider()
                            .background(Color.gray)
                        Text(transcript)
                            .font(.body)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                    }
                    
                    if case .error = state {
                        Button("Dismiss") {
                            onDismiss()
                        }
                        .buttonStyle(.borderedProminent)
                    }
                }
                .padding(32)
                .frame(maxWidth: 400)
                .background(Color(.systemBackground))
                .cornerRadius(16)
                .shadow(radius: 10)
            }
            .transition(.opacity.combined(with: .move(edge: .bottom)))
            .animation(.easeInOut, value: state)
        }
    }
    
    @ViewBuilder
    private var stateIcon: some View {
        switch state {
        case .listening:
            Image(systemName: "mic.fill")
                .font(.system(size: 48))
                .foregroundColor(Color(red: 0.9, green: 0.22, blue: 0.21))
        case .processing:
            ProgressView()
                .scaleEffect(1.5)
        case .speaking:
            Image(systemName: "speaker.wave.3.fill")
                .font(.system(size: 48))
                .foregroundColor(.orange)
        case .error(let message):
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 48))
                .foregroundColor(.red)
        default:
            EmptyView()
        }
    }
    
    @ViewBuilder
    private var stateTitle: some View {
        switch state {
        case .listening:
            Text("Listening...")
                .font(.title2)
                .fontWeight(.bold)
        case .processing:
            Text("Processing...")
                .font(.title2)
                .fontWeight(.bold)
        case .speaking:
            Text("Speaking...")
                .font(.title2)
                .fontWeight(.bold)
        case .error(let message):
            VStack(spacing: 8) {
                Text("Error")
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundColor(.red)
                Text(message)
                    .font(.body)
                    .multilineTextAlignment(.center)
            }
        default:
            EmptyView()
        }
    }
}