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

---

## MainActivity.kt

```kotlin
package com.gemnav.app.ui.main

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.gemnav.app.R
import com.gemnav.app.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupNavigation()
        observeViewModel()
        checkPermissions()
    }
    
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        binding.bottomNav.setupWithNavController(navController)
    }
    
    private fun observeViewModel() {
        viewModel.navigationState.observe(this) { state ->
            // Handle navigation state changes
        }
        
        viewModel.permissionState.observe(this) { granted ->
            if (!granted) {
                // Show permission rationale
            }
        }
    }
    
    private fun checkPermissions() {
        viewModel.checkRequiredPermissions()
    }
}
```

---

## MainViewModel.kt

```kotlin
package com.gemnav.app.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemnav.app.domain.model.NavigationState
import com.gemnav.app.domain.repository.UserRepository
import com.gemnav.app.util.PermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val permissionManager: PermissionManager
) : ViewModel() {
    
    private val _navigationState = MutableLiveData<NavigationState>()
    val navigationState: LiveData<NavigationState> = _navigationState
    
    private val _permissionState = MutableLiveData<Boolean>()
    val permissionState: LiveData<Boolean> = _permissionState
    
    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Idle)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    fun checkRequiredPermissions() {
        viewModelScope.launch {
            val granted = permissionManager.areLocationPermissionsGranted()
            _permissionState.value = granted
        }
    }
    
    fun requestPermissions() {
        // Trigger permission request via activity
    }
}

sealed class MainUiState {
    object Idle : MainUiState()
    object Loading : MainUiState()
    data class Success(val message: String) : MainUiState()
    data class Error(val message: String) : MainUiState()
}
```

---

## BaseFragment.kt

```kotlin
package com.gemnav.app.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

abstract class BaseFragment<VB : ViewBinding> : Fragment() {
    
    private var _binding: VB? = null
    protected val binding get() = _binding!!
    
    abstract fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): VB
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = getViewBinding(inflater, container)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeData()
    }
    
    abstract fun setupUI()
    abstract fun observeData()
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    protected fun <T> Flow<T>.collectWhenStarted(action: suspend (T) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                collect(action)
            }
        }
    }
}
```

---

## BaseViewModel.kt

```kotlin
package com.gemnav.app.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel<State : ViewState, Event : ViewEvent>(
    initialState: State,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<State> = _uiState.asStateFlow()
    
    private val _event = MutableSharedFlow<Event>()
    val event: SharedFlow<Event> = _event.asSharedFlow()
    
    protected fun updateState(reducer: State.() -> State) {
        _uiState.value = _uiState.value.reducer()
    }
    
    protected fun sendEvent(event: Event) {
        viewModelScope.launch(dispatcher) {
            _event.emit(event)
        }
    }
    
    protected fun launchIO(block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            block()
        }
    }
}

interface ViewState
interface ViewEvent
```

---

## NavigationFragment.kt

```kotlin
package com.gemnav.app.ui.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.gemnav.app.databinding.FragmentNavigationBinding
import com.gemnav.app.ui.common.BaseFragment
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NavigationFragment : BaseFragment<FragmentNavigationBinding>(), OnMapReadyCallback {
    
    private val viewModel: NavigationViewModel by viewModels()
    private var googleMap: GoogleMap? = null
    
    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentNavigationBinding {
        return FragmentNavigationBinding.inflate(inflater, container, false)
    }
    
    override fun setupUI() {
        val mapFragment = childFragmentManager
            .findFragmentById(binding.mapContainer.id) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
        
        binding.searchBar.setOnClickListener {
            viewModel.openSearchDialog()
        }
        
        binding.startNavigationButton.setOnClickListener {
            viewModel.startNavigation()
        }
    }
    
    override fun observeData() {
        viewModel.uiState.collectWhenStarted { state ->
            when (state) {
                is NavigationUiState.Idle -> showIdleState()
                is NavigationUiState.RouteCalculating -> showLoadingState()
                is NavigationUiState.RouteReady -> showRoute(state.route)
                is NavigationUiState.Navigating -> showNavigating(state)
                is NavigationUiState.Error -> showError(state.message)
            }
        }
    }
    
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        viewModel.onMapReady()
        
        // Configure map
        map.apply {
            uiSettings.isZoomControlsEnabled = true
            uiSettings.isCompassEnabled = true
            uiSettings.isMyLocationButtonEnabled = true
        }
    }
    
    private fun showIdleState() {
        // Show search prompt
    }
    
    private fun showLoadingState() {
        // Show progress indicator
    }
    
    private fun showRoute(route: com.gemnav.app.domain.model.Route) {
        // Draw route on map
        googleMap?.let { map ->
            // Add polyline, markers, etc.
        }
    }
    
    private fun showNavigating(state: NavigationUiState.Navigating) {
        // Update turn-by-turn UI
    }
    
    private fun showError(message: String) {
        // Show error message
    }
}
```

---

## NavigationViewModel.kt

```kotlin
package com.gemnav.app.ui.navigation

import androidx.lifecycle.viewModelScope
import com.gemnav.app.domain.model.Location
import com.gemnav.app.domain.model.Route
import com.gemnav.app.domain.usecase.CalculateRouteUseCase
import com.gemnav.app.domain.usecase.GetNavigationStateUseCase
import com.gemnav.app.ui.common.BaseViewModel
import com.gemnav.app.ui.common.ViewEvent
import com.gemnav.app.ui.common.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val calculateRouteUseCase: CalculateRouteUseCase,
    private val getNavigationStateUseCase: GetNavigationStateUseCase
) : BaseViewModel<NavigationUiState, NavigationEvent>(NavigationUiState.Idle) {
    
    init {
        observeNavigationState()
    }
    
    fun calculateRoute(origin: Location, destination: Location) {
        updateState { NavigationUiState.RouteCalculating }
        
        launchIO {
            calculateRouteUseCase(origin, destination).fold(
                onSuccess = { route ->
                    updateState { NavigationUiState.RouteReady(route) }
                },
                onFailure = { error ->
                    updateState { NavigationUiState.Error(error.message ?: "Unknown error") }
                }
            )
        }
    }
    
    fun startNavigation() {
        val currentState = uiState.value
        if (currentState is NavigationUiState.RouteReady) {
            updateState {
                NavigationUiState.Navigating(
                    route = currentState.route,
                    currentLocation = null,
                    nextTurn = null
                )
            }
            sendEvent(NavigationEvent.NavigationStarted)
        }
    }
    
    fun openSearchDialog() {
        sendEvent(NavigationEvent.OpenSearch)
    }
    
    fun onMapReady() {
        sendEvent(NavigationEvent.MapReady)
    }
    
    private fun observeNavigationState() {
        viewModelScope.launch {
            getNavigationStateUseCase().collectLatest { navState ->
                // Update UI based on navigation state changes
            }
        }
    }
}

sealed class NavigationUiState : ViewState {
    object Idle : NavigationUiState()
    object RouteCalculating : NavigationUiState()
    data class RouteReady(val route: Route) : NavigationUiState()
    data class Navigating(
        val route: Route,
        val currentLocation: Location?,
        val nextTurn: String?
    ) : NavigationUiState()
    data class Error(val message: String) : NavigationUiState()
}

sealed class NavigationEvent : ViewEvent {
    object NavigationStarted : NavigationEvent()
    object NavigationStopped : NavigationEvent()
    object OpenSearch : NavigationEvent()
    object MapReady : NavigationEvent()
}
```

---

## ChatFragment.kt

```kotlin
package com.gemnav.app.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.gemnav.app.databinding.FragmentChatBinding
import com.gemnav.app.ui.chat.adapters.ChatAdapter
import com.gemnav.app.ui.common.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatFragment : BaseFragment<FragmentChatBinding>() {
    
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter
    
    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentChatBinding {
        return FragmentChatBinding.inflate(inflater, container, false)
    }
    
    override fun setupUI() {
        chatAdapter = ChatAdapter()
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chatAdapter
        }
        
        binding.sendButton.setOnClickListener {
            val message = binding.inputField.text.toString()
            if (message.isNotBlank()) {
                viewModel.sendMessage(message)
                binding.inputField.text?.clear()
            }
        }
        
        binding.voiceButton.setOnClickListener {
            viewModel.startVoiceInput()
        }
    }
    
    override fun observeData() {
        viewModel.messages.collectWhenStarted { messages ->
            chatAdapter.submitList(messages)
            binding.recyclerView.scrollToPosition(messages.size - 1)
        }
        
        viewModel.event.collectWhenStarted { event ->
            when (event) {
                is ChatEvent.MessageSent -> {
                    // Animate message sent
                }
                is ChatEvent.VoiceInputStarted -> {
                    // Show voice input UI
                }
                is ChatEvent.Error -> {
                    // Show error
                }
            }
        }
    }
}
```

---

## ChatViewModel.kt

```kotlin
package com.gemnav.app.ui.chat

import androidx.lifecycle.viewModelScope
import com.gemnav.app.domain.model.ChatMessage
import com.gemnav.app.domain.usecase.SendMessageUseCase
import com.gemnav.app.ui.common.BaseViewModel
import com.gemnav.app.ui.common.ViewEvent
import com.gemnav.app.ui.common.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase
) : BaseViewModel<ChatUiState, ChatEvent>(ChatUiState()) {
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    fun sendMessage(text: String) {
        val userMessage = ChatMessage.User(text, System.currentTimeMillis())
        _messages.value = _messages.value + userMessage
        
        updateState { copy(isLoading = true) }
        
        viewModelScope.launch {
            sendMessageUseCase(text).fold(
                onSuccess = { response ->
                    val aiMessage = ChatMessage.Assistant(response, System.currentTimeMillis())
                    _messages.value = _messages.value + aiMessage
                    updateState { copy(isLoading = false) }
                    sendEvent(ChatEvent.MessageSent)
                },
                onFailure = { error ->
                    updateState { copy(isLoading = false, error = error.message) }
                    sendEvent(ChatEvent.Error(error.message ?: "Unknown error"))
                }
            )
        }
    }
    
    fun startVoiceInput() {
        sendEvent(ChatEvent.VoiceInputStarted)
    }
}

data class ChatUiState(
    val isLoading: Boolean = false,
    val error: String? = null
) : ViewState

sealed class ChatEvent : ViewEvent {
    object MessageSent : ChatEvent()
    object VoiceInputStarted : ChatEvent()
    data class Error(val message: String) : ChatEvent()
}
```

---

## Tier-Specific Implementations

### Free Tier - OnDeviceNavigationViewModel.kt

```kotlin
package com.gemnav.app.ui.navigation.free

import com.gemnav.app.BuildConfig
import com.gemnav.app.ui.navigation.NavigationViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnDeviceNavigationViewModel @Inject constructor(
    // Free tier specific dependencies
) : NavigationViewModel(
    calculateRouteUseCase,
    getNavigationStateUseCase
) {
    
    init {
        if (BuildConfig.TIER_FREE) {
            // Use on-device AI (Gemini Nano)
            // Launch Google Maps via intents
        }
    }
    
    override fun startNavigation() {
        // Launch Google Maps app via intent
        sendEvent(NavigationEvent.LaunchGoogleMapsIntent)
    }
}
```

### Pro Tier - HERENavigationViewModel.kt

```kotlin
package com.gemnav.app.ui.navigation.pro

import com.gemnav.app.BuildConfig
import com.gemnav.app.domain.usecase.CalculateHERETruckRouteUseCase
import com.gemnav.app.ui.navigation.NavigationViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HERENavigationViewModel @Inject constructor(
    private val calculateHERETruckRouteUseCase: CalculateHERETruckRouteUseCase
    // Pro tier specific dependencies
) : NavigationViewModel(
    calculateRouteUseCase,
    getNavigationStateUseCase
) {
    
    private var useHERERouting = true
    
    init {
        if (BuildConfig.TIER_PRO) {
            // Initialize HERE SDK
            // Enable routing engine toggle
        }
    }
    
    fun toggleRoutingEngine() {
        useHERERouting = !useHERERouting
        sendEvent(NavigationEvent.RoutingEngineToggled(useHERERouting))
    }
    
    fun calculateTruckRoute(vehicleProfile: VehicleProfile) {
        launchIO {
            calculateHERETruckRouteUseCase(vehicleProfile).fold(
                onSuccess = { route ->
                    updateState { NavigationUiState.RouteReady(route) }
                },
                onFailure = { error ->
                    updateState { NavigationUiState.Error(error.message ?: "Unknown error") }
                }
            )
        }
    }
}
```

---

## Key Architecture Principles

### 1. Single Responsibility
- Each class has one clear purpose
- ViewModels manage UI state
- Repositories handle data operations
- Use Cases contain business logic

### 2. Dependency Inversion
- High-level modules depend on abstractions
- Use Hilt for dependency injection
- Interfaces define contracts

### 3. Separation of Concerns
- UI layer only handles display
- ViewModel manages state
- Repository coordinates data sources
- Domain models are platform-agnostic

### 4. Reactive Programming
- StateFlow for state management
- SharedFlow for one-time events
- Coroutines for async operations

### 5. Tier Isolation
- Build variants control tier features
- Conditional compilation via BuildConfig
- Separate ViewModels for tier-specific logic
