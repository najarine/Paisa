package com.paisa.najarine.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * DailyIslamicScheduler schedules the Daily Quran Ayah (morning after Fajr)
 * and Daily Hadith (evening after Isha) at two distinct separate times in 24 hours.
 * It prevents notification flooding and ensures each delivers at most 1 notification per day.
 */
object DailyIslamicScheduler {
    private const val TAG = "DailyIslamicScheduler"

    const val WORK_NAME_DAILY_QURAN = "daily_quran_morning_work"
    const val WORK_NAME_DAILY_HADITH = "daily_hadith_evening_work"

    private const val REQUEST_CODE_QURAN_ALARM = 9011
    private const val REQUEST_CODE_HADITH_ALARM = 9012

    fun scheduleDailySync(
        context: Context,
        fajrTimeStr: String? = null,
        ishaTimeStr: String? = null
    ) {
        cancelOldHourlyWorkers(context)
        scheduleDailyMorningQuran(context, fajrTimeStr)
        scheduleDailyEveningHadith(context, ishaTimeStr)
    }

    /**
     * Cancels legacy hourly sync workers and hourly repeating alarms
     */
    fun cancelOldHourlyWorkers(context: Context) {
        try {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork("hourly_islamic_content_sync")
            workManager.cancelUniqueWork("hourly_islamic_content_onetime")

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            val legacyIntent = Intent(context, HourlyIslamicAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                8821,
                legacyIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null && alarmManager != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling legacy hourly workers: ${e.message}")
        }
    }

    /**
     * Schedules the Quran Ayah refresh and notification in the morning after Fajr.
     * Offset: 20 minutes after Fajr (or 06:15 AM default).
     */
    fun scheduleDailyMorningQuran(context: Context, fajrTimeStr: String? = null) {
        try {
            val now = Calendar.getInstance()
            val targetCal = Calendar.getInstance()

            var hour = 6
            var minute = 15

            if (!fajrTimeStr.isNullOrBlank()) {
                val parts = fajrTimeStr.split(":")
                val fajrHour = parts.getOrNull(0)?.toIntOrNull()
                val fajrMinute = parts.getOrNull(1)?.toIntOrNull()
                if (fajrHour != null && fajrMinute != null) {
                    val rawMinutes = fajrHour * 60 + fajrMinute + 20 // 20 minutes after Fajr
                    hour = (rawMinutes / 60) % 24
                    minute = rawMinutes % 60
                }
            }

            targetCal.set(Calendar.HOUR_OF_DAY, hour)
            targetCal.set(Calendar.MINUTE, minute)
            targetCal.set(Calendar.SECOND, 0)
            targetCal.set(Calendar.MILLISECOND, 0)

            if (targetCal.before(now)) {
                // If morning time has already passed today, schedule for tomorrow
                targetCal.add(Calendar.DAY_OF_YEAR, 1)
            }

            val delayMillis = targetCal.timeInMillis - now.timeInMillis
            Log.d(TAG, "Scheduling Daily Morning Quran for: ${targetCal.time} (delay: ${delayMillis / 1000}s)")

            val workRequest = OneTimeWorkRequestBuilder<DailyQuranWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .addTag("daily_quran")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_DAILY_QURAN,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )

            // Also schedule exact AlarmManager for the morning time
            scheduleExactAlarm(context, targetCal.timeInMillis, REQUEST_CODE_QURAN_ALARM, DailyQuranAlarmReceiver::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling daily morning Quran: ${e.message}")
        }
    }

    /**
     * Schedules the Hadith refresh and notification in the evening after Isha.
     * Offset: 20 minutes after Isha (or 20:30 default).
     */
    fun scheduleDailyEveningHadith(context: Context, ishaTimeStr: String? = null) {
        try {
            val now = Calendar.getInstance()
            val targetCal = Calendar.getInstance()

            var hour = 20
            var minute = 30

            if (!ishaTimeStr.isNullOrBlank()) {
                val parts = ishaTimeStr.split(":")
                val ishaHour = parts.getOrNull(0)?.toIntOrNull()
                val ishaMinute = parts.getOrNull(1)?.toIntOrNull()
                if (ishaHour != null && ishaMinute != null) {
                    val rawMinutes = ishaHour * 60 + ishaMinute + 20 // 20 minutes after Isha
                    hour = (rawMinutes / 60) % 24
                    minute = rawMinutes % 60
                }
            }

            targetCal.set(Calendar.HOUR_OF_DAY, hour)
            targetCal.set(Calendar.MINUTE, minute)
            targetCal.set(Calendar.SECOND, 0)
            targetCal.set(Calendar.MILLISECOND, 0)

            if (targetCal.before(now)) {
                // If evening time has already passed today, schedule for tomorrow
                targetCal.add(Calendar.DAY_OF_YEAR, 1)
            }

            val delayMillis = targetCal.timeInMillis - now.timeInMillis
            Log.d(TAG, "Scheduling Daily Evening Hadith for: ${targetCal.time} (delay: ${delayMillis / 1000}s)")

            val workRequest = OneTimeWorkRequestBuilder<DailyHadithWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .addTag("daily_hadith")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_DAILY_HADITH,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )

            // Also schedule exact AlarmManager for the evening time
            scheduleExactAlarm(context, targetCal.timeInMillis, REQUEST_CODE_HADITH_ALARM, DailyHadithAlarmReceiver::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling daily evening Hadith: ${e.message}")
        }
    }

    private fun scheduleExactAlarm(
        context: Context,
        triggerAtMillis: Long,
        requestCode: Int,
        receiverClass: Class<*>
    ) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, receiverClass)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exact alarm scheduling fallback: ${e.message}")
        }
    }

    /**
     * Immediate trigger for manual test button in Quran settings
     */
    fun triggerTestQuranWork(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<DailyQuranWorker>()
            .addTag("test_quran")
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }

    /**
     * Immediate trigger for manual test button in Hadith settings
     */
    fun triggerTestHadithWork(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<DailyHadithWorker>()
            .addTag("test_hadith")
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
