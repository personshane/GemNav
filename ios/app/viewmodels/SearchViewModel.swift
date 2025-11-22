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

enum SubscriptionTier {
    case free, plus, pro
    
    func allowsAdvancedSearch() -> Bool {
        return self == .plus || self == .pro
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
