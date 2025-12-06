# MP-G2 Testing Guide

## Prerequisites

### 1. Verify Google Maps API Key
Check that your API key is configured in `android/local.properties`:

```properties
google_maps_api_key=YOUR_GOOGLE_MAPS_API_KEY_HERE
```

**Important**: Ensure this key has the **Directions API** enabled in Google Cloud Console:
1. Go to https://console.cloud.google.com/
2. Select your project
3. Navigate to APIs & Services > Library
4. Search for "Directions API"
5. Click "Enable" if not already enabled

### 2. Build Configuration
The API key is injected via `android/app/build.gradle.kts`:
```kotlin
buildConfigField("String", "GOOGLE_MAPS_API_KEY", "\"${googleMapsApiKey}\"")
```

## Test Method 1: Automatic Validation (Recommended)

This test runs automatically when the app starts.

### Steps:
1. **Build the app** (from `C:\Users\perso\GemNav\android`):
   ```cmd
   gradlew :app:assembleDebug
   ```

2. **Install on device/emulator**:
   ```cmd
   gradlew :app:installDebug
   ```
   Or use Android Studio: Run > Run 'app'

3. **Monitor logcat** for validation results:
   ```cmd
   adb logcat -s OrchestratorValidation GoogleRoutingEngine GoogleDirectionsClient GoogleRouteParser RoutingOrchestrator
   ```

### Expected Output (Success):
```
OrchestratorValidation: === Starting Orchestrator Validation ===
OrchestratorValidation: Test 1: Routing with PRO tier (expect HERE)
RoutingOrchestrator: Routing request: tier=PRO → engine=HERE
OrchestratorValidation: ✓ PRO tier result: Failure from engine 'HERE'
OrchestratorValidation:   ✓ PASS: HERE engine was invoked as expected

OrchestratorValidation: Test 2: Routing with FREE tier (expect GOOGLE)
RoutingOrchestrator: Routing request: tier=FREE → engine=GOOGLE
GoogleRoutingEngine: calculateRoute called for origin=LatLng(34.0522, -118.2437), dest=LatLng(36.1699, -115.1398)
GoogleDirectionsClient: Requesting directions: origin=34.0522,-118.2437, dest=36.1699,-115.1398, mode=driving
GoogleDirectionsClient: Response status: 200
GoogleRouteParser: Parse success: 1 legs, 435200m, 14520s, 312 polyline points
GoogleRoutingEngine: SUCCESS - route with 1 legs, 435200m, 14520s
OrchestratorValidation:   ✓ PASS: GOOGLE engine was invoked as expected

OrchestratorValidation: === Orchestrator Validation Complete ===
```

### Expected Output (API Key Issue):
```
GoogleDirectionsClient: Response status: 403
GoogleDirectionsClient: Google Directions API failed with status 403: REQUEST_DENIED
GoogleRoutingEngine: FAILURE - Google Directions request failed: ...
```
**Fix**: Check API key is valid and Directions API is enabled.

## Test Method 2: Manual Route Request

If you have UI wired up for routing:

### Steps:
1. Launch the app
2. Enter origin: Los Angeles (34.0522, -118.2437)
3. Enter destination: Las Vegas (36.1699, -115.1398)
4. Trigger route calculation
5. Watch logcat for detailed flow

### Expected Logcat Flow (FREE/BASIC tier):
```
RoutingOrchestrator: Routing request: tier=FREE → engine=GOOGLE
GoogleRoutingEngine: calculateRoute called for origin=LatLng(...), dest=LatLng(...)
GoogleDirectionsClient: Requesting directions: origin=..., dest=..., mode=driving
GoogleDirectionsClient: Response status: 200
GoogleRouteParser: Parse success: 1 legs, XXXXXXm, XXXXs, XXX polyline points
GoogleRoutingEngine: SUCCESS - route with 1 legs, XXXXXXm, XXXXs
```

### Expected Logcat Flow (PRO tier):
```
RoutingOrchestrator: Routing request: tier=PRO → engine=HERE
[HERE engine returns stub Failure until HERE SDK is integrated]
```

## Test Method 3: Unit Test (Create New)

Create a simple unit test to verify integration:

**File**: `android/app/src/test/java/com/gemnav/routing/google/GoogleDirectionsIntegrationTest.kt`

```kotlin
package com.gemnav.routing.google

import com.gemnav.routing.domain.LatLng
import com.gemnav.routing.domain.RouteRequest
import com.gemnav.routing.domain.RouteResult
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

class GoogleDirectionsIntegrationTest {

    @Test
    fun testGoogleRouteCalculation() = runBlocking {
        val engine = GoogleRoutingEngine()
        
        val request = RouteRequest(
            origin = LatLng(34.0522, -118.2437),  // LA
            destination = LatLng(36.1699, -115.1398)  // Vegas
        )
        
        val result = engine.calculateRoute(request)
        
        when (result) {
            is RouteResult.Success -> {
                println("✓ Route calculated successfully")
                println("  Distance: ${result.route.distanceMeters}m")
                println("  Duration: ${result.route.durationSeconds}s")
                println("  Legs: ${result.route.legs.size}")
                assertTrue(result.route.distanceMeters > 0)
            }
            is RouteResult.Failure -> {
                println("✗ Route calculation failed: ${result.message}")
                fail("Expected success but got failure: ${result.message}")
            }
        }
    }
}
```

Run: `gradlew :app:testDebugUnitTest`

## Troubleshooting

### Issue: "REQUEST_DENIED" (403)
**Cause**: API key issue or Directions API not enabled
**Fix**:
1. Verify API key in `local.properties`
2. Enable Directions API in Google Cloud Console
3. Check API key restrictions (HTTP referrers, API restrictions)
4. Rebuild: `gradlew clean :app:assembleDebug`

### Issue: "ZERO_RESULTS"
**Cause**: No route exists between origin/destination
**Fix**: Try different coordinates (e.g., LA to Vegas)

### Issue: Network timeout
**Cause**: Network connectivity or slow response
**Fix**: Check internet connection, retry

### Issue: JSON parse error
**Cause**: Unexpected API response format
**Fix**: Check logcat for full error, verify API version compatibility

### Issue: Build fails - "Cannot resolve BuildConfig.GOOGLE_MAPS_API_KEY"
**Cause**: API key not in local.properties or build not synced
**Fix**:
1. Add key to `android/local.properties`
2. Sync Gradle: `gradlew --refresh-dependencies`
3. Rebuild

## Success Criteria

✓ App builds without errors
✓ OrchestratorValidation logs show PASS for both tiers
✓ FREE tier routes through GOOGLE engine
✓ GOOGLE engine returns RouteResult.Success with valid route data
✓ Route contains: legs, distance, duration, polyline points
✓ PRO tier still routes through HERE engine (stub)
✓ No crashes or exceptions in routing flow

## Log Tags Reference

Monitor these tags for complete visibility:
- `OrchestratorValidation` - Test results
- `RoutingOrchestrator` - Tier → engine dispatch
- `GoogleRoutingEngine` - Engine-level flow
- `GoogleDirectionsClient` - HTTP requests/responses
- `GoogleRouteParser` - JSON parsing details

Command:
```cmd
adb logcat -s OrchestratorValidation:D RoutingOrchestrator:D GoogleRoutingEngine:D GoogleDirectionsClient:D GoogleRouteParser:D
```

## Next Steps After Testing

1. **If tests pass**: 
   - Commit to GitHub
   - Update STATUS.md with test results
   - Move to next micro-project

2. **If tests fail**:
   - Check troubleshooting section
   - Review logcat for detailed errors
   - Verify API key configuration
   - Test with simple curl to verify API works:
     ```bash
     curl "https://maps.googleapis.com/maps/api/directions/json?origin=34.0522,-118.2437&destination=36.1699,-115.1398&key=YOUR_KEY"
     ```
