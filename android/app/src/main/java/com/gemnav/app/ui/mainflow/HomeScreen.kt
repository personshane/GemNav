package com.gemnav.app.ui.mainflow

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gemnav.app.ui.common.SafeModeBanner
import com.gemnav.app.ui.voice.VoiceButton
import com.gemnav.app.ui.voice.VoiceButtonState
import com.gemnav.core.feature.FeatureGate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val viewModel: HomeViewModel = viewModel()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    
    val favorites by viewModel.favorites.collectAsState()
    val recentDestinations by viewModel.recent.collectAsState()
    val home by viewModel.home.collectAsState()
    val work by viewModel.work.collectAsState()
    val featureSummary by viewModel.featureSummary.collectAsState()
    
    // Feature availability states
    val isAIEnabled = featureSummary.aiFeatures
    val isAdvancedVoiceEnabled = featureSummary.advancedVoice
    val isSafeModeActive = featureSummary.isSafeModeActive
    
    LaunchedEffect(Unit) {
        viewModel.loadMockData()
        viewModel.refreshFeatureState()
    }
    
    Scaffold(
        topBar = {
            Column {
                // Safe Mode Banner at top
                SafeModeBanner(isVisible = isSafeModeActive)
                
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
            }
        },
        floatingActionButton = {
            // Voice button - always visible but behavior changes based on tier
            VoiceButton(
                state = if (isSafeModeActive) VoiceButtonState.Disabled else VoiceButtonState.Idle,
                onClick = {
                    if (!isSafeModeActive) {
                        navController.navigate("voice")
                    }
                    // TODO: Show safe mode message if disabled
                }
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
            Spacer(modifier = Modifier.height(8.dp))
            
            // Search bar - AI suggestions disabled in safe mode
            SearchBar(
                query = searchQuery,
                onQueryChange = { 
                    searchQuery = it
                    // Only trigger AI suggestions if enabled
                    if (isAIEnabled && it.length >= 3) {
                        viewModel.getAISuggestions(it)
                    }
                },
                onSearch = { navController.navigate("search") },
                isSearching = false
            )
            
            // Show hint when AI is disabled
            if (!isAIEnabled && searchQuery.isNotEmpty()) {
                Text(
                    text = if (isSafeModeActive) {
                        "AI suggestions unavailable in compatibility mode"
                    } else {
                        "Upgrade to Plus for AI-powered suggestions"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
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
