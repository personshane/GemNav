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
import com.gemnav.app.ui.voice.VoiceButton
import com.gemnav.app.ui.voice.VoiceButtonState

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
    
    LaunchedEffect(Unit) {
        viewModel.loadMockData()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GemNav") },
                actions = {
                    IconButton(
                        onClick = { /* TODO: Navigate to settings when route exists */ }
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            VoiceButton(
                state = VoiceButtonState.Idle,
                onClick = { /* TODO: Navigate to voice when route exists */ }
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
            
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { /* TODO: Navigate to search when route exists */ },
                isSearching = false
            )
            
            QuickActionsRow(
                home = home,
                work = work,
                onHomeClick = { /* TODO: Navigate to search when route exists */ },
                onWorkClick = { /* TODO: Navigate to search when route exists */ }
            )
            
            FavoritesCard(
                favorites = favorites,
                onFavoriteClick = { destination ->
                    /* TODO: Navigate to routeDetails/${destination.id} when route exists */
                },
                onToggleFavorite = { /* TODO: Implement favorite toggle */ }
            )
            
            RecentDestinationsCard(
                destinations = recentDestinations,
                onDestinationClick = { destination ->
                    /* TODO: Navigate to routeDetails/${destination.id} when route exists */
                },
                onToggleFavorite = { /* TODO: Implement favorite toggle */ }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
