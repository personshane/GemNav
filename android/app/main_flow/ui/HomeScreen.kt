package com.gemnav.android.app.main_flow.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gemnav.android.app.main_flow.HomeViewModel
import com.gemnav.android.app.main_flow.models.Destination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToRoute: (Destination) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val recentDestinations by viewModel.recentDestinations.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val home by viewModel.home.collectAsState()
    val work by viewModel.work.collectAsState()
    
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
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::onSearchQueryChanged,
                    onSearch = { /* Handle search */ },
                    isSearching = isSearching
                )
            }
            
            item {
                QuickActionsRow(
                    home = home,
                    work = work,
                    onHomeClick = { home?.let(onNavigateToRoute) },
                    onWorkClick = { work?.let(onNavigateToRoute) }
                )
            }
            
            if (recentDestinations.isNotEmpty()) {
                item {
                    RecentDestinationsCard(
                        destinations = recentDestinations,
                        onDestinationClick = onNavigateToRoute,
                        onToggleFavorite = viewModel::toggleFavorite
                    )
                }
            }
            
            if (favorites.isNotEmpty()) {
                item {
                    FavoritesCard(
                        favorites = favorites,
                        onFavoriteClick = onNavigateToRoute,
                        onToggleFavorite = viewModel::toggleFavorite
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
