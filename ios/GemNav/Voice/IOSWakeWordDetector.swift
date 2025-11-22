import Foundation
import Speech
import AVFoundation

/// Wake word detector for "Hey GemNav" on iOS
/// Only available for Plus/Pro tiers
/// Uses continuous speech recognition to detect wake phrase
class IOSWakeWordDetector {
    
    private let speechRecognizer: SFSpeechRecognizer?
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest?
    private var recognitionTask: SFSpeechRecognitionTask?
    private let audioEngine = AVAudioEngine()
    
    private var onWakeWordDetected: (() -> Void)?
    private var isRunning = false
    
    private let wakePhrase = "hey gemnav"
    private let wakePhraseVariations = ["hey gem nav", "hi gemnav", "hi gem nav"]
    
    init() {
        speechRecognizer = SFSpeechRecognizer(locale: Locale(identifier: "en-US"))
        requestAuthorization()
    }
    
    /// Request authorization for speech recognition
    private func requestAuthorization() {
        SFSpeechRecognizer.requestAuthorization { _ in }
    }
    
    /// Set callback for wake word detection
    func setOnWakeWordDetectedListener(listener: @escaping () -> Void) {
        onWakeWordDetected = listener
    }
    
    /// Start listening for wake word
    func start() {
        guard !isRunning else { return }
        guard let speechRecognizer = speechRecognizer, speechRecognizer.isAvailable else {
            return
        }
        
        // Cancel any existing task
        recognitionTask?.cancel()
        recognitionTask = nil
        
        // Configure audio session for background listening
        let audioSession = AVAudioSession.sharedInstance()
        do {
            try audioSession.setCategory(.record, mode: .measurement, options: [.duckOthers, .allowBluetooth])
            try audioSession.setActive(true, options: .notifyOthersOnDeactivation)
        } catch {
            return
        }
        
        // Create recognition request
        recognitionRequest = SFSpeechAudioBufferRecognitionRequest()
        guard let recognitionRequest = recognitionRequest else {
            return
        }
        
        // Use on-device recognition for privacy and efficiency
        recognitionRequest.requiresOnDeviceRecognition = true
        recognitionRequest.shouldReportPartialResults = true
        
        // Configure audio input
        let inputNode = audioEngine.inputNode
        let recordingFormat = inputNode.outputFormat(forBus: 0)
        
        inputNode.installTap(onBus: 0, bufferSize: 1024, format: recordingFormat) { buffer, _ in
            recognitionRequest.append(buffer)
        }
        
        // Start audio engine
        audioEngine.prepare()
        do {
            try audioEngine.start()
        } catch {
            return
        }
        
        // Start recognition task
        recognitionTask = speechRecognizer.recognitionTask(with: recognitionRequest) { [weak self] result, error in
            guard let self = self else { return }
            
            if let error = error {
                // Handle error by restarting if still active
                if self.isRunning {
                    self.restart()
                }
                return
            }
            
            if let result = result {
                let transcript = result.bestTranscription.formattedString.lowercased()
                
                // Check if wake phrase is detected
                if transcript.contains(self.wakePhrase) ||
                   self.wakePhraseVariations.contains(where: { transcript.contains($0) }) {
                    // Wake word detected!
                    self.onWakeWordDetected?()
                    
                    // Brief pause before restarting listening
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                        if self.isRunning {
                            self.restart()
                        }
                    }
                }
            }
        }
        
        isRunning = true
    }
    
    /// Stop listening for wake word
    func stop() {
        guard isRunning else { return }
        
        audioEngine.stop()
        audioEngine.inputNode.removeTap(onBus: 0)
        recognitionRequest?.endAudio()
        recognitionTask?.cancel()
        
        recognitionTask = nil
        recognitionRequest = nil
        isRunning = false
        
        // Deactivate audio session
        do {
            try AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
        } catch {
            // Error deactivating audio session
        }
    }
    
    /// Check if wake word detection is running
    func isActive() -> Bool {
        return isRunning
    }
    
    /// Restart wake word detection (used after detection or error)
    private func restart() {
        stop()
        // Small delay before restarting
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak self] in
            self?.start()
        }
    }
}
