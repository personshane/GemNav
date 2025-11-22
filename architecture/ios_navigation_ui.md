# iOS Navigation UI Implementation

**Micro-Project**: MP-012 (iOS)  
**Component**: Navigation UI Layer  
**Dependencies**: ios_service_layer.md, ios_app_architecture.md

---

## 1. NavigationViewController

```swift
// NavigationViewController.swift
import UIKit
import MapKit
import Combine

class NavigationViewController: UIViewController {
    
    // MARK: - Properties
    private let viewModel: NavigationViewModel
    private var cancellables = Set<AnyCancellable>()
    
    private let mapView: BaseMapView
    private let turnInstructionView = TurnInstructionView()
    private let routeInfoView = RouteInfoView()
    private let speedOverlayView = SpeedOverlayView()
    private let voiceCommandButton = VoiceCommandButton()
    
    private let permissionManager: PermissionManager
    
    // MARK: - Initialization
    init(viewModel: NavigationViewModel, tier: TierType, permissionManager: PermissionManager) {
        self.viewModel = viewModel
        self.permissionManager = permissionManager
        
        // Initialize appropriate map view based on tier
        switch tier {
        case .free:
            self.mapView = EmptyMapView() // No in-app map for Free tier
        case .plus:
            self.mapView = GoogleMapView()
        case .pro:
            self.mapView = HEREMapView()
        }
        
        super.init(nibName: nil, bundle: nil)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) not implemented")
    }
    
    // MARK: - Lifecycle
    override func viewDidLoad() {
        super.viewDidLoad()
        
        setupUI()
        setupConstraints()
        setupBindings()
        checkPermissions()
        
        // Keep screen on during navigation
        UIApplication.shared.isIdleTimerDisabled = true
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        UIApplication.shared.isIdleTimerDisabled = false
    }
    
    // MARK: - UI Setup
    private func setupUI() {
        view.backgroundColor = .systemBackground
        
        // Add subviews
        view.addSubview(mapView)
        view.addSubview(turnInstructionView)
        view.addSubview(routeInfoView)
        view.addSubview(speedOverlayView)
        view.addSubview(voiceCommandButton)
        
        // Configure voice button action
        voiceCommandButton.addTarget(self, action: #selector(voiceCommandTapped), for: .touchUpInside)
    }
    
    private func setupConstraints() {
        mapView.translatesAutoresizingMaskIntoConstraints = false
        turnInstructionView.translatesAutoresizingMaskIntoConstraints = false
        routeInfoView.translatesAutoresizingMaskIntoConstraints = false
        speedOverlayView.translatesAutoresizingMaskIntoConstraints = false
        voiceCommandButton.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            // Map fills entire view
            mapView.topAnchor.constraint(equalTo: view.topAnchor),
            mapView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            mapView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            mapView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            
            // Turn instruction at top
            turnInstructionView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 16),
            turnInstructionView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
            turnInstructionView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
            
            // Speed overlay at bottom-left
            speedOverlayView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
            speedOverlayView.bottomAnchor.constraint(equalTo: routeInfoView.topAnchor, constant: -16),
            
            // Route info at bottom
            routeInfoView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
            routeInfoView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
            routeInfoView.bottomAnchor.constraint(equalTo: voiceCommandButton.topAnchor, constant: -12),
            
            // Voice command button at bottom center
            voiceCommandButton.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            voiceCommandButton.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -16),
            voiceCommandButton.widthAnchor.constraint(equalToConstant: 60),
            voiceCommandButton.heightAnchor.constraint(equalToConstant: 60)
        ])
    }
    
    private func setupBindings() {
        // Navigation state
        viewModel.$navigationState
            .receive(on: DispatchQueue.main)
            .sink { [weak self] state in
                self?.handleNavigationState(state)
            }
            .store(in: &cancellables)
        
        // Turn instruction
        viewModel.$turnInstruction
            .receive(on: DispatchQueue.main)
            .sink { [weak self] instruction in
                self?.updateTurnInstruction(instruction)
            }
            .store(in: &cancellables)
        
        // Route progress
        viewModel.$routeProgress
            .receive(on: DispatchQueue.main)
            .sink { [weak self] progress in
                self?.updateRouteProgress(progress)
            }
            .store(in: &cancellables)
    }
    
    // MARK: - Navigation Updates
    private func handleNavigationState(_ state: NavigationState) {
        switch state {
        case .idle:
            break
        case .active(let route, let location, let bearing):
            turnInstructionView.isHidden = false
            routeInfoView.isHidden = false
            mapView.updateCamera(location: location, bearing: bearing)
            mapView.drawRoute(route)
        case .completed:
            showRouteCompleted()
        case .cancelled:
            dismiss(animated: true)
        }
    }
    
    private func updateTurnInstruction(_ instruction: TurnInstruction?) {
        guard let instruction = instruction else { return }
        turnInstructionView.configure(with: instruction)
    }
    
    private func updateRouteProgress(_ progress: RouteProgress) {
        routeInfoView.configure(with: progress)
        speedOverlayView.configure(
            currentSpeed: progress.currentSpeedKmh,
            speedLimit: progress.speedLimitKmh,
            streetName: progress.currentStreet
        )
    }
    
    // MARK: - Voice Commands
    @objc private func voiceCommandTapped() {
        guard permissionManager.hasMicrophonePermission() else {
            permissionManager.requestMicrophonePermission { [weak self] granted in
                if granted {
                    self?.startVoiceCommand()
                }
            }
            return
        }
        
        startVoiceCommand()
    }
    
    private func startVoiceCommand() {
        voiceCommandButton.startListening()
        
        viewModel.startVoiceCommand { [weak self] result in
            self?.voiceCommandButton.stopListening()
            self?.showVoiceCommandFeedback(result)
        }
    }
    
    private func showVoiceCommandFeedback(_ result: VoiceCommandResult) {
        let feedbackLabel = UILabel()
        feedbackLabel.text = result.message
        feedbackLabel.textColor = .white
        feedbackLabel.backgroundColor = UIColor.black.withAlphaComponent(0.8)
        feedbackLabel.textAlignment = .center
        feedbackLabel.layer.cornerRadius = 8
        feedbackLabel.clipsToBounds = true
        feedbackLabel.translatesAutoresizingMaskIntoConstraints = false
        
        view.addSubview(feedbackLabel)
        
        NSLayoutConstraint.activate([
            feedbackLabel.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            feedbackLabel.bottomAnchor.constraint(equalTo: voiceCommandButton.topAnchor, constant: -16),
            feedbackLabel.leadingAnchor.constraint(greaterThanOrEqualTo: view.leadingAnchor, constant: 32),
            feedbackLabel.trailingAnchor.constraint(lessThanOrEqualTo: view.trailingAnchor, constant: -32),
            feedbackLabel.heightAnchor.constraint(equalToConstant: 44)
        ])
        
        UIView.animate(withDuration: 0.3, delay: 2.0, options: .curveEaseOut) {
            feedbackLabel.alpha = 0
        } completion: { _ in
            feedbackLabel.removeFromSuperview()
        }
    }
    
    // MARK: - Permissions
    private func checkPermissions() {
        guard !permissionManager.hasLocationPermission() else { return }
        
        permissionManager.requestLocationPermission { granted in
            if !granted {
                // Show alert or handle denied permission
            }
        }
    }
    
    // MARK: - Route Completion
    private func showRouteCompleted() {
        let alert = UIAlertController(
            title: "Destination Reached",
            message: "You have arrived at your destination",
            preferredStyle: .alert
        )
        alert.addAction(UIAlertAction(title: "OK", style: .default) { [weak self] _ in
            self?.dismiss(animated: true)
        })
        present(alert, animated: true)
    }
}
```

---

## 2. TurnInstructionView

```swift
// TurnInstructionView.swift
import UIKit

class TurnInstructionView: UIView {
    
    private let containerView: UIView = {
        let view = UIView()
        view.backgroundColor = .systemBackground
        view.layer.cornerRadius = 12
        view.layer.shadowColor = UIColor.black.cgColor
        view.layer.shadowOpacity = 0.15
        view.layer.shadowOffset = CGSize(width: 0, height: 2)
        view.layer.shadowRadius = 8
        return view
    }()
    
    private let turnIconView: UIImageView = {
        let imageView = UIImageView()
        imageView.contentMode = .scaleAspectFit
        imageView.tintColor = .systemBlue
        return imageView
    }()
    
    private let distanceLabel: UILabel = {
        let label = UILabel()
        label.font = .systemFont(ofSize: 28, weight: .bold)
        label.textColor = .label
        return label
    }()
    
    private let streetNameLabel: UILabel = {
        let label = UILabel()
        label.font = .systemFont(ofSize: 16, weight: .regular)
        label.textColor = .secondaryLabel
        label.numberOfLines = 1
        return label
    }()
    
    private let nextTurnIconView: UIImageView = {
        let imageView = UIImageView()
        imageView.contentMode = .scaleAspectFit
        imageView.tintColor = .tertiaryLabel
        imageView.isHidden = true
        return imageView
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
        containerView.addSubview(turnIconView)
        containerView.addSubview(distanceLabel)
        containerView.addSubview(streetNameLabel)
        containerView.addSubview(nextTurnIconView)
        
        containerView.translatesAutoresizingMaskIntoConstraints = false
        turnIconView.translatesAutoresizingMaskIntoConstraints = false
        distanceLabel.translatesAutoresizingMaskIntoConstraints = false
        streetNameLabel.translatesAutoresizingMaskIntoConstraints = false
        nextTurnIconView.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            containerView.topAnchor.constraint(equalTo: topAnchor),
            containerView.leadingAnchor.constraint(equalTo: leadingAnchor),
            containerView.trailingAnchor.constraint(equalTo: trailingAnchor),
            containerView.bottomAnchor.constraint(equalTo: bottomAnchor),
            
            turnIconView.leadingAnchor.constraint(equalTo: containerView.leadingAnchor, constant: 16),
            turnIconView.centerYAnchor.constraint(equalTo: containerView.centerYAnchor),
            turnIconView.widthAnchor.constraint(equalToConstant: 64),
            turnIconView.heightAnchor.constraint(equalToConstant: 64),
            
            distanceLabel.leadingAnchor.constraint(equalTo: turnIconView.trailingAnchor, constant: 16),
            distanceLabel.topAnchor.constraint(equalTo: containerView.topAnchor, constant: 16),
            
            streetNameLabel.leadingAnchor.constraint(equalTo: distanceLabel.leadingAnchor),
            streetNameLabel.topAnchor.constraint(equalTo: distanceLabel.bottomAnchor, constant: 4),
            streetNameLabel.trailingAnchor.constraint(equalTo: nextTurnIconView.leadingAnchor, constant: -8),
            streetNameLabel.bottomAnchor.constraint(equalTo: containerView.bottomAnchor, constant: -16),
            
            nextTurnIconView.trailingAnchor.constraint(equalTo: containerView.trailingAnchor, constant: -16),
            nextTurnIconView.centerYAnchor.constraint(equalTo: containerView.centerYAnchor),
            nextTurnIconView.widthAnchor.constraint(equalToConstant: 32),
            nextTurnIconView.heightAnchor.constraint(equalToConstant: 32)
        ])
    }
    
    func configure(with instruction: TurnInstruction) {
        turnIconView.image = instruction.type.icon
        distanceLabel.text = formatDistance(instruction.distanceMeters)
        streetNameLabel.text = instruction.streetName ?? "Continue"
        streetNameLabel.isHidden = instruction.streetName == nil
    }
    
    func setNextTurnPreview(_ type: TurnType?) {
        if let type = type {
            nextTurnIconView.image = type.icon
            nextTurnIconView.isHidden = false
        } else {
            nextTurnIconView.isHidden = true
        }
    }
    
    private func formatDistance(_ meters: Int) -> String {
        switch meters {
        case ..<100:
            return "\(meters) m"
        case ..<1000:
            return "\((meters / 10) * 10) m"
        default:
            let km = Double(meters) / 1000.0
            return String(format: "%.1f km", km)
        }
    }
}
```

---

## 3. RouteInfoView

```swift
// RouteInfoView.swift
import UIKit

class RouteInfoView: UIView {
    
    private let containerView: UIView = {
        let view = UIView()
        view.backgroundColor = .systemBackground
        view.layer.cornerRadius = 12
        view.layer.shadowColor = UIColor.black.cgColor
        view.layer.shadowOpacity = 0.1
        view.layer.shadowOffset = CGSize(width: 0, height: -2)
        view.layer.shadowRadius = 8
        return view
    }()
    
    private let stackView: UIStackView = {
        let stack = UIStackView()
        stack.axis = .horizontal
        stack.distribution = .fillEqually
        stack.spacing = 16
        return stack
    }()
    
    private let distanceInfoView = InfoItemView(title: "Distance")
    private let durationInfoView = InfoItemView(title: "Time")
    private let etaInfoView = InfoItemView(title: "Arrival")
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) not implemented")
    }
    
    private func setupUI() {
        addSubview(containerView)
        containerView.addSubview(stackView)
        
        stackView.addArrangedSubview(distanceInfoView)
        stackView.addArrangedSubview(durationInfoView)
        stackView.addArrangedSubview(etaInfoView)
        
        containerView.translatesAutoresizingMaskIntoConstraints = false
        stackView.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            containerView.topAnchor.constraint(equalTo: topAnchor),
            containerView.leadingAnchor.constraint(equalTo: leadingAnchor),
            containerView.trailingAnchor.constraint(equalTo: trailingAnchor),
            containerView.bottomAnchor.constraint(equalTo: bottomAnchor),
            
            stackView.topAnchor.constraint(equalTo: containerView.topAnchor, constant: 16),
            stackView.leadingAnchor.constraint(equalTo: containerView.leadingAnchor, constant: 16),
            stackView.trailingAnchor.constraint(equalTo: containerView.trailingAnchor, constant: -16),
            stackView.bottomAnchor.constraint(equalTo: containerView.bottomAnchor, constant: -16)
        ])
    }
    
    func configure(with progress: RouteProgress) {
        distanceInfoView.setValue(formatDistance(progress.remainingDistanceMeters))
        durationInfoView.setValue(formatDuration(progress.remainingDurationSeconds))
        etaInfoView.setValue(formatETA(progress.estimatedArrivalTime))
    }
    
    private func formatDistance(_ meters: Int) -> String {
        if meters < 1000 {
            return "\(meters) m"
        } else {
            let km = Double(meters) / 1000.0
            return String(format: "%.1f km", km)
        }
    }
    
    private func formatDuration(_ seconds: Int) -> String {
        switch seconds {
        case ..<60:
            return "\(seconds) sec"
        case ..<3600:
            return "\(seconds / 60) min"
        default:
            let hours = seconds / 3600
            let mins = (seconds % 3600) / 60
            return "\(hours)h \(mins)m"
        }
    }
    
    private func formatETA(_ timestamp: TimeInterval) -> String {
        let date = Date(timeIntervalSince1970: timestamp)
        let formatter = DateFormatter()
        formatter.dateFormat = "h:mm a"
        return formatter.string(from: date)
    }
}

// MARK: - InfoItemView

private class InfoItemView: UIView {
    
    private let valueLabel: UILabel = {
        let label = UILabel()
        label.font = .systemFont(ofSize: 20, weight: .bold)
        label.textColor = .label
        label.textAlignment = .center
        return label
    }()
    
    private let titleLabel: UILabel = {
        let label = UILabel()
        label.font = .systemFont(ofSize: 12, weight: .regular)
        label.textColor = .secondaryLabel
        label.textAlignment = .center
        return label
    }()
    
    init(title: String) {
        super.init(frame: .zero)
        titleLabel.text = title
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) not implemented")
    }
    
    private func setupUI() {
        addSubview(valueLabel)
        addSubview(titleLabel)
        
        valueLabel.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            valueLabel.topAnchor.constraint(equalTo: topAnchor),
            valueLabel.leadingAnchor.constraint(equalTo: leadingAnchor),
            valueLabel.trailingAnchor.constraint(equalTo: trailingAnchor),
            
            titleLabel.topAnchor.constraint(equalTo: valueLabel.bottomAnchor, constant: 2),
            titleLabel.leadingAnchor.constraint(equalTo: leadingAnchor),
            titleLabel.trailingAnchor.constraint(equalTo: trailingAnchor),
            titleLabel.bottomAnchor.constraint(equalTo: bottomAnchor)
        ])
    }
    
    func setValue(_ value: String) {
        valueLabel.text = value
    }
}
```

---

## 4. SpeedOverlayView

```swift
// SpeedOverlayView.swift
import UIKit

class SpeedOverlayView: UIView {
    
    private let containerView: UIView = {
        let view = UIView()
        view.backgroundColor = .systemBackground
        view.layer.cornerRadius = 8
        view.layer.shadowColor = UIColor.black.cgColor
        view.layer.shadowOpacity = 0.1
        view.layer.shadowOffset = CGSize(width: 0, height: 2)
        view.layer.shadowRadius = 4
        return view
    }()
    
    private let speedLabel: UILabel = {
        let label = UILabel()
        label.font = .systemFont(ofSize: 24, weight: .bold)
        label.textColor = .label
        label.textAlignment = .center
        return label
    }()
    
    private let speedLimitLabel: UILabel = {
        let label = UILabel()
        label.font = .systemFont(ofSize: 12, weight: .regular)
        label.textColor = .secondaryLabel
        label.textAlignment = .center
        return label
    }()
    
    private let streetLabel: UILabel = {
        let label = UILabel()
        label.font = .systemFont(ofSize: 10, weight: .regular)
        label.textColor = .tertiaryLabel
        label.textAlignment = .center
        label.numberOfLines = 1
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
        containerView.addSubview(speedLabel)
        containerView.addSubview(speedLimitLabel)
        containerView.addSubview(streetLabel)
        
        containerView.translatesAutoresizingMaskIntoConstraints = false
        speedLabel.translatesAutoresizingMaskIntoConstraints = false
        speedLimitLabel.translatesAutoresizingMaskIntoConstraints = false
        streetLabel.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            containerView.topAnchor.constraint(equalTo: topAnchor),
            containerView.leadingAnchor.constraint(equalTo: leadingAnchor),
            containerView.trailingAnchor.constraint(equalTo: trailingAnchor),
            containerView.bottomAnchor.constraint(equalTo: bottomAnchor),
            containerView.widthAnchor.constraint(equalToConstant: 80),
            
            speedLabel.topAnchor.constraint(equalTo: containerView.topAnchor, constant: 12),
            speedLabel.leadingAnchor.constraint(equalTo: containerView.leadingAnchor, constant: 12),
            speedLabel.trailingAnchor.constraint(equalTo: containerView.trailingAnchor, constant: -12),
            
            speedLimitLabel.topAnchor.constraint(equalTo: speedLabel.bottomAnchor, constant: 2),
            speedLimitLabel.leadingAnchor.constraint(equalTo: containerView.leadingAnchor, constant: 12),
            speedLimitLabel.trailingAnchor.constraint(equalTo: containerView.trailingAnchor, constant: -12),
            
            streetLabel.topAnchor.constraint(equalTo: speedLimitLabel.bottomAnchor, constant: 4),
            streetLabel.leadingAnchor.constraint(equalTo: containerView.leadingAnchor, constant: 12),
            streetLabel.trailingAnchor.constraint(equalTo: containerView.trailingAnchor, constant: -12),
            streetLabel.bottomAnchor.constraint(equalTo: containerView.bottomAnchor, constant: -12)
        ])
    }
    
    func configure(currentSpeed: Float, speedLimit: Int?, streetName: String?) {
        speedLabel.text = "\(Int(currentSpeed))"
        
        // Show warning color if exceeding speed limit
        if let limit = speedLimit, currentSpeed > Float(limit) {
            speedLabel.textColor = .systemRed
        } else {
            speedLabel.textColor = .label
        }
        
        if let limit = speedLimit {
            speedLimitLabel.text = "\(limit) km/h"
            speedLimitLabel.isHidden = false
        } else {
            speedLimitLabel.isHidden = true
        }
        
        if let street = streetName {
            streetLabel.text = street
            streetLabel.isHidden = false
        } else {
            streetLabel.isHidden = true
        }
    }
}
```

---

## 5. VoiceCommandButton

```swift
// VoiceCommandButton.swift
import UIKit

class VoiceCommandButton: UIButton {
    
    private var isListening = false
    private let pulseLayer = CAShapeLayer()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) not implemented")
    }
    
    private func setupUI() {
        backgroundColor = .systemBlue
        layer.cornerRadius = 30
        layer.shadowColor = UIColor.black.cgColor
        layer.shadowOpacity = 0.3
        layer.shadowOffset = CGSize(width: 0, height: 4)
        layer.shadowRadius = 8
        
        let imageConfig = UIImage.SymbolConfiguration(pointSize: 24, weight: .medium)
        let micImage = UIImage(systemName: "mic.fill", withConfiguration: imageConfig)
        setImage(micImage, for: .normal)
        tintColor = .white
    }
    
    func startListening() {
        isListening = true
        animatePulse()
    }
    
    func stopListening() {
        isListening = false
        pulseLayer.removeAllAnimations()
        pulseLayer.removeFromSuperlayer()
    }
    
    private func animatePulse() {
        pulseLayer.path = UIBezierPath(ovalIn: bounds).cgPath
        pulseLayer.fillColor = UIColor.systemBlue.withAlphaComponent(0.3).cgColor
        layer.insertSublayer(pulseLayer, below: imageView?.layer)
        
        let scaleAnimation = CABasicAnimation(keyPath: "transform.scale")
        scaleAnimation.fromValue = 1.0
        scaleAnimation.toValue = 1.3
        scaleAnimation.duration = 1.0
        scaleAnimation.repeatCount = .infinity
        scaleAnimation.autoreverses = true
        
        let opacityAnimation = CABasicAnimation(keyPath: "opacity")
        opacityAnimation.fromValue = 0.7
        opacityAnimation.toValue = 0.0
        opacityAnimation.duration = 1.0
        opacityAnimation.repeatCount = .infinity
        
        pulseLayer.add(scaleAnimation, forKey: "pulse")
        pulseLayer.add(opacityAnimation, forKey: "opacity")
    }
}
```

---

## 6. Map View Protocols

```swift
// BaseMapView.swift
import UIKit
import CoreLocation

protocol BaseMapView: UIView {
    func updateCamera(location: CLLocation, bearing: Double)
    func drawRoute(_ route: Route)
    func clearRoute()
}

// EmptyMapView.swift (Free Tier)
class EmptyMapView: UIView, BaseMapView {
    func updateCamera(location: CLLocation, bearing: Double) {}
    func drawRoute(_ route: Route) {}
    func clearRoute() {}
}
```

---

## 7. GoogleMapView (Plus Tier)

```swift
// GoogleMapView.swift
import UIKit
import GoogleMaps

class GoogleMapView: UIView, BaseMapView {
    
    private var mapView: GMSMapView!
    private var routePolyline: GMSPolyline?
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupMap()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) not implemented")
    }
    
    private func setupMap() {
        let camera = GMSCameraPosition.camera(
            withLatitude: 0,
            longitude: 0,
            zoom: 15
        )
        mapView = GMSMapView(frame: bounds, camera: camera)
        mapView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        mapView.settings.compassButton = true
        mapView.settings.myLocationButton = false
        mapView.isMyLocationEnabled = true
        addSubview(mapView)
    }
    
    func updateCamera(location: CLLocation, bearing: Double) {
        let position = GMSCameraPosition(
            latitude: location.coordinate.latitude,
            longitude: location.coordinate.longitude,
            zoom: 17,
            bearing: bearing,
            viewingAngle: 45
        )
        mapView.animate(to: position)
    }
    
    func drawRoute(_ route: Route) {
        routePolyline?.map = nil
        
        let path = GMSMutablePath()
        route.coordinates.forEach {
            path.add(CLLocationCoordinate2D(latitude: $0.latitude, longitude: $0.longitude))
        }
        
        routePolyline = GMSPolyline(path: path)
        routePolyline?.strokeColor = .systemBlue
        routePolyline?.strokeWidth = 5
        routePolyline?.geodesic = true
        routePolyline?.map = mapView
    }
    
    func clearRoute() {
        routePolyline?.map = nil
        routePolyline = nil
    }
}
```

---

## 8. HEREMapView (Pro Tier)

```swift
// HEREMapView.swift
import UIKit
import heresdk

class HEREMapView: UIView, BaseMapView {
    
    private var mapView: MapView!
    private var routePolyline: MapPolyline?
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupMap()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) not implemented")
    }
    
    private func setupMap() {
        mapView = MapView(frame: bounds)
        mapView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        addSubview(mapView)
        
        mapView.mapScene.loadScene(mapScheme: .normalDay) { error in
            guard error == nil else {
                print("Map scene load error: \(error!)")
                return
            }
        }
    }
    
    func updateCamera(location: CLLocation, bearing: Double) {
        let target = GeoCoordinates(
            latitude: location.coordinate.latitude,
            longitude: location.coordinate.longitude
        )
        
        let orientation = GeoOrientationUpdate(
            bearing: bearing,
            tilt: 45
        )
        
        mapView.camera.lookAt(
            point: target,
            orientation: orientation,
            distanceInMeters: 500
        )
    }
    
    func drawRoute(_ route: Route) {
        if let existing = routePolyline {
            mapView.mapScene.removeMapPolyline(existing)
        }
        
        let coordinates = route.coordinates.map {
            GeoCoordinates(latitude: $0.latitude, longitude: $0.longitude)
        }
        
        guard let polyline = try? GeoPolyline(vertices: coordinates) else { return }
        
        let lineColor = UIColor.systemBlue
        let lineWidth = MapPolyline.SolidRepresentation.MetricDependentRenderSize(
            sizeUnit: .meters,
            size: 5
        )
        
        routePolyline = MapPolyline(
            geometry: polyline,
            representation: MapPolyline.SolidRepresentation(
                lineWidth: lineWidth,
                color: lineColor,
                outlineColor: .clear,
                outlineWidth: .constant(pixels: 0)
            )
        )
        
        mapView.mapScene.addMapPolyline(routePolyline!)
    }
    
    func clearRoute() {
        if let polyline = routePolyline {
            mapView.mapScene.removeMapPolyline(polyline)
            routePolyline = nil
        }
    }
}
```

---

## 9. NavigationViewModel

```swift
// NavigationViewModel.swift
import Foundation
import Combine
import CoreLocation

class NavigationViewModel: ObservableObject {
    
    @Published var navigationState: NavigationState = .idle
    @Published var turnInstruction: TurnInstruction?
    @Published var routeProgress = RouteProgress.empty
    
    private let navigationService: NavigationService
    private let voiceService: VoiceCommandService
    private var cancellables = Set<AnyCancellable>()
    
    init(navigationService: NavigationService, voiceService: VoiceCommandService) {
        self.navigationService = navigationService
        self.voiceService = voiceService
        setupBindings()
    }
    
    private func setupBindings() {
        navigationService.navigationStatePublisher
            .assign(to: &$navigationState)
        
        navigationService.turnInstructionPublisher
            .assign(to: &$turnInstruction)
        
        navigationService.routeProgressPublisher
            .assign(to: &$routeProgress)
    }
    
    func startVoiceCommand(completion: @escaping (VoiceCommandResult) -> Void) {
        voiceService.startListening { command in
            Task {
                let result = await self.voiceService.processCommand(command)
                DispatchQueue.main.async {
                    completion(result)
                }
            }
        }
    }
    
    func cancelNavigation() {
        Task {
            await navigationService.stopNavigation()
        }
    }
}
```

---

## 10. Data Models

```swift
// NavigationModels.swift
import Foundation
import CoreLocation

enum NavigationState {
    case idle
    case active(route: Route, location: CLLocation, bearing: Double)
    case completed
    case cancelled
}

struct TurnInstruction {
    let type: TurnType
    let distanceMeters: Int
    let streetName: String?
}

enum TurnType {
    case straight, right, left
    case slightRight, slightLeft
    case sharpRight, sharpLeft
    case uturn
    case exitRight, exitLeft
    case merge, roundabout
    
    var icon: UIImage? {
        let name: String
        switch self {
        case .straight: name = "arrow.up"
        case .right: name = "arrow.turn.up.right"
        case .left: name = "arrow.turn.up.left"
        case .slightRight: name = "arrow.up.right"
        case .slightLeft: name = "arrow.up.left"
        case .sharpRight: name = "arrow.turn.right.up"
        case .sharpLeft: name = "arrow.turn.left.up"
        case .uturn: name = "arrow.uturn.left"
        case .exitRight: name = "arrow.turn.right.down"
        case .exitLeft: name = "arrow.turn.left.down"
        case .merge: name = "arrow.merge"
        case .roundabout: name = "arrow.clockwise"
        }
        return UIImage(systemName: name)
    }
}

struct RouteProgress {
    let remainingDistanceMeters: Int
    let remainingDurationSeconds: Int
    let estimatedArrivalTime: TimeInterval
    let currentSpeedKmh: Float
    let speedLimitKmh: Int?
    let currentStreet: String?
    
    static let empty = RouteProgress(
        remainingDistanceMeters: 0,
        remainingDurationSeconds: 0,
        estimatedArrivalTime: 0,
        currentSpeedKmh: 0,
        speedLimitKmh: nil,
        currentStreet: nil
    )
}

struct Route {
    let coordinates: [Coordinate]
}

struct Coordinate {
    let latitude: Double
    let longitude: Double
}

enum TierType {
    case free, plus, pro
}

struct VoiceCommandResult {
    let success: Bool
    let message: String
}
```

---

## Integration Notes

1. **Free Tier**: Uses EmptyMapView, navigation via Google Maps app URL scheme
2. **Plus Tier**: Uses GoogleMapView with Google Maps SDK
3. **Pro Tier**: Uses HEREMapView with HERE SDK

All UI components tier-agnostic except map rendering.

Permission handling via PermissionManager (ios_service_layer.md).

---

**File Output**: ios_navigation_ui.md
