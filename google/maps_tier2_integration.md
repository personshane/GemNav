# Google Maps SDK Integration - Plus Tier

**Version**: 1.0  
**Tier**: Plus  
**Platform**: Android (iOS equivalent via Apple Maps SDK + Google Maps for iOS SDK)

---

## 1. SDK Selection & Licensing

**Android**: Google Maps SDK for Android v18.2.0+  
**iOS**: Google Maps SDK for iOS v8.4.0+

**API Keys Required**:
- Maps SDK for Android
- Directions API
- Places API
- Distance Matrix API (route optimization)
- Geocoding API

**Billing**: Standard Google Maps Platform pricing applies to Plus tier users. App must implement quota management.

**Legal Compliance**: Must follow Google Maps Platform Terms of Service, including attribution requirements and no data extraction.

---

## 2. SDK Initialization

### Android Implementation

```kotlin
// Application class initialization
class GemNavApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Google Maps with API key
        MapsInitializer.initialize(
            applicationContext,
            MapsInitializer.Renderer.LATEST
        ) { renderer ->
            when (renderer) {
                MapsInitializer.Renderer.LATEST -> 
                    Log.d("Maps", "Latest renderer initialized")
                MapsInitializer.Renderer.LEGACY -> 
                    Log.d("Maps", "Legacy renderer initialized")
            }
        }
    }
}

// MapFragment setup
class PlusNavFragment : Fragment() {
    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { map ->
            googleMap = map
            configureMap(map)
        }
    }
    
    private fun configureMap(map: GoogleMap) {
        map.apply {
            uiSettings.isZoomControlsEnabled = false
            uiSettings.isMyLocationButtonEnabled = true
            uiSettings.isCompassEnabled = true
            mapType = GoogleMap.MAP_TYPE_NORMAL
            
            // Traffic layer
            isTrafficEnabled = true
        }
    }
}
```

### iOS Implementation

```swift
// AppDelegate initialization
func application(_ application: UIApplication, 
                didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
    GMSServices.provideAPIKey("YOUR_API_KEY")
    return true
}

// MapViewController setup
class PlusNavViewController: UIViewController {
    var mapView: GMSMapView!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        let camera = GMSCameraPosition.camera(
            withLatitude: 37.7749,
            longitude: -122.4194,
            zoom: 14.0
        )
        
        mapView = GMSMapView.map(withFrame: view.bounds, camera: camera)
        mapView.isMyLocationEnabled = true
        mapView.settings.myLocationButton = true
        mapView.settings.compassButton = true
        mapView.isTrafficEnabled = true
        
        view.addSubview(mapView)
    }
}
```

---

## 3. Route Calculation Architecture

### Flow Overview

1. User query → Gemini Cloud API (natural language processing)
2. Gemini extracts destinations → Geocoding API (address resolution)
3. Waypoints validated → Directions API (route calculation)
4. Multiple routes returned → Gemini Cloud API (route reasoning & recommendation)
5. Selected route → Map rendering + turn-by-turn navigation

### Multi-Waypoint Routing

**Maximum Waypoints**: 10 stops (Google Directions API limit: 25 waypoints, but UX constrains to 10)

**Optimization**: Use `optimize:true` parameter in Directions API for automatic waypoint reordering.

```kotlin
// Directions API request structure
data class RouteRequest(
    val origin: LatLng,
    val destination: LatLng,
    val waypoints: List<Waypoint>,
    val travelMode: TravelMode = TravelMode.DRIVING,
    val alternatives: Boolean = true,
    val optimizeWaypoints: Boolean = true,
    val departureTime: Long? = null, // For traffic-aware routing
    val trafficModel: TrafficModel = TrafficModel.BEST_GUESS
)

data class Waypoint(
    val location: LatLng,
    val stopover: Boolean = true // false for via points
)

enum class TravelMode {
    DRIVING, WALKING, BICYCLING, TRANSIT
}

enum class TrafficModel {
    BEST_GUESS, PESSIMISTIC, OPTIMISTIC
}
```

### API Call Implementation

```kotlin
class DirectionsService(private val apiKey: String) {
    private val client = OkHttpClient()
    private val baseUrl = "https://maps.googleapis.com/maps/api/directions/json"
    
    suspend fun calculateRoute(request: RouteRequest): DirectionsResponse {
        val url = buildDirectionsUrl(request)
        
        return withContext(Dispatchers.IO) {
            val httpRequest = Request.Builder()
                .url(url)
                .get()
                .build()
            
            val response = client.newCall(httpRequest).execute()
            val json = response.body?.string() ?: throw IOException("Empty response")
            
            Json.decodeFromString(json)
        }
    }
    
    private fun buildDirectionsUrl(request: RouteRequest): String {
        return buildString {
            append(baseUrl)
            append("?origin=${request.origin.toUrlParam()}")
            append("&destination=${request.destination.toUrlParam()}")
            
            if (request.waypoints.isNotEmpty()) {
                val waypointsParam = request.waypoints.joinToString("|") { wp ->
                    if (!wp.stopover) "via:" else "" + wp.location.toUrlParam()
                }
                append("&waypoints=")
                if (request.optimizeWaypoints) append("optimize:true|")
                append(waypointsParam)
            }
            
            append("&mode=${request.travelMode.name.lowercase()}")
            append("&alternatives=${request.alternatives}")
            append("&departure_time=${request.departureTime ?: "now"}")
            append("&traffic_model=${request.trafficModel.name.lowercase()}")
            append("&key=$apiKey")
        }
    }
}

private fun LatLng.toUrlParam(): String = "$latitude,$longitude"
```

---

## 4. Route Rendering

### Polyline Decoding & Display

```kotlin
class RouteRenderer(private val map: GoogleMap) {
    private var currentPolyline: Polyline? = null
    private val alternativePolylines = mutableListOf<Polyline>()
    
    fun renderMainRoute(encodedPath: String, color: Int = Color.BLUE) {
        // Clear existing route
        currentPolyline?.remove()
        
        val decodedPath = PolyUtil.decode(encodedPath)
        
        currentPolyline = map.addPolyline(
            PolylineOptions()
                .addAll(decodedPath)
                .color(color)
                .width(12f)
                .jointType(JointType.ROUND)
                .startCap(RoundCap())
                .endCap(RoundCap())
        )
        
        // Fit camera to route bounds
        val boundsBuilder = LatLngBounds.Builder()
        decodedPath.forEach { boundsBuilder.include(it) }
        map.animateCamera(
            CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100)
        )
    }
    
    fun renderAlternativeRoutes(routes: List<String>) {
        // Clear existing alternatives
        alternativePolylines.forEach { it.remove() }
        alternativePolylines.clear()
        
        routes.forEach { encodedPath ->
            val decodedPath = PolyUtil.decode(encodedPath)
            val polyline = map.addPolyline(
                PolylineOptions()
                    .addAll(decodedPath)
                    .color(Color.GRAY)
                    .width(8f)
                    .jointType(JointType.ROUND)
                    .clickable(true)
            )
            alternativePolylines.add(polyline)
        }
    }
    
    fun clearAllRoutes() {
        currentPolyline?.remove()
        alternativePolylines.forEach { it.remove() }
        alternativePolylines.clear()
    }
}
```

### Markers & Waypoints

```kotlin
class WaypointMarkerManager(private val map: GoogleMap) {
    private val markers = mutableMapOf<String, Marker>()
    
    fun addOriginMarker(location: LatLng, title: String): Marker {
        val marker = map.addMarker(
            MarkerOptions()
                .position(location)
                .title(title)
                .icon(BitmapDescriptorFactory.defaultMarker(
                    BitmapDescriptorFactory.HUE_GREEN
                ))
        )
        markers["origin"] = marker!!
        return marker
    }
    
    fun addWaypointMarkers(waypoints: List<Pair<LatLng, String>>) {
        waypoints.forEachIndexed { index, (location, title) ->
            val marker = map.addMarker(
                MarkerOptions()
                    .position(location)
                    .title("${index + 1}. $title")
                    .icon(BitmapDescriptorFactory.defaultMarker(
                        BitmapDescriptorFactory.HUE_AZURE
                    ))
            )
            markers["waypoint_$index"] = marker!!
        }
    }
    
    fun addDestinationMarker(location: LatLng, title: String): Marker {
        val marker = map.addMarker(
            MarkerOptions()
                .position(location)
                .title(title)
                .icon(BitmapDescriptorFactory.defaultMarker(
                    BitmapDescriptorFactory.HUE_RED
                ))
        )
        markers["destination"] = marker!!
        return marker
    }
    
    fun clearAllMarkers() {
        markers.values.forEach { it.remove() }
        markers.clear()
    }
}
```

---

## 5. Turn-by-Turn Navigation

### Navigation UI Components

**Required Elements**:
- Top instruction banner (next maneuver)
- Distance to next turn
- Estimated time of arrival (ETA)
- Current speed (if available)
- Lane guidance visualization
- Map reorientation (heading-up mode)

```kotlin
data class NavigationState(
    val currentStep: RouteStep,
    val nextStep: RouteStep?,
    val distanceToNextTurn: Int, // meters
    val timeToNextTurn: Int, // seconds
    val totalDistanceRemaining: Int,
    val totalTimeRemaining: Int,
    val currentSpeed: Float?, // m/s
    val speedLimit: Int? // km/h
)

class NavigationManager(
    private val map: GoogleMap,
    private val route: Route
) {
    private var currentStepIndex = 0
    private var locationListener: LocationListener? = null
    
    fun startNavigation(locationSource: FusedLocationProviderClient) {
        // Enable 3D buildings and tilt
        map.apply {
            isBuildingsEnabled = true
            mapType = GoogleMap.MAP_TYPE_NORMAL
        }
        
        // Start location tracking
        locationSource.lastLocation.addOnSuccessListener { location ->
            location?.let {
                updateNavigationState(LatLng(it.latitude, it.longitude))
            }
        }
        
        // Request location updates
        val locationRequest = LocationRequest.create().apply {
            interval = 1000 // 1 second
            fastestInterval = 500
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        
        locationSource.requestLocationUpdates(
            locationRequest,
            object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { loc ->
                        updateNavigationState(LatLng(loc.latitude, loc.longitude))
                    }
                }
            },
            Looper.getMainLooper()
        )
    }
    
    private fun updateNavigationState(currentLocation: LatLng) {
        val currentStep = route.steps[currentStepIndex]
        val distanceToNext = calculateDistance(currentLocation, currentStep.endLocation)
        
        // Check if approaching next step (within 50 meters)
        if (distanceToNext < 50 && currentStepIndex < route.steps.size - 1) {
            currentStepIndex++
            // Trigger UI update and voice announcement
        }
        
        // Update camera to follow user
        map.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(currentLocation)
                    .zoom(17f)
                    .bearing(calculateBearing(currentLocation, currentStep.endLocation))
                    .tilt(45f)
                    .build()
            )
        )
    }
    
    private fun calculateDistance(from: LatLng, to: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            from.latitude, from.longitude,
            to.latitude, to.longitude,
            results
        )
        return results[0]
    }
    
    private fun calculateBearing(from: LatLng, to: LatLng): Float {
        val results = FloatArray(2)
        Location.distanceBetween(
            from.latitude, from.longitude,
            to.latitude, to.longitude,
            results
        )
        return results[1]
    }
}
```

---

## 6. Real-Time Traffic Integration

### Traffic Layer Display

Traffic overlay is enabled by default in Plus tier:

```kotlin
googleMap.isTrafficEnabled = true
```

### Traffic-Aware Routing

Use `departure_time=now` and `traffic_model` parameters in Directions API requests to get traffic-adjusted ETAs.

```kotlin
// Periodic route recalculation during navigation
class TrafficMonitor(
    private val directionsService: DirectionsService,
    private val currentRoute: Route
) {
    private val recalculationInterval = 5 * 60 * 1000L // 5 minutes
    
    fun startMonitoring() {
        Timer().scheduleAtFixedRate(recalculationInterval, recalculationInterval) {
            checkForBetterRoute()
        }
    }
    
    private suspend fun checkForBetterRoute() {
        val newRoute = directionsService.calculateRoute(
            RouteRequest(
                origin = currentRoute.currentLocation,
                destination = currentRoute.destination,
                waypoints = currentRoute.remainingWaypoints,
                departureTime = System.currentTimeMillis()
            )
        )
        
        // Compare ETAs
        if (newRoute.duration < currentRoute.remainingDuration - 300) { // 5+ min savings
            // Notify user of faster route available
            notifyBetterRouteAvailable(newRoute)
        }
    }
}
```

---

## 7. Places API Integration

### Search & Autocomplete

```kotlin
class PlacesService(private val context: Context) {
    private val placesClient: PlacesClient = Places.createClient(context)
    
    fun searchPlaces(query: String, callback: (List<AutocompletePrediction>) -> Unit) {
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .setCountries("US", "CA") // Restrict to supported countries
            .build()
        
        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                callback(response.autocompletePredictions)
            }
            .addOnFailureListener { exception ->
                Log.e("Places", "Autocomplete error", exception)
            }
    }
    
    fun getPlaceDetails(placeId: String, callback: (Place) -> Unit) {
        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG,
            Place.Field.PHONE_NUMBER,
            Place.Field.WEBSITE_URI,
            Place.Field.RATING,
            Place.Field.OPENING_HOURS
        )
        
        val request = FetchPlaceRequest.builder(placeId, placeFields).build()
        
        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                callback(response.place)
            }
            .addOnFailureListener { exception ->
                Log.e("Places", "Place details error", exception)
            }
    }
}
```

---

## 8. Offline Maps Support

### Region Download

```kotlin
class OfflineMapManager(private val context: Context) {
    private val offlineManager: OfflineRegionManager = 
        OfflineRegionManager.getInstance(context)
    
    fun downloadRegion(
        bounds: LatLngBounds,
        regionName: String,
        callback: (success: Boolean) -> Unit
    ) {
        // Note: Google Maps SDK for Android doesn't support offline downloads directly
        // Must use Maps SDK for offline navigation or implement custom caching
        
        // Alternative: Use Google Maps SDK Lite mode with cached tiles
        // Or integrate MapBox SDK for offline support (requires separate license)
    }
}
```

**Important**: Google Maps SDK for Android has limited offline capabilities. Consider:
- Pre-caching visible map tiles
- Saving route geometry locally
- Implementing offline fallback UI
- Or use MapBox SDK (requires additional licensing)

---

## 9. Attribution & Legal Compliance

### Required Attribution

All map views must display Google Maps attribution. This is handled automatically by the SDK, but ensure:

1. Attribution text is never obscured by UI elements
2. Google logo is always visible
3. "Terms of Use" and "Privacy Policy" links are accessible

### Data Usage Restrictions

**Prohibited**:
- Extracting Places data for external use
- Caching map tiles beyond session
- Reverse engineering map data
- Using Maps SDK with non-Google routing in same UI

**Allowed**:
- Displaying routes from Directions API
- Showing Places results
- Custom markers and overlays
- Traffic layer display

---

## 10. Performance Optimization

### Best Practices

1. **Lazy Load Map**: Initialize MapView only when Plus tier navigation is triggered
2. **Bitmap Pooling**: Reuse marker icons instead of creating new bitmaps
3. **Polyline Simplification**: For long routes, simplify geometry for better rendering performance
4. **Throttle Location Updates**: 1-second intervals sufficient for navigation
5. **Cache API Responses**: Store recent route calculations for quick retry

### Memory Management

```kotlin
override fun onLowMemory() {
    super.onLowMemory()
    mapView?.onLowMemory()
    // Clear alternative route polylines
    routeRenderer.clearAlternativeRoutes()
}

override fun onDestroy() {
    super.onDestroy()
    mapView?.onDestroy()
    placesClient.clear()
}
```

---

## 11. Error Handling

### Common Scenarios

```kotlin
sealed class MapsError {
    object NoInternetConnection : MapsError()
    object ApiKeyInvalid : MapsError()
    object QuotaExceeded : MapsError()
    object LocationPermissionDenied : MapsError()
    data class RouteNotFound(val reason: String) : MapsError()
    data class ApiError(val statusCode: Int, val message: String) : MapsError()
}

class MapsErrorHandler {
    fun handleError(error: MapsError): String {
        return when (error) {
            is MapsError.NoInternetConnection -> 
                "No internet connection. Using cached routes."
            is MapsError.ApiKeyInvalid -> 
                "Map service unavailable. Please contact support."
            is MapsError.QuotaExceeded -> 
                "Service temporarily unavailable. Try again later."
            is MapsError.LocationPermissionDenied -> 
                "Location permission required for navigation."
            is MapsError.RouteNotFound -> 
                "No route found: ${error.reason}"
            is MapsError.ApiError -> 
                "Service error: ${error.message}"
        }
    }
}
```

---

## 12. Testing Strategy

### Unit Tests
- Route request URL builder
- Polyline decoder
- Distance/bearing calculations
- Error handling logic

### Integration Tests
- Maps SDK initialization
- Directions API calls (using test keys)
- Places API search
- Marker placement and removal

### UI Tests
- Map rendering
- Route display
- Navigation flow
- Alternative route selection

---

## Summary

This document defines the complete Google Maps SDK integration for GemNav Plus tier. Key points:

- In-app navigation with full Google Maps SDK
- Multi-waypoint routing (up to 10 stops)
- Real-time traffic integration
- Places API for search and autocomplete
- Turn-by-turn navigation with 3D map
- Alternative routes with visual comparison
- Comprehensive error handling
- Performance-optimized rendering

All implementations must respect Google Maps Platform Terms of Service and maintain clear separation from HERE SDK (Pro tier).