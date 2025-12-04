package com.gemnav.app.ui.map

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gemnav.app.BuildConfig
import com.gemnav.core.feature.FeatureGate
import com.gemnav.core.safety.SafeModeManager
import com.gemnav.data.navigation.NavStep
import com.gemnav.data.route.LatLng
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.*

/**
 * GoogleMapContainer - Plus/Pro tier Google Maps SDK composable.
 * Handles map initialization, lifecycle, and error states.
 * Supports navigation mode with camera follow.
 * 
 * Note: Pro tier users will see HERE maps for truck routing,
 * but can switch to Google Maps for car routing.
 */
@Composable
fun GoogleMapContainer(
    modifier: Modifier = Modifier,
    originLocation: LatLng? = null,
    destinationLocation: LatLng? = null,
    centerLocation: LatLng? = null,
    isNavigating: Boolean = false,
    currentLocation: LatLng? = null,
    nextStep: NavStep? = null,
    routePolyline: List<LatLng>? = null,
    onMapReady: () -> Unit = {},
    onMapError: (String) -> Unit = {}
) {
    val TAG = "GoogleMapContainer"
    
    // State
    var mapState by remember { mutableStateOf<GoogleMapState>(GoogleMapState.Initializing) }
    
    // Check prerequisites
    val isSafeModeActive = SafeModeManager.isSafeModeEnabled()
    val isInAppMapsEnabled = FeatureGate.areInAppMapsEnabled()
    val hasValidKey = BuildConfig.GOOGLE_MAPS_API_KEY.isNotBlank()
    
    LaunchedEffect(Unit) {
        when {
            isSafeModeActive -> {
                Log.w(TAG, "Map blocked - SafeMode active")
                mapState = GoogleMapState.Error("Safe Mode is active")
                onMapError("Safe Mode is active")
            }
            !isInAppMapsEnabled -> {
                Log.w(TAG, "Map blocked - tier doesn't support in-app maps")
                mapState = GoogleMapState.Error("Plus subscription required for in-app maps")
                onMapError("Plus subscription required")
            }
            !hasValidKey -> {
                Log.w(TAG, "Map blocked - missing Google Maps API key")
                mapState = GoogleMapState.Error("Google Maps API key not configured")
                onMapError("Google Maps API key not configured")
            }
            else -> {
                Log.i(TAG, "Google Maps initialization approved")
                mapState = GoogleMapState.Ready
                onMapReady()
            }
        }
    }
    
    // Render based on state
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when (val state = mapState) {
            is GoogleMapState.Initializing -> {
                GoogleMapLoadingPlaceholder()
            }
            is GoogleMapState.Ready -> {
                GoogleMapView(
                    centerLocation = centerLocation,
                    originLocation = originLocation,
                    destinationLocation = destinationLocation,
                    isNavigating = isNavigating,
                    currentLocation = currentLocation,
                    nextStep = nextStep,
                    routePolyline = routePolyline
                )
            }
            is GoogleMapState.Error -> {
                GoogleMapErrorDisplay(message = state.message)
            }
        }
    }
}

/**
 * Google Map initialization states.
 */
private sealed class GoogleMapState {
    object Initializing : GoogleMapState()
    object Ready : GoogleMapState()
    data class Error(val message: String) : GoogleMapState()
}

/**
 * Loading placeholder while map initializes.
 */
@Composable
private fun GoogleMapLoadingPlaceholder() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Loading Google Maps...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Error display when map fails to load.
 */
@Composable
private fun GoogleMapErrorDisplay(message: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Error",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Map Unavailable",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Google Maps Compose view with markers and navigation support.
 */
@Composable
private fun GoogleMapView(
    centerLocation: LatLng?,
    originLocation: LatLng?,
    destinationLocation: LatLng?,
    isNavigating: Boolean = false,
    currentLocation: LatLng? = null,
    nextStep: NavStep? = null,
    routePolyline: List<LatLng>? = null
) {
    // Default to Phoenix, AZ if no location provided
    val defaultCenter = com.google.android.gms.maps.model.LatLng(33.4484, -112.0740)
    
    val center = when {
        isNavigating && currentLocation != null -> {
            com.google.android.gms.maps.model.LatLng(currentLocation.latitude, currentLocation.longitude)
        }
        centerLocation != null -> {
            com.google.android.gms.maps.model.LatLng(centerLocation.latitude, centerLocation.longitude)
        }
        destinationLocation != null -> {
            com.google.android.gms.maps.model.LatLng(destinationLocation.latitude, destinationLocation.longitude)
        }
        else -> defaultCenter
    }
    
    // Calculate bearing if navigating
    val bearing = if (isNavigating && currentLocation != null && nextStep != null) {
        calculateBearing(currentLocation, nextStep.location)
    } else {
        0f
    }
    
    // Zoom level: closer when navigating
    val zoomLevel = if (isNavigating) 17f else 12f
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.Builder()
            .target(center)
            .zoom(zoomLevel)
            .bearing(bearing)
            .tilt(if (isNavigating) 45f else 0f)
            .build()
    }
    
    // Update camera when location changes during navigation
    LaunchedEffect(currentLocation, isNavigating) {
        if (isNavigating && currentLocation != null) {
            val newPosition = CameraPosition.Builder()
                .target(com.google.android.gms.maps.model.LatLng(currentLocation.latitude, currentLocation.longitude))
                .zoom(17f)
                .bearing(if (nextStep != null) calculateBearing(currentLocation, nextStep.location) else 0f)
                .tilt(45f)
                .build()
            cameraPositionState.animate(CameraUpdateFactory.newCameraPosition(newPosition))
        }
    }
    
    val mapProperties by remember {
        mutableStateOf(
            MapProperties(
                mapType = MapType.NORMAL,
                isMyLocationEnabled = false // TODO: Enable with permission
            )
        )
    }
    
    val mapUiSettings by remember(isNavigating) {
        mutableStateOf(
            MapUiSettings(
                zoomControlsEnabled = !isNavigating,
                compassEnabled = true,
                myLocationButtonEnabled = false,
                scrollGesturesEnabled = !isNavigating,
                zoomGesturesEnabled = !isNavigating,
                rotationGesturesEnabled = !isNavigating
            )
        )
    }
    
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = mapUiSettings
    ) {
        // Current location marker (during navigation)
        if (isNavigating && currentLocation != null) {
            Marker(
                state = MarkerState(
                    position = com.google.android.gms.maps.model.LatLng(currentLocation.latitude, currentLocation.longitude)
                ),
                title = "You",
                snippet = "Current location"
            )
        }
        
        // Origin marker (not during navigation)
        if (!isNavigating) {
            originLocation?.let { loc ->
                Marker(
                    state = MarkerState(
                        position = com.google.android.gms.maps.model.LatLng(loc.latitude, loc.longitude)
                    ),
                    title = "Origin",
                    snippet = "Start point"
                )
            }
        }
        
        // Destination marker
        destinationLocation?.let { loc ->
            Marker(
                state = MarkerState(
                    position = com.google.android.gms.maps.model.LatLng(loc.latitude, loc.longitude)
                ),
                title = "Destination",
                snippet = "End point"
            )
        }
        
        // Route polyline
        routePolyline?.takeIf { it.size >= 2 }?.let { points ->
            Polyline(
                points = points.map { com.google.android.gms.maps.model.LatLng(it.latitude, it.longitude) },
                color = Color(0xFF1976D2),
                width = 12f
            )
        }
    }
}

/**
 * Calculate bearing from p1 to p2 in degrees.
 */
private fun calculateBearing(p1: LatLng, p2: LatLng): Float {
    val lat1 = Math.toRadians(p1.latitude)
    val lat2 = Math.toRadians(p2.latitude)
    val dLon = Math.toRadians(p2.longitude - p1.longitude)
    
    val y = kotlin.math.sin(dLon) * kotlin.math.cos(lat2)
    val x = kotlin.math.cos(lat1) * kotlin.math.sin(lat2) - kotlin.math.sin(lat1) * kotlin.math.cos(lat2) * kotlin.math.cos(dLon)
    
    var bearing = Math.toDegrees(kotlin.math.atan2(y, x))
    bearing = (bearing + 360) % 360
    
    return bearing.toFloat()
}

/**
 * Stub version for testing without API key.
 * Used when Google Maps SDK can't initialize.
 */
@Composable
fun GoogleMapStub(
    modifier: Modifier = Modifier,
    centerLocation: LatLng?
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF3D5A80)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Map,
                contentDescription = "Map",
                tint = Color(0xFF98C1D9),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Google Maps (Stub Mode)",
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFFE0FBFC)
            )
            Text(
                text = "Configure API key in local.properties",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF98C1D9)
            )
            
            centerLocation?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Center: ${String.format("%.4f", it.latitude)}, ${String.format("%.4f", it.longitude)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF98C1D9)
                )
            }
        }
    }
}
