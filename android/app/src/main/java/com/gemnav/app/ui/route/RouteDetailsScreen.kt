package com.gemnav.app.ui.route

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gemnav.app.models.Destination
import com.gemnav.app.ui.common.SafeModeBanner
import com.gemnav.app.ui.map.HereMapContainer
import com.gemnav.app.ui.map.GoogleMapContainer
import com.gemnav.core.location.LocationViewModel
import com.gemnav.core.navigation.DetourState
import com.gemnav.data.navigation.*
import com.gemnav.data.route.LatLng
import com.gemnav.data.route.TruckRouteData
import com.gemnav.data.route.TruckRouteState
import com.gemnav.data.route.WarningSeverity

@Composable
fun RouteDetailsScreen(
    navController: NavController,
    id: String,
    destinationProvider: (String) -> Destination? = { null }
) {
    val viewModel: RouteDetailsViewModel = viewModel()
    val locationViewModel: LocationViewModel = viewModel()
    
    val destination = destinationProvider(id) ?: Destination(
        name = "Mock Destination",
        address = "1234 Example St",
        id = id,
        latitude = 33.4484,  // Phoenix, AZ (mock)
        longitude = -112.0740
    )
    
    var isFavorite by remember { mutableStateOf(false) }
    val truckRouteState by viewModel.truckRouteState.collectAsState()
    val isProTier = viewModel.isProTier()
    val isPlusTier = viewModel.isPlusTier()
    
    val currentLocation by locationViewModel.currentLocation.collectAsState()
    val locationStatus by locationViewModel.locationStatus.collectAsState()
    
    // Navigation state (MP-017)
    val navigationState by viewModel.navigationState.collectAsState()
    val isNavigating by viewModel.isNavigating.collectAsState()
    val currentNavRoute by viewModel.currentNavRoute.collectAsState()
    
    // Google route state (MP-019)
    val googleRouteState by viewModel.googleRouteState.collectAsState()
    val googlePolyline by viewModel.currentGooglePolyline.collectAsState()
    
    // Detour state (MP-023)
    val detourState by viewModel.detourState.collectAsState()
    
    // Feed location updates to navigation engine
    LaunchedEffect(currentLocation) {
        currentLocation?.let { loc ->
            viewModel.updateUserLocation(LatLng(loc.latitude, loc.longitude))
        }
    }
    
    // Start location tracking for Plus/Pro tiers
    LaunchedEffect(isPlusTier) {
        if (isPlusTier) {
            locationViewModel.startTracking()
        }
    }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            locationViewModel.stopTracking()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SafeModeBanner()
        
        // MP-023: Detour panel for along-route POI (PLUS only)
        if (isPlusTier) {
            DetourPanel(
                detourState = detourState,
                onAddStop = { viewModel.onAddStopConfirmed() },
                onDismiss = { viewModel.onDetourDismissed() }
            )
        }
        
        // Navigation Mode UI (MP-017)
        when (val navState = navigationState) {
            is NavigationState.Navigating -> {
                NavigationOverlay(
                    state = navState,
                    onStopNavigation = { viewModel.stopNavigation() }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            is NavigationState.OffRoute -> {
                OffRouteOverlay(
                    state = navState,
                    onRecalculate = { viewModel.onOffRoute() },
                    onStopNavigation = { viewModel.stopNavigation() }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            is NavigationState.Recalculating -> {
                RecalculatingOverlay()
                Spacer(modifier = Modifier.height(8.dp))
            }
            is NavigationState.Finished -> {
                NavigationFinishedOverlay(
                    state = navState,
                    onDismiss = { viewModel.stopNavigation() }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            is NavigationState.Blocked -> {
                BlockedOverlay(reason = navState.reason)
                Spacer(modifier = Modifier.height(8.dp))
            }
            else -> { /* Idle, LoadingRoute - no overlay */ }
        }

        Text(
            text = destination.name,
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = destination.address,
            style = MaterialTheme.typography.bodyLarge
        )
        
        // Current Location Display (Plus/Pro tiers)
        if (isPlusTier || isProTier) {
            Spacer(modifier = Modifier.height(8.dp))
            CurrentLocationIndicator(
                location = currentLocation,
                status = locationStatus
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { /* TODO: Car navigation */ },
                modifier = Modifier.weight(1f)
            ) {
                Text("Navigate")
            }

            Button(
                onClick = { isFavorite = !isFavorite },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isFavorite) "Unfavorite" else "Favorite")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // ==================== Truck Route Section (Pro Tier) ====================
        
        TruckRouteSection(
            isProTier = isProTier,
            truckRouteState = truckRouteState,
            onRequestTruckRoute = {
                // Mock origin (current location placeholder)
                viewModel.requestTruckRoute(
                    startLat = 33.4255,  // Mock start
                    startLng = -111.9400,
                    endLat = destination.latitude,
                    endLng = destination.longitude
                )
            },
            onClearRoute = { viewModel.clearTruckRoute() }
        )
        
        // ==================== Plus Tier Map Section (MP-019) ====================
        
        if (isPlusTier && !isProTier) {
            Spacer(modifier = Modifier.height(16.dp))
            PlusTierMapSection(
                destination = destination,
                googleRouteState = googleRouteState,
                googlePolyline = googlePolyline,
                isNavigating = isNavigating,
                currentLocation = currentLocation?.let { LatLng(it.latitude, it.longitude) },
                currentNavRoute = currentNavRoute,
                onRequestRoute = { viewModel.requestGoogleRoute() },
                onStartNavigation = { viewModel.startNavigation() },
                onStopNavigation = { viewModel.stopNavigation() },
                onMapReady = { viewModel.onGoogleMapReady() },
                onMapError = { viewModel.onGoogleMapError(it) }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Recent Activity",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("• Last visited: 2 days ago")
        Text("• Typical ETA: 14 min")
        Text("• Category: General")
    }
}

@Composable
private fun TruckRouteSection(
    isProTier: Boolean,
    truckRouteState: TruckRouteState,
    isNavigating: Boolean = false,
    currentLocation: LatLng? = null,
    nextStep: NavStep? = null,
    onRequestTruckRoute: () -> Unit,
    onClearRoute: () -> Unit,
    onStartNavigation: () -> Unit = {},
    onStopNavigation: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isProTier) 
                MaterialTheme.colorScheme.tertiaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = "Truck",
                    tint = if (isProTier) 
                        MaterialTheme.colorScheme.onTertiaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Commercial Truck Routing",
                    style = MaterialTheme.typography.titleMedium
                )
                if (isProTier) {
                    Text(
                        text = "PRO",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (!isProTier) {
                Text(
                    text = "Upgrade to Pro for truck-legal routing with height, weight, and hazmat restrictions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { /* TODO: Navigate to subscription */ }
                ) {
                    Text("Upgrade to Pro")
                }
            } else {
                // Pro tier - show truck route functionality
                when (truckRouteState) {
                    is TruckRouteState.Idle -> {
                        Button(
                            onClick = onRequestTruckRoute,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Request Truck Route")
                        }
                    }
                    
                    is TruckRouteState.Loading -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Calculating truck route...")
                        }
                    }
                    
                    is TruckRouteState.Success -> {
                        val data = truckRouteState.data
                        TruckRouteResultCard(
                            routeData = data,
                            distanceKm = data.distanceMeters / 1000.0,
                            durationMinutes = data.durationSeconds / 60,
                            warningCount = data.warnings.size,
                            criticalWarnings = data.warnings.count { it.severity == WarningSeverity.CRITICAL },
                            isFallback = data.isFallback,
                            isNavigating = isNavigating,
                            currentLocation = currentLocation,
                            nextStep = nextStep,
                            onClear = onClearRoute,
                            onStartNavigation = onStartNavigation,
                            onStopNavigation = onStopNavigation
                        )
                    }
                    
                    is TruckRouteState.Error -> {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = truckRouteState.message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(onClick = onClearRoute) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TruckRouteResultCard(
    routeData: TruckRouteData,
    distanceKm: Double,
    durationMinutes: Long,
    warningCount: Int,
    criticalWarnings: Int,
    isFallback: Boolean,
    isNavigating: Boolean = false,
    currentLocation: LatLng? = null,
    nextStep: NavStep? = null,
    onClear: () -> Unit,
    onStartNavigation: () -> Unit = {},
    onStopNavigation: () -> Unit = {}
) {
    Column {
        // HERE Map Display (Pro tier only)
        HereMapContainer(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            routeData = routeData,
            centerLocation = routeData.polylineCoordinates.firstOrNull(),
            isNavigating = isNavigating,
            currentLocation = currentLocation,
            nextStep = nextStep,
            onMapReady = { /* Map ready */ },
            onMapError = { /* Handle error silently - UI shows fallback */ }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        if (isFallback) {
            Surface(
                color = Color(0xFFFFEBEE),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = Color(0xFFD32F2F)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "FALLBACK ROUTE - Restrictions not verified!",
                        color = Color(0xFFD32F2F),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Distance",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = String.format("%.1f km", distanceKm),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Column {
                Text(
                    text = "Duration",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = "${durationMinutes} min",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Column {
                Text(
                    text = "Warnings",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = "$warningCount",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (criticalWarnings > 0) 
                        MaterialTheme.colorScheme.error 
                    else if (warningCount > 0) 
                        Color(0xFFFF9800)
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onClear,
                modifier = Modifier.weight(1f),
                enabled = !isNavigating
            ) {
                Text("Clear")
            }
            Button(
                onClick = if (isNavigating) onStopNavigation else onStartNavigation,
                modifier = Modifier.weight(1f),
                colors = if (isNavigating) 
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                else 
                    ButtonDefaults.buttonColors()
            ) {
                Icon(
                    if (isNavigating) Icons.Default.Stop else Icons.Default.Navigation,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(if (isNavigating) "Stop" else "Navigate")
            }
        }
    }
}

/**
 * Plus tier map section with Google Maps and turn-by-turn navigation.
 * MP-019: Full Google Directions API integration.
 */
@Composable
private fun PlusTierMapSection(
    destination: Destination,
    googleRouteState: RouteDetailsViewModel.GoogleRouteState,
    googlePolyline: List<LatLng>,
    isNavigating: Boolean,
    currentLocation: LatLng?,
    currentNavRoute: NavRoute?,
    onRequestRoute: () -> Unit,
    onStartNavigation: () -> Unit,
    onStopNavigation: () -> Unit,
    onMapReady: () -> Unit,
    onMapError: (String) -> Unit
) {
    val nextStep = currentNavRoute?.steps?.firstOrNull()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "Map",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = if (isNavigating) "Navigation Active" else "Route Preview",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "PLUS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Map with route polyline
            GoogleMapContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isNavigating) 300.dp else 200.dp),
                destinationLocation = LatLng(destination.latitude, destination.longitude),
                centerLocation = currentLocation ?: LatLng(destination.latitude, destination.longitude),
                routePolyline = googlePolyline.takeIf { it.isNotEmpty() },
                isNavigating = isNavigating,
                currentLocation = currentLocation,
                nextStep = nextStep,
                onMapReady = onMapReady,
                onMapError = onMapError
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Route info or loading state
            when (googleRouteState) {
                is RouteDetailsViewModel.GoogleRouteState.Loading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Calculating route...")
                    }
                }
                is RouteDetailsViewModel.GoogleRouteState.Success -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Distance", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = formatDistance(googleRouteState.distanceMeters),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Duration", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = formatDuration(googleRouteState.durationSeconds),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                is RouteDetailsViewModel.GoogleRouteState.Error -> {
                    Text(
                        text = "Route error: ${googleRouteState.message}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                else -> { /* Idle state */ }
            }
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (googleRouteState is RouteDetailsViewModel.GoogleRouteState.Success) {
                    if (isNavigating) {
                        Button(
                            onClick = onStopNavigation,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Stop Navigation")
                        }
                    } else {
                        Button(
                            onClick = onStartNavigation,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Start Navigation")
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { /* TODO: Open in Google Maps app */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Open in Maps")
                    }
                    Button(
                        onClick = onRequestRoute,
                        modifier = Modifier.weight(1f),
                        enabled = googleRouteState !is RouteDetailsViewModel.GoogleRouteState.Loading
                    ) {
                        Text("Get Route")
                    }
                }
            }
        }
    }
}

/**
 * Format distance for display.
 */
private fun formatDistance(meters: Int): String {
    return if (meters < 1000) {
        "$meters m"
    } else {
        String.format("%.1f km", meters / 1000.0)
    }
}

/**
 * Format duration for display.
 */
private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    return if (minutes < 60) {
        "$minutes min"
    } else {
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        "${hours}h ${remainingMinutes}m"
    }
}

/**
 * Shows current GPS location for Plus/Pro tiers.
 * TODO: Replace with navigation overlay in MP-017.
 */
@Composable
private fun CurrentLocationIndicator(
    location: LatLng?,
    status: LocationViewModel.LocationStatus
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (status) {
                is LocationViewModel.LocationStatus.Active -> {
                    location?.let {
                        Text(
                            text = "📍 ${String.format("%.5f", it.latitude)}, ${String.format("%.5f", it.longitude)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                is LocationViewModel.LocationStatus.Searching -> {
                    CircularProgressIndicator(modifier = Modifier.size(12.dp))
                    Text(
                        text = "Acquiring GPS...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is LocationViewModel.LocationStatus.Error -> {
                    Text(
                        text = "GPS: ${status.message}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is LocationViewModel.LocationStatus.PermissionDenied -> {
                    Text(
                        text = "GPS: Permission required",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {
                    Text(
                        text = "GPS: Idle",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


// ==================== MP-023: DETOUR PANEL ====================

/**
 * Detour panel showing POI suggestion with detour cost.
 * PLUS TIER ONLY.
 */
@Composable
private fun DetourPanel(
    detourState: DetourState,
    onAddStop: () -> Unit,
    onDismiss: () -> Unit
) {
    when (detourState) {
        is DetourState.Idle -> { /* Nothing to show */ }
        
        is DetourState.Calculating -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text(
                        text = "Calculating detour...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        is DetourState.Ready -> {
            val poi = detourState.poi
            val info = detourState.detourInfo
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = poi.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            poi.address?.let { address ->
                                Text(
                                    text = address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Detour: ${info.formatDetour()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = onAddStop,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Stop & Navigate")
                    }
                }
            }
        }
        
        is DetourState.Error -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = detourState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
        
        is DetourState.Blocked -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = detourState.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
