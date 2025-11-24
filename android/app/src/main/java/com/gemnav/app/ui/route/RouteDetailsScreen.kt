package com.gemnav.app.ui.route

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gemnav.app.models.Destination
import com.gemnav.app.ui.common.SafeModeBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailsScreen(
    navController: NavController,
    id: String,
    destinationProvider: (String) -> Destination? = { null }
) {
    val viewModel: RouteDetailsViewModel = viewModel()
    
    val routeState by viewModel.routeState.collectAsState()
    val isTruckMode by viewModel.isTruckMode.collectAsState()
    val featureSummary by viewModel.featureSummary.collectAsState()
    
    val isSafeModeActive = featureSummary.isSafeModeActive
    val isTruckRoutingEnabled = featureSummary.commercialRouting
    val isInAppMapsEnabled = featureSummary.inAppMaps
    
    val destination = destinationProvider(id) ?: Destination(
        name = "Mock Destination",
        address = "1234 Example St",
        id = id
    )

    var isFavorite by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.refreshFeatureState()
        viewModel.setDestination(destination)
    }

    Scaffold(
        topBar = {
            Column {
                SafeModeBanner(isVisible = isSafeModeActive)
                TopAppBar(
                    title = { Text("Route Details") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Destination info
            Text(
                text = destination.name,
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = destination.address,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Route type toggle (Pro tier only)
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = if (isTruckRoutingEnabled) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Truck Routing",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = if (isTruckRoutingEnabled) "Commercial-grade routing"
                                       else "Pro tier required",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isTruckMode,
                        onCheckedChange = { viewModel.setTruckMode(it) },
                        enabled = isTruckRoutingEnabled && !isSafeModeActive
                    )
                }
            }
            
            // Upgrade hint for truck routing
            if (!isTruckRoutingEnabled && !isSafeModeActive) {
                TextButton(
                    onClick = { /* TODO: Navigate to upgrade screen */ },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Upgrade to Pro for truck routing")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Navigate button
                Button(
                    onClick = {
                        if (isInAppMapsEnabled) {
                            viewModel.calculateRoute()
                        } else {
                            // TODO: Trigger Maps intent for Free tier
                        }
                    },
                    enabled = !isSafeModeActive,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Navigate")
                }

                // Favorite button
                OutlinedButton(
                    onClick = { isFavorite = !isFavorite },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isFavorite) "Unfavorite" else "Favorite")
                }
            }
            
            // Route calculation status
            when (val state = routeState) {
                is RouteDetailsViewModel.RouteState.Loading -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                is RouteDetailsViewModel.RouteState.Success -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    RouteInfoCard(route = state.route)
                }
                is RouteDetailsViewModel.RouteState.Error -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Recent activity (placeholder)
            Text(
                text = "Recent Activity",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("• Last visited: 2 days ago")
            Text("• Typical ETA: 14 min")
            Text("• Category: ${if (isTruckMode) "Commercial" else "General"}")
        }
    }
}

@Composable
private fun RouteInfoCard(route: RouteDetailsViewModel.RouteInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                        text = "${route.distanceMeters / 1000.0} km",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Column {
                    Text(
                        text = "Duration",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "${route.durationSeconds / 60} min",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            
            if (route.isTruckRoute) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (route.isFallback) "⚠️ Truck restrictions not verified"
                           else "✓ Truck-legal route",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            route.warnings.forEach { warning ->
                Text(
                    text = "⚠️ $warning",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
