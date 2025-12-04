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
- Gemini promotes GemNav Pro trial

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

### Access Rule:
**Basic tier is NOT available as standalone subscription.**
Basic features are ONLY accessible as part of a paid Pro subscription.

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

# 🟥 PRO TIER TRIAL & SUBSCRIPTION RULES

## 7-Day GemNav Pro Trial:
- Unlocks **Pro tier features ONLY** (HERE + Gemini)
- Does **NOT** include Basic features during trial
- Shows full HERE stack and all truck features
- User sees truck routing exclusively

## After 7-Day Trial Expires:

### If User Subscribes to Paid Pro:
**Unlock BOTH tiers:**
- ✅ GemNav Basic (Google Maps SDK, Directions, Places, Gemini)
- ✅ GemNav Pro (HERE SDK, Truck Routing, HERE Search, Gemini)
- User can switch between Google routing and HERE truck routing
- Full access to entire app feature set

### If User Does NOT Subscribe:
**Downgrade to FREE tier:**
- ❌ Lose ALL app access (both Basic and Pro)
- ❌ No Google Maps SDK features
- ❌ No HERE SDK features
- ✅ Revert to Gemini + Google Maps app only (Free tier funnel)
- User must start new trial or subscribe to regain app access

---

# 🧱 ARCHITECTURE ENFORCEMENT RULES

## Tier Separation

### Free = No custom app
- Gemini assistant + Google Maps deep links only
- No API integrations

### Basic = Google stack (Paid Pro subscription only)
- No HERE routing in Basic
- No HERE map tiles in Basic
- No HERE search in Basic
- Basic NOT available as standalone purchase

### Pro Trial = HERE stack only (7 days)
- No Google Directions during trial
- No Google map tiles during trial
- No Google Places API during trial
- HERE + Gemini only

### Pro Paid = Google + HERE stacks (full access)
- Access to both Basic AND Pro features
- Can toggle between Google and HERE routing
- Full feature set unlocked

### Gemini AI = Shared across all tiers
- Different prompts and contexts per tier
- Nano for Free tier
- Cloud for trial and paid users

---

# 📋 SUBSCRIPTION LOGIC SUMMARY

```
User Journey:

FREE (start) 
  → Gemini assistant only
  → No app access

↓ (starts trial)

PRO TRIAL (7 days)
  → HERE + Gemini only
  → No Google features
  
↓ (trial expires)

PAID PRO SUBSCRIPTION?
  ├─ YES → Basic + Pro features (Google + HERE + Gemini)
  └─ NO  → Revert to FREE (lose all app access)
```

**Critical Rule:** There is NO path to Basic tier without paying for Pro subscription. Basic is always bundled with Pro in paid subscriptions only.

---

# END OF BLUEPRINT
