package com.gemnav.app.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gemnav.data.models.Destination
import com.gemnav.app.ui.common.SafeModeBanner
import com.gemnav.core.feature.FeatureGate
import com.gemnav.data.ai.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController) {
    val viewModel: SearchViewModel = viewModel()
    
    var query by remember { mutableStateOf("") }
    val aiRouteState by viewModel.aiRouteState.collectAsState()
    val aiIntentState by viewModel.aiIntentState.collectAsState()
    val classifiedIntent by viewModel.classifiedIntent.collectAsState()
    val isAiEnabled = FeatureGate.areAIFeaturesEnabled()
    
    val mockResults = remember(query) {
        if (query.isBlank()) emptyList()
        else List(5) { index ->
            Destination(
                name = "Mock Result $index",
                address = "$query Street $index",
                id = UUID.randomUUID().toString()
            )
        }
    }
    
    // Handle AI route success - navigate to route details
    LaunchedEffect(aiRouteState) {
        if (aiRouteState is AiRouteState.Success) {
            val suggestion = (aiRouteState as AiRouteState.Success).suggestion
            // TODO: Pass suggestion to RouteDetailsScreen
            navController.navigate("routeDetails/ai_route")
            viewModel.clearAiRouteState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SafeModeBanner()
        
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (isAiEnabled && query.isNotBlank()) {
                    IconButton(
                        onClick = { viewModel.onAiRouteRequested(query) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Route",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        )
        
        // AI Route Status
        when (val state = aiRouteState) {
            is AiRouteState.Loading -> {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI planning route...", style = MaterialTheme.typography.bodySmall)
                }
            }
            is AiRouteState.Error -> {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            is AiRouteState.Success -> {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "AI route to: ${state.suggestion.destinationName}",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            else -> {}
        }
        
        // MP-020: AI Intent Classification Status
        AiIntentStatusPanel(aiIntentState, classifiedIntent)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // AI Route Button (Plus/Pro tiers)
        if (isAiEnabled && query.isNotBlank()) {
            OutlinedButton(
                onClick = { viewModel.onAiRouteRequested(query) },
                modifier = Modifier.fillMaxWidth(),
                enabled = aiRouteState !is AiRouteState.Loading
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Route")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(mockResults) { result ->
                SearchResultItem(
                    result = result,
                    onClick = {
                        navController.navigate("routeDetails/${result.id}")
                    }
                )
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

/**
 * MP-020: Panel showing AI intent classification progress.
 */
@Composable
fun AiIntentStatusPanel(
    intentState: AiIntentState,
    classifiedIntent: NavigationIntent?
) {
    if (intentState is AiIntentState.Idle) return
    
    Spacer(modifier = Modifier.height(8.dp))
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Status message
            val statusText = when (intentState) {
                is AiIntentState.Classifying -> "AI Understanding..."
                is AiIntentState.Reasoning -> intentState.statusMessage
                is AiIntentState.Suggesting -> intentState.statusMessage
                is AiIntentState.Success -> "Route found!"
                is AiIntentState.Error -> intentState.message
                else -> ""
            }
            
            if (statusText.isNotBlank()) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (intentState is AiIntentState.Error) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Show classified intent type
            classifiedIntent?.let { intent ->
                val intentLabel = when (intent) {
                    is NavigationIntent.NavigateTo -> "Navigate to: ${intent.destinationText}"
                    is NavigationIntent.FindPOI -> "Find POI: ${intent.poiType.name.lowercase().replace("_", " ")}"
                    is NavigationIntent.AddStop -> "Add Stop: ${intent.destinationText ?: intent.poiType?.name ?: ""}"
                    is NavigationIntent.RoutePreferences -> "Route Preferences"
                    is NavigationIntent.Question -> "Question"
                    is NavigationIntent.Unknown -> "Unknown"
                }
                Text(
                    text = "Intent: $intentLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
