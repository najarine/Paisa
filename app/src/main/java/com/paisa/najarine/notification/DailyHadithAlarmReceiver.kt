package com.paisa.najarine.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * DailyHadithAlarmReceiver triggers the evening Hadith refresh work.
 */
class DailyHadithAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("DailyHadithAlarm", "Evening Hadith alarm triggered. Enqueuing DailyHadithWorker...")
        try {
            val workRequest = OneTimeWorkRequestBuilder<DailyHadithWorker>()
                .addTag("alarm_daily_hadith")
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                DailyIslamicScheduler.WORK_NAME_DAILY_HADITH,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        } catch (e: Exception) {
            Log.e("DailyHadithAlarm", "Error enqueuing DailyHadithWorker: ${e.message}")
        }
    }
}
