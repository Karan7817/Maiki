package com.example.ui.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiApiClient
import com.example.data.model.AudioPlaybackState
import com.example.data.model.ConnectionStatus
import com.example.data.model.ElevenLabsVoice
import com.example.data.model.VoiceConfig
import com.example.data.model.VoiceSelectionMode
import com.example.data.model.VoiceValidationResult
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

data class VoiceSettingsUiState(
    val apiKeyInput: String = "",
    val voiceIdInput: String = "",
    val voiceNameInput: String = "",
    val selectionMode: VoiceSelectionMode = VoiceSelectionMode.MANUAL,
    val autoVoices: List<ElevenLabsVoice> = emptyList(),
    val selectedAutoVoiceId: String = "",
    val selectedAutoVoiceName: String = "",
    val defaultVoiceId: String = "",
    val defaultVoiceName: String = "",
    val isVoiceResponseEnabled: Boolean = true,
    val speakingSpeed: Float = 1.0f,
    val stability: Float = 0.5f,
    val similarityBoost: Float = 0.75f,
    val language: String = "English",
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val voiceConfigurationStatus: String = "No Voice ID Configured",
    val validationError: String? = null,
    val successMessage: String? = null,
    val isTesting: Boolean = false,
    val isLoadingVoices: Boolean = false,
    val lastPreviewPath: String? = null,
    // Gemini AI Core State
    val geminiApiKeyInput: String = "",
    val geminiModel: String = "gemini-2.0-flash",
    val availableGeminiModels: List<String> = GeminiApiClient.ALL_GEMINI_MODELS,
    val isLoadingGeminiModels: Boolean = false,
    val geminiConnectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val geminiStatusMessage: String? = null,
    val isTestingGemini: Boolean = false,
    // Background Voice Assistant State
    val isBackgroundListeningActive: Boolean = false,
    val backgroundServiceStateText: String = "IDLE"
)

class VoiceSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VoiceRepository(application)
    private val audioService = VoiceAudioService(application)

    private val _uiState = MutableStateFlow(VoiceSettingsUiState())
    val uiState: StateFlow<VoiceSettingsUiState> = _uiState.asStateFlow()

    val playbackState: StateFlow<AudioPlaybackState> = audioService.playbackState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AudioPlaybackState.Idle)

    init {
        loadSavedConfig()
        observeBackgroundService()
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
    }

    fun loadSavedConfig() {
        val config = repository.loadVoiceConfig()
        val voiceStatus = when {
            config.customVoiceId.isNotBlank() ->
                "Custom Voice Configured: ${config.customVoiceId}"
            config.defaultVoiceId.isNotBlank() ->
                "Default Voice Configured: ${config.defaultVoiceName.ifBlank { config.defaultVoiceId }}"
            else -> "No Voice ID Configured"
        }

        _uiState.value = _uiState.value.copy(
            apiKeyInput = config.apiKey,
            voiceIdInput = config.customVoiceId,
            voiceNameInput = config.voiceName,
            selectionMode = config.selectionMode,
            selectedAutoVoiceId = config.selectedAutoVoiceId,
            selectedAutoVoiceName = config.selectedAutoVoiceName,
            defaultVoiceId = config.defaultVoiceId,
            defaultVoiceName = config.defaultVoiceName,
            isVoiceResponseEnabled = config.isVoiceResponseEnabled,
            speakingSpeed = config.speakingSpeed,
            stability = config.stability,
            similarityBoost = config.similarityBoost,
            language = config.language,
            voiceConfigurationStatus = voiceStatus,
            connectionStatus = if (config.apiKey.isNotBlank()) ConnectionStatus.CONNECTED else ConnectionStatus.DISCONNECTED,
            geminiApiKeyInput = config.geminiApiKey,
            geminiModel = config.geminiModel.ifBlank { "gemini-2.0-flash" },
            geminiConnectionStatus = if (config.geminiApiKey.isNotBlank()) ConnectionStatus.CONNECTED else ConnectionStatus.DISCONNECTED
        )

        if (config.apiKey.isNotBlank()) {
            fetchAvailableVoices()
        }
        if (config.geminiApiKey.isNotBlank()) {
            detectGeminiModels()
        }
    }

    fun onApiKeyChanged(key: String) {
        _uiState.value = _uiState.value.copy(
            apiKeyInput = key,
            validationError = null,
            successMessage = null
        )
    }

    fun onVoiceIdChanged(id: String) {
        _uiState.value = _uiState.value.copy(
            voiceIdInput = id,
            validationError = null,
            successMessage = null
        )
    }

    fun onVoiceNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(voiceNameInput = name)
    }

    fun onGeminiApiKeyChanged(key: String) {
        _uiState.value = _uiState.value.copy(
            geminiApiKeyInput = key,
            geminiStatusMessage = null
        )
    }

    fun onGeminiModelChanged(model: String) {
        _uiState.value = _uiState.value.copy(geminiModel = model)
        persistCurrentConfig()
    }

    fun testGeminiApi() {
        val apiKey = _uiState.value.geminiApiKeyInput.trim()
        val model = _uiState.value.geminiModel.ifBlank { "gemini-2.0-flash" }

        if (apiKey.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                geminiConnectionStatus = ConnectionStatus.ERROR,
                geminiStatusMessage = "Gemini API Key cannot be empty."
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isTestingGemini = true,
            geminiConnectionStatus = ConnectionStatus.VALIDATING,
            geminiStatusMessage = "Querying Google ModelService & testing neural generation..."
        )

        viewModelScope.launch {
            val result = repository.testGeminiApi(apiKey, model)
            result.onSuccess { testResult ->
                val updatedModels = (testResult.availableModels + GeminiApiClient.ALL_GEMINI_MODELS).distinct()

                val autoSwitchNotice = if (testResult.workingModel != model) {
                    " (Auto-switched to active: ${testResult.workingModel})"
                } else ""

                _uiState.value = _uiState.value.copy(
                    isTestingGemini = false,
                    geminiModel = testResult.workingModel,
                    availableGeminiModels = updatedModels,
                    geminiConnectionStatus = ConnectionStatus.CONNECTED,
                    geminiStatusMessage = "Gemini Online$autoSwitchNotice: \"${testResult.reply}\""
                )
                persistCurrentConfig()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isTestingGemini = false,
                    geminiConnectionStatus = ConnectionStatus.ERROR,
                    geminiStatusMessage = error.message ?: "Failed to connect to Google Gemini"
                )
            }
        }
    }

    fun detectGeminiModels() {
        val apiKey = _uiState.value.geminiApiKeyInput.trim()
        if (apiKey.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                geminiStatusMessage = "Enter Gemini API Key first to auto-detect models."
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoadingGeminiModels = true,
            geminiStatusMessage = "Scanning supported models from Google ModelService..."
        )

        viewModelScope.launch {
            val result = repository.listSupportedGeminiModels(apiKey)
            result.onSuccess { models ->
                val allCombined = (models + GeminiApiClient.ALL_GEMINI_MODELS).distinct()
                val current = _uiState.value.geminiModel
                val bestModel = if (allCombined.contains(current)) {
                    current
                } else {
                    allCombined.firstOrNull { it.contains("2.0-flash") }
                        ?: allCombined.firstOrNull { it.contains("flash") }
                        ?: allCombined.firstOrNull()
                        ?: current
                }

                _uiState.value = _uiState.value.copy(
                    isLoadingGeminiModels = false,
                    availableGeminiModels = allCombined,
                    geminiModel = bestModel,
                    geminiConnectionStatus = ConnectionStatus.CONNECTED,
                    geminiStatusMessage = "Verified ${allCombined.size} Gemini models. Selected: $bestModel"
                )
                persistCurrentConfig()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoadingGeminiModels = false,
                    geminiStatusMessage = error.message ?: "Failed to scan models from Google API"
                )
            }
        }
    }

    fun toggleBackgroundListening(context: Context) {
        if (_uiState.value.isBackgroundListeningActive) {
            MaikiVoiceForegroundService.stop(context)
            _uiState.value = _uiState.value.copy(isBackgroundListeningActive = false)
        } else {
            MaikiVoiceForegroundService.start(context)
            _uiState.value = _uiState.value.copy(isBackgroundListeningActive = true)
        }
        persistCurrentConfig()
    }

    fun setSelectionMode(mode: VoiceSelectionMode) {
        _uiState.value = _uiState.value.copy(selectionMode = mode)
        persistCurrentConfig()
    }

    fun selectAutoVoice(voice: ElevenLabsVoice) {
        _uiState.value = _uiState.value.copy(
            selectedAutoVoiceId = voice.voiceId,
            selectedAutoVoiceName = voice.name,
            voiceConfigurationStatus = "Automatic Voice Selected: ${voice.name}"
        )
        persistCurrentConfig()
    }

    fun saveVoiceSettings() {
        val state = _uiState.value
        val cleanedKey = state.apiKeyInput.trim()
        val cleanedVoiceId = state.voiceIdInput.trim()
        val cleanedName = state.voiceNameInput.trim()
        val cleanedGeminiKey = state.geminiApiKeyInput.trim()

        if (state.selectionMode == VoiceSelectionMode.MANUAL && cleanedVoiceId.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                validationError = "Voice ID cannot be empty. Please enter your ElevenLabs Voice ID.",
                successMessage = null
            )
            return
        }

        val config = VoiceConfig(
            apiKey = cleanedKey,
            customVoiceId = cleanedVoiceId,
            voiceName = cleanedName,
            selectionMode = state.selectionMode,
            selectedAutoVoiceId = state.selectedAutoVoiceId,
            selectedAutoVoiceName = state.selectedAutoVoiceName,
            defaultVoiceId = state.defaultVoiceId,
            defaultVoiceName = state.defaultVoiceName,
            isVoiceResponseEnabled = state.isVoiceResponseEnabled,
            speakingSpeed = state.speakingSpeed,
            stability = state.stability,
            similarityBoost = state.similarityBoost,
            language = state.language,
            geminiApiKey = cleanedGeminiKey,
            geminiModel = state.geminiModel,
            isBackgroundListeningEnabled = state.isBackgroundListeningActive
        )

        repository.saveVoiceConfig(config)

        val voiceStatus = if (cleanedVoiceId.isNotBlank()) {
            "Custom Voice Saved: $cleanedVoiceId"
        } else if (state.selectedAutoVoiceName.isNotBlank()) {
            "Automatic Voice: ${state.selectedAutoVoiceName}"
        } else {
            "Voice Settings Saved"
        }

        _uiState.value = _uiState.value.copy(
            voiceIdInput = cleanedVoiceId,
            voiceNameInput = cleanedName,
            geminiApiKeyInput = cleanedGeminiKey,
            voiceConfigurationStatus = voiceStatus,
            successMessage = "Voice and Gemini settings securely saved in Android KeyStore",
            validationError = null
        )
    }

    fun updateVoiceId() {
        saveVoiceSettings()
    }

    fun deleteCustomVoiceId() {
        repository.deleteCustomVoiceId()
        _uiState.value = _uiState.value.copy(
            voiceIdInput = "",
            voiceNameInput = "",
            voiceConfigurationStatus = "Custom Voice Removed",
            successMessage = "Custom Voice ID has been deleted.",
            validationError = null
        )
        persistCurrentConfig()
    }

    fun setAsDefaultVoice() {
        val state = _uiState.value
        val voiceId: String
        val voiceName: String

        if (state.selectionMode == VoiceSelectionMode.MANUAL) {
            voiceId = state.voiceIdInput.trim()
            voiceName = state.voiceNameInput.trim().ifBlank { "Custom Voice ($voiceId)" }
        } else {
            voiceId = state.selectedAutoVoiceId.trim()
            voiceName = state.selectedAutoVoiceName.trim().ifBlank { "Voice ($voiceId)" }
        }

        if (voiceId.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                validationError = "Cannot set default voice: No Voice ID selected.",
                successMessage = null
            )
            return
        }

        repository.setAsDefaultVoice(voiceId, voiceName)
        _uiState.value = _uiState.value.copy(
            defaultVoiceId = voiceId,
            defaultVoiceName = voiceName,
            voiceConfigurationStatus = "Default Voice Set: $voiceName",
            successMessage = "Saved '$voiceName' as MAIKI's default voice.",
            validationError = null
        )
    }

    fun testVoice() {
        val state = _uiState.value
        val apiKey = state.apiKeyInput.trim()
        val voiceId = if (state.selectionMode == VoiceSelectionMode.MANUAL) {
            state.voiceIdInput.trim()
        } else {
            state.selectedAutoVoiceId.trim().ifBlank { state.defaultVoiceId.trim() }
        }

        if (apiKey.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                connectionStatus = ConnectionStatus.ERROR,
                validationError = "Invalid ElevenLabs API Key.",
                successMessage = null
            )
            return
        }

        if (voiceId.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                voiceConfigurationStatus = "Invalid Voice ID",
                validationError = "Invalid Voice ID. Please check your ElevenLabs Voice ID.",
                successMessage = null
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isTesting = true,
            connectionStatus = ConnectionStatus.VALIDATING,
            validationError = null,
            successMessage = null
        )

        viewModelScope.launch {
            val result = repository.validateAndPreviewVoice(
                apiKey = apiKey,
                voiceId = voiceId,
                voiceName = state.voiceNameInput,
                sampleText = "Hello! I am MAIKI, your intelligent AI voice assistant. This is a preview of your custom voice ID."
            )

            when (result) {
                is VoiceValidationResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isTesting = false,
                        connectionStatus = ConnectionStatus.CONNECTED,
                        voiceConfigurationStatus = "Verified Active: ${result.voiceName}",
                        successMessage = "Voice ID verified successfully! Playing sample audio...",
                        validationError = null,
                        lastPreviewPath = result.audioFilePath
                    )
                    // Play audio preview
                    result.audioFilePath?.let { path ->
                        audioService.playAudioFile(File(path))
                    }
                }
                is VoiceValidationResult.InvalidApiKey -> {
                    _uiState.value = _uiState.value.copy(
                        isTesting = false,
                        connectionStatus = ConnectionStatus.ERROR,
                        validationError = "Invalid ElevenLabs API Key.",
                        successMessage = null
                    )
                }
                is VoiceValidationResult.InvalidVoiceId -> {
                    _uiState.value = _uiState.value.copy(
                        isTesting = false,
                        voiceConfigurationStatus = "Invalid Voice ID",
                        validationError = "Invalid Voice ID. Please check your ElevenLabs Voice ID.",
                        successMessage = null
                    )
                }
                is VoiceValidationResult.VoiceUnavailable -> {
                    _uiState.value = _uiState.value.copy(
                        isTesting = false,
                        voiceConfigurationStatus = "Voice Unavailable",
                        validationError = "This voice is unavailable for your account.",
                        successMessage = null
                    )
                }
                is VoiceValidationResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isTesting = false,
                        connectionStatus = ConnectionStatus.ERROR,
                        validationError = result.message,
                        successMessage = null
                    )
                }
            }
        }
    }

    fun previewVoice() {
        val previewPath = _uiState.value.lastPreviewPath
        if (previewPath != null && File(previewPath).exists()) {
            audioService.playAudioFile(File(previewPath))
        } else {
            testVoice()
        }
    }

    fun stopAudio() {
        audioService.stopAudio()
    }

    fun fetchAvailableVoices() {
        val key = _uiState.value.apiKeyInput.trim()
        if (key.isEmpty()) return

        _uiState.value = _uiState.value.copy(isLoadingVoices = true)
        viewModelScope.launch {
            val result = repository.fetchAvailableVoices(key)
            result.onSuccess { voices ->
                _uiState.value = _uiState.value.copy(
                    autoVoices = voices,
                    isLoadingVoices = false,
                    connectionStatus = ConnectionStatus.CONNECTED
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isLoadingVoices = false
                )
            }
        }
    }

    fun setVoiceResponseEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isVoiceResponseEnabled = enabled)
        persistCurrentConfig()
    }

    fun setSpeakingSpeed(speed: Float) {
        _uiState.value = _uiState.value.copy(speakingSpeed = speed)
        persistCurrentConfig()
    }

    fun setStability(stability: Float) {
        _uiState.value = _uiState.value.copy(stability = stability)
        persistCurrentConfig()
    }

    fun setSimilarityBoost(similarity: Float) {
        _uiState.value = _uiState.value.copy(similarityBoost = similarity)
        persistCurrentConfig()
    }

    fun setLanguage(language: String) {
        _uiState.value = _uiState.value.copy(language = language)
        persistCurrentConfig()
    }

    private fun persistCurrentConfig() {
        val state = _uiState.value
        val config = VoiceConfig(
            apiKey = state.apiKeyInput.trim(),
            customVoiceId = state.voiceIdInput.trim(),
            voiceName = state.voiceNameInput.trim(),
            selectionMode = state.selectionMode,
            selectedAutoVoiceId = state.selectedAutoVoiceId,
            selectedAutoVoiceName = state.selectedAutoVoiceName,
            defaultVoiceId = state.defaultVoiceId,
            defaultVoiceName = state.defaultVoiceName,
            isVoiceResponseEnabled = state.isVoiceResponseEnabled,
            speakingSpeed = state.speakingSpeed,
            stability = state.stability,
            similarityBoost = state.similarityBoost,
            language = state.language,
            geminiApiKey = state.geminiApiKeyInput.trim(),
            geminiModel = state.geminiModel,
            isBackgroundListeningEnabled = state.isBackgroundListeningActive
        )
        repository.saveVoiceConfig(config)
    }

    override fun onCleared() {
        super.onCleared()
        audioService.release()
    }
}
