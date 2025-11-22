import Foundation
import Speech
import AVFoundation

class IOSSpeechRecognitionService: SpeechRecognitionService {
    private let tier: SubscriptionTier
    private var speechRecognizer: SFSpeechRecognizer?
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest?
    private var recognitionTask: SFSpeechRecognitionTask?
    private let audioEngine = AVAudioEngine()
    
    private var onTranscriptListener: ((String) -> Void)?
    private var onErrorListener: ((SpeechRecognitionError) -> Void)?
    
    init(tier: SubscriptionTier) {
        self.tier = tier
        self.speechRecognizer = SFSpeechRecognizer(locale: Locale.current)
        requestAuthorization()
    }
    
    // MARK: - Authorization
    
    private func requestAuthorization() {
        SFSpeechRecognizer.requestAuthorization { [weak self] status in
            switch status {
            case .authorized:
                break
            case .denied, .restricted, .notDetermined:
                self?.onErrorListener?(.permissionDenied)
            @unknown default:
                self?.onErrorListener?(.unknown)
            }
        }
    }
    
    // MARK: - SpeechRecognitionService Protocol
    
    func startListening() throws {
        // Cancel any ongoing task
        recognitionTask?.cancel()
        recognitionTask = nil
        
        // Check authorization
        guard SFSpeechRecognizer.authorizationStatus() == .authorized else {
            throw SpeechRecognitionError.permissionDenied
        }
        
        // Check recognizer availability
        guard let speechRecognizer = speechRecognizer, speechRecognizer.isAvailable else {
            throw SpeechRecognitionError.serviceUnavailable
        }
        
        // Configure audio session
        let audioSession = AVAudioSession.sharedInstance()
        try audioSession.setCategory(.record, mode: .measurement, options: .duckOthers)
        try audioSession.setActive(true, options: .notifyOthersOnDeactivation)
        
        // Create recognition request
        recognitionRequest = SFSpeechAudioBufferRecognitionRequest()
        guard let recognitionRequest = recognitionRequest else {
            throw SpeechRecognitionError.unknown
        }
        
        // Configure request
        recognitionRequest.shouldReportPartialResults = true
        
        // Tier-based configuration
        if tier.allowsAdvancedVoice {
            // Plus/Pro: Use cloud recognition for better accuracy
            recognitionRequest.requiresOnDeviceRecognition = false
        } else {
            // Free: Use on-device recognition when available
            if #available(iOS 13.0, *) {
                recognitionRequest.requiresOnDeviceRecognition = true
            }
        }
        
        // Start recognition task
        recognitionTask = speechRecognizer.recognitionTask(with: recognitionRequest) { [weak self] result, error in
            guard let self = self else { return }
            
            if let result = result {
                let transcript = result.bestTranscription.formattedString
                
                // Report final results
                if result.isFinal {
                    self.onTranscriptListener?(transcript)
                    self.stopListening()
                }
            }
            
            if let error = error {
                self.handleError(error)
            }
        }
        
        // Configure audio input
        let inputNode = audioEngine.inputNode
        let recordingFormat = inputNode.outputFormat(forBus: 0)
        
        inputNode.installTap(onBus: 0, bufferSize: 1024, format: recordingFormat) { [weak self] buffer, _ in
            self?.recognitionRequest?.append(buffer)
        }
        
        // Start audio engine
        audioEngine.prepare()
        try audioEngine.start()
    }
    
    func stopListening() {
        // Stop audio engine
        audioEngine.stop()
        audioEngine.inputNode.removeTap(onBus: 0)
        
        // End recognition
        recognitionRequest?.endAudio()
        recognitionTask?.cancel()
        
        recognitionRequest = nil
        recognitionTask = nil
    }
    
    func setOnTranscriptListener(_ listener: @escaping (String) -> Void) {
        onTranscriptListener = listener
    }
    
    func setOnErrorListener(_ listener: @escaping (SpeechRecognitionError) -> Void) {
        onErrorListener = listener
    }
    
    // MARK: - Error Handling
    
    private func handleError(_ error: Error) {
        let speechError: SpeechRecognitionError
        
        if let sfError = error as? SFError {
            switch sfError.code {
            case .requestFailed:
                speechError = .serviceUnavailable
            default:
                speechError = .unknown
            }
        } else {
            let nsError = error as NSError
            switch nsError.code {
            case 1100: // kAudioSessionErrorCodeCannotInterruptOthersError
                speechError = .audioError
            case 203: // Recognition timeout
                speechError = .noMatch
            default:
                speechError = .unknown
            }
        }
        
        onErrorListener?(speechError)
        stopListening()
    }
    
    // MARK: - Cleanup
    
    deinit {
        stopListening()
    }
}
