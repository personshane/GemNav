# MP-A09-A: iOS Build System Audit Report

**Date**: 2025-11-22
**Project**: GemNav iOS Build Scaffolding
**Audit Type**: READ-ONLY (No build commands executed)

---

## A. Podfile Audit

### Pod Declarations
```ruby
platform :ios, '16.0'

target 'GemNav' do
  use_frameworks!
  
  # 1. GoogleMaps (~> 8.0) - Remote pod
  # 2. HEREMapsSDK - Local path: '../here-sdk'
  # 3. Firebase/Crashlytics - Remote pod
  # 4. Firebase/Analytics - Remote pod
  # 5. SwiftProtobuf (~> 1.24) - Remote pod
end
```

### Pod Analysis

**Remote Pods (Will Download from CocoaPods)**:
1. **GoogleMaps** (~> 8.0)
   - Status: ✅ Valid version constraint
   - Source: CocoaPods trunk
   - Size: ~140MB
   - Requires: Google Maps API key configuration

2. **Firebase/Crashlytics**
   - Status: ✅ Valid subspecs
   - Source: CocoaPods trunk
   - Requires: GoogleService-Info.plist (MISSING)

3. **Firebase/Analytics**
   - Status: ✅ Valid subspecs
   - Source: CocoaPods trunk
   - Requires: GoogleService-Info.plist (MISSING)

4. **SwiftProtobuf** (~> 1.24)
   - Status: ✅ Valid version constraint
   - Source: CocoaPods trunk
   - Size: Small

**Local Path Pods**:
1. **HEREMapsSDK**
   - Path: `../here-sdk`
   - Resolved Path: `C:\Users\perso\here-sdk`
   - Status: ❌ **FATAL - Directory does not exist**
   - Expected: HERE SDK XCFramework binary

### Podfile Structure
- ✅ Correct platform declaration (iOS 16.0)
- ✅ Valid target declaration ('GemNav')
- ✅ use_frameworks! directive present
- ✅ No syntax errors detected
- ✅ No version conflicts detected

---

## B. Directory Structure Audit

### Present Directories
```
ios/
├── GemNav.xcodeproj/          ✅ EXISTS
│   ├── project.pbxproj         ✅ EXISTS (303 lines)
│   └── xcshareddata/           ✅ EXISTS
│       └── xcschemes/
│           └── GemNav.xcscheme ✅ EXISTS (79 lines)
├── GemNav.xcworkspace/         ✅ EXISTS
│   └── contents.xcworkspacedata ✅ EXISTS (11 lines, references correct projects)
├── Pods/                       ✅ EXISTS (placeholder)
│   ├── Manifest.lock           ✅ EXISTS (placeholder)
│   ├── Pods.xcodeproj/         ✅ EXISTS (minimal placeholder)
│   │   └── project.pbxproj     ✅ EXISTS (185 lines)
│   └── Target Support Files/   ✅ EXISTS (empty)
├── Podfile                     ✅ EXISTS (20 lines)
└── GemNav/                     ✅ EXISTS
    ├── Core/                   ✅ EXISTS (2 Swift files)
    ├── Navigation/             ✅ EXISTS (1 Swift file)
    └── Voice/                  ✅ EXISTS (8 Swift files)
        ├── Permissions/        ✅ EXISTS (1 Swift file)
        └── UI/                 ✅ EXISTS (4 Swift files)
```

### Swift File Count
**Total: 11 Swift files** (confirmed present on filesystem)

**Breakdown**:
- Core: 2 files (DependencyContainer.swift, TierManager.swift)
- Navigation: 1 file (NavigationView.swift)
- Voice: 3 files (IOSSpeechRecognitionService, IOSVoiceResponseService, IOSWakeWordDetector)
- Voice/Permissions: 1 file (MicrophonePermissionManager.swift)
- Voice/UI: 4 files (VoiceButton, VoiceFeedbackOverlay, VoicePermissionView, WakeWordIndicator)

### Xcode Project Analysis

**GemNav.xcodeproj/project.pbxproj**:
- ✅ Valid pbxproj structure
- ✅ References Core, Navigation, Voice as folder groups
- ❌ **NO Swift files added to build phases** (PBXBuildFile section is empty)
- ❌ **NO file references for individual Swift files** (only folder references)
- ✅ Bundle ID: com.gemnav.app
- ✅ Deployment Target: iOS 16.0
- ✅ Swift Version: 5.0
- ✅ GENERATE_INFOPLIST_FILE = YES (auto-generates Info.plist)
- ⚠️ DEVELOPMENT_TEAM = "" (empty, will require manual setting)

**GemNav.xcworkspace/contents.xcworkspacedata**:
- ✅ References GemNav.xcodeproj correctly
- ✅ References Pods/Pods.xcodeproj correctly

---

## C. Missing Dependencies (FATAL)

These issues will cause `pod install` to **FAIL IMMEDIATELY**:

### 1. HERE SDK Directory Missing ❌ FATAL
- **Expected Path**: `C:\Users\perso\here-sdk`
- **Status**: Directory does not exist
- **Impact**: `pod install` will fail with "No such file or directory" error
- **Podfile Line**: `pod 'HEREMapsSDK', :path => '../here-sdk'`
- **Fix Required**: Create placeholder structure OR remove from Podfile

---

## D. Missing Dependencies (NON-FATAL)

These issues will NOT block `pod install` but WILL block builds/runtime:

### 1. Missing App Entry Point ⚠️ CRITICAL
- **File**: `GemNav/GemNavApp.swift` or `GemNav/App.swift`
- **Status**: Does not exist
- **Impact**: Project will not compile (no @main entry point)
- **Required Content**:
```swift
import SwiftUI

@main
struct GemNavApp: App {
    var body: some Scene {
        WindowGroup {
            NavigationView()
        }
    }
}
```

### 2. Firebase Configuration Missing ⚠️ CRITICAL
- **File**: `GemNav/GoogleService-Info.plist`
- **Status**: Does not exist
- **Impact**: Firebase pods will crash at runtime
- **Required**: Download from Firebase Console

### 3. Google Maps API Key Missing ⚠️ CRITICAL
- **Configuration**: Must be added to Info.plist or build settings
- **Key**: `GMSServicesKey`
- **Impact**: Google Maps SDK will not initialize

### 4. No Swift Files in Build Phases ⚠️ CRITICAL
- **Status**: project.pbxproj has empty PBXBuildFile section
- **Impact**: None of the 11 Swift files will be compiled
- **Cause**: Files added as folder references, not file references
- **Fix Required**: Re-add Swift files to project with proper file references

### 5. No Asset Catalog ⚠️ WARNING
- **Expected**: `GemNav/Assets.xcassets`
- **Status**: Does not exist
- **Impact**: AppIcon and AccentColor references will fail
- **Build Setting References**:
  - ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon
  - ASSETCATALOG_COMPILER_GLOBAL_ACCENT_COLOR_NAME = AccentColor

### 6. Empty Development Team ⚠️ WARNING
- **Build Setting**: DEVELOPMENT_TEAM = ""
- **Impact**: Cannot sign for device builds
- **Fix**: Set to valid Apple Developer Team ID

---

## E. Recommended Fix Order

### Phase 1: Resolve FATAL Blockers (Required for `pod install`)

**Option A: Create HERE SDK Placeholder**
```
1. Create: C:\Users\perso\here-sdk\
2. Create: C:\Users\perso\here-sdk\HEREMapsSDK.podspec
3. Create minimal placeholder structure
```

**Option B: Remove HERE SDK from Podfile (Recommended for initial testing)**
```
1. Comment out or remove: pod 'HEREMapsSDK', :path => '../here-sdk'
2. Document: Will add back when HERE SDK binary is available
```

### Phase 2: Run `pod install` (MP-A09-B)
After Phase 1 complete, `pod install` should succeed and generate:
- Pods/ directory with real frameworks
- GemNav.xcworkspace with proper pod integration
- Podfile.lock with resolved dependencies

### Phase 3: Fix Build Blockers (Required for compilation)

**Priority Order**:
1. **Add Swift files to project**
   - Open GemNav.xcodeproj in Xcode
   - Remove folder references
   - Add all 11 .swift files individually with "Add to target: GemNav" checked
   - Verify in Build Phases → Compile Sources

2. **Create App entry point**
   - Create: `GemNav/GemNavApp.swift`
   - Add @main struct with WindowGroup

3. **Create Assets.xcassets**
   - Create: `GemNav/Assets.xcassets`
   - Add AppIcon.appiconset
   - Add AccentColor.colorset

4. **Add Firebase configuration**
   - Download GoogleService-Info.plist from Firebase Console
   - Add to GemNav/ folder
   - Add to Xcode project

5. **Configure Google Maps API**
   - Create or obtain API key from Google Cloud Console
   - Add to build settings or Info.plist

6. **Set Development Team**
   - In Xcode: Signing & Capabilities
   - Select Apple Developer Team
   - Or set manually in project.pbxproj

### Phase 4: Test Build (MP-A09-C)
- Build for simulator
- Resolve any remaining issues
- Document successful build

---

## F. Readiness Assessment

### Current Status: ❌ NOT READY for `pod install`

**Blocking Issues**: 1 FATAL
- HERE SDK path does not exist

**Non-Blocking Issues**: 6 CRITICAL/WARNING
- Missing app entry point
- Missing Firebase config
- Missing Google Maps API key
- No Swift files in build phases
- No asset catalog
- Empty development team

### Recommendation

**For MP-A09-B (Next Phase)**:

**OPTION 1: Remove HERE SDK temporarily**
- Edit Podfile: Comment out HEREMapsSDK line
- Run `pod install`
- Verify Google Maps + Firebase pods install correctly
- Document: Will add HERE SDK in future MP when binary is available

**OPTION 2: Create HERE SDK placeholder**
- More complex, requires creating fake podspec
- Not recommended without actual HERE SDK binary

**RECOMMENDED PATH**: Option 1

This allows:
- ✅ Testing Google Maps integration
- ✅ Testing Firebase integration
- ✅ Testing SwiftProtobuf integration
- ✅ Validating Podfile structure
- ✅ Generating Podfile.lock for version tracking
- ⚠️ Defer HERE SDK integration to future MP when binary is available

---

## G. Summary

### What Works
- ✅ Podfile structure is valid
- ✅ All remote pods have correct version constraints
- ✅ Directory structure is correct
- ✅ Xcode project exists with correct settings
- ✅ Workspace references are correct
- ✅ 11 Swift source files exist on filesystem

### What Blocks `pod install`
- ❌ HERE SDK directory missing at `../here-sdk`

### What Blocks Build (after `pod install`)
- ❌ No Swift files added to build phases
- ❌ No app entry point (@main)
- ❌ No Firebase config
- ❌ No Google Maps API key
- ❌ No asset catalog

### Recommended Next Steps
1. **MP-A09-B**: Modify Podfile to remove HERE SDK, run `pod install`
2. **MP-A09-C**: Fix build blockers, add files to Xcode project
3. **MP-A09-D**: Test build for simulator
4. **MP-A10**: Integrate HERE SDK when binary is available

---

**Audit Complete**
**No filesystem modifications made**
**No build commands executed**
