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

        // Play dedicated Adhan sound only if user enabled the Adhan toggle
        if (AdhanPreferences.isAdhanEnabled(context)) {
            DefaultAdhanAudioProvider.playAdhan(context, isFajr)
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
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    cal.timeInMillis,
                    pendingIntent
                )
            } catch (_: SecurityException) {
                // If exact alarms capability is missing, gracefully use standard wakeup
                alarmManager.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
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
