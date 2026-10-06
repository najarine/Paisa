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
