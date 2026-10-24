package com.example.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AudioPlaybackState
import com.example.data.model.ChatMessage
import com.example.ui.components.HudArcReactorCore
import com.example.ui.components.HudPanelFrame
import com.example.ui.components.HudTopScannerReticle
import com.example.ui.theme.HudAmber
import com.example.ui.theme.HudBorderSubtle
import com.example.ui.theme.HudCyan
import com.example.ui.theme.HudCyanContainer
import com.example.ui.theme.HudCyanDim
import com.example.ui.theme.HudDarkBg
import com.example.ui.theme.HudGreen
import com.example.ui.theme.HudPanelBg
import com.example.ui.theme.HudPanelBorder
import com.example.ui.theme.HudRed
import com.example.ui.theme.HudTextPrimary
import com.example.ui.theme.HudTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadActiveVoiceInfo()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CutCornerShape(6.dp))
                                .background(HudPanelBorder.copy(alpha = 0.2f))
                                .border(1.dp, HudCyan, CutCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "MAIKI HUD Core",
                                tint = HudCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "MAIKI AI // HUD v4.0",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = HudCyan
                            )
                            Text(
                                text = if (uiState.activeVoiceId.isNotBlank())
                                    "VOICE CORE: ${uiState.activeVoiceName.uppercase()}"
                                else "VOICE CORE: UNCONFIGURED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                ),
                                color = if (uiState.activeVoiceId.isNotBlank()) HudGreen else HudAmber
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Voice Settings",
                            tint = HudCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HudDarkBg,
                    titleContentColor = HudCyan
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(HudDarkBg)
        ) {
            // Futuristic Iron Man HUD Background Layer
            Image(
                painter = painterResource(id = R.drawable.hud_bg),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.28f)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                // Top Diagnostics Reticle
                HudTopScannerReticle(
                    statusText = "SYSTEM TEMPERATURE: 34.2°C // OPTIMAL",
                    metricText = if (uiState.activeVoiceId.isNotBlank())
                        "NEURAL LINK: ELEVENLABS ONLINE [${uiState.activeVoiceId.take(8)}...]"
                    else "NEURAL LINK: STANDBY // SET VOICE ID IN SETTINGS",
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )

                // Interactive Arc Reactor Core Widget
                HudArcReactorCore(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    isActive = playbackState is AudioPlaybackState.Playing || uiState.isSynthesizingVoice,
                    coreText = "MAIKI AI",
                    energyPercent = if (playbackState is AudioPlaybackState.Playing) 100 else 88,
                    label = if (playbackState is AudioPlaybackState.Playing) "VOICE SYNTHESIS ACTIVE" else "ENERGY LEVEL"
                )

                // Communication Dock Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "// COMMUNICATION LOG",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = HudCyan
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Background Mic Toggle Button
                        Box(
                            modifier = Modifier
                                .clip(CutCornerShape(4.dp))
                                .background(
                                    if (uiState.isBackgroundListeningActive) HudGreen.copy(alpha = 0.2f)
                                    else HudAmber.copy(alpha = 0.15f)
                                )
                                .border(
                                    1.dp,
                                    if (uiState.isBackgroundListeningActive) HudGreen else HudAmber,
                                    CutCornerShape(4.dp)
                                )
                                .clickable { viewModel.toggleBackgroundVoice(context) }
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                .testTag("background_voice_quick_toggle")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (uiState.isBackgroundListeningActive) Icons.Default.Mic else Icons.Default.MicOff,
                                    contentDescription = "Mic",
                                    tint = if (uiState.isBackgroundListeningActive) HudGreen else HudAmber,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (uiState.isBackgroundListeningActive) "BG MIC: ON" else "BG MIC: OFF",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (uiState.isBackgroundListeningActive) HudGreen else HudAmber
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(CutCornerShape(4.dp))
                                .background(HudPanelBorder.copy(alpha = 0.15f))
                                .border(1.dp, HudCyanDim, CutCornerShape(4.dp))
                                .clickable { onNavigateToSettings() }
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "CONFIG",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp
                                ),
                                color = HudCyan
                            )
                        }
                    }
                }

                // Live Background Listener Status Banner
                if (uiState.isBackgroundListeningActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .background(HudGreen.copy(alpha = 0.15f), CutCornerShape(6.dp))
                            .border(1.dp, HudGreen, CutCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("bg_listener_live_status_banner")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Listening",
                                    tint = HudGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "BACKGROUND LISTENER: ${uiState.backgroundServiceStateText}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = HudGreen
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(CutCornerShape(4.dp))
                                    .background(HudRed.copy(alpha = 0.2f))
                                    .border(1.dp, HudRed, CutCornerShape(4.dp))
                                    .clickable { viewModel.toggleBackgroundVoice(context) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "STOP",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = HudRed
                                )
                            }
                        }
                    }
                }

                // Fallback Warning Banner
                AnimatedVisibility(
                    visible = uiState.fallbackWarning != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    uiState.fallbackWarning?.let { warning ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(HudAmber.copy(alpha = 0.15f), CutCornerShape(6.dp))
                                .border(1.dp, HudAmber, CutCornerShape(6.dp))
                                .padding(8.dp)
                                .testTag("fallback_warning_banner")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Alert",
                                    tint = HudAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = warning,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    ),
                                    color = HudAmber,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Active Speaking Visualizer Bar
                AnimatedVisibility(
                    visible = playbackState is AudioPlaybackState.Playing,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    HudSpeakingBar(onStop = { viewModel.stopAudio() })
                }

                // Messages Stream
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("chat_messages_list"),
                    contentPadding = PaddingValues(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.messages, key = { it.id }) { message ->
                        HudMessageBubble(
                            message = message,
                            isPlaying = playbackState is AudioPlaybackState.Playing && message.audioFilePath != null,
                            onPlayAudio = { viewModel.playMessageAudio(message) },
                            onRetry = { viewModel.retrySynthesis(message) }
                        )
                    }

                    if (uiState.isAiThinking || uiState.isSynthesizingVoice) {
                        item {
                            Box(
                                modifier = Modifier
                                    .background(HudPanelBg, CutCornerShape(6.dp))
                                    .border(1.dp, HudCyanDim, CutCornerShape(6.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = HudCyan
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (uiState.isSynthesizingVoice)
                                            "NEURAL AUDIO SYNTHESIS: ELEVENLABS..."
                                        else "CORE PROCESSING // GEMINI AI...",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp
                                        ),
                                        color = HudCyan
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Sci-Fi Input Console
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .background(HudPanelBg, CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp))
                        .border(1.dp, HudPanelBorder, CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp))
                        .padding(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = uiState.inputPrompt,
                            onValueChange = { viewModel.onInputChanged(it) },
                            placeholder = {
                                Text(
                                    "Enter command or question...",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = HudTextSecondary.copy(alpha = 0.5f)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_input_field"),
                            shape = CutCornerShape(8.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = HudCyan,
                                unfocusedBorderColor = HudBorderSubtle,
                                focusedTextColor = HudTextPrimary,
                                unfocusedTextColor = HudTextPrimary,
                                cursorColor = HudCyan
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                keyboardController?.hide()
                                viewModel.sendMessage()
                            })
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                keyboardController?.hide()
                                viewModel.sendMessage()
                            },
                            enabled = uiState.inputPrompt.isNotBlank() && !uiState.isAiThinking,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("send_button"),
                            shape = CutCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HudCyan,
                                contentColor = Color.Black,
                                disabledContainerColor = HudPanelBorder.copy(alpha = 0.2f),
                                disabledContentColor = HudCyanDim
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Transmit",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HudMessageBubble(
    message: ChatMessage,
    isPlaying: Boolean,
    onPlayAudio: () -> Unit,
    onRetry: () -> Unit
) {
    val isUser = message.isUser
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val borderColor = if (isUser) HudCyan.copy(alpha = 0.7f) else HudPanelBorder
    val shape = CutCornerShape(
        topStart = if (isUser) 10.dp else 0.dp,
        topEnd = if (isUser) 0.dp else 10.dp,
        bottomStart = 10.dp,
        bottomEnd = 10.dp
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(if (isUser) "user_message_item" else "maiki_message_item"),
        horizontalAlignment = alignment
    ) {
        Text(
            text = if (isUser) "[ TRANSMISSION // OPERATOR ]" else "[ PROTOCOL // MAIKI AI ]",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            ),
            color = if (isUser) HudAmber else HudCyan,
            modifier = Modifier.padding(bottom = 2.dp)
        )

        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .background(HudPanelBg, shape)
                .border(1.dp, borderColor, shape)
                .padding(10.dp)
        ) {
            Column {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    color = HudTextPrimary
                )

                if (!isUser) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = onPlayAudio,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPlaying) HudCyan else HudCyanContainer,
                                contentColor = if (isPlaying) Color.Black else HudCyan
                            ),
                            shape = CutCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("play_message_audio_button")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                                contentDescription = "Play Speech",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isPlaying) "STREAMING..." else "SPEAK VOICE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        if (message.voiceIdUsed != null && !message.isFallbackTts) {
                            Text(
                                text = "ID: ${message.voiceIdUsed.take(6)}...",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.sp
                                ),
                                color = HudGreen
                            )
                        } else if (message.isFallbackTts) {
                            Text(
                                text = "TTS FALLBACK",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.sp
                                ),
                                color = HudAmber
                            )
                        }

                        if (message.errorMessage != null) {
                            IconButton(
                                onClick = onRetry,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Retry",
                                    tint = HudAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HudSpeakingBar(onStop: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(HudPanelBg, CutCornerShape(6.dp))
            .border(1.dp, HudCyan, CutCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Active",
                    tint = HudCyan,
                    modifier = Modifier
                        .size(20.dp)
                        .scale(pulse)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "NEURAL VOICE STREAMING // ELEVENLABS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = HudCyan
                )
            }

            IconButton(
                onClick = onStop,
                modifier = Modifier
                    .size(26.dp)
                    .testTag("stop_speaking_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop",
                    tint = HudRed,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
