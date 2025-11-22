package com.gemnav.android.voice.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import com.gemnav.android.voice.VoiceState

@Composable
fun VoiceButton(
    state: VoiceState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val buttonColor = when (state) {
        VoiceState.Idle -> MaterialTheme.colorScheme.primary
        VoiceState.Listening -> Color(0xFFE53935)
        VoiceState.Processing -> MaterialTheme.colorScheme.secondary
        VoiceState.Speaking -> MaterialTheme.colorScheme.tertiary
        is VoiceState.Error -> MaterialTheme.colorScheme.error
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val scale = when (state) {
        VoiceState.Listening -> pulseScale
        else -> 1f
    }

    FloatingActionButton(
        onClick = { if (enabled) onClick() },
        modifier = modifier.scale(scale),
        containerColor = buttonColor,
        contentColor = Color.White
    ) {
        when (state) {
            VoiceState.Processing -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice input"
                )
            }
        }
    }
}