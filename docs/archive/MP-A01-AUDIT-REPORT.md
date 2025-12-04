# MP-A01: COMPLETE FILE INTEGRITY AUDIT
**GEMNAV PROJECT — FORENSIC FILE SCAN**
**DATE**: 2025-11-25
**MODE**: DISK SCAN ONLY — NO MEMORY
**STATUS**: CRITICAL ISSUES DETECTED

---

## SECTION 1: DIRECTORY TREE STRUCTURE

### ROOT STRUCTURE
```
GemNav/
├── .claude/                     # Claude artifacts
├── .git/                        # Git repository
├── .vscode/                     # VS Code settings
├── android/                     # Android implementation
├── architecture/                # Architecture docs
├── build/                       # Build configuration docs
├── docs/                        # Project documentation
├── google/                      # Google Maps integration docs
├── here/                        # HERE SDK integration docs
├── ios/                         # iOS implementation
├── logo/                        # Logo assets
├── prompts/                     # AI prompts
└── [Root files]
```

### ANDROID MODULE STRUCTURE (ACTUAL)
```
android/
├── app/
│   ├── api/                     # ⚠️ WRONG LOCATION
│   │   ├── DirectionsApiClient.kt
│   │   ├── GeminiApiClient.kt
│   │   ├── HereApiClient.kt
│   │   └── PlacesApiClient.kt
│   ├── core/                    # ⚠️ WRONG LOCATION
│   │   └── TierManager.kt
│   ├── di/                      # ⚠️ WRONG LOCATION
│   │   ├── ApiModule.kt
│   │   ├── AppModule.kt
│   │   ├── DatabaseModule.kt
│   │   ├── RepositoryModule.kt
│   │   ├── ServiceModule.kt
│   │   └── ViewModelModule.kt
│   ├── main_flow/               # ⚠️ WRONG LOCATION
│   │   ├── database/
│   │   ├── models/
│   │   ├── ui/
│   │   └── [ViewModels & Repositories]
│   ├── navigation/              # ⚠️ WRONG LOCATION
│   │   ├── NavigationActivity.kt
│   │   ├── NavigationLauncher.kt
│   │   └── NavigationViewModel.kt
│   ├── search/                  # ⚠️ WRONG LOCATION
│   │   └── SearchViewModel.kt
│   ├── voice/                   # ⚠️ WRONG LOCATION
│   │   ├── permissions/
│   │   ├── ui/
│   │   └── [Voice services]
│   └── src/
│       └── main/
│           └── java/
│               └── com/
│                   └── gemnav/
│                       ├── app/         # ✓ CORRECT LOCATION
│                       ├── core/        # ✓ CORRECT LOCATION
│                       └── data/        # ✓ CORRECT LOCATION
```

### IOS STRUCTURE
```
ios/
├── app/                         # ⚠️ WRONG LOCATION
│   ├── viewmodels/
│   └── voice/
└── GemNav/                      # ✓ CORRECT LOCATION
    ├── Core/
    ├── Navigation/
    └── Voice/
```

---

## SECTION 2: FULL FILE INVENTORY

### ANDROID SOURCE FILES IN CORRECT LOCATION
*Location: android/app/src/main/java/com/gemnav/*

| File Path | Package | Lines | Size (KB) | Status |
|-----------|---------|-------|-----------|--------|
| app/GemNavApplication.kt | com.gemnav.app | ~50 | ~2 | ✓ |
| app/MainActivity.kt | com.gemnav.app | ~80 | ~3 | ✓ |
| app/models/Destination.kt | com.gemnav.app.models | ~30 | ~1 | ✓ |
| app/ui/AppNavHost.kt | com.gemnav.app.ui | ~120 | ~5 | ✓ |
| app/ui/common/SafeModeBanner.kt | com.gemnav.app.ui.common | ~40 | ~2 | ✓ |
| app/ui/common/VoiceButton.kt | com.gemnav.app.ui.common | ~60 | ~2 | ✓ |
| app/ui/mainflow/[6 files] | com.gemnav.app.ui.mainflow | ~500 | ~20 | ✓ |
| app/ui/map/GoogleMapContainer.kt | com.gemnav.app.ui.map | ~80 | ~3 | ✓ |
| app/ui/map/HereMapContainer.kt | com.gemnav.app.ui.map | ~90 | ~4 | ✓ |
| app/ui/route/[3 files] | com.gemnav.app.ui.route | ~300 | ~12 | ✓ |
| app/ui/search/[2 files] | com.gemnav.app.ui.search | ~200 | ~8 | ✓ |
| app/ui/settings/[2 files] | com.gemnav.app.ui.settings | ~150 | ~6 | ✓ |
| app/ui/theme/AppTheme.kt | com.gemnav.app.ui.theme | ~100 | ~4 | ✓ |
| app/ui/voice/[3 files] | com.gemnav.app.ui.voice | ~250 | ~10 | ✓ |
| app/voice/VoiceFeedbackManager.kt | com.gemnav.app.voice | ~80 | ~3 | ✓ |
| core/feature/FeatureGate.kt | com.gemnav.core.feature | ~120 | ~5 | ✓ |
| core/here/HereEngineManager.kt | com.gemnav.core.here | ~200 | ~8 | ✓ |
| core/location/[2 files] | com.gemnav.core.location | ~250 | ~10 | ✓ |
| core/maps/google/[3 files] | com.gemnav.core.maps.google | ~400 | ~16 | ✓ |
| core/navigation/[6 files] | com.gemnav.core.navigation | ~600 | ~24 | ✓ |
| core/places/[2 files] | com.gemnav.core.places | ~300 | ~12 | ✓ |
| core/shim/[7 files] | com.gemnav.core.shim | ~800 | ~32 | ✓ |
| core/subscription/[3 files] | com.gemnav.core.subscription | ~300 | ~12 | ✓ |
| core/voice/SpeechRecognizerManager.kt | com.gemnav.core.voice | ~150 | ~6 | ✓ |
| data/ai/[2 files] | com.gemnav.data.ai | ~150 | ~6 | ✓ |
| data/navigation/NavigationState.kt | com.gemnav.data.navigation | ~100 | ~4 | ✓ |
| data/route/TruckRouteResult.kt | com.gemnav.data.route | ~80 | ~3 | ✓ |

**SUBTOTAL**: ~60 files, ~5,500 lines in correct location

### ANDROID SOURCE FILES IN WRONG LOCATION
*Location: android/app/* (should be in src/main/java)

| File Path | Package | Lines | Size (KB) | Status |
|-----------|---------|-------|-----------|--------|
| api/DirectionsApiClient.kt | com.gemnav.app.api | 187 | 6.4 | ⚠️ RELOCATE |
| api/GeminiApiClient.kt | com.gemnav.app.api | ~150 | ~6 | ⚠️ RELOCATE |
| api/HereApiClient.kt | com.gemnav.app.api | ~200 | ~8 | ⚠️ RELOCATE |
| api/PlacesApiClient.kt | com.gemnav.app.api | ~120 | ~5 | ⚠️ RELOCATE |
| core/TierManager.kt | com.gemnav.app.core | 67 | 2.5 | ⚠️ RELOCATE |
| di/ApiModule.kt | com.gemnav.app.di | 63 | 2.4 | ⚠️ RELOCATE |
| di/AppModule.kt | com.gemnav.app.di | ~60 | ~2 | ⚠️ RELOCATE |
| di/DatabaseModule.kt | com.gemnav.app.di | ~50 | ~2 | ⚠️ RELOCATE |
| di/RepositoryModule.kt | com.gemnav.app.di | ~50 | ~2 | ⚠️ RELOCATE |
| di/ServiceModule.kt | com.gemnav.app.di | ~80 | ~3 | ⚠️ RELOCATE |
| di/ViewModelModule.kt | com.gemnav.app.di | ~70 | ~3 | ⚠️ RELOCATE |
| main_flow/database/[3 files] | com.gemnav.android.app.main_flow.database | ~200 | ~8 | ⚠️ PKG MISMATCH |
| main_flow/models/[3 files] | com.gemnav.android.app.main_flow.models | ~150 | ~6 | ⚠️ PKG MISMATCH |
| main_flow/ui/[7 files] | com.gemnav.android.app.main_flow.ui | ~500 | ~20 | ⚠️ PKG MISMATCH |
| main_flow/[5 ViewModels/Repos] | com.gemnav.android.app.main_flow | ~500 | ~20 | ⚠️ PKG MISMATCH |
| navigation/NavigationActivity.kt | com.gemnav.android.navigation | 542 | 21 | ⚠️ PKG MISMATCH |
| navigation/NavigationLauncher.kt | com.gemnav.android.navigation | ~80 | ~3 | ⚠️ PKG MISMATCH |
| navigation/NavigationViewModel.kt | com.gemnav.android.navigation | ~200 | ~8 | ⚠️ PKG MISMATCH |
| search/SearchViewModel.kt | com.gemnav.app.search | ~150 | ~6 | ⚠️ RELOCATE |
| voice/[14 files] | com.gemnav.app.voice | ~1500 | ~60 | ⚠️ RELOCATE |

**SUBTOTAL**: ~50 files, ~4,500 lines in wrong location

### IOS SOURCE FILES

| File Path | Lines | Size (KB) | Status |
|-----------|-------|-----------|--------|
| GemNav/Core/DependencyContainer.swift | ~100 | ~4 | ✓ |
| GemNav/Core/TierManager.swift | ~80 | ~3 | ✓ |
| GemNav/Navigation/NavigationView.swift | ~150 | ~6 | ✓ |
| GemNav/Voice/[4 services] | ~500 | ~20 | ✓ |
| GemNav/Voice/Permissions/[1 file] | ~80 | ~3 | ✓ |
| GemNav/Voice/UI/[4 files] | ~300 | ~12 | ✓ |
| app/viewmodels/[2 files] | ~300 | ~12 | ⚠️ RELOCATE |
| app/voice/[9 files] | ~800 | ~32 | ⚠️ DUPLICATES |

**SUBTOTAL**: ~20 iOS files, ~2,300 lines

---

## SECTION 3: DUPLICATE DETECTION

### CRITICAL DUPLICATES FOUND

#### 1. VoiceButton.kt — THREE COPIES
```
android/app/src/main/java/com/gemnav/app/ui/common/VoiceButton.kt (✓ KEEP)
android/app/src/main/java/com/gemnav/app/ui/voice/VoiceButton.kt (❌ DELETE)
android/app/voice/ui/VoiceButton.kt (❌ DELETE)
```
**ACTION**: Keep only the common/VoiceButton.kt version.

#### 2. TierManager.kt — TWO COPIES
```
android/app/core/TierManager.kt (❌ DELETE - old version)
android/app/src/main/java/com/gemnav/core/subscription/TierManager.kt (✓ KEEP)
```
**ACTION**: Delete the one in android/app/core.

#### 3. HomeViewModel.kt — TWO COPIES
```
android/app/main_flow/HomeViewModel.kt (❌ OLD)
android/app/src/main/java/com/gemnav/app/ui/mainflow/HomeViewModel.kt (✓ KEEP)
```
**ACTION**: Delete the main_flow version.

#### 4. SearchViewModel.kt — THREE COPIES
```
android/app/search/SearchViewModel.kt (❌ DELETE)
android/app/main_flow/ (check if exists) (❌ DELETE)
android/app/src/main/java/com/gemnav/app/ui/search/SearchViewModel.kt (✓ KEEP)
```
**ACTION**: Keep only the one in ui/search.

#### 5. NavigationViewModel.kt — TWO COPIES
```
android/app/navigation/NavigationViewModel.kt (❌ OLD)
android/app/src/main/java/com/gemnav/app/ui/route/RouteDetailsViewModel.kt (✓ KEEP - renamed)
```
**ACTION**: Delete navigation/NavigationViewModel.kt.

#### 6. iOS Voice Files — COMPLETE DUPLICATION
```
ios/app/voice/ [9 files] (❌ DELETE ALL)
ios/GemNav/Voice/ [same files] (✓ KEEP)
```
**ACTION**: Delete entire ios/app/voice directory.

#### 7. iOS ViewModel Files — DUPLICATES
```
ios/app/viewmodels/ (❌ DELETE)
ios/GemNav/ (✓ KEEP - should contain all VMs)
```
**ACTION**: Verify and delete ios/app/viewmodels.

---

## SECTION 4: ORPHANED / UNREFERENCED FILES

### FILES NOT IN BUILD PATH
All files in these directories are NOT compiled:
```
android/app/api/          [4 files]
android/app/core/         [1 file]
android/app/di/           [6 files]
android/app/main_flow/    [~25 files]
android/app/navigation/   [3 files]
android/app/search/       [1 file]
android/app/voice/        [14 files]
ios/app/                  [~11 files]
```

**TOTAL ORPHANED**: ~65 files not in build path

### OLD MICRO-PROJECT ARTIFACTS
These directories contain code from old MPs:
```
android/app/main_flow/    (MP-010 to MP-013 remnants)
android/app/navigation/   (MP-014 remnants)
android/app/voice/        (MP-015 remnants)
ios/app/                  (MP-015 iOS remnants)
```

---

## SECTION 5: PACKAGE MISMATCHES

### INCONSISTENT PACKAGE NAMING

#### Pattern 1: com.gemnav.app.* vs com.gemnav.android.*
```
CORRECT:   com.gemnav.app.*
INCORRECT: com.gemnav.android.* (used in main_flow, navigation)
```

#### Pattern 2: Missing 'app' in core packages
```
CORRECT:   com.gemnav.core.*
CORRECT:   com.gemnav.app.*
CORRECT:   com.gemnav.data.*
```

### FILES WITH WRONG PACKAGES
```
android/app/main_flow/HomeViewModel.kt
  DECLARES: com.gemnav.android.app.main_flow
  SHOULD BE: com.gemnav.app.ui.mainflow (and relocated)

android/app/navigation/NavigationActivity.kt
  DECLARES: com.gemnav.android.navigation
  SHOULD BE: com.gemnav.app.navigation (and relocated)

android/app/main_flow/database/GemNavDatabase.kt
  DECLARES: com.gemnav.android.app.main_flow.database
  SHOULD BE: com.gemnav.data.local.database
```

---

## SECTION 6: LOCATION ERRORS

### STRUCTURAL VIOLATIONS

#### Android Source Code Organization
**RULE**: All .kt source files must be in `android/app/src/main/java/`
**VIOLATIONS**: 54+ files outside this structure

#### Correct Android Structure Should Be:
```
android/app/src/main/java/com/gemnav/
├── app/
│   ├── GemNavApplication.kt
│   ├── MainActivity.kt
│   ├── api/              ← MOVE from android/app/api/
│   ├── di/               ← MOVE from android/app/di/
│   ├── models/
│   ├── ui/
│   └── voice/            ← MOVE from android/app/voice/
├── core/
│   ├── feature/
│   ├── here/
│   ├── location/
│   ├── maps/
│   ├── navigation/
│   ├── places/
│   ├── shim/
│   ├── subscription/
│   └── voice/
└── data/
    ├── ai/
    ├── local/            ← MOVE main_flow/database/ here
    ├── navigation/
    ├── repository/       ← MOVE repositories here
    └── route/
```

#### iOS Source Organization
**VIOLATIONS**: ios/app/ directory shouldn't exist
**CORRECT**: All source in ios/GemNav/

---

## SECTION 7: SUMMARY — PASS/FAIL

### ❌ AUDIT STATUS: FAILED

### CRITICAL ISSUES (Must Fix)
1. **54+ source files in wrong locations** — Not in src/main/java
2. **7+ duplicate files** — Same classes in multiple locations
3. **3 package naming patterns** — Inconsistent package declarations
4. **65+ orphaned files** — Not compiled, not referenced
5. **Build system ignoring most code** — Only src/main/java compiled

### SEVERITY ASSESSMENT
```
BLOCKING:    File location errors (prevents clean builds)
CRITICAL:    Duplicate files (ambiguous references)
HIGH:        Package mismatches (import errors)
MEDIUM:      Orphaned files (clutter, confusion)
LOW:         Documentation organization
```

### ESTIMATED CODEBASE SIZE
```
Total Kotlin Files:    ~110 files
Total Lines:           ~12,000 lines
In Correct Location:   ~5,500 lines (46%)
In Wrong Location:     ~4,500 lines (38%)
Duplicates/Orphans:    ~2,000 lines (16%)
```

### BUILD SYSTEM IMPACT
**Current state**: Android build only compiles files in `src/main/java/`
**Files ignored**: Everything in android/app/{api,core,di,main_flow,navigation,search,voice}
**Consequence**: Project appears to compile but missing functionality

---

## SECTION 8: REQUIRED FIXES FOR MP-A02

### PHASE 1: ELIMINATE DUPLICATES (Priority: CRITICAL)
```
DELETE android/app/src/main/java/com/gemnav/app/ui/voice/VoiceButton.kt
DELETE android/app/core/TierManager.kt
DELETE android/app/main_flow/HomeViewModel.kt
DELETE android/app/search/SearchViewModel.kt
DELETE android/app/navigation/NavigationViewModel.kt
DELETE ios/app/voice/ [entire directory]
DELETE ios/app/viewmodels/ [entire directory]
```

### PHASE 2: RELOCATE FILES (Priority: CRITICAL)
Move all files from wrong locations to correct src/main/java structure:
```
MOVE android/app/api/* → android/app/src/main/java/com/gemnav/app/api/
MOVE android/app/di/* → android/app/src/main/java/com/gemnav/app/di/
MOVE android/app/voice/* → android/app/src/main/java/com/gemnav/app/voice/
```

### PHASE 3: CONSOLIDATE main_flow (Priority: HIGH)
Merge main_flow files into proper structure:
```
MOVE main_flow/database/* → data/local/database/
MOVE main_flow/models/* → app/models/ (merge with existing)
MOVE main_flow/ui/* → app/ui/mainflow/ (merge with existing)
MOVE main_flow/*Repository.kt → data/repository/
UPDATE all package declarations to match new locations
```

### PHASE 4: FIX PACKAGE DECLARATIONS (Priority: HIGH)
```
REPLACE "com.gemnav.android." → "com.gemnav."
REPLACE "com.gemnav.android.app.main_flow" → "com.gemnav.app.ui.mainflow"
REPLACE "com.gemnav.android.navigation" → "com.gemnav.app.navigation"
UPDATE all imports in dependent files
```

### PHASE 5: CLEAN UP ORPHANS (Priority: MEDIUM)
```
DELETE android/app/navigation/ [after migration]
DELETE android/app/main_flow/ [after migration]
DELETE ios/app/ [after verification]
```

### PHASE 6: VERIFY BUILD (Priority: CRITICAL)
```
RUN ./gradlew clean
RUN ./gradlew build
VERIFY no compilation errors
VERIFY all features still functional
```

---

## MP-A01 COMPLETION CRITERIA

- [x] Complete directory tree loaded
- [x] Full file inventory created
- [x] Duplicates identified
- [x] Orphaned files identified
- [x] Package mismatches documented
- [x] Location errors catalogued
- [x] Pass/Fail assessment complete
- [x] MP-A02 action plan defined

**NEXT MICRO-PROJECT**: MP-A02 — File Relocation & Duplicate Elimination

---

## FORENSIC NOTES

### Why This Happened
1. Multiple micro-projects created parallel file structures
2. Refactoring moved some files but not others
3. Package renaming incomplete (android.* vs app.*)
4. Build system only sees src/main/java (others ignored)
5. No systematic cleanup between micro-projects

### Critical Insight
**The project appears to build successfully because Gradle ignores files outside src/main/java.** This created false confidence that the codebase was complete, when in fact ~50 files with ~4,500 lines of code are not being compiled.

---

**END OF MP-A01 AUDIT REPORT**
