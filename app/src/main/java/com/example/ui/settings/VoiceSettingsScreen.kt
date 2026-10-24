package com.example.ui.settings

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import com.example.data.api.GeminiApiClient
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AudioPlaybackState
import com.example.data.model.ConnectionStatus
import com.example.data.model.VoiceSelectionMode
import com.example.ui.components.HudPanelFrame
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VoiceSettingsScreen(
    viewModel: VoiceSettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var isApiKeyVisible by remember { mutableStateOf(false) }
    var isVoiceDropdownExpanded by remember { mutableStateOf(false) }
    var isGeminiKeyVisible by remember { mutableStateOf(false) }
    var isModelDropdownExpanded by remember { mutableStateOf(false) }
    var geminiModelSearchQuery by remember { mutableStateOf("") }
    var selectedModelCategoryFilter by remember { mutableStateOf("ALL") }
    var showCustomModelDialog by remember { mutableStateOf(false) }
    var customModelInput by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "MAIKI AI — Voice Settings",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = HudCyan
                        )
                        Text(
                            text = "ElevenLabs Custom Voice ID Integration",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            color = HudTextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Chat",
                            tint = HudCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HudDarkBg,
                    titleContentColor = HudCyan,
                    navigationIconContentColor = HudCyan
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
            // Background HUD hologram asset
            Image(
                painter = painterResource(id = R.drawable.hud_bg),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.22f)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("voice_settings_scroll_list"),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Connection & Voice Status HUD Panel
                item {
                    HudPanelFrame(headerTitle = "TELEMETRY & VOICE CORE STATUS") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "API Connection Status:",
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                                    color = HudTextSecondary
                                )

                                val (badgeColor, badgeText) = when (uiState.connectionStatus) {
                                    ConnectionStatus.CONNECTED -> HudGreen to "CONNECTED"
                                    ConnectionStatus.VALIDATING -> HudAmber to "VALIDATING..."
                                    ConnectionStatus.ERROR -> HudRed to "ERROR"
                                    ConnectionStatus.DISCONNECTED -> HudTextSecondary to "DISCONNECTED"
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(badgeColor.copy(alpha = 0.15f), CutCornerShape(4.dp))
                                        .border(1.dp, badgeColor, CutCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                        .testTag("connection_status_badge")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(badgeColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = badgeText,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = badgeColor
                                    )
                                }
                            }

                            HorizontalDivider(color = HudBorderSubtle)

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Voice Status:",
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                                    color = HudTextSecondary
                                )
                                Text(
                                    text = uiState.voiceConfigurationStatus,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = HudCyan,
                                    modifier = Modifier.testTag("voice_configuration_status_text")
                                )
                            }
                        }
                    }
                }

                // 2. Feedback Banners (Error or Success)
                if (uiState.validationError != null) {
                    item {
                        HudBanner(
                            text = uiState.validationError!!,
                            isError = true,
                            modifier = Modifier.testTag("validation_error_banner")
                        )
                    }
                }

                if (uiState.successMessage != null) {
                    item {
                        HudBanner(
                            text = uiState.successMessage!!,
                            isError = false,
                            modifier = Modifier.testTag("success_banner")
                        )
                    }
                }

                // 3. Audio Player Bar (When Testing or Previewing)
                item {
                    AnimatedVisibility(
                        visible = playbackState is AudioPlaybackState.Playing || playbackState is AudioPlaybackState.Loading || uiState.isTesting,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        HudPanelFrame(headerTitle = "VOICE PREVIEW SYNTHESIS") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("audio_playback_bar"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Playing Audio",
                                        tint = HudCyan,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (uiState.isTesting) "Generating ElevenLabs speech..." else "Playing Voice Preview",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = HudCyan
                                        )
                                        if (playbackState is AudioPlaybackState.Playing) {
                                            LinearProgressIndicator(
                                                progress = { (playbackState as AudioPlaybackState.Playing).progress },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp),
                                                color = HudCyan,
                                                trackColor = HudPanelBorder.copy(alpha = 0.2f)
                                            )
                                        } else {
                                            LinearProgressIndicator(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp),
                                                color = HudCyan
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = { viewModel.stopAudio() },
                                    colors = ButtonDefaults.buttonColors(containerColor = HudRed),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = CutCornerShape(4.dp),
                                    modifier = Modifier.testTag("stop_audio_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = "Stop Playback",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "STOP",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3.5. Background Voice Assistant [Foreground Service]
                item {
                    HudPanelFrame(headerTitle = "HANDS-FREE BACKGROUND VOICE SERVICE") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (uiState.isBackgroundListeningActive) Icons.Default.Mic else Icons.Default.MicOff,
                                        contentDescription = "Mic Status",
                                        tint = if (uiState.isBackgroundListeningActive) HudGreen else HudAmber,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "BACKGROUND SERVICE:",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                        color = HudTextSecondary
                                    )
                                }

                                val statusColor = if (uiState.isBackgroundListeningActive) HudGreen else HudAmber
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(statusColor.copy(alpha = 0.15f), CutCornerShape(4.dp))
                                        .border(1.dp, statusColor, CutCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(statusColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (uiState.isBackgroundListeningActive) "ACTIVE (LISTENING)" else "STANDBY (OFF)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = statusColor
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "ASSISTANT STATUS:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = HudTextSecondary
                                )
                                Text(
                                    text = uiState.backgroundServiceStateText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = HudCyan
                                )
                            }

                            Text(
                                text = "Runs in the background via Android Foreground Service. MAIKI listens for your speech, queries Gemini AI, and speaks with your configured ElevenLabs voice even when minimized or phone is locked.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                ),
                                color = HudTextSecondary
                            )

                            Button(
                                onClick = { viewModel.toggleBackgroundListening(context) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("toggle_background_service_button"),
                                shape = CutCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (uiState.isBackgroundListeningActive) HudRed else HudCyan,
                                    contentColor = if (uiState.isBackgroundListeningActive) Color.White else Color.Black
                                )
                            ) {
                                Icon(
                                    imageVector = if (uiState.isBackgroundListeningActive) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = "Toggle Background",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (uiState.isBackgroundListeningActive) "STOP BACKGROUND LISTENING" else "START BACKGROUND LISTENING",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }

                // 3.6. Google Gemini AI Neural Configuration Panel
                item {
                    HudPanelFrame(headerTitle = "GOOGLE GEMINI AI NEURAL CONFIGURATION") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Gemini",
                                        tint = HudCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Google Gemini API Key",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = HudTextPrimary
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = "Encrypted",
                                        tint = HudGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "KEYSTORE AES-256",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp
                                        ),
                                        color = HudGreen
                                    )
                                }
                            }

                            // Gemini API Key Input Field
                            OutlinedTextField(
                                value = uiState.geminiApiKeyInput,
                                onValueChange = { viewModel.onGeminiApiKeyChanged(it) },
                                placeholder = {
                                    Text(
                                        "Enter your Google Gemini API Key",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("gemini_api_key_input"),
                                visualTransformation = if (isGeminiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    Row {
                                        IconButton(onClick = { isGeminiKeyVisible = !isGeminiKeyVisible }) {
                                            Icon(
                                                imageVector = if (isGeminiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = if (isGeminiKeyVisible) "Hide Key" else "Show Key",
                                                tint = HudCyan
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                                                if (!clip.isNullOrBlank()) {
                                                    viewModel.onGeminiApiKeyChanged(clip.trim())
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentPaste,
                                                contentDescription = "Paste Gemini Key",
                                                tint = HudCyan
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = CutCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = HudCyan,
                                    unfocusedBorderColor = HudBorderSubtle,
                                    focusedTextColor = HudTextPrimary,
                                    unfocusedTextColor = HudTextPrimary,
                                    cursorColor = HudCyan
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                            )

                            // Model Selector Dropdown with All Gemini Models Catalogue & Auto-Detection
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "GEMINI MODEL SELECTION",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                            color = HudTextSecondary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "(${uiState.availableGeminiModels.size} MODELS)",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = HudGreen
                                            )
                                        )
                                    }
                                    TextButton(
                                        onClick = { viewModel.detectGeminiModels() },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                        modifier = Modifier.testTag("auto_detect_models_button")
                                    ) {
                                        if (uiState.isLoadingGeminiModels) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(10.dp),
                                                color = HudCyan,
                                                strokeWidth = 1.5.dp
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Refresh Models",
                                                tint = HudCyan,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = "SYNC WITH GOOGLE",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = HudCyan
                                        )
                                    }
                                }

                                // Quick 1-tap select chips for most popular models
                                val quickModels = listOf(
                                    "gemini-2.0-flash",
                                    "gemini-2.0-flash-thinking-exp-01-21",
                                    "gemini-1.5-flash",
                                    "gemini-1.5-pro",
                                    "gemini-2.5-flash"
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    quickModels.forEach { quickId ->
                                        val isSelected = quickId == uiState.geminiModel
                                        val desc = GeminiApiClient.getModelDescriptor(quickId)
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (isSelected) HudCyan.copy(alpha = 0.25f) else HudPanelBorder.copy(alpha = 0.2f),
                                                    CutCornerShape(4.dp)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isSelected) HudCyan else HudBorderSubtle,
                                                    CutCornerShape(4.dp)
                                                )
                                                .clickable {
                                                    viewModel.onGeminiModelChanged(quickId)
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (isSelected) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .background(HudCyan, CircleShape)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                }
                                                Text(
                                                    text = desc.tag,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        fontSize = 9.sp
                                                    ),
                                                    color = if (isSelected) HudCyan else HudTextSecondary
                                                )
                                            }
                                        }
                                    }
                                }

                                // Main dropdown with full list, search bar, and category filters
                                ExposedDropdownMenuBox(
                                    expanded = isModelDropdownExpanded,
                                    onExpandedChange = { isModelDropdownExpanded = it }
                                ) {
                                    val currentDesc = GeminiApiClient.getModelDescriptor(uiState.geminiModel)
                                    OutlinedTextField(
                                        value = uiState.geminiModel,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = {
                                            Text(
                                                "Active Model [${currentDesc.tag}]",
                                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                                color = HudCyan
                                            )
                                        },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isModelDropdownExpanded) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                            .testTag("gemini_model_dropdown"),
                                        shape = CutCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = HudCyan,
                                            unfocusedBorderColor = HudBorderSubtle,
                                            focusedTextColor = HudTextPrimary,
                                            unfocusedTextColor = HudTextPrimary
                                        )
                                    )

                                    ExposedDropdownMenu(
                                        expanded = isModelDropdownExpanded,
                                        onDismissRequest = {
                                            isModelDropdownExpanded = false
                                            geminiModelSearchQuery = ""
                                        },
                                        modifier = Modifier
                                            .background(HudDarkBg)
                                            .heightIn(max = 440.dp)
                                    ) {
                                        // Search Bar inside Dropdown
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = geminiModelSearchQuery,
                                                onValueChange = { geminiModelSearchQuery = it },
                                                placeholder = {
                                                    Text(
                                                        "Search models (e.g. 2.0, flash, pro, think)...",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                                        color = HudTextSecondary
                                                    )
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.Search,
                                                        contentDescription = "Search",
                                                        tint = HudCyan,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                },
                                                trailingIcon = {
                                                    if (geminiModelSearchQuery.isNotEmpty()) {
                                                        IconButton(onClick = { geminiModelSearchQuery = "" }) {
                                                            Icon(
                                                                imageVector = Icons.Default.Close,
                                                                contentDescription = "Clear",
                                                                tint = HudCyan,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                    }
                                                },
                                                singleLine = true,
                                                shape = CutCornerShape(4.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = HudCyan,
                                                    unfocusedBorderColor = HudBorderSubtle,
                                                    focusedTextColor = HudTextPrimary,
                                                    unfocusedTextColor = HudTextPrimary
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Spacer(modifier = Modifier.height(6.dp))

                                            // Category filter chips inside dropdown
                                            val categories = listOf("ALL", "2.0", "2.5", "1.5", "PRO", "THINK", "8B", "EXP")
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                categories.forEach { cat ->
                                                    val isCatSelected = selectedModelCategoryFilter == cat
                                                    Box(
                                                        modifier = Modifier
                                                            .background(
                                                                if (isCatSelected) HudCyan else HudPanelBorder.copy(alpha = 0.3f),
                                                                CutCornerShape(2.dp)
                                                            )
                                                            .clickable { selectedModelCategoryFilter = cat }
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = cat,
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontFamily = FontFamily.Monospace,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold
                                                            ),
                                                            color = if (isCatSelected) Color.Black else HudTextPrimary
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        HorizontalDivider(color = HudBorderSubtle)

                                        val filteredModels = uiState.availableGeminiModels.filter { modelName ->
                                            val queryMatches = geminiModelSearchQuery.isBlank() ||
                                                    modelName.contains(geminiModelSearchQuery.trim(), ignoreCase = true)
                                            val catMatches = when (selectedModelCategoryFilter) {
                                                "2.0" -> modelName.contains("2.0")
                                                "2.5" -> modelName.contains("2.5")
                                                "1.5" -> modelName.contains("1.5")
                                                "PRO" -> modelName.contains("pro")
                                                "THINK" -> modelName.contains("thinking")
                                                "8B" -> modelName.contains("8b")
                                                "EXP" -> modelName.contains("exp") || modelName.contains("learnlm")
                                                else -> true
                                            }
                                            queryMatches && catMatches
                                        }

                                        if (filteredModels.isEmpty()) {
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        "No matching models found in catalogue. Tap below to use custom ID.",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                                        color = HudAmber
                                                    )
                                                },
                                                onClick = {
                                                    if (geminiModelSearchQuery.isNotBlank()) {
                                                        viewModel.onGeminiModelChanged(geminiModelSearchQuery.trim())
                                                    }
                                                    isModelDropdownExpanded = false
                                                }
                                            )
                                        } else {
                                            filteredModels.forEach { modelName ->
                                                val isSelected = modelName == uiState.geminiModel
                                                val desc = GeminiApiClient.getModelDescriptor(modelName)
                                                DropdownMenuItem(
                                                    text = {
                                                        Column(modifier = Modifier.padding(vertical = 3.dp)) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                modifier = Modifier.fillMaxWidth()
                                                            ) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    modifier = Modifier.weight(1f, fill = false)
                                                                ) {
                                                                    if (isSelected) {
                                                                        Box(
                                                                            modifier = Modifier
                                                                                .size(6.dp)
                                                                                .background(HudCyan, CircleShape)
                                                                        )
                                                                        Spacer(modifier = Modifier.width(6.dp))
                                                                    }
                                                                    Text(
                                                                        text = modelName,
                                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                                        color = if (isSelected) HudCyan else HudTextPrimary,
                                                                        fontFamily = FontFamily.Monospace,
                                                                        style = MaterialTheme.typography.bodySmall
                                                                    )
                                                                }

                                                                Spacer(modifier = Modifier.width(8.dp))

                                                                Box(
                                                                    modifier = Modifier
                                                                        .background(
                                                                            if (desc.isRecommended) HudGreen.copy(alpha = 0.2f) else HudCyan.copy(alpha = 0.15f),
                                                                            CutCornerShape(3.dp)
                                                                        )
                                                                        .border(
                                                                            1.dp,
                                                                            if (desc.isRecommended) HudGreen else HudCyan.copy(alpha = 0.5f),
                                                                            CutCornerShape(3.dp)
                                                                        )
                                                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                                                ) {
                                                                    Text(
                                                                        text = desc.tag,
                                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                                            fontFamily = FontFamily.Monospace,
                                                                            fontSize = 8.sp,
                                                                            fontWeight = FontWeight.Bold
                                                                        ),
                                                                        color = if (desc.isRecommended) HudGreen else HudCyan
                                                                    )
                                                                }
                                                            }
                                                            Text(
                                                                text = desc.subtitle,
                                                                style = MaterialTheme.typography.labelSmall.copy(
                                                                    fontFamily = FontFamily.Monospace,
                                                                    fontSize = 10.sp
                                                                ),
                                                                color = HudTextSecondary
                                                            )
                                                        }
                                                    },
                                                    onClick = {
                                                        viewModel.onGeminiModelChanged(modelName)
                                                        isModelDropdownExpanded = false
                                                        geminiModelSearchQuery = ""
                                                    }
                                                )
                                            }
                                        }

                                        HorizontalDivider(color = HudBorderSubtle)

                                        // Custom Model ID Option
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Tune,
                                                        contentDescription = "Custom Model",
                                                        tint = HudAmber,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Enter Custom / Fine-Tuned Model ID...",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontFamily = FontFamily.Monospace,
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        color = HudAmber
                                                    )
                                                }
                                            },
                                            onClick = {
                                                isModelDropdownExpanded = false
                                                customModelInput = uiState.geminiModel
                                                showCustomModelDialog = true
                                            }
                                        )
                                    }
                                }

                                // Active Model Subtitle
                                val activeDesc = GeminiApiClient.getModelDescriptor(uiState.geminiModel)
                                Text(
                                    text = "Active: ${activeDesc.subtitle}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    ),
                                    color = HudCyan
                                )
                            }

                            // Test Gemini API Button
                            Button(
                                onClick = {
                                    keyboardController?.hide()
                                    viewModel.testGeminiApi()
                                },
                                enabled = !uiState.isTestingGemini,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("test_gemini_api_button"),
                                shape = CutCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HudPanelBorder.copy(alpha = 0.3f),
                                    contentColor = HudCyan
                                )
                            ) {
                                if (uiState.isTestingGemini) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = HudCyan,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("TESTING GEMINI LINK...", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace))
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = "Test Gemini",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("TEST GEMINI API CONNECTION", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold))
                                }
                            }

                            // Gemini Status Feedback
                            if (uiState.geminiStatusMessage != null) {
                                val isErr = uiState.geminiConnectionStatus == ConnectionStatus.ERROR
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isErr) HudRed.copy(alpha = 0.15f) else HudGreen.copy(alpha = 0.15f), CutCornerShape(4.dp))
                                        .border(1.dp, if (isErr) HudRed else HudGreen, CutCornerShape(4.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = uiState.geminiStatusMessage!!,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        ),
                                        color = if (isErr) HudRed else HudGreen
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. ElevenLabs Credentials & Voice ID Configuration
                item {
                    HudPanelFrame(headerTitle = "ELEVENLABS CREDENTIALS & VOICE ID") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = "API Key",
                                        tint = HudCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "ElevenLabs API Key",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = HudTextPrimary
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = "Keystore Encrypted",
                                        tint = HudGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "KEYSTORE AES-256",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp
                                        ),
                                        color = HudGreen
                                    )
                                }
                            }

                            // API Key Input
                            OutlinedTextField(
                                value = uiState.apiKeyInput,
                                onValueChange = { viewModel.onApiKeyChanged(it) },
                                placeholder = {
                                    Text(
                                        "Enter your ElevenLabs API Key",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("api_key_input"),
                                visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    Row {
                                        IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                            Icon(
                                                imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = if (isApiKeyVisible) "Hide API Key" else "Show API Key",
                                                tint = HudCyan
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                                                if (!clip.isNullOrBlank()) {
                                                    viewModel.onApiKeyChanged(clip.trim())
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentPaste,
                                                contentDescription = "Paste API Key",
                                                tint = HudCyan
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = CutCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = HudCyan,
                                    unfocusedBorderColor = HudBorderSubtle,
                                    focusedTextColor = HudTextPrimary,
                                    unfocusedTextColor = HudTextPrimary,
                                    cursorColor = HudCyan
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                            )

                            HorizontalDivider(color = HudBorderSubtle)

                            // Voice Selection Mode Tab: [ Automatic / Manual ]
                            Text(
                                text = "VOICE SELECTION MODE",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = HudTextSecondary
                            )

                            TabRow(
                                selectedTabIndex = if (uiState.selectionMode == VoiceSelectionMode.AUTOMATIC) 0 else 1,
                                containerColor = HudPanelBg,
                                contentColor = HudCyan,
                                indicator = { tabPositions ->
                                    SecondaryIndicator(
                                        Modifier.tabIndicatorOffset(
                                            tabPositions[if (uiState.selectionMode == VoiceSelectionMode.AUTOMATIC) 0 else 1]
                                        ),
                                        color = HudCyan
                                    )
                                },
                                modifier = Modifier
                                    .clip(CutCornerShape(8.dp))
                                    .border(1.dp, HudBorderSubtle, CutCornerShape(8.dp))
                                    .testTag("voice_selection_mode_tabs")
                            ) {
                                Tab(
                                    selected = uiState.selectionMode == VoiceSelectionMode.AUTOMATIC,
                                    onClick = { viewModel.setSelectionMode(VoiceSelectionMode.AUTOMATIC) },
                                    text = {
                                        Text(
                                            "Automatic Voice",
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                                        )
                                    },
                                    modifier = Modifier.testTag("mode_automatic_tab")
                                )
                                Tab(
                                    selected = uiState.selectionMode == VoiceSelectionMode.MANUAL,
                                    onClick = { viewModel.setSelectionMode(VoiceSelectionMode.MANUAL) },
                                    text = {
                                        Text(
                                            "Manual Voice ID",
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                                        )
                                    },
                                    modifier = Modifier.testTag("mode_manual_tab")
                                )
                            }

                            // MODE A: Automatic Voice Selection Dropdown
                            if (uiState.selectionMode == VoiceSelectionMode.AUTOMATIC) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "SELECT DISCOVERED VOICE",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                            color = HudTextSecondary
                                        )
                                        IconButton(
                                            onClick = { viewModel.fetchAvailableVoices() },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .testTag("refresh_voices_button")
                                        ) {
                                            if (uiState.isLoadingVoices) {
                                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = HudCyan)
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = "Refresh Voices",
                                                    tint = HudCyan,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    ExposedDropdownMenuBox(
                                        expanded = isVoiceDropdownExpanded,
                                        onExpandedChange = { isVoiceDropdownExpanded = it }
                                    ) {
                                        OutlinedTextField(
                                            value = if (uiState.selectedAutoVoiceName.isNotBlank()) {
                                                "${uiState.selectedAutoVoiceName} (${uiState.selectedAutoVoiceId.take(8)}...)"
                                            } else "Select a voice...",
                                            onValueChange = {},
                                            readOnly = true,
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isVoiceDropdownExpanded) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                                .testTag("automatic_voice_dropdown"),
                                            shape = CutCornerShape(8.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = HudCyan,
                                                unfocusedBorderColor = HudBorderSubtle,
                                                focusedTextColor = HudTextPrimary,
                                                unfocusedTextColor = HudTextPrimary
                                            )
                                        )

                                        ExposedDropdownMenu(
                                            expanded = isVoiceDropdownExpanded,
                                            onDismissRequest = { isVoiceDropdownExpanded = false },
                                            modifier = Modifier.background(HudDarkBg)
                                        ) {
                                            if (uiState.autoVoices.isEmpty()) {
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            "No voices loaded. Enter API Key & Refresh.",
                                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                                            color = HudTextSecondary
                                                        )
                                                    },
                                                    onClick = { isVoiceDropdownExpanded = false }
                                                )
                                            } else {
                                                uiState.autoVoices.forEach { voice ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Column {
                                                                Text(
                                                                    voice.name,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = HudCyan,
                                                                    fontFamily = FontFamily.Monospace
                                                                )
                                                                Text(
                                                                    "${voice.category ?: "custom"} • ${voice.voiceId}",
                                                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                                                    color = HudTextSecondary
                                                                )
                                                            }
                                                        },
                                                        onClick = {
                                                            viewModel.selectAutoVoice(voice)
                                                            isVoiceDropdownExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // MODE B: Manual Voice ID Input
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "ElevenLabs Voice ID",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = HudTextPrimary
                                    )
                                    if (uiState.selectionMode == VoiceSelectionMode.MANUAL) {
                                        Text(
                                            text = "MANUAL OVERRIDE ACTIVE",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.sp
                                            ),
                                            color = HudCyan
                                        )
                                    }
                                }

                                // Manual Voice ID Text Field
                                OutlinedTextField(
                                    value = uiState.voiceIdInput,
                                    onValueChange = { viewModel.onVoiceIdChanged(it) },
                                    placeholder = {
                                        Text(
                                            "Enter your ElevenLabs Voice ID",
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("voice_id_input"),
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                                                if (!clip.isNullOrBlank()) {
                                                    viewModel.onVoiceIdChanged(clip.trim())
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentPaste,
                                                contentDescription = "Paste Voice ID",
                                                tint = HudCyan
                                            )
                                        }
                                    },
                                    shape = CutCornerShape(8.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = HudCyan,
                                        unfocusedBorderColor = HudBorderSubtle,
                                        focusedTextColor = HudTextPrimary,
                                        unfocusedTextColor = HudTextPrimary,
                                        cursorColor = HudCyan
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                                )

                                Text(
                                    text = "Example format: 21m00Tcm4TlvDq8ikWAM (The application uses the exact Voice ID you enter)",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    ),
                                    color = HudTextSecondary
                                )
                            }

                            // Voice Name (Optional)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Voice Name",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = HudTextPrimary
                                )
                                OutlinedTextField(
                                    value = uiState.voiceNameInput,
                                    onValueChange = { viewModel.onVoiceNameChanged(it) },
                                    placeholder = {
                                        Text(
                                            "Optional Voice Name (e.g., Rachel, Adam, Custom)",
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("voice_name_input"),
                                    shape = CutCornerShape(8.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = HudCyan,
                                        unfocusedBorderColor = HudBorderSubtle,
                                        focusedTextColor = HudTextPrimary,
                                        unfocusedTextColor = HudTextPrimary,
                                        cursorColor = HudCyan
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
                                )
                            }
                        }
                    }
                }

                // 5. Action Buttons Module
                item {
                    HudPanelFrame(headerTitle = "EXECUTION & VOICE CONFIG CONTROLS") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Test Voice Button
                                Button(
                                    onClick = {
                                        keyboardController?.hide()
                                        viewModel.testVoice()
                                    },
                                    enabled = !uiState.isTesting,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("test_voice_button"),
                                    shape = CutCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = HudCyan,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    if (uiState.isTesting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.Black,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("VALIDATING...", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace))
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.GraphicEq,
                                            contentDescription = "Test Voice",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("TEST VOICE", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold))
                                    }
                                }

                                // Preview Voice Button
                                OutlinedButton(
                                    onClick = {
                                        keyboardController?.hide()
                                        viewModel.previewVoice()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("preview_voice_button"),
                                    shape = CutCornerShape(6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HudCyan)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Preview Voice",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("PREVIEW VOICE", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace))
                                }
                            }

                            // Save Voice Settings Button
                            Button(
                                onClick = {
                                    keyboardController?.hide()
                                    viewModel.saveVoiceSettings()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("save_voice_settings_button"),
                                shape = CutCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HudCyanContainer,
                                    contentColor = HudCyan
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = "Save Voice Settings",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SAVE VOICE SETTINGS", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold))
                            }

                            // Update Voice ID Button & Set As Default Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        keyboardController?.hide()
                                        viewModel.updateVoiceId()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("update_voice_id_button"),
                                    shape = CutCornerShape(6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HudCyan)
                                ) {
                                    Text("UPDATE VOICE ID", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace))
                                }

                                Button(
                                    onClick = {
                                        keyboardController?.hide()
                                        viewModel.setAsDefaultVoice()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("set_default_voice_button"),
                                    shape = CutCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = HudPanelBorder.copy(alpha = 0.2f),
                                        contentColor = HudAmber
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Set as Default",
                                        modifier = Modifier.size(14.dp),
                                        tint = HudAmber
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("SET AS DEFAULT", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace))
                                }
                            }

                            // Remove Custom Voice Button
                            OutlinedButton(
                                onClick = { viewModel.deleteCustomVoiceId() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("remove_custom_voice_button"),
                                shape = CutCornerShape(6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = HudRed)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove Custom Voice",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("REMOVE CUSTOM VOICE", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace))
                            }
                        }
                    }
                }

                // 6. Voice Tuning & Preferences
                item {
                    HudPanelFrame(headerTitle = "MAIKI SPEECH MATRIX & TUNING") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Voice Response Toggle
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Voice Response",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = HudTextPrimary
                                    )
                                    Text(
                                        text = if (uiState.isVoiceResponseEnabled) "Enabled (speaks answers automatically)" else "Disabled (text only)",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp
                                        ),
                                        color = HudTextSecondary
                                    )
                                }
                                Switch(
                                    checked = uiState.isVoiceResponseEnabled,
                                    onCheckedChange = { viewModel.setVoiceResponseEnabled(it) },
                                    modifier = Modifier.testTag("voice_response_toggle"),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = HudCyan,
                                        checkedTrackColor = HudCyanContainer
                                    )
                                )
                            }

                            HorizontalDivider(color = HudBorderSubtle)

                            // Speaking Speed Slider
                            Column {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Speaking Speed", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = HudTextSecondary)
                                    Text(
                                        String.format(java.util.Locale.US, "%.1fx", uiState.speakingSpeed),
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                                        color = HudCyan
                                    )
                                }
                                Slider(
                                    value = uiState.speakingSpeed,
                                    onValueChange = { viewModel.setSpeakingSpeed(it) },
                                    valueRange = 0.7f..1.4f,
                                    steps = 6,
                                    modifier = Modifier.testTag("speed_slider"),
                                    colors = SliderDefaults.colors(thumbColor = HudCyan, activeTrackColor = HudCyan)
                                )
                            }

                            // Stability Slider
                            Column {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Stability", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = HudTextSecondary)
                                    Text(
                                        String.format(java.util.Locale.US, "%.2f", uiState.stability),
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                                        color = HudCyan
                                    )
                                }
                                Slider(
                                    value = uiState.stability,
                                    onValueChange = { viewModel.setStability(it) },
                                    valueRange = 0.0f..1.0f,
                                    modifier = Modifier.testTag("stability_slider"),
                                    colors = SliderDefaults.colors(thumbColor = HudCyan, activeTrackColor = HudCyan)
                                )
                            }

                            // Similarity Boost Slider
                            Column {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Clarity & Similarity Boost", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = HudTextSecondary)
                                    Text(
                                        String.format(java.util.Locale.US, "%.2f", uiState.similarityBoost),
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                                        color = HudCyan
                                    )
                                }
                                Slider(
                                    value = uiState.similarityBoost,
                                    onValueChange = { viewModel.setSimilarityBoost(it) },
                                    valueRange = 0.0f..1.0f,
                                    modifier = Modifier.testTag("similarity_slider"),
                                    colors = SliderDefaults.colors(thumbColor = HudCyan, activeTrackColor = HudCyan)
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    if (showCustomModelDialog) {
        AlertDialog(
            onDismissRequest = { showCustomModelDialog = false },
            title = {
                Text(
                    "CUSTOM GEMINI MODEL ID",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = HudCyan
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Enter any Google Gemini model identifier (e.g. tunedModels/... or custom preview model):",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = HudTextPrimary
                    )
                    OutlinedTextField(
                        value = customModelInput,
                        onValueChange = { customModelInput = it },
                        placeholder = { Text("e.g. gemini-2.0-flash", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HudCyan,
                            unfocusedBorderColor = HudBorderSubtle,
                            focusedTextColor = HudTextPrimary,
                            unfocusedTextColor = HudTextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleaned = customModelInput.trim()
                        if (cleaned.isNotBlank()) {
                            viewModel.onGeminiModelChanged(cleaned)
                        }
                        showCustomModelDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HudCyan, contentColor = Color.Black)
                ) {
                    Text("APPLY MODEL", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCustomModelDialog = false }) {
                    Text("CANCEL", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = HudTextSecondary)
                }
            },
            containerColor = HudDarkBg,
            shape = CutCornerShape(8.dp)
        )
    }
}

@Composable
fun HudBanner(
    text: String,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isError) HudRed.copy(alpha = 0.15f) else HudGreen.copy(alpha = 0.15f)
    val contentColor = if (isError) HudRed else HudGreen
    val icon = if (isError) Icons.Default.Error else Icons.Default.CheckCircle

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor, CutCornerShape(8.dp))
            .border(1.dp, contentColor, CutCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = if (isError) "Error" else "Success",
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                ),
                color = contentColor
            )
        }
    }
}
