package com.gemnav.android.voice.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WakeWordIndicator(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha_pulse"
    )

    if (isActive) {
        Surface(
            modifier = modifier
                .alpha(alpha)
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF4CAF50).copy(alpha = 0.9f),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.Hearing,
                    contentDescription = "Wake word active",
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
                Text(
                    "Say \"Hey GemNav\"",
                    color = Color.White,
                    fontSize = 12.sp,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}