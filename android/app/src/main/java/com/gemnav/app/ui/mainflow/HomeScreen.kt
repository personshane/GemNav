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
import com.gemnav.app.ui.voice.VoiceButton
import com.gemnav.app.ui.voice.VoiceButtonState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToRoute: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    
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
                home = null,
                work = null,
                onHomeClick = { },
                onWorkClick = { }
            )
            
            FavoritesCard(
                favorites = emptyList(),
                onFavoriteClick = { },
                onToggleFavorite = { }
            )
            
            RecentDestinationsCard(
                destinations = emptyList(),
                onDestinationClick = { },
                onToggleFavorite = { }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
