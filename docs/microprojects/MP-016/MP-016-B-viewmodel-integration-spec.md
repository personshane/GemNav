# MP-016-B: ViewModel Voice Integration Specification

**Status**: In Progress  
**Platform**: Android + iOS  
**Dependencies**: MP-016 (Voice Command System)  
**Estimated Lines**: ~1,200 (600 Android + 600 iOS)

---

## Objective

Integrate voice command execution capabilities into NavigationViewModel and create SearchViewModel with voice-aware methods on both Android and iOS platforms. This enables CommandExecutor to route voice commands to appropriate ViewModels.

---

## Scope

### Android Components

1. **NavigationViewModel Extensions** (`android/app/navigation/NavigationViewModel.kt`)
   - Add voice command execution methods
   - State management for voice interactions
   - Pro tier truck routing queries

2. **SearchViewModel** (`android/app/search/SearchViewModel.kt`)
   - New ViewModel for search and POI operations
   - Route-aware search capabilities
   - Truck POI support for Pro tier

### iOS Components

1. **NavigationViewModel** (`ios/app/viewmodels/NavigationViewModel.swift`)
   - SwiftUI-compatible @Published properties
   - Async voice command methods
   - Combine integration for reactive updates

2. **SearchViewModel** (`ios/app/viewmodels/SearchViewModel.swift`)
   - ObservableObject for SwiftUI
   - Async search operations
   - Truck POI support

---

## Android Implementation

### NavigationViewModel Extensions

**Location**: `android/app/navigation/NavigationViewModel.kt`

**New Methods** (~300 lines):

```kotlin
// Navigation control
suspend fun navigateTo(destination: String, waypoints: List<String> = emptyList()): Boolean
suspend fun addWaypoint(location: String): Boolean
fun cancelNavigation()

// Voice guidance control
fun setVoiceGuidanceMuted(muted: Boolean)
fun getCurrentInstruction(): String
fun getEstimatedArrival(): ETAInfo?
fun getRemainingDistance(): DistanceInfo?

// Traffic and route info (Plus tier)
fun getTrafficStatus(): String
fun showAlternativeRoutes()
suspend fun setRoutePreference(feature: RouteFeature, avoid: Boolean)
suspend fun optimizeRoute(criterion: OptimizationCriterion)

// Truck-specific (Pro tier)
fun getBridgeClearances(): String
fun getHeightRestrictions(): String
fun getWeightLimits(): String
```

**Data Classes**:

```kotlin
data class ETAInfo(
    val formattedTime: String,
    val timestamp: Long
)

data class DistanceInfo(
    val formattedDistance: String,
    val meters: Int
)

enum class RouteFeature {
    TOLLS, HIGHWAYS, FERRIES, UNPAVED_ROADS
}

enum class OptimizationCriterion {
    FASTEST, SHORTEST, ECO_FRIENDLY, AVOID_TRAFFIC
}
```

**State Extensions**:

```kotlin
data class NavigationUiState(
    // ... existing fields ...
    val voiceGuidanceMuted: Boolean = false,
    val activeWaypoints: List<String> = emptyList(),
    val routePreferences: Set<RouteFeature> = emptySet(),
    val trafficStatus: TrafficStatus? = null
)

enum class TrafficStatus {
    CLEAR, LIGHT, MODERATE, HEAVY
}
```

### SearchViewModel (New)

**Location**: `android/app/search/SearchViewModel.kt`

**Implementation** (~300 lines):

```kotlin
package com.gemnav.android.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val results: List<SearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class SearchResult(
    val name: String,
    val address: String,
    val location: LatLng,
    val distance: Double,
    val rating: Float? = null,
    val placeId: String? = null
)

data class SearchFilters(
    val openNow: Boolean = false,
    val maxDistance: Double? = null,
    val minRating: Float? = null
)

enum class TruckPOIType(val displayName: String) {
    TRUCK_STOP("truck stops"),
    REST_AREA("rest areas"),
    TRUCK_PARKING("truck parking"),
    WEIGH_STATION("weigh stations"),
    TRUCK_WASH("truck washes"),
    TRUCK_REPAIR("truck repair shops"),
    FUEL_STATION("fuel stations")
}

class SearchViewModel @Inject constructor(
    private val placesRepository: PlacesRepository,
    private val routeRepository: RouteRepository,
    private val tier: SubscriptionTier
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    /**
     * Search for places along the active route
     * Requires Plus or Pro tier
     */
    suspend fun searchAlongRoute(
        query: String,
        filters: SearchFilters = SearchFilters()
    ): List<SearchResult> {
        if (!tier.allowsAdvancedSearch()) {
            return emptyList()
        }
        
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        return try {
            val routePoints = routeRepository.getActiveRoutePoints()
            if (routePoints.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "No active route"
                )
                return emptyList()
            }
            
            val results = placesRepository.searchAlongRoute(
                query = query,
                routePoints = routePoints,
                filters = filters
            )
            
            _uiState.value = _uiState.value.copy(
                results = results,
                isLoading = false
            )
            
            results
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Search failed: ${e.message}"
            )
            emptyList()
        }
    }
    
    /**
     * Find truck-specific POIs along route
     * Requires Pro tier
     */
    suspend fun findTruckPOI(type: TruckPOIType): List<SearchResult> {
        if (tier != SubscriptionTier.PRO) {
            return emptyList()
        }
        
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        return try {
            val routePoints = routeRepository.getActiveRoutePoints()
            if (routePoints.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "No active route"
                )
                return emptyList()
            }
            
            val results = placesRepository.searchTruckPOI(
                type = type,
                routePoints = routePoints
            )
            
            _uiState.value = _uiState.value.copy(
                results = results,
                isLoading = false
            )
            
            results
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Search failed: ${e.message}"
            )
            emptyList()
        }
    }
    
    fun clearResults() {
        _uiState.value = _uiState.value.copy(results = emptyList())
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
```

**Repository Interface** (for reference):

```kotlin
interface PlacesRepository {
    suspend fun searchAlongRoute(
        query: String,
        routePoints: List<LatLng>,
        filters: SearchFilters
    ): List<SearchResult>
    
    suspend fun searchTruckPOI(
        type: TruckPOIType,
        routePoints: List<LatLng>
    ): List<SearchResult>
}
```

---

## iOS Implementation

### NavigationViewModel

**Location**: `ios/app/viewmodels/NavigationViewModel.swift`

**Implementation** (~300 lines):

```swift
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

struct ETAInfo {
    let formattedTime: String
    let timestamp: TimeInterval
}

struct DistanceInfo {
    let formattedDistance: String
    let meters: Int
}

enum RouteFeature {
    case tolls, highways, ferries, unpavedRoads
}

enum OptimizationCriterion {
    case fastest, shortest, ecoFriendly, avoidTraffic
}

enum TrafficStatus {
    case clear, light, moderate, heavy
}

// MARK: - NavigationViewModel

@MainActor
class NavigationViewModel: ObservableObject {
    @Published private(set) var state = NavigationState()
    
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
    
    // MARK: - Navigation Control
    
    func navigateTo(destination: String, waypoints: [String] = []) async throws {
        // Implementation depends on routing service integration
        // This is a placeholder for voice command integration
        state.destination = destination
        state.activeWaypoints = waypoints
        
        // TODO: Trigger route calculation
    }
    
    func addWaypoint(_ location: String) async throws {
        state.activeWaypoints.append(location)
        
        // TODO: Recalculate route with new waypoint
    }
    
    func cancelNavigation() {
        state.destination = nil
        state.route = nil
        state.activeWaypoints = []
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
        
        // Parse ETA string back to timestamp if needed
        // For now, return formatted version
        return ETAInfo(
            formattedTime: eta,
            timestamp: Date().timeIntervalSince1970
        )
    }
    
    func getRemainingDistance() -> DistanceInfo? {
        guard let remaining = state.remainingDistance else { return nil }
        
        // Parse distance string to meters
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
    
    // MARK: - Helper Methods
    
    private func parseDistanceToMeters(_ distanceString: String) -> Int {
        // Parse "1.5 km" or "500m" to meters
        if distanceString.hasSuffix("km") {
            let value = distanceString.replacingOccurrences(of: " km", with: "")
            if let km = Double(value) {
                return Int(km * 1000)
            }
        } else if distanceString.hasSuffix("m") {
            let value = distanceString.replacingOccurrences(of: "m", with: "")
            if let meters = Int(value) {
                return meters
            }
        }
        return 0
    }
}

struct NavigationInstruction {
    let text: String
    let location: CLLocationCoordinate2D
    let distanceText: String
}
```

### SearchViewModel

**Location**: `ios/app/viewmodels/SearchViewModel.swift`

**Implementation** (~300 lines):

```swift
import Foundation
import Combine
import CoreLocation

// MARK: - State Models

struct SearchState {
    var results: [SearchResult] = []
    var isLoading: Bool = false
    var error: String?
}

struct SearchResult: Identifiable {
    let id = UUID()
    let name: String
    let address: String
    let location: CLLocationCoordinate2D
    let distance: Double
    let rating: Float?
    let placeId: String?
}

struct SearchFilters {
    var openNow: Bool = false
    var maxDistance: Double?
    var minRating: Float?
}

enum TruckPOIType {
    case truckStop
    case restArea
    case truckParking
    case weighStation
    case truckWash
    case truckRepair
    case fuelStation
    
    var displayName: String {
        switch self {
        case .truckStop: return "truck stops"
        case .restArea: return "rest areas"
        case .truckParking: return "truck parking"
        case .weighStation: return "weigh stations"
        case .truckWash: return "truck washes"
        case .truckRepair: return "truck repair shops"
        case .fuelStation: return "fuel stations"
        }
    }
}

// MARK: - SearchViewModel

@MainActor
class SearchViewModel: ObservableObject {
    @Published private(set) var state = SearchState()
    
    private let placesRepository: PlacesRepository
    private let routeRepository: RouteRepository
    private let tier: SubscriptionTier
    
    init(
        placesRepository: PlacesRepository,
        routeRepository: RouteRepository,
        tier: SubscriptionTier
    ) {
        self.placesRepository = placesRepository
        self.routeRepository = routeRepository
        self.tier = tier
    }
    
    // MARK: - Search Operations
    
    func searchAlongRoute(
        query: String,
        filters: SearchFilters = SearchFilters()
    ) async -> [SearchResult] {
        guard tier.allowsAdvancedSearch() else {
            return []
        }
        
        state.isLoading = true
        
        do {
            let routePoints = await routeRepository.getActiveRoutePoints()
            
            guard !routePoints.isEmpty else {
                state.isLoading = false
                state.error = "No active route"
                return []
            }
            
            let results = try await placesRepository.searchAlongRoute(
                query: query,
                routePoints: routePoints,
                filters: filters
            )
            
            state.results = results
            state.isLoading = false
            
            return results
        } catch {
            state.isLoading = false
            state.error = "Search failed: \(error.localizedDescription)"
            return []
        }
    }
    
    func findTruckPOI(_ type: TruckPOIType) async -> [SearchResult] {
        guard tier == .pro else {
            return []
        }
        
        state.isLoading = true
        
        do {
            let routePoints = await routeRepository.getActiveRoutePoints()
            
            guard !routePoints.isEmpty else {
                state.isLoading = false
                state.error = "No active route"
                return []
            }
            
            let results = try await placesRepository.searchTruckPOI(
                type: type,
                routePoints: routePoints
            )
            
            state.results = results
            state.isLoading = false
            
            return results
        } catch {
            state.isLoading = false
            state.error = "Search failed: \(error.localizedDescription)"
            return []
        }
    }
    
    func clearResults() {
        state.results = []
    }
    
    func clearError() {
        state.error = nil
    }
}

// MARK: - Repository Protocols

protocol PlacesRepository {
    func searchAlongRoute(
        query: String,
        routePoints: [CLLocationCoordinate2D],
        filters: SearchFilters
    ) async throws -> [SearchResult]
    
    func searchTruckPOI(
        type: TruckPOIType,
        routePoints: [CLLocationCoordinate2D]
    ) async throws -> [SearchResult]
}

protocol RouteRepository {
    func getActiveRoutePoints() async -> [CLLocationCoordinate2D]
}

// MARK: - SubscriptionTier Extension

extension SubscriptionTier {
    func allowsAdvancedSearch() -> Bool {
        self == .plus || self == .pro
    }
}
```

---

## Integration Points

### CommandExecutor Updates

Both Android and iOS CommandExecutor already reference these methods. No changes needed to CommandExecutor itself.

### Dependency Injection

**Android (Hilt)**:

```kotlin
@Module
@InstallIn(ViewModelComponent::class)
object ViewModelModule {
    
    @Provides
    fun provideNavigationViewModel(
        locationService: LocationService,
        routeRepository: RouteRepository
    ): NavigationViewModel {
        return NavigationViewModel()
    }
    
    @Provides
    fun provideSearchViewModel(
        placesRepository: PlacesRepository,
        routeRepository: RouteRepository,
        @ApplicationContext context: Context
    ): SearchViewModel {
        val tier = getTierFromPreferences(context)
        return SearchViewModel(placesRepository, routeRepository, tier)
    }
}
```

**iOS (Manual DI)**:

```swift
class AppDependencyContainer {
    let navigationViewModel: NavigationViewModel
    let searchViewModel: SearchViewModel
    
    init() {
        let placesRepository = DefaultPlacesRepository()
        let routeRepository = DefaultRouteRepository()
        let tier = getTierFromUserDefaults()
        
        self.navigationViewModel = NavigationViewModel()
        self.searchViewModel = SearchViewModel(
            placesRepository: placesRepository,
            routeRepository: routeRepository,
            tier: tier
        )
    }
}
```

---

## Testing Considerations

### Unit Tests Required

1. **NavigationViewModel**:
   - Voice command method execution
   - State updates for voice interactions
   - Tier-based feature gating

2. **SearchViewModel**:
   - Search along route functionality
   - Truck POI filtering
   - Error handling

### Integration Tests

1. Voice command flow through CommandExecutor → ViewModel
2. Tier validation for premium features
3. Repository interaction patterns

---

## File Summary

### Android
- `android/app/navigation/NavigationViewModel.kt`: +300 lines (voice methods)
- `android/app/search/SearchViewModel.kt`: +300 lines (new file)

### iOS
- `ios/app/viewmodels/NavigationViewModel.swift`: +300 lines (new file)
- `ios/app/viewmodels/SearchViewModel.swift`: +300 lines (new file)

**Total**: ~1,200 lines

---

## Dependencies

- MP-016 voice components (complete)
- Repository interfaces (PlacesRepository, RouteRepository)
- SubscriptionTier enum
- Dependency injection setup

---

## Acceptance Criteria

✅ NavigationViewModel has all voice command methods on both platforms  
✅ SearchViewModel created with route-aware search on both platforms  
✅ Tier validation properly enforced  
✅ CommandExecutor can successfully call all ViewModel methods  
✅ State properly managed with Flow (Android) and @Published (iOS)  
✅ Async operations properly handled on both platforms

---

**Specification Complete**  
**Ready for Implementation**
