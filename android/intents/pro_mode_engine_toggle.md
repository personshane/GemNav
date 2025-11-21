# Pro Mode Routing Engine Toggle

## Toggle Mechanism
User can switch between:
- HERE SDK (truck routing)
- Google Maps (car routing)

## Implementation Requirements
- Clear UI indicator of active engine
- Separate route calculation pipelines
- No data mixing between engines
- Route recalculation on toggle

## Use Cases
- Truck driver needs car route for personal trip
- Compare truck vs car route options
- Fallback if HERE route unavailable

## Legal Compliance
- Display only HERE data with HERE routes
- Display only Google data with Google routes
- No overlay or mixing

---
*To be populated in future micro-projects*
