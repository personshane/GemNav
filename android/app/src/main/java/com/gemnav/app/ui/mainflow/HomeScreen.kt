package com.gemnav.app.ui.mainflow

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gemnav.app.ui.common.SafeModeBanner
import com.gemnav.app.ui.voice.VoiceButton
import com.gemnav.app.ui.voice.VoiceButtonState
import com.gemnav.core.location.LocationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val viewModel: HomeViewModel = viewModel()
    val locationViewModel: LocationViewModel = viewModel()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    
    val favorites by viewModel.favorites.collectAsState()
    val recentDestinations by viewModel.recent.collectAsState()
    val home by viewModel.home.collectAsState()
    val work by viewModel.work.collectAsState()
    
    val locationStatus by locationViewModel.locationStatus.collectAsState()
    val hasLocationPermission by locationViewModel.hasPermission.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadMockData()
        locationViewModel.refreshLastLocation()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GemNav") },
                actions = {
                    IconButton(
                        onClick = { navController.navigate("settings") }
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            VoiceButton(
                state = VoiceButtonState.Idle,
                onClick = { navController.navigate("voice") }
            )
        }
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SafeModeBanner()
            
            // GPS Status Indicator
            GpsStatusChip(
                status = locationStatus,
                hasPermission = hasLocationPermission
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { navController.navigate("search") },
                isSearching = false
            )
            
            QuickActionsRow(
                home = home,
                work = work,
                onHomeClick = { navController.navigate("search") },
                onWorkClick = { navController.navigate("search") }
            )
            
            FavoritesCard(
                favorites = favorites,
                onFavoriteClick = { destination ->
                    navController.navigate("routeDetails/${destination.id}")
                },
                onToggleFavorite = { /* TODO: Implement favorite toggle */ }
            )
            
            RecentDestinationsCard(
                destinations = recentDestinations,
                onDestinationClick = { destination ->
                    navController.navigate("routeDetails/${destination.id}")
                },
                onToggleFavorite = { /* TODO: Implement favorite toggle */ }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * GPS status indicator chip.
 */
@Composable
private fun GpsStatusChip(
    status: LocationViewModel.LocationStatus,
    hasPermission: Boolean
) {
    val (icon, label, color) = when {
        !hasPermission -> Triple(
            Icons.Default.GpsOff,
            "GPS: Permission Required",
            MaterialTheme.colorScheme.error
        )
        status is LocationViewModel.LocationStatus.Active -> Triple(
            Icons.Default.GpsFixed,
            "GPS: OK",
            MaterialTheme.colorScheme.primary
        )
        status is LocationViewModel.LocationStatus.Searching -> Triple(
            Icons.Default.GpsNotFixed,
            "GPS: Searching...",
            MaterialTheme.colorScheme.tertiary
        )
        status is LocationViewModel.LocationStatus.Error -> Triple(
            Icons.Default.GpsOff,
            "GPS: ${status.message}",
            MaterialTheme.colorScheme.error
        )
        else -> Triple(
            Icons.Default.GpsNotFixed,
            "GPS: Idle",
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
