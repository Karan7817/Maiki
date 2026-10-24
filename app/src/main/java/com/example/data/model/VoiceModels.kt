package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class VoiceSelectionMode {
    AUTOMATIC,
    MANUAL
}

enum class ConnectionStatus {
    DISCONNECTED,
    VALIDATING,
    CONNECTED,
    ERROR
}

@JsonClass(generateAdapter = true)
data class ElevenLabsVoice(
    @Json(name = "voice_id") val voiceId: String,
    @Json(name = "name") val name: String,
    @Json(name = "category") val category: String? = null,
    @Json(name = "preview_url") val previewUrl: String? = null,
    @Json(name = "description") val description: String? = null
)

@JsonClass(generateAdapter = true)
data class VoicesResponse(
    @Json(name = "voices") val voices: List<ElevenLabsVoice> = emptyList()
)

@JsonClass(generateAdapter = true)
data class VoiceSettingsRequest(
    @Json(name = "stability") val stability: Float = 0.5f,
    @Json(name = "similarity_boost") val similarityBoost: Float = 0.75f,
    @Json(name = "style") val style: Float = 0.0f,
    @Json(name = "use_speaker_boost") val useSpeakerBoost: Boolean = true
)

@JsonClass(generateAdapter = true)
data class TtsRequestBody(
    @Json(name = "text") val text: String,
    @Json(name = "model_id") val modelId: String = "eleven_turbo_v2_5",
    @Json(name = "voice_settings") val voiceSettings: VoiceSettingsRequest? = null
)

data class VoiceConfig(
    val apiKey: String = "",
    val customVoiceId: String = "",
    val voiceName: String = "",
    val selectionMode: VoiceSelectionMode = VoiceSelectionMode.MANUAL,
    val selectedAutoVoiceId: String = "",
    val selectedAutoVoiceName: String = "",
    val defaultVoiceId: String = "",
    val defaultVoiceName: String = "",
    val isVoiceResponseEnabled: Boolean = true,
    val speakingSpeed: Float = 1.0f,
    val stability: Float = 0.5f,
    val similarityBoost: Float = 0.75f,
    val language: String = "English",
    val geminiApiKey: String = "",
    val geminiModel: String = "gemini-2.0-flash",
    val isBackgroundListeningEnabled: Boolean = false
) {
    /**
     * Determines the active Voice ID.
     * In MANUAL mode, the user's custom Voice ID takes top priority.
     * In AUTOMATIC mode, the selected automatic voice is used, falling back to defaultVoiceId.
     */
    val effectiveVoiceId: String
        get() = when (selectionMode) {
            VoiceSelectionMode.MANUAL -> customVoiceId.trim()
            VoiceSelectionMode.AUTOMATIC -> {
                if (selectedAutoVoiceId.isNotBlank()) selectedAutoVoiceId.trim()
                else defaultVoiceId.trim()
            }
        }

    val effectiveVoiceName: String
        get() = when (selectionMode) {
            VoiceSelectionMode.MANUAL -> {
                if (voiceName.isNotBlank()) voiceName.trim()
                else if (customVoiceId.isNotBlank()) "Custom Voice (${customVoiceId.take(8)}...)"
                else "Unconfigured"
            }
            VoiceSelectionMode.AUTOMATIC -> {
                if (selectedAutoVoiceName.isNotBlank()) selectedAutoVoiceName
                else if (defaultVoiceName.isNotBlank()) defaultVoiceName
                else "None Selected"
            }
        }
}

sealed class VoiceValidationResult {
    data class Success(val voiceId: String, val voiceName: String, val audioFilePath: String?) : VoiceValidationResult()
    object InvalidApiKey : VoiceValidationResult()
    object InvalidVoiceId : VoiceValidationResult()
    object VoiceUnavailable : VoiceValidationResult()
    data class Error(val message: String) : VoiceValidationResult()
}

sealed class AudioPlaybackState {
    object Idle : AudioPlaybackState()
    object Loading : AudioPlaybackState()
    data class Playing(val progress: Float, val currentMs: Int, val totalMs: Int) : AudioPlaybackState()
    object Paused : AudioPlaybackState()
    object Stopped : AudioPlaybackState()
    data class Error(val message: String) : AudioPlaybackState()
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val audioFilePath: String? = null,
    val voiceIdUsed: String? = null,
    val isFallbackTts: Boolean = false,
    val errorMessage: String? = null
)
