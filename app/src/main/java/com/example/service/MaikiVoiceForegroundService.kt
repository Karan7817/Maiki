package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.api.GeminiApiClient
import com.example.data.model.VoiceSettingsRequest
import com.example.data.repository.VoiceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Background Voice Assistant Foreground Service.
 * Continuously listens for voice queries in foreground or background,
 * processes them with Gemini AI, and speaks the response via ElevenLabs voice.
 */
class MaikiVoiceForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var repository: VoiceRepository
    private lateinit var audioService: VoiceAudioService

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isListeningLoopActive = false
    private var isProcessingQuery = false

    companion object {
        const val ACTION_START = "com.example.service.action.START_VOICE_SERVICE"
        const val ACTION_STOP = "com.example.service.action.STOP_VOICE_SERVICE"
        private const val CHANNEL_ID = "maiki_voice_channel"
        private const val NOTIFICATION_ID = 2001

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _assistantState = MutableStateFlow("IDLE")
        val assistantState: StateFlow<String> = _assistantState.asStateFlow()

        private val _latestTransmission = MutableStateFlow<Pair<String, String>?>(null)
        val latestTransmission: StateFlow<Pair<String, String>?> = _latestTransmission.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, MaikiVoiceForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, MaikiVoiceForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = VoiceRepository(applicationContext)
        audioService = VoiceAudioService(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForegroundListening()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START, null -> {
                startForegroundWithNotification()
                startContinuousListening()
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MAIKI AI Voice Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground microphone listener for MAIKI voice assistant"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundWithNotification() {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingOpenIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, MaikiVoiceForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStopIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MAIKI AI // Background Voice Active")
            .setContentText("Listening for hands-free voice commands...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingOpenIntent)
            .addAction(R.drawable.ic_launcher_foreground, "STOP LISTENER", pendingStopIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        _isRunning.value = true
        _assistantState.value = "LISTENING"
    }

    private fun startContinuousListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            _assistantState.value = "SPEECH RECOGNITION UNAVAILABLE"
            return
        }

        isListeningLoopActive = true
        initRecognizer()
        listenOnce()
    }

    private fun initRecognizer() {
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _assistantState.value = "LISTENING"
                }

                override fun onBeginningOfSpeech() {
                    _assistantState.value = "DETECTING SPEECH"
                }

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _assistantState.value = "PROCESSING AUDIO"
                }

                override fun onError(error: Int) {
                    if (isListeningLoopActive && !isProcessingQuery) {
                        _assistantState.value = "LISTENING (STANDBY)"
                        mainHandler.postDelayed({
                            if (isListeningLoopActive && !isProcessingQuery) {
                                listenOnce()
                            }
                        }, 1200)
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val recognizedText = matches?.firstOrNull()?.trim()
                    if (!recognizedText.isNullOrEmpty()) {
                        processUserVoiceQuery(recognizedText)
                    } else if (isListeningLoopActive) {
                        mainHandler.postDelayed({
                            if (isListeningLoopActive && !isProcessingQuery) {
                                listenOnce()
                            }
                        }, 800)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun listenOnce() {
        if (!isListeningLoopActive || isProcessingQuery) return
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            mainHandler.postDelayed({
                if (isListeningLoopActive && !isProcessingQuery) {
                    initRecognizer()
                    listenOnce()
                }
            }, 1500)
        }
    }

    private fun processUserVoiceQuery(query: String) {
        isProcessingQuery = true
        _assistantState.value = "PROCESSING WITH GEMINI AI..."

        serviceScope.launch {
            try {
                val config = repository.loadVoiceConfig()

                // 1. Generate Response with Gemini AI
                val aiReply = GeminiApiClient.generateAiResponse(
                    userPrompt = query,
                    customGeminiKey = config.geminiApiKey,
                    model = config.geminiModel
                )

                _latestTransmission.value = Pair(query, aiReply)
                _assistantState.value = "SPEAKING ELEVENLABS VOICE..."

                // 2. Synthesize & Speak with ElevenLabs
                val voiceId = config.effectiveVoiceId
                val apiKey = config.apiKey.trim()

                if (config.isVoiceResponseEnabled && voiceId.isNotBlank() && apiKey.isNotBlank()) {
                    val synthResult = repository.synthesizeSpeech(
                        text = aiReply,
                        voiceId = voiceId,
                        apiKey = apiKey,
                        settings = VoiceSettingsRequest(
                            stability = config.stability,
                            similarityBoost = config.similarityBoost
                        )
                    )

                    synthResult.onSuccess { audioFile ->
                        audioService.playAudioFile(audioFile) {
                            onSpeechFinished()
                        }
                    }.onFailure {
                        // Fallback to Android TTS
                        audioService.speakUsingAndroidTts(aiReply, config.speakingSpeed) {
                            onSpeechFinished()
                        }
                    }
                } else if (config.isVoiceResponseEnabled) {
                    audioService.speakUsingAndroidTts(aiReply, config.speakingSpeed) {
                        onSpeechFinished()
                    }
                } else {
                    onSpeechFinished()
                }
            } catch (_: Exception) {
                onSpeechFinished()
            }
        }
    }

    private fun onSpeechFinished() {
        isProcessingQuery = false
        _assistantState.value = "LISTENING"
        if (isListeningLoopActive) {
            mainHandler.postDelayed({
                if (isListeningLoopActive && !isProcessingQuery) {
                    listenOnce()
                }
            }, 600)
        }
    }

    private fun stopForegroundListening() {
        isListeningLoopActive = false
        isProcessingQuery = false
        _isRunning.value = false
        _assistantState.value = "IDLE"

        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null

        audioService.stopAudio()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForegroundListening()
        audioService.release()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
