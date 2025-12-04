# MP-016-E: Voice UI Integration

**Status**: In Progress  
**Estimated Lines**: ~400 (Android: ~200, iOS: ~200)  
**Dependencies**: MP-016-A/B/C/D (all voice components complete)

## Objective

Integrate voice UI components (VoiceButton, VoiceFeedbackOverlay, VoicePermissionDialog, WakeWordIndicator) into NavigationActivity (Android) and NavigationView (iOS). Wire components to VoiceCommandManager, connect permission flows, and enable tier-specific voice features.

## Integration Points

### Android: NavigationActivity.kt

**Current State**: NavigationScreen composable with tier-specific map views, NavigationInfoCard, NavigationControls

**Changes Needed**:
1. Add VoiceCommandManager injection/initialization
2. Add VoiceButton to NavigationControls row
3. Add VoiceFeedbackOverlay as top-level overlay
4. Add VoicePermissionDialog triggered by VoiceButton
5. Add WakeWordIndicator (Plus/Pro only) near top bar
6. Connect VoiceButton state to VoiceCommandManager
7. Handle voice command results (route changes, settings updates)

### iOS: NavigationView.swift (Create New)

**Current State**: Not created yet - using NavigationViewModel.swift

**Changes Needed**:
1. Create NavigationView.swift as main navigation screen
2. Integrate VoiceButton into navigation overlay
3. Add VoiceFeedbackOverlay as ZStack layer
4. Add VoicePermissionView sheet presentation
5. Add WakeWordIndicator (Plus/Pro only)
6. Connect to VoiceCommandManager via ViewModel
7. Handle voice command routing updates

## Tier-Specific Features

| Tier | Voice Button | Wake Word | Permissions |
|------|-------------|-----------|-------------|
| Free | ✓ (basic)   | ✗         | Microphone  |
| Plus | ✓ (full)    | ✓         | Microphone  |
| Pro  | ✓ (full)    | ✓         | Microphone  |

**Free Tier**: Basic voice commands, no wake word, launches Google Maps app for navigation
**Plus Tier**: Full voice control, wake word detection, in-app Google Maps SDK navigation
**Pro Tier**: Full voice control, wake word detection, HERE SDK navigation with truck routing

## State Management

### Android ViewModel Extensions

```kotlin
// Add to NavigationViewModel
private val voiceCommandManager: VoiceCommandManager by inject()
private val _voiceState = MutableStateFlow(VoiceState())
val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

data class VoiceState(
    val isListening: Boolean = false,
    val isProcessing: Boolean = false,
    val wakeWordActive: Boolean = false,
    val permissionGranted: Boolean = false,
    val feedbackMessage: String? = null,
    val feedbackType: FeedbackType = FeedbackType.INFO
)

fun initializeVoice(context: Context) {
    voiceCommandManager.initialize(tier, context)
    observeVoiceCommands()
}

private fun observeVoiceCommands() {
    viewModelScope.launch {
        voiceCommandManager.commandFlow.collect { command ->
            handleVoiceCommand(command)
        }
    }
}

fun startVoiceListening() {
    viewModelScope.launch {
        voiceCommandManager.startListening()
        _voiceState.update { it.copy(isListening = true) }
    }
}

fun stopVoiceListening() {
    voiceCommandManager.stopListening()
    _voiceState.update { it.copy(isListening = false) }
}

fun handleVoiceCommand(command: VoiceCommand) {
    _voiceState.update { it.copy(isProcessing = true) }
    
    when (command) {
        is NavigateToCommand -> updateDestination(command.location)
        is MuteCommand -> toggleMute()
        is RecenterCommand -> recenterMap()
        is AlternateRouteCommand -> requestAlternateRoute()
        // ... other commands
    }
    
    _voiceState.update { 
        it.copy(
            isProcessing = false,
            feedbackMessage = "Command executed",
            feedbackType = FeedbackType.SUCCESS
        )
    }
}
```

### iOS ObservableObject Extensions

```swift
// Add to NavigationViewModel
@Published var voiceState = VoiceState()
private var voiceCommandManager: VoiceCommandManager?
private var cancellables = Set<AnyCancellable>()

struct VoiceState {
    var isListening: Bool = false
    var isProcessing: Bool = false
    var wakeWordActive: Bool = false
    var permissionGranted: Bool = false
    var feedbackMessage: String? = nil
    var feedbackType: FeedbackType = .info
}

func initializeVoice() {
    voiceCommandManager = VoiceCommandManager(tier: tier)
    voiceCommandManager?.initialize()
    observeVoiceCommands()
}

private func observeVoiceCommands() {
    voiceCommandManager?.commandPublisher
        .sink { [weak self] command in
            self?.handleVoiceCommand(command)
        }
        .store(in: &cancellables)
}

func startVoiceListening() {
    voiceCommandManager?.startListening()
    voiceState.isListening = true
}

func stopVoiceListening() {
    voiceCommandManager?.stopListening()
    voiceState.isListening = false
}

func handleVoiceCommand(_ command: VoiceCommand) {
    voiceState.isProcessing = true
    
    switch command {
    case .navigateTo(let location):
        updateDestination(location)
    case .mute:
        toggleMute()
    case .recenter:
        recenterMap()
    case .alternateRoute:
        requestAlternateRoute()
    }
    
    voiceState.isProcessing = false
    voiceState.feedbackMessage = "Command executed"
    voiceState.feedbackType = .success
}
```

## UI Layout Changes

### Android NavigationScreen Composable

```kotlin
@Composable
fun NavigationScreen(
    viewModel: NavigationViewModel,
    onExit: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()
    val tier = uiState.tier
    
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.initializeVoice(LocalContext.current)
    }
    
    Scaffold(
        topBar = {
            NavigationTopBar(
                destination = uiState.destination,
                onExit = {
                    viewModel.stopNavigation()
                    onExit()
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Map views (existing)
            when (tier) {
                "free" -> FreeTierNavigationMessage()
                "plus" -> GoogleMapsNavigation(...)
                "pro" -> HereNavigation(...)
            }
            
            // Wake word indicator (Plus/Pro only)
            if (tier in listOf("plus", "pro") && voiceState.wakeWordActive) {
                WakeWordIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                )
            }
            
            // Voice feedback overlay
            VoiceFeedbackOverlay(
                isVisible = voiceState.feedbackMessage != null,
                message = voiceState.feedbackMessage ?: "",
                type = voiceState.feedbackType,
                onDismiss = { viewModel.clearVoiceFeedback() }
            )
            
            // Navigation info and controls (existing + voice button)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                NavigationInfoCard(...)
                Spacer(modifier = Modifier.height(16.dp))
                
                // Updated controls with voice button
                NavigationControls(
                    onRecenter = { viewModel.recenterMap() },
                    onMute = { viewModel.toggleMute() },
                    isMuted = uiState.isMuted,
                    voiceState = voiceState,
                    onVoiceClick = {
                        if (voiceState.permissionGranted) {
                            if (voiceState.isListening) {
                                viewModel.stopVoiceListening()
                            } else {
                                viewModel.startVoiceListening()
                            }
                        } else {
                            showPermissionDialog = true
                        }
                    }
                )
            }
            
            // Error snackbar (existing)
            if (uiState.error != null) {
                ErrorSnackbar(...)
            }
        }
    }
    
    // Voice permission dialog
    if (showPermissionDialog) {
        VoicePermissionDialog(
            onGranted = {
                viewModel.onVoicePermissionGranted()
                showPermissionDialog = false
            },
            onDenied = {
                showPermissionDialog = false
            },
            onDismiss = {
                showPermissionDialog = false
            }
        )
    }
}
```

### iOS NavigationView

```swift
struct NavigationView: View {
    @StateObject var viewModel: NavigationViewModel
    @State private var showPermissionSheet = false
    
    var body: some View {
        ZStack {
            // Map view based on tier
            if viewModel.tier == "free" {
                FreeTierNavigationMessage()
            } else if viewModel.tier == "plus" {
                GoogleMapsNavigationView(viewModel: viewModel)
            } else {
                HERENavigationView(viewModel: viewModel)
            }
            
            VStack {
                // Wake word indicator (Plus/Pro)
                if ["plus", "pro"].contains(viewModel.tier) && viewModel.voiceState.wakeWordActive {
                    WakeWordIndicator()
                        .padding(.top, 16)
                }
                
                Spacer()
                
                // Navigation info card
                NavigationInfoCard(viewModel: viewModel)
                    .padding(.horizontal)
                
                // Controls with voice button
                HStack(spacing: 20) {
                    Button(action: viewModel.recenterMap) {
                        Image(systemName: "location.fill")
                            .font(.title2)
                    }
                    .buttonStyle(.borderedProminent)
                    
                    VoiceButton(
                        isListening: viewModel.voiceState.isListening,
                        isProcessing: viewModel.voiceState.isProcessing,
                        onTap: {
                            if viewModel.voiceState.permissionGranted {
                                if viewModel.voiceState.isListening {
                                    viewModel.stopVoiceListening()
                                } else {
                                    viewModel.startVoiceListening()
                                }
                            } else {
                                showPermissionSheet = true
                            }
                        }
                    )
                    
                    Button(action: viewModel.toggleMute) {
                        Image(systemName: viewModel.isMuted ? "speaker.slash.fill" : "speaker.wave.2.fill")
                            .font(.title2)
                    }
                    .buttonStyle(.borderedProminent)
                }
                .padding(.bottom)
            }
            
            // Voice feedback overlay
            if viewModel.voiceState.feedbackMessage != nil {
                VoiceFeedbackOverlay(
                    message: viewModel.voiceState.feedbackMessage ?? "",
                    type: viewModel.voiceState.feedbackType,
                    onDismiss: viewModel.clearVoiceFeedback
                )
            }
        }
        .sheet(isPresented: $showPermissionSheet) {
            VoicePermissionView(
                onGranted: {
                    viewModel.onVoicePermissionGranted()
                    showPermissionSheet = false
                },
                onDenied: {
                    showPermissionSheet = false
                }
            )
        }
        .onAppear {
            viewModel.initializeVoice()
        }
    }
}
```

## Permission Flow Integration

### Android: MicrophonePermissionManager Integration

Update NavigationViewModel to check/request permissions:

```kotlin
fun onVoicePermissionGranted() {
    permissionManager.requestMicrophonePermission { granted ->
        _voiceState.update { it.copy(permissionGranted = granted) }
        if (granted) {
            startVoiceListening()
        }
    }
}
```

### iOS: MicrophonePermissionManager Integration

Update NavigationViewModel to handle iOS permissions:

```swift
func onVoicePermissionGranted() {
    MicrophonePermissionManager.shared.requestPermission { granted in
        DispatchQueue.main.async {
            self.voiceState.permissionGranted = granted
            if granted {
                self.startVoiceListening()
            }
        }
    }
}
```

## Testing Scenarios

### Android
1. Launch navigation in Free tier → Voice button present, no wake word
2. Tap voice button without permission → Permission dialog appears
3. Grant permission → Voice listening starts, button shows listening state
4. Speak "navigate to Starbucks" → Feedback overlay shows processing → Route updates
5. Plus tier → Wake word indicator appears when "Hey GemNav" detected
6. Pro tier → Voice commands work with HERE SDK navigation

### iOS
1. Present NavigationView in Free tier → Voice button visible, no wake word
2. Tap voice button → Permission sheet presented
3. Grant permission → Voice listening active, button animates
4. Speak command → Feedback overlay displays → Command executed
5. Plus/Pro tiers → Wake word indicator active
6. Background voice → Wake word detection continues

## Line Count Estimates

**Android**: ~200 lines
- NavigationViewModel voice extensions: ~80 lines
- NavigationScreen integration: ~80 lines
- NavigationControls update: ~40 lines

**iOS**: ~200 lines
- NavigationView.swift (new file): ~120 lines
- NavigationViewModel extensions: ~80 lines

**Total**: ~400 lines

## Files Modified/Created

### Android
- `android/app/navigation/NavigationViewModel.kt` (modify, add ~80 lines)
- `android/app/navigation/NavigationActivity.kt` (modify, add ~120 lines)

### iOS
- `ios/GemNav/Navigation/NavigationView.swift` (create new, ~120 lines)
- `ios/app/viewmodels/NavigationViewModel.swift` (modify, add ~80 lines)

## Handoff

After completion:
1. Voice UI fully integrated into navigation screens (Android + iOS)
2. VoiceButton wired to VoiceCommandManager
3. Permission flows connected to MicrophonePermissionManager
4. Tier-specific features enabled (wake word for Plus/Pro)
5. Voice commands trigger navigation updates
6. Ready for end-to-end testing across all three tiers

Next: MP-016-F (Integration Testing) or MP-017 (next feature)
