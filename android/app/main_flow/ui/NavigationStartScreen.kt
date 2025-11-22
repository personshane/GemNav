package com.gemnav.android.main_flow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gemnav.android.main_flow.models.Destination
import com.gemnav.android.main_flow.models.Route
import com.gemnav.android.main_flow.models.RouteOptions
import com.gemnav.android.tier.TierLevel

@Composable
fun NavigationStartScreen(
    currentTier: TierLevel,
    route: Route,
    routeOptions: RouteOptions,
    origin: Destination,
    destination: Destination,
    onStartNavigation: () -> Unit,
    onRouteOptionsChange: (RouteOptions) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SmallTopAppBar(
            title = { Text("Start Navigation") },
            navigationIcon = {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.ArrowBack, "Back")
                }
            },
            colors = TopAppBarDefaults.smallTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RouteInfoCard(
                route = route,
                origin = origin,
                destination = destination
            )

            if (currentTier != TierLevel.FREE) {
                RouteOptionsCard(
                    currentTier = currentTier,
                    routeOptions = routeOptions,
                    onRouteOptionsChange = onRouteOptionsChange
                )
            }

            if (currentTier == TierLevel.PRO) {
                ProFeatureReminders()
            }

            PermissionsInfoCard(
                currentTier = currentTier,
                onLearnMorePermissions = { showPermissionDialog = true },
                onLearnMoreNotifications = { showNotificationDialog = true }
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentTier == TierLevel.FREE) {
                    Text(
                        text = "Navigation will open in Google Maps app",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onStartNavigation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (currentTier == TierLevel.FREE) "Open Google Maps" else "Start Navigation",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }

    if (showPermissionDialog) {
        PermissionExplanationDialog(
            onDismiss = { showPermissionDialog = false }
        )
    }

    if (showNotificationDialog) {
        NotificationExplanationDialog(
            onDismiss = { showNotificationDialog = false }
        )
    }
}

@Composable
private fun RouteInfoCard(
    route: Route,
    origin: Destination,
    destination: Destination,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = route.distanceText,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Distance",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = route.durationText,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Duration",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            RoutePoint(
                icon = Icons.Default.Circle,
                iconTint = MaterialTheme.colorScheme.primary,
                title = "From",
                address = origin.name ?: origin.address
            )

            Spacer(modifier = Modifier.height(8.dp))

            RoutePoint(
                icon = Icons.Default.Place,
                iconTint = MaterialTheme.colorScheme.error,
                title = "To",
                address = destination.name ?: destination.address
            )
        }
    }
}

@Composable
private fun RoutePoint(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    address: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = address,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun RouteOptionsCard(
    currentTier: TierLevel,
    routeOptions: RouteOptions,
    onRouteOptionsChange: (RouteOptions) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Route Options",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            RouteOptionToggle(
                label = "Avoid Tolls",
                checked = routeOptions.avoidTolls,
                onCheckedChange = { onRouteOptionsChange(routeOptions.copy(avoidTolls = it)) }
            )

            RouteOptionToggle(
                label = "Avoid Highways",
                checked = routeOptions.avoidHighways,
                onCheckedChange = { onRouteOptionsChange(routeOptions.copy(avoidHighways = it)) }
            )

            RouteOptionToggle(
                label = "Avoid Ferries",
                checked = routeOptions.avoidFerries,
                onCheckedChange = { onRouteOptionsChange(routeOptions.copy(avoidFerries = it)) }
            )

            if (currentTier == TierLevel.PRO) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                RouteOptionToggle(
                    label = "Commercial Vehicle Mode",
                    checked = routeOptions.useCommercialRouting,
                    onCheckedChange = { onRouteOptionsChange(routeOptions.copy(useCommercialRouting = it)) },
                    subtitle = if (routeOptions.useCommercialRouting) "HERE SDK routing active" else "Google Maps routing active"
                )
            }
        }
    }
}

@Composable
private fun RouteOptionToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun ProFeatureReminders() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Pro Mode Active",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "Legal compliance checking enabled for commercial vehicles",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
private fun PermissionsInfoCard(
    currentTier: TierLevel,
    onLearnMorePermissions: () -> Unit,
    onLearnMoreNotifications: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Required Permissions",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            PermissionInfoRow(
                icon = Icons.Default.LocationOn,
                text = if (currentTier != TierLevel.FREE) "Background location for turn-by-turn guidance" else "Location access for navigation",
                onClick = onLearnMorePermissions
            )

            if (currentTier != TierLevel.FREE) {
                PermissionInfoRow(
                    icon = Icons.Default.Notifications,
                    text = "Notifications for route alerts and updates",
                    onClick = onLearnMoreNotifications
                )
            }
        }
    }
}

@Composable
private fun PermissionInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onClick) {
            Text("Learn More")
        }
    }
}

@Composable
private fun PermissionExplanationDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.LocationOn, contentDescription = null)
        },
        title = {
            Text("Location Permission")
        },
        text = {
            Text("GemNav needs location access to provide navigation. Background location allows turn-by-turn guidance even when the app is not in the foreground.")
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        }
    )
}

@Composable
private fun NotificationExplanationDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Notifications, contentDescription = null)
        },
        title = {
            Text("Notification Permission")
        },
        text = {
            Text("Notifications keep you informed about route changes, traffic alerts, and arrival times while navigating.")
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        }
    )
}
