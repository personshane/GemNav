package com.gemnav.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gemnav.app.ui.common.SafeModeBanner
import com.gemnav.core.feature.FeatureGate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val viewModel: SettingsViewModel = viewModel()
    
    val featureSummary by viewModel.featureSummary.collectAsState()
    val safeModeStatus by viewModel.safeModeStatus.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isVoiceEnabled by viewModel.isVoiceEnabled.collectAsState()
    val distanceUnit by viewModel.distanceUnit.collectAsState()
    val isTruckModeEnabled by viewModel.isTruckModeEnabled.collectAsState()
    
    val isSafeModeActive = featureSummary.isSafeModeActive
    val currentTier = featureSummary.tier
    
    LaunchedEffect(Unit) {
        viewModel.refreshState()
    }

    Scaffold(
        topBar = {
            Column {
                SafeModeBanner(isVisible = isSafeModeActive)
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
        ) {
            // Subscription tier info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Current Plan",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = currentTier.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    if (currentTier != FeatureGate.SubscriptionTier.PRO) {
                        Button(onClick = { /* TODO: Upgrade flow */ }) {
                            Text("Upgrade")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // General Settings
            Text(
                text = "General",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            
            SettingsToggle(
                title = "Dark Mode",
                checked = isDarkMode,
                onCheckedChange = { viewModel.setDarkMode(it) }
            )
            
            SettingsToggle(
                title = "Voice Guidance",
                checked = isVoiceEnabled,
                onCheckedChange = { viewModel.setVoiceEnabled(it) }
            )
            
            SettingsOption(
                title = "Distance Unit",
                value = if (distanceUnit == SettingsViewModel.DistanceUnit.MILES) "Miles" else "Kilometers",
                onClick = {
                    viewModel.setDistanceUnit(
                        if (distanceUnit == SettingsViewModel.DistanceUnit.MILES) 
                            SettingsViewModel.DistanceUnit.KILOMETERS 
                        else SettingsViewModel.DistanceUnit.MILES
                    )
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Pro Features
            Text(
                text = "Pro Features",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            
            SettingsToggle(
                title = "Truck Routing Mode",
                subtitle = if (featureSummary.commercialRouting) "Commercial-grade routing enabled"
                          else "Requires Pro subscription",
                checked = isTruckModeEnabled,
                onCheckedChange = { viewModel.setTruckModeEnabled(it) },
                enabled = featureSummary.commercialRouting && !isSafeModeActive
            )
            
            // TODO: Add truck specs configuration
            if (isTruckModeEnabled && featureSummary.commercialRouting) {
                OutlinedButton(
                    onClick = { /* TODO: Open truck specs dialog */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp)
                ) {
                    Text("Configure Truck Specifications")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Safe Mode section
            if (isSafeModeActive) {
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
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Compatibility Mode Active",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Recent failures: ${safeModeStatus.recentFailureCount}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        TextButton(onClick = { viewModel.disableSafeMode() }) {
                            Text("Reset")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // App info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "GemNav v0.1 (dev)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (safeModeStatus.hasVersionWarning) {
                    Text(
                        text = "SDK version warning active",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsToggle(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
fun SettingsOption(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        TextButton(onClick = onClick) {
            Text(value)
        }
    }
}
