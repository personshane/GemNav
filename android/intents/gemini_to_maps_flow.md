# Gemini to Maps Flow (Free Tier)

## System Architecture

```
[User Input] → [Gemini Nano] → [Intent Builder] → [Google Maps App]
     ↓              ↓                 ↓                    ↓
  Voice/Text   On-Device AI    URI Construction    External Navigation
```

## Detailed Flow

| Step | Component | Action | Output |
|------|-----------|--------|--------|
| 1 | User | Voice/text input | Raw natural language |
| 2 | GemNav | Capture input | Audio/text data |
| 3 | Gemini Nano | Parse intent on-device | Structured intent object |
| 4 | Intent Builder | Construct URI | Valid Maps intent URI |
| 5 | Android System | Launch intent | Opens Google Maps |
| 6 | Google Maps | Process navigation | User navigates externally |

## Gemini Nano Integration

### Input Processing

**Voice Input**:
```
User says: "Navigate to the nearest coffee shop"
→ Speech-to-text (Android SpeechRecognizer)
→ Text: "Navigate to the nearest coffee shop"
```

**Text Input**:
```
User types: "Take me to 1600 Amphitheatre Parkway"
→ Direct text: "Take me to 1600 Amphitheatre Parkway"
```

### Gemini Nano Prompt Template

```
You are a navigation intent parser. Extract structured data from user requests.

User input: "{user_input}"

Extract:
- action: (navigate, search, directions, place_details)
- destination: (address, place name, or coordinates)
- mode: (driving, walking, bicycling, transit) [optional]
- origin: (current location or specific address) [optional]

Respond ONLY with JSON:
{
  "action": "navigate",
  "destination": "1600 Amphitheatre Parkway, Mountain View, CA",
  "mode": "driving"
}
```

### Intent Parsing Examples

| User Input | Gemini Output | Intent URI |
|------------|---------------|------------|
| "Navigate to LAX" | `{action: "navigate", destination: "LAX"}` | `google.navigation:q=LAX` |
| "Find coffee near me" | `{action: "search", destination: "coffee near me"}` | `geo:0,0?q=coffee+near+me` |
| "Walk to Central Park" | `{action: "navigate", destination: "Central Park", mode: "walking"}` | `google.navigation:q=Central+Park&mode=w` |
| "37.7749, -122.4194" | `{action: "navigate", destination: "37.7749,-122.4194"}` | `geo:37.7749,-122.4194` |

## Intent Builder Logic

```kotlin
fun buildIntent(nanoResponse: GeminiResponse): Intent {
    val uri = when (nanoResponse.action) {
        "navigate" -> buildNavigationUri(nanoResponse)
        "search" -> buildSearchUri(nanoResponse)
        "directions" -> buildDirectionsUri(nanoResponse)
        "place_details" -> buildPlaceUri(nanoResponse)
        else -> buildFallbackUri(nanoResponse)
    }
    
    return Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
        setPackage("com.google.android.apps.maps")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
}
```

## Data Flow & Privacy

**On-Device Processing**:
- All Gemini Nano processing happens locally
- No user input sent to cloud
- No PII leaves device
- Intent URIs contain only navigation parameters

**Data Retention**:
- Recent destinations stored locally (last 10)
- No cloud sync in Free tier
- User can clear history anytime

## Error Handling

### Ambiguous Input

**Problem**: "Take me home"  
**Challenge**: Gemini doesn't know user's home address  
**Solution**:
1. Check local storage for saved "Home" location
2. If not found, prompt user to set Home address
3. Cache for future requests

### Invalid Destination

**Problem**: User says "Take me to nowhere"  
**Gemini Output**: `{action: "navigate", destination: "nowhere"}`  
**Solution**:
1. Validate destination before launching intent
2. If validation fails, show error: "Couldn't understand destination"
3. Offer voice input retry

### Location Permission Denied

**Problem**: User denies location permission  
**Impact**: "Near me" searches fail  
**Solution**:
1. Detect permission state
2. Fallback to generic search without location
3. Educate user on permission benefit

### Network Unavailable (Offline Mode)

**Problem**: Device offline during voice input  
**Impact**: Speech-to-text may fail on some devices  
**Solution**:
1. Use on-device speech recognition (if available)
2. Fallback to text input prompt
3. Cache request and retry when online (optional)

## Fallback Strategies

| Failure | Fallback Action |
|---------|-----------------|
| Gemini Nano unavailable | Use simple keyword matching (navigate, search, go) |
| Intent construction fails | Use generic search: `geo:0,0?q={raw_input}` |
| Maps app not installed | Redirect to Play Store |
| Parsing timeout (>5s) | Show "Processing took too long" error |

## Performance Requirements

| Metric | Target | Notes |
|--------|--------|-------|
| Voice to intent | <1s | Including Gemini processing |
| Text to intent | <500ms | Gemini processing only |
| Intent launch | Instant | Android system handles |

## Testing Scenarios

1. **Basic navigation**: "Take me to Times Square"
2. **Search query**: "Find gas stations"
3. **Coordinates**: "Navigate to 40.7589,-73.9851"
4. **Mode selection**: "Walk to the park"
5. **Ambiguous input**: "Go there" (should fail gracefully)
6. **No Maps app**: Simulate uninstalled app
7. **Offline mode**: Test without network
8. **Permission denied**: Test without location access

---

**Last Updated**: MP-003
