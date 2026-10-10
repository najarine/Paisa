package com.paisa.najarine.notification

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.paisa.najarine.MainActivity
import com.paisa.najarine.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AdhanPlaybackService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var playbackObserveJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_ADHAN
        if (action == ACTION_STOP_ADHAN) {
            stopAdhanPlayback()
            return START_NOT_STICKY
        }

        val prayerName = intent?.getStringExtra(EXTRA_PRAYER_NAME) ?: "নামাজ"
        val isFajr = intent?.getBooleanExtra(EXTRA_IS_FAJR, false) ?: false

        acquireWakeLock()
        val notification = createAdhanNotification(prayerName)
        startForeground(NOTIFICATION_ID, notification)

        // Start playback with prayer name
        DefaultAdhanAudioProvider.playSelectedAdhan(this, isFajr, prayerName)

        // Observe when playback finishes to stop service
        playbackObserveJob?.cancel()
        playbackObserveJob = CoroutineScope(Dispatchers.Main).launch {
            DefaultAdhanAudioProvider.isPlayingState.collectLatest { isPlaying ->
                if (!isPlaying) {
                    stopAdhanPlayback()
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Paisa:AdhanWakeLock")?.apply {
                acquire(5 * 60 * 1000L) // 5 minutes timeout max
            }
        } catch (_: Exception) {}
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {} finally {
            wakeLock = null
        }
    }

    private fun createAdhanNotification(prayerName: String): Notification {
        PaisaNotificationManager.createNotificationChannels(this)

        val stopIntent = Intent(this, AdhanActionReceiver::class.java).apply {
            action = ACTION_STOP_ADHAN
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            this,
            199,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_TARGET_TAB", 3)
            putExtra("EXTRA_TARGET_SCREEN", "ADHAN_PRAYER")
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            200,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, PaisaNotificationManager.CHANNEL_PRAYER)
            .setSmallIcon(R.drawable.ic_notification_p)
            .setColor(0xFF0D9488.toInt())
            .setContentTitle("🔊 $prayerName আযান চলছে...")
            .setContentText("ওয়াক্ত হয়েছে। আযান বন্ধ করতে 'আযান বন্ধ করুন' চাপুন।")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(openAppPendingIntent)
            .setFullScreenIntent(openAppPendingIntent, true)
            .addAction(
                android.R.drawable.ic_media_pause,
                "⛔ আযান বন্ধ করুন",
                stopPendingIntent
            )
            .build()
    }

    private fun stopAdhanPlayback() {
        playbackObserveJob?.cancel()
        playbackObserveJob = null
        DefaultAdhanAudioProvider.stopPlayback()
        releaseWakeLock()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAdhanPlayback()
    }

    companion object {
        const val NOTIFICATION_ID = 9001
        const val ACTION_START_ADHAN = "com.paisa.najarine.ACTION_START_ADHAN"
        const val ACTION_STOP_ADHAN = "com.paisa.najarine.ACTION_STOP_ADHAN"
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_IS_FAJR = "extra_is_fajr"

        fun start(context: Context, prayerName: String, isFajr: Boolean) {
            val intent = Intent(context, AdhanPlaybackService::class.java).apply {
                action = ACTION_START_ADHAN
                putExtra(EXTRA_PRAYER_NAME, prayerName)
                putExtra(EXTRA_IS_FAJR, isFajr)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // In case of restriction, fallback to direct audio provider
                DefaultAdhanAudioProvider.playSelectedAdhan(context, isFajr, prayerName)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AdhanPlaybackService::class.java).apply {
                action = ACTION_STOP_ADHAN
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
            DefaultAdhanAudioProvider.stopPlayback()
        }
    }
}

class AdhanActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == AdhanPlaybackService.ACTION_STOP_ADHAN) {
            AdhanPlaybackService.stop(context)
        }
    }
}
