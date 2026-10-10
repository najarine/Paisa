package com.paisa.najarine.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object HourlyIslamicScheduler {
    private const val WORK_NAME_PERIODIC = "hourly_islamic_content_sync"
    private const val WORK_NAME_ONETIME = "hourly_islamic_content_onetime"

    fun scheduleHourlySync(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<HourlyIslamicSyncWorker>(
            1, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        scheduleHourlyAlarm(context)
    }

    fun scheduleHourlyAlarm(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return
            val intent = android.content.Intent(context, HourlyIslamicAlarmReceiver::class.java)
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                8821,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            // Trigger every hour on the top of the hour or +1 hour
            val nextTriggerMillis = System.currentTimeMillis() + java.util.concurrent.TimeUnit.HOURS.toMillis(1)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    nextTriggerMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    android.app.AlarmManager.RTC_WAKEUP,
                    nextTriggerMillis,
                    pendingIntent
                )
            }
        } catch (_: Exception) {}
    }

    fun triggerImmediateSync(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<HourlyIslamicSyncWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME_ONETIME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}
