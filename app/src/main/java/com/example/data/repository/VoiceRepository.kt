package com.example.data.repository

import android.content.Context
import com.example.data.api.ElevenLabsApiClient
import com.example.data.api.GeminiApiClient
import com.example.data.api.GeminiTestResult
import com.example.data.model.ElevenLabsVoice
import com.example.data.model.TtsRequestBody
import com.example.data.model.VoiceConfig
import com.example.data.model.VoiceSelectionMode
import com.example.data.model.VoiceSettingsRequest
import com.example.data.model.VoiceValidationResult
import com.example.data.secure.SecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class VoiceRepository(private val context: Context) {

    private val secureStorage = SecureStorage(context)

    companion object {
        private const val KEY_API_KEY = "eleven_api_key"
        private const val KEY_CUSTOM_VOICE_ID = "eleven_custom_voice_id"
        private const val KEY_VOICE_NAME = "eleven_voice_name"
        private const val KEY_SELECTION_MODE = "eleven_selection_mode"
        private const val KEY_AUTO_VOICE_ID = "eleven_auto_voice_id"
        private const val KEY_AUTO_VOICE_NAME = "eleven_auto_voice_name"
        private const val KEY_DEFAULT_VOICE_ID = "eleven_default_voice_id"
        private const val KEY_DEFAULT_VOICE_NAME = "eleven_default_voice_name"
        private const val KEY_VOICE_RESPONSE_ENABLED = "eleven_voice_response_enabled"
        private const val KEY_SPEAKING_SPEED = "eleven_speaking_speed"
        private const val KEY_STABILITY = "eleven_stability"
        private const val KEY_SIMILARITY_BOOST = "eleven_similarity_boost"
        private const val KEY_LANGUAGE = "eleven_language"
        private const val KEY_GEMINI_API_KEY = "maiki_gemini_api_key"
        private const val KEY_GEMINI_MODEL = "maiki_gemini_model"
        private const val KEY_BG_LISTENING = "maiki_bg_listening"
    }

    fun loadVoiceConfig(): VoiceConfig {
        val apiKey = secureStorage.getSecureString(KEY_API_KEY, "")
        val customVoiceId = secureStorage.getSecureString(KEY_CUSTOM_VOICE_ID, "")
        val voiceName = secureStorage.getSecureString(KEY_VOICE_NAME, "")
        val modeStr = secureStorage.getSecureString(KEY_SELECTION_MODE, VoiceSelectionMode.MANUAL.name)
        val selectionMode = try {
            VoiceSelectionMode.valueOf(modeStr)
        } catch (_: Exception) {
            VoiceSelectionMode.MANUAL
        }
        val autoVoiceId = secureStorage.getSecureString(KEY_AUTO_VOICE_ID, "")
        val autoVoiceName = secureStorage.getSecureString(KEY_AUTO_VOICE_NAME, "")
        val defaultVoiceId = secureStorage.getSecureString(KEY_DEFAULT_VOICE_ID, "")
        val defaultVoiceName = secureStorage.getSecureString(KEY_DEFAULT_VOICE_NAME, "")
        val isVoiceResponseEnabled = secureStorage.getBoolean(KEY_VOICE_RESPONSE_ENABLED, true)
        val speakingSpeed = secureStorage.getFloat(KEY_SPEAKING_SPEED, 1.0f)
        val stability = secureStorage.getFloat(KEY_STABILITY, 0.5f)
        val similarityBoost = secureStorage.getFloat(KEY_SIMILARITY_BOOST, 0.75f)
        val language = secureStorage.getSecureString(KEY_LANGUAGE, "English")
        val geminiApiKey = secureStorage.getSecureString(KEY_GEMINI_API_KEY, "")
        val geminiModel = secureStorage.getSecureString(KEY_GEMINI_MODEL, "gemini-2.0-flash")
        val isBgListening = secureStorage.getBoolean(KEY_BG_LISTENING, false)

        return VoiceConfig(
            apiKey = apiKey,
            customVoiceId = customVoiceId,
            voiceName = voiceName,
            selectionMode = selectionMode,
            selectedAutoVoiceId = autoVoiceId,
            selectedAutoVoiceName = autoVoiceName,
            defaultVoiceId = defaultVoiceId,
            defaultVoiceName = defaultVoiceName,
            isVoiceResponseEnabled = isVoiceResponseEnabled,
            speakingSpeed = speakingSpeed,
            stability = stability,
            similarityBoost = similarityBoost,
            language = language,
            geminiApiKey = geminiApiKey,
            geminiModel = geminiModel,
            isBackgroundListeningEnabled = isBgListening
        )
    }

    fun saveVoiceConfig(config: VoiceConfig) {
        secureStorage.putSecureString(KEY_API_KEY, config.apiKey.trim())
        secureStorage.putSecureString(KEY_CUSTOM_VOICE_ID, config.customVoiceId.trim())
        secureStorage.putSecureString(KEY_VOICE_NAME, config.voiceName.trim())
        secureStorage.putSecureString(KEY_SELECTION_MODE, config.selectionMode.name)
        secureStorage.putSecureString(KEY_AUTO_VOICE_ID, config.selectedAutoVoiceId.trim())
        secureStorage.putSecureString(KEY_AUTO_VOICE_NAME, config.selectedAutoVoiceName.trim())
        secureStorage.putSecureString(KEY_DEFAULT_VOICE_ID, config.defaultVoiceId.trim())
        secureStorage.putSecureString(KEY_DEFAULT_VOICE_NAME, config.defaultVoiceName.trim())
        secureStorage.putBoolean(KEY_VOICE_RESPONSE_ENABLED, config.isVoiceResponseEnabled)
        secureStorage.putFloat(KEY_SPEAKING_SPEED, config.speakingSpeed)
        secureStorage.putFloat(KEY_STABILITY, config.stability)
        secureStorage.putFloat(KEY_SIMILARITY_BOOST, config.similarityBoost)
        secureStorage.putSecureString(KEY_LANGUAGE, config.language.trim())
        secureStorage.putSecureString(KEY_GEMINI_API_KEY, config.geminiApiKey.trim())
        secureStorage.putSecureString(KEY_GEMINI_MODEL, config.geminiModel.trim())
        secureStorage.putBoolean(KEY_BG_LISTENING, config.isBackgroundListeningEnabled)
    }

    fun saveCustomVoiceId(voiceId: String, voiceName: String? = null): Boolean {
        val trimmed = voiceId.trim()
        if (trimmed.isEmpty()) return false
        secureStorage.putSecureString(KEY_CUSTOM_VOICE_ID, trimmed)
        if (!voiceName.isNullOrBlank()) {
            secureStorage.putSecureString(KEY_VOICE_NAME, voiceName.trim())
        }
        return true
    }

    fun updateCustomVoiceId(voiceId: String, voiceName: String? = null): Boolean {
        return saveCustomVoiceId(voiceId, voiceName)
    }

    fun deleteCustomVoiceId() {
        secureStorage.remove(KEY_CUSTOM_VOICE_ID)
        secureStorage.remove(KEY_VOICE_NAME)
    }

    fun setAsDefaultVoice(voiceId: String, voiceName: String) {
        val trimmedId = voiceId.trim()
        val trimmedName = voiceName.trim()
        if (trimmedId.isNotEmpty()) {
            secureStorage.putSecureString(KEY_DEFAULT_VOICE_ID, trimmedId)
            secureStorage.putSecureString(KEY_DEFAULT_VOICE_NAME, trimmedName.ifBlank { "Voice $trimmedId" })
        }
    }

    suspend fun testGeminiApi(apiKey: String, model: String): Result<GeminiTestResult> {
        return GeminiApiClient.testGeminiApiKey(apiKey, model)
    }

    suspend fun listSupportedGeminiModels(apiKey: String): Result<List<String>> {
        return GeminiApiClient.listSupportedModels(apiKey)
    }

    suspend fun fetchAvailableVoices(apiKey: String): Result<List<ElevenLabsVoice>> =
        withContext(Dispatchers.IO) {
            val key = apiKey.trim()
            if (key.isEmpty()) {
                return@withContext Result.failure(Exception("Invalid ElevenLabs API Key."))
            }

            try {
                val response = ElevenLabsApiClient.service.getVoices(key)
                if (response.isSuccessful) {
                    val voices = response.body()?.voices ?: emptyList()
                    Result.success(voices)
                } else {
                    val code = response.code()
                    val errorMsg = parseErrorMessage(code, response.errorBody())
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Result.failure(Exception(e.message ?: "Network error fetching voices"))
            }
        }

    /**
     * Validates the voice ID with the ElevenLabs API and generates a voice preview audio.
     * Matches user-specified error messages exactly.
     */
    suspend fun validateAndPreviewVoice(
        apiKey: String,
        voiceId: String,
        voiceName: String? = null,
        sampleText: String? = null
    ): VoiceValidationResult = withContext(Dispatchers.IO) {
        val trimmedKey = apiKey.trim()
        val trimmedVoiceId = voiceId.trim()

        if (trimmedKey.isEmpty()) {
            return@withContext VoiceValidationResult.InvalidApiKey
        }

        if (trimmedVoiceId.isEmpty()) {
            return@withContext VoiceValidationResult.InvalidVoiceId
        }

        val textToSpeak = sampleText?.takeIf { it.isNotBlank() }
            ?: "Hello! I am MAIKI, your intelligent AI voice assistant. This is a preview of your custom voice ID."

        val request = TtsRequestBody(
            text = textToSpeak,
            modelId = "eleven_turbo_v2_5",
            voiceSettings = VoiceSettingsRequest(
                stability = 0.5f,
                similarityBoost = 0.75f,
                style = 0.0f,
                useSpeakerBoost = true
            )
        )

        try {
            val response = ElevenLabsApiClient.service.textToSpeech(
                apiKey = trimmedKey,
                voiceId = trimmedVoiceId,
                request = request
            )

            if (response.isSuccessful && response.body() != null) {
                val audioFile = saveAudioResponseToFile(response.body()!!, "test_preview_$trimmedVoiceId.mp3")
                val resolvedName = voiceName?.takeIf { it.isNotBlank() } ?: "Voice $trimmedVoiceId"
                VoiceValidationResult.Success(
                    voiceId = trimmedVoiceId,
                    voiceName = resolvedName,
                    audioFilePath = audioFile.absolutePath
                )
            } else {
                when (response.code()) {
                    401 -> VoiceValidationResult.InvalidApiKey
                    404 -> VoiceValidationResult.InvalidVoiceId
                    403 -> VoiceValidationResult.VoiceUnavailable
                    else -> {
                        val errorText = parseErrorMessage(response.code(), response.errorBody())
                        if (errorText.contains("voice", ignoreCase = true) && errorText.contains("not found", ignoreCase = true)) {
                            VoiceValidationResult.InvalidVoiceId
                        } else {
                            VoiceValidationResult.Error(errorText)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            VoiceValidationResult.Error(e.message ?: "Connection error communicating with ElevenLabs")
        }
    }

    /**
     * Synthesizes speech for MAIKI's conversational AI response.
     */
    suspend fun synthesizeSpeech(
        text: String,
        voiceId: String,
        apiKey: String,
        settings: VoiceSettingsRequest = VoiceSettingsRequest()
    ): Result<File> = withContext(Dispatchers.IO) {
        val trimmedKey = apiKey.trim()
        val trimmedVoiceId = voiceId.trim()

        if (trimmedKey.isEmpty()) {
            return@withContext Result.failure(Exception("Invalid ElevenLabs API Key."))
        }
        if (trimmedVoiceId.isEmpty()) {
            return@withContext Result.failure(Exception("Invalid Voice ID. Please check your ElevenLabs Voice ID."))
        }

        val request = TtsRequestBody(
            text = text,
            modelId = "eleven_turbo_v2_5",
            voiceSettings = settings
        )

        try {
            val response = ElevenLabsApiClient.service.textToSpeech(
                apiKey = trimmedKey,
                voiceId = trimmedVoiceId,
                request = request
            )

            if (response.isSuccessful && response.body() != null) {
                val file = saveAudioResponseToFile(response.body()!!, "maiki_speech_${System.currentTimeMillis()}.mp3")
                Result.success(file)
            } else {
                val code = response.code()
                val message = when (code) {
                    401 -> "Invalid ElevenLabs API Key."
                    404 -> "Invalid Voice ID. Please check your ElevenLabs Voice ID."
                    403 -> "This voice is unavailable for your account."
                    else -> parseErrorMessage(code, response.errorBody())
                }
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveAudioResponseToFile(body: ResponseBody, filename: String): File {
        val cacheDir = context.cacheDir
        val audioFile = File(cacheDir, filename)
        body.byteStream().use { input ->
            FileOutputStream(audioFile).use { output ->
                input.copyTo(output)
            }
        }
        return audioFile
    }

    private fun parseErrorMessage(statusCode: Int, errorBody: ResponseBody?): String {
        return try {
            val raw = errorBody?.string() ?: ""
            if (raw.isNotBlank()) {
                val json = JSONObject(raw)
                if (json.has("detail")) {
                    val detail = json.get("detail")
                    if (detail is JSONObject && detail.has("message")) {
                        detail.getString("message")
                    } else {
                        detail.toString()
                    }
                } else if (json.has("message")) {
                    json.getString("message")
                } else {
                    "HTTP $statusCode error"
                }
            } else {
                when (statusCode) {
                    401 -> "Invalid ElevenLabs API Key."
                    404 -> "Invalid Voice ID. Please check your ElevenLabs Voice ID."
                    403 -> "This voice is unavailable for your account."
                    else -> "ElevenLabs request failed (HTTP $statusCode)"
                }
            }
        } catch (_: Exception) {
            when (statusCode) {
                401 -> "Invalid ElevenLabs API Key."
                404 -> "Invalid Voice ID. Please check your ElevenLabs Voice ID."
                403 -> "This voice is unavailable for your account."
                else -> "ElevenLabs request failed (HTTP $statusCode)"
            }
        }
    }
}
