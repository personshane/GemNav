import Foundation
import AVFoundation

class IOSVoiceResponseService: NSObject, VoiceResponseService, AVSpeechSynthesizerDelegate {
    private let synthesizer = AVSpeechSynthesizer()
    private var utteranceCompletionHandlers: [String: (Bool) -> Void] = [:]
    
    override init() {
        super.init()
        synthesizer.delegate = self
    }
    
    // MARK: - VoiceResponseService Protocol
    
    func speak(text: String, interrupt: Bool) {
        guard !text.isEmpty else { return }
        
        if interrupt {
            synthesizer.stopSpeaking(at: .immediate)
        }
        
        let utterance = AVSpeechUtterance(string: text)
        
        // Configure voice
        utterance.voice = AVSpeechSynthesisVoice(language: Locale.current.languageCode ?? "en")
        
        // Configure speech parameters
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate
        utterance.pitchMultiplier = 1.0
        utterance.volume = 1.0
        
        // Generate unique identifier for this utterance
        let utteranceId = UUID().uuidString
        
        synthesizer.speak(utterance)
    }
    
    func stop() {
        synthesizer.stopSpeaking(at: .immediate)
    }
    
    func shutdown() {
        synthesizer.stopSpeaking(at: .immediate)
        utteranceCompletionHandlers.removeAll()
    }
    
    // MARK: - Completion Handling
    
    func addUtteranceCompletionHandler(_ handler: @escaping (Bool) -> Void) {
        let id = UUID().uuidString
        utteranceCompletionHandlers[id] = handler
    }
    
    func removeUtteranceCompletionHandler(id: String) {
        utteranceCompletionHandlers.removeValue(forKey: id)
    }
    
    // MARK: - AVSpeechSynthesizerDelegate
    
    func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didStart utterance: AVSpeechUtterance) {
        // TTS started speaking
    }
    
    func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) {
        // TTS finished speaking
        utteranceCompletionHandlers.values.forEach { $0(true) }
    }
    
    func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didCancel utterance: AVSpeechUtterance) {
        // TTS was cancelled
        utteranceCompletionHandlers.values.forEach { $0(false) }
    }
}
