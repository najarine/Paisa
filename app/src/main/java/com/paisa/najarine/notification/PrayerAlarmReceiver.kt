package com.paisa.najarine.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.paisa.najarine.data.local.PaisaDatabase
import com.paisa.najarine.data.repository.IslamicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val prayerName = intent?.getStringExtra("PRAYER_NAME") ?: "Prayer"
        val isFajr = intent?.getBooleanExtra("IS_FAJR", false) ?: false
        val scheduledTime = intent?.getLongExtra("SCHEDULED_TIME", 0L) ?: 0L

        // Discard stale alarms delayed by more than 15 minutes (e.g. from device sleep/reboot)
        val now = System.currentTimeMillis()
        if (scheduledTime > 0L && (now - scheduledTime > 15 * 60 * 1000L)) {
            Log.w("PrayerAlarmReceiver", "Stale alarm for $prayerName received (${(now - scheduledTime) / 1000}s late). Discarding to prevent audio overlap.")
            return
        }

        // Respect Total Silence / DND None mode
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager != null) {
            val filter = notificationManager.currentInterruptionFilter
            if (filter == android.app.NotificationManager.INTERRUPTION_FILTER_NONE) {
                Log.d("PrayerAlarmReceiver", "Device in Total Silence (DND None). Suppressing audio.")
                return
            }
        }

        val title = if (isFajr) "Fajr Adhan — الصَّلَاةُ خَيْرٌ مِنَ النَّوْمِ" else "$prayerName Adhan — حي على الصلاة"
        val message = if (isFajr) {
            "Prayer is better than sleep. Time for Fajr prayer and beginning your fast."
        } else {
            "Time to pause worldly matters and offer $prayerName prayer."
        }

        // Show prominent notification
        PaisaNotificationManager.showNotification(
            context = context,
            channelId = PaisaNotificationManager.CHANNEL_PRAYER,
            notificationId = prayerName.hashCode(),
            title = title,
            body = message,
            targetTab = 3, // Islamic Tab
            targetScreen = "ADHAN_PRAYER"
        )

        // Play dedicated Adhan sound via Foreground Service if enabled by user
        if (AdhanPreferences.isAdhanEnabled(context)) {
            AdhanPlaybackService.start(context, prayerName, isFajr)
        }
    }
}

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED || intent?.action == Intent.ACTION_TIMEZONE_CHANGED) {
            Log.d("BootCompletedReceiver", "Device rebooted or timezone changed. Rescheduling exact prayer alarms...")
            val database = PaisaDatabase.getDatabase(context)
            val repo = IslamicRepository(database)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    PrayerNotificationWorker.refreshAndScheduleDailyPrayerAlerts(context)
                } catch (e: Exception) {
                    Log.e("BootCompletedReceiver", "Failed to reschedule WorkManager prayer alerts on boot: ${e.localizedMessage}")
                }
            }
        }
    }
}

object AdhanScheduler {

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun schedulePrayerAlarms(
        context: Context,
        fajr: String,
        dhuhr: String,
        asr: String,
        maghrib: String,
        isha: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val timings = listOf(
            Triple("Fajr", fajr, true),
            Triple("Dhuhr", dhuhr, false),
            Triple("Asr", asr, false),
            Triple("Maghrib", maghrib, false),
            Triple("Isha", isha, false)
        )

        val dateKey = SimpleDateFormat("yyyyMMdd", Locale.US).format(Calendar.getInstance().time)

        // Pre-cache active Adhans to local storage so playback works instantly even offline
        DefaultAdhanAudioProvider.preCacheUserSelections(context)

        timings.forEach { (name, timeStr, isFajr) ->
            val parts = timeStr.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: return@forEach
            val min = parts.getOrNull(1)?.toIntOrNull() ?: return@forEach

            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, min)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(Calendar.getInstance())) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                putExtra("PRAYER_NAME", name)
                putExtra("IS_FAJR", isFajr)
                putExtra("SCHEDULED_TIME", cal.timeInMillis)
            }

            // Unique scheduled-event ID per date + prayer to prevent duplicates
            val eventId = (dateKey.hashCode() * 31 + name.hashCode()) and 0x7FFFFFFF

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                eventId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                val showIntent = com.paisa.najarine.ui.AdhanAlarmActivity.createIntent(context, name, isFajr)
                val showPendingIntent = PendingIntent.getActivity(
                    context,
                    eventId + 1,
                    showIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // AlarmClockInfo guarantees exact firing regardless of Doze mode or battery restrictions
                val alarmClockInfo = AlarmManager.AlarmClockInfo(cal.timeInMillis, showPendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                Log.d("AdhanScheduler", "Scheduled exact AlarmClock for $name at ${cal.time} (millis=${cal.timeInMillis})")
            } catch (e: SecurityException) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        cal.timeInMillis,
                        pendingIntent
                    )
                } catch (_: Exception) {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
                }
            } catch (e: Exception) {
                Log.e("AdhanScheduler", "Error setting alarm clock: ${e.localizedMessage}")
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        cal.timeInMillis,
                        pendingIntent
                    )
                } catch (_: Exception) {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
                }
            }
        }
    }

    fun cancelAllAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val prayerNames = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
        val dateKey = SimpleDateFormat("yyyyMMdd", Locale.US).format(Calendar.getInstance().time)

        prayerNames.forEach { name ->
            val intent = Intent(context, PrayerAlarmReceiver::class.java)
            val eventId = (dateKey.hashCode() * 31 + name.hashCode()) and 0x7FFFFFFF
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                eventId,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }
}
