import Foundation

/// Configuration for app dependencies.
/// Loads API keys and configuration from environment or Info.plist.
struct AppConfiguration {
    let googleApiKey: String
    let geminiApiKey: String
    let hereApiKey: String
    
    static func loadFromEnvironment() -> AppConfiguration {
        // TODO: Load from Info.plist or secure keychain
        return AppConfiguration(
            googleApiKey: "YOUR_GOOGLE_API_KEY_HERE",
            geminiApiKey: "YOUR_GEMINI_API_KEY_HERE",
            hereApiKey: "YOUR_HERE_API_KEY_HERE"
        )
    }
}

/// Protocol for dependency injection container.
protocol DependencyContainer {
    // Core
    var tierManager: TierManager { get }
    var config: AppConfiguration { get }
    
    // API Clients
    var geminiApiClient: GeminiApiClient { get }
    var placesApiClient: PlacesApiClient { get }
    var directionsApiClient: DirectionsApiClient { get }
    var hereApiClient: HereApiClient { get }
    
    // Repositories
    var destinationRepository: DestinationRepository { get }
    var searchRepository: SearchRepository { get }
    var routeRepository: RouteRepository { get }
    
    // Services
    var voiceCommandManager: VoiceCommandManager { get }
    var speechRecognitionService: SpeechRecognitionService { get }
    var voiceResponseService: VoiceResponseService { get }
    var commandParser: CommandParser { get }
    var commandExecutor: CommandExecutor { get }
    var wakeWordDetector: WakeWordDetector? { get }
    
    // ViewModels
    func makeHomeViewModel() -> HomeViewModel
    func makeRoutePreviewViewModel() -> RoutePreviewViewModel
    func makeNavigationViewModel() -> NavigationViewModel
    func makeSearchViewModel() -> SearchViewModel
}

/// Default implementation of dependency container.
class AppDependencyContainer: DependencyContainer {
    
    // MARK: - Core
    
    lazy var config: AppConfiguration = {
        AppConfiguration.loadFromEnvironment()
    }()
    
    lazy var tierManager: TierManager = {
        DefaultTierManager()
    }()
    
    // MARK: - API Clients
    
    lazy var geminiApiClient: GeminiApiClient = {
        GeminiApiClient(
            apiKey: config.geminiApiKey,
            tier: tierManager.getCurrentTier()
        )
    }()
    
    lazy var placesApiClient: PlacesApiClient = {
        PlacesApiClient(apiKey: config.googleApiKey)
    }()
    
    lazy var directionsApiClient: DirectionsApiClient = {
        DirectionsApiClient(apiKey: config.googleApiKey)
    }()
    
    lazy var hereApiClient: HereApiClient = {
        HereApiClient(apiKey: config.hereApiKey)
    }()
    
    // MARK: - Repositories
    
    lazy var destinationRepository: DestinationRepository = {
        // TODO: Inject DAO when database layer is implemented
        DestinationRepository()
    }()
    
    lazy var searchRepository: SearchRepository = {
        SearchRepository(
            placesClient: placesApiClient
            // TODO: Inject DAO for search history
        )
    }()
    
    lazy var routeRepository: RouteRepository = {
        RouteRepository(
            tierManager: tierManager,
            directionsClient: directionsApiClient,
            hereClient: hereApiClient
        )
    }()
    
    // MARK: - Services
    
    lazy var speechRecognitionService: SpeechRecognitionService = {
        IOSSpeechRecognitionService(tier: tierManager.getCurrentTier())
    }()
    
    lazy var voiceResponseService: VoiceResponseService = {
        IOSVoiceResponseService()
    }()
    
    lazy var commandParser: CommandParser = {
        CommandParser(
            geminiService: geminiApiClient,
            tier: tierManager.getCurrentTier()
        )
    }()
    
    lazy var commandExecutor: CommandExecutor = {
        // CommandExecutor needs ViewModel references, created during ViewModel initialization
        fatalError("CommandExecutor should be created with ViewModel dependencies")
    }()
    
    lazy var wakeWordDetector: WakeWordDetector? = {
        // Only for Plus/Pro tiers
        guard tierManager.getCurrentTier().isPlus else { return nil }
        return IOSWakeWordDetector()
    }()
    
    lazy var voiceCommandManager: VoiceCommandManager = {
        // VoiceCommandManager needs CommandExecutor which needs ViewModels
        fatalError("VoiceCommandManager should be created after ViewModels")
    }()
    
    // MARK: - ViewModel Factories
    
    func makeHomeViewModel() -> HomeViewModel {
        HomeViewModel(
            destinationRepository: destinationRepository,
            geminiClient: geminiApiClient,
            tierManager: tierManager
        )
    }
    
    func makeRoutePreviewViewModel() -> RoutePreviewViewModel {
        RoutePreviewViewModel(
            routeRepository: routeRepository,
            tierManager: tierManager
        )
    }
    
    func makeNavigationViewModel() -> NavigationViewModel {
        NavigationViewModel(
            tierManager: tierManager,
            voiceResponseService: voiceResponseService
        )
    }
    
    func makeSearchViewModel() -> SearchViewModel {
        SearchViewModel(
            searchRepository: searchRepository,
            tierManager: tierManager
        )
    }
}
