# HERE Compliance Engine

**Version**: 1.0  
**Date**: 2025-11-21  
**Tier**: Pro Only  
**Platforms**: Android, iOS

---

## 1. Overview

The Compliance Engine validates all truck routes against legal restrictions to ensure drivers stay within legal weight, height, width, length, and hazmat limits. This is the core safety and legal protection system for GemNav Pro tier.

**Legal Mandate**: Commercial drivers face severe penalties for violating truck restrictions:
- Weight violations: $500-$10,000 fines, possible vehicle impoundment
- Height violations: Property damage liability, potential license suspension
- Hazmat violations: Federal criminal charges, $75,000+ fines
- Bridge/tunnel violations: Structural damage liability, criminal negligence

**GemNav Compliance Guarantee**: All Pro tier routes are validated for legal compliance. If a violation is detected, the route is flagged and the user is warned before navigation begins.

---

## 2. Compliance Validation Workflow

### 2.1 Pre-Route Validation

**Validation occurs at three stages**:
1. **Before route calculation**: Validate truck profile against destination constraints
2. **After route calculation**: Analyze route notices for violations
3. **During navigation**: Real-time monitoring for unexpected restrictions

**Validation Pipeline**:

```kotlin
class ComplianceEngine(
    private val restrictionDatabase: RestrictionDatabase,
    private val routingEngine: TruckRoutingEngine
) {
    
    suspend fun validateRoute(
        route: Route,
        truckProfile: TruckProfile
    ): ComplianceResult {
        
        // Stage 1: Extract all restrictions from route
        val routeRestrictions = extractRestrictions(route)
        
        // Stage 2: Check each restriction against truck profile
        val violations = checkViolations(routeRestrictions, truckProfile)
        
        // Stage 3: Classify severity
        val severity = classifySeverity(violations)
        
        // Stage 4: Generate user-facing report
        val report = generateComplianceReport(violations, severity)
        
        return ComplianceResult(
            isCompliant = violations.isEmpty(),
            severity = severity,
            violations = violations,
            report = report,
            alternateRoutesAvailable = checkForAlternatives(route, violations)
        )
    }
    
    private fun extractRestrictions(route: Route): List<RouteRestriction> {
        val restrictions = mutableListOf<RouteRestriction>()
        
        route.sections.forEach { section ->
            // Process route notices
            section.notices.forEach { notice ->
                val restriction = when (notice.code) {
                    NoticeCode.VIOLATED_WEIGHT_RESTRICTION ->
                        RouteRestriction.Weight(
                            location = notice.position,
                            maxWeight = extractWeightLimit(notice),
                            roadName = section.roadName,
                            severity = RestrictionSeverity.HIGH
                        )
                    
                    NoticeCode.VIOLATED_HEIGHT_RESTRICTION ->
                        RouteRestriction.Height(
                            location = notice.position,
                            maxHeight = extractHeightLimit(notice),
                            obstacleType = extractObstacleType(notice), // Bridge, tunnel, etc.
                            severity = RestrictionSeverity.CRITICAL
                        )
                    
                    NoticeCode.VIOLATED_TRUCK_RESTRICTION ->
                        RouteRestriction.TruckProhibited(
                            location = notice.position,
                            reason = notice.localizedText,
                            severity = RestrictionSeverity.HIGH
                        )
                    
                    NoticeCode.VIOLATED_TUNNEL_CATEGORY ->
                        RouteRestriction.HazmatTunnel(
                            location = notice.position,
                            allowedCategories = extractAllowedCategories(notice),
                            severity = RestrictionSeverity.CRITICAL
                        )
                    
                    else -> null
                }
                
                restriction?.let { restrictions.add(it) }
            }
            
            // Check bridge restrictions
            section.spanAttributes?.forEach { span ->
                if (span.hasWeightLimit()) {
                    restrictions.add(
                        RouteRestriction.Bridge(
                            location = span.startPosition,
                            maxWeight = span.weightLimit,
                            bridgeName = span.name,
                            severity = RestrictionSeverity.HIGH
                        )
                    )
                }
            }
        }
        
        return restrictions
    }
    
    private fun checkViolations(
        restrictions: List<RouteRestriction>,
        profile: TruckProfile
    ): List<ComplianceViolation> {
        val violations = mutableListOf<ComplianceViolation>()
        
        restrictions.forEach { restriction ->
            val violation = when (restriction) {
                is RouteRestriction.Weight -> {
                    if (profile.grossWeight > restriction.maxWeight) {
                        ComplianceViolation.OverWeight(
                            restriction = restriction,
                            truckWeight = profile.grossWeight,
                            excess = profile.grossWeight - restriction.maxWeight
                        )
                    } else null
                }
                
                is RouteRestriction.Height -> {
                    if (profile.height > restriction.maxHeight) {
                        ComplianceViolation.OverHeight(
                            restriction = restriction,
                            truckHeight = profile.height,
                            excess = profile.height - restriction.maxHeight
                        )
                    } else null
                }
                
                is RouteRestriction.TruckProhibited -> {
                    // Truck not allowed on this road
                    ComplianceViolation.TruckNotAllowed(restriction)
                }
                
                is RouteRestriction.HazmatTunnel -> {
                    // Check if truck's tunnel category is allowed
                    if (!restriction.allowedCategories.contains(profile.tunnelCategory)) {
                        ComplianceViolation.HazmatNotAllowed(
                            restriction = restriction,
                            truckCategory = profile.tunnelCategory
                        )
                    } else null
                }
                
                is RouteRestriction.Bridge -> {
                    if (profile.grossWeight > restriction.maxWeight) {
                        ComplianceViolation.BridgeOverWeight(
                            restriction = restriction,
                            truckWeight = profile.grossWeight
                        )
                    } else null
                }
            }
            
            violation?.let { violations.add(it) }
        }
        
        return violations
    }
    
    private fun classifySeverity(violations: List<ComplianceViolation>): ComplianceSeverity {
        if (violations.isEmpty()) return ComplianceSeverity.COMPLIANT
        
        val hasCritical = violations.any { 
            it.severity == RestrictionSeverity.CRITICAL 
        }
        val hasHigh = violations.any { 
            it.severity == RestrictionSeverity.HIGH 
        }
        
        return when {
            hasCritical -> ComplianceSeverity.CRITICAL
            hasHigh -> ComplianceSeverity.HIGH
            else -> ComplianceSeverity.MODERATE
        }
    }
}
```

### 2.2 Violation Types

**Sealed Class Hierarchy**:

```kotlin
sealed class ComplianceViolation {
    abstract val restriction: RouteRestriction
    abstract val severity: RestrictionSeverity
    abstract val userMessage: String
    abstract val legalConsequence: String
    
    data class OverWeight(
        override val restriction: RouteRestriction.Weight,
        val truckWeight: Weight,
        val excess: Weight
    ) : ComplianceViolation() {
        override val severity = RestrictionSeverity.HIGH
        override val userMessage = 
            "Your truck (${truckWeight.display()}) exceeds the weight limit " +
            "(${restriction.maxWeight.display()}) by ${excess.display()}"
        override val legalConsequence = 
            "Operating an overweight vehicle can result in fines up to $10,000 " +
            "and vehicle impoundment"
    }
    
    data class OverHeight(
        override val restriction: RouteRestriction.Height,
        val truckHeight: Dimension,
        val excess: Dimension
    ) : ComplianceViolation() {
        override val severity = RestrictionSeverity.CRITICAL
        override val userMessage = 
            "Your truck (${truckHeight.display()}) exceeds ${restriction.obstacleType} " +
            "clearance (${restriction.maxHeight.display()}) by ${excess.display()}"
        override val legalConsequence = 
            "Striking a bridge or tunnel can result in property damage liability, " +
            "license suspension, and criminal charges"
    }
    
    data class TruckNotAllowed(
        override val restriction: RouteRestriction.TruckProhibited
    ) : ComplianceViolation() {
        override val severity = RestrictionSeverity.HIGH
        override val userMessage = 
            "Trucks are not allowed on ${restriction.roadName}: ${restriction.reason}"
        override val legalConsequence = 
            "Violating truck restrictions can result in fines and citations"
    }
    
    data class HazmatNotAllowed(
        override val restriction: RouteRestriction.HazmatTunnel,
        val truckCategory: TunnelCategory
    ) : ComplianceViolation() {
        override val severity = RestrictionSeverity.CRITICAL
        override val userMessage = 
            "Your hazmat category (${truckCategory}) is not allowed in this tunnel. " +
            "Allowed: ${restriction.allowedCategories.joinToString()}"
        override val legalConsequence = 
            "Violating hazmat tunnel restrictions is a federal offense with fines " +
            "exceeding $75,000 and possible criminal prosecution"
    }
    
    data class BridgeOverWeight(
        override val restriction: RouteRestriction.Bridge,
        val truckWeight: Weight
    ) : ComplianceViolation() {
        override val severity = RestrictionSeverity.CRITICAL
        override val userMessage = 
            "Your truck weight (${truckWeight.display()}) exceeds ${restriction.bridgeName} " +
            "weight limit (${restriction.maxWeight.display()})"
        override val legalConsequence = 
            "Crossing a bridge over its weight limit risks structural damage and " +
            "criminal liability for endangerment"
    }
}

enum class RestrictionSeverity {
    LOW,        // Warnings, preferences
    MODERATE,   // Minor legal issue (can be bypassed with acknowledgment)
    HIGH,       // Significant legal violation (strong warning)
    CRITICAL    // Immediate safety or legal danger (route should not be used)
}

enum class ComplianceSeverity {
    COMPLIANT,  // No violations
    MODERATE,   // Minor issues only
    HIGH,       // Significant violations present
    CRITICAL    // Critical safety or legal violations
}
```

---

## 3. User-Facing Compliance Reports

### 3.1 Compliance Report Generation

```kotlin
fun generateComplianceReport(
    violations: List<ComplianceViolation>,
    severity: ComplianceSeverity
): ComplianceReport {
    
    return ComplianceReport(
        overallStatus = severity,
        summary = generateSummary(violations, severity),
        violations = violations.map { violation ->
            ViolationReport(
                type = violation.javaClass.simpleName,
                severity = violation.severity,
                location = violation.restriction.location,
                userMessage = violation.userMessage,
                legalConsequence = violation.legalConsequence,
                recommendedAction = getRecommendedAction(violation)
            )
        },
        alternativeRoutesAvailable = violations.isNotEmpty(),
        timestamp = Instant.now()
    )
}

private fun generateSummary(
    violations: List<ComplianceViolation>,
    severity: ComplianceSeverity
): String {
    return when (severity) {
        ComplianceSeverity.COMPLIANT ->
            "✅ This route is compliant with all truck restrictions for your vehicle"
        
        ComplianceSeverity.MODERATE ->
            "⚠️ This route has ${violations.size} minor compliance issue(s) that " +
            "should be reviewed before proceeding"
        
        ComplianceSeverity.HIGH ->
            "🚫 This route violates ${violations.size} truck restriction(s). " +
            "Using this route may result in fines or citations"
        
        ComplianceSeverity.CRITICAL ->
            "🛑 CRITICAL: This route has ${violations.size} serious violation(s) " +
            "that could result in safety hazards, property damage, or criminal charges. " +
            "DO NOT use this route"
    }
}

private fun getRecommendedAction(violation: ComplianceViolation): String {
    return when (violation) {
        is ComplianceViolation.OverWeight ->
            "Reduce load weight to ${violation.restriction.maxWeight.display()} or find alternate route"
        
        is ComplianceViolation.OverHeight ->
            "Verify truck height measurement or find alternate route avoiding ${violation.restriction.obstacleType}"
        
        is ComplianceViolation.TruckNotAllowed ->
            "Find alternate route that allows commercial vehicles"
        
        is ComplianceViolation.HazmatNotAllowed ->
            "Use alternate route or change hazmat tunnel category if incorrect"
        
        is ComplianceViolation.BridgeOverWeight ->
            "Reduce load weight or find alternate route avoiding ${violation.restriction.bridgeName}"
    }
}

data class ComplianceReport(
    val overallStatus: ComplianceSeverity,
    val summary: String,
    val violations: List<ViolationReport>,
    val alternativeRoutesAvailable: Boolean,
    val timestamp: Instant
)

data class ViolationReport(
    val type: String,
    val severity: RestrictionSeverity,
    val location: GeoCoordinates,
    val userMessage: String,
    val legalConsequence: String,
    val recommendedAction: String
)
```

### 3.2 UI Display Guidelines

**Critical Violation Dialog**:
```kotlin
@Composable
fun CriticalViolationDialog(
    report: ComplianceReport,
    onFindAlternate: () -> Unit,
    onAcknowledge: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Require user action */ },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, tint = Color.Red)
                Spacer(Modifier.width(8.dp))
                Text("Route Compliance Warning", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    text = report.summary,
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Spacer(Modifier.height(16.dp))
                
                report.violations.forEach { violation ->
                    ComplianceViolationCard(violation)
                    Spacer(Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onFindAlternate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Find Alternate Route")
            }
        },
        dismissButton = {
            if (report.overallStatus != ComplianceSeverity.CRITICAL) {
                TextButton(onClick = onAcknowledge) {
                    Text("I Understand the Risks")
                }
            }
        }
    )
}

@Composable
fun ComplianceViolationCard(violation: ViolationReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (violation.severity) {
                RestrictionSeverity.CRITICAL -> Color(0xFFFFEBEE)
                RestrictionSeverity.HIGH -> Color(0xFFFFF3E0)
                else -> Color(0xFFFFFDE7)
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = violation.userMessage,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = "⚖️ Legal Risk: ${violation.legalConsequence}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666)
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = "✅ Action: ${violation.recommendedAction}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
```

---

## 4. Real-Time Compliance Monitoring

### 4.1 Navigation Compliance Checks

```kotlin
class NavigationComplianceMonitor(
    private val complianceEngine: ComplianceEngine,
    private val locationProvider: LocationProvider
) {
    
    private var activeRoute: Route? = null
    private var truckProfile: TruckProfile? = null
    private val upcomingRestrictions = mutableListOf<RouteRestriction>()
    
    fun startMonitoring(
        route: Route,
        profile: TruckProfile,
        alertCallback: (ComplianceAlert) -> Unit
    ) {
        activeRoute = route
        truckProfile = profile
        
        // Build list of upcoming restrictions
        upcomingRestrictions.clear()
        upcomingRestrictions.addAll(complianceEngine.extractRestrictions(route))
        
        // Monitor location
        locationProvider.startLocationUpdates { location ->
            checkProximityAlerts(location, alertCallback)
            updateRestrictionsList(location)
        }
    }
    
    private fun checkProximityAlerts(
        location: GeoCoordinates,
        callback: (ComplianceAlert) -> Unit
    ) {
        upcomingRestrictions.forEach { restriction ->
            val distanceToRestriction = location.distanceTo(restriction.location)
            
            // Alert at different distances based on severity
            val alertDistance = when (restriction.severity) {
                RestrictionSeverity.CRITICAL -> 2000.0 // 2km
                RestrictionSeverity.HIGH -> 1000.0     // 1km
                RestrictionSeverity.MODERATE -> 500.0  // 500m
                RestrictionSeverity.LOW -> 250.0       // 250m
            }
            
            if (distanceToRestriction <= alertDistance && 
                !restriction.alerted) {
                
                restriction.alerted = true
                callback(
                    ComplianceAlert(
                        restriction = restriction,
                        distanceMeters = distanceToRestriction,
                        estimatedTimeSeconds = calculateETA(distanceToRestriction)
                    )
                )
            }
        }
    }
    
    private fun updateRestrictionsList(location: GeoCoordinates) {
        // Remove restrictions that have been passed
        upcomingRestrictions.removeIf { restriction ->
            location.distanceTo(restriction.location) < 50.0 && 
            restriction.alerted
        }
    }
    
    fun stopMonitoring() {
        locationProvider.stopLocationUpdates()
        activeRoute = null
        truckProfile = null
        upcomingRestrictions.clear()
    }
}

data class ComplianceAlert(
    val restriction: RouteRestriction,
    val distanceMeters: Double,
    val estimatedTimeSeconds: Int
)
```

### 4.2 Voice Alerts

```kotlin
class ComplianceVoiceAlerts(
    private val textToSpeech: TextToSpeech
) {
    
    fun announceRestriction(alert: ComplianceAlert) {
        val message = when (alert.restriction) {
            is RouteRestriction.Height -> {
                val distance = formatDistance(alert.distanceMeters)
                "Warning: Low clearance ${alert.restriction.obstacleType} ahead in $distance. " +
                "Clearance is ${alert.restriction.maxHeight.display()}. Your truck is ${alert.truckProfile.height.display()}"
            }
            
            is RouteRestriction.Weight -> {
                val distance = formatDistance(alert.distanceMeters)
                "Warning: Weight restriction ahead in $distance. " +
                "Limit is ${alert.restriction.maxWeight.display()}"
            }
            
            is RouteRestriction.Bridge -> {
                "Approaching ${alert.restriction.bridgeName}. " +
                "Weight limit ${alert.restriction.maxWeight.display()}"
            }
            
            else -> return // Don't announce all restriction types
        }
        
        textToSpeech.speak(
            message,
            TextToSpeech.QUEUE_ADD,
            null,
            "compliance_alert_${alert.restriction.id}"
        )
    }
    
    private fun formatDistance(meters: Double): String {
        return when {
            meters < 100 -> "${meters.toInt()} meters"
            meters < 1000 -> "${(meters / 100).toInt() * 100} meters"
            else -> "${"%.1f".format(meters / 1000)} kilometers"
        }
    }
}
```

---

## 5. Compliance Database Integration

### 5.1 Restriction Data Sources

**Primary Sources**:
- HERE SDK restriction database (updated weekly)
- State DOT feeds (updated daily where available)
- User-reported restrictions (verified before inclusion)
- Infrastructure databases (bridges, tunnels)

**Database Schema** (simplified):

```kotlin
@Entity(tableName = "restrictions")
data class RestrictionEntity(
    @PrimaryKey val id: String,
    val type: RestrictionType,
    val location: GeoPoint,
    val maxWeight: Double?,      // Kilograms
    val maxHeight: Double?,      // Centimeters
    val maxWidth: Double?,       // Centimeters
    val maxLength: Double?,      // Centimeters
    val allowedCategories: String?, // Tunnel categories (comma-separated)
    val roadName: String,
    val jurisdiction: String,    // State/province code
    val sourceType: DataSource,
    val lastVerified: Instant,
    val confidence: Double       // 0.0 to 1.0
)

enum class DataSource {
    HERE_SDK,
    STATE_DOT,
    USER_REPORTED,
    MANUAL_VERIFICATION
}
```

### 5.2 Restriction Lookup

```kotlin
class RestrictionDatabase(
    private val dao: RestrictionDao,
    private val hereSDK: HERESDKWrapper
) {
    
    suspend fun getRestrictionsAlongRoute(
        route: Route
    ): List<RestrictionEntity> {
        val polyline = route.polyline
        val buffer = 100.0 // meters
        
        // Query local database first
        val localRestrictions = dao.getRestrictionsInBounds(
            polyline.boundingBox.expand(buffer)
        )
        
        // Merge with HERE SDK restrictions
        val hereRestrictions = hereSDK.getRouteRestrictions(route)
        
        return mergeRestrictions(localRestrictions, hereRestrictions)
    }
    
    private fun mergeRestrictions(
        local: List<RestrictionEntity>,
        remote: List<RestrictionEntity>
    ): List<RestrictionEntity> {
        // Deduplication logic
        // Prefer higher confidence sources
        // Prefer more recent data
        // Return merged list
    }
    
    suspend fun updateRestrictionsCache() {
        // Fetch latest restriction updates from HERE
        // Update local database
        // Prune old, unverified restrictions
    }
}
```

---

## 6. Testing & Validation

### 6.1 Compliance Test Scenarios

**Critical Tests**:
1. **Overweight Truck on Bridge**
   - Input: 85,000 lb truck, 80,000 lb bridge limit
   - Expected: CRITICAL violation, no route acceptance
   
2. **Oversized Load, Low Bridge**
   - Input: 14.5 ft truck, 13.5 ft clearance
   - Expected: CRITICAL violation, alternate route suggested
   
3. **Hazmat Tunnel Restriction**
   - Input: Category D hazmat, tunnel allows Category B only
   - Expected: CRITICAL violation, no route acceptance
   
4. **Truck Prohibited Road**
   - Input: Standard semi on "No Trucks" residential street
   - Expected: HIGH violation, warning with alternate
   
5. **Multi-Violation Route**
   - Input: Route with 3+ violations of varying severity
   - Expected: List all violations, classify as CRITICAL overall

### 6.2 Regression Tests

**Test Suite**:
- Verify all violation types detected correctly
- Confirm severity classification logic
- Validate user message generation
- Test alternate route suggestions
- Verify voice alert timing and content
- Confirm compliance report accuracy

---

## 7. Legal Disclaimer Management

### 7.1 Liability Protection

**User Acknowledgment Required**:

```kotlin
data class ComplianceAcknowledgment(
    val userId: String,
    val routeId: String,
    val violations: List<ComplianceViolation>,
    val acknowledgedAt: Instant,
    val userSignature: String, // "I understand the risks"
    val ipAddress: String
)

// Store all acknowledgments for legal protection
class ComplianceAcknowledgmentRepository {
    suspend fun recordAcknowledgment(ack: ComplianceAcknowledgment) {
        // Store in database
        // Upload to cloud backup
        // Retain for legal minimum (7 years)
    }
}
```

**Disclaimer Text** (shown with all violation warnings):

```
GemNav Pro provides truck routing based on publicly available restriction 
data and HERE Technologies databases. While we strive for accuracy, 
restrictions may change without notice. Drivers are responsible for:

• Verifying all route restrictions before departure
• Obeying posted signage and local regulations
• Ensuring their vehicle complies with all legal requirements
• Reporting any route compliance issues to GemNav support

By proceeding, you acknowledge these limitations and agree that GemNav 
is not liable for fines, violations, or damages resulting from route usage.
```

---

## 8. Performance & Optimization

### 8.1 Compliance Check Performance

**Targets**:
- Route validation: < 500ms for typical route
- Real-time monitoring: < 50ms per location update
- Database queries: < 100ms for restriction lookup
- Voice alert generation: < 200ms

### 8.2 Caching Strategy

```kotlin
class ComplianceCache {
    private val routeCache = LruCache<String, ComplianceResult>(50)
    
    fun getCachedResult(routeHash: String): ComplianceResult? {
        return routeCache.get(routeHash)
    }
    
    fun cacheResult(routeHash: String, result: ComplianceResult) {
        routeCache.put(routeHash, result)
    }
    
    fun invalidateForLocation(location: GeoCoordinates) {
        // Invalidate cached results near location
        // Used when restriction data updates
    }
}
```

---

**File Version**: 1.0  
**Last Updated**: 2025-11-21  
**Related Files**: truck_routing_api.md, restriction_database.md, legal_constraints.md
