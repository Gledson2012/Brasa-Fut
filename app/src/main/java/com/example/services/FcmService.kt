package com.example.services

import com.example.utils.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FcmService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val notificationHelper = NotificationHelper(this)

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            val title = remoteMessage.data["title"] ?: "BrasaFut"
            val body = remoteMessage.data["body"] ?: "Você tem uma nova notificação!"
            val matchIdStr = remoteMessage.data["matchId"]
            val matchId = matchIdStr?.toIntOrNull() ?: System.currentTimeMillis().toInt()

            notificationHelper.showNotification(
                title = title,
                message = body,
                notificationId = matchId
            )
        } 
        
        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            val title = it.title ?: "BrasaFut"
            val body = it.body ?: "Você tem uma nova notificação!"
            
            notificationHelper.showNotification(
                title = title,
                message = body,
                notificationId = System.currentTimeMillis().toInt()
            )
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // If we needed to send this token to our custom backend, we would do it here.
    }
}
