package com.example.service

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.data.model.AudioPlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileInputStream
import java.util.Locale

class VoiceAudioService(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var androidTts: TextToSpeech? = null
    private var isTtsInitialized = false

    private val _playbackState = MutableStateFlow<AudioPlaybackState>(AudioPlaybackState.Idle)
    val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private val progressUpdater = object : Runnable {
        override fun run() {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    val current = player.currentPosition
                    val total = player.duration.coerceAtLeast(1)
                    val progress = (current.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    _playbackState.value = AudioPlaybackState.Playing(
                        progress = progress,
                        currentMs = current,
                        totalMs = total
                    )
                    handler.postDelayed(this, 100)
                }
            }
        }
    }

    init {
        initAndroidTts()
    }

    private fun initAndroidTts() {
        androidTts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true
                androidTts?.language = Locale.US
                androidTts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _playbackState.value = AudioPlaybackState.Playing(0.5f, 0, 1000)
                    }

                    override fun onDone(utteranceId: String?) {
                        _playbackState.value = AudioPlaybackState.Stopped
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _playbackState.value = AudioPlaybackState.Error("TTS fallback playback error")
                    }
                })
            }
        }
    }

    fun playAudioFile(file: File, onCompletion: (() -> Unit)? = null) {
        stopAudio()

        if (!file.exists() || file.length() == 0L) {
            _playbackState.value = AudioPlaybackState.Error("Audio file is empty or missing")
            return
        }

        try {
            _playbackState.value = AudioPlaybackState.Loading
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .build()
                )
                val fis = FileInputStream(file)
                setDataSource(fis.fd)
                fis.close()

                setOnPreparedListener { mp ->
                    mp.start()
                    val total = mp.duration.coerceAtLeast(1)
                    _playbackState.value = AudioPlaybackState.Playing(0f, 0, total)
                    handler.post(progressUpdater)
                }

                setOnCompletionListener {
                    handler.removeCallbacks(progressUpdater)
                    _playbackState.value = AudioPlaybackState.Stopped
                    onCompletion?.invoke()
                }

                setOnErrorListener { _, what, extra ->
                    handler.removeCallbacks(progressUpdater)
                    _playbackState.value = AudioPlaybackState.Error("Media playback failed ($what, $extra)")
                    true
                }

                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            _playbackState.value = AudioPlaybackState.Error("Failed to initialize audio: ${e.message}")
        }
    }

    fun speakUsingAndroidTts(text: String, speed: Float = 1.0f, onCompletion: (() -> Unit)? = null) {
        stopAudio()
        if (isTtsInitialized && androidTts != null) {
            _playbackState.value = AudioPlaybackState.Playing(0.5f, 0, 2000)
            androidTts?.setSpeechRate(speed)
            val utteranceId = "maiki_fallback_${System.currentTimeMillis()}"
            androidTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } else {
            _playbackState.value = AudioPlaybackState.Error("Android TextToSpeech engine not ready")
        }
    }

    fun pauseAudio() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                handler.removeCallbacks(progressUpdater)
                _playbackState.value = AudioPlaybackState.Paused
            }
        }
    }

    fun resumeAudio() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                handler.post(progressUpdater)
                val current = it.currentPosition
                val total = it.duration.coerceAtLeast(1)
                _playbackState.value = AudioPlaybackState.Playing(
                    progress = (current.toFloat() / total.toFloat()).coerceIn(0f, 1f),
                    currentMs = current,
                    totalMs = total
                )
            }
        }
    }

    fun stopAudio() {
        handler.removeCallbacks(progressUpdater)
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.stop()
                }
                it.reset()
                it.release()
            } catch (_: Exception) {}
        }
        mediaPlayer = null

        androidTts?.let {
            try {
                if (it.isSpeaking) {
                    it.stop()
                }
            } catch (_: Exception) {}
        }

        if (_playbackState.value !is AudioPlaybackState.Idle) {
            _playbackState.value = AudioPlaybackState.Stopped
        }
    }

    fun release() {
        stopAudio()
        try {
            androidTts?.shutdown()
        } catch (_: Exception) {}
        androidTts = null
    }
}
