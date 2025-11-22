package com.gemnav.android.navigation

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.here.sdk.routing.Route as HereRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class NavigationUiState(
    val tier: String = "free",
    val destination: String? = null,
    val currentLocation: LatLng? = null,
    val route: List<LatLng>? = null,
    val hereRoute: HereRoute? = null,
    val nextInstruction: String? = null,
    val distanceToNextTurn: String? = null,
    val eta: String? = null,
    val remainingDistance: String? = null,
    val isMuted: Boolean = false,
    val isMapReady: Boolean = false,
    val error: String? = null
)

class NavigationViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(NavigationUiState())
    val uiState: StateFlow<NavigationUiState> = _uiState.asStateFlow()
    
    private var routePoints: List<LatLng> = emptyList()
    private var instructions: List<NavigationInstruction> = emptyList()
    private var currentInstructionIndex = 0
    private var lastSpokenInstruction: String? = null
    private var isTtsReady = false
    
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.US)
    
    fun initialize(tier: String, routeJson: String?, hereRoute: HereRoute?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                tier = tier,
                hereRoute = hereRoute
            )
            
            when (tier) {
                "free" -> {
                    // Free tier - minimal setup, navigation in Maps app
                    _uiState.value = _uiState.value.copy(
                        destination = "Google Maps"
                    )
                }
                "plus" -> {
                    // Plus tier - parse Google Maps route
                    routeJson?.let { parseGoogleRoute(it) }
                }
                "pro" -> {
                    // Pro tier - use HERE route
                    hereRoute?.let { parseHereRoute(it) }
                }
            }
        }
    }
    
    private fun parseGoogleRoute(json: String) {
        try {
            val route = JSONObject(json)
            val legs = route.getJSONArray("legs")
            
            if (legs.length() > 0) {
                val leg = legs.getJSONObject(0)
                
                // Extract destination
                val endAddress = leg.getJSONObject("end_address").getString("formatted_address")
                
                // Extract route polyline points
                val steps = leg.getJSONArray("steps")
                val points = mutableListOf<LatLng>()
                val instrs = mutableListOf<NavigationInstruction>()
                
                for (i in 0 until steps.length()) {
                    val step = steps.getJSONObject(i)
                    val startLoc = step.getJSONObject("start_location")
                    val endLoc = step.getJSONObject("end_location")
                    
                    points.add(LatLng(startLoc.getDouble("lat"), startLoc.getDouble("lng")))
                    
                    // Extract instruction
                    val instruction = step.getString("html_instructions")
                        .replace("<[^>]*>".toRegex(), "") // Strip HTML
                    val distance = step.getJSONObject("distance").getString("text")
                    
                    instrs.add(
                        NavigationInstruction(
                            text = instruction,
                            location = LatLng(startLoc.getDouble("lat"), startLoc.getDouble("lng")),
                            distanceText = distance
                        )
                    )
                }
                
                // Add final point
                val lastStep = steps.getJSONObject(steps.length() - 1)
                val lastLoc = lastStep.getJSONObject("end_location")
                points.add(LatLng(lastLoc.getDouble("lat"), lastLoc.getDouble("lng")))
                
                routePoints = points
                instructions = instrs
                
                _uiState.value = _uiState.value.copy(
                    destination = endAddress,
                    route = points,
                    remainingDistance = leg.getJSONObject("distance").getString("text")
                )
                
                calculateEta(leg.getJSONObject("duration").getInt("value"))
            }
        } catch (e: Exception) {
            onNavigationError("Failed to parse route: ${e.message}")
        }
    }
    
    private fun parseHereRoute(route: HereRoute) {
        try {
            val sections = route.sections
            if (sections.isNotEmpty()) {
                val points = mutableListOf<LatLng>()
                val instrs = mutableListOf<NavigationInstruction>()
                
                sections.forEach { section ->
                    section.polyline.forEach { coord ->
                        points.add(LatLng(coord.latitude, coord.longitude))
                    }
                    
                    // Extract HERE maneuvers
                    section.maneuvers.forEach { maneuver ->
                        val instruction = maneuver.text
                        val location = maneuver.coordinates
                        
                        instrs.add(
                            NavigationInstruction(
                                text = instruction,
                                location = LatLng(location.latitude, location.longitude),
                                distanceText = "${maneuver.lengthInMeters}m"
                            )
                        )
                    }
                }
                
                routePoints = points
                instructions = instrs
                
                val totalLength = sections.sumOf { it.lengthInMeters }
                val totalDuration = sections.sumOf { it.durationInSeconds }
                
                _uiState.value = _uiState.value.copy(
                    destination = "Destination", // HERE route doesn't always have address
                    route = points,
                    remainingDistance = formatDistance(totalLength)
                )
                
                calculateEta(totalDuration.toInt())
            }
        } catch (e: Exception) {
            onNavigationError("Failed to parse HERE route: ${e.message}")
        }
    }
    
    fun updateLocation(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        
        _uiState.value = _uiState.value.copy(
            currentLocation = latLng
        )
        
        // Update navigation progress
        updateNavigationProgress(latLng)
    }
    
    private fun updateNavigationProgress(currentLocation: LatLng) {
        if (instructions.isEmpty()) return
        
        // Find next instruction based on proximity
        var closestIndex = currentInstructionIndex
        var closestDistance = Double.MAX_VALUE
        
        for (i in currentInstructionIndex until instructions.size) {
            val distance = calculateDistance(currentLocation, instructions[i].location)
            if (distance < closestDistance) {
                closestDistance = distance
                closestIndex = i
            }
        }
        
        // Update if we've progressed
        if (closestIndex > currentInstructionIndex) {
            currentInstructionIndex = closestIndex
        }
        
        // Update current instruction if within threshold (50 meters)
        if (closestDistance < 50 && currentInstructionIndex < instructions.size) {
            val instruction = instructions[currentInstructionIndex]
            _uiState.value = _uiState.value.copy(
                nextInstruction = instruction.text,
                distanceToNextTurn = instruction.distanceText
            )
        }
        
        // Calculate remaining distance
        calculateRemainingDistance(currentLocation)
    }
    
    private fun calculateRemainingDistance(currentLocation: LatLng) {
        if (routePoints.isEmpty()) return
        
        // Find closest point on route
        var closestIndex = 0
        var closestDistance = Double.MAX_VALUE
        
        for (i in routePoints.indices) {
            val distance = calculateDistance(currentLocation, routePoints[i])
            if (distance < closestDistance) {
                closestDistance = distance
                closestIndex = i
            }
        }
        
        // Sum remaining distance
        var remaining = 0.0
        for (i in closestIndex until routePoints.size - 1) {
            remaining += calculateDistance(routePoints[i], routePoints[i + 1])
        }
        
        _uiState.value = _uiState.value.copy(
            remainingDistance = formatDistance(remaining.toInt())
        )
    }
    
    private fun calculateDistance(point1: LatLng, point2: LatLng): Double {
        val earthRadius = 6371000.0 // meters
        
        val lat1 = Math.toRadians(point1.latitude)
        val lat2 = Math.toRadians(point2.latitude)
        val dLat = Math.toRadians(point2.latitude - point1.latitude)
        val dLng = Math.toRadians(point2.longitude - point1.longitude)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1) * cos(lat2) *
                sin(dLng / 2) * sin(dLng / 2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadius * c
    }
    
    private fun formatDistance(meters: Int): String {
        return when {
            meters < 1000 -> "${meters}m"
            else -> String.format("%.1f km", meters / 1000.0)
        }
    }
    
    private fun calculateEta(durationSeconds: Int) {
        val etaTime = Calendar.getInstance().apply {
            add(Calendar.SECOND, durationSeconds)
        }.time
        
        _uiState.value = _uiState.value.copy(
            eta = timeFormat.format(etaTime)
        )
    }
    
    fun getNextInstruction(location: Location): String? {
        if (!isTtsReady || _uiState.value.isMuted) return null
        if (currentInstructionIndex >= instructions.size) return null
        
        val currentLatLng = LatLng(location.latitude, location.longitude)
        val instruction = instructions[currentInstructionIndex]
        val distance = calculateDistance(currentLatLng, instruction.location)
        
        // Speak instruction when within 100m and not already spoken
        return if (distance < 100 && instruction.text != lastSpokenInstruction) {
            lastSpokenInstruction = instruction.text
            "In ${instruction.distanceText}, ${instruction.text}"
        } else null
    }
    
    fun setTtsReady(ready: Boolean) {
        isTtsReady = ready
    }
    
    fun onMapReady() {
        _uiState.value = _uiState.value.copy(isMapReady = true)
    }
    
    fun recenterMap() {
        // Trigger recentering via state change
        // UI will observe currentLocation changes
    }
    
    fun toggleMute() {
        _uiState.value = _uiState.value.copy(
            isMuted = !_uiState.value.isMuted
        )
    }
    
    fun stopNavigation() {
        // Cleanup navigation state
        currentInstructionIndex = 0
        lastSpokenInstruction = null
    }
    
    fun onNavigationError(message: String) {
        _uiState.value = _uiState.value.copy(error = message)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun cleanup() {
        // Release resources
        routePoints = emptyList()
        instructions = emptyList()
    }
}

data class NavigationInstruction(
    val text: String,
    val location: LatLng,
    val distanceText: String
)