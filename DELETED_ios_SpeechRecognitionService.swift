import Foundation

// MARK: - Speech Recognition Service Protocol

protocol SpeechRecognitionService {
    func startListening() throws
    func stopListening()
    func setOnTranscriptListener(_ listener: @escaping (String) -> Void)
    func setOnErrorListener(_ listener: @escaping (SpeechRecognitionError) -> Void)
}

// MARK: - Speech Recognition Error

enum SpeechRecognitionError: Error {
    case noMatch
    case networkError
    case permissionDenied
    case insufficientPermissions
    case serviceUnavailable
    case recognizerBusy
    case audioError
    case unknown
    
    var localizedDescription: String {
        switch self {
        case .noMatch:
            return "No speech detected"
        case .networkError:
            return "Network error occurred"
        case .permissionDenied, .insufficientPermissions:
            return "Microphone permission required"
        case .serviceUnavailable:
            return "Speech recognition unavailable"
        case .recognizerBusy:
            return "Speech recognizer is busy"
        case .audioError:
            return "Audio system error"
        case .unknown:
            return "Unknown error occurred"
        }
    }
}
