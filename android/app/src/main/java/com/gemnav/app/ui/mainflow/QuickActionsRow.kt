package com.gemnav.app.ui.mainflow

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gemnav.app.models.Destination

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun QuickActionsRow(
    home: Destination?,
    work: Destination?,
    onHomeClick: () -> Unit,
    onWorkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickActionCard(
            icon = Icons.Default.Home,
            label = home?.name ?: "Set Home",
            onClick = onHomeClick,
            enabled = home != null,
            modifier = Modifier.weight(1f)
        )
        
        QuickActionCard(
            icon = Icons.Default.Work,
            label = work?.name ?: "Set Work",
            onClick = onWorkClick,
            enabled = work != null,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) MaterialTheme.colorScheme.primary 
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface 
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
