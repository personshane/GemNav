import Foundation
import AVFoundation

/// iOS implementation of voice response using AVSpeechSynthesizer
/// Provides text-to-speech functionality for voice feedback
class IOSVoiceResponseService: VoiceResponseService {
    
    private let synthesizer = AVSpeechSynthesizer()
    private var utteranceListeners: [(Bool) -> Void] = []
    private var isInitialized = false
    
    init() {
        setupSynthesizer()
    }
    
    /// Setup speech synthesizer with delegate
    private func setupSynthesizer() {
        synthesizer.delegate = self
        
        // Configure audio session for playback
        do {
            let audioSession = AVAudioSession.sharedInstance()
            try audioSession.setCategory(.playback, mode: .voicePrompt, options: .duckOthers)
            try audioSession.setActive(true)
            isInitialized = true
        } catch {
            // Error setting up audio session
            isInitialized = false
        }
    }
    
    func speak(text: String, interrupt: Bool) {
        guard isInitialized, !text.isEmpty else { return }
        
        // Stop current speech if interrupt is true
        if interrupt && synthesizer.isSpeaking {
            synthesizer.stopSpeaking(at: .immediate)
        }
        
        // Create utterance
        let utterance = AVSpeechUtterance(string: text)
        
        // Configure voice and speech parameters
        utterance.voice = AVSpeechSynthesisVoice(language: Locale.current.identifier)
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate
        utterance.pitchMultiplier = 1.0
        utterance.volume = 1.0
        
        // Speak the utterance
        synthesizer.speak(utterance)
    }
    
    func stop() {
        if synthesizer.isSpeaking {
            synthesizer.stopSpeaking(at: .immediate)
        }
    }
    
    func shutdown() {
        stop()
        utteranceListeners.removeAll()
        
        // Deactivate audio session
        do {
            try AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
        } catch {
            // Error deactivating audio session
        }
    }
    
    /// Add listener for utterance completion
    func addUtteranceListener(_ listener: @escaping (Bool) -> Void) {
        utteranceListeners.append(listener)
    }
    
    /// Remove utterance listener
    func removeUtteranceListener(_ listener: @escaping (Bool) -> Void) {
        // Note: Swift doesn't support closure equality, so this is a simplified implementation
        // In production, would use identified closures or delegate pattern
        utteranceListeners.removeAll()
    }
}

// MARK: - AVSpeechSynthesizerDelegate
extension IOSVoiceResponseService: AVSpeechSynthesizerDelegate {
    
    func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didStart utterance: AVSpeechUtterance) {
        // TTS started speaking
    }
    
    func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) {
        // TTS finished speaking
        utteranceListeners.forEach { $0(true) }
    }
    
    func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didCancel utterance: AVSpeechUtterance) {
        // TTS was cancelled
        utteranceListeners.forEach { $0(false) }
    }
}
