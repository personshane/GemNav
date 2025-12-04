package com.gemnav.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gemnav.app.ui.common.SafeModeBanner
import com.gemnav.core.feature.FeatureGate
import com.gemnav.app.ui.location.LocationViewModel
import com.gemnav.core.safety.SafeModeManager
import com.gemnav.core.subscription.Tier
import com.gemnav.core.subscription.TierManager
import com.gemnav.core.subscription.displayName
import com.gemnav.core.subscription.description
import com.gemnav.core.subscription.priceString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    @Suppress("UNUSED_PARAMETER") viewModel: SettingsViewModel = viewModel()
) {
    val currentTier by TierManager.currentTier.collectAsState()
    val featureSummary = FeatureGate.getFeatureSummary()
    val scrollState = rememberScrollState()
    
    val locationViewModel: LocationViewModel = viewModel()
    val hasLocationPermission by locationViewModel.hasPermission.collectAsState()
    val locationStatus by locationViewModel.locationStatus.collectAsState()
    
    var darkMode by remember { mutableStateOf(false) }
    var voiceGuidance by remember { mutableStateOf(true) }
    var metricUnits by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        locationViewModel.checkPermission()
    }
    
    Scaffold(
        topBar = {
            Column {
                SafeModeBanner(
                    onDismiss = { SafeModeManager.disableSafeMode() }
                )
                TopAppBar(
                    title = { Text("Settings") },
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
                .verticalScroll(scrollState)
        ) {
            // Subscription Section
            SubscriptionCard(
                currentTier = currentTier,
                onUpgradeClick = { targetTier ->
                    // TODO: Launch billing flow
                    // For now, debug set tier
                    TierManager.debugSetTier(targetTier)
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Feature Status Section
            FeatureStatusSection(featureSummary)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Location Permission Section
            LocationPermissionSection(
                hasPermission = hasLocationPermission,
                status = locationStatus,
                onRequestPermission = { /* TODO: Launch permission request */ }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Safe Mode Section
            if (featureSummary.isSafeModeActive) {
                SafeModeCard(
                    onResetClick = { SafeModeManager.disableSafeMode() }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // General Settings
            Text(
                text = "General",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            SettingsToggle(
                title = "Dark Mode",
                checked = darkMode,
                onCheckedChange = { darkMode = it }
            )
            SettingsToggle(
                title = "Voice Guidance",
                checked = voiceGuidance,
                onCheckedChange = { voiceGuidance = it }
            )
            SettingsToggle(
                title = "Use Metric Units",
                checked = metricUnits,
                onCheckedChange = { metricUnits = it }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Pro Features (Truck Settings)
            if (currentTier == Tier.PRO) {
                TruckSettingsSection()
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // About Section
            Button(
                onClick = { /* TODO: navigate to favorites */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Manage Favorites")
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = "GemNav v0.1 (dev)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SubscriptionCard(
    currentTier: Tier,
    onUpgradeClick: (Tier) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (currentTier) {
                Tier.FREE -> MaterialTheme.colorScheme.surfaceVariant
                Tier.PLUS -> MaterialTheme.colorScheme.primaryContainer
                Tier.PRO -> MaterialTheme.colorScheme.tertiaryContainer
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Current Plan",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = currentTier.displayName(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Icon(
                    imageVector = when (currentTier) {
                        Tier.FREE -> Icons.Default.Person
                        Tier.PLUS -> Icons.Default.Star
                        Tier.PRO -> Icons.Default.LocalShipping
                    },
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Text(
                text = currentTier.description(),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            // Upgrade buttons
            if (currentTier != Tier.PRO) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (currentTier == Tier.FREE) {
                        OutlinedButton(
                            onClick = { onUpgradeClick(Tier.PLUS) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Plus ${Tier.PLUS.priceString()}")
                        }
                    }
                    
                    Button(
                        onClick = { onUpgradeClick(Tier.PRO) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Pro ${Tier.PRO.priceString()}")
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureStatusSection(summary: FeatureGate.FeatureSummary) {
    Text(
        text = "Feature Status",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            FeatureRow("AI Features", summary.aiFeatures, Icons.Default.Psychology)
            FeatureRow("Cloud AI", summary.cloudAI, Icons.Default.Cloud)
            FeatureRow("In-App Maps", summary.inAppMaps, Icons.Default.Map)
            FeatureRow("Advanced Voice", summary.advancedVoice, Icons.Default.Mic)
            FeatureRow("Multi-Waypoint", summary.multiWaypoint, Icons.Default.Route)
            FeatureRow("Truck Routing", summary.commercialRouting, Icons.Default.LocalShipping)
        }
    }
}

@Composable
fun FeatureRow(name: String, enabled: Boolean, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (enabled) MaterialTheme.colorScheme.primary 
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(name, style = MaterialTheme.typography.bodyMedium)
        }
        
        Icon(
            imageVector = if (enabled) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = if (enabled) "Enabled" else "Disabled",
            tint = if (enabled) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun SafeModeCard(onResetClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFE65100)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Safe Mode Active",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Some features are temporarily disabled due to SDK issues. " +
                       "Try resetting to restore full functionality.",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onResetClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE65100)
                )
            ) {
                Text("Reset Safe Mode")
            }
        }
    }
}

@Composable
fun TruckSettingsSection() {
    Text(
        text = "Truck Settings",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // TODO: Add truck spec inputs
            Text(
                text = "Configure your vehicle specifications for accurate routing.",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedButton(
                onClick = { /* TODO: Open truck config */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.LocalShipping, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Configure Vehicle")
            }
        }
    }
}

@Composable
fun SettingsToggle(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun LocationPermissionSection(
    hasPermission: Boolean,
    status: LocationViewModel.LocationStatus,
    onRequestPermission: () -> Unit
) {
    Text(
        text = "Location",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (hasPermission) Icons.Default.LocationOn else Icons.Default.LocationOff,
                        contentDescription = null,
                        tint = if (hasPermission) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Location Permission",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Text(
                    text = if (hasPermission) "Granted" else "Denied",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (hasPermission) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                )
            }
            
            if (!hasPermission) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Location is required for in-app navigation (Plus/Pro).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Request Permission")
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                val statusText = when (status) {
                    is LocationViewModel.LocationStatus.Active -> "GPS Active"
                    is LocationViewModel.LocationStatus.Searching -> "Searching..."
                    is LocationViewModel.LocationStatus.Error -> "Error: ${status.message}"
                    else -> "Idle"
                }
                Text(
                    text = "Status: $statusText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
