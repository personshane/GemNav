package com.gemnav.app.voice

import com.gemnav.app.navigation.NavigationViewModel
import com.gemnav.app.search.SearchViewModel
import javax.inject.Inject

/**
 * Executes parsed voice commands by routing them to appropriate app components
 */
class CommandExecutor @Inject constructor(
    private val navigationViewModel: NavigationViewModel,
    private val searchViewModel: SearchViewModel,
    private val tier: SubscriptionTier
) {
    /**
     * Execute a voice command and return result
     */
    suspend fun execute(command: VoiceCommand): CommandResult {
        return when (command) {
            // Navigation commands
            is VoiceCommand.Navigate -> executeNavigate(command)
            is VoiceCommand.AddStop -> executeAddStop(command)
            is VoiceCommand.CancelNavigation -> executeCancelNavigation()
            is VoiceCommand.RecenterMap -> executeRecenterMap()
            
            // Audio commands
            is VoiceCommand.MuteVoice -> executeMute()
            is VoiceCommand.UnmuteVoice -> executeUnmute()
            is VoiceCommand.RepeatInstruction -> executeRepeat()
            
            // Information queries
            is VoiceCommand.GetETA -> executeGetETA()
            is VoiceCommand.GetDistanceRemaining -> executeGetDistance()
            is VoiceCommand.GetTrafficStatus -> executeGetTraffic()
            is VoiceCommand.SearchAlongRoute -> executeSearchAlongRoute(command)
            
            // Route modifications
            is VoiceCommand.AvoidRouteFeature -> executeAvoidFeature(command)
            is VoiceCommand.ShowAlternativeRoutes -> executeShowAlternatives()
            is VoiceCommand.OptimizeRoute -> executeOptimizeRoute(command)
            
            // Truck-specific (Pro tier)
            is VoiceCommand.FindTruckPOI -> executeFindTruckPOI(command)
            is VoiceCommand.CheckBridgeClearances -> executeCheckBridges()
            is VoiceCommand.CheckHeightRestrictions -> executeCheckHeights()
            is VoiceCommand.CheckWeightLimits -> executeCheckWeights()
            is VoiceCommand.FindWeighStations -> executeFindWeighStations()
            
            // Conversation
            is VoiceCommand.Clarification -> CommandResult.NeedsClarification(command.question)
            is VoiceCommand.Unknown -> CommandResult.Error("I didn't understand that command. Try saying 'navigate to' followed by a destination.")
        }
    }
    
    private suspend fun executeNavigate(command: VoiceCommand.Navigate): CommandResult {
        // Multi-waypoint navigation requires Plus/Pro
        if (command.waypoints.isNotEmpty() && !tier.allowsAdvancedVoice()) {
            return CommandResult.TierRestricted(
                "Multi-stop navigation requires GemNav Plus. Upgrade to add waypoints."
            )
        }
        
        return try {
            navigationViewModel.navigateTo(command.destination, command.waypoints)
            
            val message = if (command.waypoints.isEmpty()) {
                "Navigating to ${command.destination}"
            } else {
                "Navigating to ${command.destination} with ${command.waypoints.size} stops"
            }
            
            CommandResult.Success(message)
        } catch (e: Exception) {
            CommandResult.Error("Unable to navigate to ${command.destination}")
        }
    }
    
    private suspend fun executeAddStop(command: VoiceCommand.AddStop): CommandResult {
        if (!tier.allowsAdvancedVoice()) {
            return CommandResult.TierRestricted(
                "Adding stops requires GemNav Plus"
            )
        }
        
        return try {
            navigationViewModel.addWaypoint(command.location)
            CommandResult.Success("Added stop at ${command.location}")
        } catch (e: Exception) {
            CommandResult.Error("Unable to add stop")
        }
    }
    
    private fun executeCancelNavigation(): CommandResult {
        navigationViewModel.cancelNavigation()
        return CommandResult.Success("Navigation cancelled")
    }
    
    private fun executeRecenterMap(): CommandResult {
        navigationViewModel.recenterMap()
        return CommandResult.Success("Map recentered")
    }
    
    private fun executeMute(): CommandResult {
        navigationViewModel.setVoiceGuidanceMuted(true)
        return CommandResult.Success("Voice guidance muted")
    }
    
    private fun executeUnmute(): CommandResult {
        navigationViewModel.setVoiceGuidanceMuted(false)
        return CommandResult.Success("Voice guidance unmuted")
    }
    
    private fun executeRepeat(): CommandResult {
        val instruction = navigationViewModel.getCurrentInstruction()
        return if (instruction.isNotEmpty()) {
            CommandResult.Success(instruction)
        } else {
            CommandResult.Error("No instruction to repeat")
        }
    }
    
    private fun executeGetETA(): CommandResult {
        val eta = navigationViewModel.getEstimatedArrival()
        return if (eta != null) {
            CommandResult.Success("You'll arrive at ${eta.formattedTime}")
        } else {
            CommandResult.Error("No active navigation")
        }
    }
    
    private fun executeGetDistance(): CommandResult {
        val distance = navigationViewModel.getRemainingDistance()
        return if (distance != null) {
            CommandResult.Success("${distance.formattedDistance} remaining")
        } else {
            CommandResult.Error("No active navigation")
        }
    }
    
    private fun executeGetTraffic(): CommandResult {
        if (!tier.allowsAdvancedVoice()) {
            return CommandResult.TierRestricted("Traffic status requires GemNav Plus")
        }
        
        val trafficInfo = navigationViewModel.getTrafficStatus()
        return CommandResult.Success(trafficInfo)
    }
    
    private suspend fun executeSearchAlongRoute(command: VoiceCommand.SearchAlongRoute): CommandResult {
        if (!tier.allowsAdvancedVoice()) {
            return CommandResult.TierRestricted(
                "Advanced search requires GemNav Plus"
            )
        }
        
        return try {
            val results = searchViewModel.searchAlongRoute(command.query, command.filters)
            
            if (results.isEmpty()) {
                CommandResult.Success("No results found for ${command.query}")
            } else {
                CommandResult.Success(
                    "Found ${results.size} ${command.query} along your route",
                    data = results
                )
            }
        } catch (e: Exception) {
            CommandResult.Error("Unable to search for ${command.query}")
        }
    }
    
    private suspend fun executeAvoidFeature(command: VoiceCommand.AvoidRouteFeature): CommandResult {
        if (!tier.allowsAdvancedVoice()) {
            return CommandResult.TierRestricted("Route customization requires GemNav Plus")
        }
        
        navigationViewModel.setRoutePreference(command.feature, avoid = true)
        
        val featureName = when (command.feature) {
            RouteFeature.TOLLS -> "tolls"
            RouteFeature.HIGHWAYS -> "highways"
            RouteFeature.FERRIES -> "ferries"
            RouteFeature.UNPAVED_ROADS -> "unpaved roads"
        }
        
        return CommandResult.Success("Route updated to avoid $featureName")
    }
    
    private fun executeShowAlternatives(): CommandResult {
        if (!tier.allowsAdvancedVoice()) {
            return CommandResult.TierRestricted("Alternative routes require GemNav Plus")
        }
        
        navigationViewModel.showAlternativeRoutes()
        return CommandResult.Success("Showing alternative routes")
    }
    
    private suspend fun executeOptimizeRoute(command: VoiceCommand.OptimizeRoute): CommandResult {
        if (!tier.allowsAdvancedVoice()) {
            return CommandResult.TierRestricted("Route optimization requires GemNav Plus")
        }
        
        navigationViewModel.optimizeRoute(command.criterion)
        
        val criterionName = when (command.criterion) {
            OptimizationCriterion.FASTEST -> "fastest route"
            OptimizationCriterion.SHORTEST -> "shortest distance"
            OptimizationCriterion.ECO_FRIENDLY -> "eco-friendly route"
            OptimizationCriterion.AVOID_TRAFFIC -> "traffic avoidance"
        }
        
        return CommandResult.Success("Route optimized for $criterionName")
    }
    
    private suspend fun executeFindTruckPOI(command: VoiceCommand.FindTruckPOI): CommandResult {
        if (tier != SubscriptionTier.PRO) {
            return CommandResult.TierRestricted("Truck features require GemNav Pro")
        }
        
        return try {
            val results = searchViewModel.findTruckPOI(command.type)
            
            if (results.isEmpty()) {
                CommandResult.Success("No ${command.type.displayName} found along your route")
            } else {
                CommandResult.Success(
                    "Found ${results.size} ${command.type.displayName}",
                    data = results
                )
            }
        } catch (e: Exception) {
            CommandResult.Error("Unable to find ${command.type.displayName}")
        }
    }
    
    private fun executeCheckBridges(): CommandResult {
        if (tier != SubscriptionTier.PRO) {
            return CommandResult.TierRestricted("Bridge clearance checks require GemNav Pro")
        }
        
        val clearanceInfo = navigationViewModel.getBridgeClearances()
        return CommandResult.Success(clearanceInfo)
    }
    
    private fun executeCheckHeights(): CommandResult {
        if (tier != SubscriptionTier.PRO) {
            return CommandResult.TierRestricted("Height restriction checks require GemNav Pro")
        }
        
        val heightInfo = navigationViewModel.getHeightRestrictions()
        return CommandResult.Success(heightInfo)
    }
    
    private fun executeCheckWeights(): CommandResult {
        if (tier != SubscriptionTier.PRO) {
            return CommandResult.TierRestricted("Weight limit checks require GemNav Pro")
        }
        
        val weightInfo = navigationViewModel.getWeightLimits()
        return CommandResult.Success(weightInfo)
    }
    
    private suspend fun executeFindWeighStations(): CommandResult {
        if (tier != SubscriptionTier.PRO) {
            return CommandResult.TierRestricted("Weigh station finder requires GemNav Pro")
        }
        
        return try {
            val results = searchViewModel.findTruckPOI(TruckPOIType.WEIGH_STATION)
            CommandResult.Success(
                "Found ${results.size} weigh stations ahead",
                data = results
            )
        } catch (e: Exception) {
            CommandResult.Error("Unable to find weigh stations")
        }
    }
}
