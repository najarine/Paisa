package com.paisa.najarine.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.paisa.najarine.MainActivity

object PaisaNotificationManager {

    const val CHANNEL_PRAYER = "channel_prayer_adhan"
    const val CHANNEL_QURAN_HADITH = "channel_quran_hadith"
    const val CHANNEL_FINANCIAL = "channel_financial_bills"
    const val CHANNEL_COLLABORATION = "channel_collaboration"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val prayerChannel = NotificationChannel(
                CHANNEL_PRAYER,
                "Prayer Times & Adhan",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Automated prayer alerts and Adhan audio reminders"
                enableVibration(true)
            }

            val quranChannel = NotificationChannel(
                CHANNEL_QURAN_HADITH,
                "Quran & Hadith Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily Quranic reflections, authentic Hadiths, and Duas"
            }

            val financialChannel = NotificationChannel(
                CHANNEL_FINANCIAL,
                "Financial, Bills & Cards",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Bill payment alerts, credit card due dates, and budget thresholds"
            }

            val collabChannel = NotificationChannel(
                CHANNEL_COLLABORATION,
                "Partner & Workspace Collaboration",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Partner invitations, member transactions, and shared wallet activity"
            }

            notificationManager.createNotificationChannels(
                listOf(prayerChannel, quranChannel, financialChannel, collabChannel)
            )
        }
    }

    fun showNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        body: String,
        targetTab: Int = 0,
        targetScreen: String? = null
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_TARGET_TAB", targetTab)
            targetScreen?.let { putExtra("EXTRA_TARGET_SCREEN", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.paisa.najarine.R.drawable.ic_notification_p)
            .setColor(0xFF0D9488.toInt())
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(defaultSoundUri)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Missing POST_NOTIFICATIONS permission
        }
    }
}
