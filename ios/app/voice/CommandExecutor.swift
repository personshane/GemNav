import Foundation

class CommandExecutor {
    private let navigationViewModel: NavigationViewModel
    private let searchViewModel: SearchViewModel
    private let tier: SubscriptionTier
    
    init(
        navigationViewModel: NavigationViewModel,
        searchViewModel: SearchViewModel,
        tier: SubscriptionTier
    ) {
        self.navigationViewModel = navigationViewModel
        self.searchViewModel = searchViewModel
        self.tier = tier
    }
    
    // MARK: - Execute Command
    
    func execute(command: VoiceCommand) async -> CommandResult {
        switch command {
        // Navigation commands
        case .navigate(let destination, let waypoints):
            return await executeNavigate(destination: destination, waypoints: waypoints)
        case .addStop(let location):
            return await executeAddStop(location: location)
        case .cancelNavigation:
            return executeCancelNavigation()
        case .recenterMap:
            return executeRecenterMap()
            
        // Audio commands
        case .muteVoice:
            return executeMute()
        case .unmuteVoice:
            return executeUnmute()
        case .repeatInstruction:
            return executeRepeat()
            
        // Information queries
        case .getETA:
            return executeGetETA()
        case .getDistanceRemaining:
            return executeGetDistance()
        case .getTrafficStatus:
            return executeGetTraffic()
        case .searchAlongRoute(let query, let filters):
            return await executeSearchAlongRoute(query: query, filters: filters)
            
        // Route modifications
        case .avoidRouteFeature(let feature):
            return await executeAvoidFeature(feature: feature)
        case .showAlternativeRoutes:
            return executeShowAlternatives()
        case .optimizeRoute(let criterion):
            return await executeOptimizeRoute(criterion: criterion)
            
        // Truck-specific
        case .findTruckPOI(let type):
            return await executeFindTruckPOI(type: type)
        case .checkBridgeClearances:
            return executeCheckBridges()
        case .checkHeightRestrictions:
            return executeCheckHeights()
        case .checkWeightLimits:
            return executeCheckWeights()
        case .findWeighStations:
            return await executeFindWeighStations()
            
        // Conversation
        case .clarification(let question):
            return .needsClarification(question: question)
        case .unknown:
            return .error(message: "I didn't understand that command. Try saying 'navigate to' followed by a destination.")
        }
    }
    
    // MARK: - Navigation Commands
    
    private func executeNavigate(destination: String, waypoints: [String]) async -> CommandResult {
        // Multi-waypoint navigation requires Plus/Pro
        if !waypoints.isEmpty && !tier.allowsAdvancedVoice {
            return .tierRestricted(message: "Multi-stop navigation requires GemNav Plus. Upgrade to add waypoints.")
        }
        
        do {
            try await navigationViewModel.navigateTo(destination: destination, waypoints: waypoints)
            
            let message = waypoints.isEmpty
                ? "Navigating to \(destination)"
                : "Navigating to \(destination) with \(waypoints.count) stops"
            
            return .success(message: message)
        } catch {
            return .error(message: "Unable to navigate to \(destination)")
        }
    }
    
    private func executeAddStop(location: String) async -> CommandResult {
        guard tier.allowsAdvancedVoice else {
            return .tierRestricted(message: "Adding stops requires GemNav Plus")
        }
        
        do {
            try await navigationViewModel.addWaypoint(location: location)
            return .success(message: "Added stop at \(location)")
        } catch {
            return .error(message: "Unable to add stop")
        }
    }
    
    private func executeCancelNavigation() -> CommandResult {
        navigationViewModel.cancelNavigation()
        return .success(message: "Navigation cancelled")
    }
    
    private func executeRecenterMap() -> CommandResult {
        navigationViewModel.recenterMap()
        return .success(message: "Map recentered")
    }
    
    // MARK: - Audio Commands
    
    private func executeMute() -> CommandResult {
        navigationViewModel.setVoiceGuidanceMuted(true)
        return .success(message: "Voice guidance muted")
    }
    
    private func executeUnmute() -> CommandResult {
        navigationViewModel.setVoiceGuidanceMuted(false)
        return .success(message: "Voice guidance unmuted")
    }
    
    private func executeRepeat() -> CommandResult {
        let instruction = navigationViewModel.getCurrentInstruction()
        return instruction.isEmpty
            ? .error(message: "No instruction to repeat")
            : .success(message: instruction)
    }
    
    // MARK: - Information Queries
    
    private func executeGetETA() -> CommandResult {
        guard let eta = navigationViewModel.getEstimatedArrival() else {
            return .error(message: "No active navigation")
        }
        return .success(message: "You'll arrive at \(eta.formattedTime)")
    }
    
    private func executeGetDistance() -> CommandResult {
        guard let distance = navigationViewModel.getRemainingDistance() else {
            return .error(message: "No active navigation")
        }
        return .success(message: "\(distance.formattedDistance) remaining")
    }
    
    private func executeGetTraffic() -> CommandResult {
        guard tier.allowsAdvancedVoice else {
            return .tierRestricted(message: "Traffic status requires GemNav Plus")
        }
        
        let trafficInfo = navigationViewModel.getTrafficStatus()
        return .success(message: trafficInfo)
    }
    
    private func executeSearchAlongRoute(query: String, filters: [String: String]) async -> CommandResult {
        guard tier.allowsAdvancedVoice else {
            return .tierRestricted(message: "Advanced search requires GemNav Plus")
        }
        
        do {
            let results = try await searchViewModel.searchAlongRoute(query: query, filters: filters)
            
            if results.isEmpty {
                return .success(message: "No results found for \(query)")
            } else {
                return .success(
                    message: "Found \(results.count) \(query) along your route",
                    data: results
                )
            }
        } catch {
            return .error(message: "Unable to search for \(query)")
        }
    }
    
    // MARK: - Route Modifications
    
    private func executeAvoidFeature(feature: RouteFeature) async -> CommandResult {
        guard tier.allowsAdvancedVoice else {
            return .tierRestricted(message: "Route customization requires GemNav Plus")
        }
        
        await navigationViewModel.setRoutePreference(feature: feature, avoid: true)
        
        let featureName: String
        switch feature {
        case .tolls: featureName = "tolls"
        case .highways: featureName = "highways"
        case .ferries: featureName = "ferries"
        case .unpavedRoads: featureName = "unpaved roads"
        }
        
        return .success(message: "Route updated to avoid \(featureName)")
    }
    
    private func executeShowAlternatives() -> CommandResult {
        guard tier.allowsAdvancedVoice else {
            return .tierRestricted(message: "Alternative routes require GemNav Plus")
        }
        
        navigationViewModel.showAlternativeRoutes()
        return .success(message: "Showing alternative routes")
    }
    
    private func executeOptimizeRoute(criterion: OptimizationCriterion) async -> CommandResult {
        guard tier.allowsAdvancedVoice else {
            return .tierRestricted(message: "Route optimization requires GemNav Plus")
        }
        
        await navigationViewModel.optimizeRoute(criterion: criterion)
        
        let criterionName: String
        switch criterion {
        case .fastest: criterionName = "fastest route"
        case .shortest: criterionName = "shortest distance"
        case .ecoFriendly: criterionName = "eco-friendly route"
        case .avoidTraffic: criterionName = "traffic avoidance"
        }
        
        return .success(message: "Route optimized for \(criterionName)")
    }
    
    // MARK: - Truck-Specific Commands (Pro Tier)
    
    private func executeFindTruckPOI(type: TruckPOIType) async -> CommandResult {
        guard tier == .pro else {
            return .tierRestricted(message: "Truck features require GemNav Pro")
        }
        
        do {
            let results = try await searchViewModel.findTruckPOI(type: type)
            
            if results.isEmpty {
                return .success(message: "No \(type.displayName) found along your route")
            } else {
                return .success(
                    message: "Found \(results.count) \(type.displayName)",
                    data: results
                )
            }
        } catch {
            return .error(message: "Unable to find \(type.displayName)")
        }
    }
    
    private func executeCheckBridges() -> CommandResult {
        guard tier == .pro else {
            return .tierRestricted(message: "Bridge clearance checks require GemNav Pro")
        }
        
        let clearanceInfo = navigationViewModel.getBridgeClearances()
        return .success(message: clearanceInfo)
    }
    
    private func executeCheckHeights() -> CommandResult {
        guard tier == .pro else {
            return .tierRestricted(message: "Height restriction checks require GemNav Pro")
        }
        
        let heightInfo = navigationViewModel.getHeightRestrictions()
        return .success(message: heightInfo)
    }
    
    private func executeCheckWeights() -> CommandResult {
        guard tier == .pro else {
            return .tierRestricted(message: "Weight limit checks require GemNav Pro")
        }
        
        let weightInfo = navigationViewModel.getWeightLimits()
        return .success(message: weightInfo)
    }
    
    private func executeFindWeighStations() async -> CommandResult {
        guard tier == .pro else {
            return .tierRestricted(message: "Weigh station finder requires GemNav Pro")
        }
        
        do {
            let results = try await searchViewModel.findTruckPOI(type: .weighStation)
            return .success(
                message: "Found \(results.count) weigh stations ahead",
                data: results
            )
        } catch {
            return .error(message: "Unable to find weigh stations")
        }
    }
}
