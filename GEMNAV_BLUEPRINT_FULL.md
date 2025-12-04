# GEMNAV BLUEPRINT – FINAL TIER ARCHITECTURE (OPTION A: HARD SEPARATION)

## 1. GEMNAV TIER SYSTEM (FINAL)

This document defines the authoritative tier separation between Free, Basic, and Pro versions of GemNav. All routing engines, map tiles, search providers, and AI integrations must follow this separation exactly.

---

# 🟢 FREE TIER – "Gemini + Google Maps Funnel Mode"
No app. No API keys. No backend.

Free tier operates entirely through:
- Android Gemini assistant
- Google Maps app (user's device)

### Capabilities:
- Gemini answers navigation questions
- Gemini launches Google Maps deep-links
- Gemini promotes GemNav Basic and Pro upgrades

### Technical:
- Zero API keys required
- Zero custom Android code
- Only:
  - Gemini prompt design
  - Google Maps deep link templates
  - Upgrade CTA script

---

# 🔵 BASIC TIER – "GemNav Basic" (Google Everything)
GemNav Basic exclusively uses the Google stack.

### VISUALS:
- Google Maps SDK (Android)

### ROUTING:
- Google Directions API

### SEARCH / POI:
- Google Places API

### AI:
- Gemini API

### Required API Keys:
- GOOGLE_MAPS_API_KEY
- GOOGLE_DIRECTIONS_API_KEY
- GOOGLE_PLACES_API_KEY
- GEMINI_API_KEY

### Purpose:
- Entry-level navigation
- General drivers and light trucks
- Upsell path into GemNav Pro

---

# 🔴 PRO TIER – "GemNav Pro / GeoTruck Maps" (HERE Everything)
GemNav Pro exclusively uses HERE APIs and HERE map tiles.

### VISUALS:
- HERE SDK Map Tiles / Vector Tiles

### TRUCK-SAFE ROUTING:
- HERE Routing API (Truck Mode)
  - Height
  - Weight
  - Length
  - Axles
  - Hazmat
  - Low clearance avoidance
  - Weight restrictions
  - No-truck routes

### SEARCH / GEOCODING:
- HERE Geocoding & Search API

### AI:
- Gemini API with truck-aware intent parsing

### Required API Keys:
- HERE_ROUTING_API_KEY
- HERE_MAP_TILES_API_KEY
- HERE_VECTOR_TILES_API_KEY
- (Optional) HERE SDK License Files
- GEMINI_API_KEY (shared with Basic)

---

# 🟥 PRO TIER TRIAL RULE

### 7-Day GemNav Pro Trial:
- Unlocks **Pro tier only**
- Does **not** include Basic features during trial
- Shows full HERE stack and all truck features

### After Trial:
- If user subscribes → Unlock:
  - GemNav Pro (HERE)
  - GemNav Basic (Google)
- If user does not subscribe:
  - Downgrade to Basic (Google stack)

---

# 🧱 ARCHITECTURE ENFORCEMENT RULES

### Basic = Google stack
- No HERE routing in Basic
- No HERE map tiles in Basic
- No HERE search in Basic

### Pro = HERE stack
- No Google Directions in Pro
- No Google map tiles in Pro
- No Google Places API in Pro

### Gemini is shared
- With different prompts and contexts per tier

---

# END OF BLUEPRINT
