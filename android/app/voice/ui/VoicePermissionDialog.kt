package com.gemnav.android.voice.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun VoicePermissionDialog(
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit,
    showRationale: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Mic,
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                if (showRationale) "Microphone Permission Required" 
                else "Enable Voice Commands",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    if (showRationale) {
                        "GemNav needs microphone access to enable voice commands during navigation. " +
                        "Your voice is processed locally on your device (Free tier) or securely sent to " +
                        "Google Cloud (Plus/Pro tiers) for enhanced understanding."
                    } else {
                        "Use voice commands to navigate hands-free. Say \"Navigate to [destination]\" " +
                        "or \"Find gas stations nearby\" without touching your phone."
                    }
                )
                if (showRationale) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Please grant permission in Settings to use this feature.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onRequestPermission) {
                Text(if (showRationale) "Open Settings" else "Allow")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (showRationale) "Not Now" else "Cancel")
            }
        }
    )
}