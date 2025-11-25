# GEMNAV HANDOFF DOCUMENT
## Session: MP-014 HERE Map Rendering Complete

---

## COMPLETED: MP-014 HERE Map Rendering + Secure Key Pipeline

### Branch
`mp-014-here-map-rendering`

### Files Created
| File | Lines | Purpose |
|------|-------|---------|
| ui/map/HereMapContainer.kt | 250 | Pro-tier map composable |
| android/.gitignore | 21 | Secrets/build exclusions |
| local.properties.template | 15 | Key configuration template |

### Files Modified
| File | Changes | Purpose |
|------|---------|---------|
| build.gradle.kts | +15 | buildConfig, key injection |
| HereEngineManager.kt | +25 | BuildConfig keys, hasValidKeys() |
| RouteDetailsScreen.kt | +18 | HereMapContainer integration |

### Secure Key Pipeline
```
1. Create local.properties (from template):
   here_api_key=YOUR_KEY
   here_map_key=YOUR_KEY

2. build.gradle.kts reads local.properties:
   buildConfigField("String", "HERE_API_KEY", "...")

3. Code accesses via BuildConfig:
   BuildConfig.HERE_API_KEY
```

### HereMapContainer Architecture
```
HereMapContainer(routeData, centerLocation)
  ↓
Check: SafeMode → Error state
Check: !ProTier → Error state  
Check: !hasValidKeys → Error state
  ↓
HereEngineManager.initialize()
  ↓
MapState.Ready → HereMapViewStub (pipeline testing)
```

### States
- MapState.Initializing → Loading spinner
- MapState.Ready → Map stub with route overlay
- MapState.Error → Warning icon + message

### RouteDetailsScreen Integration
When TruckRouteState.Success:
- Shows HereMapContainer (200dp height)
- Shows route stats (distance, duration, warnings)
- Shows action buttons (Clear, Start Navigation)

---

## TODO: Actual HERE SDK Integration
```kotlin
// Replace HereMapViewStub with:
// AndroidView(factory = { MapView(it) })
// Configure MapStyle, add Polyline from routeData
```

---

## BUILD STATUS
✅ Gradle dry-run successful

---
**Last Updated**: 2025-11-24
**Branch**: mp-014-here-map-rendering


---

# MP-015: GOOGLE MAPS SDK INTEGRATION (PLUS TIER)

## COMPLETED
Google Maps SDK integrated for Plus tier with secure key pipeline.

## FILES CHANGED

### Created
- `android/app/src/main/java/com/gemnav/app/ui/map/GoogleMapContainer.kt`

### Modified  
- `android/app/build.gradle.kts` - Google Maps key + maps-compose dependency
- `android/local.properties.template` - Uncommented google_maps_api_key
- `android/app/src/main/java/com/gemnav/app/ui/route/RouteDetailsScreen.kt` - PlusTierMapSection
- `android/app/src/main/java/com/gemnav/app/ui/route/RouteDetailsViewModel.kt` - isPlusTier() + callbacks

## KEY IMPLEMENTATIONS

### GoogleMapContainer.kt
```kotlin
// Tier-gated Google Maps composable
@Composable fun GoogleMapContainer(
    modifier: Modifier,
    originLocation: LatLng?,
    destinationLocation: LatLng?,
    centerLocation: LatLng?,
    onMapReady: () -> Unit,
    onMapError: (String) -> Unit
)
```

### Tier Flow in RouteDetailsScreen
- Pro: TruckRouteSection → HereMapContainer
- Plus (not Pro): PlusTierMapSection → GoogleMapContainer  
- Free: No in-app maps

### Secure Key Pipeline
```
local.properties → build.gradle.kts → BuildConfig.GOOGLE_MAPS_API_KEY
```

## TODO for Next MP
- MP-016: Gemini routing integration (route polylines from AI)
- MP-017: Turn-by-turn navigation flow

---

## BUILD STATUS
✅ Gradle dry-run successful

---
**Last Updated**: 2025-11-24
**Branch**: mp-015-google-maps-integration


---

# MP-016: GEMINI ROUTING INTEGRATION (AI → ROUTE PIPELINE)

## COMPLETED
Full AI routing pipeline from Search/Voice → GeminiShim → RouteDetailsViewModel.

## FILES CHANGED

### Created
- `android/app/src/main/java/com/gemnav/data/ai/AiRouteModels.kt`

### Modified  
- `android/app/build.gradle.kts` - Gemini API key injection
- `android/local.properties.template` - Uncommented gemini_api_key
- `android/app/src/main/java/com/gemnav/core/shim/GeminiShim.kt`
- `android/app/src/main/java/com/gemnav/app/ui/search/SearchViewModel.kt`
- `android/app/src/main/java/com/gemnav/app/ui/search/SearchScreen.kt`
- `android/app/src/main/java/com/gemnav/app/ui/voice/VoiceViewModel.kt`
- `android/app/src/main/java/com/gemnav/app/ui/voice/VoiceScreen.kt`
- `android/app/src/main/java/com/gemnav/app/ui/route/RouteDetailsViewModel.kt`

## KEY IMPLEMENTATIONS

### AiRouteModels.kt
```kotlin
data class AiRouteRequest(rawQuery, currentLocation, tier, isTruck, maxStops)
data class AiRouteSuggestion(origin, destination, waypoints, mode, destinationName)
sealed class AiRouteResult { Success, Failure }
sealed class AiRouteState { Idle, Loading, Success, Error }
sealed class VoiceAiRouteState { Idle, Listening, AiRouting, Success, Error }
```

### GeminiShim.kt - New Functions
```kotlin
suspend fun getRouteSuggestion(request: AiRouteRequest): AiRouteResult
fun isNavigationQuery(query: String): Boolean
// Stub parsing with navigation keyword detection
```

### Pipeline Flow
1. Search/Voice → builds AiRouteRequest
2. GeminiShim.getRouteSuggestion() → validates tier + SafeMode + API key
3. Returns AiRouteSuggestion with mode (CAR/TRUCK)
4. RouteDetailsViewModel.applyAiRouteSuggestion() routes to engine:
   - CAR → calculateRoute() (Google Maps)
   - TRUCK → requestTruckRoute() (HERE SDK)

## TODO for Real Implementation
- Replace stub parsing with actual Gemini HTTP client
- Integrate location provider for currentLocation
- Add geocoding for destination coordinates
- Design proper Gemini prompts for routing

---

## BUILD STATUS
✅ Gradle dry-run successful

---
**Last Updated**: 2025-11-24
**Branch**: mp-016-gemini-routing-integration


---

# MP-018: LOCATION PROVIDER INTEGRATION - HANDOFF

## What Was Done
Implemented complete GPS location pipeline using FusedLocationProviderClient with ViewModel state management, SafeMode enforcement, and FeatureGate tier gating.

## Files Created
1. `android/app/src/main/java/com/gemnav/core/location/LocationService.kt` (199 lines)
   - FusedLocationProviderClient wrapper
   - startLocationUpdates() / stopLocationUpdates() / getLastKnownLocation()
   - SafeMode + FeatureGate enforcement
   - LocationListener interface for callbacks

2. `android/app/src/main/java/com/gemnav/core/location/LocationViewModel.kt` (176 lines)
   - StateFlows: currentLocation, lastKnownLocation, locationStatus, hasPermission
   - LocationStatus sealed class: Idle/Active/Searching/Error/PermissionDenied
   - Lifecycle-aware tracking management

## Files Modified
- `AndroidManifest.xml`: GPS feature declaration, background location placeholder
- `HomeScreen.kt`: GpsStatusChip composable, LocationViewModel integration
- `RouteDetailsScreen.kt`: CurrentLocationIndicator, location tracking lifecycle
- `RouteDetailsViewModel.kt`: currentUserLocation StateFlow, navigation stubs for MP-017
- `SettingsScreen.kt`: LocationPermissionSection with permission status

## Tier Behavior
- **Free**: Location blocked (FeatureGate.areInAppMapsEnabled() = false)
- **Plus**: Location enabled for in-app maps
- **Pro**: Location enabled for maps + truck navigation

## What to Do Next
**MP-017: Turn-by-turn Navigation Engine**
- Implement TTS voice guidance
- Route progress tracking (distance to next step)
- Lane guidance UI
- Off-route detection + recalculation
- Step announcements

## Build Status
✅ Gradle dry-run successful (3s)

---
**Last Updated**: 2025-11-24
**Branch**: mp-018-location-provider
**Commit**: 5df059a


---

# MP-017: TURN-BY-TURN NAVIGATION ENGINE - HANDOFF

## What Was Done
Implemented complete turn-by-turn navigation core including NavigationEngine with step tracking, off-route detection, state machine, and UI overlays.

## Files Created
1. `android/app/src/main/java/com/gemnav/data/navigation/NavigationState.kt` (126 lines)
   - NavigationState sealed class: Idle, LoadingRoute, Navigating, OffRoute, Recalculating, Finished, Blocked
   - NavStep data class: instruction, maneuverIcon, distanceMeters, streetName, location
   - NavRoute data class: steps, polylineCoordinates, totalDistance, totalDuration
   - NavManeuver enum: STRAIGHT, LEFT, RIGHT, SLIGHT_*, SHARP_*, UTURN, MERGE, EXIT, etc.
   - NavigationEvent sealed class for one-shot events

2. `android/app/src/main/java/com/gemnav/core/navigation/NavigationEngine.kt` (422 lines)
   - startNavigation(NavRoute) / stopNavigation() / updateLocation(LatLng)
   - Step completion radius: 30m
   - Off-route threshold: 50m (severe: 150m)
   - Haversine distance + bearing calculations
   - Point-to-line-segment distance for route deviation
   - StateFlow<NavigationState> + SharedFlow<NavigationEvent>

3. `android/app/src/main/java/com/gemnav/core/navigation/NavigationTts.kt` (105 lines)
   - Stub: speak(), speakImmediate(), queue(), mute(), unmute()
   - Ready for Android TextToSpeech integration

4. `android/app/src/main/java/com/gemnav/app/ui/route/NavigationComponents.kt` (499 lines)
   - NavigationOverlay: ManeuverCard + progress + ETA + next step preview
   - OffRouteOverlay: deviation display + recalculate button
   - RecalculatingOverlay: loading indicator
   - NavigationFinishedOverlay: arrival stats
   - BlockedOverlay: SafeMode/Free tier messaging
   - Helper functions: getManeuverIcon(), formatDistance(), formatDuration()

## Files Modified
- `HereShim.kt`: Added parseSteps(TruckRouteData), createNavRoute(), maneuver mapping
- `MapsShim.kt`: Added parseSteps() stub (TODO MP-019), createNavRoute() stub
- `RouteDetailsScreen.kt`: Navigation state handling, overlay dispatch by state type
- `RouteDetailsViewModel.kt`: NavigationEngine instance, startNavigation/stopNavigation, onOffRoute handler
- `GoogleMapContainer.kt`: isNavigating, currentLocation, nextStep params + camera follow
- `HereMapContainer.kt`: Same navigation mode support + NavigationInfoStub

## Navigation Flow
```
User taps "Navigate" on TruckRouteResultCard
    → RouteDetailsViewModel.startNavigation()
    → Creates NavRoute from HereShim.createNavRoute(TruckRouteData)
    → NavigationEngine.startNavigation(navRoute)
    → navigationState = Navigating(...)
    
Location updates flow:
    LocationViewModel.currentLocation
    → RouteDetailsScreen LaunchedEffect
    → viewModel.updateUserLocation()
    → NavigationEngine.updateLocation()
    → State updates → UI overlays react
```

## What to Do Next
**MP-019: Google Directions API Integration**
- Implement actual Google Directions API calls for Plus tier
- Parse steps from Google response format
- Create NavRoute from Google route data
- Enable Plus tier turn-by-turn navigation

## Build Status
✅ Gradle dry-run successful (5s)

---
**Last Updated**: 2025-11-24
**Branch**: mp-017-turn-by-turn
**Commit**: b013087


---

## HANDOFF — MP-019: Google Directions API (Plus Tier)

**Session**: November 24, 2025
**Branch**: mp-019-google-directions  
**Commit**: 96051fc (6 files, +1044/-49)

### What Was Done
Implemented complete Google Directions API routing pipeline enabling Plus tier users to receive full turn-by-turn navigation with maneuver instructions, polyline rendering, and NavigationEngine integration.

### Files Created
| File | Lines | Purpose |
|------|-------|---------|
| `core/maps/google/DirectionsModels.kt` | 147 | API response data classes |
| `core/maps/google/PolylineDecoder.kt` | 175 | Encoded polyline decoder |
| `core/maps/google/DirectionsApiClient.kt` | 345 | HTTP client with JSON parsing |

### Files Modified
| File | Changes |
|------|---------|
| `core/shim/MapsShim.kt` | +parseGoogleSteps(), +createNavRoute(DirectionsResult.Success), +mapGoogleManeuver(), +extractStreetName() |
| `app/ui/route/RouteDetailsViewModel.kt` | +GoogleRouteState sealed class, +googleRouteState flow, +requestGoogleRoute(), updated calculateCarRoute() |
| `app/ui/route/RouteDetailsScreen.kt` | +googleRouteState/googlePolyline collectors, redesigned PlusTierMapSection |

### API Flow
1. User taps "Get Route" → requestGoogleRoute()
2. DirectionsApiClient.getRoute() → HTTP GET to Google
3. JSON parsed into DirectionsResponse
4. PolylineDecoder.decode() → List<LatLng>
5. MapsShim.createNavRoute() → NavRoute with steps
6. GoogleMapContainer renders polyline
7. User taps "Start Navigation" → NavigationEngine starts

### Key Implementation Details
- Uses existing GOOGLE_MAPS_API_KEY from local.properties
- HTML stripped from instructions via DirectionsApiClient.stripHtml()
- 14 Google maneuver types mapped to NavManeuver enum
- Street names extracted from instruction text via regex
- SafeMode and tier gating enforced before API calls

### What To Do Next
**MP-020: AI Intent Improvements + Multi-Step Reasoning**
- Enhance GeminiShim for complex navigation queries
- Add multi-waypoint handling from natural language
- Improve context retention between AI turns
- Better error recovery when AI interpretation fails

### Dependencies Satisfied
- NavigationEngine fully works with Google routes (MP-017 integration)
- GoogleMapContainer already supported polyline param
- Tier gating infrastructure from FeatureGate

### Testing Notes
- Requires valid GOOGLE_MAPS_API_KEY in local.properties
- Plus tier subscription needed for route requests
- SafeMode must be disabled
- Polyline renders immediately after route success
- Navigation button enabled only after successful route


---

## MP-020: Advanced AI Intent System — HANDOFF

### Summary
Implemented complete multi-step AI intent classification and reasoning pipeline. Users can now speak/type complex natural language queries like "Find a truck stop with showers near Flagstaff" and GemNav will classify the intent, resolve it into an actionable route request, and trigger the appropriate routing engine.

### Files Created
| File | Lines | Purpose |
|------|-------|---------|
| `data/ai/IntentModel.kt` | 185 | NavigationIntent sealed class hierarchy, POI types/filters |

### Files Modified
| File | Changes |
|------|---------|
| `core/shim/GeminiShim.kt` | +classifyIntent(), +resolveIntent(), +heuristic intent parsing |
| `data/ai/AiRouteModels.kt` | +AiIntentState sealed class for UI state tracking |
| `app/ui/search/SearchViewModel.kt` | Replaced direct AI calls with full intent pipeline |
| `app/ui/search/SearchScreen.kt` | +AiIntentStatusPanel showing classification progress |
| `app/ui/voice/VoiceViewModel.kt` | +aiIntentState, +classifiedIntent, +processNavigationWithAI() |
| `app/ui/voice/VoiceScreen.kt` | +VoiceAiIntentStatusPanel for intent display |
| `app/ui/route/RouteDetailsViewModel.kt` | +handleResolvedIntent() for pipeline integration |

### AI Intent Flow
```
User Input → classifyIntent() → NavigationIntent
                    ↓
            resolveIntent() → IntentResolutionResult
                    ↓
            RouteRequest → getRouteSuggestion() → AiRouteSuggestion
                    ↓
            handleResolvedIntent() → HereShim (truck) / Google (car)
```

### Key Implementation Details
- Heuristic intent classification (stub for actual Gemini API)
- POI filters: showers, overnight parking, truck parking, hazmat, diesel, 24hr
- Route settings: avoid tolls/highways/mountains/snow, prefer fastest/shortest/scenic
- AiIntentState tracks: Idle → Classifying → Reasoning → Suggesting → Success/Error
- Full tier gating: Free blocked, Plus car-only, Pro truck+car

### What To Do Next
**MP-021: Places API Integration**
- Replace stub POI search with real Google Places API
- Geocode destinations from natural language
- Return actual LatLng coordinates for POI results
- Price/rating filters for POI search

### Testing Notes
- Voice: Speak "Find nearest truck stop with showers" - should classify as FindPOI
- Search: Type "Avoid tolls" - should classify as RoutePreferences
- Pro tier required for truck mode intent resolution
- UI shows intent classification status during processing

### Dependencies
- GeminiShim.isNavigationQuery() for intent detection
- FeatureGate.areAIFeaturesEnabled() for tier gating
- TierManager.isPro() for truck mode access


---

## MP-021: Google Places API (Plus Tier Only) — HANDOFF

### Summary
Implemented Plus-only Google Places REST API integration. Users can now ask "Find nearest truck stop with showers" and GemNav will use Google Places to find matching POIs, then route via Google Directions. Strict tier enforcement prevents Free and Pro tiers from using Google Places.

### Files Created
| File | Lines | Purpose |
|------|-------|---------|
| `core/places/PlacesApiClient.kt` | 327 | REST-based Google Places API client |
| `core/places/PoiTypeMapper.kt` | 105 | POIType → Google Places type/keyword mapping |

### Files Modified
| File | Changes |
|------|---------|
| `build.gradle.kts` | +GOOGLE_PLACES_API_KEY from local.properties |
| `local.properties.template` | +google_places_api_key entry |
| `core/shim/GeminiShim.kt` | Replaced stub resolveFindPOI() with PlacesApiClient integration |

### Tier Rules (CRITICAL)
```kotlin
// In PlacesApiClient.checkTierAccess() and GeminiShim.resolveFindPOI()
FREE → "POI search requires Plus subscription"
PLUS → PlacesApiClient.searchNearby() → full POI search
PRO  → "Truck-specific POI coming soon (HERE-based)"
```

### API Endpoints Used
- `maps.googleapis.com/maps/api/place/nearbysearch/json` - location-based POI
- `maps.googleapis.com/maps/api/place/textsearch/json` - text query POI

### POI Type Mapping
```kotlin
TRUCK_STOP → type="gas_station" + keyword="truck stop"
DIESEL     → type="gas_station" + keyword="diesel fuel"
HOTEL      → type="lodging"
WALMART    → keyword="Walmart"
RESTAURANT → type="restaurant"
// ... see PoiTypeMapper.kt for full list
```

### Attribute Inference
PlacesApiClient infers truck-friendly attributes from place names:
- Contains "Pilot", "Flying J", "Loves", "TA ", "Petro" → truck stop features
- hasShowers, truckParking, overnightAllowed, dieselAvailable, hazmatFriendly

### What To Do Next
**MP-022: Along-Route POI + HERE Truck POI**
- Add along-route POI filtering (search POIs within corridor of current route)
- Implement HERE-based truck POI for Pro tier
- Add TTS feedback for voice POI results

### Testing Notes
- Requires valid google_places_api_key in local.properties
- Plus tier subscription needed for POI search
- Test queries: "Find truck stop with showers", "Find diesel nearby", "Find Walmart"
- Free tier should see upgrade prompt
- Pro tier should see "truck POI coming soon" message

### Dependencies
- PlacesApiClient requires GOOGLE_PLACES_API_KEY BuildConfig field
- GeminiShim.resolveFindPOI() calls PlacesApiClient for Plus tier only
- PoiTypeMapper provides type/keyword mapping for all 18 POI types


---

## MP-022: Along-Route POI Search (PLUS TIER ONLY)
**Completed**: Session continues
**Commit**: 40cadd8
**Branch**: mp-022-along-route-poi

### Summary
Implemented along-route POI search that filters Places API results to only show POIs within 2km of the active route polyline. PLUS tier only - FREE and PRO correctly blocked.

### Files Created
| File | Purpose |
|------|---------|
| `core/navigation/RouteCorridor.kt` | Corridor filtering with Haversine distance calculations |
| `core/shim/RouteDetailsViewModelProvider.kt` | Service locator for polyline access from shim layer |

### Files Modified
| File | Changes |
|------|---------|
| `core/shim/GeminiShim.kt` | +RouteCorridor import, enhanced resolveFindPOI() with along-route detection |
| `app/ui/route/RouteDetailsViewModel.kt` | +getActiveRoutePolyline(), +init/onCleared for provider registration, +TierManager import |
| `STATUS.md` | +MP-022 completion status |

### Tier Enforcement
```kotlin
FREE → "POI search requires Plus subscription" // No Places, no polylines
PLUS → Full along-route POI search enabled    // This MP
PRO  → "Truck-specific POI coming soon"       // No Google Places allowed
```

### Along-Route Flow (PLUS)
1. User: "Find gas station along my route"
2. classifyIntent() → FindPOI with nearLocation containing keyword
3. RouteCorridor.containsAlongRouteKeywords() → true
4. PlacesApiClient.searchNearby(radius=50km) → wider search
5. RouteDetailsViewModelProvider.getActiveRoutePolyline() → decoded Google polyline
6. RouteCorridor.filterPlacesAlongRoute(places, polyline, tolerance=2km)
7. Return POIs sorted by route progress (segment index)

### Detection Keywords
```kotlin
"along my route", "along the route", "on my way", "on the way",
"next ", "upcoming", "ahead", "coming up", "before i arrive"
```

### What To Do Next
**MP-023 Options:**
1. Detour time estimation - Calculate time added by POI stops
2. HERE truck POI for Pro tier - Implement truck-legal POI search
3. Voice feedback - Add TTS for POI search results

### Architecture Notes
- RouteDetailsViewModelProvider uses service locator pattern to avoid tight coupling
- Polyline only accessible for PLUS tier (Pro returns emptyList())
- Haversine-based point-to-segment distance for accuracy
- Fallback to all results if no active route or no POIs in corridor


---

## MP-023: Detour Cost + Add-Stop Flow (PLUS TIER ONLY)
**Completed**: Session continues
**Commit**: 8b1b66a
**Branch**: mp-023-detour-and-stop-flow

### Summary
Implemented detour cost calculation and add-stop flow for along-route POIs. When Plus tier user finds a POI along their route, the system calculates the extra time/distance and presents a UI panel to add the POI as a stop.

### Files Created
| File | Purpose |
|------|---------|
| `core/navigation/AiDetourModels.kt` | DetourInfo, DetourState sealed class, SelectedPoi data models |

### Files Modified
| File | Changes |
|------|---------|
| `core/maps/google/DirectionsApiClient.kt` | +getRouteWithWaypoint(), +getRouteWithMultipleWaypoints() |
| `core/shim/RouteDetailsViewModelProvider.kt` | +registerPoiSelectionHandler(), +selectPoiForDetour() |
| `core/shim/GeminiShim.kt` | Triggers detour calculation when along-route POI found |
| `app/ui/route/RouteDetailsViewModel.kt` | +detourState, +onPoiSelected(), +calculateDetourInfoForPoi(), +onAddStopConfirmed(), +onDetourDismissed(), +calculateRouteWithWaypoints() |
| `app/ui/route/RouteDetailsScreen.kt` | +DetourPanel composable with Calculating/Ready/Error/Blocked states |

### Tier Enforcement
```kotlin
FREE → DetourState.Blocked("Detour calculation requires Plus subscription")
PLUS → Full detour calculation + add-stop flow
PRO  → DetourState.Blocked("Truck-specific POI coming soon")
```

### Flow (PLUS Tier)
1. User: "Find gas station along my route"
2. GeminiShim.resolveFindPOI() → filters POIs to route corridor
3. RouteDetailsViewModelProvider.selectPoiForDetour(poi)
4. RouteDetailsViewModel.onPoiSelected(poi) → _detourState = Calculating
5. calculateDetourInfoForPoi() → DirectionsApiClient.getRouteWithWaypoint()
6. _detourState = Ready(poi, DetourInfo(+X min, +Y mi))
7. UI shows DetourPanel with POI info + "Add Stop & Navigate" button
8. User taps button → onAddStopConfirmed() → adds waypoint, recalculates route

### DetourInfo Model
```kotlin
data class DetourInfo(
    val extraDistanceMeters: Int,
    val extraDurationSeconds: Int,
    val baseDistanceMeters: Int,
    val baseDurationSeconds: Int
) {
    fun formatDetour(): String  // "+5 min, +2.3 mi"
}
```

### What To Do Next
**MP-024 Options:**
1. Voice TTS feedback for POI results ("Found Pilot Travel Center, 5 minutes ahead")
2. HERE truck POI for Pro tier
3. Multiple POI results display (show top 3 along route)

### Architecture Notes
- SelectedPoi.fromPlaceResult() converts PlacesApiClient results to SelectedPoi
- RouteDetailsViewModelProvider uses service locator pattern for layer separation
- calculateRouteWithWaypoints() supports multiple waypoints for future expansion
- SafeMode checks at entry points (onPoiSelected, calculateDetourInfoForPoi, onAddStopConfirmed)


---

## MP-024: Voice Feedback for POI & Detours
**Completed**: Session continues
**Commit**: 78246b5
**Branch**: mp-024-voice-poi-feedback

### Summary
Added spoken voice feedback for along-route POIs, detour calculations, upgrade blocks, and stop confirmations using Android TextToSpeech. No external APIs required.

### Files Created
| File | Purpose |
|------|---------|
| `core/navigation/AiVoiceEvent.kt` | Sealed class with 6 event types for spoken feedback |
| `app/voice/VoiceFeedbackManager.kt` | Android TTS wrapper with event-to-speech formatting |

### Files Modified
| File | Changes |
|------|---------|
| `core/shim/RouteDetailsViewModelProvider.kt` | +voiceEventHandler property, +emitVoiceEvent() function |
| `app/ui/route/RouteDetailsViewModel.kt` | Emits AiVoiceEvent at key points (detour ready, stop added, tier blocks, errors) |
| `app/ui/route/RouteDetailsScreen.kt` | +VoiceFeedbackManager lifecycle, +voiceEventHandler wiring |

### Voice Event Types
```kotlin
sealed class AiVoiceEvent {
    data class DetourSummary(poiName, addedMinutes, addedMiles, distanceOffRouteMiles)
    data class UpgradeRequired(requiredTierName, featureName)
    data class StopAdded(poiName)
    data class PoiFound(poiName, poiType, distanceAheadMiles, totalResults)
    data class NoPoisFound(poiType)
    object GenericError
}
```

### Voice Output Examples
- "Detour found to Pilot Travel Center. About 5 minutes extra and 2.3 miles added to your trip."
- "This feature requires a Plus subscription to use detour calculation."
- "Added Pilot Travel Center as a stop on your route."
- "Found 3 gas stations along your route. The closest is Shell, 2 miles ahead."
- "Sorry, I couldn't calculate a detour right now."

### Architecture
```
RouteDetailsViewModel
    ↓ emits AiVoiceEvent
RouteDetailsViewModelProvider.emitVoiceEvent()
    ↓ invokes handler
RouteDetailsScreen.voiceEventHandler
    ↓ delegates to
VoiceFeedbackManager.handleEvent()
    ↓ speaks via
Android TextToSpeech
```

### What To Do Next
**MP-025 Options:**
1. HERE truck POI for Pro tier - Complete truck-legal POI search
2. Multiple POI results display - Show top 3 POIs along route with swipe selection
3. Voice command integration - Use VoiceFeedbackManager from VoiceViewModel

### Testing Notes
- VoiceFeedbackManager auto-initializes TTS on construction
- `enabled` property allows toggle without destroying TTS
- Graceful degradation if TTS unavailable (logs warning, no crash)
- QUEUE_FLUSH means new speech interrupts old speech


---

## MP-025: HERE Truck POIs for PRO Tier ✅ COMPLETE

### Date: 2025-11-24
### Branch: mp-025-here-truck-poi
### Commit: ceb085c

### Summary
Integrated HERE truck-specific POIs (truck stops, weigh stations, rest areas, parking) into the along-route POI + detour engine. PRO tier only. Uses HERE Places Discover API with route corridor filtering.

### Files Created
| File | Lines | Purpose |
|------|-------|---------|
| `core/navigation/TruckPoiModels.kt` | 58 | TruckPoi data class, TruckPoiType enum (4 categories), TruckPoiResult, TruckPoiState sealed class |
| `core/shim/HereTruckPoiClient.kt` | 299 | HERE Places Discover API client with bbox/corridor filtering |

### Files Modified
| File | Changes |
|------|---------|
| `core/shim/RouteDetailsViewModelProvider.kt` | +truckPoiSelectionHandler, +herePolylineProvider, +selectTruckPoiForDetour(), +getHereRoutePolyline() |
| `core/shim/HereShim.kt` | +requestTruckRouteWithWaypoint() (116 lines), +createMockRouteWithWaypoint() |
| `app/ui/route/RouteDetailsViewModel.kt` | +truckPoiState StateFlow, +currentHerePolyline, +findTruckPois(), +onTruckPoiSelected(), +calculateDetourForTruckPoi(), +onTruckStopConfirmed(), +onTruckPoiDismissed() (~280 lines) |
| `app/ui/route/RouteDetailsScreen.kt` | +TruckPoiBar composable, +TruckPoiButton, +TruckPoiResultCard (+353 lines) |

### HERE API Integration
```kotlin
// Discover endpoint
https://discover.search.hereapi.com/v1/discover?in=bbox:<west>,<south>,<east>,<north>&categories=<id>&limit=20&apiKey=<key>

// Category IDs
TruckPoiType.TRUCK_STOP     -> "700-7600-0322"
TruckPoiType.WEIGH_STATION  -> "700-7600-0116"
TruckPoiType.REST_AREA      -> "800-8500-0000"
TruckPoiType.PARKING        -> "800-8500-0177"
```

### Data Flow
```
User taps "Find Truck Stops" (PRO tier only)
    ↓
RouteDetailsViewModel.findTruckPois(TruckPoiType.TRUCK_STOP)
    ↓ tier/safety checks
HereTruckPoiClient.fetchTruckPoisAlongRoute(herePolyline, [type])
    ↓ bbox from polyline
HERE Discover API call
    ↓ parse response
RouteCorridor.isPointAlongRoute() filtering (3km corridor)
    ↓
TruckPoiState.Found(result)
    ↓ auto-select first POI
onTruckPoiSelected(poi)
    ↓ convert to SelectedPoi
calculateDetourForTruckPoi(poi)
    ↓ HERE truck routing
HereShim.requestTruckRouteWithWaypoint()
    ↓
DetourState.Ready(poi, detourInfo)
    ↓ voice feedback
AiVoiceEvent.DetourSummary
```

### Tier Enforcement
| Tier | Behavior |
|------|----------|
| FREE | TruckPoiState.Blocked + AiVoiceEvent.UpgradeRequired("Plus") |
| PLUS | TruckPoiState.Blocked + AiVoiceEvent.UpgradeRequired("Pro") |
| PRO | Full truck POI search, detour calculation, stop addition |
| SafeMode | All blocked immediately |

### UI Components (PRO only)
```
TruckPoiBar
├── Quick buttons: Truck Stops | Weigh | Rest | Parking
├── TruckPoiState.Searching → Progress indicator
├── TruckPoiState.Found → TruckPoiResultCard
│   ├── POI name, address, category
│   ├── DetourState.Calculating → spinner
│   ├── DetourState.Ready → detour cost + "Add Stop & Navigate" button
│   └── DetourState.Error → error message
├── TruckPoiState.NotFound → "No X found along route"
├── TruckPoiState.Error → error message
└── TruckPoiState.Blocked → tier upgrade message
```

### Voice Integration
- PoiFound: "Found 3 truck stops along your route. The closest is Pilot Travel Center, 5 miles ahead."
- NoPoisFound: "No truck parking found along your route."
- DetourSummary: "Detour found to Love's Travel Stop. About 8 minutes extra and 3.5 miles added to your trip."
- StopAdded: "Added Love's Travel Stop as a stop on your route."
- UpgradeRequired: "This feature requires a Pro subscription to use truck-specific POI search."

### What To Do Next
**MP-026 Options:**
1. Multiple truck POI results display - Show top 3 POIs with swipe/list selection
2. Real HERE SDK integration - Replace mock routing with actual HERE SDK calls
3. Truck profile settings UI - Let user configure height/weight/hazmat for TruckConfig
4. iOS truck POI implementation - Port HERE truck POI functionality to iOS

### Testing Notes
- HereTruckPoiClient uses mock HTTP client (HttpURLConnection) - replace with OkHttp/Ktor in production
- HereShim.requestTruckRouteWithWaypoint() returns mock data - implement real HERE SDK routing
- TruckPoiBar only visible when isProTier is true
- Corridor filtering uses same RouteCorridor as Google POI filtering (3km tolerance)

### Build Status
✅ Gradle dry-run successful (3s)
✅ Pushed to origin/main
