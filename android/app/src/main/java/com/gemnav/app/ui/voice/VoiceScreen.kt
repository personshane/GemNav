package com.gemnav.app.ui.voice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gemnav.app.ui.common.SafeModeBanner
import com.gemnav.core.feature.FeatureGate
import com.gemnav.data.ai.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(
    navController: NavController,
    viewModel: VoiceViewModel = viewModel()
) {
    val voiceState by viewModel.voiceState.collectAsState()
    val transcribedText by viewModel.transcribedText.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val audioLevel by viewModel.audioLevel.collectAsState()
    val needsPermission by viewModel.needsPermission.collectAsState()
    val featureSummary by viewModel.featureSummary.collectAsState()
    val voiceAiRouteState by viewModel.voiceAiRouteState.collectAsState()
    val aiIntentState by viewModel.aiIntentState.collectAsState()
    val classifiedIntent by viewModel.classifiedIntent.collectAsState()
    
    val isVoiceEnabled = FeatureGate.areAdvancedFeaturesEnabled()
    
    // Handle AI route success - navigate to route details
    LaunchedEffect(voiceAiRouteState) {
        if (voiceAiRouteState is VoiceAiRouteState.Success) {
            val suggestion = (voiceAiRouteState as VoiceAiRouteState.Success).suggestion
            // TODO: Pass suggestion to RouteDetailsScreen
            navController.navigate("routeDetails/ai_voice_route")
            viewModel.clearAiRouteState()
        }
    }
    
    // Pulsing animation for listening state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    Scaffold(
        topBar = {
            Column {
                SafeModeBanner()
                TopAppBar(
                    title = { Text("Voice Command") },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Status text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = when {
                        !isVoiceEnabled -> "Voice Unavailable"
                        needsPermission -> "Permission Required"
                        isListening -> "Listening..."
                        voiceState is VoiceViewModel.VoiceState.Processing -> "Processing..."
                        voiceState is VoiceViewModel.VoiceState.Error -> "Error"
                        voiceState is VoiceViewModel.VoiceState.Result -> "Complete"
                        else -> "Tap to Speak"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = when {
                        !isVoiceEnabled -> MaterialTheme.colorScheme.error
                        isListening -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Tier hint
                if (!FeatureGate.areAdvancedVoiceCommandsEnabled()) {
                    Text(
                        text = "Basic voice (Upgrade for AI processing)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Mic button with pulse animation
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(160.dp)
                    .scale(if (isListening) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(
                        when {
                            !isVoiceEnabled -> MaterialTheme.colorScheme.surfaceVariant
                            isListening -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.primaryContainer
                        }
                    )
                    .then(
                        if (isListening) {
                            Modifier.border(
                                width = 4.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                        } else Modifier
                    )
                    .clickable(enabled = isVoiceEnabled) {
                        viewModel.onVoiceButtonPressed()
                    }
            ) {
                Icon(
                    imageVector = when {
                        !isVoiceEnabled -> Icons.Default.MicOff
                        isListening -> Icons.Default.Stop
                        else -> Icons.Default.Mic
                    },
                    contentDescription = if (isListening) "Stop" else "Start listening",
                    modifier = Modifier.size(64.dp),
                    tint = when {
                        !isVoiceEnabled -> MaterialTheme.colorScheme.onSurfaceVariant
                        isListening -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                    }
                )
            }
            
            // Transcription area
            Column(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Live transcription box
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 200.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = if (transcribedText.isEmpty()) Alignment.Center else Alignment.TopStart
                    ) {
                        if (transcribedText.isEmpty()) {
                            Text(
                                text = when {
                                    isListening -> "Speak now..."
                                    voiceState is VoiceViewModel.VoiceState.Error -> 
                                        (voiceState as VoiceViewModel.VoiceState.Error).message
                                    else -> "Your words will appear here"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = transcribedText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Result actions
                AnimatedVisibility(
                    visible = voiceState is VoiceViewModel.VoiceState.Result,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    val result = voiceState as? VoiceViewModel.VoiceState.Result
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (result?.intent != null) {
                            Text(
                                text = "Destination: ${result.intent.destination}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    // TODO: Navigate to route with intent
                                    navController.navigate("route")
                                },
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                Text("Start Navigation")
                            }
                        } else if (transcribedText.isNotBlank()) {
                            Text(
                                text = "Could not understand destination",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { viewModel.startListening() }) {
                                Text("Try Again")
                            }
                        }
                    }
                }
                
                // AI Route Status (MP-016)
                AnimatedVisibility(
                    visible = voiceAiRouteState is VoiceAiRouteState.AiRouting || 
                              voiceAiRouteState is VoiceAiRouteState.Success ||
                              voiceAiRouteState is VoiceAiRouteState.Error,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when (voiceAiRouteState) {
                                is VoiceAiRouteState.Success -> MaterialTheme.colorScheme.primaryContainer
                                is VoiceAiRouteState.Error -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            when (voiceAiRouteState) {
                                is VoiceAiRouteState.AiRouting -> {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("AI planning route...", style = MaterialTheme.typography.bodySmall)
                                }
                                is VoiceAiRouteState.Success -> {
                                    val suggestion = (voiceAiRouteState as VoiceAiRouteState.Success).suggestion
                                    Text(
                                        text = "AI route to: ${suggestion.destinationName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                is VoiceAiRouteState.Error -> {
                                    Text(
                                        text = (voiceAiRouteState as VoiceAiRouteState.Error).message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                else -> {}
                            }
                        }
                    }
                }
                
                // MP-020: AI Intent Status Panel
                VoiceAiIntentStatusPanel(aiIntentState, classifiedIntent)
                
                // Permission prompt
                AnimatedVisibility(visible = needsPermission) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Microphone access is required for voice commands",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // TODO: Wire to permission launcher
                            Button(onClick = { /* TODO: Request permission */ }) {
                                Text("Grant Permission")
                            }
                        }
                    }
                }
            }
            
            // Cancel button when listening
            AnimatedVisibility(visible = isListening) {
                TextButton(
                    onClick = { viewModel.cancel() },
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}
