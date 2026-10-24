package com.example.data.api

import com.example.data.model.TtsRequestBody
import com.example.data.model.VoicesResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming
import java.util.concurrent.TimeUnit

interface ElevenLabsApiService {

    @GET("v1/voices")
    suspend fun getVoices(
        @Header("xi-api-key") apiKey: String
    ): Response<VoicesResponse>

    @GET("v1/voices/{voice_id}")
    suspend fun getVoiceDetails(
        @Header("xi-api-key") apiKey: String,
        @Path("voice_id") voiceId: String
    ): Response<ResponseBody>

    @Streaming
    @POST("v1/text-to-speech/{voice_id}")
    suspend fun textToSpeech(
        @Header("xi-api-key") apiKey: String,
        @Path("voice_id") voiceId: String,
        @Body request: TtsRequestBody
    ): Response<ResponseBody>
}

object ElevenLabsApiClient {
    private const val BASE_URL = "https://api.elevenlabs.io/"

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // Note: Do not log headers or sensitive bodies in production logs to protect user API keys
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val service: ElevenLabsApiService = retrofit.create(ElevenLabsApiService::class.java)
}
