package com.paisa.najarine.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * DailyQuranAlarmReceiver triggers the morning Quran refresh work.
 */
class DailyQuranAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("DailyQuranAlarm", "Morning Quran alarm triggered. Enqueuing DailyQuranWorker...")
        try {
            val workRequest = OneTimeWorkRequestBuilder<DailyQuranWorker>()
                .addTag("alarm_daily_quran")
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                DailyIslamicScheduler.WORK_NAME_DAILY_QURAN,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        } catch (e: Exception) {
            Log.e("DailyQuranAlarm", "Error enqueuing DailyQuranWorker: ${e.message}")
        }
    }
}
