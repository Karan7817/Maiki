package com.example.data.api

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>,
    @Json(name = "role") val role: String? = "user"
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent?
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiModelItem(
    @Json(name = "name") val name: String,
    @Json(name = "displayName") val displayName: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "supportedGenerationMethods") val supportedGenerationMethods: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiListModelsResponse(
    @Json(name = "models") val models: List<GeminiModelItem>? = null
)

data class GeminiTestResult(
    val workingModel: String,
    val reply: String,
    val availableModels: List<String>
)

interface GeminiApiService {
    @GET("v1beta/models")
    suspend fun listModels(
        @Query("key") apiKey: String
    ): Response<GeminiListModelsResponse>

    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): Response<GeminiResponse>
}

object GeminiApiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    data class ModelDescriptor(
        val id: String,
        val tag: String,
        val subtitle: String,
        val isRecommended: Boolean = false
    )

    val ALL_GEMINI_MODELS = listOf(
        // Gemini 2.0 & 2.5 Next-Gen
        "gemini-2.0-flash",
        "gemini-2.0-flash-lite",
        "gemini-2.0-flash-lite-preview-02-05",
        "gemini-2.0-flash-thinking-exp-01-21",
        "gemini-2.0-pro-exp-02-05",
        "gemini-2.5-flash",
        "gemini-2.5-pro",
        "gemini-2.0-flash-001",
        "gemini-2.0-flash-exp",
        // Gemini 1.5 Flash
        "gemini-1.5-flash",
        "gemini-1.5-flash-latest",
        "gemini-1.5-flash-001",
        "gemini-1.5-flash-002",
        // Gemini 1.5 Flash-8B
        "gemini-1.5-flash-8b",
        "gemini-1.5-flash-8b-latest",
        "gemini-1.5-flash-8b-001",
        // Gemini 1.5 Pro
        "gemini-1.5-pro",
        "gemini-1.5-pro-latest",
        "gemini-1.5-pro-001",
        "gemini-1.5-pro-002",
        // Experimental & Specialized
        "gemini-exp-1206",
        "gemini-exp-1121",
        "gemini-exp-1114",
        "learnlm-1.5-pro-experimental",
        // Gemini 1.0 Legacy
        "gemini-1.0-pro",
        "gemini-pro",
        "gemini-1.0-pro-latest",
        "gemini-1.0-pro-001"
    )

    val PREFERRED_MODELS = ALL_GEMINI_MODELS

    fun getModelDescriptor(modelId: String): ModelDescriptor {
        val clean = cleanModelName(modelId)
        return when (clean) {
            "gemini-2.0-flash" -> ModelDescriptor(clean, "2.0 FLASH", "Ultra-fast multimodal core - Recommended", isRecommended = true)
            "gemini-2.0-flash-lite" -> ModelDescriptor(clean, "2.0 LITE", "Maximum speed & lowest latency")
            "gemini-2.0-flash-lite-preview-02-05" -> ModelDescriptor(clean, "2.0 LITE", "Flash-Lite preview build 02-05")
            "gemini-2.0-flash-thinking-exp-01-21" -> ModelDescriptor(clean, "THINKING", "Deep chain-of-thought reasoning")
            "gemini-2.0-pro-exp-02-05" -> ModelDescriptor(clean, "2.0 PRO", "Frontier intelligence & complex coding")
            "gemini-2.5-flash" -> ModelDescriptor(clean, "2.5 NEXT-GEN", "Next-gen adaptive Flash architecture")
            "gemini-2.5-pro" -> ModelDescriptor(clean, "2.5 PRO", "Next-gen frontier reasoning model")
            "gemini-2.0-flash-001" -> ModelDescriptor(clean, "2.0 STABLE", "Production pinned 2.0 release 001")
            "gemini-2.0-flash-exp" -> ModelDescriptor(clean, "2.0 EXP", "Experimental 2.0 Flash preview")
            "gemini-1.5-flash" -> ModelDescriptor(clean, "1.5 FLASH", "Proven 1M token context engine")
            "gemini-1.5-flash-latest" -> ModelDescriptor(clean, "1.5 FLASH", "Always points to newest 1.5 Flash")
            "gemini-1.5-flash-001" -> ModelDescriptor(clean, "1.5 FLASH", "Stable 1.5 Flash build 001")
            "gemini-1.5-flash-002" -> ModelDescriptor(clean, "1.5 FLASH", "Updated 1.5 Flash snapshot 002")
            "gemini-1.5-flash-8b" -> ModelDescriptor(clean, "8B LIGHT", "High-throughput 8B parameter engine")
            "gemini-1.5-flash-8b-latest" -> ModelDescriptor(clean, "8B LIGHT", "Latest 8B Flash pointer")
            "gemini-1.5-flash-8b-001" -> ModelDescriptor(clean, "8B LIGHT", "Stable 8B Flash build 001")
            "gemini-1.5-pro" -> ModelDescriptor(clean, "1.5 PRO", "2M tokens context & complex analysis")
            "gemini-1.5-pro-latest" -> ModelDescriptor(clean, "1.5 PRO", "Always points to newest 1.5 Pro")
            "gemini-1.5-pro-001" -> ModelDescriptor(clean, "1.5 PRO", "Stable 1.5 Pro build 001")
            "gemini-1.5-pro-002" -> ModelDescriptor(clean, "1.5 PRO", "Updated 1.5 Pro snapshot 002")
            "gemini-exp-1206" -> ModelDescriptor(clean, "EXPERIMENTAL", "Frontier benchmark leader 1206")
            "gemini-exp-1121" -> ModelDescriptor(clean, "EXPERIMENTAL", "Quality experimental snapshot 1121")
            "gemini-exp-1114" -> ModelDescriptor(clean, "EXPERIMENTAL", "Math & logic snapshot 1114")
            "learnlm-1.5-pro-experimental" -> ModelDescriptor(clean, "LEARNLM", "Pedagogical instruction & tutoring")
            "gemini-1.0-pro" -> ModelDescriptor(clean, "1.0 PRO", "Classic 1.0 generation model")
            "gemini-pro" -> ModelDescriptor(clean, "1.0 PRO", "Original Gemini Pro endpoint")
            "gemini-1.0-pro-latest" -> ModelDescriptor(clean, "1.0 PRO", "Latest 1.0 Pro pointer")
            "gemini-1.0-pro-001" -> ModelDescriptor(clean, "1.0 PRO", "Stable 1.0 Pro build 001")
            else -> {
                val tag = when {
                    clean.contains("thinking") -> "THINKING"
                    clean.contains("2.5") -> "2.5 GEN"
                    clean.contains("2.0") -> "2.0 GEN"
                    clean.contains("8b") -> "8B LITE"
                    clean.contains("pro") -> "PRO"
                    clean.contains("flash") -> "FLASH"
                    clean.contains("exp") -> "EXP"
                    else -> "GEMINI"
                }
                ModelDescriptor(clean, tag, "Google Generative Model ($clean)")
            }
        }
    }

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val service: GeminiApiService = retrofit.create(GeminiApiService::class.java)

    /**
     * Strips leading "models/" or slashes to ensure valid format for the API path.
     */
    fun cleanModelName(model: String): String {
        return model.trim()
            .removePrefix("models/")
            .removePrefix("/")
    }

    /**
     * Calls Google's official ModelService (GET /v1beta/models) to discover
     * all models supported by the provided API key that support generateContent.
     */
    suspend fun listSupportedModels(apiKey: String): Result<List<String>> = withContext(Dispatchers.IO) {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isEmpty()) {
            return@withContext Result.failure(Exception("Gemini API Key cannot be empty."))
        }

        try {
            val response = service.listModels(trimmedKey)
            if (response.isSuccessful) {
                val rawModels = response.body()?.models ?: emptyList()
                val generateModels = rawModels
                    .filter { it.supportedGenerationMethods?.contains("generateContent") == true }
                    .map { cleanModelName(it.name) }
                    .filter { it.isNotBlank() }
                    .distinct()

                val discovered = if (generateModels.isNotEmpty()) {
                    generateModels
                } else {
                    rawModels.map { cleanModelName(it.name) }.filter { it.isNotBlank() }.distinct()
                }

                val combined = if (discovered.isNotEmpty()) {
                    (discovered + ALL_GEMINI_MODELS).distinct()
                } else {
                    ALL_GEMINI_MODELS
                }
                Result.success(combined)
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                Result.failure(Exception(parseGeminiError(response.code(), errorBody)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to list Gemini models from Google API"))
        }
    }

    /**
     * Validates a user-provided Gemini API key with Google's API endpoint.
     * Proactively verifies available models from ModelService and tests generateContent.
     * If the requested model returns 404, it automatically falls back to an active supported model.
     */
    suspend fun testGeminiApiKey(
        apiKey: String,
        model: String = "gemini-2.0-flash"
    ): Result<GeminiTestResult> = withContext(Dispatchers.IO) {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isEmpty()) {
            return@withContext Result.failure(Exception("Gemini API Key cannot be empty."))
        }

        val cleanedRequestedModel = cleanModelName(model).ifBlank { "gemini-2.0-flash" }

        // 1. Fetch available models from Google ModelService
        val modelsListResult = listSupportedModels(trimmedKey)
        val availableModels: List<String> = modelsListResult.getOrDefault(emptyList())

        // If listing models failed with auth/permission error, fail early with clear message
        if (modelsListResult.isFailure && availableModels.isEmpty()) {
            val err = modelsListResult.exceptionOrNull()?.message ?: "Invalid Gemini API Key or connection error."
            return@withContext Result.failure(Exception(err))
        }

        // 2. Build candidate fallback chain
        val candidateModels = mutableListOf<String>()

        // Priority A: requested model if present in availableModels
        if (availableModels.contains(cleanedRequestedModel)) {
            candidateModels.add(cleanedRequestedModel)
        }

        // Priority B: preferred active flash models present in availableModels
        for (pref in PREFERRED_MODELS) {
            if (availableModels.contains(pref) && !candidateModels.contains(pref)) {
                candidateModels.add(pref)
            }
        }

        // Priority C: any remaining available models that support generateContent
        for (avail in availableModels) {
            if (!candidateModels.contains(avail)) {
                candidateModels.add(avail)
            }
        }

        // Priority D: If availableModels was empty or missing matches, try requested model and defaults
        if (candidateModels.isEmpty()) {
            candidateModels.add(cleanedRequestedModel)
            for (pref in PREFERRED_MODELS) {
                if (!candidateModels.contains(pref)) {
                    candidateModels.add(pref)
                }
            }
        }

        // 3. Test generation with candidate models
        var lastError: String? = null
        val testRequest = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = "Confirm MAIKI neural link status in under 8 words."))
                )
            )
        )

        for (candidate in candidateModels) {
            try {
                val response = service.generateContent(
                    model = candidate,
                    apiKey = trimmedKey,
                    request = testRequest
                )

                if (response.isSuccessful) {
                    val reply = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        ?: "Neural Core Online"
                    return@withContext Result.success(
                        GeminiTestResult(
                            workingModel = candidate,
                            reply = reply.trim(),
                            availableModels = if (availableModels.isNotEmpty()) availableModels else candidateModels
                        )
                    )
                } else {
                    val errorBody = response.errorBody()?.string() ?: ""
                    lastError = parseGeminiError(response.code(), errorBody)

                    // For non-404 errors (such as 400 Bad Request or 403 Forbidden), abort immediately
                    if (response.code() == 400 || response.code() == 403) {
                        return@withContext Result.failure(Exception(lastError))
                    }
                }
            } catch (e: Exception) {
                lastError = e.message ?: "Connection error with Gemini API"
            }
        }

        Result.failure(Exception(lastError ?: "Failed to generate content with available Gemini models."))
    }

    /**
     * Generates an AI response for spoken audio playback.
     * Includes automatic model fallback if the primary model returns 404.
     */
    suspend fun generateAiResponse(
        userPrompt: String,
        customGeminiKey: String? = null,
        model: String = "gemini-2.0-flash"
    ): String = withContext(Dispatchers.IO) {
        val apiKey: String = customGeminiKey?.takeIf { it.isNotBlank() }
            ?: runCatching { BuildConfig.GEMINI_API_KEY }.getOrDefault("")

        val cleanedModel = cleanModelName(model).ifBlank { "gemini-2.0-flash" }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            val trimmedKey = apiKey.trim()
            val promptWithSystem = "You are MAIKI, an advanced, concise, intelligent voice AI assistant (like Iron Man's J.A.R.V.I.S.). Keep answers spoken, natural, confident, and direct (1-3 sentences) suitable for text-to-speech audio playback. User says: $userPrompt"
            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = promptWithSystem))
                    )
                )
            )

            // Try primary requested model
            try {
                val response = service.generateContent(
                    model = cleanedModel,
                    apiKey = trimmedKey,
                    request = request
                )
                if (response.isSuccessful) {
                    val text = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (!text.isNullOrBlank()) {
                        return@withContext text.trim()
                    }
                }
            } catch (_: Exception) {
                // Proceed to fallback
            }

            // Fallback chain across known active flash models
            for (fallbackCandidate in PREFERRED_MODELS) {
                if (fallbackCandidate == cleanedModel) continue
                try {
                    val response = service.generateContent(
                        model = fallbackCandidate,
                        apiKey = trimmedKey,
                        request = request
                    )
                    if (response.isSuccessful) {
                        val text = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        if (!text.isNullOrBlank()) {
                            return@withContext text.trim()
                        }
                    }
                } catch (_: Exception) {
                    // Try next candidate
                }
            }
        }

        // Smart built-in J.A.R.V.I.S. conversational fallback
        return@withContext generateJarvisFallback(userPrompt)
    }

    private fun parseGeminiError(code: Int, errorBody: String): String {
        return try {
            if (errorBody.isNotBlank()) {
                val json = JSONObject(errorBody)
                if (json.has("error")) {
                    val err = json.getJSONObject("error")
                    val msg = err.optString("message", "")
                    val status = err.optString("status", "")
                    if (msg.isNotBlank()) {
                        return "Gemini API Error ($code $status): $msg"
                    }
                }
            }
            when (code) {
                400 -> "Invalid Gemini API Key or malformed request."
                403 -> "Gemini API Key permission denied. Ensure Generative Language API is enabled."
                404 -> "Requested Gemini model not found on this API key. Try 'gemini-2.0-flash' or tap AUTO-DETECT."
                429 -> "Gemini API rate limit exceeded. Please wait a moment."
                else -> "Gemini API request failed (HTTP $code)"
            }
        } catch (_: Exception) {
            "Gemini API request failed (HTTP $code)"
        }
    }

    private fun generateJarvisFallback(query: String): String {
        val lower = query.trim().lowercase()
        return when {
            lower.contains("hello") || lower.contains("hi") || lower.contains("hey") ->
                "Greetings. MAIKI AI systems are online, vocal synthesis is primed, and Mark armor telemetry is nominal."
            lower.contains("who are you") || lower.contains("what is your name") ->
                "I am MAIKI AI, your intelligent tactical voice assistant powered by Gemini intelligence and ElevenLabs neural speech."
            lower.contains("how are you") || lower.contains("status") ->
                "All systems nominal. Arc Reactor is operating at peak efficiency, and speech synthesis channels are active."
            lower.contains("time") ->
                "Current temporal index synchronized. Standing by for your next voice directive."
            lower.contains("voice") || lower.contains("elevenlabs") ->
                "You can configure your ElevenLabs API Key and custom Voice ID in Voice Settings. Every transmission is synthesized using your exact configured voice."
            lower.contains("gemini") ->
                "Gemini AI neural link can be configured in settings with your Google Gemini API Key for unbounded conversational intelligence."
            lower.contains("help") ->
                "You can speak to me, trigger background voice monitoring, or customize speech parameters and API keys in Voice Settings."
            else ->
                "Transmission received. Processing your directive: \"$query\". All neural audio channels are responsive."
        }
    }
}
