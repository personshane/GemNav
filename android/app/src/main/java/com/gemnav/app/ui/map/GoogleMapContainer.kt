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
import com.gemnav.core.shim.SafeModeManager
import com.gemnav.data.route.LatLng
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.*

/**
 * GoogleMapContainer - Plus/Pro tier Google Maps SDK composable.
 * Handles map initialization, lifecycle, and error states.
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
                    destinationLocation = destinationLocation
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
 * Google Maps Compose view with markers.
 * TODO: Add route polyline rendering when Gemini routing is implemented.
 */
@Composable
private fun GoogleMapView(
    centerLocation: LatLng?,
    originLocation: LatLng?,
    destinationLocation: LatLng?
) {
    // Default to Phoenix, AZ if no location provided
    val defaultCenter = com.google.android.gms.maps.model.LatLng(33.4484, -112.0740)
    
    val center = centerLocation?.let {
        com.google.android.gms.maps.model.LatLng(it.latitude, it.longitude)
    } ?: destinationLocation?.let {
        com.google.android.gms.maps.model.LatLng(it.latitude, it.longitude)
    } ?: defaultCenter
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(center, 12f)
    }
    
    val mapProperties by remember {
        mutableStateOf(
            MapProperties(
                mapType = MapType.NORMAL,
                isMyLocationEnabled = false // TODO: Enable with permission
            )
        )
    }
    
    val mapUiSettings by remember {
        mutableStateOf(
            MapUiSettings(
                zoomControlsEnabled = true,
                compassEnabled = true,
                myLocationButtonEnabled = false
            )
        )
    }
    
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = mapUiSettings
    ) {
        // Origin marker
        originLocation?.let { loc ->
            Marker(
                state = MarkerState(
                    position = com.google.android.gms.maps.model.LatLng(loc.latitude, loc.longitude)
                ),
                title = "Origin",
                snippet = "Start point"
            )
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
        
        // TODO: Add Polyline for navigation route when Gemini routing provides coordinates
        // Polyline(
        //     points = routePolylinePoints,
        //     color = Color.Blue,
        //     width = 8f
        // )
    }
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
