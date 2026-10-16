package com.paisa.najarine.notification

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.paisa.najarine.data.local.HourlyHadithEntity
import com.paisa.najarine.data.local.PaisaDatabase
import com.paisa.najarine.data.remote.HadithApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * DailyHadithWorker runs once every 24 hours in the evening after Isha.
 * It randomly selects 1 authentic Hadith, updates the database for the daily Hadith section,
 * and delivers exactly 1 notification in 24 hours (when enabled).
 */
class DailyHadithWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val database = PaisaDatabase.getDatabase(applicationContext)
            val sharedPrefs = applicationContext.getSharedPreferences("paisa_notification_freq", Context.MODE_PRIVATE)
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val lastNotifDate = sharedPrefs.getString("last_daily_hadith_notif_date", "")

            // 1. Fetch 1 random authentic Hadith
            val hadithService = HadithApiService()
            val hadithItem = hadithService.fetchHourlyHadith(forceNext = true)
            val now = System.currentTimeMillis()

            // 2. Update database for the Daily Hadith section in the app
            val hadithEntity = HourlyHadithEntity(
                id = "latest_hourly_hadith",
                title = hadithItem.title,
                arabic = hadithItem.arabic,
                translation = hadithItem.translation,
                banglaTranslation = hadithItem.banglaTranslation,
                narrator = hadithItem.narrator,
                source = hadithItem.source,
                hadithNumber = hadithItem.hadithNumber,
                grade = hadithItem.grade,
                topic = hadithItem.topic,
                fetchedAtMillis = now
            )
            database.hourlyHadithDao().insertHadith(hadithEntity)

            // 3. Deliver exactly 1 notification in 24 hours (only in evening if not sent today)
            val isHadithNotifEnabled = AdhanPreferences.isDailyHadithNotificationEnabled(applicationContext)
            val alreadyNotifiedToday = lastNotifDate == todayStr

            if (isHadithNotifEnabled && !alreadyNotifiedToday) {
                PaisaNotificationManager.showNotification(
                    context = applicationContext,
                    channelId = PaisaNotificationManager.CHANNEL_QURAN_HADITH,
                    notificationId = 1001,
                    title = "আজকের নির্বাচিত বিশুদ্ধ হাদিস",
                    body = "${hadithItem.title}\n${hadithItem.banglaTranslation}\n(${hadithItem.source})",
                    targetTab = 3,
                    targetScreen = "HADITH"
                )
                sharedPrefs.edit {
                    putString("last_daily_hadith_notif_date", todayStr)
                    putLong("last_hadith_notification_time", now)
                }
                Log.d("DailyHadithWorker", "Daily Hadith evening notification successfully delivered for $todayStr.")
            } else {
                Log.d("DailyHadithWorker", "Hadith updated in database. Notification skipped (enabled: $isHadithNotifEnabled, alreadyNotifiedToday: $alreadyNotifiedToday).")
            }

            // Reschedule next evening's refresh
            DailyIslamicScheduler.scheduleDailyEveningHadith(applicationContext)

            Result.success()
        } catch (e: Exception) {
            Log.e("DailyHadithWorker", "Error in DailyHadithWorker: ${e.localizedMessage}", e)
            Result.retry()
        }
    }
}
