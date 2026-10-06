package com.paisa.najarine.notification

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PaisaFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        try {
            val dbId = getString(com.paisa.najarine.R.string.firestore_database_id)
            FirebaseFirestore.getInstance(dbId).collection("users").document(uid).set(
                hashMapOf(
                    "fcmToken" to token,
                    "tokenUpdatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
        } catch (_: Exception) {
            // Ignore offline failure
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Paisa Notification"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "New financial update in your workspace."

        val type = remoteMessage.data["type"] ?: "SYSTEM"
        val targetTab = when (type) {
            "PARTNER_INVITE", "COLLABORATION" -> 4 // More / Workspace Members
            "TRANSACTION", "BUDGET" -> 2 // Transactions / Ledger
            "PRAYER" -> 3 // Islamic
            else -> 0 // Home
        }

        val channel = when (type) {
            "PARTNER_INVITE", "COLLABORATION" -> PaisaNotificationManager.CHANNEL_COLLABORATION
            "TRANSACTION", "BUDGET" -> PaisaNotificationManager.CHANNEL_FINANCIAL
            "PRAYER" -> PaisaNotificationManager.CHANNEL_PRAYER
            else -> PaisaNotificationManager.CHANNEL_PRAYER
        }

        PaisaNotificationManager.showNotification(
            context = applicationContext,
            channelId = channel,
            notificationId = (System.currentTimeMillis() % 100000).toInt(),
            title = title,
            body = body,
            targetTab = targetTab
        )
    }
}
