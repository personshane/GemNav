package com.gemnav.app.ui.mainflow

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gemnav.app.models.Destination

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun RecentDestinationsCard(
    destinations: List<Destination>,
    onDestinationClick: (Destination) -> Unit,
    onToggleFavorite: (Destination) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Recent",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            destinations.forEach { destination ->
                DestinationItem(
                    destination = destination,
                    onClick = { onDestinationClick(destination) },
                    onToggleFavorite = { onToggleFavorite(destination) }
                )
                if (destination != destinations.last()) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun DestinationItem(
    destination: Destination,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = destination.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = destination.address,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (destination.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = if (destination.isFavorite) "Remove from favorites" else "Add to favorites",
                tint = if (destination.isFavorite) MaterialTheme.colorScheme.primary 
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
