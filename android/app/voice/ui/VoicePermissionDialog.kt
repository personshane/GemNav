package com.gemnav.app.voice.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Dialog for requesting microphone permission
 * Shows rationale and guides user through permission flow
 */
@Composable
fun VoicePermissionDialog(
    permissionState: PermissionState,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Microphone permission",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = when (permissionState) {
                    PermissionState.NotRequested -> "Enable Voice Commands"
                    PermissionState.Denied -> "Microphone Access Required"
                    PermissionState.PermanentlyDenied -> "Enable in Settings"
                    else -> "Voice Commands"
                },
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = when (permissionState) {
                    PermissionState.NotRequested ->
                        "GemNav uses your microphone to enable hands-free voice commands during navigation. " +
                        "Your voice data is processed securely and never stored without your permission."
                    PermissionState.Denied ->
                        "Voice commands require microphone access. Please grant permission to use this feature. " +
                        "You can always disable voice commands in settings."
                    PermissionState.PermanentlyDenied ->
                        "Microphone permission was denied. To enable voice commands, please go to Settings > " +
                        "Apps > GemNav > Permissions and enable Microphone access."
                    else -> ""
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when (permissionState) {
                        PermissionState.PermanentlyDenied -> onOpenSettings()
                        else -> onRequestPermission()
                    }
                }
            ) {
                Text(
                    text = when (permissionState) {
                        PermissionState.PermanentlyDenied -> "Open Settings"
                        else -> "Grant Permission"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not Now")
            }
        }
    )
}

/**
 * Permission states for voice input
 */
enum class PermissionState {
    NotRequested,       // Permission not yet requested
    Granted,            // Permission granted
    Denied,             // Permission denied but can be requested again
    PermanentlyDenied   // Permission permanently denied (user must go to settings)
}
