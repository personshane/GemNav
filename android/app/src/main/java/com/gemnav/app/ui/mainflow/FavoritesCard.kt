package com.gemnav.app.ui.mainflow

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gemnav.app.models.Destination

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun FavoritesCard(
    favorites: List<Destination>,
    onFavoriteClick: (Destination) -> Unit,
    onToggleFavorite: (Destination) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Favorites",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            favorites.forEach { favorite ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFavoriteClick(favorite) }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = favorite.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = favorite.address,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    IconButton(onClick = { onToggleFavorite(favorite) }) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Remove from favorites",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                if (favorite != favorites.last()) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}
