import SwiftUI

struct WakeWordIndicator: View {
    let isActive: Bool
    
    @State private var pulseOpacity: Double = 0.3
    
    var body: some View {
        if isActive {
            HStack(spacing: 6) {
                Image(systemName: "ear.fill")
                    .font(.system(size: 14))
                    .foregroundColor(.white)
                
                Text("Say \"Hey GemNav\"")
                    .font(.caption)
                    .fontWeight(.medium)
                    .foregroundColor(.white)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(
                Color.green
                    .opacity(pulseOpacity)
                    .cornerRadius(16)
            )
            .shadow(radius: 4)
            .onAppear {
                withAnimation(
                    Animation.easeInOut(duration: 1.5)
                        .repeatForever(autoreverses: true)
                ) {
                    pulseOpacity = 1.0
                }
            }
        }
    }
}