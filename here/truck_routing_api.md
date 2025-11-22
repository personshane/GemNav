# HERE Truck Routing API

**Version**: 1.0  
**Date**: 2025-11-21  
**Tier**: Pro Only  
**Platforms**: Android, iOS

---

## 1. Overview

HERE SDK Truck Routing API provides commercial-grade routing that respects legal restrictions for commercial vehicles including height, weight, length, hazmat classification, and axle count. All routes are calculated server-side to ensure access to latest restriction databases.

**Key Capabilities**:
- Truck-specific route calculation with legal compliance
- Real-time traffic integration for commercial vehicles
- Cost optimization (fuel, tolls, time)
- Multi-waypoint optimization (up to 150 stops)
- Hazmat routing with UN/NA classifications
- Tunnel category restrictions
- Bridge clearance validation
- Axle weight distribution calculations

---

## 2. Truck Profile Configuration

### 2.1 Vehicle Specifications

**TruckSpecifications Class**:

**Android (Kotlin)**:
```kotlin
data class TruckProfile(
    val grossWeight: Weight,              // Total weight including cargo
    val weightPerAxle: Weight,            // Weight per axle
    val height: Dimension,                // Vehicle height
    val width: Dimension,                 // Vehicle width
    val length: Dimension,                // Vehicle length
    val axleCount: Int,                   // Number of axles
    val trailerCount: Int,                // Number of trailers
    val truckType: TruckType,             // Straight, Tractor, etc.
    val tunnelCategory: TunnelCategory,   // B, C, D, E for hazmat
    val hazmatTypes: List<HazardousGood>, // Hazmat classifications
    val commercial: Boolean = true        // Always true for Pro tier
)

enum class TruckType {
    STRAIGHT,           // Single-unit truck
    TRACTOR,            // Tractor with trailer
    VEHICLE_WITH_TRAILER // Truck with attached trailer
}

enum class TunnelCategory {
    B,  // Goods capable of causing explosion
    C,  // Goods capable of causing large explosion
    D,  // Poisonous or corrosive goods
    E   // Radioactive goods
}

enum class HazardousGood {
    EXPLOSIVE,
    GAS,
    FLAMMABLE,
    COMBUSTIBLE,
    ORGANIC,
    POISON,
    RADIOACTIVE,
    CORROSIVE,
    POISONOUS_INHALATION,
    HARMFUL_TO_WATER,
    OTHER
}

// Weight and Dimension helpers
data class Weight(val value: Double, val unit: WeightUnit)
enum class WeightUnit { POUNDS, KILOGRAMS, TONS }

data class Dimension(val value: Double, val unit: DimensionUnit)
enum class DimensionUnit { FEET, METERS, INCHES, CENTIMETERS }
```

**iOS (Swift)**:
```swift
struct TruckProfile {
    let grossWeight: Weight
    let weightPerAxle: Weight
    let height: Dimension
    let width: Dimension
    let length: Dimension
    let axleCount: Int
    let trailerCount: Int
    let truckType: TruckType
    let tunnelCategory: TunnelCategory?
    let hazmatTypes: [HazardousGood]
    let commercial: Bool = true
}

enum TruckType {
    case straight
    case tractor
    case vehicleWithTrailer
}

enum TunnelCategory: String {
    case B, C, D, E
}

enum HazardousGood {
    case explosive
    case gas
    case flammable
    case combustible
    case organic
    case poison
    case radioactive
    case corrosive
    case poisonousInhalation
    case harmfulToWater
    case other
}

struct Weight {
    let value: Double
    let unit: WeightUnit
}

enum WeightUnit {
    case pounds, kilograms, tons
}

struct Dimension {
    let value: Double
    let unit: DimensionUnit
}

enum DimensionUnit {
    case feet, meters, inches, centimeters
}
```

### 2.2 Common Truck Profiles (Presets)

**Standard Profiles**:

```kotlin
object TruckProfiles {
    // Class 8 Semi-Truck (53' trailer, max legal weight)
    val CLASS_8_SEMI = TruckProfile(
        grossWeight = Weight(80000.0, WeightUnit.POUNDS),
        weightPerAxle = Weight(34000.0, WeightUnit.POUNDS),
        height = Dimension(13.5, DimensionUnit.FEET),
        width = Dimension(8.5, DimensionUnit.FEET),
        length = Dimension(73.5, DimensionUnit.FEET), // Tractor + 53' trailer
        axleCount = 5,
        trailerCount = 1,
        truckType = TruckType.TRACTOR,
        tunnelCategory = TunnelCategory.B,
        hazmatTypes = emptyList()
    )
    
    // Box Truck (26' non-CDL)
    val BOX_TRUCK_26 = TruckProfile(
        grossWeight = Weight(26000.0, WeightUnit.POUNDS),
        weightPerAxle = Weight(10000.0, WeightUnit.POUNDS),
        height = Dimension(12.0, DimensionUnit.FEET),
        width = Dimension(8.0, DimensionUnit.FEET),
        length = Dimension(26.0, DimensionUnit.FEET),
        axleCount = 2,
        trailerCount = 0,
        truckType = TruckType.STRAIGHT,
        tunnelCategory = TunnelCategory.B,
        hazmatTypes = emptyList()
    )
    
    // Cargo Van (Sprinter-class)
    val CARGO_VAN = TruckProfile(
        grossWeight = Weight(10000.0, WeightUnit.POUNDS),
        weightPerAxle = Weight(5000.0, WeightUnit.POUNDS),
        height = Dimension(9.5, DimensionUnit.FEET),
        width = Dimension(7.0, DimensionUnit.FEET),
        length = Dimension(24.0, DimensionUnit.FEET),
        axleCount = 2,
        trailerCount = 0,
        truckType = TruckType.STRAIGHT,
        tunnelCategory = TunnelCategory.B,
        hazmatTypes = emptyList()
    )
}
```

---

## 3. Route Calculation

### 3.1 Basic Truck Route Request

**Android (Kotlin)**:
```kotlin
class TruckRoutingEngine(private val routingEngine: RoutingEngine) {
    
    fun calculateRoute(
        origin: GeoCoordinates,
        destination: GeoCoordinates,
        truckProfile: TruckProfile,
        waypoints: List<GeoCoordinates> = emptyList(),
        callback: (Result<TruckRoute>) -> Unit
    ) {
        val truckOptions = TruckOptions().apply {
            // Vehicle specifications
            this.grossWeightPerAxleInKilograms = 
                truckProfile.weightPerAxle.toKilograms().toInt()
            this.weightPerAxleInKilograms = 
                truckProfile.weightPerAxle.toKilograms().toInt()
            this.heightInCentimeters = 
                truckProfile.height.toCentimeters().toInt()
            this.widthInCentimeters = 
                truckProfile.width.toCentimeters().toInt()
            this.lengthInCentimeters = 
                truckProfile.length.toCentimeters().toInt()
            this.axleCount = truckProfile.axleCount
            this.trailerCount = truckProfile.trailerCount
            this.truckType = convertTruckType(truckProfile.truckType)
            
            // Hazmat configuration
            if (truckProfile.hazmatTypes.isNotEmpty()) {
                this.tunnelCategory = convertTunnelCategory(truckProfile.tunnelCategory)
                this.hazardousGoods = convertHazmatTypes(truckProfile.hazmatTypes)
            }
        }
        
        val routeOptions = RouteOptions().apply {
            this.transportMode = TransportMode.TRUCK
            this.routeOptions = truckOptions
            this.optimizationMode = OptimizationMode.FASTEST
            this.alternatives = 2 // Request 2 alternative routes
            this.avoidanceOptions = AvoidanceOptions().apply {
                // Configurable by user
                this.avoidTolls = false
                this.avoidFerries = false
                this.avoidHighways = false
            }
        }
        
        // Build waypoint list
        val allWaypoints = buildList {
            add(Waypoint(origin))
            waypoints.forEach { add(Waypoint(it)) }
            add(Waypoint(destination))
        }
        
        routingEngine.calculateRoute(allWaypoints, routeOptions) { routingError, routes ->
            if (routingError != null) {
                callback(Result.failure(routingError.toException()))
                return@calculateRoute
            }
            
            routes?.firstOrNull()?.let { route ->
                val truckRoute = TruckRoute(
                    route = route,
                    complianceStatus = validateCompliance(route, truckProfile),
                    warnings = extractWarnings(route),
                    restrictions = extractRestrictions(route)
                )
                callback(Result.success(truckRoute))
            } ?: callback(Result.failure(Exception("No route found")))
        }
    }
    
    private fun validateCompliance(
        route: Route, 
        profile: TruckProfile
    ): ComplianceStatus {
        // Check for violations (see compliance_engine.md)
        return ComplianceStatus.COMPLIANT // Simplified
    }
    
    private fun extractWarnings(route: Route): List<RouteWarning> {
        val warnings = mutableListOf<RouteWarning>()
        
        route.sections.forEach { section ->
            section.notices.forEach { notice ->
                when (notice.code) {
                    NoticeCode.VIOLATED_TRUCK_RESTRICTION -> {
                        warnings.add(RouteWarning.TruckRestrictionViolation(notice))
                    }
                    NoticeCode.VIOLATED_WEIGHT_RESTRICTION -> {
                        warnings.add(RouteWarning.WeightRestrictionViolation(notice))
                    }
                    NoticeCode.VIOLATED_HEIGHT_RESTRICTION -> {
                        warnings.add(RouteWarning.HeightRestrictionViolation(notice))
                    }
                    else -> {
                        // Log other notices
                    }
                }
            }
        }
        
        return warnings
    }
}
```

**iOS (Swift)**:
```swift
class TruckRoutingEngine {
    private let routingEngine: RoutingEngine
    
    init(routingEngine: RoutingEngine) {
        self.routingEngine = routingEngine
    }
    
    func calculateRoute(
        origin: GeoCoordinates,
        destination: GeoCoordinates,
        truckProfile: TruckProfile,
        waypoints: [GeoCoordinates] = [],
        completion: @escaping (Result<TruckRoute, Error>) -> Void
    ) {
        var truckOptions = TruckOptions()
        
        // Vehicle specifications
        truckOptions.grossWeightPerAxleInKilograms = 
            Int(truckProfile.weightPerAxle.toKilograms())
        truckOptions.heightInCentimeters = 
            Int(truckProfile.height.toCentimeters())
        truckOptions.widthInCentimeters = 
            Int(truckProfile.width.toCentimeters())
        truckOptions.lengthInCentimeters = 
            Int(truckProfile.length.toCentimeters())
        truckOptions.axleCount = Int32(truckProfile.axleCount)
        truckOptions.trailerCount = Int32(truckProfile.trailerCount)
        truckOptions.truckType = convertTruckType(truckProfile.truckType)
        
        // Hazmat configuration
        if !truckProfile.hazmatTypes.isEmpty {
            if let tunnelCategory = truckProfile.tunnelCategory {
                truckOptions.tunnelCategory = convertTunnelCategory(tunnelCategory)
            }
            truckOptions.hazardousGoods = convertHazmatTypes(truckProfile.hazmatTypes)
        }
        
        var routeOptions = RouteOptions()
        routeOptions.transportMode = .truck
        routeOptions.truckOptions = truckOptions
        routeOptions.optimizationMode = .fastest
        routeOptions.alternatives = 2
        
        // Build waypoint list
        var allWaypoints: [Waypoint] = []
        allWaypoints.append(Waypoint(coordinates: origin))
        waypoints.forEach { allWaypoints.append(Waypoint(coordinates: $0)) }
        allWaypoints.append(Waypoint(coordinates: destination))
        
        routingEngine.calculateRoute(with: allWaypoints, routeOptions: routeOptions) { 
            routingError, routes in
            
            if let error = routingError {
                completion(.failure(error))
                return
            }
            
            guard let route = routes?.first else {
                completion(.failure(NSError(domain: "TruckRouting", code: -1)))
                return
            }
            
            let truckRoute = TruckRoute(
                route: route,
                complianceStatus: self.validateCompliance(route: route, profile: truckProfile),
                warnings: self.extractWarnings(from: route),
                restrictions: self.extractRestrictions(from: route)
            )
            
            completion(.success(truckRoute))
        }
    }
}
```

### 3.2 Route with Cost Optimization

```kotlin
fun calculateCostOptimizedRoute(
    origin: GeoCoordinates,
    destination: GeoCoordinates,
    truckProfile: TruckProfile,
    costModel: CostModel,
    callback: (Result<TruckRoute>) -> Unit
) {
    val routeOptions = RouteOptions().apply {
        this.transportMode = TransportMode.TRUCK
        
        // Cost-based optimization
        this.optimizationMode = when (costModel.priority) {
            CostPriority.FUEL -> OptimizationMode.FASTEST // Minimize fuel via speed
            CostPriority.TIME -> OptimizationMode.FASTEST
            CostPriority.TOLL -> OptimizationMode.SHORTEST // Avoid tolls
            CostPriority.BALANCED -> OptimizationMode.BALANCED
        }
        
        // Avoidance preferences impact cost
        this.avoidanceOptions = AvoidanceOptions().apply {
            this.avoidTolls = costModel.avoidTolls
            this.avoidFerries = costModel.avoidFerries
        }
        
        // Truck-specific options
        this.truckOptions = buildTruckOptions(truckProfile)
        
        // Cost parameters for route calculation
        this.consumptionModel = ConsumptionModel().apply {
            this.fuelConsumptionInLitersPer100Km = costModel.fuelConsumption
            this.costPerKilometer = costModel.costPerMile / 1.60934 // Convert to km
        }
    }
    
    // Calculate route with cost data
    routingEngine.calculateRoute(
        listOf(Waypoint(origin), Waypoint(destination)),
        routeOptions
    ) { error, routes ->
        routes?.firstOrNull()?.let { route ->
            val costBreakdown = calculateCostBreakdown(route, costModel)
            val truckRoute = TruckRoute(
                route = route,
                cost = costBreakdown,
                complianceStatus = validateCompliance(route, truckProfile)
            )
            callback(Result.success(truckRoute))
        }
    }
}

data class CostModel(
    val fuelConsumption: Double,      // Liters per 100km
    val fuelPricePerLiter: Double,    // USD per liter
    val costPerMile: Double,          // Operating cost per mile
    val driverHourlyRate: Double,     // USD per hour
    val avoidTolls: Boolean,
    val avoidFerries: Boolean,
    val priority: CostPriority
)

enum class CostPriority {
    FUEL,       // Minimize fuel cost
    TIME,       // Minimize driver hours
    TOLL,       // Minimize toll costs
    BALANCED    // Balance all factors
}
```

---

## 4. Multi-Stop Optimization

### 4.1 Waypoint Optimization

```kotlin
fun calculateOptimizedMultiStop(
    startPoint: GeoCoordinates,
    endPoint: GeoCoordinates,
    stops: List<DeliveryStop>,
    truckProfile: TruckProfile,
    callback: (Result<OptimizedRoute>) -> Unit
) {
    // Convert stops to waypoints with constraints
    val waypoints = stops.map { stop ->
        Waypoint(stop.location).apply {
            // Time window constraints
            stop.timeWindow?.let {
                this.arrivalTime = it.start
                this.departureTime = it.end
            }
            
            // Stop duration for loading/unloading
            this.waitDuration = Duration.ofMinutes(stop.serviceTimeMinutes.toLong())
        }
    }
    
    val routeOptions = RouteOptions().apply {
        this.transportMode = TransportMode.TRUCK
        this.truckOptions = buildTruckOptions(truckProfile)
        this.optimizationMode = OptimizationMode.FASTEST
        
        // Enable waypoint optimization
        this.waypointOptimization = true
    }
    
    val allWaypoints = buildList {
        add(Waypoint(startPoint))
        addAll(waypoints)
        add(Waypoint(endPoint))
    }
    
    routingEngine.calculateRoute(allWaypoints, routeOptions) { error, routes ->
        routes?.firstOrNull()?.let { route ->
            val optimizedOrder = extractOptimizedStopOrder(route)
            val optimizedRoute = OptimizedRoute(
                route = route,
                stopOrder = optimizedOrder,
                totalTime = route.duration,
                totalDistance = route.lengthInMeters
            )
            callback(Result.success(optimizedRoute))
        }
    }
}

data class DeliveryStop(
    val location: GeoCoordinates,
    val timeWindow: TimeWindow?,
    val serviceTimeMinutes: Int,
    val loadType: LoadType
)

data class TimeWindow(
    val start: Instant,
    val end: Instant
)

enum class LoadType {
    PICKUP,
    DELIVERY,
    BOTH
}
```

---

## 5. Real-Time Route Updates

### 5.1 Traffic-Aware Routing

```kotlin
class LiveRoutingEngine(
    private val routingEngine: RoutingEngine,
    private val locationProvider: LocationProvider
) {
    private var currentRoute: TruckRoute? = null
    private var isNavigating = false
    
    fun startNavigation(
        route: TruckRoute,
        updateCallback: (RouteUpdate) -> Unit
    ) {
        currentRoute = route
        isNavigating = true
        
        // Monitor location and recalculate if deviation detected
        locationProvider.startLocationUpdates { location ->
            if (!isNavigating) return@startLocationUpdates
            
            val deviation = calculateDeviation(location, route.route)
            
            if (deviation > REROUTE_THRESHOLD_METERS) {
                recalculateRoute(location, route.destination) { newRoute ->
                    currentRoute = newRoute
                    updateCallback(RouteUpdate.Rerouted(newRoute))
                }
            }
            
            // Check for traffic changes
            checkTrafficUpdates(route) { trafficUpdate ->
                if (trafficUpdate.delayMinutes > 5) {
                    updateCallback(RouteUpdate.TrafficDelay(trafficUpdate))
                }
            }
        }
    }
    
    fun stopNavigation() {
        isNavigating = false
        locationProvider.stopLocationUpdates()
    }
    
    private fun recalculateRoute(
        currentLocation: GeoCoordinates,
        destination: GeoCoordinates,
        callback: (TruckRoute) -> Unit
    ) {
        // Recalculate with current location as origin
        // Use same truck profile and preferences
    }
    
    companion object {
        private const val REROUTE_THRESHOLD_METERS = 50.0
    }
}

sealed class RouteUpdate {
    data class Rerouted(val newRoute: TruckRoute) : RouteUpdate()
    data class TrafficDelay(val update: TrafficUpdate) : RouteUpdate()
    data class RestrictionAlert(val restriction: Restriction) : RouteUpdate()
}
```

---

## 6. Restriction Handling

### 6.1 Route Notice Processing

```kotlin
fun processRouteNotices(route: Route): List<ComplianceIssue> {
    val issues = mutableListOf<ComplianceIssue>()
    
    route.sections.forEach { section ->
        section.notices.forEach { notice ->
            val issue = when (notice.code) {
                NoticeCode.VIOLATED_TRUCK_RESTRICTION -> 
                    ComplianceIssue.TruckRestricted(
                        location = notice.position,
                        message = "Truck access restricted on ${section.roadName}"
                    )
                
                NoticeCode.VIOLATED_WEIGHT_RESTRICTION ->
                    ComplianceIssue.WeightViolation(
                        location = notice.position,
                        maxWeight = extractWeightLimit(notice),
                        message = notice.localizedText
                    )
                
                NoticeCode.VIOLATED_HEIGHT_RESTRICTION ->
                    ComplianceIssue.HeightViolation(
                        location = notice.position,
                        maxHeight = extractHeightLimit(notice),
                        message = notice.localizedText
                    )
                
                NoticeCode.VIOLATED_TUNNEL_CATEGORY ->
                    ComplianceIssue.TunnelRestriction(
                        location = notice.position,
                        allowedCategories = extractAllowedCategories(notice)
                    )
                
                else -> null
            }
            
            issue?.let { issues.add(it) }
        }
    }
    
    return issues
}

sealed class ComplianceIssue {
    data class TruckRestricted(
        val location: GeoCoordinates,
        val message: String
    ) : ComplianceIssue()
    
    data class WeightViolation(
        val location: GeoCoordinates,
        val maxWeight: Weight,
        val message: String
    ) : ComplianceIssue()
    
    data class HeightViolation(
        val location: GeoCoordinates,
        val maxHeight: Dimension,
        val message: String
    ) : ComplianceIssue()
    
    data class TunnelRestriction(
        val location: GeoCoordinates,
        val allowedCategories: List<TunnelCategory>
    ) : ComplianceIssue()
}
```

---

## 7. Error Handling

### 7.1 Routing Errors

```kotlin
fun handleRoutingError(error: RoutingError): UserFacingError {
    return when (error) {
        RoutingError.NO_ROUTE_FOUND -> 
            UserFacingError(
                title = "No Route Available",
                message = "Cannot find a route to your destination. Try adjusting your truck settings or choosing a different destination.",
                recoverable = true
            )
        
        RoutingError.VIOLATED_RESTRICTIONS ->
            UserFacingError(
                title = "Route Violates Restrictions",
                message = "Your truck specifications don't allow a legal route to this destination.",
                recoverable = true,
                suggestion = "Try adjusting truck height, weight, or hazmat settings"
            )
        
        RoutingError.NETWORK_ERROR ->
            UserFacingError(
                title = "Network Error",
                message = "Cannot calculate route. Check your internet connection.",
                recoverable = true,
                retryable = true
            )
        
        RoutingError.AUTHENTICATION_FAILED ->
            UserFacingError(
                title = "Service Unavailable",
                message = "Truck routing is temporarily unavailable. Please try again later.",
                recoverable = false
            )
    }
}
```

---

## 8. Testing & Validation

### 8.1 Test Scenarios

**Basic Routing**:
1. Class 8 semi: Los Angeles to Dallas
2. Box truck: Short urban route with height restrictions
3. Cargo van: Multi-stop delivery route

**Restriction Compliance**:
1. Overweight truck on weight-restricted bridge
2. Oversized load on low-clearance route
3. Hazmat tunnel restrictions (Category D)

**Multi-Stop Optimization**:
1. 10 stops with time windows
2. 5 stops with varying service times
3. Mixed pickup/delivery optimization

**Real-Time Updates**:
1. Deviation triggering reroute
2. Traffic delay exceeding threshold
3. Road closure requiring alternate route

---

## 9. Performance Benchmarks

**Route Calculation Times**:
- Simple A-to-B: < 2 seconds
- 10-stop optimization: < 5 seconds
- 50-stop optimization: < 15 seconds
- Reroute during navigation: < 1 second

**Accuracy Targets**:
- ETA accuracy: ±10% for routes > 2 hours
- Distance accuracy: ±2% on highway routes
- Traffic prediction: ±15 minutes for long routes

---

**File Version**: 1.0  
**Last Updated**: 2025-11-21  
**Related Files**: compliance_engine.md, restriction_database.md, cost_calculations.md
