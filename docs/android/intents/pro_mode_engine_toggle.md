# Pro Mode Routing Engine Toggle

**Status**: Phase 2 Feature (Not in MVP)

## Overview

Pro tier users can toggle between two routing engines:
- **HERE SDK**: Commercial truck routing with legal compliance
- **Google Maps**: Standard car routing

## Toggle Mechanism (Phase 2)

| Engine | Use Case | Map Rendering |
|--------|----------|---------------|
| HERE SDK | Truck routing, commercial vehicles | HERE MapView only |
| Google Maps | Personal car routing | Google Maps SDK only |

## Implementation Requirements (Phase 2)

- Clear UI indicator of active engine
- Separate route calculation pipelines
- No data mixing between engines
- Route recalculation on toggle
- Persist user preference

## Legal Compliance

**CRITICAL CONSTRAINT**: NEVER mix HERE route data with Google Maps tiles/UI.

When HERE routing active:
- Use HERE SDK MapView exclusively
- Display HERE attribution
- Render route polylines with HERE SDK

When Google routing active:
- Use Google Maps SDK exclusively
- Display Google attribution
- Render route polylines with Maps SDK

## Use Cases (Phase 2)

1. Truck driver switches to car mode for personal errands
2. Compare truck route vs car route
3. Fallback if HERE route unavailable in region

## Technical Architecture (Placeholder)

```
[User Toggle] → [Engine Selector] → [Route Calculator] → [Map Renderer]
                        ↓
                [HERE or Google]
```

**Implementation Details**: To be defined in MP-005 (Pro Tier HERE SDK Setup)

---

**Last Updated**: MP-003 (Placeholder only)  
**Implementation**: Deferred to Phase 2
