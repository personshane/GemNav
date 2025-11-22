import Foundation
import Porcupine

/**
 * Wake word detector for "Hey GemNav" using Porcupine SDK
 * Only available for Plus/Pro tiers
 *
 * Note: Requires Porcupine SDK dependency and access key
 */
class WakeWordDetector {
    private var porcupineManager: PorcupineManager?
    private var onWakeWordDetected: (() -> Void)?
    private var isRunning = false
    
    // MARK: - Initialization
    
    init() {}
    
    // MARK: - Setup
    
    func setOnWakeWordDetectedListener(_ listener: @escaping () -> Void) {
        onWakeWordDetected = listener
    }
    
    // MARK: - Start/Stop
    
    func start() {
        guard !isRunning else { return }
        
        do {
            // Initialize Porcupine with custom wake word
            // Note: Custom wake word model would be trained and included in bundle
            // For MVP, using built-in keyword as placeholder
            porcupineManager = try PorcupineManager(
                accessKey: ProcessInfo.processInfo.environment["PORCUPINE_ACCESS_KEY"] ?? "",
                keywordPath: Bundle.main.path(forResource: "hey_gemnav", ofType: "ppn"),
                sensitivity: 0.7, // 0.0 (least sensitive) to 1.0 (most sensitive)
                onDetection: { [weak self] _ in
                    // Wake word detected
                    self?.onWakeWordDetected?()
                },
                errorCallback: { [weak self] error in
                    // Handle error
                    print("Wake word detection error: \(error)")
                    self?.stop()
                }
            )
            
            try porcupineManager?.start()
            isRunning = true
            
        } catch {
            print("Failed to start wake word detection: \(error)")
            isRunning = false
        }
    }
    
    func stop() {
        guard isRunning else { return }
        
        porcupineManager?.stop()
        porcupineManager = nil
        isRunning = false
    }
    
    func isActive() -> Bool {
        return isRunning
    }
    
    // MARK: - Cleanup
    
    deinit {
        stop()
    }
}

/**
 * Alternative wake word detector using on-device model
 * For environments where Porcupine SDK is not available
 */
class SimpleWakeWordDetector {
    private var onWakeWordDetected: (() -> Void)?
    private var isRunning = false
    
    // Placeholder for simpler wake word detection
    // In production, would use CoreML or similar
    
    func setOnWakeWordDetectedListener(_ listener: @escaping () -> Void) {
        onWakeWordDetected = listener
    }
    
    func start() {
        guard !isRunning else { return }
        // Initialize simple wake word model
        isRunning = true
    }
    
    func stop() {
        guard isRunning else { return }
        // Stop wake word detection
        isRunning = false
    }
    
    func isActive() -> Bool {
        return isRunning
    }
}
