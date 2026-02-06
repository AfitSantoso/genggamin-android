package com.example.genggaminmobile.core.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.genggaminmobile.MainActivity
import com.example.genggaminmobile.R
import com.example.genggaminmobile.data.local.datastore.TimerPreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Foreground Service untuk menampilkan countdown timer di notifikasi.
 * Service ini akan terus berjalan di background dan update notifikasi setiap detik.
 *
 * Keuntungan Foreground Service:
 * - User dapat melihat countdown meskipun membuka aplikasi lain
 * - Android tidak akan mematikan service ini karena ada notifikasi permanen
 */
@AndroidEntryPoint
class LoanTimerService : Service() {

    companion object {
        const val CHANNEL_ID = "loan_timer_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.genggaminmobile.ACTION_START_TIMER"
        const val ACTION_STOP = "com.example.genggaminmobile.ACTION_STOP_TIMER"

        const val EXTRA_LOAN_ID = "extra_loan_id"
        const val EXTRA_SUBMISSION_TIME = "extra_submission_time"

        /**
         * Helper function to start the timer service
         */
        fun startService(context: Context, loanId: Long, submissionTime: Long) {
            val intent = Intent(context, LoanTimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_LOAN_ID, loanId)
                putExtra(EXTRA_SUBMISSION_TIME, submissionTime)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Helper function to stop the timer service
         */
        fun stopService(context: Context) {
            val intent = Intent(context, LoanTimerService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    @Inject
    lateinit var timerPreferences: TimerPreferencesManager

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timerJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val loanId = intent.getLongExtra(EXTRA_LOAN_ID, -1L)
                val submissionTime = intent.getLongExtra(EXTRA_SUBMISSION_TIME, System.currentTimeMillis())

                if (loanId != -1L) {
                    // Save timer end time
                    timerPreferences.startTimer(loanId, submissionTime)

                    // Set AlarmManager as backup
                    scheduleAlarm(timerPreferences.getEndTime(), loanId)

                    // Start foreground with initial notification
                    startForeground(NOTIFICATION_ID, createNotification(timerPreferences.getRemainingSeconds()))

                    // Start timer update loop
                    startTimerLoop()
                }
            }
            ACTION_STOP -> {
                stopTimerAndCleanup()
            }
        }

        return START_STICKY // Restart service if killed
    }

    private fun startTimerLoop() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                val remainingSeconds = timerPreferences.getRemainingSeconds()

                if (remainingSeconds <= 0) {
                    // Timer selesai
                    showCompletionNotification()
                    stopTimerAndCleanup()
                    break
                }

                // Update notification
                val notification = createNotification(remainingSeconds)
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, notification)

                delay(1000L) // Update every second
            }
        }
    }

    private fun createNotification(remainingSeconds: Int): Notification {
        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        val timeText = String.format("%02d:%02d", minutes, seconds)

        // Intent to open app when notification is tapped
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Stop button intent
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, LoanTimerService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Pengajuan Pinjaman")
            .setContentText("Estimasi keputusan: $timeText")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Hentikan",
                stopIntent,
            )
            .build()
    }

    private fun showCompletionNotification() {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Proses Pengajuan")
            .setContentText("Pengajuan Anda sedang dalam tahap verifikasi akhir!")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun scheduleAlarm(endTime: Long, loanId: Long) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(this, TimerExpiredReceiver::class.java).apply {
            putExtra(EXTRA_LOAN_ID, loanId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            loanId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Use setAlarmClock for reliable alarm even in Doze mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setAlarmClock(
                            AlarmManager.AlarmClockInfo(endTime, pendingIntent),
                            pendingIntent,
                        )
                    } else {
                        // Fallback for devices without SCHEDULE_EXACT_ALARM permission
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            endTime,
                            pendingIntent,
                        )
                    }
                } else {
                    alarmManager.setAlarmClock(
                        AlarmManager.AlarmClockInfo(endTime, pendingIntent),
                        pendingIntent,
                    )
                }
            } catch (e: SecurityException) {
                // Fallback if exact alarm not allowed
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    endTime,
                    pendingIntent,
                )
            }
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, endTime, pendingIntent)
        }
    }

    private fun cancelAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val loanId = timerPreferences.getLoanId()

        if (loanId != -1L) {
            val intent = Intent(this, TimerExpiredReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                loanId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    private fun stopTimerAndCleanup() {
        timerJob?.cancel()
        cancelAlarm()
        timerPreferences.clearTimer()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Timer Pengajuan Pinjaman",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Menampilkan countdown waktu pengajuan pinjaman"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        timerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
}
