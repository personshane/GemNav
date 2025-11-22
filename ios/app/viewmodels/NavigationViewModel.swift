import Foundation
import Combine
import CoreLocation

// MARK: - State Models

struct NavigationState {
    var tier: String = "free"
    var destination: String?
    var currentLocation: CLLocationCoordinate2D?
    var route: [CLLocationCoordinate2D]?
    var nextInstruction: String?
    var distanceToNextTurn: String?
    var eta: String?
    var remainingDistance: String?
    var voiceGuidanceMuted: Bool = false
    var activeWaypoints: [String] = []
    var routePreferences: Set<RouteFeature> = []
    var trafficStatus: TrafficStatus?
    var isMapReady: Bool = false
    var error: String?
}

enum FeedbackType {
    case info, success, warning, error
}

struct VoiceState {
    var isListening: Bool = false
    var isProcessing: Bool = false
    var wakeWordActive: Bool = false
    var permissionGranted: Bool = false
    var feedbackMessage: String? = nil
    var feedbackType: FeedbackType = .info
}

struct ETAInfo {
    let formattedTime: String
    let timestamp: TimeInterval
}

struct DistanceInfo {
    let formattedDistance: String
    let meters: Int
}

enum RouteFeature: Hashable {
    case tolls, highways, ferries, unpavedRoads
}

enum OptimizationCriterion {
    case fastest, shortest, ecoFriendly, avoidTraffic
}

enum TrafficStatus {
    case clear, light, moderate, heavy
}

struct NavigationInstruction {
    let text: String
    let location: CLLocationCoordinate2D
    let distanceText: String
}

// MARK: - NavigationViewModel

@MainActor
class NavigationViewModel: ObservableObject {
    @Published private(set) var state = NavigationState()
    @Published var voiceState = VoiceState()
    
    private var voiceCommandManager: VoiceCommandManager?
    private var cancellables = Set<AnyCancellable>()
    
    private var routePoints: [CLLocationCoordinate2D] = []
    private var instructions: [NavigationInstruction] = []
    private var currentInstructionIndex = 0
    private var lastSpokenInstruction: String?
    private var isTtsReady = false
    
    private let dateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "h:mm a"
        return formatter
    }()
    
    // Convenience properties for NavigationView
    var tier: String { state.tier }
    var destination: String? { state.destination }
    var currentLocation: CLLocationCoordinate2D? { state.currentLocation }
    var nextInstruction: String? { state.nextInstruction }
    var distanceToNextTurn: String? { state.distanceToNextTurn }
    var eta: String? { state.eta }
    var remainingDistance: String? { state.remainingDistance }
    var isMuted: Bool { state.voiceGuidanceMuted }
    
    // MARK: - Navigation Control
    
    func navigateTo(destination: String, waypoints: [String] = []) async throws {
        state.destination = destination
        state.activeWaypoints = waypoints
        
        // TODO: Trigger route calculation with routing service
        // For now, just update state
    }
    
    func addWaypoint(_ location: String) async throws {
        state.activeWaypoints.append(location)
        
        // TODO: Recalculate route with new waypoint
    }
    
    func cancelNavigation() {
        state.destination = nil
        state.route = nil
        state.activeWaypoints = []
        state.nextInstruction = nil
        state.distanceToNextTurn = nil
        state.eta = nil
        state.remainingDistance = nil
        routePoints = []
        instructions = []
        currentInstructionIndex = 0
    }
    
    // MARK: - Voice Guidance Control
    
    func setVoiceGuidanceMuted(_ muted: Bool) {
        state.voiceGuidanceMuted = muted
    }
    
    func getCurrentInstruction() -> String {
        guard currentInstructionIndex < instructions.count else {
            return ""
        }
        return instructions[currentInstructionIndex].text
    }
    
    func getEstimatedArrival() -> ETAInfo? {
        guard let eta = state.eta else { return nil }
        
        return ETAInfo(
            formattedTime: eta,
            timestamp: Date().timeIntervalSince1970
        )
    }
    
    func getRemainingDistance() -> DistanceInfo? {
        guard let remaining = state.remainingDistance else { return nil }
        
        let meters = parseDistanceToMeters(remaining)
        return DistanceInfo(
            formattedDistance: remaining,
            meters: meters
        )
    }
    
    // MARK: - Traffic and Route Info (Plus tier)
    
    func getTrafficStatus() -> String {
        guard let status = state.trafficStatus else {
            return "No traffic data available"
        }
        
        switch status {
        case .clear:
            return "Traffic is clear ahead"
        case .light:
            return "Light traffic conditions"
        case .moderate:
            return "Moderate traffic ahead"
        case .heavy:
            return "Heavy traffic on your route"
        }
    }
    
    func showAlternativeRoutes() {
        // TODO: Trigger alternative routes UI
        // This would be handled by the SwiftUI view
    }
    
    func setRoutePreference(_ feature: RouteFeature, avoid: Bool) async {
        if avoid {
            state.routePreferences.insert(feature)
        } else {
            state.routePreferences.remove(feature)
        }
        
        // TODO: Recalculate route with preferences
    }
    
    func optimizeRoute(_ criterion: OptimizationCriterion) async {
        // TODO: Recalculate route with optimization criterion
        // This would interact with the routing service (Google or HERE)
    }
    
    // MARK: - Truck-Specific (Pro tier)
    
    func getBridgeClearances() -> String {
        // TODO: Query HERE SDK for bridge clearances on route
        return "No low bridges on your route"
    }
    
    func getHeightRestrictions() -> String {
        // TODO: Query HERE SDK for height restrictions
        return "No height restrictions on your route"
    }
    
    func getWeightLimits() -> String {
        // TODO: Query HERE SDK for weight limits
        return "No weight restrictions on your route"
    }
    
    // MARK: - Location Updates
    
    func updateLocation(_ location: CLLocation) {
        state.currentLocation = location.coordinate
        updateNavigationProgress(location.coordinate)
    }
    
    private func updateNavigationProgress(_ currentLocation: CLLocationCoordinate2D) {
        guard !instructions.isEmpty else { return }
        
        // Find next instruction based on proximity
        var closestIndex = currentInstructionIndex
        var closestDistance = Double.greatestFiniteMagnitude
        
        for i in currentInstructionIndex..<instructions.count {
            let distance = calculateDistance(from: currentLocation, to: instructions[i].location)
            if distance < closestDistance {
                closestDistance = distance
                closestIndex = i
            }
        }
        
        // Update if we've progressed
        if closestIndex > currentInstructionIndex {
            currentInstructionIndex = closestIndex
        }
        
        // Update current instruction if within threshold (50 meters)
        if closestDistance < 50 && currentInstructionIndex < instructions.count {
            let instruction = instructions[currentInstructionIndex]
            state.nextInstruction = instruction.text
            state.distanceToNextTurn = instruction.distanceText
        }
        
        // Calculate remaining distance
        calculateRemainingDistance(currentLocation)
    }
    
    private func calculateRemainingDistance(_ currentLocation: CLLocationCoordinate2D) {
        guard !routePoints.isEmpty else { return }
        
        // Find closest point on route
        var closestIndex = 0
        var closestDistance = Double.greatestFiniteMagnitude
        
        for i in 0..<routePoints.count {
            let distance = calculateDistance(from: currentLocation, to: routePoints[i])
            if distance < closestDistance {
                closestDistance = distance
                closestIndex = i
            }
        }
        
        // Sum remaining distance
        var remaining = 0.0
        for i in closestIndex..<(routePoints.count - 1) {
            remaining += calculateDistance(from: routePoints[i], to: routePoints[i + 1])
        }
        
        state.remainingDistance = formatDistance(Int(remaining))
    }
    
    // MARK: - Helper Methods
    
    private func parseDistanceToMeters(_ distanceString: String) -> Int {
        if distanceString.contains("km") {
            let value = distanceString.replacingOccurrences(of: Regex("[^0-9.]"), with: "")
            if let km = Double(value) {
                return Int(km * 1000)
            }
        } else if distanceString.contains("m") {
            let value = distanceString.replacingOccurrences(of: Regex("[^0-9]"), with: "")
            if let meters = Int(value) {
                return meters
            }
        }
        return 0
    }
    
    private func calculateDistance(from: CLLocationCoordinate2D, to: CLLocationCoordinate2D) -> Double {
        let earthRadius = 6371000.0 // meters
        
        let lat1 = from.latitude * .pi / 180
        let lat2 = to.latitude * .pi / 180
        let dLat = (to.latitude - from.latitude) * .pi / 180
        let dLng = (to.longitude - from.longitude) * .pi / 180
        
        let a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1) * cos(lat2) *
                sin(dLng / 2) * sin(dLng / 2)
        
        let c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadius * c
    }
    
    private func formatDistance(_ meters: Int) -> String {
        if meters < 1000 {
            return "\(meters)m"
        } else {
            let km = Double(meters) / 1000.0
            return String(format: "%.1f km", km)
        }
    }
    
    func recenterMap() {
        // Trigger recentering via state change
        // UI will observe currentLocation changes
    }
    
    func onMapReady() {
        state.isMapReady = true
    }
    
    func onNavigationError(_ message: String) {
        state.error = message
    }
    
    func clearError() {
        state.error = nil
    }
    
    // MARK: - Voice Integration
    
    func initializeVoice() {
        voiceCommandManager = VoiceCommandManager(tier: state.tier)
        voiceCommandManager?.initialize()
        observeVoiceCommands()
        checkMicrophonePermission()
    }
    
    func toggleMute() {
        state.voiceGuidanceMuted.toggle()
    }
    
    private func observeVoiceCommands() {
        voiceCommandManager?.commandPublisher
            .sink { [weak self] command in
                self?.handleVoiceCommand(command)
            }
            .store(in: &cancellables)
    }
    
    private func checkMicrophonePermission() {
        voiceState.permissionGranted = MicrophonePermissionManager.shared.hasPermission()
    }
    
    func startVoiceListening() {
        voiceCommandManager?.startListening()
        voiceState.isListening = true
    }
    
    func stopVoiceListening() {
        voiceCommandManager?.stopListening()
        voiceState.isListening = false
    }
    
    private func handleVoiceCommand(_ command: VoiceCommand) {
        voiceState.isProcessing = true
        
        defer {
            voiceState.isProcessing = false
        }
        
        switch command {
        case .navigateTo(let location):
            showVoiceFeedback("Searching for \(location)", type: .info)
            Task {
                do {
                    try await navigateTo(destination: location)
                    showVoiceFeedback("Route calculated", type: .success)
                } catch {
                    showVoiceFeedback("Failed to calculate route", type: .error)
                }
            }
            
        case .mute:
            setVoiceGuidanceMuted(true)
            showVoiceFeedback("Audio muted", type: .success)
            
        case .unmute:
            setVoiceGuidanceMuted(false)
            showVoiceFeedback("Audio unmuted", type: .success)
            
        case .recenter:
            recenterMap()
            showVoiceFeedback("Map recentered", type: .success)
            
        case .alternateRoute:
            showVoiceFeedback("Calculating alternate route", type: .info)
            showAlternativeRoutes()
            
        case .cancelNavigation:
            cancelNavigation()
            showVoiceFeedback("Navigation cancelled", type: .warning)
            
        default:
            showVoiceFeedback("Command not supported", type: .error)
        }
    }
    
    func onVoicePermissionGranted() {
        MicrophonePermissionManager.shared.requestPermission { [weak self] granted in
            DispatchQueue.main.async {
                self?.voiceState.permissionGranted = granted
                if granted {
                    self?.startVoiceListening()
                }
            }
        }
    }
    
    private func showVoiceFeedback(_ message: String, type: FeedbackType) {
        voiceState.feedbackMessage = message
        voiceState.feedbackType = type
    }
    
    func clearVoiceFeedback() {
        voiceState.feedbackMessage = nil
    }
}
