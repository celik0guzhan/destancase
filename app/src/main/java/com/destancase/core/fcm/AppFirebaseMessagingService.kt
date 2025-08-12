package com.destancase.core.fcm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.destancase.R

// Firebase Cloud Messaging servisi
class AppFirebaseMessagingService : FirebaseMessagingService() {

    // Yeni FCM token üretildiğinde çağrılır
    override fun onNewToken(token: String) {
        android.util.Log.d("FCM", "token=$token")
    }

    // Bildirim geldiğinde çağrılır
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "Yeni Bildirim"
        val body  = message.notification?.body  ?: message.data["body"]  ?: ""

        ensureChannel()

        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val nm = NotificationManagerCompat.from(this)
        if (Build.VERSION.SDK_INT < 33 ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            nm.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notif)
        }
    }

    // Android  için kanal oluşturma
    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val ch = NotificationChannel(CHANNEL_ID, "General", NotificationManager.IMPORTANCE_HIGH)
            mgr.createNotificationChannel(ch)
        }
    }

    companion object { private const val CHANNEL_ID = "general" }
}
