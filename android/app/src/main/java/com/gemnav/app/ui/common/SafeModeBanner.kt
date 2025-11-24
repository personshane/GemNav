package com.gemnav.app.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * SafeModeBanner - Displays when Safe Mode is active.
 * Non-intrusive banner informing users that advanced features are temporarily disabled.
 */
@Composable
fun SafeModeBanner(
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(expandFrom = Alignment.Top),
        exit = shrinkVertically(shrinkTowards = Alignment.Top),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SafeModeColors.BannerBackground)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "GemNav is running in compatibility mode. Advanced features temporarily disabled.",
                color = SafeModeColors.BannerText,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * Colors for Safe Mode UI elements.
 */
object SafeModeColors {
    val BannerBackground = Color(0xFFFFF3CD) // Amber/warning yellow
    val BannerText = Color(0xFF856404) // Dark amber
    val DisabledOverlay = Color(0x80000000) // Semi-transparent black
    val DisabledButton = Color(0xFFBDBDBD) // Grey
}

/**
 * FeatureLockedBanner - Shows when a specific feature requires upgrade.
 */
@Composable
fun FeatureLockedBanner(
    featureName: String,
    requiredTier: String,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(expandFrom = Alignment.Top),
        exit = shrinkVertically(shrinkTowards = Alignment.Top),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE3F2FD)) // Light blue
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$featureName requires $requiredTier. Tap to upgrade.",
                color = Color(0xFF1565C0), // Dark blue
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * Modifier extension for disabled state styling.
 */
fun Modifier.featureGated(isEnabled: Boolean): Modifier {
    return if (isEnabled) {
        this
    } else {
        this.background(SafeModeColors.DisabledOverlay.copy(alpha = 0.1f))
    }
}
