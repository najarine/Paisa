package com.paisa.najarine.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object AutoZakatScheduler {
    private const val WORK_NAME_PERIODIC = "auto_zakat_calculation_periodic"
    private const val WORK_NAME_ONETIME = "auto_zakat_calculation_onetime"

    /**
     * Schedules periodic automatic Zakat evaluation (runs daily in the background).
     */
    fun schedulePeriodicZakatCheck(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<AutoZakatCalculatorWorker>(
            24, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * Triggers an immediate one-time background calculation and report generation.
     */
    fun triggerImmediateCalculation(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<AutoZakatCalculatorWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME_ONETIME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}
