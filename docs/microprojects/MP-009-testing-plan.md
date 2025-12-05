# MP-009: GemNav Testing Plan

**Status**: ✅ COMPLETE  
**Created**: 2025-12-03  
**Dependencies**: MP-001 through MP-008 (specifications), Android refactoring

---

## Test Strategy

### Testing Approach
- **Unit Testing**: Validate individual modules in isolation
- **Integration Testing**: Verify module interactions and data flow
- **UI Testing**: Ensure user-facing components function correctly
- **Framework**: JUnit 5 + MockK for Kotlin, Espresso for UI

### Testing Priorities
1. **Critical Path**: safety/, data/navigation/, core/location/
2. **High Risk**: HERE SDK integration, Google Maps API calls
3. **Standard**: UI components, utility functions

### Coverage Goals
- Unit tests: >80% for core business logic
- Integration tests: All critical module interactions
- UI tests: Primary user flows (search, navigate, route display)

---

## Unit Test Templates

### Safety Module Template
```kotlin
// File: android/app/src/test/kotlin/com/gemnav/core/safety/SafeModeManagerTest.kt
package com.gemnav.core.safety

import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

class SafeModeManagerTest {
    private lateinit var safeModeManager: SafeModeManager
    
    @BeforeEach
    fun setup() {
        safeModeManager = SafeModeManager()
    }
    
    @Test
    fun `test safe mode activation when criteria met`() {
        // Arrange
        val context = mockk<Context>(relaxed = true)
        
        // Act
        val result = safeModeManager.evaluateSafeMode(context)
        
        // Assert
        assertTrue(result.isActivated)
        assertEquals("Expected reason", result.reason)
    }
    
    @Test
    fun `test safe mode deactivation when conditions clear`() {
        // Test implementation
    }
}
```

### Utils Module Template
```kotlin
// File: android/app/src/test/kotlin/com/gemnav/core/utils/RouteCorridorTest.kt
package com.gemnav.core.utils

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class RouteCorridorTest {
    @Test
    fun `test route corridor calculation with valid points`() {
        // Arrange
        val startPoint = LatLng(37.7749, -122.4194)
        val endPoint = LatLng(34.0522, -118.2437)
        
        // Act
        val corridor = RouteCorridor.calculate(startPoint, endPoint, radiusMeters = 500)
        
        // Assert
        assertNotNull(corridor)
        assertTrue(corridor.contains(startPoint))
        assertTrue(corridor.contains(endPoint))
    }
}
```

### Data Module Template
```kotlin
// File: android/app/src/test/kotlin/com/gemnav/data/maps/DirectionsModelsTest.kt
package com.gemnav.data.maps

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class DirectionsModelsTest {
    @Test
    fun `test DirectionsResponse parsing from JSON`() {
        // Arrange
        val json = """{"routes": [], "status": "OK"}"""
        
        // Act
        val response = DirectionsResponse.fromJson(json)
        
        // Assert
        assertEquals("OK", response.status)
        assertTrue(response.routes.isEmpty())
    }
}
```

---

## Integration Test Framework

### Module Interaction Tests
```kotlin
// File: android/app/src/androidTest/kotlin/com/gemnav/integration/NavigationFlowTest.kt
package com.gemnav.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationFlowTest {
    @Test
    fun testSearchToRouteFlow() {
        // 1. User enters search query
        // 2. PlacesApiClient returns results
        // 3. User selects destination
        // 4. NavigationEngine calculates route
        // 5. Map displays route
        // Assert: Route visible, navigation ready
    }
    
    @Test
    fun testTierSwitchingFlow() {
        // 1. User on Free tier
        // 2. Upgrades to Plus
        // 3. FeatureGate unlocks maps SDK
        // 4. Route recalculated with Plus features
        // Assert: Correct routing engine active
    }
}
```

### Data Layer Integration
```kotlin
@Test
fun testLocationToNavigationDataFlow() {
    // LocationService → LocationViewModel → NavigationEngine → RouteDetailsViewModel
    // Verify data transformation at each layer
}
```

---

## Bug Report Template

### File: `C:\Users\perso\GemNav\docs\BUG_REPORT_TEMPLATE.md`

```markdown
# Bug Report

**ID**: BUG-####  
**Date**: YYYY-MM-DD  
**Reporter**: [Name]  
**Priority**: [Critical/High/Medium/Low]  
**Status**: [Open/In Progress/Resolved/Closed]

---

## Summary
Brief description of the issue

## Environment
- Device: [Model]
- Android Version: [Version]
- App Version: [Version]
- Tier: [Free/Plus/Pro]

## Steps to Reproduce
1. Step one
2. Step two
3. Step three

## Expected Behavior
What should happen

## Actual Behavior
What actually happens

## Screenshots/Logs
[Attach if applicable]

## Module Affected
- [ ] UI Layer (app/ui/)
- [ ] Data Layer (data/)
- [ ] Core Logic (core/)
- [ ] Safety Module (core/safety/)
- [ ] Utils (core/utils/)
- [ ] HERE SDK Integration
- [ ] Google Maps Integration
- [ ] Gemini AI Integration

## Potential Cause
Initial diagnosis (if known)

## Proposed Fix
Suggested solution (if identified)

## Related Issues
- Links to related bugs or features
```

---

## Test Execution Plan

### Phase 1: Unit Tests (Week 1)
1. Implement safety/ module tests
2. Implement utils/ module tests  
3. Implement data/ module tests
4. Target: >80% coverage

### Phase 2: Integration Tests (Week 2)
1. Navigation flow tests
2. Tier switching tests
3. API integration tests
4. Target: All critical paths covered

### Phase 3: UI Tests (Week 3)
1. Search flow
2. Route display
3. Settings management
4. Target: Primary user flows validated

---

## Testing Tools Setup

### Dependencies (build.gradle.kts)
```kotlin
dependencies {
    // Unit Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
    testImplementation("io.mockk:mockk:1.13.7")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    
    // Android Testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.0")
}
```

### Test Directory Structure
```
android/app/src/
├── test/kotlin/com/gemnav/          # Unit tests
│   ├── core/safety/
│   ├── core/utils/
│   └── data/
└── androidTest/kotlin/com/gemnav/   # Integration tests
    ├── integration/
    └── ui/
```

---

## Success Criteria

✅ All test templates created  
✅ Testing framework dependencies added  
✅ Bug report template available  
✅ Test execution plan defined  
✅ Coverage goals established  

**NEXT**: MP-010 (Implementation of unit tests for safety/ module)
