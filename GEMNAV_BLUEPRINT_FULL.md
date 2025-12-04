# GEMNAV – FULL BLUEPRINT (FREE / BASIC / PRO)

**Source of Truth for GemNav Project**

---

## 1. PRODUCT LAYERS & TIERS

### 1.1 Overall Concept

**GemNav** is a 3-tier navigation + AI copilot system focused on drivers, with Pro specifically targeting truckers under the **GeoTruck Maps** branding.

* **Free** – No app. Uses Android **Gemini + Google Maps app** as a funnel.
* **Basic** – Your **own Android app** using **Google Maps SDK + Directions API + Gemini API**.
* **Pro** – Basic + **HERE SDK/Truck Routing**, full truck-centric AI and routing.

---

### 1.2 Tiers: Responsibilities and Boundaries

#### 🟢 FREE – "Gemini + Maps Funnel Mode"

**User surface:**
* Android Gemini (chat/voice)
* Google Maps app

**What Free does:**
* Gemini answers navigation questions.
* Gemini constructs deep links to Google Maps for routes.
* Gemini pitches **GemNav Basic/Pro** as the better experience.
* No GemNav app installed yet.

**Core behavior:**
* Example user: "How do I get from Phoenix to Dallas?"
* Gemini:
  * Figures out origin/destination.
  * Generates a Google Maps deeplink:
    `https://www.google.com/maps/dir/?api=1&origin=...&destination=...`
  * Optionally says: "If you want smarter routing, AI copilot and truck-safe paths, try the GemNav app (Basic/Pro)."

**Required artifacts:**
* Gemini prompt design that:
  * Detects when to recommend GemNav.
  * Explains value props of Basic vs Pro.
* Documentation for:
  * Deeplink formats
  * Upgrade CTA wording

No codebase required here, beyond:
* A **GemNav marketing page / Play Store listing** Gemini can link to.

---

#### 🔵 BASIC – "GemNav App with Gemini + Google Maps (Cars/Light Trucks)"

**User surface:**
* **Android app: GemNav Basic**

**Capabilities:**
* Map display using **Google Maps SDK**.
* Routing using **Google Directions API**.
* Search / autocomplete using **Places API**.
* AI assistant using **Gemini API**.
* Focus: cars + light trucks (non-compliance-critical routing).

**Use cases (Basic):**
* "Plan a route from A to B."
* "Find fuel, rest stops, or food along my route."
* "Avoid tolls or certain areas."
* "Explain traffic or delays."
* "Summarize my trip."

**Deliberate non-capabilities in Basic:**
* No guaranteed truck-safe routing.
* No detailed clearance/weight/hazmat enforcement.
* No HERE integration.

Basic is the **gateway app**: you own the UI, the experience, and can upsell Pro.

---

#### 🔴 PRO – "GemNav Pro / GeoTruck Maps – HERE Truck Routing + AI Truck Copilot"

**User surface:**
* **GemNav app with Pro unlocked** (same app as Basic, extra features enabled via subscription/flag).

**Capabilities (over Basic):**
* Truck Profile:
  * Height, length, width
  * Gross weight, axle count
  * Trailer count
  * Hazmat flags
* Truck-safe routing using **HERE Truck Routing**:
  * Low bridge avoidance
  * Weight restrictions
  * Hazmat restrictions
  * Scenic/residential avoidance where needed
* **SafeMode**:
  * Persistent truck profile banner
  * List of upcoming restrictions
  * Preemptive alerts (low bridge, restricted road, etc.)
* **Truck AI Copilot**:
  * "Find parking in ~30–60 minutes up ahead."
  * "Explain why we diverted."
  * "Plan a truck-safe route for tomorrow given my preferences."

Pro is the **GeoTruck Maps** identity.

---

## 2. KEYS & THIRD-PARTY SERVICES

### 2.1 Google Maps / Places / Directions

* **Google Cloud Project**: `gemnav-<env>`
* APIs to enable:
  * Maps SDK for Android
  * Directions API
  * Places API (Text Search, Autocomplete)
  * Geocoding API (optional)
* Store keys in:
  * Android `local.properties` or secure Gradle variables.

---

### 2.2 Gemini API

* Project + API key at **ai.google.dev** (or through Google Cloud if using unified project).
* Scopes:
  * Text / chat completion models (Gemini 1.5 / 2.0 depending on availability).
* Integration:
  * Direct from Android app via HTTPS (recommended: via your backend to hide key and centralize usage).
  * Use structured output / tools mode for nice JSON intents.

---

### 2.3 HERE API (Pro)

* **HERE Developer Account**
* SDK/API credentials:
  * HERE SDK for Android (Navigation / Map rendering if you don't fully stick to Google tiles).
  * HERE Routing API with **Truck Routing**.
  * HERE Map Tiles / Vector Map API (optional if you want HERE visuals; otherwise you can keep Google tiles but use HERE only for route calculation).

---

## 3. SYSTEM ARCHITECTURE

### 3.1 High-Level View

* **Free**: No system, just Gemini + Maps (scripts/guides).
* **Basic / Pro**: One Android app with runtime feature flags.

Core components for Basic/Pro:
1. **Android App (GemNav)**
2. **Optional Backend** (for sync, auth, subscriptions, analytics)
3. **3rd-party APIs**:
   * Google Maps / Places / Directions
   * Gemini
   * HERE Routing

---

### 3.2 Android App Architecture

**Language/Framework:**
* Kotlin + Jetpack Compose

**Modules:**
```text
/app                 // entry, DI, global nav graph
/core                // shared models, networking, utils, config
/feature_home        // Home screen, recent routes, CTA to Pro
/feature_navigation  // Map + routing flow
/feature_ai          // Gemini assistant UI + logic
/feature_truck       // Pro-only: truck profile, SafeMode, restrictions
/feature_settings    // profile, subscription, preferences
```

**State Management:**
* ViewModel + StateFlow
* One ViewModel per feature module.

**Navigation:**
* Jetpack Navigation + Compose.

---

### 3.3 Optional Backend

Initially, you could avoid a backend and:
* Keep everything on-device.
* Use Google / HERE / Gemini directly.

Later backend provides:
* Login & account sync.
* Subscription verification.
* Telemetry.
* Multi-device sync.
* Central AI orchestration (if you bring in Claude/ChatGPT later).

Tech suggestion:
* FastAPI (Python) or Node (NestJS/Express)
* Postgres or Firestore as DB.

---

## 4. FEATURE BREAKDOWN BY TIER

### 4.1 Free Tier Feature Spec

**No code in your repo** – but you should define:
* Gemini prompts that:
  * Understand navigation questions.
  * Use:
    * Address extraction
    * Intent detection
  * Respond with:
    * Basic navigation answer
    * A CTA like: "For smarter routes and AI copilot, install GemNav Basic/Pro."
* Deeplink patterns:
  * Straight route:
    `https://www.google.com/maps/dir/?api=1&origin=LAT,LON&destination=LAT,LON`
  * Route with waypoints:
    `...&waypoints=LAT1,LON1|LAT2,LON2`

---

### 4.2 Basic Tier – App Feature Spec

#### Screens

1. **HomeScreen**
   * Recent routes list
   * "Start Route" button
   * "Ask GemNav AI" button
   * Soft-banner upsell to Pro (truck features preview)

2. **SearchScreen**
   * Origin input (optional default: current location)
   * Destination input
   * Autocomplete via Places API
   * Route preference toggles:
     * avoid tolls
     * avoid highways
   * Buttons:
     * "Get Directions"
     * "Ask AI to plan"

3. **NavigationScreen**
   * Map with route displayed.
   * Step-list / next turn card.
   * "Ask AI about this route" button.

4. **AiAssistantOverlay**
   * Chat UI with Gemini.
   * Quick suggestions:
     * "Find fuel ahead"
     * "Find rest area"
     * "Explain delay"
   * Gemini returns structured responses used to:
     * Show POIs along route
     * Adjust route preferences
     * Open alternate routes

5. **SettingsScreen**
   * Basic preferences
   * "Upgrade to Pro" CTA

#### Routing (Basic)

* Use Google Directions API:
  * Modes: driving
  * Options: avoid tolls / highways
* Error handling:
  * No network
  * Invalid address
* Store:
  * Trip history locally (Room DB or simple file).

#### AI (Basic)

* All AI calls go through Gemini:
  * Input: user text + context (current route, ETA, etc.)
  * Output: either natural language or structured JSON (tool calling).

---

### 4.3 Pro Tier – Extra Feature Spec

Pro reuses all Basic screens and adds:

#### TruckProfileScreen
* Fields:
  * vehicle height, length, width
  * gross weight, axle count
  * hazmat:true/false
  * trailer count
* Stored locally, and optionally synced.

#### SafeMode Banner (always visible in Pro)
* Shows:
  * "Truck: 13'6", 80K lbs, Reefer, Hazmat: No"
* Tap opens TruckProfileScreen.

#### RestrictionsOverlay
* Panel showing upcoming route restrictions:
  * low bridge ahead in X miles
  * weight limit road in Y miles
* Uses HERE route metadata.

#### Pro Navigation Enhancements

* Use HERE for route calculation when:
  * Truck profile exists
  * Pro tier is active
* Google Maps can still render visually (or you may choose HERE visuals; this is a design decision).

**Routing logic in Pro:**

```kotlin
if (userTier == PRO && truckProfile != null) {
    route = hereRoutingService.getTruckRoute(
        origin,
        destination,
        truckProfile,
        preferences
    )
} else {
    route = googleRoutingService.getStandardRoute(
        origin,
        destination,
        preferences
    )
}
```

#### AI (Pro)

Gemini has extra context:
* Truck profile
* Route restrictions
* History of deviations

Example requests:
* "Why are we avoiding this road?"
* "Where can I safely park in the next 45 minutes?"
* "Warn me if I'm approaching a low bridge I don't fit under."

---

## 5. DATA MODELS (ON-DEVICE / BACKEND-READY)

```kotlin
data class User(
    val id: String,
    val tier: Tier,
    val createdAt: Long
)

enum class Tier { FREE, BASIC, PRO }

data class TruckProfile(
    val heightMeters: Double,
    val lengthMeters: Double,
    val widthMeters: Double,
    val grossWeightKg: Double,
    val axleCount: Int,
    val hazmat: Boolean,
    val trailerCount: Int
)

data class RouteRequest(
    val origin: LatLng,
    val destination: LatLng,
    val waypoints: List<LatLng> = emptyList(),
    val avoidTolls: Boolean = false,
    val avoidHighways: Boolean = false,
    val useTruckRouting: Boolean = false
)

data class RouteResult(
    val polyline: String,
    val distanceMeters: Int,
    val durationSeconds: Int,
    val restrictions: List<RouteRestriction> = emptyList()
)

data class RouteRestriction(
    val type: RestrictionType,
    val location: LatLng,
    val description: String
)

enum class RestrictionType {
    LOW_CLEARANCE,
    WEIGHT_LIMIT,
    HAZMAT_RESTRICTION,
    TRUCK_FORBIDDEN,
    OTHER
}
```

---

## 6. AI INTENT SCHEMA (GEMINI)

Define a common intent schema Gemini should output:

```json
{
  "intent": "ROUTE" | "FIND_POI" | "EXPLAIN_ROUTE" | "GENERAL_QA",
  "route": {
    "origin": "string or 'current_location'",
    "destination": "string",
    "preferences": {
      "avoid_tolls": true,
      "avoid_highways": false,
      "use_truck_profile": true
    }
  },
  "poi": {
    "type": "fuel" | "parking" | "rest_area" | "food",
    "time_window_minutes": 60
  },
  "meta": {
    "text_response": "Fallback narrative answer if needed"
  }
}
```

---

## 7. SECURITY & KEY HANDLING

* Store API keys in:
  * Android: `local.properties` / encrypted config.
  * Backend: environment variables.
* Never hardcode keys in repo.
* For production:
  * Use your backend as a proxy for Google/HERE/Gemini to:
    * Control usage
    * Avoid client-side exposure
    * Enforce tier limits per user.

---

## 8. DEVOPS (LITE)

For now, minimal:
* Single repo: `gemnav-android`
* Branching:
  * `main` – stable
  * `dev` – active
  * feature branches: `feature/basic-mvp`, `feature/pro-routing`
* CI:
  * GitHub Actions:
    * Build on PR
    * Run unit tests
* Play Store:
  * Internal testing track → closed test → production.

---

## 9. MVP CUTS

* **MVP 1 – Basic App**
  * Home, Search, Navigation
  * Google Maps + Directions
  * Basic Gemini commands

* **MVP 2 – Pro Core**
  * TruckProfileScreen
  * HERE truck routing
  * SafeMode banner (minimal version)

* **MVP 3 – Pro AI**
  * AI Truck Copilot features
  * Restrictions overlay
  * Parking/POI ahead logic

---

**LAST UPDATED:** 2025-12-03
**AUTHORITATIVE BLUEPRINT**
