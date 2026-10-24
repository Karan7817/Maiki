package com.example.ui.chat

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiApiClient
import com.example.data.model.AudioPlaybackState
import com.example.data.model.ChatMessage
import com.example.data.model.VoiceConfig
import com.example.data.model.VoiceSettingsRequest
import com.example.data.repository.VoiceRepository
import com.example.service.MaikiVoiceForegroundService
import com.example.service.VoiceAudioService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputPrompt: String = "",
    val isAiThinking: Boolean = false,
    val isSynthesizingVoice: Boolean = false,
    val activeVoiceName: String = "No Voice Configured",
    val activeVoiceId: String = "",
    val fallbackWarning: String? = null,
    val isVoiceEnabled: Boolean = true,
    val isBackgroundListeningActive: Boolean = false,
    val backgroundServiceStateText: String = "IDLE",
    val activeGeminiModel: String = "gemini-1.5-flash",
    val hasGeminiKey: Boolean = false
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VoiceRepository(application)
    private val audioService = VoiceAudioService(application)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val playbackState: StateFlow<AudioPlaybackState> = audioService.playbackState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AudioPlaybackState.Idle)

    init {
        loadActiveVoiceInfo()
        observeBackgroundService()

        // Welcome transmission
        val welcomeMsg = ChatMessage(
            text = "Greetings, Operator. MAIKI AI neural link is active. Speak commands or enter queries. Powered by Google Gemini & ElevenLabs voice core.",
            isUser = false
        )
        _uiState.value = _uiState.value.copy(messages = listOf(welcomeMsg))
    }

    private fun observeBackgroundService() {
        viewModelScope.launch {
            MaikiVoiceForegroundService.isRunning.collect { running ->
                _uiState.value = _uiState.value.copy(isBackgroundListeningActive = running)
            }
        }
        viewModelScope.launch {
            MaikiVoiceForegroundService.assistantState.collect { state ->
                _uiState.value = _uiState.value.copy(backgroundServiceStateText = state)
            }
        }
        viewModelScope.launch {
            MaikiVoiceForegroundService.latestTransmission.collect { transmission ->
                transmission?.let { (userText, aiText) ->
                    val userMsg = ChatMessage(text = userText, isUser = true)
                    val maikiMsg = ChatMessage(text = aiText, isUser = false)
                    val current = _uiState.value.messages
                    if (current.none { it.text == aiText && !it.isUser }) {
                        _uiState.value = _uiState.value.copy(messages = current + userMsg + maikiMsg)
                    }
                }
            }
        }
    }

    fun loadActiveVoiceInfo() {
        val config = repository.loadVoiceConfig()
        val voiceId = config.effectiveVoiceId
        val voiceName = config.effectiveVoiceName
        _uiState.value = _uiState.value.copy(
            activeVoiceId = voiceId,
            activeVoiceName = if (voiceId.isNotBlank()) voiceName else "No Voice Configured",
            isVoiceEnabled = config.isVoiceResponseEnabled,
            activeGeminiModel = config.geminiModel.ifBlank { "gemini-1.5-flash" },
            hasGeminiKey = config.geminiApiKey.isNotBlank()
        )
    }

    fun toggleBackgroundVoice(context: Context) {
        if (_uiState.value.isBackgroundListeningActive) {
            MaikiVoiceForegroundService.stop(context)
        } else {
            MaikiVoiceForegroundService.start(context)
        }
    }

    fun onInputChanged(text: String) {
        _uiState.value = _uiState.value.copy(inputPrompt = text)
    }

    fun sendMessage() {
        val prompt = _uiState.value.inputPrompt.trim()
        if (prompt.isEmpty() || _uiState.value.isAiThinking) return

        val userMessage = ChatMessage(text = prompt, isUser = true)
        val currentList = _uiState.value.messages + userMessage

        _uiState.value = _uiState.value.copy(
            messages = currentList,
            inputPrompt = "",
            isAiThinking = true,
            fallbackWarning = null
        )

        viewModelScope.launch {
            val config = repository.loadVoiceConfig()

            // 1. Generate AI Response using configured Gemini API Key and Model
            val aiResponseText = GeminiApiClient.generateAiResponse(
                userPrompt = prompt,
                customGeminiKey = config.geminiApiKey,
                model = config.geminiModel
            )

            _uiState.value = _uiState.value.copy(
                isAiThinking = false,
                isSynthesizingVoice = true
            )

            val voiceId = config.effectiveVoiceId
            val apiKey = config.apiKey.trim()

            // 2. Synthesize with ElevenLabs if voice response is enabled
            if (config.isVoiceResponseEnabled && voiceId.isNotBlank() && apiKey.isNotBlank()) {
                val settings = VoiceSettingsRequest(
                    stability = config.stability,
                    similarityBoost = config.similarityBoost
                )

                val synthResult = repository.synthesizeSpeech(
                    text = aiResponseText,
                    voiceId = voiceId,
                    apiKey = apiKey,
                    settings = settings
                )

                synthResult.onSuccess { audioFile ->
                    val maikiMessage = ChatMessage(
                        text = aiResponseText,
                        isUser = false,
                        audioFilePath = audioFile.absolutePath,
                        voiceIdUsed = voiceId,
                        isFallbackTts = false
                    )
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages + maikiMessage,
                        isSynthesizingVoice = false,
                        fallbackWarning = null
                    )
                    // Play audio
                    audioService.playAudioFile(audioFile)
                }.onFailure { error ->
                    // ElevenLabs failed: preserve voiceId, display error, fallback to Android TTS
                    val fallbackError = error.message ?: "ElevenLabs synthesis failed"
                    val maikiMessage = ChatMessage(
                        text = aiResponseText,
                        isUser = false,
                        voiceIdUsed = voiceId,
                        isFallbackTts = true,
                        errorMessage = fallbackError
                    )
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages + maikiMessage,
                        isSynthesizingVoice = false,
                        fallbackWarning = "ElevenLabs unavailable: $fallbackError (Using Android TTS fallback)"
                    )
                    // Android TTS fallback
                    audioService.speakUsingAndroidTts(aiResponseText, config.speakingSpeed)
                }
            } else {
                // ElevenLabs voice not configured or voice response disabled
                val reason = if (!config.isVoiceResponseEnabled) {
                    null
                } else if (apiKey.isBlank()) {
                    "ElevenLabs API Key not configured in Voice Settings."
                } else {
                    "No Voice ID configured. Please set a custom Voice ID in Settings."
                }

                val maikiMessage = ChatMessage(
                    text = aiResponseText,
                    isUser = false,
                    isFallbackTts = config.isVoiceResponseEnabled,
                    errorMessage = reason
                )

                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + maikiMessage,
                    isSynthesizingVoice = false,
                    fallbackWarning = reason
                )

                if (config.isVoiceResponseEnabled) {
                    audioService.speakUsingAndroidTts(aiResponseText, config.speakingSpeed)
                }
            }
        }
    }

    fun playMessageAudio(message: ChatMessage) {
        if (!message.audioFilePath.isNullOrBlank()) {
            val file = File(message.audioFilePath)
            if (file.exists()) {
                audioService.playAudioFile(file)
                return
            }
        }

        val config = repository.loadVoiceConfig()
        val voiceId = message.voiceIdUsed ?: config.effectiveVoiceId
        val apiKey = config.apiKey.trim()

        if (voiceId.isNotBlank() && apiKey.isNotBlank()) {
            viewModelScope.launch {
                val result = repository.synthesizeSpeech(message.text, voiceId, apiKey)
                result.onSuccess { file ->
                    audioService.playAudioFile(file)
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        fallbackWarning = "Playback retry failed: ${error.message}. Using Android TTS."
                    )
                    audioService.speakUsingAndroidTts(message.text, config.speakingSpeed)
                }
            }
        } else {
            audioService.speakUsingAndroidTts(message.text, config.speakingSpeed)
        }
    }

    fun stopAudio() {
        audioService.stopAudio()
    }

    fun retrySynthesis(message: ChatMessage) {
        playMessageAudio(message)
    }

    override fun onCleared() {
        super.onCleared()
        audioService.release()
    }
}
