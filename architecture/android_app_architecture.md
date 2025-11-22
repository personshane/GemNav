# Android Application Architecture

**Version**: 1.0  
**Platform**: Android  
**Pattern**: MVVM + Clean Architecture  
**DI**: Hilt

---

## Architecture Overview

```
Presentation Layer (UI)
    ↓
ViewModel Layer (State Management)
    ↓
Domain Layer (Use Cases)
    ↓
Data Layer (Repository)
    ↓
Data Sources (Remote/Local)
```

---

## Package Structure

```
com.gemnav.app/
├── di/                      # Dependency Injection
│   ├── AppModule.kt
│   ├── NetworkModule.kt
│   ├── DatabaseModule.kt
│   ├── RepositoryModule.kt
│   └── NavigationModule.kt
│
├── ui/                      # Presentation Layer
│   ├── main/
│   │   ├── MainActivity.kt
│   │   └── MainViewModel.kt
│   ├── chat/
│   │   ├── ChatFragment.kt
│   │   ├── ChatViewModel.kt
│   │   └── adapters/
│   ├── navigation/
│   │   ├── NavigationFragment.kt
│   │   ├── NavigationViewModel.kt
│   │   └── MapViewController.kt
│   ├── settings/
│   │   ├── SettingsFragment.kt
│   │   └── SettingsViewModel.kt
│   └── common/
│       ├── BaseFragment.kt
│       ├── BaseViewModel.kt
│       └── ViewState.kt
│
├── domain/                  # Domain Layer
│   ├── model/
│   │   ├── Route.kt
│   │   ├── Location.kt
│   │   ├── ChatMessage.kt
│   │   └── NavigationState.kt
│   ├── repository/
│   │   ├── NavigationRepository.kt
│   │   ├── ChatRepository.kt
│   │   └── UserRepository.kt
│   └── usecase/
│       ├── CalculateRouteUseCase.kt
│       ├── SendMessageUseCase.kt
│       └── GetNavigationStateUseCase.kt
│
├── data/                    # Data Layer
│   ├── repository/
│   │   ├── NavigationRepositoryImpl.kt
│   │   ├── ChatRepositoryImpl.kt
│   │   └── UserRepositoryImpl.kt
│   ├── local/
│   │   ├── database/
│   │   │   ├── GemNavDatabase.kt
│   │   │   ├── dao/
│   │   │   │   ├── RouteDao.kt
│   │   │   │   ├── MessageDao.kt
│   │   │   │   └── LocationDao.kt
│   │   │   └── entity/
│   │   │       ├── RouteEntity.kt
│   │   │       ├── MessageEntity.kt
│   │   │       └── LocationEntity.kt
│   │   └── preferences/
│   │       └── UserPreferences.kt
│   ├── remote/
│   │   ├── api/
│   │   │   ├── GeminiApi.kt
│   │   │   ├── NavigationApi.kt
│   │   │   └── UserApi.kt
│   │   ├── dto/
│   │   │   ├── RouteResponse.kt
│   │   │   ├── ChatResponse.kt
│   │   │   └── UserDto.kt
│   │   └── interceptor/
│   │       ├── AuthInterceptor.kt
│   │       └── LoggingInterceptor.kt
│   └── mapper/
│       ├── RouteMapper.kt
│       ├── MessageMapper.kt
│       └── LocationMapper.kt
│
├── service/                 # Background Services
│   ├── LocationService.kt
│   ├── NavigationService.kt
│   └── VoiceCommandService.kt
│
└── util/                    # Utilities
    ├── Constants.kt
    ├── Extensions.kt
    ├── PermissionManager.kt
    └── NetworkMonitor.kt
```

[Continue with rest of android_app_architecture.md content...]