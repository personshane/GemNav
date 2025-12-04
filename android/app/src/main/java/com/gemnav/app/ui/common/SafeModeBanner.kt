package com.gemnav.app.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gemnav.core.safety.SafeModeManager

/**
 * Safe Mode color definitions.
 */
object SafeModeColors {
    val WarningBackground = Color(0xFFFFF3E0)
    val WarningText = Color(0xFFE65100)
    val WarningIcon = Color(0xFFFF9800)
}

/**
 * Banner displayed when Safe Mode is active.
 */
@Composable
fun SafeModeBanner(
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null
) {
    val isSafeMode = SafeModeManager.isSafeModeEnabled()
    
    AnimatedVisibility(
        visible = isSafeMode,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        Surface(
            color = SafeModeColors.WarningBackground,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = SafeModeColors.WarningIcon,
                    modifier = Modifier.size(20.dp)
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Safe Mode Active",
                        style = MaterialTheme.typography.labelMedium,
                        color = SafeModeColors.WarningText
                    )
                    Text(
                        text = "Some features are temporarily disabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = SafeModeColors.WarningText.copy(alpha = 0.8f)
                    )
                }
                
                if (onDismiss != null) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = SafeModeColors.WarningText
                        )
                    ) {
                        Text("RESET")
                    }
                }
            }
        }
    }
}

/**
 * Banner displayed when a feature requires upgrade.
 */
@Composable
fun FeatureLockedBanner(
    message: String,
    requiredTier: String,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked",
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Requires $requiredTier",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
            
            TextButton(onClick = onUpgradeClick) {
                Text("Upgrade")
            }
        }
    }
}

/**
 * Modifier extension for feature gating visual effect.
 */
fun Modifier.featureGated(enabled: Boolean): Modifier {
    return if (enabled) {
        this
    } else {
        this.alpha(0.5f)
    }
}
