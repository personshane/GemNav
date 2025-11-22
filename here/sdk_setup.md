# HERE SDK Setup & Initialization

**Version**: 1.0  
**Date**: 2025-11-21  
**Tier**: Pro Only  
**Platforms**: Android, iOS

---

## 1. Overview

HERE SDK provides commercial-grade truck routing with legal compliance for height, weight, hazmat, and axle restrictions. GemNav Pro tier uses HERE SDK exclusively for truck navigation while maintaining the option to toggle to Google Maps for personal vehicle routing.

**Critical Rules**:
- HERE SDK data NEVER mixed with Google Maps UI or tiles
- HERE routing results displayed ONLY on HERE map tiles
- Legal compliance checks run on every route calculation
- Offline map support required for commercial drivers

---

## 2. SDK Versions & Dependencies

### 2.1 Android

**HERE SDK Version**: 4.21.0 (Navigate Edition)  
**Minimum Android SDK**: 24 (Android 7.0)  
**Target SDK**: 34 (Android 14)  
**Kotlin Version**: 1.9.20  
**Gradle Plugin**: 8.2.0

**Dependencies (build.gradle)**:
```gradle
dependencies {
    // HERE SDK Navigate Edition
    implementation 'com.here.sdk:sdk-navigate:4.21.0'
    
    // Required for HERE authentication
    implementation 'com.here.sdk:sdk-core:4.21.0'
    
    // Offline maps support
    implementation 'com.here.sdk:sdk-map-data:4.21.0'
    
    // Traffic and real-time data
    implementation 'com.here.sdk:sdk-traffic:4.21.0'
    
    // Search and Places
    implementation 'com.here.sdk:sdk-search:4.21.0'
    
    // Kotlin coroutines for async operations
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    
    // AndroidX dependencies
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'
}
```

**Maven Repository (build.gradle - project level)**:
```gradle
allprojects {
    repositories {
        maven {
            url 'https://repo.heremaps.com/artifactory/open-location-platform-gradle-release'
            credentials {
                username = project.findProperty("HERE_SDK_USERNAME") ?: ""
                password = project.findProperty("HERE_SDK_PASSWORD") ?: ""
            }
        }
    }
}
```

### 2.2 iOS

**HERE SDK Version**: 4.21.0 (Navigate Edition)  
**Minimum iOS Version**: 14.0  
**Target iOS Version**: 17.0  
**Swift Version**: 5.9  
**Xcode Version**: 15.0+

**Dependencies (Package.swift or CocoaPods)**:

**Swift Package Manager (Preferred)**:
```swift
dependencies: [
    .package(
        url: "https://github.com/heremaps/here-sdk-ios-swift-package",
        from: "4.21.0"
    )
]

targets: [
    .target(
        name: "GemNav",
        dependencies: [
            .product(name: "HEREMapsSDK", package: "here-sdk-ios-swift-package")
        ]
    )
]
```

**CocoaPods (Alternative)**:
```ruby
pod 'HEREMaps', '~> 4.21.0'
```

---

## 3. Credentials & API Keys

### 3.1 HERE Platform Account Setup

1. Register at https://platform.here.com/
2. Create new project: "GemNav Pro Production"
3. Generate API Key with permissions:
   - Navigate Edition
   - Offline Maps
   - Traffic & Incidents
   - Search & Geocoding
   - Routing (Truck Profile)

### 3.2 Credential Storage

**Android (local.properties)**:
```properties
HERE_SDK_ACCESS_KEY_ID=your_access_key_id
HERE_SDK_ACCESS_KEY_SECRET=your_secret_key
HERE_SDK_USERNAME=your_maven_username
HERE_SDK_PASSWORD=your_maven_password
```

**Android (BuildConfig generation)**:
```gradle
android {
    buildTypes {
        release {
            buildConfigField "String", "HERE_ACCESS_KEY_ID", 
                "\"${project.findProperty("HERE_SDK_ACCESS_KEY_ID")}\""
            buildConfigField "String", "HERE_ACCESS_KEY_SECRET", 
                "\"${project.findProperty("HERE_SDK_ACCESS_KEY_SECRET")}\""
        }
    }
}
```

**iOS (Info.plist)**:
```xml
<key>HEREAccessKeyId</key>
<string>$(HERE_ACCESS_KEY_ID)</string>
<key>HEREAccessKeySecret</key>
<string>$(HERE_ACCESS_KEY_SECRET)</string>
```

**iOS (Xcode Build Settings)**:
Create user-defined settings:
- HERE_ACCESS_KEY_ID
- HERE_ACCESS_KEY_SECRET

Store actual values in separate config file (not in git).

---

## 4. SDK Initialization

### 4.1 Android Initialization

**Application Class (GemNavApplication.kt)**:
```kotlin
class GemNavApplication : Application() {
    
    companion object {
        private const val TAG = "GemNavApp"
    }
    
    override fun onCreate() {
        super.onCreate()
        initializeHERESDK()
    }
    
    private fun initializeHERESDK() {
        try {
            val accessKeyId = BuildConfig.HERE_ACCESS_KEY_ID
            val accessKeySecret = BuildConfig.HERE_ACCESS_KEY_SECRET
            
            SDKNativeEngine.makeSharedInstance(
                this,
                SDKOptions(accessKeyId, accessKeySecret)
            ) { error ->
                if (error != null) {
                    Log.e(TAG, "HERE SDK initialization failed: ${error.name}")
                    // Fall back to Plus tier functionality
                    handleSDKInitFailure(error)
                } else {
                    Log.i(TAG, "HERE SDK initialized successfully")
                    // Enable Pro tier features
                    enableProTierFeatures()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during HERE SDK initialization", e)
            handleSDKInitFailure(null)
        }
    }
    
    private fun handleSDKInitFailure(error: InstantiationErrorCode?) {
        // Store failure state
        // Disable Pro tier
        // Show user-friendly error message
        // Offer to continue with Plus tier features
    }
    
    private fun enableProTierFeatures() {
        // Set flag indicating Pro tier is available
        // Initialize truck routing engine
        // Load offline maps if available
    }
}
```

**MapView Initialization (ProNavigationScreen.kt)**:
```kotlin
@Composable
fun ProNavigationMapView() {
    AndroidView(
        factory = { context ->
            MapView(context).apply {
                // Configure map for truck routing display
                mapScene.loadScene(MapScheme.NORMAL_DAY) { error ->
                    if (error != null) {
                        Log.e("MapView", "Map scene load failed: ${error.name}")
                    } else {
                        // Map loaded successfully
                        configureMapForTruckRouting(this)
                    }
                }
            }
        },
        update = { mapView ->
            // Update map state
        }
    )
}

private fun configureMapForTruckRouting(mapView: MapView) {
    // Set camera position
    // Enable traffic layer
    // Configure truck-specific visual preferences
    // Set up gesture handlers
}
```

### 4.2 iOS Initialization

**App Delegate (AppDelegate.swift)**:
```swift
import UIKit
import heresdk

@main
class AppDelegate: UIResponder, UIApplicationDelegate {
    
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        initializeHERESDK()
        return true
    }
    
    private func initializeHERESDK() {
        guard let accessKeyId = Bundle.main.infoDictionary?["HEREAccessKeyId"] as? String,
              let accessKeySecret = Bundle.main.infoDictionary?["HEREAccessKeySecret"] as? String else {
            print("HERE SDK credentials not found in Info.plist")
            handleSDKInitFailure()
            return
        }
        
        let options = SDKOptions(accessKeyId: accessKeyId, accessKeySecret: accessKeySecret)
        
        do {
            try SDKNativeEngine.makeSharedInstance(options: options)
            print("HERE SDK initialized successfully")
            enableProTierFeatures()
        } catch let engineInstantiationError {
            print("HERE SDK initialization failed: \\(engineInstantiationError)")
            handleSDKInitFailure()
        }
    }
    
    private func handleSDKInitFailure() {
        // Store failure state
        // Disable Pro tier
        // Notify user
    }
    
    private func enableProTierFeatures() {
        // Enable truck routing
        // Load offline maps
    }
}
```

**MapView in SwiftUI (ProNavigationView.swift)**:
```swift
import SwiftUI
import heresdk

struct ProNavigationMapView: UIViewRepresentable {
    
    func makeUIView(context: Context) -> MapView {
        let mapView = MapView(frame: .zero)
        
        mapView.mapScene.loadScene(mapScheme: .normalDay) { error in
            if let error = error {
                print("Map scene load failed: \\(error)")
            } else {
                configureMapForTruckRouting(mapView: mapView)
            }
        }
        
        return mapView
    }
    
    func updateUIView(_ mapView: MapView, context: Context) {
        // Update map state
    }
    
    private func configureMapForTruckRouting(mapView: MapView) {
        // Set camera position
        // Enable truck-specific layers
        // Configure visual preferences
    }
}
```

---

## 5. Offline Maps Setup

### 5.1 Region Download (Android)

```kotlin
class OfflineMapDownloader(private val mapUpdater: MapUpdater) {
    
    fun downloadRegion(region: Region, callback: (DownloadProgress) -> Unit) {
        val regions = listOf(region)
        
        mapUpdater.downloadRegions(
            regions,
            object : DownloadRegionsStatusListener {
                override fun onProgress(progress: MapUpdateProgress) {
                    val percentComplete = (progress.completedBytes.toDouble() / 
                        progress.totalBytes.toDouble() * 100).toInt()
                    callback(DownloadProgress(percentComplete, progress.completedBytes))
                }
                
                override fun onComplete(error: MapUpdateError?) {
                    if (error != null) {
                        Log.e("OfflineMap", "Download failed: ${error.name}")
                    } else {
                        Log.i("OfflineMap", "Region downloaded successfully")
                    }
                }
            }
        )
    }
    
    fun getAvailableRegions(): List<Region> {
        return mapUpdater.getDownloadableRegions()
    }
}
```

### 5.2 Region Download (iOS)

```swift
class OfflineMapDownloader {
    private let mapUpdater: MapUpdater
    
    init(mapUpdater: MapUpdater) {
        self.mapUpdater = mapUpdater
    }
    
    func downloadRegion(
        region: Region,
        progressHandler: @escaping (Int, Int64) -> Void,
        completionHandler: @escaping (Error?) -> Void
    ) {
        mapUpdater.downloadRegions(
            regions: [region],
            statusListener: MapUpdateStatusListener(
                onProgress: { progress in
                    let percent = Int((Double(progress.completedBytes) / 
                        Double(progress.totalBytes)) * 100)
                    progressHandler(percent, progress.completedBytes)
                },
                onComplete: { error in
                    completionHandler(error)
                }
            )
        )
    }
}
```

---

## 6. Error Handling

### 6.1 Initialization Errors

**InstantiationErrorCode Types**:
- `AUTHENTICATION_FAILED`: Invalid credentials
- `NETWORK_ERROR`: Cannot reach HERE servers
- `DEVICE_NOT_SUPPORTED`: Hardware/OS incompatible
- `INTERNAL_ERROR`: SDK internal failure

**Handling Strategy**:
1. Log error details for debugging
2. Disable Pro tier features gracefully
3. Offer user option to continue with Plus tier
4. Provide clear error message (avoid technical jargon)
5. Allow retry with exponential backoff

### 6.2 Runtime Errors

**Map Loading Errors**:
- `AUTHENTICATION_FAILED`: Re-authenticate
- `DISK_FULL`: Prompt user to free space
- `NETWORK_ERROR`: Retry with exponential backoff
- `MALFORMED_MAP_DATA`: Clear cache and re-download

**Routing Errors**:
- `ROUTE_NOT_FOUND`: Suggest alternative start/end points
- `VIOLATION`: Display restriction details to user
- `NO_ROUTE_FOUND_WITH_RESTRICTIONS`: Offer to relax constraints
- `NETWORK_ERROR`: Use cached routes if available

---

## 7. Performance Optimization

### 7.1 Map Rendering

**Frame Rate Targets**:
- Minimum: 30 FPS
- Target: 60 FPS
- Reduce quality if FPS drops below 25

**Optimization Techniques**:
- Render only visible tiles
- Use LOD (Level of Detail) for distant objects
- Cache frequently accessed tiles
- Preload tiles along route corridor
- Limit number of 3D buildings rendered

### 7.2 Memory Management

**Memory Limits**:
- Android: Max 256 MB for map cache
- iOS: Max 256 MB for map cache
- Offline maps stored on disk, not in memory

**Cache Strategy**:
- LRU eviction for tile cache
- Persist favorite routes
- Clear cache on low memory warning
- Compress offline map data

---

## 8. Platform-Specific Considerations

### 8.1 Android

**Permissions Required**:
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" 
    android:maxSdkVersion="28" />
```

**ProGuard Rules**:
```proguard
-keep class com.here.sdk.** { *; }
-dontwarn com.here.sdk.**
```

### 8.2 iOS

**Permissions Required (Info.plist)**:
```xml
<key>NSLocationWhenInUseUsageDescription</key>
<string>GemNav needs your location to provide truck navigation</string>
<key>NSLocationAlwaysAndWhenInUseUsageDescription</key>
<string>GemNav needs your location for turn-by-turn navigation</string>
```

**Background Modes**:
```xml
<key>UIBackgroundModes</key>
<array>
    <string>location</string>
    <string>audio</string>
</array>
```

---

## 9. Testing & Validation

### 9.1 Initialization Tests

**Test Cases**:
1. Valid credentials → SDK initializes successfully
2. Invalid credentials → Graceful error handling
3. Network unavailable → Offline mode works
4. Device incompatible → Clear error message
5. First launch → Proper setup flow

### 9.2 Map Rendering Tests

**Test Cases**:
1. Map loads within 3 seconds on 4G connection
2. Offline map displays without network
3. Tile cache eviction works correctly
4. Memory usage stays under limit
5. FPS remains above 30 under load

---

## 10. Deployment Checklist

**Before Production**:
- [ ] HERE credentials secured (not in git)
- [ ] ProGuard rules tested on release build
- [ ] Offline maps tested in airplane mode
- [ ] Error handling tested for all scenarios
- [ ] Memory profiling completed
- [ ] FPS benchmarks meet targets
- [ ] Crash reporting integrated
- [ ] Analytics events configured
- [ ] Legal compliance verified
- [ ] Terms of service reviewed

---

## 11. Support & Documentation

**HERE SDK Documentation**: https://developer.here.com/documentation/android-sdk-navigate/  
**HERE SDK iOS Documentation**: https://developer.here.com/documentation/ios-sdk-navigate/  
**GemNav Internal Docs**: See `/docs/here/` for routing logic and compliance rules  
**Support Contact**: support@gemnav.app

---

**File Version**: 1.0  
**Last Updated**: 2025-11-21  
**Next Review**: 2025-12-21
