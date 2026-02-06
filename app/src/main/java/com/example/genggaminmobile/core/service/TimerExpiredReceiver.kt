package com.example.genggaminmobile.core.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.genggaminmobile.MainActivity
import com.example.genggaminmobile.R

/**
 * BroadcastReceiver yang dipanggil oleh AlarmManager saat timer berakhir.
 *
 * Ini adalah "jaminan" bahwa meskipun Service mati karena memori penuh,
 * aplikasi tetap bisa menampilkan notifikasi saat waktu habis.
 *
 * AlarmManager akan "membangunkan" aplikasi dan menjalankan receiver ini.
 */
class TimerExpiredReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "timer_expired_channel"
        const val NOTIFICATION_ID = 2001
    }

    override fun onReceive(context: Context, intent: Intent) {
        val loanId = intent.getLongExtra(LoanTimerService.EXTRA_LOAN_ID, -1L)

        // Create notification channel for Android O+
        createNotificationChannel(context)

        // Show completion notification
        showNotification(context, loanId)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pengajuan Selesai",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Notifikasi saat waktu pengajuan berakhir"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(context: Context, loanId: Long) {
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                // Can pass loan ID to navigate to specific loan detail
                putExtra("navigate_to_loan", loanId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Play notification sound
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("⏰ Waktu Proses Selesai!")
            .setContentText("Pengajuan Anda sedang dalam tahap verifikasi akhir. Tap untuk melihat status.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Waktu estimasi 10 menit telah berakhir. Pengajuan Anda sedang dalam proses verifikasi akhir. Tap untuk melihat status terkini."),
            )
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
