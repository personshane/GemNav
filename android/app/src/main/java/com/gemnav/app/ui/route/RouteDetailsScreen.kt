package com.gemnav.app.ui.route

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Warning
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SafeModeBanner()

        Text(
            text = destination.name,
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = destination.address,
            style = MaterialTheme.typography.bodyLarge
        )

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
    onRequestTruckRoute: () -> Unit,
    onClearRoute: () -> Unit
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
                            onClear = onClearRoute
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
    onClear: () -> Unit
) {
    Column {
        // HERE Map Display (Pro tier only)
        HereMapContainer(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            routeData = routeData,
            centerLocation = routeData.polylineCoordinates.firstOrNull(),
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
                modifier = Modifier.weight(1f)
            ) {
                Text("Clear")
            }
            Button(
                onClick = { /* TODO: Start navigation */ },
                modifier = Modifier.weight(1f)
            ) {
                Text("Start Navigation")
            }
        }
    }
}
