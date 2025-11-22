# iOS Service Layer Implementation

**Micro-Project**: MP-011  
**Platform**: iOS  
**Last Updated**: 2025-11-21

---

## Overview

Service layer for GemNav iOS app handling GPS tracking, navigation guidance, voice commands, and background execution using CoreLocation, AVFoundation, and iOS background modes.

---

## Architecture

```
GemNav/
├── Services/
│   ├── Location/
│   │   ├── LocationService.swift
│   │   └── LocationServiceImpl.swift
│   ├── Navigation/
│   │   ├── NavigationService.swift
│   │   ├── NavigationServiceImpl.swift
│   │   └── TurnByTurnManager.swift
│   ├── Voice/
│   │   ├── VoiceCommandService.swift
│   │   ├── VoiceCommandServiceImpl.swift
│   │   └── SpeechRecognitionManager.swift
│   └── Background/
│       └── BackgroundTaskManager.swift
```

---

## 1. LocationService

### Protocol

```swift
// Services/Location/LocationService.swift
import CoreLocation
import Combine

protocol LocationService: AnyObject {
    /// Start location tracking
    /// - Parameters:
    ///   - highAccuracy: Use GPS (true) or significant location changes (false)
    ///   - allowsBackgroundUpdates: Continue tracking in background
    func startTracking(highAccuracy: Bool, allowsBackgroundUpdates: Bool) -> Result<Void, Error>
    
    /// Stop location tracking
    func stopTracking() -> Result<Void, Error>
    
    /// Get current location (one-time)
    func getCurrentLocation() async -> Result<CLLocation, Error>
    
    /// Publisher for location updates
    var locationPublisher: AnyPublisher<CLLocation, Never> { get }
    
    /// Check if tracking is active
    var isTracking: Bool { get }
    
    /// Get last known location
    var lastKnownLocation: CLLocation? { get }
    
    /// Request location permissions
    func requestLocationPermission() async -> Bool
}
```

### Implementation

```swift
// Services/Location/LocationServiceImpl.swift
import CoreLocation
import Combine

final class LocationServiceImpl: NSObject, LocationService {
    
    // MARK: - Properties
    
    private let locationManager: CLLocationManager
    private let locationSubject = PassthroughSubject<CLLocation, Never>()
    private var trackingActive = false
    private var lastLocation: CLLocation?
    
    var locationPublisher: AnyPublisher<CLLocation, Never> {
        locationSubject.eraseToAnyPublisher()
    }
    
    var isTracking: Bool {
        trackingActive
    }
    
    var lastKnownLocation: CLLocation? {
        lastLocation
    }
    
    // MARK: - Initialization
    
    init(locationManager: CLLocationManager = CLLocationManager()) {
        self.locationManager = locationManager
        super.init()
        
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.distanceFilter = 10 // meters
        locationManager.pausesLocationUpdatesAutomatically = false
        locationManager.showsBackgroundLocationIndicator = true
    }
    
    // MARK: - Public Methods
    
    func startTracking(highAccuracy: Bool, allowsBackgroundUpdates: Bool) -> Result<Void, Error> {
        guard hasLocationPermission() else {
            return .failure(LocationError.permissionDenied)
        }
        
        if trackingActive {
            return .success(())
        }
        
        locationManager.desiredAccuracy = highAccuracy ? 
            kCLLocationAccuracyBest : 
            kCLLocationAccuracyHundredMeters
        
        locationManager.allowsBackgroundLocationUpdates = allowsBackgroundUpdates
        
        if highAccuracy {
            locationManager.startUpdatingLocation()
        } else {
            locationManager.startMonitoringSignificantLocationChanges()
        }
        
        trackingActive = true
        return .success(())
    }
    
    func stopTracking() -> Result<Void, Error> {
        locationManager.stopUpdatingLocation()
        locationManager.stopMonitoringSignificantLocationChanges()
        trackingActive = false
        return .success(())
    }
    
    func getCurrentLocation() async -> Result<CLLocation, Error> {
        guard hasLocationPermission() else {
            return .failure(LocationError.permissionDenied)
        }
        
        return await withCheckedContinuation { continuation in
            var observer: AnyCancellable?
            var timeoutTask: Task<Void, Never>?
            
            // Set up timeout
            timeoutTask = Task {
                try? await Task.sleep(nanoseconds: 10_000_000_000) // 10 seconds
                observer?.cancel()
                continuation.resume(returning: .failure(LocationError.timeout))
            }
            
            // Listen for location update
            observer = locationPublisher
                .first()
                .sink { location in
                    timeoutTask?.cancel()
                    continuation.resume(returning: .success(location))
                }
            
            // Request location
            locationManager.requestLocation()
        }
    }
    
    func requestLocationPermission() async -> Bool {
        let status = locationManager.authorizationStatus
        
        switch status {
        case .authorizedAlways, .authorizedWhenInUse:
            return true
        case .notDetermined:
            return await withCheckedContinuation { continuation in
                // Store continuation and request permission
                // Delegate will handle response
                locationManager.requestWhenInUseAuthorization()
                
                // For simplicity, return after short delay
                // In production, use delegate callback
                Task {
                    try? await Task.sleep(nanoseconds: 1_000_000_000)
                    let newStatus = locationManager.authorizationStatus
                    continuation.resume(returning: 
                        newStatus == .authorizedAlways || newStatus == .authorizedWhenInUse
                    )
                }
            }
        default:
            return false
        }
    }
    
    // MARK: - Private Methods
    
    private func hasLocationPermission() -> Bool {
        let status = locationManager.authorizationStatus
        return status == .authorizedAlways || status == .authorizedWhenInUse
    }
}

// MARK: - CLLocationManagerDelegate

extension LocationServiceImpl: CLLocationManagerDelegate {
    
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last else { return }
        
        lastLocation = location
        locationSubject.send(location)
    }
    
    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        print("Location error: \(error.localizedDescription)")
    }
    
    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        // Handle authorization changes
        let status = manager.authorizationStatus
        print("Location authorization changed: \(status.rawValue)")
    }
}

// MARK: - LocationError

enum LocationError: Error {
    case permissionDenied
    case timeout
    case notAvailable
}
```

---

## 2. NavigationService

### Protocol

```swift
// Services/Navigation/NavigationService.swift
import CoreLocation
import Combine

protocol NavigationService: AnyObject {
    /// Start turn-by-turn navigation
    func startNavigation(route: Route) async -> Result<Void, Error>
    
    /// Stop navigation
    func stopNavigation() async -> Result<Void, Error>
    
    /// Publisher for turn instructions
    var turnInstructionPublisher: AnyPublisher<TurnInstruction, Never> { get }
    
    /// Check if navigation is active
    var isNavigating: Bool { get }
    
    /// Get next turn instruction
    var nextTurn: TurnInstruction? { get }
    
    /// Simulate navigation (testing mode)
    func simulateNavigation(route: Route, speedMultiplier: Float) async -> Result<Void, Error>
}
```

### Implementation

```swift
// Services/Navigation/NavigationServiceImpl.swift
import CoreLocation
import Combine

final class NavigationServiceImpl: NavigationService {
    
    // MARK: - Properties
    
    private let locationService: LocationService
    private let voiceCommandService: VoiceCommandService
    private let turnByTurnManager: TurnByTurnManager
    
    private let turnSubject = PassthroughSubject<TurnInstruction, Never>()
    private var navigationActive = false
    private var currentRoute: Route?
    private var locationCancellable: AnyCancellable?
    private var simulationTask: Task<Void, Never>?
    
    var turnInstructionPublisher: AnyPublisher<TurnInstruction, Never> {
        turnSubject.eraseToAnyPublisher()
    }
    
    var isNavigating: Bool {
        navigationActive
    }
    
    var nextTurn: TurnInstruction? {
        guard let route = currentRoute,
              let location = locationService.lastKnownLocation else {
            return nil
        }
        return turnByTurnManager.getNextInstruction(for: route, at: location)
    }
    
    // MARK: - Initialization
    
    init(
        locationService: LocationService,
        voiceCommandService: VoiceCommandService,
        turnByTurnManager: TurnByTurnManager
    ) {
        self.locationService = locationService
        self.voiceCommandService = voiceCommandService
        self.turnByTurnManager = turnByTurnManager
    }
    
    // MARK: - Public Methods
    
    func startNavigation(route: Route) async -> Result<Void, Error> {
        if navigationActive {
            _ = await stopNavigation()
        }
        
        currentRoute = route
        navigationActive = true
        
        // Start location tracking with high accuracy and background updates
        let result = locationService.startTracking(
            highAccuracy: true,
            allowsBackgroundUpdates: true
        )
        
        guard case .success = result else {
            navigationActive = false
            currentRoute = nil
            return result
        }
        
        // Subscribe to location updates
        locationCancellable = locationService.locationPublisher
            .sink { [weak self] location in
                self?.processLocationUpdate(location)
            }
        
        return .success(())
    }
    
    func stopNavigation() async -> Result<Void, Error> {
        simulationTask?.cancel()
        simulationTask = nil
        locationCancellable?.cancel()
        locationCancellable = nil
        navigationActive = false
        currentRoute = nil
        
        return locationService.stopTracking()
    }
    
    func simulateNavigation(route: Route, speedMultiplier: Float) async -> Result<Void, Error> {
        currentRoute = route
        navigationActive = true
        
        simulationTask = Task {
            await turnByTurnManager.simulateRoute(route, speedMultiplier: speedMultiplier) { [weak self] instruction in
                guard let self = self else { return }
                
                self.turnSubject.send(instruction)
                
                Task {
                    await self.voiceCommandService.speak(text: instruction.text)
                }
            }
        }
        
        return .success(())
    }
    
    // MARK: - Private Methods
    
    private func processLocationUpdate(_ location: CLLocation) {
        guard let route = currentRoute else { return }
        
        if let instruction = turnByTurnManager.calculateNextInstruction(for: route, at: location) {
            turnSubject.send(instruction)
            
            // Speak instruction if close enough (within 200 meters)
            if instruction.distanceMeters < 200 {
                Task {
                    await voiceCommandService.speak(text: instruction.text)
                }
            }
        }
    }
}
```

### Turn-by-Turn Manager

```swift
// Services/Navigation/TurnByTurnManager.swift
import CoreLocation
import Foundation

final class TurnByTurnManager {
    
    // MARK: - Properties
    
    private var currentStepIndex = 0
    
    // MARK: - Public Methods
    
    func calculateNextInstruction(for route: Route, at location: CLLocation) -> TurnInstruction? {
        guard currentStepIndex < route.steps.count else { return nil }
        
        let currentStep = route.steps[currentStepIndex]
        let endLocation = CLLocation(
            latitude: currentStep.endLocation.latitude,
            longitude: currentStep.endLocation.longitude
        )
        
        let distance = location.distance(from: endLocation)
        
        // Move to next step if close to end point
        if distance < 20.0 {
            currentStepIndex += 1
            
            if currentStepIndex >= route.steps.count {
                return TurnInstruction(
                    text: "You have arrived at your destination",
                    type: .arrive,
                    distanceMeters: 0,
                    streetName: ""
                )
            }
        }
        
        guard let nextStep = route.steps[safe: currentStepIndex] else { return nil }
        
        return TurnInstruction(
            text: generateInstructionText(for: nextStep, distance: Int(distance)),
            type: nextStep.turnType,
            distanceMeters: Int(distance),
            streetName: nextStep.streetName ?? ""
        )
    }
    
    func getNextInstruction(for route: Route, at location: CLLocation) -> TurnInstruction? {
        return calculateNextInstruction(for: route, at: location)
    }
    
    func simulateRoute(
        _ route: Route,
        speedMultiplier: Float,
        onInstruction: @escaping (TurnInstruction) -> Void
    ) async {
        for (index, step) in route.steps.enumerated() {
            let distance = step.distanceMeters
            let duration = UInt64(Double(step.durationSeconds) * 1_000_000_000 / Double(speedMultiplier))
            
            let instruction = TurnInstruction(
                text: generateInstructionText(for: step, distance: distance),
                type: step.turnType,
                distanceMeters: distance,
                streetName: step.streetName ?? ""
            )
            
            onInstruction(instruction)
            
            try? await Task.sleep(nanoseconds: duration)
        }
        
        // Final arrival instruction
        let arrivalInstruction = TurnInstruction(
            text: "You have arrived at your destination",
            type: .arrive,
            distanceMeters: 0,
            streetName: ""
        )
        
        onInstruction(arrivalInstruction)
    }
    
    // MARK: - Private Methods
    
    private func generateInstructionText(for step: Route.Step, distance: Int) -> String {
        let action: String
        
        switch step.turnType {
        case .left:
            action = "Turn left"
        case .right:
            action = "Turn right"
        case .slightLeft:
            action = "Slight left"
        case .slightRight:
            action = "Slight right"
        case .sharpLeft:
            action = "Sharp left"
        case .sharpRight:
            action = "Sharp right"
        case .straight:
            action = "Continue straight"
        case .uTurn:
            action = "Make a U-turn"
        case .arrive:
            action = "Arrive"
        }
        
        let distanceText: String
        if distance < 1000 {
            distanceText = "in \(distance) meters"
        } else {
            distanceText = "in \(distance / 1000) kilometers"
        }
        
        if let streetName = step.streetName, !streetName.isEmpty {
            return "\(action) onto \(streetName) \(distanceText)"
        } else {
            return "\(action) \(distanceText)"
        }
    }
}

// MARK: - Array Extension

private extension Array {
    subscript(safe index: Int) -> Element? {
        return indices.contains(index) ? self[index] : nil
    }
}
```

---

## 3. VoiceCommandService

### Protocol

```swift
// Services/Voice/VoiceCommandService.swift
import Combine

protocol VoiceCommandService: AnyObject {
    /// Start listening for voice commands
    func startListening() async -> Result<Void, Error>
    
    /// Stop listening
    func stopListening() async -> Result<Void, Error>
    
    /// Publisher for recognized speech
    var recognizedSpeechPublisher: AnyPublisher<String, Never> { get }
    
    /// Text-to-speech output
    func speak(text: String) async
    
    /// Check if listening is active
    var isListening: Bool { get }
    
    /// Set language for recognition
    func setLanguage(_ languageCode: String)
}
```

### Implementation

```swift
// Services/Voice/VoiceCommandServiceImpl.swift
import AVFoundation
import Speech
import Combine

final class VoiceCommandServiceImpl: NSObject, VoiceCommandService {
    
    // MARK: - Properties
    
    private let speechRecognitionManager: SpeechRecognitionManager
    private let speechSynthesizer = AVSpeechSynthesizer()
    private var currentLanguage = "en-US"
    
    private let speechSubject = PassthroughSubject<String, Never>()
    
    var recognizedSpeechPublisher: AnyPublisher<String, Never> {
        speechSubject.eraseToAnyPublisher()
    }
    
    var isListening: Bool {
        speechRecognitionManager.isListening
    }
    
    // MARK: - Initialization
    
    init(speechRecognitionManager: SpeechRecognitionManager) {
        self.speechRecognitionManager = speechRecognitionManager
        super.init()
        
        setupAudioSession()
    }
    
    // MARK: - Public Methods
    
    func startListening() async -> Result<Void, Error> {
        let authorized = await requestSpeechRecognitionPermission()
        
        guard authorized else {
            return .failure(VoiceError.permissionDenied)
        }
        
        return await speechRecognitionManager.startRecognition { [weak self] text in
            self?.speechSubject.send(text)
        }
    }
    
    func stopListening() async -> Result<Void, Error> {
        return await speechRecognitionManager.stopRecognition()
    }
    
    func speak(text: String) async {
        return await withCheckedContinuation { continuation in
            let utterance = AVSpeechUtterance(string: text)
            utterance.voice = AVSpeechSynthesisVoice(language: currentLanguage)
            utterance.rate = AVSpeechUtteranceDefaultSpeechRate
            
            // Set delegate to detect completion
            var observer: NSObjectProtocol?
            observer = NotificationCenter.default.addObserver(
                forName: .AVSpeechSynthesizerDidFinishSpeechUtterance,
                object: speechSynthesizer,
                queue: .main
            ) { _ in
                if let observer = observer {
                    NotificationCenter.default.removeObserver(observer)
                }
                continuation.resume()
            }
            
            speechSynthesizer.speak(utterance)
        }
    }
    
    func setLanguage(_ languageCode: String) {
        currentLanguage = languageCode
        speechRecognitionManager.setLanguage(languageCode)
    }
    
    // MARK: - Private Methods
    
    private func setupAudioSession() {
        let audioSession = AVAudioSession.sharedInstance()
        try? audioSession.setCategory(.playAndRecord, mode: .default, options: [.defaultToSpeaker])
        try? audioSession.setActive(true)
    }
    
    private func requestSpeechRecognitionPermission() async -> Bool {
        return await withCheckedContinuation { continuation in
            SFSpeechRecognizer.requestAuthorization { status in
                continuation.resume(returning: status == .authorized)
            }
        }
    }
}

// MARK: - VoiceError

enum VoiceError: Error {
    case permissionDenied
    case recognitionFailed
    case notAvailable
}
```

### Speech Recognition Manager

```swift
// Services/Voice/SpeechRecognitionManager.swift
import Speech
import AVFoundation

final class SpeechRecognitionManager {
    
    // MARK: - Properties
    
    private var speechRecognizer: SFSpeechRecognizer?
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest?
    private var recognitionTask: SFSpeechRecognitionTask?
    private let audioEngine = AVAudioEngine()
    
    private(set) var isListening = false
    private var currentLanguage = Locale(identifier: "en-US")
    
    // MARK: - Public Methods
    
    func startRecognition(onResult: @escaping (String) -> Void) async -> Result<Void, Error> {
        guard !isListening else {
            return .success(())
        }
        
        speechRecognizer = SFSpeechRecognizer(locale: currentLanguage)
        
        guard let recognizer = speechRecognizer, recognizer.isAvailable else {
            return .failure(VoiceError.notAvailable)
        }
        
        do {
            recognitionRequest = SFSpeechAudioBufferRecognitionRequest()
            
            guard let request = recognitionRequest else {
                return .failure(VoiceError.recognitionFailed)
            }
            
            request.shouldReportPartialResults = true
            
            recognitionTask = recognizer.recognitionTask(with: request) { result, error in
                if let result = result {
                    let text = result.bestTranscription.formattedString
                    onResult(text)
                }
                
                if error != nil || result?.isFinal == true {
                    Task {
                        await self.stopRecognition()
                    }
                }
            }
            
            let inputNode = audioEngine.inputNode
            let recordingFormat = inputNode.outputFormat(forBus: 0)
            
            inputNode.installTap(onBus: 0, bufferSize: 1024, format: recordingFormat) { buffer, _ in
                request.append(buffer)
            }
            
            audioEngine.prepare()
            try audioEngine.start()
            
            isListening = true
            return .success(())
            
        } catch {
            return .failure(error)
        }
    }
    
    func stopRecognition() async -> Result<Void, Error> {
        guard isListening else {
            return .success(())
        }
        
        audioEngine.stop()
        audioEngine.inputNode.removeTap(onBus: 0)
        
        recognitionRequest?.endAudio()
        recognitionTask?.cancel()
        
        recognitionRequest = nil
        recognitionTask = nil
        isListening = false
        
        return .success(())
    }
    
    func setLanguage(_ languageCode: String) {
        currentLanguage = Locale(identifier: languageCode)
        speechRecognizer = SFSpeechRecognizer(locale: currentLanguage)
    }
}
```

---

## 4. Background Task Management

```swift
// Services/Background/BackgroundTaskManager.swift
import UIKit

final class BackgroundTaskManager {
    
    // MARK: - Properties
    
    private var backgroundTask: UIBackgroundTaskIdentifier = .invalid
    
    // MARK: - Public Methods
    
    func beginBackgroundTask(named name: String = "GemNavTask") {
        endBackgroundTask() // End any existing task
        
        backgroundTask = UIApplication.shared.beginBackgroundTask(withName: name) { [weak self] in
            self?.endBackgroundTask()
        }
    }
    
    func endBackgroundTask() {
        guard backgroundTask != .invalid else { return }
        
        UIApplication.shared.endBackgroundTask(backgroundTask)
        backgroundTask = .invalid
    }
    
    func registerBackgroundTasks() {
        // Register refresh tasks for iOS 13+
        if #available(iOS 13.0, *) {
            // BGTaskScheduler.shared.register(...)
            // Implement as needed for background refresh
        }
    }
}
```

---

## 5. Dependency Injection

```swift
// DI/ServiceContainer.swift
import Foundation
import CoreLocation

final class ServiceContainer {
    
    // MARK: - Singleton
    
    static let shared = ServiceContainer()
    
    // MARK: - Services
    
    lazy var locationService: LocationService = {
        LocationServiceImpl()
    }()
    
    lazy var turnByTurnManager: TurnByTurnManager = {
        TurnByTurnManager()
    }()
    
    lazy var speechRecognitionManager: SpeechRecognitionManager = {
        SpeechRecognitionManager()
    }()
    
    lazy var voiceCommandService: VoiceCommandService = {
        VoiceCommandServiceImpl(speechRecognitionManager: speechRecognitionManager)
    }()
    
    lazy var navigationService: NavigationService = {
        NavigationServiceImpl(
            locationService: locationService,
            voiceCommandService: voiceCommandService,
            turnByTurnManager: turnByTurnManager
        )
    }()
    
    lazy var backgroundTaskManager: BackgroundTaskManager = {
        BackgroundTaskManager()
    }()
    
    // MARK: - Initialization
    
    private init() {}
}
```

---

## 6. Info.plist Additions

```xml
<!-- Required permissions -->
<key>NSLocationAlwaysAndWhenInUseUsageDescription</key>
<string>GemNav needs your location to provide turn-by-turn navigation</string>

<key>NSLocationWhenInUseUsageDescription</key>
<string>GemNav needs your location to provide navigation</string>

<key>NSLocationAlwaysUsageDescription</key>
<string>GemNav needs background location access for continuous navigation</string>

<key>NSMicrophoneUsageDescription</key>
<string>GemNav needs microphone access for voice commands</string>

<key>NSSpeechRecognitionUsageDescription</key>
<string>GemNav uses speech recognition for voice commands</string>

<!-- Background modes -->
<key>UIBackgroundModes</key>
<array>
    <string>location</string>
    <string>audio</string>
</array>
```

---

## 7. Usage Example

```swift
// In ViewModel
final class NavigationViewModel: ObservableObject {
    
    @Published var currentLocation: CLLocation?
    @Published var turnInstruction: TurnInstruction?
    
    private let locationService: LocationService
    private let navigationService: NavigationService
    private let voiceCommandService: VoiceCommandService
    
    private var cancellables = Set<AnyCancellable>()
    
    init(
        locationService: LocationService = ServiceContainer.shared.locationService,
        navigationService: NavigationService = ServiceContainer.shared.navigationService,
        voiceCommandService: VoiceCommandService = ServiceContainer.shared.voiceCommandService
    ) {
        self.locationService = locationService
        self.navigationService = navigationService
        self.voiceCommandService = voiceCommandService
        
        setupBindings()
    }
    
    private func setupBindings() {
        locationService.locationPublisher
            .receive(on: DispatchQueue.main)
            .assign(to: &$currentLocation)
        
        navigationService.turnInstructionPublisher
            .receive(on: DispatchQueue.main)
            .assign(to: &$turnInstruction)
    }
    
    func startNavigation(route: Route) {
        Task {
            await navigationService.startNavigation(route: route)
        }
    }
    
    func startVoiceCommand() {
        Task {
            await voiceCommandService.startListening()
        }
    }
}
```

---

## Testing Strategy

### Unit Tests
- LocationService mock tests
- NavigationService turn calculation tests
- VoiceCommandService speech recognition mocks
- TurnByTurnManager distance calculation tests

### Integration Tests
- End-to-end navigation flow
- Location update handling
- Voice command recognition pipeline
- Background mode transitions

---

## Platform Differences Summary

| Feature | Android | iOS |
|---------|---------|-----|
| Location | FusedLocationProviderClient | CoreLocation CLLocationManager |
| Background | Foreground Service | Background Modes + BGTaskScheduler |
| Speech Recognition | SpeechRecognizer | SFSpeechRecognizer |
| Text-to-Speech | TextToSpeech | AVSpeechSynthesizer |
| Notifications | NotificationManager | UserNotifications |
| DI | Hilt/Dagger | Manual/Protocol-based |
