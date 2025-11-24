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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gemnav.app.BuildConfig
import com.gemnav.core.feature.FeatureGate
import com.gemnav.core.here.HereEngineManager
import com.gemnav.core.shim.SafeModeManager
import com.gemnav.data.route.LatLng
import com.gemnav.data.route.TruckRouteData

/**
 * HereMapContainer - Pro-tier only HERE SDK map composable.
 * Handles map initialization, lifecycle, route rendering, and error states.
 */
@Composable
fun HereMapContainer(
    modifier: Modifier = Modifier,
    routeData: TruckRouteData? = null,
    centerLocation: LatLng? = null,
    onMapReady: () -> Unit = {},
    onMapError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val TAG = "HereMapContainer"
    
    // State
    var mapState by remember { mutableStateOf<MapState>(MapState.Initializing) }
    var isMapReady by remember { mutableStateOf(false) }
    
    // Check prerequisites
    val isSafeModeActive = SafeModeManager.isSafeModeEnabled()
    val isProTier = FeatureGate.areCommercialRoutingFeaturesEnabled()
    val hasValidKeys = BuildConfig.HERE_API_KEY.isNotBlank() && 
                       BuildConfig.HERE_MAP_KEY.isNotBlank()
    
    LaunchedEffect(Unit) {
        when {
            isSafeModeActive -> {
                Log.w(TAG, "Map blocked - SafeMode active")
                mapState = MapState.Error("Safe Mode is active")
                onMapError("Safe Mode is active")
            }
            !isProTier -> {
                Log.w(TAG, "Map blocked - not Pro tier")
                mapState = MapState.Error("Pro subscription required")
                onMapError("Pro subscription required")
            }
            !hasValidKeys -> {
                Log.w(TAG, "Map blocked - missing API keys")
                mapState = MapState.Error("HERE API keys not configured")
                onMapError("HERE API keys not configured")
            }
            else -> {
                // Initialize HERE engine
                val success = HereEngineManager.initialize(context)
                if (success) {
                    Log.i(TAG, "HERE engine initialized")
                    mapState = MapState.Ready
                    isMapReady = true
                    onMapReady()
                } else {
                    val error = HereEngineManager.getError() ?: "Unknown error"
                    Log.e(TAG, "HERE engine init failed: $error")
                    mapState = MapState.Error(error)
                    onMapError(error)
                }
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
            is MapState.Initializing -> {
                MapLoadingPlaceholder()
            }
            is MapState.Ready -> {
                // TODO: Replace with actual HERE MapView when SDK integrated
                HereMapViewStub(
                    routeData = routeData,
                    centerLocation = centerLocation
                )
            }
            is MapState.Error -> {
                MapErrorDisplay(message = state.message)
            }
        }
    }
}

/**
 * Map initialization states.
 */
private sealed class MapState {
    object Initializing : MapState()
    object Ready : MapState()
    data class Error(val message: String) : MapState()
}

/**
 * Loading placeholder while map initializes.
 */
@Composable
private fun MapLoadingPlaceholder() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Loading map...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Error display when map fails to load.
 */
@Composable
private fun MapErrorDisplay(message: String) {
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
 * Stub MapView for pipeline testing.
 * TODO: Replace with actual HERE MapView integration.
 */
@Composable
private fun HereMapViewStub(
    routeData: TruckRouteData?,
    centerLocation: LatLng?
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2D3748)), // Dark map-like background
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Map,
                contentDescription = "Map",
                tint = Color(0xFF4A5568),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "HERE Map (Stub Mode)",
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF718096)
            )
            
            if (routeData != null) {
                Spacer(modifier = Modifier.height(16.dp))
                RouteOverlayStub(routeData)
            }
            
            centerLocation?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Center: ${String.format("%.4f", it.latitude)}, ${String.format("%.4f", it.longitude)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF718096)
                )
            }
        }
    }
}

/**
 * Visual indicator of route data on stub map.
 */
@Composable
private fun RouteOverlayStub(routeData: TruckRouteData) {
    Surface(
        color = Color(0xFF3182CE).copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Route Polyline Ready",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF3182CE)
            )
            Text(
                text = "${routeData.polylineCoordinates.size} points",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF718096)
            )
            if (routeData.warnings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${routeData.warnings.size} warnings",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE53E3E)
                )
            }
        }
    }
}
