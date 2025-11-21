# GemNav Product Requirements Document (PRD)

**Version**: 1.0  
**Date**: 2025-11-21  
**Status**: Draft  
**Owner**: GemNav Product Team

---

## 1. Executive Summary

GemNav is a multi-tier AI-powered navigation application that combines Gemini AI with Google Maps and HERE SDK to deliver conversational, intelligent routing for consumers and commercial drivers.

**MVP Strategy**: Android-first launch with Free and Plus tiers, followed by Pro tier and iOS support in Phase 2.

**Target Markets**: United States and Canada (Mexico optional for future expansion).

**Age Rating**: 12+

**Revenue Model**: Freemium with subscription tiers (Plus: $4.99/mo, Pro: $14.99/mo with 7-day trial).

---

## 2. Product Vision

Create the most natural, conversational navigation experience by combining Gemini AI with best-in-class routing engines, serving everyone from daily commuters to professional truck drivers through a seamless, privacy-respecting interface.

---

## 3. Tier Structure & Features

### 3.1 Free Tier

**Target Users**: General consumers, everyday drivers, commuters, hands-free navigation users

**Core Value Proposition**: AI-powered navigation with privacy-first on-device processing, zero cost, zero ads

**Features**:
- Gemini Nano on-device AI processing
- Natural language input (voice and text)
- Google Maps app integration via intents (Android) / URL schemes (iOS)
- Basic voice commands
- Recent destinations (stored locally)
- Offline-capable AI processing
- Privacy-first (no cloud AI calls)
**Monetization**: Free (no ads, no IAP)

**Technical Stack**:
- Android: Gemini Nano SDK (on-device)
- iOS: Gemini Nano SDK (when available on iOS)
- Google Maps app (external, via intents/URL schemes)
- No cloud API dependencies

### 3.2 Plus Tier

**Target Users**: Power users, frequent travelers, long-distance drivers, non-truck professionals (sales reps, delivery drivers, rideshare drivers)

**Core Value Proposition**: Enhanced AI reasoning with in-app routing, multi-stop optimization, real-time traffic, and advanced features

**Features**:
- All Free tier features PLUS:
- Gemini Cloud API (enhanced AI with larger context)
- Google Maps SDK (in-app navigation and rendering)
- Multi-waypoint routing (up to 10 stops)
- Real-time traffic integration
- Alternative routes with AI-generated reasoning
- Search and Places API integration
- Route history and favorites (cloud sync)
- ETA sharing with contacts
- Offline maps (cached regions)
- Voice-guided turn-by-turn navigation
- Advanced voice commands and conversation
- Route optimization (fastest, shortest, eco-friendly)
- Parking suggestions with availability
- Gas station finder with real-time prices
- Weather overlay on routes
- Speed limit warnings
- Lane guidance

**Monetization**:
- $4.99/month (billed monthly)
- $29.99/year (billed annually, save 50%)
- No free trial
- Promo codes supported
- Introductory pricing supported

**Technical Stack**:
- Android: Gemini Cloud API, Google Maps SDK, Places API
- iOS: Gemini Cloud API, Google Maps SDK, Places API
- Cloud-based AI processing
- Real-time data sync

### 3.3 Pro Tier

**Target Users**: Individual truck drivers, owner-operators, small fleets (1-10 trucks)

**Core Value Proposition**: Commercial truck routing with legal compliance for height, weight, hazmat restrictions, and optimized commercial routes

**Features**:
- All Plus tier features PLUS:
- HERE SDK commercial truck routing
- Legal compliance checking:
  * Height restrictions (bridges, tunnels, overpasses)
  * Weight limits (bridges, roads)
  * Axle count restrictions
  * Hazmat routing and restrictions
  * Tunnel restrictions and prohibitions
  * Bridge clearance warnings
  * Road type restrictions (residential, unpaved)
- Vehicle profile configuration (dimensions, weight, cargo type)
- Routing engine toggle: Commercial Vehicle Mode (HERE) OR Standard Car Mode (Google Maps)
- Commercial POI database (truck stops, weigh stations, rest areas)
- Route cost estimation (tolls, fuel, time)
- Hours of Service (HOS) tracking integration (Phase 2 feature)
- Separate map rendering pipelines (HERE or Google, never mixed)

**Monetization**:
- $14.99/month (billed monthly)
- $99/year (billed annually)
- **7-day free trial** (auto-converts to paid unless canceled)
- One trial per user/account/device (enforced)
- Promo codes supported
- Introductory pricing supported

**Technical Stack**:
- Android: Gemini Cloud API, HERE SDK (truck routing), Google Maps SDK (car mode)
- iOS: Gemini Cloud API, HERE SDK (truck routing), Google Maps SDK (car mode)
- Dual rendering pipelines (HERE MapView OR Google MapView, never both)
- Cloud-based AI processing

**CRITICAL LEGAL CONSTRAINT**: NEVER mix HERE route data with Google Maps tiles/UI. When HERE routing is active, only HERE SDK map rendering is used. When Google routing is active, only Google Maps SDK rendering is used. No data overlay or mixing permitted.

---

## 4. Monetization & Billing Requirements

### 4.1 Payment Integration

**Android (Phase 1 MVP)**:
- Google Play Billing Library (required)
- Google Pay support (where available)
- Credit/debit card via Google Play payment sheet
- Support for promo codes
- Support for introductory pricing
- Support for discounted offers

**iOS (Phase 2)**:
- Apple In-App Purchases via StoreKit 2
- Apple Pay support
- Promo codes via App Store Connect
- Introductory pricing via App Store offers

**Alternative Payment (Optional)**:
- Stripe integration for direct card payments (if permitted by platform policies)

### 4.2 Subscription Management

**In-App Capabilities**:
- Upgrade from Free to Plus or Pro
- Upgrade from Plus to Pro
- Downgrade from Pro to Plus or Free
- Cancel subscription (retains access until period ends)
- Restore purchases (cross-device sync)
- View subscription status and renewal date
- Manage billing through platform settings

**Real-Time Entitlement Management**:
- Billing status detected in real-time via platform APIs
- Features locked/unlocked instantly on status change
- Seamless tier transitions (no app restart required)
- Grace period handling (temporary access during billing issues)
- Billing retry logic for failed payments
- Clear UI indicators for subscription status

**Trial Enforcement**:
- Pro tier: 7-day free trial (one per user/account/device)
- Trial tracking via user account ID + device fingerprint
- Auto-conversion to paid subscription unless canceled
- Clear trial countdown displayed in UI
- Trial cancellation allowed at any time during trial period

**Edge Cases**:
- Handle subscription expiration gracefully
- Handle refunds (revert to Free tier)
- Handle family sharing (per platform rules)
- Handle account deletion (cancel subscriptions)

---

## 5. Technical Requirements

### 5.1 Android Requirements (Phase 1 MVP)

**Minimum SDK**: Android 8.0 (API 26)  
**Target SDK**: Android 14 (API 34)

**Core Dependencies**:
- Gemini Nano SDK (on-device AI)
- Gemini Cloud API (cloud AI for Plus/Pro)
- Google Maps SDK for Android
- Google Places API
- Google Play Billing Library
- HERE SDK for Android (Pro tier only)

**Architecture**:
- Native Android (Kotlin)
- MVVM architecture
- Jetpack Compose UI
- Room database (local persistence)
- WorkManager (background tasks)
- Coroutines and Flow

**Performance Requirements**:
- App launch: <2 seconds cold start
- AI response: <500ms for Free tier, <1s for Plus/Pro
- Route calculation: <3 seconds
- Map rendering: 60fps smooth scrolling
- Memory usage: <200MB average

**Battery Requirements**:
- Background location: <5% battery drain per hour
- Foreground navigation: <15% battery drain per hour
- Efficient wake locks and location updates

### 5.2 iOS Requirements (Phase 2)

**Minimum Version**: iOS 15.0  
**Target Version**: iOS 18

**Core Dependencies**:
- Gemini Nano SDK (when available on iOS)
- Gemini Cloud API
- Google Maps SDK for iOS
- Google Places API
- StoreKit 2 (IAP)
- HERE SDK for iOS (Pro tier)

**Architecture**:
- Native iOS (Swift)
- MVVM architecture
- SwiftUI
- Core Data (local persistence)
- Combine framework

**Performance Requirements**: Same as Android

### 5.3 Google Maps Integration

**Free Tier (Intent-Based)**:
- Android: Use Google Maps intents (`geo:`, `google.navigation:`)
- iOS: Use Google Maps URL schemes
- No SDK integration required
- Launches external Google Maps app

**Plus/Pro Tier (SDK-Based)**:
- Google Maps SDK embedded in app
- Full map customization and control
- Polyline rendering for routes
- Custom markers and overlays
- Traffic layer integration

### 5.4 HERE SDK Integration (Pro Tier Only)

**Commercial License Required**: HERE SDK commercial routing license

**Features Used**:
- Truck routing with vehicle profiles
- Route calculation with restrictions
- Map rendering (separate from Google Maps)
- POI search for commercial locations

**Critical Rule**: HERE SDK map rendering and Google Maps SDK rendering must NEVER be used simultaneously. App must use one or the other based on routing engine selection.

---

## 6. Legal & Compliance Requirements

### 6.1 Google Maps Terms of Service

- Comply with Google Maps Platform Terms of Service
- Display required Google attribution
- Intent-based usage permitted for Free tier
- SDK usage requires API key and billing enabled
- No mixing HERE data with Google Maps tiles/UI

### 6.2 HERE SDK Terms

- Commercial routing license required for Pro tier
- Truck routing features under commercial agreement
- Cannot display HERE routes on Google Maps UI
- Separate map rendering required
- HERE attribution displayed when HERE routing active

### 6.3 Data Privacy & GDPR

**Free Tier**:
- On-device processing only
- No personal data sent to cloud
- Local storage of recent destinations
- Minimal data collection

**Plus/Pro Tier**:
- Cloud AI processing (user consent required)
- Route history synced to cloud (encrypted)
- Location data processed for routing
- User account data (email, subscription status)
- Clear privacy policy and consent flow

**User Rights**:
- Access to personal data
- Data deletion on request
- Data export capability
- Opt-out of analytics

### 6.4 App Store Compliance

**Google Play Store (Android)**:
- Age rating: 12+ (navigation with location services)
- Content rating questionnaire completed
- Google Play Billing integration required
- Privacy policy link required
- Data safety form completed
- Location permissions justified in listing

**Apple App Store (iOS - Phase 2)**:
- Age rating: 12+
- StoreKit 2 for IAP
- Privacy nutrition label completed
- App Tracking Transparency compliance
- Location permission justification

**Launch Regions**:
- United States (primary)
- Canada (primary)
- Mexico (optional, Phase 2)

**Restricted Content**: None (navigation app with no controversial content)

---

## 7. Permissions & Privacy

### 7.1 Android Permissions

**Required (All Tiers)**:
- `ACCESS_FINE_LOCATION` (foreground)
- `ACCESS_COARSE_LOCATION` (foreground)
- `INTERNET` (Plus/Pro cloud features)
- `RECORD_AUDIO` (voice input)

**Optional (Plus/Pro)**:
- `ACCESS_BACKGROUND_LOCATION` (navigation in background)
- `POST_NOTIFICATIONS` (route alerts, ETA notifications)

**Requested at Runtime**: Location and microphone permissions

### 7.2 iOS Permissions (Phase 2)

**Required**:
- Location Services (When In Use)
- Microphone (voice input)

**Optional**:
- Location Services (Always) for background navigation
- Notifications (route alerts)

**Privacy Descriptions Required**: Clear, user-friendly explanations for each permission

---

## 8. Phase 1 vs Phase 2 Breakdown

### 8.1 Phase 1 Launch (MVP)

**Platform**: Android only

**Tiers**: Free + Plus

**Timeline**: Initial release (MVP)

**Core Features**:
- Free tier: Gemini Nano + Google Maps intents
- Plus tier: Gemini Cloud + Google Maps SDK in-app
- Voice input (Gemini AI processing)
- Multi-waypoint routing (Plus)
- Real-time traffic integration
- Google Play Billing (subscriptions)
- User accounts (email/Google Sign-In)
- Route history and favorites
- Basic settings and preferences
- Privacy-compliant analytics
- In-app support and FAQ

**Out of Scope for Phase 1**:
- Pro tier (HERE SDK)
- iOS support
- Fleet features
- HOS integration
- Advanced vehicle profiles

### 8.2 Phase 2 Launch

**Platform**: Android + iOS

**Tiers**: Free + Plus + Pro

**Timeline**: Post-MVP (6-12 months after Phase 1)

**New Features**:
- Pro tier with HERE SDK truck routing
- iOS app (Free + Plus + Pro)
- Advanced vehicle profiles (Pro)
- Commercial POI database (Pro)
- Route cost estimation (Pro)
- HOS integration (Pro)
- Fleet features (basic, 1-10 trucks)
- Expanded regions (Mexico, Europe consideration)
- Enhanced analytics and reporting

---

## 9. Success Metrics & KPIs

### 9.1 User Acquisition
- Daily Active Users (DAU)
- Monthly Active Users (MAU)
- User retention (Day 1, Day 7, Day 30)
- App store rating (target: 4.5+)

### 9.2 Monetization
- Free → Plus conversion rate (target: 5%)
- Free → Pro conversion rate (target: 1%)
- Plus → Pro upgrade rate (target: 10%)
- Pro trial → paid conversion (target: 30%)
- Monthly Recurring Revenue (MRR)
- Average Revenue Per User (ARPU)
- Churn rate (target: <5% monthly)

### 9.3 Technical Performance
- App crash rate (target: <0.5%)
- API response time (target: <1s avg)
- Route calculation success rate (target: >99%)
- Map rendering performance (target: 60fps)
- Battery efficiency (see section 5.1)

### 9.4 User Engagement
- Voice input usage rate
- Multi-waypoint route creation (Plus users)
- Pro tier feature adoption (vehicle profiles, truck routing)
- Average routes per user per week

---

## 10. User Experience Requirements

### 10.1 Onboarding Flow

**First Launch**:
1. Welcome screen with tier overview
2. Permission requests (location, microphone)
3. Optional account creation (required for Plus/Pro)
4. Tier selection (Free starts immediately)
5. Quick tutorial (voice input demo)

**Upgrade Flow**:
- Clear tier comparison screen
- Highlight features unlocked by upgrade
- Seamless checkout (Google Play Billing)
- Instant feature unlock post-purchase

### 10.2 Navigation Flow

**Free Tier**:
1. Voice or text input for destination
2. Gemini Nano processes request
3. Generate Google Maps intent
4. Launch Maps app with navigation

**Plus Tier**:
1. Voice or text input
2. Gemini Cloud processes request
3. Display route options in-app
4. Start in-app navigation with Maps SDK

**Pro Tier**:
1. Select routing engine (HERE truck or Google car)
2. Configure vehicle profile (if truck mode)
3. Voice or text input
4. Display route with compliance warnings
5. Start navigation with appropriate SDK

### 10.3 Accessibility Requirements

- Screen reader support (TalkBack/VoiceOver)
- Voice-only navigation option
- High contrast mode
- Scalable text sizes
- Color-blind friendly UI
- Haptic feedback for alerts

### 10.4 Localization (Phase 1)

**Languages**:
- English (US) - primary
- English (CA) - primary
- Spanish (Mexico) - optional Phase 2

**Units**:
- Imperial (miles, feet) for US
- Metric (kilometers, meters) for Canada
- User preference override

---

## 11. Risk Assessment & Mitigation

### 11.1 Technical Risks

**Risk**: Gemini Nano availability on older Android devices  
**Mitigation**: Fallback to Gemini Cloud for Free tier if on-device unavailable

**Risk**: HERE SDK integration complexity (Pro tier)  
**Mitigation**: Phase 2 launch allows more dev time, partner with HERE support

**Risk**: Battery drain from continuous location tracking  
**Mitigation**: Optimized location updates, low-power mode options

### 11.2 Legal Risks

**Risk**: Violating Google Maps + HERE SDK no-mixing rule  
**Mitigation**: Strict architectural separation, code review enforcement, automated testing

**Risk**: Subscription billing disputes  
**Mitigation**: Clear terms, transparent pricing, easy cancellation, responsive support

### 11.3 Market Risks

**Risk**: Low Free → Plus conversion rate  
**Mitigation**: Strong value proposition, trial periods considered, feature gating optimization

**Risk**: Pro tier adoption lower than expected  
**Mitigation**: Phase 2 launch allows market validation with Free/Plus first

---

## 12. Dependencies & Constraints

### 12.1 External Dependencies

- Gemini Nano SDK (Google)
- Gemini Cloud API (Google)
- Google Maps SDK and APIs (Google)
- HERE SDK commercial license (HERE Technologies)
- Google Play Billing (Google)
- Apple StoreKit 2 (Apple, Phase 2)

### 12.2 Internal Dependencies

- Backend infrastructure for user accounts
- Subscription management system
- Analytics and monitoring tools
- Customer support system
- Content delivery network (CDN) for assets

### 12.3 Constraints

- HERE SDK cannot be used with Google Maps tiles/UI
- Google Play Billing required for Android (no alternative)
- Apple IAP required for iOS (no alternative)
- One trial per user enforcement requires account system
- GDPR compliance required for cloud data processing

---

## 13. Appendix

### 13.1 Glossary

- **Intent**: Android mechanism to launch external apps with data
- **URL Scheme**: iOS mechanism to launch external apps with data
- **HERE SDK**: Commercial routing SDK with truck-specific features
- **Gemini Nano**: On-device AI model from Google
- **IAP**: In-App Purchase
- **HOS**: Hours of Service (trucking regulations)
- **POI**: Point of Interest
- **SDK**: Software Development Kit
- **MVVM**: Model-View-ViewModel architecture pattern

### 13.2 Open Questions

*(To be resolved during development)*

1. Gemini Nano fallback strategy if unavailable on device?
2. Exact HERE SDK licensing costs for Pro tier?
3. Stripe integration feasibility within Google Play policies?
4. Fleet features scope for Phase 2?
5. Mexico region launch requirements and restrictions?

### 13.3 Document History

**v1.0 (2025-11-21)**: Initial PRD created for MP-002

---

**END OF PRODUCT REQUIREMENTS DOCUMENT**

---

*This PRD drives all future micro-projects for GemNav development. Any changes to requirements must be documented with version updates and stakeholder approval.*
