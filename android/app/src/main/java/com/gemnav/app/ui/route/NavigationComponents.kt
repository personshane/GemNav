package com.gemnav.app.ui.route

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gemnav.data.navigation.NavManeuver
import com.gemnav.data.navigation.NavigationState

/**
 * Navigation UI Overlays for turn-by-turn guidance.
 * MP-017: Turn-by-Turn Navigation Engine
 */

/**
 * Main navigation overlay showing current step, distance, and controls.
 */
@Composable
fun NavigationOverlay(
    state: NavigationState.Navigating,
    onStopNavigation: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Maneuver card
            ManeuverCard(
                instruction = state.currentStep.instruction,
                maneuver = state.currentStep.maneuverIcon,
                distanceMeters = state.distanceToNextMeters,
                streetName = state.currentStep.streetName
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = formatDistance(state.distanceRemainingMeters),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "ETA",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = formatDuration(state.etaSeconds),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Step",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${state.currentStepIndex + 1}/${state.totalSteps}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // Progress bar
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { state.progressPct },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
            )
            
            // Next step preview
            state.nextStep?.let { next ->
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Then:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = getManeuverIcon(next.maneuverIcon),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = next.instruction,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            maxLines = 1
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Stop button
            Button(
                onClick = onStopNavigation,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Stop Navigation")
            }
        }
    }
}

/**
 * Maneuver card showing turn icon and instruction.
 */
@Composable
fun ManeuverCard(
    instruction: String,
    maneuver: NavManeuver,
    distanceMeters: Double,
    streetName: String?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Maneuver icon
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = getManeuverIcon(maneuver),
                    contentDescription = maneuver.name,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Instruction text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatDistance(distanceMeters),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = instruction,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            streetName?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}


/**
 * Off-route overlay with recalculation option.
 */
@Composable
fun OffRouteOverlay(
    state: NavigationState.OffRoute,
    onRecalculate: () -> Unit,
    onStopNavigation: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.WrongLocation,
                    contentDescription = "Off Route",
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Off Route",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F)
                    )
                    Text(
                        text = state.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF757575)
                    )
                    Text(
                        text = "Deviation: ${formatDistance(state.deviationMeters)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF757575)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onStopNavigation,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Stop")
                }
                Button(
                    onClick = onRecalculate,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F)
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Recalculate")
                }
            }
        }
    }
}

/**
 * Recalculating route overlay.
 */
@Composable
fun RecalculatingOverlay() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Recalculating route...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

/**
 * Navigation finished overlay.
 */
@Composable
fun NavigationFinishedOverlay(
    state: NavigationState.Finished,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE8F5E9)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Arrived",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "You Have Arrived!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )
            state.destinationName?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF4CAF50)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatDistance(state.totalDistanceTraveled),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Text(
                        text = "Distance",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF66BB6A)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatDuration(state.totalTimeSeconds),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Text(
                        text = "Duration",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF66BB6A)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text("Done")
            }
        }
    }
}

/**
 * Blocked navigation overlay (SafeMode or Free tier).
 */
@Composable
fun BlockedOverlay(reason: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Block,
                contentDescription = "Blocked",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Navigation Unavailable",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}


// ==================== HELPER FUNCTIONS ====================

/**
 * Get icon for maneuver type.
 */
private fun getManeuverIcon(maneuver: NavManeuver): androidx.compose.ui.graphics.vector.ImageVector {
    return when (maneuver) {
        NavManeuver.STRAIGHT -> Icons.Default.ArrowUpward
        NavManeuver.LEFT -> Icons.Default.TurnLeft
        NavManeuver.RIGHT -> Icons.Default.TurnRight
        NavManeuver.SLIGHT_LEFT -> Icons.Default.TurnSlightLeft
        NavManeuver.SLIGHT_RIGHT -> Icons.Default.TurnSlightRight
        NavManeuver.SHARP_LEFT -> Icons.Default.TurnSharpLeft
        NavManeuver.SHARP_RIGHT -> Icons.Default.TurnSharpRight
        NavManeuver.UTURN -> Icons.Default.UTurnLeft
        NavManeuver.MERGE -> Icons.Default.Merge
        NavManeuver.EXIT -> Icons.Default.ExitToApp
        NavManeuver.ROUNDABOUT -> Icons.Default.RotateRight
        NavManeuver.FERRY -> Icons.Default.DirectionsBoat
        NavManeuver.ARRIVE -> Icons.Default.Flag
        NavManeuver.DEPART -> Icons.Default.TripOrigin
    }
}

/**
 * Format distance in meters to human-readable string.
 */
private fun formatDistance(meters: Double): String {
    return when {
        meters < 100 -> "${meters.toInt()} m"
        meters < 1000 -> "${(meters / 10).toInt() * 10} m"
        meters < 10000 -> String.format("%.1f km", meters / 1000)
        else -> String.format("%.0f km", meters / 1000)
    }
}

/**
 * Format duration in seconds to human-readable string.
 */
private fun formatDuration(seconds: Long): String {
    return when {
        seconds < 60 -> "< 1 min"
        seconds < 3600 -> "${seconds / 60} min"
        else -> {
            val hours = seconds / 3600
            val mins = (seconds % 3600) / 60
            if (mins == 0L) "${hours}h" else "${hours}h ${mins}m"
        }
    }
}
