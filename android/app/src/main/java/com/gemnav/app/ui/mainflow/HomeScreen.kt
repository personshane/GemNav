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
import com.gemnav.app.ui.voice.VoiceButton
import com.gemnav.app.ui.voice.VoiceButtonState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToRoute: () -> Unit,
    onSettingsClick: () -> Unit,
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
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            VoiceButton(
                state = VoiceButtonState.Idle,
                onClick = { }
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
                onSearch = { },
                isSearching = false
            )
            
            QuickActionsRow(
                home = home,
                work = work,
                onHomeClick = { },
                onWorkClick = { }
            )
            
            FavoritesCard(
                favorites = favorites,
                onFavoriteClick = { },
                onToggleFavorite = { }
            )
            
            RecentDestinationsCard(
                destinations = recentDestinations,
                onDestinationClick = { },
                onToggleFavorite = { }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
