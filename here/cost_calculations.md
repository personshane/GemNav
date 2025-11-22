# HERE Cost Calculations

**Version**: 1.0  
**Date**: 2025-11-21  
**Tier**: Pro Only  
**Platforms**: Android, iOS

---

## 1. Overview

Cost Calculations enable Pro tier users to optimize routes based on total operating cost rather than just time or distance. This includes fuel consumption, tolls, driver wages, vehicle maintenance, and other operational expenses specific to commercial trucking.

**Cost Optimization Goals**:
- Minimize total trip cost (fuel + tolls + time)
- Compare route alternatives by cost
- Track historical cost data
- Project monthly/annual operating expenses
- Support fleet cost budgeting

---

## 2. Cost Model Architecture

### 2.1 Cost Components

**Total Route Cost** = Fuel Cost + Toll Cost + Labor Cost + Maintenance Cost + Other Costs

```kotlin
data class RouteCost(
    val fuelCost: Money,
    val tollCost: Money,
    val laborCost: Money,
    val maintenanceCost: Money,
    val otherCosts: Money,
    val totalCost: Money,
    
    // Breakdown details
    val fuelBreakdown: FuelCostBreakdown,
    val tollBreakdown: TollCostBreakdown,
    val laborBreakdown: LaborCostBreakdown,
    val maintenanceBreakdown: MaintenanceCostBreakdown
)

data class Money(
    val amount: Double,
    val currency: Currency = Currency.USD
)

enum class Currency {
    USD, CAD, MXN
}
```

### 2.2 User Cost Profile

**Cost Profile Configuration**:

```kotlin
data class TruckCostProfile(
    val truckId: String,
    val profileName: String,
    
    // Fuel parameters
    val fuelType: FuelType,
    val fuelEconomyMPG: Double,              // Miles per gallon (loaded)
    val fuelEconomyEmptyMPG: Double,         // Miles per gallon (empty)
    val currentFuelPricePerGallon: Double,   // User-set or auto-updated
    val autoUpdateFuelPrices: Boolean,
    
    // Labor parameters
    val driverHourlyRate: Double,
    val includeOvertimeCost: Boolean,
    val overtimeThresholdHours: Double,      // e.g., 8 hours
    val overtimeMultiplier: Double,           // e.g., 1.5x
    
    // Maintenance parameters
    val maintenanceCostPerMile: Double,      // e.g., $0.15/mile
    val includeDepreciation: Boolean,
    val depreciationPerMile: Double,
    
    // Toll preferences
    val avoidTolls: Boolean,
    val maxTollCostPerTrip: Double?,         // Optional cap
    
    // Other costs
    val parkingCostPerHour: Double?,
    val unloadingFeePerStop: Double?,
    
    // Optimization priority
    val costPriority: CostPriority,
    
    // Created/Updated
    val createdAt: Instant,
    val lastUpdated: Instant
)

enum class FuelType {
    DIESEL,
    GASOLINE,
    CNG,        // Compressed Natural Gas
    ELECTRIC,
    HYDROGEN
}

enum class CostPriority {
    MINIMIZE_TOTAL_COST,    // Balance all factors
    MINIMIZE_FUEL,          // Fuel cost is primary
    MINIMIZE_TIME,          // Time/labor cost is primary
    AVOID_TOLLS,            // Avoid tolls even if more expensive
    FASTEST_DELIVERY        // Time-critical, cost secondary
}
```

---

## 3. Fuel Cost Calculations

### 3.1 Basic Fuel Cost

```kotlin
class FuelCostCalculator {
    
    fun calculateFuelCost(
        route: Route,
        profile: TruckCostProfile,
        isLoaded: Boolean
    ): FuelCostBreakdown {
        
        val distanceMiles = route.lengthInMeters / 1609.34
        
        // Determine MPG based on load status
        val mpg = if (isLoaded) {
            profile.fuelEconomyMPG
        } else {
            profile.fuelEconomyEmptyMPG
        }
        
        // Calculate gallons needed
        val gallonsNeeded = distanceMiles / mpg
        
        // Calculate cost
        val fuelCost = gallonsNeeded * profile.currentFuelPricePerGallon
        
        return FuelCostBreakdown(
            totalCost = Money(fuelCost, Currency.USD),
            gallonsUsed = gallonsNeeded,
            averageMPG = mpg,
            pricePerGallon = Money(profile.currentFuelPricePerGallon, Currency.USD),
            distanceMiles = distanceMiles
        )
    }
}

data class FuelCostBreakdown(
    val totalCost: Money,
    val gallonsUsed: Double,
    val averageMPG: Double,
    val pricePerGallon: Money,
    val distanceMiles: Double
)
```

### 3.2 Terrain-Adjusted Fuel Cost

```kotlin
fun calculateTerrainAdjustedFuelCost(
    route: Route,
    profile: TruckCostProfile
): FuelCostBreakdown {
    
    var totalGallons = 0.0
    var totalDistance = 0.0
    
    route.sections.forEach { section ->
        val sectionDistanceMiles = section.lengthInMeters / 1609.34
        totalDistance += sectionDistanceMiles
        
        // Get elevation data
        val elevationChange = section.elevationChangeInMeters
        val averageGrade = calculateAverageGrade(section)
        
        // Adjust MPG based on terrain
        val adjustedMPG = adjustMPGForTerrain(
            baseMPG = profile.fuelEconomyMPG,
            grade = averageGrade,
            elevationChange = elevationChange
        )
        
        totalGallons += sectionDistanceMiles / adjustedMPG
    }
    
    val fuelCost = totalGallons * profile.currentFuelPricePerGallon
    
    return FuelCostBreakdown(
        totalCost = Money(fuelCost, Currency.USD),
        gallonsUsed = totalGallons,
        averageMPG = totalDistance / totalGallons,
        pricePerGallon = Money(profile.currentFuelPricePerGallon, Currency.USD),
        distanceMiles = totalDistance
    )
}

private fun adjustMPGForTerrain(
    baseMPG: Double,
    grade: Double,      // Percentage grade
    elevationChange: Double
): Double {
    
    // Uphill: Reduce MPG
    // Downhill: Increase MPG (but not beyond 110% of base)
    
    val gradeAdjustment = when {
        grade > 5.0 -> -0.20      // Steep uphill: -20% MPG
        grade > 3.0 -> -0.10      // Moderate uphill: -10% MPG
        grade > 1.0 -> -0.05      // Slight uphill: -5% MPG
        grade < -3.0 -> 0.10      // Downhill: +10% MPG
        grade < -1.0 -> 0.05      // Slight downhill: +5% MPG
        else -> 0.0
    }
    
    return baseMPG * (1.0 + gradeAdjustment)
}

private fun calculateAverageGrade(section: Section): Double {
    // Calculate average grade percentage for section
    if (section.lengthInMeters == 0.0) return 0.0
    return (section.elevationChangeInMeters / section.lengthInMeters) * 100.0
}
```

### 3.3 Traffic-Adjusted Fuel Cost

```kotlin
fun calculateTrafficAdjustedFuelCost(
    route: Route,
    profile: TruckCostProfile,
    trafficData: TrafficData
): FuelCostBreakdown {
    
    var totalGallons = 0.0
    
    route.sections.forEach { section ->
        val sectionDistanceMiles = section.lengthInMeters / 1609.34
        
        // Get traffic speed for section
        val averageSpeed = trafficData.getAverageSpeed(section.id)
        
        // Adjust MPG based on speed
        val speedAdjustedMPG = adjustMPGForSpeed(
            baseMPG = profile.fuelEconomyMPG,
            speedMPH = averageSpeed
        )
        
        totalGallons += sectionDistanceMiles / speedAdjustedMPG
    }
    
    val fuelCost = totalGallons * profile.currentFuelPricePerGallon
    
    return FuelCostBreakdown(
        totalCost = Money(fuelCost, Currency.USD),
        gallonsUsed = totalGallons,
        averageMPG = route.lengthInMeters / 1609.34 / totalGallons,
        pricePerGallon = Money(profile.currentFuelPricePerGallon, Currency.USD),
        distanceMiles = route.lengthInMeters / 1609.34
    )
}

private fun adjustMPGForSpeed(
    baseMPG: Double,
    speedMPH: Double
): Double {
    
    // Optimal speed: 55-65 MPH (100% efficiency)
    // Slower: Worse MPG (stop-and-go traffic)
    // Faster: Worse MPG (aerodynamic drag)
    
    return when {
        speedMPH < 20 -> baseMPG * 0.50  // Stop-and-go: 50% efficiency
        speedMPH < 40 -> baseMPG * 0.75  // Slow traffic: 75% efficiency
        speedMPH < 55 -> baseMPG * 0.90  // Below optimal: 90% efficiency
        speedMPH <= 65 -> baseMPG * 1.00 // Optimal speed: 100% efficiency
        speedMPH <= 75 -> baseMPG * 0.90 // Above optimal: 90% efficiency
        else -> baseMPG * 0.75           // High speed: 75% efficiency
    }
}
```

### 3.4 Real-Time Fuel Price Integration

```kotlin
class FuelPriceService(
    private val apiClient: FuelPriceAPIClient
) {
    
    suspend fun updateFuelPrices(
        location: GeoCoordinates,
        fuelType: FuelType
    ): FuelPriceData {
        
        // Fetch fuel prices from GasBuddy, AAA, or similar API
        val stations = apiClient.getNearbyStations(
            latitude = location.latitude,
            longitude = location.longitude,
            radiusMiles = 25.0,
            fuelType = fuelType
        )
        
        val averagePrice = stations.map { it.price }.average()
        val cheapestStation = stations.minByOrNull { it.price }
        
        return FuelPriceData(
            averagePrice = averagePrice,
            cheapestPrice = cheapestStation?.price ?: averagePrice,
            cheapestStation = cheapestStation,
            lastUpdated = Instant.now()
        )
    }
    
    suspend fun getFuelPriceAlongRoute(
        route: Route,
        fuelType: FuelType
    ): List<FuelStationPrice> {
        
        // Sample points along route (every 50 miles)
        val samplePoints = generateRoutePoints(route, intervalMiles = 50.0)
        
        return samplePoints.map { point ->
            val priceData = updateFuelPrices(point, fuelType)
            FuelStationPrice(
                location = point,
                averagePrice = priceData.averagePrice,
                cheapestPrice = priceData.cheapestPrice
            )
        }
    }
}

data class FuelPriceData(
    val averagePrice: Double,
    val cheapestPrice: Double,
    val cheapestStation: FuelStation?,
    val lastUpdated: Instant
)

data class FuelStation(
    val name: String,
    val location: GeoCoordinates,
    val price: Double,
    val brand: String,
    val amenities: List<String>
)

data class FuelStationPrice(
    val location: GeoCoordinates,
    val averagePrice: Double,
    val cheapestPrice: Double
)
```

---

## 4. Toll Cost Calculations

### 4.1 Toll Data Integration

```kotlin
class TollCostCalculator(
    private val tollDatabase: TollDatabase,
    private val hereSDK: HERESDKWrapper
) {
    
    fun calculateTollCost(
        route: Route,
        truckProfile: TruckProfile,
        costProfile: TruckCostProfile
    ): TollCostBreakdown {
        
        val tollSections = extractTollSections(route)
        val tollCosts = mutableListOf<TollCostItem>()
        
        var totalTollCost = 0.0
        
        tollSections.forEach { section ->
            val tollCost = calculateSectionTollCost(
                section = section,
                truckProfile = truckProfile
            )
            
            totalTollCost += tollCost.amount
            tollCosts.add(
                TollCostItem(
                    roadName = section.roadName,
                    cost = Money(tollCost.amount, Currency.USD),
                    entryPoint = section.startCoordinates,
                    exitPoint = section.endCoordinates,
                    tollAuthority = tollCost.authority
                )
            )
        }
        
        return TollCostBreakdown(
            totalCost = Money(totalTollCost, Currency.USD),
            tollCount = tollCosts.size,
            tollDetails = tollCosts
        )
    }
    
    private fun calculateSectionTollCost(
        section: TollSection,
        truckProfile: TruckProfile
    ): TollCostDetail {
        
        // Toll rates vary by:
        // - Vehicle class (based on axle count, weight)
        // - Time of day (peak/off-peak)
        // - Payment method (cash/transponder)
        
        val vehicleClass = determineTollClass(truckProfile)
        val timeOfDay = getCurrentTimeOfDay()
        val paymentMethod = PaymentMethod.TRANSPONDER // Assume E-ZPass/etc
        
        val baseTollRate = tollDatabase.getTollRate(
            sectionId = section.id,
            vehicleClass = vehicleClass,
            timeOfDay = timeOfDay
        )
        
        // Apply payment method discount
        val finalRate = when (paymentMethod) {
            PaymentMethod.TRANSPONDER -> baseTollRate * 0.90 // 10% discount
            PaymentMethod.CASH -> baseTollRate
            PaymentMethod.LICENSE_PLATE -> baseTollRate * 1.25 // 25% surcharge
        }
        
        return TollCostDetail(
            amount = finalRate,
            authority = section.tollAuthority,
            vehicleClass = vehicleClass
        )
    }
    
    private fun determineTollClass(profile: TruckProfile): TollVehicleClass {
        return when {
            profile.axleCount >= 6 -> TollVehicleClass.CLASS_7_PLUS
            profile.axleCount >= 5 -> TollVehicleClass.CLASS_6
            profile.axleCount >= 4 -> TollVehicleClass.CLASS_5
            profile.axleCount >= 3 -> TollVehicleClass.CLASS_4
            else -> TollVehicleClass.CLASS_3
        }
    }
}

data class TollCostBreakdown(
    val totalCost: Money,
    val tollCount: Int,
    val tollDetails: List<TollCostItem>
)

data class TollCostItem(
    val roadName: String,
    val cost: Money,
    val entryPoint: GeoCoordinates,
    val exitPoint: GeoCoordinates,
    val tollAuthority: String
)

enum class TollVehicleClass {
    CLASS_2,        // 2-axle vehicle
    CLASS_3,        // 3-axle vehicle
    CLASS_4,        // 4-axle vehicle
    CLASS_5,        // 5-axle vehicle
    CLASS_6,        // 6-axle vehicle
    CLASS_7_PLUS    // 7+ axle vehicle
}

enum class PaymentMethod {
    CASH,
    TRANSPONDER,    // E-ZPass, SunPass, etc.
    LICENSE_PLATE   // Toll-by-plate
}
```

### 4.2 Toll Avoidance Optimization

```kotlin
fun findTollFreeAlternative(
    route: Route,
    truckProfile: TruckProfile,
    costProfile: TruckCostProfile
): RouteComparison {
    
    // Calculate cost of current route (with tolls)
    val routeWithTolls = calculateRouteCost(route, truckProfile, costProfile)
    
    // Request toll-free alternative
    val tollFreeRoute = routingEngine.calculateRoute(
        origin = route.origin,
        destination = route.destination,
        truckProfile = truckProfile,
        avoidTolls = true
    )
    
    val tollFreeRouteCost = calculateRouteCost(tollFreeRoute, truckProfile, costProfile)
    
    // Compare routes
    return RouteComparison(
        routeWithTolls = routeWithTolls,
        tollFreeRoute = tollFreeRouteCost,
        costDifference = routeWithTolls.totalCost.amount - tollFreeRouteCost.totalCost.amount,
        timeDifference = routeWithTolls.durationSeconds - tollFreeRouteCost.durationSeconds,
        recommendation = determineRecommendation(routeWithTolls, tollFreeRouteCost)
    )
}

private fun determineRecommendation(
    withTolls: RouteCost,
    noTolls: RouteCost
): RouteRecommendation {
    
    val tollSavings = withTolls.tollCost.amount
    val fuelIncrease = noTolls.fuelCost.amount - withTolls.fuelCost.amount
    val timeIncrease = noTolls.durationSeconds - withTolls.durationSeconds
    
    return when {
        // Toll-free route saves money overall
        noTolls.totalCost.amount < withTolls.totalCost.amount ->
            RouteRecommendation.PREFER_TOLL_FREE
        
        // Toll route saves significant time (>30 min) for small cost increase (<$10)
        timeIncrease > 1800 && 
        withTolls.totalCost.amount - noTolls.totalCost.amount < 10.0 ->
            RouteRecommendation.PREFER_TOLLS
        
        // Routes are similar in cost and time
        else -> RouteRecommendation.DRIVER_PREFERENCE
    }
}

enum class RouteRecommendation {
    PREFER_TOLL_FREE,
    PREFER_TOLLS,
    DRIVER_PREFERENCE
}
```

---

## 5. Labor Cost Calculations

### 5.1 Basic Labor Cost

```kotlin
class LaborCostCalculator {
    
    fun calculateLaborCost(
        route: Route,
        profile: TruckCostProfile,
        departureTime: Instant
    ): LaborCostBreakdown {
        
        val durationHours = route.duration.toHours().toDouble() + 
                           (route.duration.toMinutes() % 60) / 60.0
        
        var regularHours = durationHours
        var overtimeHours = 0.0
        
        if (profile.includeOvertimeCost && durationHours > profile.overtimeThresholdHours) {
            regularHours = profile.overtimeThresholdHours
            overtimeHours = durationHours - profile.overtimeThresholdHours
        }
        
        val regularCost = regularHours * profile.driverHourlyRate
        val overtimeCost = overtimeHours * profile.driverHourlyRate * profile.overtimeMultiplier
        
        return LaborCostBreakdown(
            totalCost = Money(regularCost + overtimeCost, Currency.USD),
            regularHours = regularHours,
            overtimeHours = overtimeHours,
            regularCost = Money(regularCost, Currency.USD),
            overtimeCost = Money(overtimeCost, Currency.USD),
            hourlyRate = Money(profile.driverHourlyRate, Currency.USD)
        )
    }
}

data class LaborCostBreakdown(
    val totalCost: Money,
    val regularHours: Double,
    val overtimeHours: Double,
    val regularCost: Money,
    val overtimeCost: Money,
    val hourlyRate: Money
)
```

### 5.2 HOS-Adjusted Labor Cost

**Hours of Service (HOS) Regulations**:

```kotlin
fun calculateHOSAdjustedLaborCost(
    route: Route,
    profile: TruckCostProfile,
    driverHOSStatus: HOSStatus
): LaborCostBreakdown {
    
    val durationHours = route.duration.toHours().toDouble()
    
    // Check if driver needs mandatory breaks
    val requiredBreaks = calculateRequiredBreaks(durationHours, driverHOSStatus)
    
    // Add break time to total duration
    val totalDurationWithBreaks = durationHours + requiredBreaks.totalBreakHours
    
    // Calculate cost including break time (paid or unpaid based on profile)
    val laborCost = if (profile.paidBreaks) {
        totalDurationWithBreaks * profile.driverHourlyRate
    } else {
        durationHours * profile.driverHourlyRate
    }
    
    return LaborCostBreakdown(
        totalCost = Money(laborCost, Currency.USD),
        regularHours = totalDurationWithBreaks,
        overtimeHours = 0.0,
        regularCost = Money(laborCost, Currency.USD),
        overtimeCost = Money(0.0, Currency.USD),
        hourlyRate = Money(profile.driverHourlyRate, Currency.USD)
    ).copy(
        requiredBreaks = requiredBreaks
    )
}

data class HOSStatus(
    val hoursOnDutyToday: Double,
    val hoursDrivingToday: Double,
    val lastBreakTime: Instant?,
    val consecutiveDrivingHours: Double
)

data class RequiredBreaks(
    val breakCount: Int,
    val totalBreakHours: Double,
    val breakLocations: List<GeoCoordinates>
)

private fun calculateRequiredBreaks(
    durationHours: Double,
    hosStatus: HOSStatus
): RequiredBreaks {
    
    // US HOS regulations:
    // - 30-minute break required after 8 hours of driving
    // - 10-hour break required after 11 hours of driving
    // - 11-hour driving limit per day
    
    val breakCount = when {
        durationHours > 8.0 -> 1
        durationHours > 11.0 -> 2
        else -> 0
    }
    
    val breakHours = breakCount * 0.5 // 30-minute breaks
    
    return RequiredBreaks(
        breakCount = breakCount,
        totalBreakHours = breakHours,
        breakLocations = emptyList() // TODO: Calculate optimal break points
    )
}
```

---

## 6. Maintenance & Depreciation

### 6.1 Distance-Based Maintenance Cost

```kotlin
class MaintenanceCostCalculator {
    
    fun calculateMaintenanceCost(
        route: Route,
        profile: TruckCostProfile
    ): MaintenanceCostBreakdown {
        
        val distanceMiles = route.lengthInMeters / 1609.34
        
        val maintenanceCost = distanceMiles * profile.maintenanceCostPerMile
        val depreciationCost = if (profile.includeDepreciation) {
            distanceMiles * profile.depreciationPerMile
        } else {
            0.0
        }
        
        return MaintenanceCostBreakdown(
            totalCost = Money(maintenanceCost + depreciationCost, Currency.USD),
            maintenanceCost = Money(maintenanceCost, Currency.USD),
            depreciationCost = Money(depreciationCost, Currency.USD),
            distanceMiles = distanceMiles,
            costPerMile = profile.maintenanceCostPerMile
        )
    }
}

data class MaintenanceCostBreakdown(
    val totalCost: Money,
    val maintenanceCost: Money,
    val depreciationCost: Money,
    val distanceMiles: Double,
    val costPerMile: Double
)
```

---

## 7. Total Cost Calculation & Optimization

### 7.1 Comprehensive Cost Calculation

```kotlin
class ComprehensiveCostCalculator(
    private val fuelCalculator: FuelCostCalculator,
    private val tollCalculator: TollCostCalculator,
    private val laborCalculator: LaborCostCalculator,
    private val maintenanceCalculator: MaintenanceCostCalculator
) {
    
    fun calculateTotalRouteCost(
        route: Route,
        truckProfile: TruckProfile,
        costProfile: TruckCostProfile,
        isLoaded: Boolean = true
    ): RouteCost {
        
        // Calculate each cost component
        val fuelBreakdown = fuelCalculator.calculateTerrainAdjustedFuelCost(route, costProfile)
        val tollBreakdown = tollCalculator.calculateTollCost(route, truckProfile, costProfile)
        val laborBreakdown = laborCalculator.calculateLaborCost(route, costProfile, Instant.now())
        val maintenanceBreakdown = maintenanceCalculator.calculateMaintenanceCost(route, costProfile)
        
        // Sum total cost
        val totalCost = fuelBreakdown.totalCost.amount +
                       tollBreakdown.totalCost.amount +
                       laborBreakdown.totalCost.amount +
                       maintenanceBreakdown.totalCost.amount
        
        return RouteCost(
            fuelCost = fuelBreakdown.totalCost,
            tollCost = tollBreakdown.totalCost,
            laborCost = laborBreakdown.totalCost,
            maintenanceCost = maintenanceBreakdown.totalCost,
            otherCosts = Money(0.0, Currency.USD),
            totalCost = Money(totalCost, Currency.USD),
            
            fuelBreakdown = fuelBreakdown,
            tollBreakdown = tollBreakdown,
            laborBreakdown = laborBreakdown,
            maintenanceBreakdown = maintenanceBreakdown
        )
    }
}
```

### 7.2 Cost-Optimized Route Selection

```kotlin
fun selectOptimalRoute(
    routes: List<Route>,
    truckProfile: TruckProfile,
    costProfile: TruckCostProfile
): RouteSelection {
    
    val routeCosts = routes.map { route ->
        route to calculateTotalRouteCost(route, truckProfile, costProfile)
    }
    
    // Find optimal route based on cost priority
    val optimalRoute = when (costProfile.costPriority) {
        CostPriority.MINIMIZE_TOTAL_COST -> 
            routeCosts.minByOrNull { it.second.totalCost.amount }
        
        CostPriority.MINIMIZE_FUEL ->
            routeCosts.minByOrNull { it.second.fuelCost.amount }
        
        CostPriority.MINIMIZE_TIME ->
            routeCosts.minByOrNull { it.first.duration.toSeconds() }
        
        CostPriority.AVOID_TOLLS ->
            routeCosts.minByOrNull { it.second.tollCost.amount }
        
        CostPriority.FASTEST_DELIVERY ->
            routeCosts.minByOrNull { it.first.duration.toSeconds() }
    }
    
    return RouteSelection(
        selectedRoute = optimalRoute!!.first,
        selectedCost = optimalRoute.second,
        alternatives = routeCosts.filter { it != optimalRoute }
            .map { RouteAlternative(it.first, it.second) }
    )
}

data class RouteSelection(
    val selectedRoute: Route,
    val selectedCost: RouteCost,
    val alternatives: List<RouteAlternative>
)

data class RouteAlternative(
    val route: Route,
    val cost: RouteCost
)
```

---

## 8. Cost Tracking & Reporting

### 8.1 Historical Cost Tracking

```kotlin
@Entity(tableName = "trip_costs")
data class TripCostRecord(
    @PrimaryKey val id: String,
    
    @ColumnInfo(name = "trip_id")
    val tripId: String,
    
    @ColumnInfo(name = "driver_id")
    val driverId: String?,
    
    @ColumnInfo(name = "truck_id")
    val truckId: String,
    
    @ColumnInfo(name = "origin")
    val origin: String,
    
    @ColumnInfo(name = "destination")
    val destination: String,
    
    @ColumnInfo(name = "distance_miles")
    val distanceMiles: Double,
    
    @ColumnInfo(name = "duration_hours")
    val durationHours: Double,
    
    @ColumnInfo(name = "fuel_cost")
    val fuelCost: Double,
    
    @ColumnInfo(name = "toll_cost")
    val tollCost: Double,
    
    @ColumnInfo(name = "labor_cost")
    val laborCost: Double,
    
    @ColumnInfo(name = "maintenance_cost")
    val maintenanceCost: Double,
    
    @ColumnInfo(name = "total_cost")
    val totalCost: Double,
    
    @ColumnInfo(name = "trip_date")
    val tripDate: Instant,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Instant
)
```

### 8.2 Cost Reporting

```kotlin
class CostReportGenerator(
    private val database: TripCostDatabase
) {
    
    suspend fun generateMonthlyReport(
        truckId: String,
        month: YearMonth
    ): MonthlyCostReport {
        
        val trips = database.tripCostDao().getTripsByMonth(
            truckId = truckId,
            startDate = month.atDay(1),
            endDate = month.atEndOfMonth()
        )
        
        return MonthlyCostReport(
            truckId = truckId,
            month = month,
            tripCount = trips.size,
            totalMiles = trips.sumOf { it.distanceMiles },
            totalFuelCost = Money(trips.sumOf { it.fuelCost }, Currency.USD),
            totalTollCost = Money(trips.sumOf { it.tollCost }, Currency.USD),
            totalLaborCost = Money(trips.sumOf { it.laborCost }, Currency.USD),
            totalMaintenanceCost = Money(trips.sumOf { it.maintenanceCost }, Currency.USD),
            totalCost = Money(trips.sumOf { it.totalCost }, Currency.USD),
            costPerMile = Money(
                trips.sumOf { it.totalCost } / trips.sumOf { it.distanceMiles },
                Currency.USD
            )
        )
    }
}

data class MonthlyCostReport(
    val truckId: String,
    val month: YearMonth,
    val tripCount: Int,
    val totalMiles: Double,
    val totalFuelCost: Money,
    val totalTollCost: Money,
    val totalLaborCost: Money,
    val totalMaintenanceCost: Money,
    val totalCost: Money,
    val costPerMile: Money
)
```

---

## 9. Testing & Validation

### 9.1 Cost Calculation Tests

```kotlin
class CostCalculationTest {
    
    @Test
    fun testFuelCostCalculation() {
        // Given: 500-mile route, 6 MPG truck, $3.50/gal fuel
        val route = createTestRoute(distanceMiles = 500.0)
        val profile = createTestProfile(mpg = 6.0, fuelPrice = 3.50)
        
        // When: Calculate fuel cost
        val fuelCost = fuelCalculator.calculateFuelCost(route, profile, isLoaded = true)
        
        // Then: Cost should be $291.67 (500/6 * $3.50)
        assertEquals(291.67, fuelCost.totalCost.amount, 0.01)
    }
    
    @Test
    fun testTotalCostOptimization() {
        // Compare two routes: one faster with tolls, one slower toll-free
        // Verify optimizer selects route based on cost priority
    }
}
```

---

**File Version**: 1.0  
**Last Updated**: 2025-11-21  
**Related Files**: truck_routing_api.md, compliance_engine.md, sdk_setup.md
