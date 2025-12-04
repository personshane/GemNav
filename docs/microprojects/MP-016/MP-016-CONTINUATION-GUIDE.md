# MP-016: Voice Command System - Work Continuation Guide

**Created**: 2025-11-22  
**Last Updated**: 2025-11-22  
**Current Phase**: Implementation (DI Complete, Voice UI Pending)

---

## Quick Status Overview

### ✓ COMPLETED
- **MP-016 Specification** (787 lines): Voice command system design across all tiers
- **MP-016-B Specification** (1,688 lines): ViewModel integration for voice commands
- **MP-016-C Implementation** (472 lines): Dependency injection setup (Android Hilt + iOS manual DI)
- **Core Voice Components** (1,304 lines implemented):
  - VoiceCommandManager.kt (244 lines)
  - CommandParser.kt (284 lines)
  - CommandExecutor.kt (279 lines)
  - AndroidSpeechRecognitionService.kt (136 lines)
  - AndroidVoiceResponseService.kt (99 lines)
  - WakeWordDetector.kt (118 lines)
  - VoiceCommands.kt (89 lines)
  - SpeechRecognitionService.kt (55 lines interface)

### 🚧 IN PROGRESS / PENDING
1. **Voice UI Components** (MP-016-D) - NOT STARTED
2. **Microphone Permissions** (MP-016-E) - NOT STARTED
3. **iOS Voice Components** - PARTIAL (interfaces exist, implementations needed)
4. **Integration Testing** - NOT STARTED
5. **Wake Word Integration** - BASIC STRUCTURE (needs testing)

---

## Where to Start: Three Options

### OPTION 1: Voice UI Components (MP-016-D) ⭐ RECOMMENDED
**Why**: Users need UI to interact with voice system. DI is ready, components are wired, now need visual layer.

**What to Build**:
1. **Android**:
   - VoiceButton.kt: Composable with mic icon, press-to-talk, animated states
   - VoiceFeedbackOverlay.kt: Full-screen overlay showing listening/processing/error states
   - VoicePermissionDialog.kt: Request microphone permission with rationale
   - WakeWordIndicator.kt: Visual indicator when wake word is active (Plus/Pro)

2. **iOS**:
   - VoiceButton.swift: SwiftUI view with mic icon, animations
   - VoiceFeedbackOverlay.swift: SwiftUI full-screen overlay
   - VoicePermissionView.swift: Permission request UI
   - WakeWordIndicator.swift: Wake word active indicator

**Files to Create**:
```
Android:
C:\Users\perso\GemNav\android\app\voice\ui\VoiceButton.kt
C:\Users\perso\GemNav\android\app\voice\ui\VoiceFeedbackOverlay.kt
C:\Users\perso\GemNav\android\app\voice\ui\VoicePermissionDialog.kt
C:\Users\perso\GemNav\android\app\voice\ui\WakeWordIndicator.kt

iOS:
C:\Users\perso\GemNav\ios\GemNav\Voice\UI\VoiceButton.swift
C:\Users\perso\GemNav\ios\GemNav\Voice\UI\VoiceFeedbackOverlay.swift
C:\Users\perso\GemNav\ios\GemNav\Voice\UI\VoicePermissionView.swift
C:\Users\perso\GemNav\ios\GemNav\Voice\UI\WakeWordIndicator.swift
```

**Design Specs to Reference**:
- Read MP-016-voice-commands-spec.md sections 4.1-4.4 (UI/UX Requirements)
- Voice states: Idle, Listening, Processing, Speaking, Error
- Tier-specific UI (Free: button only, Plus/Pro: button + wake word indicator)

---

### OPTION 2: Microphone Permissions (MP-016-E)
**Why**: Voice features require microphone access. Handle permissions before voice UI is fully integrated.

**What to Build**:
1. **Android**:
   - MicrophonePermissionManager.kt: Runtime permission handling
   - Permission flow: Check → Request → Handle denial → Settings deep link
   - Integration with VoiceCommandManager

2. **iOS**:
   - MicrophonePermissionManager.swift: AVAudioSession + Speech permissions
   - Info.plist keys: NSMicrophoneUsageDescription, NSSpeechRecognitionUsageDescription

**Files to Create**:
```
Android:
C:\Users\perso\GemNav\android\app\voice\permissions\MicrophonePermissionManager.kt

iOS:
C:\Users\perso\GemNav\ios\GemNav\Voice\Permissions\MicrophonePermissionManager.swift
```

**Implementation Notes**:
- Android: Check android.permission.RECORD_AUDIO, use ActivityCompat.requestPermissions
- iOS: Check AVAudioSession.recordPermission, SFSpeechRecognizer.authorizationStatus
- Graceful degradation: Disable voice features if permission denied
- Settings deep link: Guide users to app settings if permanently denied

---

### OPTION 3: iOS Voice Component Implementations
**Why**: Android voice components are implemented, iOS needs equivalents.

**What to Build**:
1. IOSSpeechRecognitionService.swift: Using SFSpeechRecognizer
2. IOSVoiceResponseService.swift: Using AVSpeechSynthesizer
3. IOSWakeWordDetector.swift: Platform-specific wake word implementation

**Files to Create**:
```
C:\Users\perso\GemNav\ios\GemNav\Voice\IOSSpeechRecognitionService.swift
C:\Users\perso\GemNav\ios\GemNav\Voice\IOSVoiceResponseService.swift
C:\Users\perso\GemNav\ios\GemNav\Voice\IOSWakeWordDetector.swift
```

**Reference Android Implementations**:
- Read C:\Users\perso\GemNav\android\app\voice\AndroidSpeechRecognitionService.kt
- Read C:\Users\perso\GemNav\android\app\voice\AndroidVoiceResponseService.kt
- Read C:\Users\perso\GemNav\android\app\voice\WakeWordDetector.kt

---

## Critical Files to Review Before Starting

### 1. Voice Command System Design
```
C:\Users\perso\GemNav\docs\MP-016-voice-commands-spec.md (787 lines)
```
**Read**: Sections 2 (Tier Capabilities), 4 (UI/UX), 5 (Technical Architecture)

### 2. ViewModel Integration
```
C:\Users\perso\GemNav\docs\MP-016-B-viewmodel-integration-spec.md (1,688 lines)
```
**Read**: Sections showing how voice commands flow to ViewModels

### 3. Dependency Injection Setup
```
C:\Users\perso\GemNav\android\app\di\ServiceModule.kt
C:\Users\perso\GemNav\android\app\core\TierManager.kt
C:\Users\perso\GemNav\ios\GemNav\Core\DependencyContainer.swift
```
**Understand**: How VoiceCommandManager and services are wired

### 4. Existing Voice Components
```
Android:
C:\Users\perso\GemNav\android\app\voice\VoiceCommandManager.kt
C:\Users\perso\GemNav\android\app\voice\CommandExecutor.kt
C:\Users\perso\GemNav\android\app\voice\VoiceCommands.kt
```

---

## Typical MP-016 Work Session Flow

1. **Start**: Read STATUS.md (last 20 lines) + HANDOFF.md (last 50 lines)
2. **Choose**: Pick Option 1, 2, or 3 above
3. **Review**: Read relevant spec sections from MP-016 or MP-016-B
4. **Implement**: Create files in 25-30 line chunks
5. **Update**: Append to STATUS.md with new component details
6. **Commit**: Git add + commit + push
7. **Handoff**: Update HANDOFF.md with completion summary

---

## Voice System Architecture Overview

```
User Input (Voice/Text)
    ↓
VoiceCommandManager
    ↓
SpeechRecognitionService → Transcript
    ↓
CommandParser (Gemini AI) → VoiceCommand
    ↓
CommandExecutor → Routes to appropriate ViewModel
    ↓
    ├─→ NavigationViewModel (navigate, cancel, ETA, etc.)
    ├─→ SearchViewModel (search along route, POI finder)
    └─→ HomeViewModel (destination entry)
    ↓
VoiceResponseService (TTS) → Speaks result to user
```

---

## Tier-Specific Feature Matrix

| Feature | Free | Plus | Pro |
|---------|------|------|-----|
| Voice Input | ✓ Manual | ✓ Manual + Wake Word | ✓ Manual + Wake Word |
| Speech Recognition | On-device | Cloud | Cloud |
| Gemini AI | Nano (on-device) | Cloud API | Cloud API |
| Wake Word ("Hey GemNav") | ✗ | ✓ | ✓ |
| Multi-turn Conversations | ✗ | ✓ | ✓ |
| Complex Queries | ✗ | ✓ | ✓ |
| Truck Commands | ✗ | ✗ | ✓ |

---

## Key Design Decisions to Remember

1. **Tier Gating**: All voice features check `TierManager.hasFeature(Feature.*)` before execution
2. **Privacy First**: Free tier uses on-device processing only (no cloud API calls)
3. **Graceful Degradation**: If microphone permission denied, show text input fallback
4. **State Management**: VoiceCommandManager maintains VoiceState (Idle, Listening, Processing, Speaking, Error)
5. **Error Handling**: All voice errors return user-friendly messages via VoiceResponseService
6. **Wake Word**: Optional feature for Plus/Pro, can be disabled in settings

---

## Testing Checklist (When Ready)

- [ ] Free tier: Manual voice input → Gemini Nano → Basic commands
- [ ] Plus tier: Wake word activation → Cloud AI → Complex queries
- [ ] Pro tier: Truck-specific commands (POI finder, restrictions)
- [ ] Permission flow: Request → Denial → Settings deep link
- [ ] Error cases: No internet (Plus/Pro), speech recognition errors, Gemini timeouts
- [ ] Voice feedback: All responses spoken via TTS
- [ ] UI animations: Button states, feedback overlay transitions

---

## Quick Start Command

```bash
# Review current state
Read C:\Users\perso\GemNav\STATUS.md (last 20 lines)
Read C:\Users\perso\GemNav\HANDOFF.md (last 50 lines)

# Pick your option
Option 1: "Start MP-016-D: Voice UI Components"
Option 2: "Start MP-016-E: Microphone Permissions"
Option 3: "Start MP-016-iOS: Voice Service Implementations"
```

---

## Questions to Answer During Implementation

1. **Voice Button**: Should it be floating action button or integrated in navigation bar?
2. **Feedback Overlay**: Full-screen modal or bottom sheet?
3. **Wake Word**: Should indicator be persistent or appear only when active?
4. **Animations**: Pulse effect while listening? Waveform visualization?
5. **Error States**: Toast notification or full overlay message?

Refer to MP-016-voice-commands-spec.md Section 4 for design guidance.

---

**Next Steps**: Choose Option 1, 2, or 3 above and begin implementation. Update STATUS.md and HANDOFF.md after completion.
