package com.paisa.najarine.notification

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.paisa.najarine.data.local.PaisaDatabase
import com.paisa.najarine.data.repository.IslamicRepository
import com.paisa.najarine.data.repository.PrayerTimingsUi
import com.paisa.najarine.util.LocationHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * WorkManager-based Prayer Notification & Adhan Alert System.
 * Calculates exact prayer timings based on user's GPS/cached location and schedules
 * reliable push notifications using WorkManager.
 */
class PrayerNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prayerName = inputData.getString(KEY_PRAYER_NAME)
        val isFajr = inputData.getBoolean(KEY_IS_FAJR, false)
        val scheduledTime = inputData.getString(KEY_PRAYER_TIME) ?: ""
        val locationName = inputData.getString(KEY_LOCATION_NAME)

        return try {
            if (!prayerName.isNullOrBlank()) {
                // Triggered for a specific prayer time alert
                deliverPrayerAlert(prayerName, isFajr, scheduledTime, locationName)
            } else {
                // Daily / Periodic synchronization of location & calculation of today's prayer alert schedule
                refreshAndScheduleDailyPrayerAlerts(applicationContext)
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in PrayerNotificationWorker: ${e.message}", e)
            Result.retry()
        }
    }

    private fun deliverPrayerAlert(prayerName: String, isFajr: Boolean, timeStr: String, locationName: String?) {
        val context = applicationContext
        val isTest = inputData.getBoolean(KEY_IS_TEST, false)

        // Check if user has prayer notifications enabled (unless it's a manual test)
        if (!isTest && !AdhanPreferences.isPrayerNotificationsEnabled(context)) {
            Log.d(TAG, "Prayer notifications are disabled by user. Skipping notification delivery.")
            return
        }

        val locSuffix = if (!locationName.isNullOrBlank()) " — $locationName" else ""

        val title = when {
            isTest -> "🔔 পুশ নোটিফিকেশন টেস্ট (WorkManager)"
            isFajr -> "ফজরের আযান ও ওয়াক্ত — الصَّلَاةُ خَيْرٌ مِنَ النَّوْمِ"
            else -> "$prayerName নামাজের ওয়াক্ত — حي على الصلاة"
        }

        val message = when {
            isTest -> "WorkManager সফলভাবে অবস্থানভিত্তিক নামাজের অ্যালার্ট পুশ নোটিফিকেশন পাঠিয়েছে$locSuffix ($timeStr)।"
            isFajr -> "ঘুমের চেয়ে নামাজ উত্তম। ফজরের ওয়াক্ত শুরু হয়েছে ($timeStr)$locSuffix। সেহরি শেষ ও নামাজের সময়।"
            else -> "$prayerName নামাজের ওয়াক্ত হয়েছে ($timeStr)$locSuffix। পার্থিব ব্যস্ততা স্থগিত রেখে সালাত আদায় করুন।"
        }

        // Show push notification
        PaisaNotificationManager.showNotification(
            context = context,
            channelId = PaisaNotificationManager.CHANNEL_PRAYER,
            notificationId = (prayerName + timeStr).hashCode() and 0x7FFFFFFF,
            title = title,
            body = message,
            targetTab = 3, // Islamic Tab
            targetScreen = "ADHAN_PRAYER"
        )

        // Play dedicated Adhan sound via Foreground Service if enabled by user
        if (AdhanPreferences.isAdhanEnabled(context)) {
            AdhanPlaybackService.start(context, prayerName, isFajr)
        }
    }

    companion object {
        const val TAG = "PrayerNotificationWorker"
        const val KEY_PRAYER_NAME = "key_prayer_name"
        const val KEY_IS_FAJR = "key_is_fajr"
        const val KEY_PRAYER_TIME = "key_prayer_time"
        const val KEY_LOCATION_NAME = "key_location_name"
        const val KEY_IS_TEST = "key_is_test"

        private const val WORK_NAME_DAILY_CALCULATION = "prayer_daily_timings_sync"
        private const val WORK_NAME_PREFIX_ALERT = "prayer_alert_"

        /**
         * Enqueues periodic daily synchronization using WorkManager to keep location-aware
         * prayer times updated and schedule precise notification workers.
         */
        fun scheduleDailyPrayerWork(context: Context) {
            val dailyRequest = PeriodicWorkRequestBuilder<PrayerNotificationWorker>(
                12, TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_DAILY_CALCULATION,
                ExistingPeriodicWorkPolicy.KEEP,
                dailyRequest
            )

            // Also trigger immediate one-time calculation to schedule today's prayer push alerts right away
            triggerImmediateCalculation(context)
        }

        fun triggerImmediateCalculation(context: Context) {
            val immediateRequest = OneTimeWorkRequestBuilder<PrayerNotificationWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "${WORK_NAME_DAILY_CALCULATION}_immediate",
                ExistingWorkPolicy.REPLACE,
                immediateRequest
            )
        }

        /**
         * Resolves current location (or cached coordinates), calculates accurate prayer timings,
         * and schedules exact OneTimeWorkRequest push notification alerts with initial delays.
         */
        suspend fun refreshAndScheduleDailyPrayerAlerts(context: Context): PrayerTimingsUi {
            val database = PaisaDatabase.getDatabase(context)
            val repo = IslamicRepository(database)
            val selectedMadhab = AdhanPreferences.getSelectedMadhab(context)

            // 1. Try detecting real-time GPS location or fallback to cached location
            val locationInfo = LocationHelper.getCurrentLocation(context)
            val (latitude, longitude, locDisplayName) = if (locationInfo != null) {
                AdhanPreferences.saveLastKnownLocation(
                    context,
                    locationInfo.latitude,
                    locationInfo.longitude,
                    locationInfo.displayName
                )
                Triple(locationInfo.latitude, locationInfo.longitude, locationInfo.displayName)
            } else {
                AdhanPreferences.getLastKnownCoordinates(context) ?: Triple(23.8103, 90.4125, "ঢাকা, বাংলাদেশ")
            }

            // 2. Calculate accurate prayer times based on coordinates & selected madhab
            val timings = repo.getPrayerTimingsByCoordinates(
                latitude = latitude,
                longitude = longitude,
                madhab = selectedMadhab
            )

            // 3. Schedule WorkManager alerts for each prayer time
            schedulePrayerAlertWorkers(context, timings, locDisplayName)

            // 4. Also keep exact alarms updated if available
            try {
                AdhanScheduler.schedulePrayerAlarms(
                    context = context,
                    fajr = timings.fajr,
                    dhuhr = timings.dhuhr,
                    asr = timings.asr,
                    maghrib = timings.maghrib,
                    isha = timings.isha
                )
            } catch (_: Exception) {}

            return timings
        }

        /**
         * Schedules WorkManager push notification workers for all 5 daily prayers based on user's location
         */
        fun schedulePrayerAlertWorkers(
            context: Context,
            timings: PrayerTimingsUi,
            locationName: String? = null
        ) {
            val workManager = WorkManager.getInstance(context)

            if (!AdhanPreferences.isPrayerNotificationsEnabled(context)) {
                Log.d(TAG, "Prayer notifications are disabled; cancelling prayer workers.")
                cancelAllPrayerWork(context)
                return
            }

            val resolvedLocation = locationName ?: AdhanPreferences.getLastKnownCoordinates(context)?.third ?: "আপনার অবস্থান"

            val prayerList = listOf(
                PrayerAlertItem("fajr", "ফজর (Fajr)", timings.fajr, true),
                PrayerAlertItem("dhuhr", "যোহর (Dhuhr)", timings.dhuhr, false),
                PrayerAlertItem("asr", "আসর (Asr)", timings.asr, false),
                PrayerAlertItem("maghrib", "মাগরিব (Maghrib)", timings.maghrib, false),
                PrayerAlertItem("isha", "ইশা (Isha)", timings.isha, false)
            )

            val now = Calendar.getInstance()

            prayerList.forEach { prayer ->
                val parts = prayer.timeStr.split(":")
                val hour = parts.getOrNull(0)?.toIntOrNull() ?: return@forEach
                val minute = parts.getOrNull(1)?.toIntOrNull() ?: return@forEach

                val targetCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (before(now)) {
                        // If time has passed today, schedule for tomorrow
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }

                val delayMillis = targetCal.timeInMillis - now.timeInMillis
                if (delayMillis > 0) {
                    val inputData = Data.Builder()
                        .putString(KEY_PRAYER_NAME, prayer.prayerName)
                        .putBoolean(KEY_IS_FAJR, prayer.isFajr)
                        .putString(KEY_PRAYER_TIME, prayer.timeStr)
                        .putString(KEY_LOCATION_NAME, resolvedLocation)
                        .putBoolean(KEY_IS_TEST, false)
                        .build()

                    val alertWork = OneTimeWorkRequestBuilder<PrayerNotificationWorker>()
                        .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                        .setInputData(inputData)
                        .addTag("prayer_alert")
                        .build()

                    val uniqueWorkName = "$WORK_NAME_PREFIX_ALERT${prayer.code}"
                    workManager.enqueueUniqueWork(
                        uniqueWorkName,
                        ExistingWorkPolicy.REPLACE,
                        alertWork
                    )
                }
            }
        }

        /**
         * Triggers a test push notification via WorkManager with a 2-second delay to verify
         * the push notification system and user location integration.
         */
        fun triggerTestAlertViaWorkManager(
            context: Context,
            prayerName: String = "আসরের নামাজ (Asr)",
            scheduledTime: String = "০৪:২৫ PM"
        ) {
            val workManager = WorkManager.getInstance(context)
            val locationName = AdhanPreferences.getLastKnownCoordinates(context)?.third ?: "ঢাকা, বাংলাদেশ"

            val inputData = Data.Builder()
                .putString(KEY_PRAYER_NAME, prayerName)
                .putBoolean(KEY_IS_FAJR, false)
                .putString(KEY_PRAYER_TIME, scheduledTime)
                .putString(KEY_LOCATION_NAME, locationName)
                .putBoolean(KEY_IS_TEST, true)
                .build()

            val testWork = OneTimeWorkRequestBuilder<PrayerNotificationWorker>()
                .setInitialDelay(2, TimeUnit.SECONDS)
                .setInputData(inputData)
                .addTag("prayer_alert_test")
                .build()

            workManager.enqueueUniqueWork(
                "prayer_alert_test_immediate",
                ExistingWorkPolicy.REPLACE,
                testWork
            )
        }

        fun cancelAllPrayerWork(context: Context) {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelAllWorkByTag("prayer_alert")
            AdhanScheduler.cancelAllAlarms(context)
        }
    }

    private data class PrayerAlertItem(
        val code: String,
        val prayerName: String,
        val timeStr: String,
        val isFajr: Boolean
    )
}
