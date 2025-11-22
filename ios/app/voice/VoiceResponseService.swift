import Foundation

protocol VoiceResponseService {
    func speak(text: String, interrupt: Bool)
    func stop()
    func shutdown()
}
