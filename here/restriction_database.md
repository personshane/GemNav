# HERE Restriction Database

**Version**: 1.0  
**Date**: 2025-11-21  
**Tier**: Pro Only  
**Platforms**: Android, iOS

---

## 1. Overview

The Restriction Database stores all truck-specific legal restrictions including weight limits, height clearances, width restrictions, hazmat regulations, and time-based access controls. This database is the foundation of GemNav Pro's legal compliance system.

**Database Requirements**:
- Support offline operation (cached restrictions)
- Fast spatial queries (restrictions near route)
- Incremental updates (minimize data transfer)
- Version tracking (rollback capability)
- Multi-source integration (HERE + DOT feeds)

---

## 2. Database Architecture

### 2.1 Overall Structure

**Platform-Specific Implementations**:

**Android**: Room Database (SQLite)  
**iOS**: Core Data (SQLite backing)

**Database Components**:
1. **Restrictions Table**: Core restriction data
2. **Restriction Geometries**: Spatial data for restriction locations
3. **Update Metadata**: Version tracking and sync status
4. **User Reports**: Community-sourced restriction updates
5. **Verification Queue**: Restrictions pending verification

### 2.2 Restrictions Table Schema

**Android (Room)**:

```kotlin
@Database(
    entities = [
        RestrictionEntity::class,
        RestrictionGeometry::class,
        UpdateMetadata::class,
        UserReport::class
    ],
    version = 1,
    exportSchema = true
)
abstract class RestrictionDatabase : RoomDatabase() {
    abstract fun restrictionDao(): RestrictionDao
    abstract fun geometryDao(): GeometryDao
    abstract fun updateDao(): UpdateDao
    abstract fun reportDao(): UserReportDao
}

@Entity(
    tableName = "restrictions",
    indices = [
        Index(value = ["restriction_type"]),
        Index(value = ["jurisdiction"]),
        Index(value = ["last_verified"]),
        Index(value = ["confidence_score"])
    ]
)
data class RestrictionEntity(
    @PrimaryKey 
    val id: String,
    
    // Restriction type and classification
    @ColumnInfo(name = "restriction_type")
    val type: RestrictionType,
    
    @ColumnInfo(name = "restriction_subtype")
    val subtype: String?, // e.g., "bridge", "tunnel", "road"
    
    // Geographic data
    @ColumnInfo(name = "latitude")
    val latitude: Double,
    
    @ColumnInfo(name = "longitude")
    val longitude: Double,
    
    @ColumnInfo(name = "geometry_id")
    val geometryId: String?, // References line/polygon geometry if applicable
    
    // Restriction values
    @ColumnInfo(name = "max_weight_kg")
    val maxWeightKg: Double?,
    
    @ColumnInfo(name = "max_weight_per_axle_kg")
    val maxWeightPerAxleKg: Double?,
    
    @ColumnInfo(name = "max_height_cm")
    val maxHeightCm: Double?,
    
    @ColumnInfo(name = "max_width_cm")
    val maxWidthCm: Double?,
    
    @ColumnInfo(name = "max_length_cm")
    val maxLengthCm: Double?,
    
    @ColumnInfo(name = "allowed_tunnel_categories")
    val allowedTunnelCategories: String?, // Comma-separated: "B,C"
    
    @ColumnInfo(name = "prohibited_hazmat")
    val prohibitedHazmat: String?, // Comma-separated UN codes
    
    // Time-based restrictions
    @ColumnInfo(name = "time_restrictions")
    val timeRestrictions: String?, // JSON: {"weekdays": "08:00-18:00", "weekends": null}
    
    // Metadata
    @ColumnInfo(name = "road_name")
    val roadName: String?,
    
    @ColumnInfo(name = "jurisdiction")
    val jurisdiction: String, // ISO 3166-2 code (e.g., "US-CA", "CA-ON")
    
    @ColumnInfo(name = "authority")
    val authority: String?, // "Caltrans", "NYSDOT", etc.
    
    @ColumnInfo(name = "source_type")
    val sourceType: DataSource,
    
    @ColumnInfo(name = "source_id")
    val sourceId: String?, // External ID from source system
    
    @ColumnInfo(name = "confidence_score")
    val confidenceScore: Double, // 0.0 to 1.0
    
    // Timestamps
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    
    @ColumnInfo(name = "last_verified")
    val lastVerified: Instant,
    
    @ColumnInfo(name = "expires_at")
    val expiresAt: Instant?, // For temporary restrictions
    
    // Additional context
    @ColumnInfo(name = "description")
    val description: String?,
    
    @ColumnInfo(name = "enforcement_level")
    val enforcementLevel: EnforcementLevel
)

enum class RestrictionType {
    WEIGHT_LIMIT,
    HEIGHT_CLEARANCE,
    WIDTH_LIMIT,
    LENGTH_LIMIT,
    AXLE_WEIGHT,
    BRIDGE_WEIGHT,
    TRUCK_PROHIBITED,
    HAZMAT_PROHIBITED,
    TUNNEL_CATEGORY,
    TIME_RESTRICTED,
    SEASONAL,
    PERMIT_REQUIRED
}

enum class DataSource {
    HERE_SDK,           // HERE Technologies official data
    STATE_DOT,          // State Department of Transportation
    FEDERAL_DOT,        // Federal DOT (FHWA)
    MUNICIPAL,          // City/county data
    USER_REPORTED,      // Community-sourced
    MANUAL_ENTRY,       // GemNav staff verification
    THIRD_PARTY         // Other commercial providers
}

enum class EnforcementLevel {
    STRICTLY_ENFORCED,  // Cameras, frequent patrols
    REGULARLY_ENFORCED, // Normal enforcement
    RARELY_ENFORCED,    // Minimal enforcement
    ADVISORY_ONLY,      // Suggested, not enforced
    UNKNOWN             // Enforcement level not known
}
```

**iOS (Core Data)**:

```swift
@objc(RestrictionEntity)
public class RestrictionEntity: NSManagedObject {
    @NSManaged public var id: String
    @NSManaged public var restrictionType: String
    @NSManaged public var restrictionSubtype: String?
    
    // Geographic data
    @NSManaged public var latitude: Double
    @NSManaged public var longitude: Double
    @NSManaged public var geometryId: String?
    
    // Restriction values
    @NSManaged public var maxWeightKg: Double
    @NSManaged public var maxWeightPerAxleKg: Double
    @NSManaged public var maxHeightCm: Double
    @NSManaged public var maxWidthCm: Double
    @NSManaged public var maxLengthCm: Double
    @NSManaged public var allowedTunnelCategories: String?
    @NSManaged public var prohibitedHazmat: String?
    
    // Time-based restrictions
    @NSManaged public var timeRestrictions: String?
    
    // Metadata
    @NSManaged public var roadName: String?
    @NSManaged public var jurisdiction: String
    @NSManaged public var authority: String?
    @NSManaged public var sourceType: String
    @NSManaged public var sourceId: String?
    @NSManaged public var confidenceScore: Double
    
    // Timestamps
    @NSManaged public var createdAt: Date
    @NSManaged public var lastVerified: Date
    @NSManaged public var expiresAt: Date?
    
    // Additional context
    @NSManaged public var descriptionText: String?
    @NSManaged public var enforcementLevel: String
}
```

### 2.3 Restriction Geometries

**For restrictions that span a line or area**:

```kotlin
@Entity(
    tableName = "restriction_geometries",
    indices = [Index(value = ["restriction_id"])]
)
data class RestrictionGeometry(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "restriction_id")
    val restrictionId: String,
    
    @ColumnInfo(name = "geometry_type")
    val geometryType: GeometryType,
    
    @ColumnInfo(name = "coordinates")
    val coordinates: String, // GeoJSON or encoded polyline
    
    @ColumnInfo(name = "bounding_box")
    val boundingBox: String // "minLat,minLon,maxLat,maxLon"
)

enum class GeometryType {
    POINT,      // Single location
    LINE,       // Road segment
    POLYGON,    // Area restriction
    MULTI_POINT // Multiple discrete points
}
```

---

## 3. Data Access Layer

### 3.1 Restriction DAO (Android)

```kotlin
@Dao
interface RestrictionDao {
    
    // Spatial queries
    @Query("""
        SELECT * FROM restrictions 
        WHERE latitude BETWEEN :minLat AND :maxLat 
        AND longitude BETWEEN :minLon AND :maxLon
        AND (expires_at IS NULL OR expires_at > :now)
        ORDER BY confidence_score DESC
    """)
    suspend fun getRestrictionsInBounds(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        now: Instant = Instant.now()
    ): List<RestrictionEntity>
    
    // Type-specific queries
    @Query("""
        SELECT * FROM restrictions 
        WHERE restriction_type = :type
        AND jurisdiction = :jurisdiction
        AND confidence_score >= :minConfidence
        ORDER BY last_verified DESC
    """)
    suspend fun getRestrictionsByType(
        type: RestrictionType,
        jurisdiction: String,
        minConfidence: Double = 0.7
    ): List<RestrictionEntity>
    
    // Nearest restriction query
    @RawQuery
    suspend fun findNearestRestriction(query: SupportSQLiteQuery): RestrictionEntity?
    
    // Batch operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRestrictions(restrictions: List<RestrictionEntity>)
    
    @Update
    suspend fun updateRestriction(restriction: RestrictionEntity)
    
    @Query("DELETE FROM restrictions WHERE id IN (:ids)")
    suspend fun deleteRestrictions(ids: List<String>)
    
    // Maintenance queries
    @Query("""
        DELETE FROM restrictions 
        WHERE source_type = 'USER_REPORTED' 
        AND confidence_score < 0.5 
        AND last_verified < :cutoffDate
    """)
    suspend fun pruneLowConfidenceRestrictions(cutoffDate: Instant)
    
    @Query("""
        SELECT COUNT(*) FROM restrictions 
        WHERE jurisdiction = :jurisdiction
    """)
    suspend fun getRestrictionCountByJurisdiction(jurisdiction: String): Int
}
```

### 3.2 Advanced Spatial Queries

**Nearest Restriction Finder**:

```kotlin
class SpatialQueryHelper(private val database: RestrictionDatabase) {
    
    suspend fun findNearestRestriction(
        location: GeoCoordinates,
        maxDistanceMeters: Double,
        types: List<RestrictionType>
    ): RestrictionEntity? {
        
        // Calculate bounding box
        val bounds = calculateBoundingBox(location, maxDistanceMeters)
        
        // Get candidates within bounding box
        val candidates = database.restrictionDao().getRestrictionsInBounds(
            minLat = bounds.minLatitude,
            maxLat = bounds.maxLatitude,
            minLon = bounds.minLongitude,
            maxLon = bounds.maxLongitude
        )
        
        // Filter by type and calculate exact distances
        return candidates
            .filter { types.contains(it.type) }
            .map { restriction ->
                restriction to calculateDistance(
                    location,
                    GeoCoordinates(restriction.latitude, restriction.longitude)
                )
            }
            .filter { (_, distance) -> distance <= maxDistanceMeters }
            .minByOrNull { (_, distance) -> distance }
            ?.first
    }
    
    private fun calculateBoundingBox(
        center: GeoCoordinates,
        radiusMeters: Double
    ): GeoBoundingBox {
        // Haversine formula to calculate lat/lon deltas
        val latDelta = radiusMeters / 111320.0 // meters per degree latitude
        val lonDelta = radiusMeters / (111320.0 * cos(Math.toRadians(center.latitude)))
        
        return GeoBoundingBox(
            minLatitude = center.latitude - latDelta,
            maxLatitude = center.latitude + latDelta,
            minLongitude = center.longitude - lonDelta,
            maxLongitude = center.longitude + lonDelta
        )
    }
    
    private fun calculateDistance(p1: GeoCoordinates, p2: GeoCoordinates): Double {
        // Haversine distance calculation
        val R = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(p2.latitude - p1.latitude)
        val dLon = Math.toRadians(p2.longitude - p1.longitude)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(p1.latitude)) * cos(Math.toRadians(p2.latitude)) *
                sin(dLon / 2) * sin(dLon / 2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }
}
```

---

## 4. Data Synchronization

### 4.1 Update Metadata Tracking

```kotlin
@Entity(tableName = "update_metadata")
data class UpdateMetadata(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "jurisdiction")
    val jurisdiction: String,
    
    @ColumnInfo(name = "data_source")
    val dataSource: DataSource,
    
    @ColumnInfo(name = "last_sync_timestamp")
    val lastSyncTimestamp: Instant,
    
    @ColumnInfo(name = "last_sync_version")
    val lastSyncVersion: String,
    
    @ColumnInfo(name = "next_sync_due")
    val nextSyncDue: Instant,
    
    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus,
    
    @ColumnInfo(name = "restrictions_count")
    val restrictionsCount: Int,
    
    @ColumnInfo(name = "bytes_downloaded")
    val bytesDownloaded: Long
)

enum class SyncStatus {
    UP_TO_DATE,
    UPDATE_AVAILABLE,
    SYNCING,
    SYNC_FAILED,
    NEVER_SYNCED
}
```

### 4.2 Incremental Update System

```kotlin
class RestrictionSyncManager(
    private val database: RestrictionDatabase,
    private val hereAPI: HEREAPIClient,
    private val dotFeeds: DOTFeedClient
) {
    
    suspend fun performIncrementalSync(
        jurisdiction: String
    ): SyncResult {
        
        // Get last sync metadata
        val metadata = database.updateDao().getMetadata(jurisdiction)
            ?: createNewMetadata(jurisdiction)
        
        // Fetch updates since last sync
        val updates = hereAPI.getRestrictionUpdates(
            jurisdiction = jurisdiction,
            sinceVersion = metadata.lastSyncVersion,
            sinceTimestamp = metadata.lastSyncTimestamp
        )
        
        // Process updates
        val (added, modified, deleted) = processUpdates(updates)
        
        // Apply to database
        database.withTransaction {
            // Insert new restrictions
            database.restrictionDao().insertRestrictions(added)
            
            // Update modified restrictions
            modified.forEach { restriction ->
                database.restrictionDao().updateRestriction(restriction)
            }
            
            // Delete removed restrictions
            database.restrictionDao().deleteRestrictions(deleted.map { it.id })
            
            // Update metadata
            database.updateDao().updateMetadata(
                metadata.copy(
                    lastSyncTimestamp = Instant.now(),
                    lastSyncVersion = updates.version,
                    syncStatus = SyncStatus.UP_TO_DATE,
                    restrictionsCount = metadata.restrictionsCount + added.size - deleted.size
                )
            )
        }
        
        return SyncResult(
            jurisdiction = jurisdiction,
            addedCount = added.size,
            modifiedCount = modified.size,
            deletedCount = deleted.size,
            timestamp = Instant.now()
        )
    }
    
    private fun processUpdates(
        updates: RestrictionUpdateResponse
    ): Triple<List<RestrictionEntity>, List<RestrictionEntity>, List<RestrictionEntity>> {
        
        val added = mutableListOf<RestrictionEntity>()
        val modified = mutableListOf<RestrictionEntity>()
        val deleted = mutableListOf<RestrictionEntity>()
        
        updates.changes.forEach { change ->
            when (change.type) {
                ChangeType.ADDED -> added.add(change.toEntity())
                ChangeType.MODIFIED -> modified.add(change.toEntity())
                ChangeType.DELETED -> deleted.add(change.toEntity())
            }
        }
        
        return Triple(added, modified, deleted)
    }
}

data class SyncResult(
    val jurisdiction: String,
    val addedCount: Int,
    val modifiedCount: Int,
    val deletedCount: Int,
    val timestamp: Instant
)
```

### 4.3 Sync Scheduling

```kotlin
class RestrictionSyncScheduler(
    private val workManager: WorkManager,
    private val syncManager: RestrictionSyncManager
) {
    
    fun schedulePeriodicSync() {
        val syncRequest = PeriodicWorkRequestBuilder<RestrictionSyncWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            "restriction_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
    
    fun syncNow(jurisdiction: String? = null) {
        val syncRequest = OneTimeWorkRequestBuilder<RestrictionSyncWorker>()
            .setInputData(
                workDataOf("jurisdiction" to jurisdiction)
            )
            .build()
        
        workManager.enqueue(syncRequest)
    }
}

class RestrictionSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        val jurisdiction = inputData.getString("jurisdiction")
        
        return try {
            if (jurisdiction != null) {
                syncManager.performIncrementalSync(jurisdiction)
            } else {
                syncManager.syncAllJurisdictions()
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("RestrictionSync", "Sync failed", e)
            Result.retry()
        }
    }
}
```

---

## 5. Data Sources Integration

### 5.1 HERE SDK Integration

```kotlin
class HERERestrictionProvider(
    private val sdkEngine: SDKNativeEngine
) {
    
    suspend fun fetchRestrictions(
        boundingBox: GeoBoundingBox,
        categories: List<RestrictionType>
    ): List<RestrictionEntity> {
        
        // Query HERE SDK for restrictions in area
        val searchOptions = SearchOptions().apply {
            this.maxResults = 500
            this.categories = categories.map { it.toHERECategory() }
        }
        
        return withContext(Dispatchers.IO) {
            // HERE SDK query (simplified)
            val results = searchEngine.searchInArea(boundingBox, searchOptions)
            
            results.mapNotNull { result ->
                convertHEREResultToEntity(result)
            }
        }
    }
    
    private fun convertHEREResultToEntity(
        result: HERESearchResult
    ): RestrictionEntity? {
        // Convert HERE SDK result format to our entity format
        return try {
            RestrictionEntity(
                id = "here_${result.id}",
                type = parseRestrictionType(result),
                latitude = result.coordinates.latitude,
                longitude = result.coordinates.longitude,
                maxWeightKg = result.attributes["maxWeight"] as? Double,
                maxHeightCm = result.attributes["maxHeight"] as? Double,
                sourceType = DataSource.HERE_SDK,
                sourceId = result.id,
                confidenceScore = 0.95, // HERE data is highly reliable
                createdAt = Instant.now(),
                lastVerified = Instant.now(),
                // ... other fields
            )
        } catch (e: Exception) {
            Log.e("HEREProvider", "Failed to convert result", e)
            null
        }
    }
}
```

### 5.2 State DOT Feed Integration

```kotlin
class DOTFeedClient(
    private val httpClient: HttpClient
) {
    
    suspend fun fetchStateRestrictions(
        stateCode: String
    ): List<RestrictionEntity> {
        
        // Different states have different feed formats
        val feedUrl = getDOTFeedUrl(stateCode)
        
        return when (getFeedFormat(stateCode)) {
            FeedFormat.JSON -> parseJSONFeed(fetchFeed(feedUrl))
            FeedFormat.XML -> parseXMLFeed(fetchFeed(feedUrl))
            FeedFormat.CSV -> parseCSVFeed(fetchFeed(feedUrl))
            FeedFormat.GTFS -> parseGTFSFeed(fetchFeed(feedUrl))
        }
    }
    
    private suspend fun fetchFeed(url: String): String {
        return httpClient.get(url).bodyAsText()
    }
    
    private fun parseJSONFeed(content: String): List<RestrictionEntity> {
        // Parse state-specific JSON format
        // Convert to RestrictionEntity
        // Set confidence based on data quality
    }
    
    private fun getDOTFeedUrl(stateCode: String): String {
        // Map of state DOT restriction feed URLs
        return when (stateCode) {
            "CA" -> "https://roads.dot.ca.gov/api/restrictions"
            "NY" -> "https://511ny.org/api/restrictions"
            "TX" -> "https://www.txdot.gov/data/restrictions.json"
            // ... other states
            else -> throw IllegalArgumentException("State $stateCode not supported")
        }
    }
}

enum class FeedFormat {
    JSON, XML, CSV, GTFS
}
```

---

## 6. User-Reported Restrictions

### 6.1 User Report Schema

```kotlin
@Entity(tableName = "user_reports")
data class UserReport(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "user_id")
    val userId: String,
    
    @ColumnInfo(name = "restriction_type")
    val restrictionType: RestrictionType,
    
    @ColumnInfo(name = "latitude")
    val latitude: Double,
    
    @ColumnInfo(name = "longitude")
    val longitude: Double,
    
    @ColumnInfo(name = "description")
    val description: String,
    
    @ColumnInfo(name = "photo_urls")
    val photoUrls: String?, // Comma-separated URLs
    
    @ColumnInfo(name = "reported_at")
    val reportedAt: Instant,
    
    @ColumnInfo(name = "verification_status")
    val verificationStatus: VerificationStatus,
    
    @ColumnInfo(name = "verified_at")
    val verifiedAt: Instant?,
    
    @ColumnInfo(name = "verified_by")
    val verifiedBy: String?, // Staff member ID
    
    @ColumnInfo(name = "upvotes")
    val upvotes: Int,
    
    @ColumnInfo(name = "downvotes")
    val downvotes: Int
)

enum class VerificationStatus {
    PENDING,
    VERIFIED,
    REJECTED,
    NEEDS_MORE_INFO
}
```

### 6.2 Report Submission & Verification

```kotlin
class UserReportManager(
    private val database: RestrictionDatabase,
    private val apiClient: GemNavAPIClient
) {
    
    suspend fun submitReport(
        report: UserReport
    ): Result<String> {
        
        // Store locally first
        database.reportDao().insertReport(report)
        
        // Upload to server for verification
        return try {
            val reportId = apiClient.uploadReport(report)
            Result.success(reportId)
        } catch (e: Exception) {
            // Will retry later via sync
            Result.success(report.id)
        }
    }
    
    suspend fun processVerifiedReports() {
        // Fetch verified reports from server
        val verifiedReports = apiClient.getVerifiedReports()
        
        // Convert to restriction entities
        val newRestrictions = verifiedReports.map { report ->
            RestrictionEntity(
                id = "user_${report.id}",
                type = report.restrictionType,
                latitude = report.latitude,
                longitude = report.longitude,
                sourceType = DataSource.USER_REPORTED,
                confidenceScore = calculateConfidenceScore(report),
                // ... other fields from report
            )
        }
        
        // Add to database
        database.restrictionDao().insertRestrictions(newRestrictions)
    }
    
    private fun calculateConfidenceScore(report: UserReport): Double {
        // Confidence based on:
        // - Upvotes vs downvotes
        // - User reputation
        // - Photo evidence
        // - Verification status
        
        val voteRatio = if (report.upvotes + report.downvotes > 0) {
            report.upvotes.toDouble() / (report.upvotes + report.downvotes)
        } else {
            0.5
        }
        
        val hasPhotos = !report.photoUrls.isNullOrEmpty()
        val isVerified = report.verificationStatus == VerificationStatus.VERIFIED
        
        return when {
            isVerified && hasPhotos -> 0.9
            isVerified -> 0.8
            hasPhotos && voteRatio > 0.7 -> 0.7
            voteRatio > 0.7 -> 0.6
            else -> 0.4
        }
    }
}
```

---

## 7. Offline Operation

### 7.1 Pre-Caching Strategy

```kotlin
class OfflineRestrictionCache(
    private val database: RestrictionDatabase
) {
    
    suspend fun cacheForRegion(
        region: GeoBoundingBox,
        jurisdiction: String
    ) {
        // Download all restrictions in region
        val restrictions = fetchRestrictionsForRegion(region, jurisdiction)
        
        // Store in database
        database.restrictionDao().insertRestrictions(restrictions)
        
        // Mark region as cached
        markRegionCached(region, jurisdiction)
    }
    
    suspend fun getCachedRestrictions(
        location: GeoCoordinates,
        radiusMeters: Double
    ): List<RestrictionEntity> {
        
        val bounds = calculateBoundingBox(location, radiusMeters)
        
        return database.restrictionDao().getRestrictionsInBounds(
            minLat = bounds.minLatitude,
            maxLat = bounds.maxLatitude,
            minLon = bounds.minLongitude,
            maxLon = bounds.maxLongitude
        )
    }
    
    suspend fun isCached(location: GeoCoordinates): Boolean {
        // Check if we have cached restrictions for this area
        val count = database.restrictionDao().getRestrictionCountInArea(
            centerLat = location.latitude,
            centerLon = location.longitude,
            radiusKm = 50.0
        )
        
        return count > 0
    }
}
```

---

## 8. Performance Optimization

### 8.1 Query Optimization

**Spatial Index Creation**:

```sql
-- Android SQLite
CREATE INDEX idx_restrictions_spatial 
ON restrictions(latitude, longitude);

CREATE INDEX idx_restrictions_type_jurisdiction 
ON restrictions(restriction_type, jurisdiction);

CREATE INDEX idx_restrictions_confidence 
ON restrictions(confidence_score DESC, last_verified DESC);
```

**Query Performance Targets**:
- Bounding box query: < 50ms for 1000 restrictions
- Nearest restriction: < 20ms
- Type-filtered query: < 30ms
- Bulk insert: < 500ms for 10,000 restrictions

### 8.2 Database Size Management

```kotlin
class DatabaseMaintenanceManager(
    private val database: RestrictionDatabase
) {
    
    suspend fun performMaintenance() {
        // Remove expired temporary restrictions
        removeExpiredRestrictions()
        
        // Prune low-confidence user reports
        pruneLowConfidenceReports()
        
        // Vacuum database
        vacuumDatabase()
        
        // Update statistics
        updateDatabaseStatistics()
    }
    
    private suspend fun removeExpiredRestrictions() {
        database.restrictionDao().query("""
            DELETE FROM restrictions 
            WHERE expires_at IS NOT NULL 
            AND expires_at < datetime('now')
        """)
    }
    
    private suspend fun pruneLowConfidenceReports() {
        val cutoffDate = Instant.now().minus(90, ChronoUnit.DAYS)
        database.restrictionDao().pruneLowConfidenceRestrictions(cutoffDate)
    }
    
    private suspend fun vacuumDatabase() {
        database.openHelper.writableDatabase.execSQL("VACUUM")
    }
}
```

---

## 9. Testing & Validation

### 9.1 Data Quality Tests

**Automated Tests**:
```kotlin
class RestrictionDatabaseTest {
    
    @Test
    fun testSpatialQueryPerformance() {
        // Insert 10,000 test restrictions
        // Measure query time
        // Assert < 50ms
    }
    
    @Test
    fun testDataIntegrity() {
        // Verify all restrictions have required fields
        // Check confidence scores in valid range
        // Verify jurisdictions match expected format
    }
    
    @Test
    fun testIncrementalSync() {
        // Simulate sync with updates
        // Verify added/modified/deleted counts
        // Confirm no duplicates
    }
}
```

---

**File Version**: 1.0  
**Last Updated**: 2025-11-21  
**Related Files**: compliance_engine.md, truck_routing_api.md, sdk_setup.md
