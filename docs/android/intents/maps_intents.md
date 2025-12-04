# Google Maps Intents (Android)

## Intent URI Formats

| Intent Type | URI Scheme | Parameters | Example |
|-------------|-----------|------------|---------|
| Navigation | `google.navigation:q=` | `q` (destination), `mode` (d/w/b) | `google.navigation:q=1600+Amphitheatre+Parkway,+Mountain+View,+CA` |
| Search | `geo:0,0?q=` | `q` (search query) | `geo:0,0?q=restaurants+near+me` |
| Coordinates | `geo:` | `lat,lon` | `geo:37.7749,-122.4194` |
| Directions | `https://www.google.com/maps/dir/?api=1` | `origin`, `destination`, `travelmode` | `https://www.google.com/maps/dir/?api=1&destination=LAX` |
| Place Details | `https://www.google.com/maps/search/?api=1` | `query`, `query_place_id` | `https://www.google.com/maps/search/?api=1&query=coffee` |
| Street View | `google.streetview:cbll=` | `cbll` (lat,lon), `cbp` (heading,pitch,zoom) | `google.streetview:cbll=46.414382,10.013988` |

## Navigation Modes

| Mode | Parameter | Description |
|------|-----------|-------------|
| Driving | `mode=d` | Car navigation (default) |
| Walking | `mode=w` | Pedestrian navigation |
| Bicycling | `mode=b` | Bike navigation |
| Transit | `mode=r` | Public transit |

## Android Intent Flags

```kotlin
Intent.FLAG_ACTIVITY_NEW_TASK
Intent.FLAG_ACTIVITY_CLEAR_TOP
```

**Rationale**: Launch Maps in new task to prevent GemNav from being removed from recents.

## Required Permissions

| Permission | Purpose | Request Timing |
|------------|---------|----------------|
| `ACCESS_FINE_LOCATION` | Current location for "navigate from here" | Runtime, before intent |
| None for basic intents | Launching Maps requires no special permissions | N/A |

## Device Compatibility

| Requirement | Minimum | Notes |
|-------------|---------|-------|
| Android Version | 8.0 (API 26) | Intent support stable from API 26+ |
| Google Maps App | Latest version recommended | Fallback to Play Store if missing |

## Error Handling

### Maps App Not Installed
```kotlin
val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
if (intent.resolveActivity(packageManager) != null) {
    startActivity(intent)
} else {
    // Redirect to Play Store
    val playStoreIntent = Intent(Intent.ACTION_VIEW,
        Uri.parse("market://details?id=com.google.android.apps.maps"))
    startActivity(playStoreIntent)
}
```

### Invalid Location
- **Symptom**: Gemini Nano returns ambiguous or unparseable location
- **Fallback**: Show disambiguation UI, offer search suggestions

### Malformed URI
- **Symptom**: URI construction fails
- **Fallback**: Use generic search intent with raw user input

### No Network (Offline)
- **Impact**: Search and place details may fail in Maps
- **Mitigation**: Clearly communicate to user that online features require connection

## Implementation Notes

**Intent Construction**:
- URL-encode all parameters
- Use `+` for spaces in addresses
- Validate coordinates before constructing `geo:` URIs

**Testing Strategy**:
- Test with Maps app installed and uninstalled
- Test with airplane mode (offline)
- Test with invalid coordinates (e.g., lat > 90)
- Test with special characters in queries

---

**Last Updated**: MP-003
