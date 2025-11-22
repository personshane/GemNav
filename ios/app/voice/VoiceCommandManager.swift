import Foundation
import Combine

/**
 * Central coordinator for voice command functionality.
 * Manages speech recognition, command parsing, execution, and TTS responses.
 */
class VoiceCommandManager: ObservableObject {
    // Published state for SwiftUI
    @Published private(set) var voiceState: VoiceState = .idle
    @Published private(set) var lastTranscript: String?
    
    private let speechRecognitionService: SpeechRecognitionService
    private let commandParser: CommandParser
    private let commandExecutor: CommandExecutor
    private let voiceResponseService: VoiceResponseService
    private let wakeWordDetector: WakeWordDetector?
    private let tier: SubscriptionTier
    
    private var conversationContext: [ConversationTurn] = []
    private var isWakeWordEnabled = false
    private var cancellables = Set<AnyCancellable>()
    
    // MARK: - Initialization
    
    init(
        speechRecognitionService: SpeechRecognitionService,
        commandParser: CommandParser,
        commandExecutor: CommandExecutor,
        voiceResponseService: VoiceResponseService,
        wakeWordDetector: WakeWordDetector?,
        tier: SubscriptionTier
    ) {
        self.speechRecognitionService = speechRecognitionService
        self.commandParser = commandParser
        self.commandExecutor = commandExecutor
        self.voiceResponseService = voiceResponseService
        self.wakeWordDetector = wakeWordDetector
        self.tier = tier
        
        setupSpeechRecognition()
        
        if tier.allowsWakeWord {
            setupWakeWord()
        }
    }
    
    // MARK: - Setup
    
    private func setupSpeechRecognition() {
        speechRecognitionService.setOnTranscriptListener { [weak self] transcript in
            Task {
                await self?.handleTranscript(transcript)
            }
        }
        
        speechRecognitionService.setOnErrorListener { [weak self] error in
            self?.handleRecognitionError(error)
        }
    }
    
    private func setupWakeWord() {
        wakeWordDetector?.setOnWakeWordDetectedListener { [weak self] in
            guard let self = self else { return }
            if self.isWakeWordEnabled && self.voiceState == .idle {
                self.startListening(trigger: .wakeWord)
            }
        }
    }
    
    // MARK: - Voice Control
    
    func startListening(trigger: VoiceTrigger = .manual) {
        guard voiceState != .listening else { return }
        
        voiceState = .listening
        
        do {
            try speechRecognitionService.startListening()
        } catch {
            voiceState = .error(message: "Failed to start listening: \(error.localizedDescription)")
        }
    }
    
    func stopListening() {
        guard voiceState == .listening else { return }
        
        speechRecognitionService.stopListening()
        voiceState = .idle
    }
    
    // MARK: - Transcript Processing
    
    private func handleTranscript(_ transcript: String) async {
        await MainActor.run {
            lastTranscript = transcript
            voiceState = .processing
        }
        
        // Add to conversation context if Plus/Pro
        if tier.allowsAdvancedVoice {
            conversationContext.append(
                ConversationTurn(role: "user", content: transcript)
            )
        }
        
        // Parse command using Gemini
        let command = await commandParser.parse(transcript: transcript, context: conversationContext)
        
        // Execute command
        let result = await commandExecutor.execute(command: command)
        
        // Handle result
        await MainActor.run {
            handleCommandResult(result)
        }
    }
    
    private func handleCommandResult(_ result: CommandResult) {
        switch result {
        case .success(let message, _):
            speak(text: message, interrupt: true)
            if tier.allowsAdvancedVoice {
                conversationContext.append(
                    ConversationTurn(role: "assistant", content: message)
                )
            }
            
        case .error(let message):
            speak(text: message, interrupt: true)
            voiceState = .error(message: message)
            
        case .tierRestricted(let message):
            speak(text: message, interrupt: true)
            voiceState = .error(message: message)
            
        case .needsClarification(let question):
            speak(text: question, interrupt: true)
            // Keep listening for follow-up
            startListening(trigger: .continuation)
        }
    }
    
    // MARK: - Speech Output
    
    func speak(text: String, interrupt: Bool = false) {
        voiceState = .speaking(text: text)
        voiceResponseService.speak(text: text, interrupt: interrupt)
        
        // Auto-return to idle after speaking
        // Rough estimate: 50ms per character
        let delay = Double(text.count) * 0.05
        DispatchQueue.main.asyncAfter(deadline: .now() + delay) { [weak self] in
            guard let self = self else { return }
            if case .speaking = self.voiceState {
                self.voiceState = .idle
            }
        }
    }
    
    func cancelSpeech() {
        voiceResponseService.stop()
        if case .speaking = voiceState {
            voiceState = .idle
        }
    }
    
    // MARK: - Wake Word
    
    func setWakeWordEnabled(_ enabled: Bool) {
        guard tier.allowsWakeWord else { return }
        
        isWakeWordEnabled = enabled
        if enabled {
            wakeWordDetector?.start()
        } else {
            wakeWordDetector?.stop()
        }
    }
    
    // MARK: - Context Management
    
    func clearConversationContext() {
        conversationContext.removeAll()
    }
    
    // MARK: - Error Handling
    
    private func handleRecognitionError(_ error: SpeechRecognitionError) {
        let message: String
        
        switch error {
        case .noMatch:
            message = "I didn't catch that. Please try again."
        case .networkError:
            message = "Network error. Voice recognition unavailable."
        case .permissionDenied, .insufficientPermissions:
            message = "Microphone permission required for voice commands."
        case .serviceUnavailable:
            message = "Speech recognition service unavailable."
        case .recognizerBusy:
            message = "Speech recognizer is busy. Please try again."
        case .audioError:
            message = "Audio system error occurred."
        case .unknown:
            message = "Voice recognition error. Please try again."
        }
        
        voiceState = .error(message: message)
        speak(text: message, interrupt: true)
    }
    
    // MARK: - Cleanup
    
    func shutdown() {
        stopListening()
        wakeWordDetector?.stop()
        voiceResponseService.shutdown()
        cancellables.removeAll()
    }
    
    deinit {
        shutdown()
    }
}
