package com.gemnav.app.voice.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Voice input button with animated states
 * 
 * States:
 * - Idle: Gray microphone icon
 * - Listening: Pulsing red animation
 * - Processing: Spinner with mic icon
 * - Error: Red background with shake animation
 */
@Composable
fun VoiceButton(
    state: VoiceButtonState,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (state) {
        VoiceButtonState.Idle -> MaterialTheme.colorScheme.surfaceVariant
        VoiceButtonState.Listening -> MaterialTheme.colorScheme.error
        VoiceButtonState.Processing -> MaterialTheme.colorScheme.primary
        VoiceButtonState.Error -> MaterialTheme.colorScheme.error
    }
    
    val iconColor = when (state) {
        VoiceButtonState.Idle -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onPrimary
    }
    
    // Pulsing animation for listening state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    // Shake animation for error state
    val shakeOffset by remember {
        derivedStateOf {
            if (state == VoiceButtonState.Error) {
                listOf(-10f, 10f, -10f, 10f, 0f)
            } else {
                listOf(0f)
            }
        }
    }
    
    Box(
        modifier = modifier
            .size(64.dp)
            .scale(if (state == VoiceButtonState.Listening) pulseScale else 1f)
            .background(
                color = backgroundColor.copy(alpha = if (enabled) 1f else 0.5f),
                shape = CircleShape
            )
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (state == VoiceButtonState.Processing) {
            // Show spinner for processing state
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = iconColor,
                strokeWidth = 3.dp
            )
        } else {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = when (state) {
                    VoiceButtonState.Idle -> "Start voice input"
                    VoiceButtonState.Listening -> "Listening"
                    VoiceButtonState.Processing -> "Processing"
                    VoiceButtonState.Error -> "Voice input error"
                },
                tint = iconColor,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

/**
 * Voice button states
 */
enum class VoiceButtonState {
    Idle,       // Ready for input
    Listening,  // Currently listening to user
    Processing, // Processing voice command
    Error       // Error occurred
}
