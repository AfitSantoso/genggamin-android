package com.example.genggaminmobile.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.genggaminmobile.MainActivity
import com.example.genggaminmobile.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class GenggaminFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("GenggaminFirebase", "--- Pesan FCM Diterima ---")
        
        // 1. Cek Data Payload (Biasanya dari Backend Java)
        if (remoteMessage.data.isNotEmpty()) {
            Log.d("GenggaminFirebase", "Data: ${remoteMessage.data}")
            val title = remoteMessage.data["title"] ?: "Genggamin"
            // Cek berbagai kemungkinan key untuk isi pesan
            val message = remoteMessage.data["message"] 
                ?: remoteMessage.data["body"] 
                ?: remoteMessage.data["content"] 
                ?: "Ada pesan baru"
            
            sendNotification(title, message)
        }

        // 2. Cek Notification Payload (Jika dikirim sebagai notifikasi standar)
        remoteMessage.notification?.let {
            Log.d("GenggaminFirebase", "Notification Body: ${it.body}")
            sendNotification(it.title ?: "Genggamin", it.body ?: "")
        }
    }

    override fun onNewToken(token: String) {
        Log.d("GenggaminFirebase", "Token Baru: $token")
    }

    private fun sendNotification(title: String, messageBody: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "genggamin_main_channel"

        // Buat Channel untuk Android O ke atas
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notifikasi Utama Genggamin",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Digunakan untuk notifikasi penting"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // GUNAKAN DRAWABLE, BUKAN MIPMAP
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)

        // Gunakan ID unik agar notifikasi tidak saling menimpa
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
        Log.d("GenggaminFirebase", "Notifikasi ditampilkan: $title")
    }
}
