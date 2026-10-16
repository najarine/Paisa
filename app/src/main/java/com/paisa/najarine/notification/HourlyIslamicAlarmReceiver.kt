package com.paisa.najarine.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class HourlyIslamicAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("HourlyIslamicAlarm", "Legacy hourly alarm received. Cancelling legacy alarms and aligning with 24-hour daily schedule.")
        try {
            DailyIslamicScheduler.cancelOldHourlyWorkers(context)
            DailyIslamicScheduler.scheduleDailySync(context)
        } catch (e: Exception) {
            Log.e("HourlyIslamicAlarm", "Error handling legacy alarm: ${e.localizedMessage}")
        }
    }
}
