package com.paisa.najarine.notification

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

interface AdhanAudioProvider {
    fun playAdhan(context: Context, isFajr: Boolean = false)
    fun stopPlayback()
    fun isPlaying(): Boolean
}

object DefaultAdhanAudioProvider : AdhanAudioProvider {

    private const val TAG = "DefaultAdhanAudio"
    const val ALADHAN_REGULAR_ADHAN_URL = "https://cdn.aladhan.com/audio/adhans/a1.mp3" // Makkah / Mishary Rashid Alafasy
    const val ALADHAN_FAJR_ADHAN_URL = "https://cdn.aladhan.com/audio/adhans/a9.mp3"    // Fajr with 'as-salatu khayrun min an-nawm'
    const val ALADHAN_MADINAH_URL = "https://cdn.aladhan.com/audio/adhans/a2.mp3"       // Madinah
    const val ALADHAN_ALAQSA_URL = "https://cdn.aladhan.com/audio/adhans/a3.mp3"        // Al-Aqsa

    private val playbackLock = Any()
    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isPlayingState = MutableStateFlow(false)
    val isPlayingState: StateFlow<Boolean> = _isPlayingState.asStateFlow()

    // True ONLY when an actual scheduled prayer time triggers the alarm, NOT during in-screen testing or preview
    private val _isActualAlarmActiveState = MutableStateFlow(false)
    val isActualAlarmActiveState: StateFlow<Boolean> = _isActualAlarmActiveState.asStateFlow()

    // True when user is previewing audio inside settings (isolated from alarm notifications and call dialog)
    private val _isPreviewPlayingState = MutableStateFlow(false)
    val isPreviewPlayingState: StateFlow<Boolean> = _isPreviewPlayingState.asStateFlow()

    private val _activePrayerNameState = MutableStateFlow<String?>("আজান")
    val activePrayerNameState: StateFlow<String?> = _activePrayerNameState.asStateFlow()

    private val _currentPlayingUrlState = MutableStateFlow<String?>(null)
    val currentPlayingUrlState: StateFlow<String?> = _currentPlayingUrlState.asStateFlow()

    private val _isMutedState = MutableStateFlow(false)
    val isMutedState: StateFlow<Boolean> = _isMutedState.asStateFlow()

    private fun getAdhanStorageDir(context: Context): File {
        val dir = File(context.filesDir, "adhans")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    override fun playAdhan(context: Context, isFajr: Boolean) {
        playSelectedAdhan(context, isFajr, if (isFajr) "ফজর" else "নামাজ", isActualAlarm = true)
    }

    fun playSelectedAdhan(
        context: Context,
        isFajr: Boolean,
        prayerName: String = if (isFajr) "ফজর" else "নামাজ",
        isActualAlarm: Boolean = false
    ) {
        synchronized(playbackLock) {
            _isActualAlarmActiveState.value = isActualAlarm
            _isPreviewPlayingState.value = !isActualAlarm
            _activePrayerNameState.value = prayerName
            _isMutedState.value = false

            val audioUrl = if (isFajr) {
                AdhanPreferences.getSelectedFajrAdhanUrl(context)
            } else {
                AdhanPreferences.getSelectedAdhanUrl(context)
            }
            val selectedId = if (isFajr) {
                AdhanPreferences.getSelectedFajrAdhanId(context)
            } else {
                AdhanPreferences.getSelectedAdhanId(context)
            }
            val fileName = if (isFajr) "adhan_fajr_${selectedId}.mp3" else "adhan_regular_${selectedId}.mp3"

            playFromUrlOrCacheInternal(context, audioUrl, fileName, isAlarmUsage = isActualAlarm)
        }
    }

    fun preCacheUserSelections(context: Context) {
        scope.launch {
            try {
                val regularId = AdhanPreferences.getSelectedAdhanId(context)
                val regularUrl = AdhanPreferences.getSelectedAdhanUrl(context)
                val regularFile = File(getAdhanStorageDir(context), "adhan_regular_${regularId}.mp3")
                if (!regularFile.exists() || regularFile.length() < 50000) {
                    cacheAdhanFile(regularUrl, regularFile)
                }

                val fajrId = AdhanPreferences.getSelectedFajrAdhanId(context)
                val fajrUrl = AdhanPreferences.getSelectedFajrAdhanUrl(context)
                val fajrFile = File(getAdhanStorageDir(context), "adhan_fajr_${fajrId}.mp3")
                if (!fajrFile.exists() || fajrFile.length() < 50000) {
                    cacheAdhanFile(fajrUrl, fajrFile)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Pre-caching error: ${e.localizedMessage}")
            }
        }
    }

    fun preCacheAdhan(context: Context, id: String, url: String, isFajr: Boolean) {
        scope.launch {
            try {
                val fileName = if (isFajr) "adhan_fajr_${id}.mp3" else "adhan_regular_${id}.mp3"
                val file = File(getAdhanStorageDir(context), fileName)
                if (!file.exists() || file.length() < 50000) {
                    cacheAdhanFile(url, file)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Pre-caching adhan $id failed: ${e.localizedMessage}")
            }
        }
    }

    fun previewAdhan(context: Context, audioUrl: String, label: String) {
        synchronized(playbackLock) {
            _isActualAlarmActiveState.value = false
            _isPreviewPlayingState.value = true
            _activePrayerNameState.value = label
            _isMutedState.value = false

            val fileName = "aladhan_${label.lowercase().replace(" ", "_").replace("/", "_")}.mp3"
            playFromUrlOrCacheInternal(context, audioUrl, fileName, isAlarmUsage = false)
        }
    }

    fun playCustomAdhan(context: Context, audioUrl: String, label: String) {
        previewAdhan(context, audioUrl, label)
    }

    fun mutePlayback() {
        synchronized(playbackLock) {
            try {
                mediaPlayer?.setVolume(0f, 0f)
                _isMutedState.value = true
            } catch (e: Exception) {
                Log.w(TAG, "Failed to mute MediaPlayer: ${e.localizedMessage}")
            }
        }
    }

    private fun playFromUrlOrCacheInternal(
        context: Context,
        urlString: String,
        fileName: String,
        isAlarmUsage: Boolean
    ) {
        stopPlaybackInternal()
        _currentPlayingUrlState.value = urlString

        // Priority 1: Persistent internal storage (filesDir/adhans)
        val persistentFile = File(getAdhanStorageDir(context), fileName)
        // Priority 2: Cache directory fallback (cacheDir)
        val legacyCacheFile = File(context.cacheDir, fileName)

        val targetLocalFile = when {
            persistentFile.exists() && persistentFile.length() > 50000 -> persistentFile
            legacyCacheFile.exists() && legacyCacheFile.length() > 50000 -> legacyCacheFile
            else -> null
        }

        try {
            val audioUsage = if (isAlarmUsage) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_MEDIA
            val contentType = AudioAttributes.CONTENT_TYPE_MUSIC

            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(contentType)
                        .setUsage(audioUsage)
                        .build()
                )

                if (targetLocalFile != null) {
                    Log.d(TAG, "Playing Adhan from local storage: ${targetLocalFile.absolutePath} (${targetLocalFile.length()} bytes)")
                    setDataSource(context, Uri.fromFile(targetLocalFile))
                    prepare()
                    start()
                    _isPlayingState.value = true
                } else {
                    Log.d(TAG, "Streaming Adhan from Aladhan API: $urlString")
                    setDataSource(urlString)
                    setOnPreparedListener { mp ->
                        synchronized(playbackLock) {
                            if (mediaPlayer === mp) {
                                mp.start()
                                _isPlayingState.value = true
                            }
                        }
                    }
                    prepareAsync()

                    // Asynchronously cache into persistent storage for offline usage
                    scope.launch {
                        cacheAdhanFile(urlString, persistentFile)
                    }
                }

                setOnCompletionListener {
                    stopPlayback()
                }

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                    stopPlayback()
                    if (isAlarmUsage) {
                        fallbackPlay(context)
                    }
                    true
                }
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e(TAG, "Error playing Adhan: ${e.localizedMessage}", e)
            if (isAlarmUsage) {
                fallbackPlay(context)
            } else {
                stopPlayback()
            }
        }
    }

    private fun cacheAdhanFile(urlString: String, destination: File) {
        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 15000
            connection.connect()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val tempFile = File(destination.parentFile, "${destination.name}.tmp")
                connection.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (tempFile.length() > 50000) {
                    if (destination.exists()) {
                        destination.delete()
                    }
                    tempFile.renameTo(destination)
                    Log.d(TAG, "Cached Adhan audio successfully to persistent store (${destination.length()} bytes)")
                } else {
                    tempFile.delete()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Background Adhan caching failed: ${e.localizedMessage}")
        }
    }

    private fun fallbackPlay(context: Context) {
        synchronized(playbackLock) {
            try {
                val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

                mediaPlayer = MediaPlayer.create(context, ringtoneUri)?.apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .build()
                    )
                    start()
                    _isPlayingState.value = true
                    setOnCompletionListener { stopPlayback() }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fallback tone failed: ${e.localizedMessage}")
            }
        }
    }

    override fun stopPlayback() {
        synchronized(playbackLock) {
            stopPlaybackInternal()
        }
    }

    private fun stopPlaybackInternal() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.reset()
                it.release()
            }
        } catch (_: Exception) {
        } finally {
            mediaPlayer = null
            _isPlayingState.value = false
            _isActualAlarmActiveState.value = false
            _isPreviewPlayingState.value = false
            _currentPlayingUrlState.value = null
            _activePrayerNameState.value = null
            _isMutedState.value = false
        }
    }

    override fun isPlaying(): Boolean {
        synchronized(playbackLock) {
            return try {
                mediaPlayer?.isPlaying == true
            } catch (_: Exception) {
                false
            }
        }
    }
}
