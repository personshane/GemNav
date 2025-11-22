# MP-014: Main App Flow & Routing

## Objective
Build the core user-facing navigation flow: home screen, destination input, route preview, and navigation start.

## Scope

### 1. Home Screen
- Search bar (voice + text)
- Recent destinations (last 10)
- Saved favorites
- Quick actions (Home, Work)
- Tier indicator
- Settings access

### 2. Destination Input
- Address search with autocomplete
- Places API integration (Plus/Pro)
- Map long-press selection
- Current location handling
- Multi-waypoint support (Plus/Pro)

### 3. Route Preview
- Route overview map
- Distance/duration display
- Alternative routes (Plus/Pro)
- Route options (fastest, shortest, eco)
- Tier-specific features

### 4. Navigation Start
- Route confirmation
- Background location permission
- Notification setup
- Hand off to Maps app (Free) or SDK (Plus/Pro)

### 5. Data Persistence
- Recent destinations (Room DB)
- Favorites storage
- Search history
- Settings preferences

## Architecture

### ViewModels
- `HomeViewModel`: Search, recents, favorites
- `RoutePreviewViewModel`: Route options, alternatives
- `NavigationViewModel`: Navigation state, tracking

### Repositories
- `DestinationRepository`: CRUD for destinations
- `RouteRepository`: Route calculation, caching
- `SearchRepository`: Search history, autocomplete

### Database (Room)
- `Destination` entity: address, coordinates, timestamp, favorite flag
- `SearchHistory` entity: query, timestamp
- `RouteHistory` entity: origin, destination, route data, timestamp

### UI Components (Compose)
- `HomeScreen`
- `SearchBar`
- `RecentDestinationsCard`
- `FavoritesCard`
- `QuickActionsRow`
- `DestinationInputSheet`
- `RoutePreviewScreen`
- `NavigationStartScreen`

## Tier-Specific Behavior

### Free Tier
- Google Maps intent generation
- No in-app preview
- Launch external Maps app

### Plus Tier
- In-app route preview with Maps SDK
- Alternative routes display
- Multi-waypoint support

### Pro Tier
- All Plus features
- Routing engine toggle (HERE/Google)
- Vehicle profile configuration

## Dependencies
- MP-001: Architecture foundation
- MP-002: Tier structure
- MP-003: Google Maps integration
- MP-004: HERE SDK integration
- MP-009: Gemini integration
- MP-013: Permissions & error handling

## Files to Create

### ViewModels
- `android/app/main_flow/HomeViewModel.kt`
- `android/app/main_flow/RoutePreviewViewModel.kt`
- `android/app/main_flow/NavigationViewModel.kt`

### Repositories
- `android/app/main_flow/DestinationRepository.kt`
- `android/app/main_flow/RouteRepository.kt`
- `android/app/main_flow/SearchRepository.kt`

### Database
- `android/app/main_flow/database/GemNavDatabase.kt`
- `android/app/main_flow/database/DestinationEntity.kt`
- `android/app/main_flow/database/DestinationDao.kt`
- `android/app/main_flow/database/SearchHistoryEntity.kt`
- `android/app/main_flow/database/SearchHistoryDao.kt`

### UI
- `android/app/main_flow/ui/HomeScreen.kt`
- `android/app/main_flow/ui/SearchBar.kt`
- `android/app/main_flow/ui/RecentDestinationsCard.kt`
- `android/app/main_flow/ui/FavoritesCard.kt`
- `android/app/main_flow/ui/QuickActionsRow.kt`
- `android/app/main_flow/ui/DestinationInputSheet.kt`
- `android/app/main_flow/ui/RoutePreviewScreen.kt`
- `android/app/main_flow/ui/NavigationStartScreen.kt`

### Models
- `android/app/main_flow/models/Destination.kt`
- `android/app/main_flow/models/Route.kt`
- `android/app/main_flow/models/RouteOptions.kt`

## Implementation Order
1. Database layer (entities, DAOs, database)
2. Models (Destination, Route, RouteOptions)
3. Repositories
4. ViewModels
5. UI components
6. Integration testing

## Success Criteria
- Home screen displays with search bar and recents
- Search autocomplete works (Plus/Pro with Places API)
- Route preview shows distance/duration
- Free tier launches Google Maps app correctly
- Plus/Pro tier shows in-app navigation
- Recent destinations persist across app restarts
- Favorites can be saved/removed
- All tier-specific features properly gated

## Estimated Lines of Code
~3,500 lines total:
- Database: ~400 lines
- Models: ~200 lines
- Repositories: ~600 lines
- ViewModels: ~800 lines
- UI: ~1,500 lines

---

**Status**: Ready to implement
**Next**: Start with database layer