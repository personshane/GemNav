# MP-016: Voice Command System Specification

**Status**: In Progress  
**Created**: 2025-11-22  
**Dependencies**: MP-010 (Architecture), MP-015 (Navigation)  
**Platforms**: Android, iOS

---

## 1. Objective

Implement comprehensive voice command functionality across all three GemNav tiers, enabling hands-free operation through speech recognition, natural language processing via Gemini, and text-to-speech responses.

**Key Goals**:
- Tier-differentiated voice capabilities (Free: basic, Plus/Pro: advanced)
- Hands-free navigation control during active routing
- Wake word detection for Zero-UI activation
- Gemini integration for natural language understanding
- Cross-platform consistency (Android/iOS)
- Privacy-first processing (Free tier on-device only)

---

## 2. Tier-Specific Voice Capabilities

### 2.1 Free Tier

**Voice Input**:
- Destination entry via speech ("Navigate to Pike Place Market")
- Basic navigation commands during active route:
  - "Mute" / "Unmute"
  - "Cancel navigation"
  - "Repeat instruction"
  - "What's my ETA?"
  - "How far to destination?"

**Processing**:
- On-device speech recognition (Android SpeechRecognizer, iOS SFSpeechRecognizer)
- Gemini Nano for command intent parsing (on-device)
- No cloud API calls (privacy-first)

**Limitations**:
- No wake word detection
- No multi-turn conversations
- No complex queries
- No voice-activated search
- Manual activation required (tap microphone button)

### 2.2 Plus Tier

**Voice Input (All Free capabilities PLUS)**:
- Wake word activation: "Hey GemNav" (optional, can be disabled)
- Multi-turn conversations with context retention
- Complex search queries:
  - "Find coffee shops with outdoor seating near my destination"
  - "Show me gas stations with prices under $3 along my route"
  - "Add a stop at the nearest pharmacy"
- Route modifications:
  - "Avoid tolls"
  - "Find fastest route"
  - "Show me alternative routes"
- Information queries:
  - "What's the weather at my destination?"
  - "When will I arrive?"
  - "Are there any traffic delays?"

**Processing**:
- Cloud-based speech recognition (Google Cloud Speech-to-Text)
- Gemini Cloud API for advanced NLU
- Context retention across conversation turns
- Enhanced wake word models

**Advanced Features**:
- Voice profile customization
- Multi-language support
- Accent adaptation
- Noise cancellation improvements

### 2.3 Pro Tier

**Voice Input (All Plus capabilities PLUS)**:
- Truck-specific commands:
  - "Find truck stops with diesel along my route"
  - "Check bridge clearances ahead"
  - "Show weigh stations on this route"
  - "Find rest areas with truck parking"
  - "Are there any height restrictions on this route?"
- HOS (Hours of Service) queries:
  - "How much drive time do I have left?" (Phase 2)
  - "When's my next required break?" (Phase 2)
- Compliance checks:
  - "Can my truck use this route?" (with configured vehicle profile)
  - "Any hazmat restrictions ahead?"

**Processing**:
- Same as Plus tier (Gemini Cloud)
- Extended context for truck-specific terminology
- Integration with vehicle profile data
- Compliance database queries via voice

---

## 3. Technical Architecture

### 3.1 Component Overview

```
VoiceCommandManager (coordinator)
├── SpeechRecognitionService (platform-specific)
├── WakeWordDetector (Plus/Pro only)
├── CommandParser (Gemini integration)
├── CommandExecutor (routes to app features)
└── VoiceResponseService (TTS output)
```

### 3.2 VoiceCommandManager

**Responsibilities**:
- Manage voice command lifecycle
- Coordinate between recognition, parsing, and execution
- Handle tier-based capability gating
- Maintain conversation context (Plus/Pro)
- Emit state updates to UI layer

**State Management**:
```kotlin
sealed class VoiceState {
    object Idle : VoiceState()
    object Listening : VoiceState()
    object Processing : VoiceState()
    data class Speaking(val text: String) : VoiceState()
    data class Error(val message: String) : VoiceState()
}
```

**Key Methods**:
- `startListening(trigger: VoiceTrigger)` - Begin speech recognition
- `stopListening()` - End recognition session
- `processCommand(transcript: String)` - Parse and execute
- `speak(text: String, interrupt: Boolean)` - TTS output
- `cancelSpeech()` - Stop current TTS
- `setWakeWordEnabled(enabled: Boolean)` - Toggle wake word (Plus/Pro)

### 3.3 SpeechRecognitionService

**Android Implementation** (SpeechRecognizer API):
```kotlin
class AndroidSpeechRecognitionService(
    private val context: Context,
    private val tier: SubscriptionTier
) : SpeechRecognitionService {
    
    private var recognizer: SpeechRecognizer? = null
    private val recognitionListener = object : RecognitionListener {
        override fun onResults(results: Bundle) {
            val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            matches?.firstOrNull()?.let { transcript ->
                onTranscriptReceived(transcript)
            }
        }
        
        override fun onError(error: Int) {
            handleRecognitionError(error)
        }
        
        // Other RecognitionListener methods...
    }
    
    override fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        
        recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer?.setRecognitionListener(recognitionListener)
        recognizer?.startListening(intent)
    }
    
    override fun stopListening() {
        recognizer?.stopListening()
        recognizer?.destroy()
        recognizer = null
    }
}
```

**iOS Implementation** (SFSpeechRecognizer):
```swift
class IOSSpeechRecognitionService: SpeechRecognitionService {
    private let speechRecognizer: SFSpeechRecognizer?
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest?
    private var recognitionTask: SFSpeechRecognitionTask?
    private let audioEngine = AVAudioEngine()
    
    init(tier: SubscriptionTier) {
        speechRecognizer = SFSpeechRecognizer(locale: Locale.current)
        requestAuthorization()
    }
    
    func startListening() throws {
        // Cancel any ongoing task
        recognitionTask?.cancel()
        recognitionTask = nil
        
        // Configure audio session
        let audioSession = AVAudioSession.sharedInstance()
        try audioSession.setCategory(.record, mode: .measurement)
        try audioSession.setActive(true, options: .notifyOthersOnDeactivation)
        
        // Create recognition request
        recognitionRequest = SFSpeechAudioBufferRecognitionRequest()
        guard let recognitionRequest = recognitionRequest else {
            throw VoiceError.recognitionUnavailable
        }
        
        recognitionRequest.shouldReportPartialResults = true
        
        // Start recognition task
        recognitionTask = speechRecognizer?.recognitionTask(
            with: recognitionRequest
        ) { [weak self] result, error in
            if let result = result {
                let transcript = result.bestTranscription.formattedString
                if result.isFinal {
                    self?.onTranscriptReceived(transcript)
                }
            }
            if error != nil {
                self?.stopListening()
            }
        }
        
        // Configure audio input
        let inputNode = audioEngine.inputNode
        let recordingFormat = inputNode.outputFormat(forBus: 0)
        inputNode.installTap(onBus: 0, bufferSize: 1024, format: recordingFormat) {
            buffer, _ in
            recognitionRequest.append(buffer)
        }
        
        audioEngine.prepare()
        try audioEngine.start()
    }
    
    func stopListening() {
        audioEngine.stop()
        audioEngine.inputNode.removeTap(onBus: 0)
        recognitionRequest?.endAudio()
        recognitionTask?.cancel()
    }
}
```

### 3.4 WakeWordDetector (Plus/Pro Only)

**Approach**: Lightweight on-device model for "Hey GemNav" detection

**Android** (TensorFlow Lite or Porcupine SDK):
```kotlin
class WakeWordDetector(
    private val context: Context,
    private val onWakeWordDetected: () -> Unit
) {
    private var porcupineManager: PorcupineManager? = null
    
    fun start() {
        porcupineManager = PorcupineManager.Builder()
            .setAccessKey(BuildConfig.PORCUPINE_ACCESS_KEY)
            .setKeyword(Porcupine.BuiltInKeyword.HEY_SIRI) // Use custom wake word
            .setSensitivity(0.7f)
            .build(context) { keywordIndex ->
                onWakeWordDetected()
            }
        
        porcupineManager?.start()
    }
    
    fun stop() {
        porcupineManager?.stop()
        porcupineManager?.delete()
    }
}
```

**iOS** (Similar approach with Porcupine or custom CoreML model)

**Alternative**: Use system wake word APIs if available (limited customization)

### 3.5 CommandParser (Gemini Integration)

**Responsibilities**:
- Parse natural language transcripts into structured commands
- Extract intent, entities, and parameters
- Handle multi-turn context (Plus/Pro)
- Return executable command objects

**Command Structure**:
```kotlin
sealed class VoiceCommand {
    // Navigation commands
    data class Navigate(val destination: String, val waypoints: List<String> = emptyList()) : VoiceCommand()
    data class AddStop(val location: String) : VoiceCommand()
    object CancelNavigation : VoiceCommand()
    object RecenterMap : VoiceCommand()
    
    // Audio commands
    object MuteVoice : VoiceCommand()
    object UnmuteVoice : VoiceCommand()
    object RepeatInstruction : VoiceCommand()
    
    // Information queries
    object GetETA : VoiceCommand()
    object GetDistanceRemaining : VoiceCommand()
    data class SearchAlongRoute(val query: String, val filters: Map<String, String>) : VoiceCommand()
    
    // Route modifications
    data class AvoidRouteFeature(val feature: RouteFeature) : VoiceCommand() // tolls, highways, ferries
    object ShowAlternativeRoutes : VoiceCommand()
    data class OptimizeRoute(val criterion: OptimizationCriterion) : VoiceCommand() // fastest, shortest, eco
    
    // Truck-specific (Pro tier)
    data class FindTruckPOI(val type: TruckPOIType) : VoiceCommand() // truck stops, weigh stations, rest areas
    object CheckBridgeClearances : VoiceCommand()
    object CheckHeightRestrictions : VoiceCommand()
    
    // Conversation
    data class Clarification(val question: String) : VoiceCommand()
    object Unknown : VoiceCommand()
}
```

**Gemini Parsing Prompt** (Free Tier - Gemini Nano):
```
You are a voice command parser for a navigation app.

User said: "{transcript}"

Extract the command intent and return ONE of:
- NAVIGATE <destination>
- MUTE
- UNMUTE
- CANCEL
- REPEAT
- GET_ETA
- GET_DISTANCE
- UNKNOWN

Return only the command, no explanation.
```

**Gemini Parsing Prompt** (Plus/Pro Tier - Gemini Cloud):
```
You are an advanced voice command parser for GemNav navigation.

Conversation history:
{context}

User said: "{transcript}"

Extract structured command as JSON:
{
  "intent": "navigate|add_stop|search|modify_route|query_info|control|unknown",
  "entities": {
    "destination": "string or null",
    "query": "string or null",
    "filters": ["filter1", "filter2"],
    "feature": "tolls|highways|ferries or null",
    "poi_type": "gas|coffee|restaurant|truck_stop|weigh_station or null"
  },
  "confidence": 0.0-1.0,
  "needs_clarification": true|false,
  "clarification_question": "string or null"
}

Respond with only valid JSON.
```

### 3.6 CommandExecutor

**Responsibilities**:
- Route parsed commands to appropriate app components
- Validate tier-based permissions
- Handle command execution errors
- Generate voice responses

**Implementation**:
```kotlin
class CommandExecutor(
    private val navigationViewModel: NavigationViewModel,
    private val searchViewModel: SearchViewModel,
    private val tier: SubscriptionTier,
    private val voiceResponseService: VoiceResponseService
) {
    suspend fun execute(command: VoiceCommand): CommandResult {
        return when (command) {
            is VoiceCommand.Navigate -> {
                if (tier.allowsAdvancedVoice() || command.waypoints.isEmpty()) {
                    navigationViewModel.navigateTo(command.destination, command.waypoints)
                    CommandResult.Success("Navigating to ${command.destination}")
                } else {
                    CommandResult.TierRestricted("Multi-stop navigation requires GemNav Plus")
                }
            }
            
            is VoiceCommand.SearchAlongRoute -> {
                if (tier.allowsAdvancedVoice()) {
                    val results = searchViewModel.searchAlongRoute(command.query, command.filters)
                    CommandResult.Success("Found ${results.size} results", data = results)
                } else {
                    CommandResult.TierRestricted("Advanced search requires GemNav Plus")
                }
            }
            
            is VoiceCommand.FindTruckPOI -> {
                if (tier == SubscriptionTier.PRO) {
                    val results = searchViewModel.findTruckPOI(command.type)
                    CommandResult.Success("Found ${results.size} ${command.type.displayName}", data = results)
                } else {
                    CommandResult.TierRestricted("Truck features require GemNav Pro")
                }
            }
            
            VoiceCommand.MuteVoice -> {
                navigationViewModel.setVoiceGuidanceMuted(true)
                CommandResult.Success("Voice guidance muted")
            }
            
            VoiceCommand.GetETA -> {
                val eta = navigationViewModel.currentETA.value
                CommandResult.Success("You'll arrive at ${eta.format()}")
            }
            
            // ... other command handlers
            
            VoiceCommand.Unknown -> {
                CommandResult.Error("I didn't understand that command")
            }
        }
    }
}

sealed class CommandResult {
    data class Success(val message: String, val data: Any? = null) : CommandResult()
    data class Error(val message: String) : CommandResult()
    data class TierRestricted(val message: String) : CommandResult()
    data class NeedsClarification(val question: String) : CommandResult()
}
```

### 3.7 VoiceResponseService (TTS)

**Android** (TextToSpeech API):
```kotlin
class AndroidVoiceResponseService(
    private val context: Context
) : VoiceResponseService {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    
    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                isInitialized = true
            }
        }
    }
    
    override fun speak(text: String, interrupt: Boolean) {
        if (!isInitialized) return
        
        val queueMode = if (interrupt) {
            TextToSpeech.QUEUE_FLUSH
        } else {
            TextToSpeech.QUEUE_ADD
        }
        
        tts?.speak(text, queueMode, null, "utterance_id")
    }
    
    override fun stop() {
        tts?.stop()
    }
    
    fun shutdown() {
        tts?.shutdown()
    }
}
```

**iOS** (AVSpeechSynthesizer):
```swift
class IOSVoiceResponseService: VoiceResponseService {
    private let synthesizer = AVSpeechSynthesizer()
    
    func speak(text: String, interrupt: Bool) {
        if interrupt {
            synthesizer.stopSpeaking(at: .immediate)
        }
        
        let utterance = AVSpeechUtterance(string: text)
        utterance.voice = AVSpeechSynthesisVoice(language: Locale.current.languageCode ?? "en")
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate
        
        synthesizer.speak(utterance)
    }
    
    func stop() {
        synthesizer.stopSpeaking(at: .immediate)
    }
}
```

---

## 4. UI Integration

### 4.1 Voice Input Button

**Location**: Bottom sheet in SearchScreen, navigation controls in NavigationActivity

**States**:
- Idle: Microphone icon, gray
- Listening: Pulsing red animation, "Listening..."
- Processing: Spinner, "Processing..."
- Speaking: Sound wave animation, response text
- Error: Red X icon, error message

**Free Tier UI**:
- Manual tap to activate
- Simple transcript display
- Basic feedback ("Command recognized")

**Plus/Pro Tier UI**:
- Wake word toggle in settings
- Conversation history display
- Multi-turn context visualization
- Rich response cards (e.g., search results, route alternatives)

### 4.2 Voice Feedback Overlay

**During Navigation**:
- Minimal overlay showing voice state
- Auto-dismiss after command execution
- Option to show/hide transcript
- Visual confirmation of actions ("Route updated", "Muted")

---

## 5. Permissions & Setup

### 5.1 Android Permissions

**Manifest**:
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" /> <!-- Plus/Pro cloud API -->

<!-- Optional: Wake word background listening -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
```

**Runtime Permission Request**:
- Request `RECORD_AUDIO` on first voice activation
- Explain why permission is needed
- Fallback to text input if denied

### 5.2 iOS Permissions

**Info.plist**:
```xml
<key>NSSpeechRecognitionUsageDescription</key>
<string>GemNav uses speech recognition to enable hands-free navigation commands.</string>

<key>NSMicrophoneUsageDescription</key>
<string>GemNav needs microphone access for voice commands during navigation.</string>
```

**Authorization Request**:
- Request `SFSpeechRecognizer` authorization
- Request microphone access
- Handle denial gracefully

---

## 6. Error Handling

### 6.1 Common Errors

**No Internet (Plus/Pro)**:
- Fallback to on-device recognition (degraded capability)
- Notify user: "Voice features limited without internet"

**Speech Recognition Unavailable**:
- Check device compatibility
- Prompt to enable system speech recognition
- Offer text input alternative

**Microphone Permission Denied**:
- Show permission rationale
- Guide user to settings
- Disable voice features, show text input only

**Gemini API Errors**:
- Retry with exponential backoff
- Fallback to basic pattern matching for common commands
- Notify user if service is down

**Wake Word Detection Failure** (Plus/Pro):
- Disable wake word automatically
- Notify user
- Fall back to manual activation

### 6.2 Graceful Degradation

**Tier Downgrade During Session**:
- If Plus/Pro subscription expires during use, continue current session
- Disable advanced features for next session
- Notify user of tier change

**Partial Recognition**:
- If confidence < 0.6, ask for clarification
- Offer alternative interpretations
- Allow user to correct via text

---

## 7. Testing Strategy

### 7.1 Unit Tests

- CommandParser with various transcript inputs
- CommandExecutor tier permission checks
- VoiceResponseService speak/interrupt logic
- State machine transitions in VoiceCommandManager

### 7.2 Integration Tests

- End-to-end voice command flow (recognize → parse → execute → respond)
- Tier-based feature gating
- Multi-turn conversation context (Plus/Pro)
- Error recovery scenarios

### 7.3 Manual Testing

**Noisy Environments**:
- Test recognition accuracy in car, outdoors, busy areas
- Validate noise cancellation

**Accents & Languages**:
- Test with various English accents
- Validate multilingual support (Plus/Pro)

**Hands-Free During Driving**:
- Test wake word reliability
- Measure response latency
- Verify no UI distraction

---

## 8. Performance Considerations

**Latency Targets**:
- Wake word detection: < 500ms
- Speech recognition start: < 200ms
- Command parsing: < 1s (Free), < 2s (Plus/Pro with cloud)
- Command execution: Varies by action
- TTS response: < 500ms to start speaking

**Battery Impact**:
- Wake word detection: Optimize for low power consumption
- Disable wake word when not navigating (Plus/Pro setting)
- Use on-device recognition where possible

**Data Usage**:
- Plus/Pro cloud API: ~50-100 KB per voice command (audio + API calls)
- Optimize by using compressed audio formats
- Cache common command responses

---

## 9. Privacy & Security

**Data Handling**:
- Free tier: All processing on-device, no audio leaves phone
- Plus/Pro: Audio sent to Google Cloud Speech-to-Text (encrypted)
- No voice data stored on servers (ephemeral processing only)
- User can disable cloud processing in settings (degrades to Free capabilities)

**Transparency**:
- Privacy policy explains voice data usage
- Clear tier-based privacy differences
- Option to review and delete voice history (Plus/Pro)

---

## 10. Deliverables

### 10.1 Specification (This Document)
- ~1,100 lines
- Comprehensive voice command architecture

### 10.2 Android Implementation
- `VoiceCommandManager.kt` (~250 lines)
- `AndroidSpeechRecognitionService.kt` (~200 lines)
- `WakeWordDetector.kt` (~150 lines, Plus/Pro)
- `CommandParser.kt` (~200 lines)
- `CommandExecutor.kt` (~300 lines)
- `AndroidVoiceResponseService.kt` (~100 lines)
- UI components for voice input button and feedback (~150 lines)

**Total Android**: ~1,350 lines

### 10.3 iOS Implementation
- `VoiceCommandManager.swift` (~250 lines)
- `IOSSpeechRecognitionService.swift` (~200 lines)
- `WakeWordDetector.swift` (~150 lines, Plus/Pro)
- `CommandParser.swift` (~200 lines)
- `CommandExecutor.swift` (~300 lines)
- `IOSVoiceResponseService.swift` (~100 lines)
- SwiftUI components for voice input and feedback (~150 lines)

**Total iOS**: ~1,350 lines

### 10.4 Shared/Common
- Command definitions and models (cross-platform) (~100 lines)
- Gemini prompts for voice parsing (~50 lines)

**Total Shared**: ~150 lines

### 10.5 Documentation
- Updated STATUS.md
- Updated HANDOFF.md
- MP-016 summary

---

## 11. Implementation Phases

### Phase 1: Core Voice Recognition (Android)
- Implement VoiceCommandManager, SpeechRecognitionService
- Basic command parsing (Free tier level)
- Simple TTS responses
- UI integration (voice button in SearchScreen)

### Phase 2: Gemini Integration
- CommandParser with Gemini Nano (Free)
- CommandParser with Gemini Cloud (Plus/Pro)
- Advanced command handling
- Multi-turn context (Plus/Pro)

### Phase 3: Wake Word Detection (Plus/Pro)
- WakeWordDetector implementation
- Background listening service
- Settings integration
- Battery optimization

### Phase 4: iOS Implementation
- Port all Android components to iOS
- Platform-specific optimizations
- Cross-platform testing

### Phase 5: Testing & Refinement
- Comprehensive testing suite
- Real-world validation
- Performance tuning
- Bug fixes

---

## 12. Future Enhancements (Post-MP-016)

- Custom wake word personalization
- Voice shortcuts (user-defined phrases)
- Multi-language support beyond English
- Voice profiles for multiple drivers
- Integration with car infotainment systems (Android Auto, CarPlay)
- Offline command execution (cached AI models)

---

**END OF MP-016 SPECIFICATION**
