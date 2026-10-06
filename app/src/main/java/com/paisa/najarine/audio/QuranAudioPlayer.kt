package com.paisa.najarine.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

sealed class QuranPlaybackState {
    object Idle : QuranPlaybackState()
    data class Buffering(val surahNumber: Int, val ayahInSurah: Int, val reciter: QuranReciter) : QuranPlaybackState()
    data class Playing(val surahNumber: Int, val ayahInSurah: Int, val reciter: QuranReciter) : QuranPlaybackState()
    data class Paused(val surahNumber: Int, val ayahInSurah: Int, val reciter: QuranReciter) : QuranPlaybackState()
    data class Error(val message: String) : QuranPlaybackState()
}

class QuranAudioPlayer(private val context: Context) {
    companion object {
        private const val TAG = "QuranAudioPlayer"
    }

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _playbackState = MutableStateFlow<QuranPlaybackState>(QuranPlaybackState.Idle)
    val playbackState: StateFlow<QuranPlaybackState> = _playbackState.asStateFlow()

    private val _currentSurahNumber = MutableStateFlow(0)
    val currentSurahNumber: StateFlow<Int> = _currentSurahNumber.asStateFlow()

    private val _currentAyahInSurah = MutableStateFlow(0)
    val currentAyahInSurah: StateFlow<Int> = _currentAyahInSurah.asStateFlow()

    private val _currentReciter = MutableStateFlow(QuranReciters.DEFAULT_RECITER)
    val currentReciter: StateFlow<QuranReciter> = _currentReciter.asStateFlow()

    private val _autoPlayNext = MutableStateFlow(true)
    val autoPlayNext: StateFlow<Boolean> = _autoPlayNext.asStateFlow()

    var onAyahCompletedListener: ((surahNumber: Int, ayahInSurah: Int) -> Unit)? = null

    fun setReciter(reciter: QuranReciter) {
        _currentReciter.value = reciter
    }

    fun setAutoPlayNext(enabled: Boolean) {
        _autoPlayNext.value = enabled
    }

    fun playAyah(
        surahNumber: Int,
        ayahInSurah: Int,
        globalNumber: Int,
        reciter: QuranReciter = _currentReciter.value
    ) {
        _currentSurahNumber.value = surahNumber
        _currentAyahInSurah.value = ayahInSurah
        _currentReciter.value = reciter
        _playbackState.value = QuranPlaybackState.Buffering(surahNumber, ayahInSurah, reciter)

        stopAndResetPlayer()

        val surahStr = String.format(Locale.US, "%03d", surahNumber)
        val ayahStr = String.format(Locale.US, "%03d", ayahInSurah)

        val primaryUrl = "https://cdn.islamic.network/quran/audio/128/${reciter.networkIdentifier}/$globalNumber.mp3"
        val fallbackUrl = "https://everyayah.com/data/${reciter.everyAyahFolder}/${surahStr}${ayahStr}.mp3"

        attemptPlayUrl(primaryUrl, fallbackUrl, surahNumber, ayahInSurah, reciter)
    }

    private fun attemptPlayUrl(
        primaryUrl: String,
        fallbackUrl: String,
        surahNumber: Int,
        ayahInSurah: Int,
        reciter: QuranReciter,
        isFallbackAttempt: Boolean = false
    ) {
        val targetUrl = if (isFallbackAttempt) fallbackUrl else primaryUrl

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setWakeMode(context.applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                setDataSource(targetUrl)

                setOnPreparedListener { mp ->
                    try {
                        mp.start()
                        _playbackState.value = QuranPlaybackState.Playing(surahNumber, ayahInSurah, reciter)
                        Log.d(TAG, "Playing Ayah $surahNumber:$ayahInSurah with reciter ${reciter.nameEnglish}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error starting playback: ${e.localizedMessage}")
                        _playbackState.value = QuranPlaybackState.Error("অডিও প্লে করতে ব্যর্থ হয়েছে")
                    }
                }

                setOnCompletionListener {
                    Log.d(TAG, "Completed Ayah $surahNumber:$ayahInSurah")
                    _playbackState.value = QuranPlaybackState.Idle
                    onAyahCompletedListener?.invoke(surahNumber, ayahInSurah)
                }

                setOnErrorListener { _, what, extra ->
                    Log.w(TAG, "MediaPlayer error: what=$what, extra=$extra on URL: $targetUrl")
                    if (!isFallbackAttempt) {
                        Log.d(TAG, "Retrying with fallback URL: $fallbackUrl")
                        stopAndResetPlayer()
                        attemptPlayUrl(primaryUrl, fallbackUrl, surahNumber, ayahInSurah, reciter, isFallbackAttempt = true)
                    } else {
                        _playbackState.value = QuranPlaybackState.Error("আয়াত তিলাওয়াত অডিও লোড করা যায়নি")
                    }
                    true
                }

                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e(TAG, "Exception preparing MediaPlayer: ${e.localizedMessage}")
            if (!isFallbackAttempt) {
                attemptPlayUrl(primaryUrl, fallbackUrl, surahNumber, ayahInSurah, reciter, isFallbackAttempt = true)
            } else {
                _playbackState.value = QuranPlaybackState.Error("সংযোগ ত্রুটি: অডিও লোড করা সম্ভব হয়নি")
            }
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: return
        val currentSurah = _currentSurahNumber.value
        val currentAyah = _currentAyahInSurah.value
        val reciter = _currentReciter.value

        try {
            if (player.isPlaying) {
                player.pause()
                _playbackState.value = QuranPlaybackState.Paused(currentSurah, currentAyah, reciter)
            } else {
                player.start()
                _playbackState.value = QuranPlaybackState.Playing(currentSurah, currentAyah, reciter)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling play/pause: ${e.localizedMessage}")
        }
    }

    fun pause() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    _playbackState.value = QuranPlaybackState.Paused(
                        _currentSurahNumber.value,
                        _currentAyahInSurah.value,
                        _currentReciter.value
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing: ${e.localizedMessage}")
        }
    }

    fun resume() {
        try {
            mediaPlayer?.let {
                it.start()
                _playbackState.value = QuranPlaybackState.Playing(
                    _currentSurahNumber.value,
                    _currentAyahInSurah.value,
                    _currentReciter.value
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming: ${e.localizedMessage}")
        }
    }

    fun stop() {
        stopAndResetPlayer()
        _playbackState.value = QuranPlaybackState.Idle
    }

    private fun stopAndResetPlayer() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.reset()
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing player: ${e.localizedMessage}")
        } finally {
            mediaPlayer = null
        }
    }

    fun release() {
        stopAndResetPlayer()
        _playbackState.value = QuranPlaybackState.Idle
    }
}
