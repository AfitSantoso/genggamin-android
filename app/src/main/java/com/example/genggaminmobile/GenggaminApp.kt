package com.example.genggaminmobile

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GenggaminApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        setupSyncWorker()
    }

    private fun setupSyncWorker() {
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()

        // Use UniqueWork to ensure we don't duplicate schedules
        // Keep to existing request if present
        val syncRequest = androidx.work.PeriodicWorkRequest.Builder(
            com.example.genggaminmobile.data.worker.SyncWorker::class.java,
            15,
            java.util.concurrent.TimeUnit.MINUTES, // Min interval
        )
            .setConstraints(constraints)
            .build()

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SyncWorker",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            syncRequest,
        )
    }
}
