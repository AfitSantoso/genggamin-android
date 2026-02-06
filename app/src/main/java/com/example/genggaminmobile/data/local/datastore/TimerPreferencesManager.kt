package com.example.genggaminmobile.data.local.datastore

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager untuk menyimpan END TIME dari timer.
 * Best Practice: Simpan timestamp "kapan berakhir" bukan "sisa detik".
 * Dengan cara ini, meskipun aplikasi di-force close atau HP mati,
 * waktu berakhir tetap sama.
 */
@Singleton
class TimerPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val PREF_NAME = "loan_timer_prefs"
        private const val KEY_END_TIME = "timer_end_time"
        private const val KEY_LOAN_ID = "timer_loan_id"
        private const val KEY_IS_TIMER_ACTIVE = "is_timer_active"

        // Timer duration: 10 minutes in milliseconds
        const val TIMER_DURATION_MS = 10 * 60 * 1000L
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Set timer with end time calculated from submission time + 10 minutes
     * @param loanId ID of the loan being tracked
     * @param submissionTime The time when loan was submitted (in milliseconds)
     */
    fun startTimer(loanId: Long, submissionTime: Long) {
        val endTime = submissionTime + TIMER_DURATION_MS
        prefs.edit().apply {
            putLong(KEY_END_TIME, endTime)
            putLong(KEY_LOAN_ID, loanId)
            putBoolean(KEY_IS_TIMER_ACTIVE, true)
            apply()
        }
    }

    /**
     * Get the end time of the timer
     * @return End time in milliseconds, or 0 if not set
     */
    fun getEndTime(): Long {
        return prefs.getLong(KEY_END_TIME, 0L)
    }

    /**
     * Get the loan ID associated with the timer
     */
    fun getLoanId(): Long {
        return prefs.getLong(KEY_LOAN_ID, -1L)
    }

    /**
     * Check if timer is currently active
     */
    fun isTimerActive(): Boolean {
        return prefs.getBoolean(KEY_IS_TIMER_ACTIVE, false)
    }

    /**
     * Get remaining time in seconds
     * @return Remaining seconds, or 0 if timer has expired
     */
    fun getRemainingSeconds(): Int {
        val endTime = getEndTime()
        if (endTime == 0L) return 0

        val remaining = endTime - System.currentTimeMillis()
        return if (remaining > 0) (remaining / 1000).toInt() else 0
    }

    /**
     * Stop and clear the timer
     */
    fun clearTimer() {
        prefs.edit().apply {
            remove(KEY_END_TIME)
            remove(KEY_LOAN_ID)
            putBoolean(KEY_IS_TIMER_ACTIVE, false)
            apply()
        }
    }

    /**
     * Check if timer has expired
     */
    fun isTimerExpired(): Boolean {
        val endTime = getEndTime()
        if (endTime == 0L) return true
        return System.currentTimeMillis() >= endTime
    }
}
