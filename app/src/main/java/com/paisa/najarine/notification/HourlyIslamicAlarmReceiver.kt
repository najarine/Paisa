package com.paisa.najarine.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class HourlyIslamicAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("HourlyIslamicAlarm", "Hourly alarm triggered in background. Processing content sync...")
        try {
            HourlyIslamicScheduler.triggerImmediateSync(context)
        } catch (e: Exception) {
            Log.e("HourlyIslamicAlarm", "Error triggering hourly sync: ${e.localizedMessage}")
        } finally {
            // Schedule next hour alarm
            HourlyIslamicScheduler.scheduleHourlyAlarm(context)
        }
    }
}
