package io.mikoshift.natsu.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun scheduleImmediateSync() {
        val request = OneTimeWorkRequestBuilder<DocumentSyncWorker>()
            .setConstraints(SyncPolicy.immediateConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_DELAY_MINUTES, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_SYNC_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun scheduleOnAppStart() {
        val request = OneTimeWorkRequestBuilder<DocumentSyncWorker>()
            .setConstraints(SyncPolicy.immediateConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_DELAY_MINUTES, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_SYNC_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<DocumentSyncWorker>(
            PERIODIC_SYNC_INTERVAL_MINUTES,
            TimeUnit.MINUTES,
        )
            .setConstraints(SyncPolicy.periodicConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_DELAY_MINUTES, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private companion object {
        const val IMMEDIATE_SYNC_WORK_NAME = "document_sync_immediate"
        const val PERIODIC_SYNC_WORK_NAME = "document_sync_periodic"
        const val PERIODIC_SYNC_INTERVAL_MINUTES = 15L
        const val BACKOFF_DELAY_MINUTES = 1L
    }
}
