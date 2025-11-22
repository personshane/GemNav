package com.gemnav.android.navigation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.gemnav.android.voice.ui.VoiceButton
import com.gemnav.android.voice.ui.VoiceFeedbackOverlay
import com.gemnav.android.voice.ui.VoicePermissionDialog
import com.gemnav.android.voice.ui.WakeWordIndicator
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.here.sdk.core.GeoCoordinates
import com.here.sdk.navigation.Navigator
import com.here.sdk.routing.Route as HereRoute
import java.util.*

class NavigationActivity : ComponentActivity() {
    private val viewModel: NavigationViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var tts: TextToSpeech? = null
    
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                startLocationUpdates()
            }
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                startLocationUpdates()
            }
            else -> {
                viewModel.onNavigationError("Location permission required for navigation")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        val tier = intent.getStringExtra(EXTRA_TIER) ?: "free"
        val routeJson = intent.getStringExtra(EXTRA_ROUTE)
        val hereRoute = intent.getParcelableExtra<HereRoute>(EXTRA_HERE_ROUTE)
        
        viewModel.initialize(tier, routeJson, hereRoute)
        
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                viewModel.setTtsReady(true)
            }
        }
        
        setContent {
            NavigationScreen(
                viewModel = viewModel,
                onExit = { finish() }
            )
        }
        
        requestLocationPermissions()
    }
    
    private fun requestLocationPermissions() {
        when {
            hasLocationPermission() -> startLocationUpdates()
            else -> locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
    
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun startLocationUpdates() {
        if (!hasLocationPermission()) return
        
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000L // Update every second
        ).build()
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    viewModel.updateLocation(location)
                    checkForVoiceGuidance(location)
                }
            }
        }
        
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            mainLooper
        )
    }
    
    private fun checkForVoiceGuidance(location: Location) {
        viewModel.getNextInstruction(location)?.let { instruction ->
            speakInstruction(instruction)
        }
    }
    
    private fun speakInstruction(instruction: String) {
        tts?.speak(instruction, TextToSpeech.QUEUE_FLUSH, null, null)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        tts?.stop()
        tts?.shutdown()
        viewModel.cleanup()
    }
    
    companion object {
        private const val EXTRA_TIER = "tier"
        private const val EXTRA_ROUTE = "route"
        private const val EXTRA_HERE_ROUTE = "here_route"
        
        fun newIntent(
            context: Context,
            tier: String,
            routeJson: String? = null,
            hereRoute: HereRoute? = null
        ): Intent {
            return Intent(context, NavigationActivity::class.java).apply {
                putExtra(EXTRA_TIER, tier)
                routeJson?.let { putExtra(EXTRA_ROUTE, it) }
                hereRoute?.let { putExtra(EXTRA_HERE_ROUTE, it) }
            }
        }
    }
}

@Composable
fun NavigationScreen(
    viewModel: NavigationViewModel,
    onExit: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()
    val tier = uiState.tier
    val context = LocalContext.current
    
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.initializeVoice(context)
    }
    
    Scaffold(
        topBar = {
            NavigationTopBar(
                destination = uiState.destination,
                onExit = {
                    viewModel.stopNavigation()
                    onExit()
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (tier) {
                "free" -> {
                    // Free tier uses Google Maps app - this screen is minimal/not shown
                    FreeTierNavigationMessage()
                }
                "plus" -> {
                    // Plus tier: Google Maps SDK in-app navigation
                    GoogleMapsNavigation(
                        currentLocation = uiState.currentLocation,
                        route = uiState.route,
                        onMapLoaded = { viewModel.onMapReady() }
                    )
                }
                "pro" -> {
                    // Pro tier: HERE SDK navigation
                    HereNavigation(
                        currentLocation = uiState.currentLocation,
                        hereRoute = uiState.hereRoute,
                        onMapLoaded = { viewModel.onMapReady() }
                    )
                }
            }
            
            // Wake word indicator (Plus/Pro only)
            if (tier in listOf("plus", "pro") && voiceState.wakeWordActive) {
                WakeWordIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                )
            }
            
            // Voice feedback overlay
            VoiceFeedbackOverlay(
                isVisible = voiceState.feedbackMessage != null,
                message = voiceState.feedbackMessage ?: "",
                type = voiceState.feedbackType,
                onDismiss = { viewModel.clearVoiceFeedback() }
            )
            
            // Overlay navigation info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                NavigationInfoCard(
                    nextInstruction = uiState.nextInstruction,
                    distanceToNextTurn = uiState.distanceToNextTurn,
                    eta = uiState.eta,
                    remainingDistance = uiState.remainingDistance
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                NavigationControls(
                    onRecenter = { viewModel.recenterMap() },
                    onMute = { viewModel.toggleMute() },
                    isMuted = uiState.isMuted,
                    voiceState = voiceState,
                    onVoiceClick = {
                        if (voiceState.permissionGranted) {
                            if (voiceState.isListening) {
                                viewModel.stopVoiceListening()
                            } else {
                                viewModel.startVoiceListening()
                            }
                        } else {
                            showPermissionDialog = true
                        }
                    }
                )
            }
            
            if (uiState.error != null) {
                ErrorSnackbar(
                    error = uiState.error!!,
                    onDismiss = { viewModel.clearError() }
                )
            }
        }
    }
    
    // Voice permission dialog
    if (showPermissionDialog) {
        VoicePermissionDialog(
            onGranted = {
                viewModel.onVoicePermissionGranted()
                showPermissionDialog = false
            },
            onDenied = {
                showPermissionDialog = false
            },
            onDismiss = {
                showPermissionDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationTopBar(
    destination: String?,
    onExit: () -> Unit
) {
    TopAppBar(
        title = { Text(destination ?: "Navigation") },
        navigationIcon = {
            IconButton(onClick = onExit) {
                Icon(Icons.Default.Close, "Exit navigation")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    )
}

@Composable
fun FreeTierNavigationMessage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Navigation,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Navigation in Google Maps",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Your route is now active in the Google Maps app",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun GoogleMapsNavigation(
    currentLocation: LatLng?,
    route: List<LatLng>?,
    onMapLoaded: () -> Unit
) {
    val cameraPositionState = rememberCameraPositionState {
        position = currentLocation?.let {
            com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(it, 16f)
        } ?: com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(
            LatLng(37.7749, -122.4194), 12f
        )
    }
    
    LaunchedEffect(currentLocation) {
        currentLocation?.let {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(it, 16f),
                500
            )
        }
    }
    
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = true),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false
        ),
        onMapLoaded = onMapLoaded
    ) {
        route?.let { points ->
            if (points.isNotEmpty()) {
                Polyline(
                    points = points,
                    color = androidx.compose.ui.graphics.Color.Blue,
                    width = 10f
                )
            }
        }
    }
}

@Composable
fun HereNavigation(
    currentLocation: LatLng?,
    hereRoute: HereRoute?,
    onMapLoaded: () -> Unit
) {
    // HERE SDK map view integration
    // Note: Requires AndroidView wrapping HERE MapView
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("HERE SDK Map View - Implementation pending")
        // TODO: Integrate HERE MapView via AndroidView
    }
}

@Composable
fun NavigationInfoCard(
    nextInstruction: String?,
    distanceToNextTurn: String?,
    eta: String?,
    remainingDistance: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            nextInstruction?.let {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.TurnRight,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            it,
                            style = MaterialTheme.typography.titleMedium
                        )
                        distanceToNextTurn?.let { dist ->
                            Text(
                                dist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                eta?.let {
                    Column {
                        Text(
                            "ETA",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                remainingDistance?.let {
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            "Distance",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NavigationControls(
    onRecenter: () -> Unit,
    onMute: () -> Unit,
    isMuted: Boolean,
    voiceState: VoiceState,
    onVoiceClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        FloatingActionButton(
            onClick = onRecenter,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Icon(Icons.Default.MyLocation, "Recenter")
        }
        
        VoiceButton(
            isListening = voiceState.isListening,
            isProcessing = voiceState.isProcessing,
            onClick = onVoiceClick
        )
        
        FloatingActionButton(
            onClick = onMute,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Icon(
                if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                if (isMuted) "Unmute" else "Mute"
            )
        }
    }
}

@Composable
fun ErrorSnackbar(
    error: String,
    onDismiss: () -> Unit
) {
    Snackbar(
        modifier = Modifier.padding(16.dp),
        action = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    ) {
        Text(error)
    }
}
