package com.gemnav.app.ui.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gemnav.core.feature.FeatureGate

/**
 * Voice button states.
 */
enum class VoiceButtonState {
    Idle,
    Listening,
    Disabled
}

/**
 * Reusable voice button component with feature gating and visual states.
 */
@Composable
fun VoiceButton(
    state: VoiceButtonState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    val isEnabled = state != VoiceButtonState.Disabled && 
                    FeatureGate.areAdvancedFeaturesEnabled()
    val isListening = state == VoiceButtonState.Listening
    
    // Pulsing animation for listening state
    val infiniteTransition = rememberInfiniteTransition(label = "voicePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voicePulseScale"
    )
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .scale(if (isListening) pulseScale else 1f)
            .clip(CircleShape)
            .background(
                when {
                    !isEnabled -> MaterialTheme.colorScheme.surfaceVariant
                    isListening -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.primaryContainer
                }
            )
            .then(
                if (isListening) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
                } else Modifier
            )
            .clickable(enabled = isEnabled) { onClick() }
    ) {
        Icon(
            imageVector = when {
                !isEnabled -> Icons.Default.MicOff
                isListening -> Icons.Default.Stop
                else -> Icons.Default.Mic
            },
            contentDescription = when {
                !isEnabled -> "Voice disabled"
                isListening -> "Stop listening"
                else -> "Start voice input"
            },
            modifier = Modifier.size(size * 0.4f),
            tint = when {
                !isEnabled -> MaterialTheme.colorScheme.onSurfaceVariant
                isListening -> MaterialTheme.colorScheme.onPrimary
                else -> MaterialTheme.colorScheme.onPrimaryContainer
            }
        )
    }
}

/**
 * Compact voice button for embedding in other screens.
 */
@Composable
fun CompactVoiceButton(
    isListening: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val state = when {
        !enabled -> VoiceButtonState.Disabled
        isListening -> VoiceButtonState.Listening
        else -> VoiceButtonState.Idle
    }
    
    VoiceButton(
        state = state,
        onClick = onClick,
        modifier = modifier,
        size = 48.dp
    )
}
