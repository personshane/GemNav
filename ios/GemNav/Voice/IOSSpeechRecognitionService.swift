import Foundation
import Speech
import AVFoundation

/// iOS implementation of speech recognition using SFSpeechRecognizer
/// Supports both on-device (Free tier) and cloud-based (Plus/Pro) recognition
class IOSSpeechRecognitionService: SpeechRecognitionService {
    
    private let speechRecognizer: SFSpeechRecognizer?
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest?
    private var recognitionTask: SFSpeechRecognitionTask?
    private let audioEngine = AVAudioEngine()
    private let tier: SubscriptionTier
    
    private var onTranscriptListener: ((String) -> Void)?
    private var onErrorListener: ((SpeechRecognitionError) -> Void)?
    
    init(tier: SubscriptionTier) {
        self.tier = tier
        self.speechRecognizer = SFSpeechRecognizer(locale: Locale.current)
        requestAuthorization()
    }
    
    /// Request authorization for speech recognition
    private func requestAuthorization() {
        SFSpeechRecognizer.requestAuthorization { authStatus in
            switch authStatus {
            case .authorized:
                // Authorization granted
                break
            case .denied, .restricted, .notDetermined:
                // Authorization not available
                DispatchQueue.main.async {
                    self.onErrorListener?(.insufficientPermissions)
                }
            @unknown default:
                break
            }
        }
    }
    
    func startListening() {
        // Check if speech recognition is available
        guard let speechRecognizer = speechRecognizer, speechRecognizer.isAvailable else {
            onErrorListener?(.serviceUnavailable)
            return
        }
        
        // Cancel any existing task
        if recognitionTask != nil {
            recognitionTask?.cancel()
            recognitionTask = nil
        }
        
        // Configure audio session
        let audioSession = AVAudioSession.sharedInstance()
        do {
            try audioSession.setCategory(.record, mode: .measurement, options: .duckOthers)
            try audioSession.setActive(true, options: .notifyOthersOnDeactivation)
        } catch {
            onErrorListener?(.audioError)
            return
        }
        
        // Create recognition request
        recognitionRequest = SFSpeechAudioBufferRecognitionRequest()
        guard let recognitionRequest = recognitionRequest else {
            onErrorListener?(.unknown)
            return
        }
        
        // Configure request based on tier
        if tier.allowsAdvancedVoice() {
            // Plus/Pro: Use cloud recognition for better accuracy
            recognitionRequest.requiresOnDeviceRecognition = false
        } else {
            // Free: Use on-device when available
            recognitionRequest.requiresOnDeviceRecognition = true
        }
        
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
            onErrorListener?(.audioError)
            return
        }
        
        // Start recognition task
        recognitionTask = speechRecognizer.recognitionTask(with: recognitionRequest) { [weak self] result, error in
            guard let self = self else { return }
            
            if let error = error {
                let speechError = self.mapError(error)
                self.onErrorListener?(speechError)
                return
            }
            
            if let result = result {
                let transcript = result.bestTranscription.formattedString
                
                if result.isFinal {
                    // Final result
                    self.onTranscriptListener?(transcript)
                    self.stopListening()
                }
                // Optionally handle partial results for UI feedback
            }
        }
    }
    
    func stopListening() {
        audioEngine.stop()
        audioEngine.inputNode.removeTap(onBus: 0)
        recognitionRequest?.endAudio()
        recognitionTask?.cancel()
        recognitionTask = nil
        recognitionRequest = nil
        
        // Deactivate audio session
        do {
            try AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
        } catch {
            // Error deactivating audio session
        }
    }
    
    func setOnTranscriptListener(listener: @escaping (String) -> Void) {
        onTranscriptListener = listener
    }
    
    func setOnErrorListener(listener: @escaping (SpeechRecognitionError) -> Void) {
        onErrorListener = listener
    }
    
    /// Map iOS speech recognition errors to our error type
    private func mapError(_ error: Error) -> SpeechRecognitionError {
        let nsError = error as NSError
        
        // Check for specific SFSpeechRecognizer error codes
        if nsError.domain == "kLSRErrorDomain" {
            return .networkError
        }
        
        return .unknown
    }
}
