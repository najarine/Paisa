package com.paisa.najarine.notification

import android.content.Context

/**
 * Legacy compatibility wrapper for HourlyIslamicScheduler.
 * Delegates to DailyIslamicScheduler with drastically decreased frequency (1 in 24 hours).
 */
object HourlyIslamicScheduler {

    fun scheduleHourlySync(context: Context) {
        DailyIslamicScheduler.scheduleDailySync(context)
    }

    fun scheduleHourlyAlarm(context: Context) {
        DailyIslamicScheduler.cancelOldHourlyWorkers(context)
    }

    fun triggerImmediateSync(context: Context) {
        // No-op for immediate sync to prevent disruptive popups on app launch
        DailyIslamicScheduler.cancelOldHourlyWorkers(context)
    }
}
