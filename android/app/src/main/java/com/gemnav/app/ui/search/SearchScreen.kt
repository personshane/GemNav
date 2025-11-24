package com.gemnav.app.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
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
fun SearchScreen(navController: NavController) {
    val viewModel: SearchViewModel = viewModel()
    
    val query by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val featureSummary by viewModel.featureSummary.collectAsState()
    
    val isSafeModeActive = featureSummary.isSafeModeActive
    val isAIEnabled = featureSummary.aiFeatures
    val isInAppMapsEnabled = featureSummary.inAppMaps
    
    LaunchedEffect(Unit) {
        viewModel.refreshFeatureState()
    }

    Scaffold(
        topBar = {
            Column {
                SafeModeBanner(isVisible = isSafeModeActive)
                TopAppBar(
                    title = { Text("Search") },
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
            // Search input
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                label = { Text("Search destination") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Feature status hint
            if (!isAIEnabled || !isInAppMapsEnabled) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when {
                        isSafeModeActive -> "Search running in compatibility mode"
                        !isInAppMapsEnabled -> "Upgrade to Plus for in-app search"
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Error message
            errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Loading indicator
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            }
            
            // Search results
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(searchResults) { result ->
                    SearchResultItem(
                        result = result,
                        onClick = {
                            navController.navigate("routeDetails/${result.id}")
                        }
                    )
                }
                
                // Empty state
                if (searchResults.isEmpty() && query.isNotBlank() && !isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No results found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultItem(result: Destination, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(result.name, style = MaterialTheme.typography.titleMedium)
            Text(result.address, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
