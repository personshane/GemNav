package com.gemnav.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun SettingsScreen(navController: NavController) {
    var darkMode by remember { mutableStateOf(false) }
    var voiceGuidance by remember { mutableStateOf(true) }
    var metricUnits by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(24.dp))

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

        Button(
            onClick = {
                // TODO: navigate to favorites when route exists
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Manage Favorites")
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "GemNav v0.1 (dev)",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun SettingsToggle(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
