# iOS Permission & Error Handling UI

**Micro-Project**: MP-013 (iOS)  
**Component**: Permission & Error UI Layer  
**Dependencies**: ios_navigation_ui.md, ios_service_layer.md

---

## 1. PermissionManager (Enhanced)

```swift
// PermissionManager.swift
import Foundation
import CoreLocation
import AVFoundation
import UIKit

enum PermissionStatus {
    case unknown
    case granted
    case denied
    case deniedPermanently
}

class PermissionManager {
    
    static let shared = PermissionManager()
    
    private var locationManager: CLLocationManager?
    
    // MARK: - Location Permission
    
    func hasLocationPermission() -> Bool {
        let status = CLLocationManager.authorizationStatus()
        return status == .authorizedWhenInUse || status == .authorizedAlways
    }
    
    func locationPermissionStatus() -> PermissionStatus {
        let status = CLLocationManager.authorizationStatus()
        
        switch status {
        case .notDetermined:
            return .unknown
        case .authorizedWhenInUse, .authorizedAlways:
            return .granted
        case .denied, .restricted:
            return .deniedPermanently
        @unknown default:
            return .unknown
        }
    }
    
    func requestLocationPermission(completion: @escaping (Bool) -> Void) {
        let status = CLLocationManager.authorizationStatus()
        
        if status == .notDetermined {
            locationManager = CLLocationManager()
            locationManager?.requestWhenInUseAuthorization()
            
            // Observe authorization changes
            NotificationCenter.default.addObserver(
                forName: .CLLocationManagerDidChangeAuthorization,
                object: nil,
                queue: .main
            ) { [weak self] _ in
                completion(self?.hasLocationPermission() ?? false)
            }
        } else {
            completion(hasLocationPermission())
        }
    }
    
    // MARK: - Microphone Permission
    
    func hasMicrophonePermission() -> Bool {
        return AVAudioSession.sharedInstance().recordPermission == .granted
    }
    
    func microphonePermissionStatus() -> PermissionStatus {
        let status = AVAudioSession.sharedInstance().recordPermission
        
        switch status {
        case .undetermined:
            return .unknown
        case .granted:
            return .granted
        case .denied:
            return .deniedPermanently
        @unknown default:
            return .unknown
        }
    }
    
    func requestMicrophonePermission(completion: @escaping (Bool) -> Void) {
        AVAudioSession.sharedInstance().requestRecordPermission { granted in
            DispatchQueue.main.async {
                completion(granted)
            }
        }
    }
    
    // MARK: - Settings
    
    func openAppSettings() {
        guard let settingsURL = URL(string: UIApplication.openSettingsURLString) else {
            return
        }
        
        if UIApplication.shared.canOpenURL(settingsURL) {
            UIApplication.shared.open(settingsURL)
        }
    }
}

// MARK: - Notification Names
extension Notification.Name {
    static let CLLocationManagerDidChangeAuthorization = Notification.Name("CLLocationManagerDidChangeAuthorization")
}
```

---

## 2. Permission Alert Controllers

### PermissionAlertFactory.swift
```swift
// PermissionAlertFactory.swift
import UIKit

struct PermissionAlertFactory {
    
    // MARK: - Location Permission
    
    static func locationPermissionRationale(
        onAllow: @escaping () -> Void,
        onDeny: @escaping () -> Void
    ) -> UIAlertController {
        let alert = UIAlertController(
            title: "Location Permission Required",
            message: """
            GemNav needs access to your location to:
            
            • Provide turn-by-turn navigation
            • Show your position on the map
            • Calculate accurate routes
            • Detect when you've arrived
            
            Your location is only used while navigating and is not shared.
            """,
            preferredStyle: .alert
        )
        
        alert.addAction(UIAlertAction(title: "Allow", style: .default) { _ in
            onAllow()
        })
        
        alert.addAction(UIAlertAction(title: "Not Now", style: .cancel) { _ in
            onDeny()
        })
        
        return alert
    }
    
    static func locationPermissionDenied(
        onOpenSettings: @escaping () -> Void,
        onCancel: @escaping () -> Void
    ) -> UIAlertController {
        let alert = UIAlertController(
            title: "Location Access Required",
            message: """
            GemNav cannot provide navigation without location access.
            
            To enable location:
            1. Tap 'Open Settings' below
            2. Select 'Location'
            3. Choose 'While Using the App'
            4. Return to GemNav
            """,
            preferredStyle: .alert
        )
        
        alert.addAction(UIAlertAction(title: "Open Settings", style: .default) { _ in
            onOpenSettings()
        })
        
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel) { _ in
            onCancel()
        })
        
        return alert
    }
    
    // MARK: - Microphone Permission
    
    static func microphonePermissionRationale(
        onAllow: @escaping () -> Void,
        onDeny: @escaping () -> Void
    ) -> UIAlertController {
        let alert = UIAlertController(
            title: "Microphone Permission Required",
            message: """
            GemNav needs microphone access to:
            
            • Process voice commands
            • Enable hands-free navigation
            • Improve driving safety
            
            Voice data is processed on-device (Free tier) or securely via Gemini (Plus/Pro).
            """,
            preferredStyle: .alert
        )
        
        alert.addAction(UIAlertAction(title: "Allow", style: .default) { _ in
            onAllow()
        })
        
        alert.addAction(UIAlertAction(title: "Not Now", style: .cancel) { _ in
            onDeny()
        })
        
        return alert
    }
    
    static func microphonePermissionDenied(
        onOpenSettings: @escaping () -> Void,
        onCancel: @escaping () -> Void
    ) -> UIAlertController {
        let alert = UIAlertController(
            title: "Microphone Access Required",
            message: """
            Voice commands require microphone access.
            
            To enable microphone:
            1. Tap 'Open Settings' below
            2. Select 'Microphone'
            3. Enable access for GemNav
            4. Return to GemNav
            """,
            preferredStyle: .alert
        )
        
        alert.addAction(UIAlertAction(title: "Open Settings", style: .default) { _ in
            onOpenSettings()
        })
        
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel) { _ in
            onCancel()
        })
        
        return alert
    }
}
```

---

## 3. Error Alert Factory

### ErrorAlertFactory.swift
```swift
// ErrorAlertFactory.swift
import UIKit

struct ErrorAlertFactory {
    
    // MARK: - Navigation Errors
    
    static func noRouteFound(
        origin: String?,
        destination: String,
        onRetry: @escaping () -> Void,
        onCancel: @escaping () -> Void
    ) -> UIAlertController {
        let alert = UIAlertController(
            title: "No Route Found",
            message: """
            Unable to find a route between:
            
            From: \(origin ?? "Current location")
            To: \(destination)
            
            This may occur if:
            • Locations are not connected by roads
            • Destination is in a restricted area
            • Network connection is unavailable
            """,
            preferredStyle: .alert
        )
        
        alert.addAction(UIAlertAction(title: "Try Again", style: .default) { _ in
            onRetry()
        })
        
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel) { _ in
            onCancel()
        })
        
        return alert
    }
    
    static func gpsSignalLost(onDismiss: @escaping () -> Void) -> UIAlertController {
        let alert = UIAlertController(
            title: "GPS Signal Lost",
            message: """
            Navigation paused due to weak GPS signal.
            
            Tips to improve signal:
            • Move away from tall buildings
            • Ensure clear view of the sky
            • Check that location services are enabled
            
            Navigation will resume automatically when signal is restored.
            """,
            preferredStyle: .alert
        )
        
        alert.addAction(UIAlertAction(title: "OK", style: .default) { _ in
            onDismiss()
        })
        
        return alert
    }
    
    static func networkError(
        onRetry: @escaping () -> Void,
        onUseOffline: (() -> Void)? = nil
    ) -> UIAlertController {
        let alert = UIAlertController(
            title: "Network Connection Error",
            message: """
            Unable to connect to navigation services.
            
            Please check your internet connection and try again.
            """,
            preferredStyle: .alert
        )
        
        alert.addAction(UIAlertAction(title: "Retry", style: .default) { _ in
            onRetry()
        })
        
        if let onUseOffline = onUseOffline {
            alert.addAction(UIAlertAction(title: "Use Offline", style: .default) { _ in
                onUseOffline()
            })
        }
        
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))
        
        return alert
    }
    
    static func mapLoadError(onRetry: @escaping () -> Void) -> UIAlertController {
        let alert = UIAlertController(
            title: "Map Load Failed",
            message: """
            Failed to load map data.
            
            This may be due to:
            • Network connectivity issues
            • Map service unavailability
            • Insufficient storage
            """,
            preferredStyle: .alert
        )
        
        alert.addAction(UIAlertAction(title: "Retry", style: .default) { _ in
            onRetry()
        })
        
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))
        
        return alert
    }
    
    static func serviceUnavailable(
        serviceName: String,
        onDismiss: @escaping () -> Void
    ) -> UIAlertController {
        let alert = UIAlertController(
            title: "Service Unavailable",
            message: """
            \(serviceName) is currently unavailable.
            
            Please try again later. If the problem persists, check for app updates or contact support.
            """,
            preferredStyle: .alert
        )
        
        alert.addAction(UIAlertAction(title: "OK", style: .default) { _ in
            onDismiss()
        })
        
        return alert
    }
    
    static func tierLimitation(
        feature: String,
        requiredTier: String,
        onUpgrade: @escaping () -> Void,
        onCancel: @escaping () -> Void
    ) -> UIAlertController {
        let alert = UIAlertController(
            title: "Upgrade Required",
            message: """
            \(feature) requires GemNav \(requiredTier).
            
            Upgrade to unlock:
            • \(feature)
            • Advanced AI routing
            • Multi-stop navigation
            • And more!
            """,
            preferredStyle: .alert
        )
        
        alert.addAction(UIAlertAction(title: "Upgrade to \(requiredTier)", style: .default) { _ in
            onUpgrade()
        })
        
        alert.addAction(UIAlertAction(title: "Not Now", style: .cancel) { _ in
            onCancel()
        })
        
        return alert
    }
    
    static func voiceRecognitionError(
        message: String?,
        onRetry: @escaping () -> Void
    ) -> UIAlertController {
        let alert = UIAlertController(
            title: "Voice Command Failed",
            message: message ?? """
            Unable to process voice command.
            
            Please try again or use text input.
            """,
            preferredStyle: .alert
        )
        
        alert.addAction(UIAlertAction(title: "Try Again", style: .default) { _ in
            onRetry()
        })
        
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))
        
        return alert
    }
}
```

---

## 4. Inline Error Banner

### ErrorBannerView.swift
```swift
// ErrorBannerView.swift
import UIKit

class ErrorBannerView: UIView {
    
    private let containerView: UIView = {
        let view = UIView()
        view.layer.cornerRadius = 8
        return view
    }()
    
    private let iconImageView: UIImageView = {
        let imageView = UIImageView()
        imageView.contentMode = .scaleAspectFit
        imageView.tintColor = .white
        return imageView
    }()
    
    private let messageLabel: UILabel = {
        let label = UILabel()
        label.font = .systemFont(ofSize: 14, weight: .medium)
        label.textColor = .white
        label.numberOfLines = 0
        return label
    }()
    
    private let actionButton: UIButton = {
        let button = UIButton(type: .system)
        button.setTitleColor(.white, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 14, weight: .semibold)
        return button
    }()
    
    private var onAction: (() -> Void)?
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) not implemented")
    }
    
    private func setupUI() {
        addSubview(containerView)
        containerView.addSubview(iconImageView)
        containerView.addSubview(messageLabel)
        containerView.addSubview(actionButton)
        
        containerView.translatesAutoresizingMaskIntoConstraints = false
        iconImageView.translatesAutoresizingMaskIntoConstraints = false
        messageLabel.translatesAutoresizingMaskIntoConstraints = false
        actionButton.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            containerView.topAnchor.constraint(equalTo: topAnchor),
            containerView.leadingAnchor.constraint(equalTo: leadingAnchor),
            containerView.trailingAnchor.constraint(equalTo: trailingAnchor),
            containerView.bottomAnchor.constraint(equalTo: bottomAnchor),
            
            iconImageView.leadingAnchor.constraint(equalTo: containerView.leadingAnchor, constant: 12),
            iconImageView.centerYAnchor.constraint(equalTo: containerView.centerYAnchor),
            iconImageView.widthAnchor.constraint(equalToConstant: 24),
            iconImageView.heightAnchor.constraint(equalToConstant: 24),
            
            messageLabel.leadingAnchor.constraint(equalTo: iconImageView.trailingAnchor, constant: 12),
            messageLabel.centerYAnchor.constraint(equalTo: containerView.centerYAnchor),
            messageLabel.trailingAnchor.constraint(equalTo: actionButton.leadingAnchor, constant: -8),
            
            actionButton.trailingAnchor.constraint(equalTo: containerView.trailingAnchor, constant: -12),
            actionButton.centerYAnchor.constraint(equalTo: containerView.centerYAnchor)
        ])
        
        actionButton.addTarget(self, action: #selector(actionTapped), for: .touchUpInside)
        isHidden = true
    }
    
    func show(
        type: ErrorType,
        message: String? = nil,
        actionText: String? = nil,
        onAction: (() -> Void)? = nil
    ) {
        iconImageView.image = type.icon
        messageLabel.text = message ?? type.defaultMessage
        containerView.backgroundColor = type.backgroundColor
        
        if let actionText = actionText, let onAction = onAction {
            actionButton.setTitle(actionText, for: .normal)
            actionButton.isHidden = false
            self.onAction = onAction
        } else {
            actionButton.isHidden = true
            self.onAction = nil
        }
        
        isHidden = false
    }
    
    func hide() {
        isHidden = true
    }
    
    @objc private func actionTapped() {
        onAction?()
    }
}

enum ErrorType {
    case gpsWeak
    case offline
    case networkError
    case routeRecalculating
    
    var icon: UIImage? {
        let systemName: String
        switch self {
        case .gpsWeak: systemName = "location.slash"
        case .offline: systemName = "wifi.slash"
        case .networkError: systemName = "exclamationmark.triangle"
        case .routeRecalculating: systemName = "arrow.triangle.2.circlepath"
        }
        return UIImage(systemName: systemName)
    }
    
    var defaultMessage: String {
        switch self {
        case .gpsWeak:
            return "Weak GPS signal. Navigation may be inaccurate."
        case .offline:
            return "Offline mode. Limited functionality available."
        case .networkError:
            return "Network error. Some features unavailable."
        case .routeRecalculating:
            return "Route recalculating..."
        }
    }
    
    var backgroundColor: UIColor {
        switch self {
        case .gpsWeak: return .systemOrange
        case .offline: return .systemBlue
        case .networkError: return .systemRed
        case .routeRecalculating: return .systemBlue
        }
    }
}
```

---

## 5. Permission Request View Controller

### PermissionRequestViewController.swift
```swift
// PermissionRequestViewController.swift
import UIKit

class PermissionRequestViewController: UIViewController {
    
    private let permissionManager = PermissionManager.shared
    
    private let titleLabel: UILabel = {
        let label = UILabel()
        label.text = "Required Permissions"
        label.font = .systemFont(ofSize: 28, weight: .bold)
        label.textAlignment = .center
        return label
    }()
    
    private let subtitleLabel: UILabel = {
        let label = UILabel()
        label.text = "GemNav needs the following permissions to provide navigation"
        label.font = .systemFont(ofSize: 16)
        label.textColor = .secondaryLabel
        label.textAlignment = .center
        label.numberOfLines = 0
        return label
    }()
    
    private let locationPermissionCard = PermissionCardView(
        icon: UIImage(systemName: "location.fill")!,
        title: "Location",
        description: "Required for navigation and route tracking",
        isRequired: true
    )
    
    private let microphonePermissionCard = PermissionCardView(
        icon: UIImage(systemName: "mic.fill")!,
        title: "Microphone",
        description: "Optional for voice commands",
        isRequired: false
    )
    
    private let continueButton: UIButton = {
        let button = UIButton(type: .system)
        button.setTitle("Continue", for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 18, weight: .semibold)
        button.backgroundColor = .systemBlue
        button.setTitleColor(.white, for: .normal)
        button.layer.cornerRadius = 12
        button.isEnabled = false
        return button
    }()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        updatePermissionStatus()
    }
    
    private func setupUI() {
        view.backgroundColor = .systemBackground
        
        view.addSubview(titleLabel)
        view.addSubview(subtitleLabel)
        view.addSubview(locationPermissionCard)
        view.addSubview(microphonePermissionCard)
        view.addSubview(continueButton)
        
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        subtitleLabel.translatesAutoresizingMaskIntoConstraints = false
        locationPermissionCard.translatesAutoresizingMaskIntoConstraints = false
        microphonePermissionCard.translatesAutoresizingMaskIntoConstraints = false
        continueButton.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            titleLabel.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 40),
            titleLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 32),
            titleLabel.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -32),
            
            subtitleLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 12),
            subtitleLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 32),
            subtitleLabel.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -32),
            
            locationPermissionCard.topAnchor.constraint(equalTo: subtitleLabel.bottomAnchor, constant: 40),
            locationPermissionCard.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 24),
            locationPermissionCard.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -24),
            
            microphonePermissionCard.topAnchor.constraint(equalTo: locationPermissionCard.bottomAnchor, constant: 16),
            microphonePermissionCard.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 24),
            microphonePermissionCard.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -24),
            
            continueButton.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 32),
            continueButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -32),
            continueButton.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -24),
            continueButton.heightAnchor.constraint(equalToConstant: 56)
        ])
        
        locationPermissionCard.onTap = { [weak self] in
            self?.requestLocationPermission()
        }
        
        microphonePermissionCard.onTap = { [weak self] in
            self?.requestMicrophonePermission()
        }
        
        continueButton.addTarget(self, action: #selector(continueTapped), for: .touchUpInside)
    }
    
    private func requestLocationPermission() {
        let status = permissionManager.locationPermissionStatus()
        
        switch status {
        case .unknown:
            let alert = PermissionAlertFactory.locationPermissionRationale(
                onAllow: { [weak self] in
                    self?.permissionManager.requestLocationPermission { granted in
                        self?.updatePermissionStatus()
                    }
                },
                onDeny: { [weak self] in
                    self?.updatePermissionStatus()
                }
            )
            present(alert, animated: true)
            
        case .denied, .deniedPermanently:
            let alert = PermissionAlertFactory.locationPermissionDenied(
                onOpenSettings: { [weak self] in
                    self?.permissionManager.openAppSettings()
                },
                onCancel: {}
            )
            present(alert, animated: true)
            
        case .granted:
            break
        }
    }
    
    private func requestMicrophonePermission() {
        let status = permissionManager.microphonePermissionStatus()
        
        switch status {
        case .unknown:
            let alert = PermissionAlertFactory.microphonePermissionRationale(
                onAllow: { [weak self] in
                    self?.permissionManager.requestMicrophonePermission { granted in
                        self?.updatePermissionStatus()
                    }
                },
                onDeny: { [weak self] in
                    self?.updatePermissionStatus()
                }
            )
            present(alert, animated: true)
            
        case .denied, .deniedPermanently:
            let alert = PermissionAlertFactory.microphonePermissionDenied(
                onOpenSettings: { [weak self] in
                    self?.permissionManager.openAppSettings()
                },
                onCancel: {}
            )
            present(alert, animated: true)
            
        case .granted:
            break
        }
    }
    
    private func updatePermissionStatus() {
        // Update location card
        let locationGranted = permissionManager.hasLocationPermission()
        locationPermissionCard.setGranted(locationGranted)
        
        // Update microphone card
        let microphoneGranted = permissionManager.hasMicrophonePermission()
        microphonePermissionCard.setGranted(microphoneGranted)
        
        // Enable continue button if location is granted
        continueButton.isEnabled = locationGranted
        continueButton.backgroundColor = locationGranted ? .systemBlue : .systemGray
    }
    
    @objc private func continueTapped() {
        guard permissionManager.hasLocationPermission() else {
            let alert = UIAlertController(
                title: "Location Required",
                message: "Location permission is required to use GemNav.",
                preferredStyle: .alert
            )
            alert.addAction(UIAlertAction(title: "Grant Permission", style: .default) { [weak self] _ in
                self?.requestLocationPermission()
            })
            present(alert, animated: true)
            return
        }
        
        dismiss(animated: true)
    }
}

// MARK: - Permission Card View

class PermissionCardView: UIView {
    
    private let containerView: UIView = {
        let view = UIView()
        view.backgroundColor = .secondarySystemBackground
        view.layer.cornerRadius = 12
        return view
    }()
    
    private let iconImageView: UIImageView = {
        let imageView = UIImageView()
        imageView.contentMode = .scaleAspectFit
        imageView.tintColor = .systemBlue
        return imageView
    }()
    
    private let titleLabel: UILabel = {
        let label = UILabel()
        label.font = .systemFont(ofSize: 18, weight: .semibold)
        return label
    }()
    
    private let descriptionLabel: UILabel = {
        let label = UILabel()
        label.font = .systemFont(ofSize: 14)
        label.textColor = .secondaryLabel
        label.numberOfLines = 0
        return label
    }()
    
    private let statusLabel: UILabel = {
        let label = UILabel()
        label.font = .systemFont(ofSize: 14, weight: .medium)
        return label
    }()
    
    private let requiredBadge: UILabel = {
        let label = UILabel()
        label.text = "Required"
        label.font = .systemFont(ofSize: 12, weight: .semibold)
        label.textColor = .white
        label.backgroundColor = .systemRed
        label.textAlignment = .center
        label.layer.cornerRadius = 4
        label.clipsToBounds = true
        return label
    }()
    
    var onTap: (() -> Void)?
    
    init(icon: UIImage, title: String, description: String, isRequired: Bool) {
        super.init(frame: .zero)
        
        iconImageView.image = icon
        titleLabel.text = title
        descriptionLabel.text = description
        requiredBadge.isHidden = !isRequired
        
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) not implemented")
    }
    
    private func setupUI() {
        addSubview(containerView)
        containerView.addSubview(iconImageView)
        containerView.addSubview(titleLabel)
        containerView.addSubview(descriptionLabel)
        containerView.addSubview(statusLabel)
        containerView.addSubview(requiredBadge)
        
        containerView.translatesAutoresizingMaskIntoConstraints = false
        iconImageView.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        descriptionLabel.translatesAutoresizingMaskIntoConstraints = false
        statusLabel.translatesAutoresizingMaskIntoConstraints = false
        requiredBadge.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            containerView.topAnchor.constraint(equalTo: topAnchor),
            containerView.leadingAnchor.constraint(equalTo: leadingAnchor),
            containerView.trailingAnchor.constraint(equalTo: trailingAnchor),
            containerView.bottomAnchor.constraint(equalTo: bottomAnchor),
            
            iconImageView.leadingAnchor.constraint(equalTo: containerView.leadingAnchor, constant: 16),
            iconImageView.topAnchor.constraint(equalTo: containerView.topAnchor, constant: 16),
            iconImageView.widthAnchor.constraint(equalToConstant: 40),
            iconImageView.heightAnchor.constraint(equalToConstant: 40),
            
            titleLabel.leadingAnchor.constraint(equalTo: iconImageView.trailingAnchor, constant: 16),
            titleLabel.topAnchor.constraint(equalTo: containerView.topAnchor, constant: 16),
            titleLabel.trailingAnchor.constraint(equalTo: statusLabel.leadingAnchor, constant: -8),
            
            descriptionLabel.leadingAnchor.constraint(equalTo: titleLabel.leadingAnchor),
            descriptionLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 4),
            descriptionLabel.trailingAnchor.constraint(equalTo: containerView.trailingAnchor, constant: -16),
            descriptionLabel.bottomAnchor.constraint(equalTo: containerView.bottomAnchor, constant: -16),
            
            statusLabel.trailingAnchor.constraint(equalTo: containerView.trailingAnchor, constant: -16),
            statusLabel.centerYAnchor.constraint(equalTo: titleLabel.centerYAnchor),
            
            requiredBadge.leadingAnchor.constraint(equalTo: titleLabel.leadingAnchor),
            requiredBadge.topAnchor.constraint(equalTo: descriptionLabel.bottomAnchor, constant: 8),
            requiredBadge.widthAnchor.constraint(equalToConstant: 70),
            requiredBadge.heightAnchor.constraint(equalToConstant: 20)
        ])
        
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(handleTap))
        containerView.addGestureRecognizer(tapGesture)
        
        setGranted(false)
    }
    
    func setGranted(_ granted: Bool) {
        if granted {
            statusLabel.text = "Granted"
            statusLabel.textColor = .systemGreen
        } else {
            statusLabel.text = "Tap to Enable"
            statusLabel.textColor = .systemBlue
        }
    }
    
    @objc private func handleTap() {
        onTap?()
    }
}
```

---

## 6. Offline Mode Indicator

### OfflineModeView.swift
```swift
// OfflineModeView.swift
import UIKit

class OfflineModeView: UIView {
    
    private let containerView: UIView = {
        let view = UIView()
        view.backgroundColor = .systemBlue
        view.layer.cornerRadius = 8
        return view
    }()
    
    private let iconImageView: UIImageView = {
        let imageView = UIImageView()
        imageView.image = UIImage(systemName: "wifi.slash")
        imageView.contentMode = .scaleAspectFit
        imageView.tintColor = .white
        return imageView
    }()
    
    private let messageLabel: UILabel = {
        let label = UILabel()
        label.font = .systemFont(ofSize: 14, weight: .medium)
        label.textColor = .white
        label.numberOfLines = 0
        return label
    }()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) not implemented")
    }
    
    private func setupUI() {
        addSubview(containerView)
        containerView.addSubview(iconImageView)
        containerView.addSubview(messageLabel)
        
        containerView.translatesAutoresizingMaskIntoConstraints = false
        iconImageView.translatesAutoresizingMaskIntoConstraints = false
        messageLabel.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            containerView.topAnchor.constraint(equalTo: topAnchor),
            containerView.leadingAnchor.constraint(equalTo: leadingAnchor),
            containerView.trailingAnchor.constraint(equalTo: trailingAnchor),
            containerView.bottomAnchor.constraint(equalTo: bottomAnchor),
            
            iconImageView.leadingAnchor.constraint(equalTo: containerView.leadingAnchor, constant: 12),
            iconImageView.centerYAnchor.constraint(equalTo: containerView.centerYAnchor),
            iconImageView.widthAnchor.constraint(equalToConstant: 24),
            iconImageView.heightAnchor.constraint(equalToConstant: 24),
            
            messageLabel.leadingAnchor.constraint(equalTo: iconImageView.trailingAnchor, constant: 12),
            messageLabel.trailingAnchor.constraint(equalTo: containerView.trailingAnchor, constant: -12),
            messageLabel.topAnchor.constraint(equalTo: containerView.topAnchor, constant: 12),
            messageLabel.bottomAnchor.constraint(equalTo: containerView.bottomAnchor, constant: -12)
        ])
        
        isHidden = true
    }
    
    func showOfflineMode(tier: TierType) {
        let message: String
        
        switch tier {
        case .free:
            message = "Limited offline functionality. Location tracking only."
        case .plus:
            message = "Offline mode. Cached maps available."
        case .pro:
            message = "Offline mode. Truck routing unavailable."
        }
        
        messageLabel.text = message
        isHidden = false
    }
    
    func hide() {
        isHidden = true
    }
}
```

---

## 7. Navigation Error Handling Integration

### NavigationViewController (Enhanced)
```swift
// Add to NavigationViewController

private let errorBannerView = ErrorBannerView()
private let offlineModeView = OfflineModeView()

private func setupErrorViews() {
    view.addSubview(errorBannerView)
    view.addSubview(offlineModeView)
    
    errorBannerView.translatesAutoresizingMaskIntoConstraints = false
    offlineModeView.translatesAutoresizingMaskIntoConstraints = false
    
    NSLayoutConstraint.activate([
        errorBannerView.topAnchor.constraint(equalTo: turnInstructionView.bottomAnchor, constant: 8),
        errorBannerView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
        errorBannerView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
        
        offlineModeView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 8),
        offlineModeView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
        offlineModeView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16)
    ])
}

private func handleNavigationError(_ error: NavigationError) {
    switch error {
    case .noRouteFound(let origin, let destination):
        let alert = ErrorAlertFactory.noRouteFound(
            origin: origin,
            destination: destination,
            onRetry: { [weak self] in self?.viewModel.retryNavigation() },
            onCancel: { [weak self] in self?.dismiss(animated: true) }
        )
        present(alert, animated: true)
        
    case .gpsSignalLost:
        errorBannerView.show(
            type: .gpsWeak,
            actionText: "Details",
            onAction: { [weak self] in
                let alert = ErrorAlertFactory.gpsSignalLost(onDismiss: {})
                self?.present(alert, animated: true)
            }
        )
        
    case .networkError(let message):
        let alert = ErrorAlertFactory.networkError(
            onRetry: { [weak self] in self?.viewModel.retryNavigation() },
            onUseOffline: viewModel.currentTier != .free ? { [weak self] in
                self?.viewModel.switchToOfflineMode()
            } : nil
        )
        present(alert, animated: true)
        
    case .mapLoadFailed:
        let alert = ErrorAlertFactory.mapLoadError(
            onRetry: { [weak self] in self?.setupMap() }
        )
        present(alert, animated: true)
        
    case .serviceUnavailable(let serviceName):
        let alert = ErrorAlertFactory.serviceUnavailable(
            serviceName: serviceName,
            onDismiss: { [weak self] in self?.dismiss(animated: true) }
        )
        present(alert, animated: true)
        
    case .voiceRecognitionFailed(let message):
        let alert = ErrorAlertFactory.voiceRecognitionError(
            message: message,
            onRetry: { [weak self] in self?.startVoiceCommand() }
        )
        present(alert, animated: true)
    }
}

private func handleOfflineMode(_ isOffline: Bool) {
    if isOffline {
        offlineModeView.showOfflineMode(tier: viewModel.currentTier)
        errorBannerView.hide()
    } else {
        offlineModeView.hide()
    }
}
```

---

## 8. Error Data Models

```swift
// NavigationError.swift
enum NavigationError: Error {
    case noRouteFound(origin: String?, destination: String)
    case gpsSignalLost
    case networkError(message: String?)
    case mapLoadFailed(reason: String?)
    case serviceUnavailable(serviceName: String)
    case voiceRecognitionFailed(message: String?)
}
```

---

## Integration Notes

1. **Permission Flow**: PermissionRequestViewController → Main App
2. **Error Display**: ErrorBannerView for inline, alerts for critical
3. **Offline Detection**: Network monitoring in service layer
4. **Settings Deep Link**: UIApplication.openSettingsURLString

---

**File Output**: ios_permission_error_ui.md
