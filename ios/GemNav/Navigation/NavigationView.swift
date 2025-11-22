import SwiftUI
import MapKit

struct NavigationView: View {
    @StateObject var viewModel: NavigationViewModel
    @State private var showPermissionSheet = false
    
    var body: some View {
        ZStack {
            // Map view based on tier
            mapView
            
            VStack {
                // Wake word indicator (Plus/Pro)
                if ["plus", "pro"].contains(viewModel.tier) && viewModel.voiceState.wakeWordActive {
                    WakeWordIndicator()
                        .padding(.top, 16)
                }
                
                Spacer()
                
                // Navigation info and controls
                VStack(spacing: 16) {
                    NavigationInfoCard(viewModel: viewModel)
                        .padding(.horizontal)
                    
                    navigationControls
                        .padding(.bottom)
                }
            }
            
            // Voice feedback overlay
            if viewModel.voiceState.feedbackMessage != nil {
                VoiceFeedbackOverlay(
                    message: viewModel.voiceState.feedbackMessage ?? "",
                    type: viewModel.voiceState.feedbackType,
                    onDismiss: viewModel.clearVoiceFeedback
                )
            }
        }
        .sheet(isPresented: $showPermissionSheet) {
            VoicePermissionView(
                onGranted: {
                    viewModel.onVoicePermissionGranted()
                    showPermissionSheet = false
                },
                onDenied: {
                    showPermissionSheet = false
                }
            )
        }
        .onAppear {
            viewModel.initializeVoice()
        }
    }
    
    // MARK: - Map View
    
    @ViewBuilder
    private var mapView: some View {
        switch viewModel.tier {
        case "free":
            FreeTierNavigationMessage()
        case "plus":
            GoogleMapsNavigationView(viewModel: viewModel)
        case "pro":
            HERENavigationView(viewModel: viewModel)
        default:
            Text("Unknown tier")
                .font(.headline)
                .foregroundColor(.secondary)
        }
    }
    
    // MARK: - Navigation Controls
    
    private var navigationControls: some View {
        HStack(spacing: 20) {
            // Recenter button
            Button(action: {
                viewModel.recenterMap()
            }) {
                Image(systemName: "location.fill")
                    .font(.title2)
                    .foregroundColor(.white)
                    .frame(width: 56, height: 56)
                    .background(Color.blue)
                    .clipShape(Circle())
                    .shadow(radius: 4)
            }
            
            // Voice button
            VoiceButton(
                isListening: viewModel.voiceState.isListening,
                isProcessing: viewModel.voiceState.isProcessing,
                onTap: {
                    if viewModel.voiceState.permissionGranted {
                        if viewModel.voiceState.isListening {
                            viewModel.stopVoiceListening()
                        } else {
                            viewModel.startVoiceListening()
                        }
                    } else {
                        showPermissionSheet = true
                    }
                }
            )
            
            // Mute button
            Button(action: {
                viewModel.setVoiceGuidanceMuted(!viewModel.isVoiceGuidanceMuted)
            }) {
                Image(systemName: viewModel.isVoiceGuidanceMuted ? "speaker.slash.fill" : "speaker.wave.2.fill")
                    .font(.title2)
                    .foregroundColor(.white)
                    .frame(width: 56, height: 56)
                    .background(Color.blue)
                    .clipShape(Circle())
                    .shadow(radius: 4)
            }
        }
        .padding(.horizontal)
    }
}

// MARK: - Placeholder Views

struct FreeTierNavigationMessage: View {
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "map.fill")
                .font(.system(size: 60))
                .foregroundColor(.blue)
            
            Text("Navigation Active")
                .font(.title2)
                .fontWeight(.bold)
            
            Text("Follow directions in Google Maps app")
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(.systemBackground))
    }
}

struct GoogleMapsNavigationView: View {
    @ObservedObject var viewModel: NavigationViewModel
    
    var body: some View {
        // Placeholder for Google Maps SDK integration
        ZStack {
            Color.gray.opacity(0.3)
            
            VStack {
                Text("Google Maps Navigation")
                    .font(.headline)
                Text("Plus Tier - In-App Navigation")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
    }
}

struct HERENavigationView: View {
    @ObservedObject var viewModel: NavigationViewModel
    
    var body: some View {
        // Placeholder for HERE SDK integration
        ZStack {
            Color.green.opacity(0.2)
            
            VStack {
                Text("HERE Navigation")
                    .font(.headline)
                Text("Pro Tier - Commercial Routing")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
    }
}

struct NavigationInfoCard: View {
    @ObservedObject var viewModel: NavigationViewModel
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if let instruction = viewModel.currentInstruction {
                Text(instruction.text)
                    .font(.headline)
            }
            
            HStack {
                if let distance = viewModel.distanceToNextTurn {
                    Label(distance, systemImage: "arrow.right")
                }
                
                Spacer()
                
                if let eta = viewModel.estimatedTimeOfArrival {
                    Label(eta, systemImage: "clock")
                }
            }
            .font(.subheadline)
            .foregroundColor(.secondary)
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(radius: 4)
    }
}
