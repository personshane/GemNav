# iOS Application Architecture

**Version**: 1.0  
**Platform**: iOS  
**Pattern**: MVVM + Clean Architecture  
**DI**: Swinject  
**UI**: UIKit + SwiftUI

---

## Architecture Overview

```
Presentation Layer (Views)
    ↓
ViewModel Layer (Combine Publishers)
    ↓
Domain Layer (Use Cases)
    ↓
Data Layer (Repository)
    ↓
Data Sources (Remote/Local)
```

---

## Project Structure

```
GemNav/
├── App/
│   ├── AppDelegate.swift
│   ├── SceneDelegate.swift
│   └── DependencyContainer.swift
│
├── Presentation/
│   ├── Main/
│   │   ├── MainViewController.swift
│   │   ├── MainViewModel.swift
│   │   └── MainCoordinator.swift
│   ├── Navigation/
│   │   ├── NavigationViewController.swift
│   │   ├── NavigationViewModel.swift
│   │   ├── MapViewController.swift
│   │   └── NavigationCoordinator.swift
│   ├── Chat/
│   │   ├── ChatViewController.swift
│   │   ├── ChatViewModel.swift
│   │   └── MessageCell.swift
│   ├── Settings/
│   │   ├── SettingsViewController.swift
│   │   └── SettingsViewModel.swift
│   └── Common/
│       ├── BaseViewController.swift
│       ├── BaseViewModel.swift
│       └── Coordinator.swift
│
├── Domain/
│   ├── Models/
│   │   ├── Route.swift
│   │   ├── Location.swift
│   │   ├── ChatMessage.swift
│   │   └── NavigationState.swift
│   ├── Repositories/
│   │   ├── NavigationRepository.swift
│   │   ├── ChatRepository.swift
│   │   └── UserRepository.swift
│   └── UseCases/
│       ├── CalculateRouteUseCase.swift
│       ├── SendMessageUseCase.swift
│       └── GetNavigationStateUseCase.swift
│
├── Data/
│   ├── Repositories/
│   │   ├── NavigationRepositoryImpl.swift
│   │   ├── ChatRepositoryImpl.swift
│   │   └── UserRepositoryImpl.swift
│   ├── Local/
│   │   ├── CoreData/
│   │   │   ├── GemNavDataModel.xcdatamodeld
│   │   │   ├── CoreDataStack.swift
│   │   │   └── Entities/
│   │   └── UserDefaults/
│   │       └── UserPreferences.swift
│   ├── Remote/
│   │   ├── API/
│   │   │   ├── GeminiAPI.swift
│   │   │   ├── NavigationAPI.swift
│   │   │   └── UserAPI.swift
│   │   ├── DTOs/
│   │   │   ├── RouteResponse.swift
│   │   │   ├── ChatResponse.swift
│   │   │   └── UserDTO.swift
│   │   └── NetworkManager.swift
│   └── Mappers/
│       ├── RouteMapper.swift
│       ├── MessageMapper.swift
│       └── LocationMapper.swift
│
├── Services/
│   ├── LocationService.swift
│   ├── NavigationService.swift
│   └── VoiceCommandService.swift
│
└── Utilities/
    ├── Constants.swift
    ├── Extensions/
    ├── PermissionManager.swift
    └── NetworkMonitor.swift
```

---

## AppDelegate.swift

```swift
import UIKit
import Swinject

@main
class AppDelegate: UIResponder, UIApplicationDelegate {
    
    var window: UIWindow?
    static let container = Container()
    
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        
        // Setup Dependency Injection
        DependencyContainer.setup(container: AppDelegate.container)
        
        // Initialize HERE SDK (Pro tier only)
        #if TIER_PRO
        initializeHERESDK()
        #endif
        
        // Initialize Firebase
        // Initialize Crashlytics
        
        return true
    }
    
    func application(
        _ application: UIApplication,
        configurationForConnecting connectingSceneSession: UISceneSession,
        options: UIScene.ConnectionOptions
    ) -> UISceneConfiguration {
        return UISceneConfiguration(
            name: "Default Configuration",
            sessionRole: connectingSceneSession.role
        )
    }
    
    private func initializeHERESDK() {
        // HERE SDK initialization
    }
}
```

---

## SceneDelegate.swift

```swift
import UIKit

class SceneDelegate: UIResponder, UIWindowSceneDelegate {
    
    var window: UIWindow?
    var mainCoordinator: MainCoordinator?
    
    func scene(
        _ scene: UIScene,
        willConnectTo session: UISceneSession,
        options connectionOptions: UIScene.ConnectionOptions
    ) {
        guard let windowScene = (scene as? UIWindowScene) else { return }
        
        let window = UIWindow(windowScene: windowScene)
        
        // Setup main coordinator
        let navigationController = UINavigationController()
        mainCoordinator = MainCoordinator(navigationController: navigationController)
        mainCoordinator?.start()
        
        window.rootViewController = navigationController
        window.makeKeyAndVisible()
        
        self.window = window
    }
}
```

---

## BaseViewController.swift

```swift
import UIKit
import Combine

class BaseViewController<ViewModel: BaseViewModel>: UIViewController {
    
    let viewModel: ViewModel
    var cancellables = Set<AnyCancellable>()
    
    init(viewModel: ViewModel) {
        self.viewModel = viewModel
        super.init(nibName: nil, bundle: nil)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        bind()
    }
    
    /// Override to setup UI elements
    func setupUI() {
        view.backgroundColor = .systemBackground
    }
    
    /// Override to bind ViewModel publishers
    func bind() {
        // To be overridden
    }
    
    deinit {
        cancellables.removeAll()
    }
}
```

---

## BaseViewModel.swift

```swift
import Foundation
import Combine

protocol ViewModelType {
    associatedtype Input
    associatedtype Output
    
    func transform(input: Input) -> Output
}

class BaseViewModel {
    
    var cancellables = Set<AnyCancellable>()
    
    deinit {
        cancellables.removeAll()
    }
}
```

---

## NavigationViewController.swift

```swift
import UIKit
import MapKit
import Combine

class NavigationViewController: BaseViewController<NavigationViewModel> {
    
    // MARK: - UI Components
    
    private lazy var mapView: MKMapView = {
        let map = MKMapView()
        map.translatesAutoresizingMaskIntoConstraints = false
        map.delegate = self
        map.showsUserLocation = true
        map.userTrackingMode = .followWithHeading
        return map
    }()
    
    private lazy var searchBar: UISearchBar = {
        let bar = UISearchBar()
        bar.translatesAutoresizingMaskIntoConstraints = false
        bar.placeholder = "Where to?"
        bar.delegate = self
        bar.searchBarStyle = .minimal
        return bar
    }()
    
    private lazy var startNavigationButton: UIButton = {
        let button = UIButton(type: .system)
        button.translatesAutoresizingMaskIntoConstraints = false
        button.setTitle("Start Navigation", for: .normal)
        button.backgroundColor = .systemBlue
        button.setTitleColor(.white, for: .normal)
        button.layer.cornerRadius = 25
        button.addTarget(self, action: #selector(startNavigationTapped), for: .touchUpInside)
        button.isHidden = true
        return button
    }()
    
    // MARK: - Lifecycle
    
    override func setupUI() {
        super.setupUI()
        
        view.addSubview(mapView)
        view.addSubview(searchBar)
        view.addSubview(startNavigationButton)
        
        NSLayoutConstraint.activate([
            mapView.topAnchor.constraint(equalTo: view.topAnchor),
            mapView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            mapView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            mapView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            
            searchBar.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 16),
            searchBar.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
            searchBar.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
            
            startNavigationButton.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -32),
            startNavigationButton.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            startNavigationButton.widthAnchor.constraint(equalToConstant: 200),
            startNavigationButton.heightAnchor.constraint(equalToConstant: 50)
        ])
    }
    
    override func bind() {
        viewModel.$state
            .receive(on: DispatchQueue.main)
            .sink { [weak self] state in
                self?.handleState(state)
            }
            .store(in: &cancellables)
        
        viewModel.eventPublisher
            .receive(on: DispatchQueue.main)
            .sink { [weak self] event in
                self?.handleEvent(event)
            }
            .store(in: &cancellables)
    }
    
    // MARK: - State Handling
    
    private func handleState(_ state: NavigationState) {
        switch state {
        case .idle:
            showIdleState()
        case .routeCalculating:
            showLoadingState()
        case .routeReady(let route):
            showRoute(route)
        case .navigating(let navState):
            showNavigating(navState)
        case .error(let message):
            showError(message)
        }
    }
    
    private func showIdleState() {
        startNavigationButton.isHidden = true
        mapView.removeOverlays(mapView.overlays)
        mapView.removeAnnotations(mapView.annotations)
    }
    
    private func showLoadingState() {
        // Show loading indicator
    }
    
    private func showRoute(_ route: Route) {
        startNavigationButton.isHidden = false
        
        // Draw route on map
        let coordinates = decodePolyline(route.polyline)
        let polyline = MKPolyline(coordinates: coordinates, count: coordinates.count)
        mapView.addOverlay(polyline)
        
        // Add start/end annotations
        let startAnnotation = MKPointAnnotation()
        startAnnotation.coordinate = route.origin.coordinate
        startAnnotation.title = "Start"
        
        let endAnnotation = MKPointAnnotation()
        endAnnotation.coordinate = route.destination.coordinate
        endAnnotation.title = "Destination"
        
        mapView.addAnnotations([startAnnotation, endAnnotation])
        
        // Fit route in view
        mapView.setVisibleMapRect(
            polyline.boundingMapRect,
            edgePadding: UIEdgeInsets(top: 50, left: 50, bottom: 50, right: 50),
            animated: true
        )
    }
    
    private func showNavigating(_ navState: NavigatingState) {
        // Update turn-by-turn UI
    }
    
    private func showError(_ message: String) {
        let alert = UIAlertController(
            title: "Error",
            message: message,
            preferredStyle: .alert
        )
        alert.addAction(UIAlertAction(title: "OK", style: .default))
        present(alert, animated: true)
    }
    
    private func handleEvent(_ event: NavigationEvent) {
        switch event {
        case .navigationStarted:
            print("Navigation started")
        case .navigationStopped:
            print("Navigation stopped")
        case .openSearch:
            searchBar.becomeFirstResponder()
        case .mapReady:
            viewModel.onMapReady()
        }
    }
    
    // MARK: - Actions
    
    @objc private func startNavigationTapped() {
        viewModel.startNavigation()
    }
    
    // MARK: - Helpers
    
    private func decodePolyline(_ encoded: String) -> [CLLocationCoordinate2D] {
        // Polyline decoding implementation
        return []
    }
}

// MARK: - MKMapViewDelegate

extension NavigationViewController: MKMapViewDelegate {
    
    func mapView(_ mapView: MKMapView, rendererFor overlay: MKOverlay) -> MKOverlayRenderer {
        if let polyline = overlay as? MKPolyline {
            let renderer = MKPolylineRenderer(polyline: polyline)
            renderer.strokeColor = .systemBlue
            renderer.lineWidth = 4
            return renderer
        }
        return MKOverlayRenderer(overlay: overlay)
    }
}

// MARK: - UISearchBarDelegate

extension NavigationViewController: UISearchBarDelegate {
    
    func searchBarSearchButtonClicked(_ searchBar: UISearchBar) {
        guard let query = searchBar.text else { return }
        searchBar.resignFirstResponder()
        
        // Geocode search query
        viewModel.searchDestination(query: query)
    }
}
```

---

## NavigationViewModel.swift

```swift
import Foundation
import Combine

enum NavigationState {
    case idle
    case routeCalculating
    case routeReady(Route)
    case navigating(NavigatingState)
    case error(String)
}

struct NavigatingState {
    let route: Route
    let currentLocation: Location?
    let nextStep: RouteStep?
    let distanceRemaining: Int
    let timeRemaining: Int
}

enum NavigationEvent {
    case navigationStarted
    case navigationStopped
    case openSearch
    case mapReady
}

class NavigationViewModel: BaseViewModel {
    
    // MARK: - Dependencies
    
    private let calculateRouteUseCase: CalculateRouteUseCase
    private let getNavigationStateUseCase: GetNavigationStateUseCase
    
    // MARK: - Publishers
    
    @Published private(set) var state: NavigationState = .idle
    private let eventSubject = PassthroughSubject<NavigationEvent, Never>()
    
    var eventPublisher: AnyPublisher<NavigationEvent, Never> {
        eventSubject.eraseToAnyPublisher()
    }
    
    // MARK: - Initialization
    
    init(
        calculateRouteUseCase: CalculateRouteUseCase,
        getNavigationStateUseCase: GetNavigationStateUseCase
    ) {
        self.calculateRouteUseCase = calculateRouteUseCase
        self.getNavigationStateUseCase = getNavigationStateUseCase
        super.init()
        
        observeNavigationState()
    }
    
    // MARK: - Public Methods
    
    func calculateRoute(origin: Location, destination: Location) {
        state = .routeCalculating
        
        Task {
            let result = await calculateRouteUseCase.execute(
                origin: origin,
                destination: destination
            )
            
            await MainActor.run {
                switch result {
                case .success(let route):
                    state = .routeReady(route)
                case .failure(let error):
                    state = .error(error.localizedDescription)
                }
            }
        }
    }
    
    func startNavigation() {
        guard case .routeReady(let route) = state else { return }
        
        state = .navigating(NavigatingState(
            route: route,
            currentLocation: nil,
            nextStep: nil,
            distanceRemaining: route.distance,
            timeRemaining: route.duration
        ))
        
        eventSubject.send(.navigationStarted)
    }
    
    func stopNavigation() {
        state = .idle
        eventSubject.send(.navigationStopped)
    }
    
    func searchDestination(query: String) {
        // Geocode query and calculate route
    }
    
    func onMapReady() {
        eventSubject.send(.mapReady)
    }
    
    // MARK: - Private Methods
    
    private func observeNavigationState() {
        getNavigationStateUseCase.execute()
            .sink { [weak self] navState in
                // Handle navigation state updates
            }
            .store(in: &cancellables)
    }
}
```

---

## ChatViewController.swift

```swift
import UIKit
import Combine

class ChatViewController: BaseViewController<ChatViewModel> {
    
    // MARK: - UI Components
    
    private lazy var tableView: UITableView = {
        let table = UITableView()
        table.translatesAutoresizingMaskIntoConstraints = false
        table.register(MessageCell.self, forCellReuseIdentifier: MessageCell.identifier)
        table.separatorStyle = .none
        table.delegate = self
        table.dataSource = self
        return table
    }()
    
    private lazy var inputContainerView: UIView = {
        let view = UIView()
        view.translatesAutoresizingMaskIntoConstraints = false
        view.backgroundColor = .secondarySystemBackground
        return view
    }()
    
    private lazy var inputTextField: UITextField = {
        let field = UITextField()
        field.translatesAutoresizingMaskIntoConstraints = false
        field.placeholder = "Type a message..."
        field.borderStyle = .roundedRect
        field.delegate = self
        return field
    }()
    
    private lazy var sendButton: UIButton = {
        let button = UIButton(type: .system)
        button.translatesAutoresizingMaskIntoConstraints = false
        button.setTitle("Send", for: .normal)
        button.addTarget(self, action: #selector(sendTapped), for: .touchUpInside)
        return button
    }()
    
    private lazy var voiceButton: UIButton = {
        let button = UIButton(type: .system)
        button.translatesAutoresizingMaskIntoConstraints = false
        button.setImage(UIImage(systemName: "mic.fill"), for: .normal)
        button.addTarget(self, action: #selector(voiceTapped), for: .touchUpInside)
        return button
    }()
    
    // MARK: - Properties
    
    private var messages: [ChatMessage] = []
    
    // MARK: - Lifecycle
    
    override func setupUI() {
        super.setupUI()
        
        view.addSubview(tableView)
        view.addSubview(inputContainerView)
        inputContainerView.addSubview(voiceButton)
        inputContainerView.addSubview(inputTextField)
        inputContainerView.addSubview(sendButton)
        
        NSLayoutConstraint.activate([
            tableView.topAnchor.constraint(equalTo: view.topAnchor),
            tableView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            tableView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            tableView.bottomAnchor.constraint(equalTo: inputContainerView.topAnchor),
            
            inputContainerView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            inputContainerView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            inputContainerView.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor),
            inputContainerView.heightAnchor.constraint(equalToConstant: 60),
            
            voiceButton.leadingAnchor.constraint(equalTo: inputContainerView.leadingAnchor, constant: 16),
            voiceButton.centerYAnchor.constraint(equalTo: inputContainerView.centerYAnchor),
            voiceButton.widthAnchor.constraint(equalToConstant: 44),
            
            inputTextField.leadingAnchor.constraint(equalTo: voiceButton.trailingAnchor, constant: 8),
            inputTextField.centerYAnchor.constraint(equalTo: inputContainerView.centerYAnchor),
            inputTextField.heightAnchor.constraint(equalToConstant: 40),
            
            sendButton.leadingAnchor.constraint(equalTo: inputTextField.trailingAnchor, constant: 8),
            sendButton.trailingAnchor.constraint(equalTo: inputContainerView.trailingAnchor, constant: -16),
            sendButton.centerYAnchor.constraint(equalTo: inputContainerView.centerYAnchor),
            sendButton.widthAnchor.constraint(equalToConstant: 60)
        ])
    }
    
    override func bind() {
        viewModel.$messages
            .receive(on: DispatchQueue.main)
            .sink { [weak self] messages in
                self?.messages = messages
                self?.tableView.reloadData()
                self?.scrollToBottom()
            }
            .store(in: &cancellables)
        
        viewModel.eventPublisher
            .receive(on: DispatchQueue.main)
            .sink { [weak self] event in
                self?.handleEvent(event)
            }
            .store(in: &cancellables)
    }
    
    // MARK: - Actions
    
    @objc private func sendTapped() {
        guard let text = inputTextField.text, !text.isEmpty else { return }
        viewModel.sendMessage(text)
        inputTextField.text = ""
    }
    
    @objc private func voiceTapped() {
        viewModel.startVoiceInput()
    }
    
    // MARK: - Helpers
    
    private func scrollToBottom() {
        guard messages.count > 0 else { return }
        let indexPath = IndexPath(row: messages.count - 1, section: 0)
        tableView.scrollToRow(at: indexPath, at: .bottom, animated: true)
    }
    
    private func handleEvent(_ event: ChatEvent) {
        switch event {
        case .messageSent:
            // Animate message sent
            break
        case .voiceInputStarted:
            // Show voice input UI
            break
        case .error(let message):
            showError(message)
        }
    }
    
    private func showError(_ message: String) {
        let alert = UIAlertController(title: "Error", message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "OK", style: .default))
        present(alert, animated: true)
    }
}

// MARK: - UITableViewDataSource

extension ChatViewController: UITableViewDataSource {
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return messages.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(
            withIdentifier: MessageCell.identifier,
            for: indexPath
        ) as! MessageCell
        
        let message = messages[indexPath.row]
        cell.configure(with: message)
        return cell
    }
}

// MARK: - UITableViewDelegate

extension ChatViewController: UITableViewDelegate {
    
    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return UITableView.automaticDimension
    }
}

// MARK: - UITextFieldDelegate

extension ChatViewController: UITextFieldDelegate {
    
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        sendTapped()
        return true
    }
}
```

---

## Coordinator Pattern

### Coordinator.swift

```swift
import UIKit

protocol Coordinator: AnyObject {
    var navigationController: UINavigationController { get set }
    var childCoordinators: [Coordinator] { get set }
    
    func start()
    func addChildCoordinator(_ coordinator: Coordinator)
    func removeChildCoordinator(_ coordinator: Coordinator)
}

extension Coordinator {
    
    func addChildCoordinator(_ coordinator: Coordinator) {
        childCoordinators.append(coordinator)
    }
    
    func removeChildCoordinator(_ coordinator: Coordinator) {
        childCoordinators = childCoordinators.filter { $0 !== coordinator }
    }
}
```

### MainCoordinator.swift

```swift
import UIKit

class MainCoordinator: Coordinator {
    
    var navigationController: UINavigationController
    var childCoordinators: [Coordinator] = []
    
    init(navigationController: UINavigationController) {
        self.navigationController = navigationController
    }
    
    func start() {
        let viewModel = AppDelegate.container.resolve(MainViewModel.self)!
        let viewController = MainViewController(viewModel: viewModel)
        viewController.coordinator = self
        navigationController.pushViewController(viewController, animated: false)
    }
    
    func showNavigation() {
        let coordinator = NavigationCoordinator(navigationController: navigationController)
        addChildCoordinator(coordinator)
        coordinator.start()
    }
    
    func showChat() {
        let coordinator = ChatCoordinator(navigationController: navigationController)
        addChildCoordinator(coordinator)
        coordinator.start()
    }
}
```

---

## Tier-Specific Implementations

### Free Tier - OnDeviceNavigationViewModel.swift

```swift
#if TIER_FREE
class OnDeviceNavigationViewModel: NavigationViewModel {
    
    override init(
        calculateRouteUseCase: CalculateRouteUseCase,
        getNavigationStateUseCase: GetNavigationStateUseCase
    ) {
        super.init(
            calculateRouteUseCase: calculateRouteUseCase,
            getNavigationStateUseCase: getNavigationStateUseCase
        )
        
        // Use on-device AI (Apple's ML frameworks)
        // Launch Google Maps via URL schemes
    }
    
    override func startNavigation() {
        guard case .routeReady(let route) = state else { return }
        
        // Launch Google Maps app via URL scheme
        if let url = createGoogleMapsURL(for: route) {
            UIApplication.shared.open(url)
            eventSubject.send(.navigationStarted)
        }
    }
    
    private func createGoogleMapsURL(for route: Route) -> URL? {
        let destination = route.destination
        let urlString = "comgooglemaps://?daddr=\(destination.latitude),\(destination.longitude)&directionsmode=driving"
        return URL(string: urlString)
    }
}
#endif
```

### Pro Tier - HERENavigationViewModel.swift

```swift
#if TIER_PRO
class HERENavigationViewModel: NavigationViewModel {
    
    private var useHERERouting = true
    
    override init(
        calculateRouteUseCase: CalculateRouteUseCase,
        getNavigationStateUseCase: GetNavigationStateUseCase
    ) {
        super.init(
            calculateRouteUseCase: calculateRouteUseCase,
            getNavigationStateUseCase: getNavigationStateUseCase
        )
        
        // Initialize HERE SDK
        // Enable routing engine toggle
    }
    
    func toggleRoutingEngine() {
        useHERERouting.toggle()
        eventSubject.send(.routingEngineToggled(useHERERouting))
    }
    
    func calculateTruckRoute(vehicleProfile: VehicleProfile) {
        // Use HERE SDK for truck routing
    }
}
#endif
```

---

## Key Architecture Principles

### 1. Separation of Concerns
- ViewControllers handle UI
- ViewModels manage state
- Repositories coordinate data
- UseCases contain business logic

### 2. Reactive Programming
- Combine for state management
- Publishers for one-way data flow
- Subjects for events

### 3. Coordinator Pattern
- Navigation logic separate from VCs
- Child coordinators for features
- Dependency injection via DI container

### 4. MVVM Pattern
- Clear separation of presentation logic
- Testable ViewModels
- Reactive bindings

### 5. Tier Isolation
- Conditional compilation via build flags
- Separate ViewModels for tier-specific logic
- Protocol-based abstractions
