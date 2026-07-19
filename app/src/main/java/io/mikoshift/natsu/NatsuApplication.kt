package io.mikoshift.natsu

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import io.mikoshift.natsu.data.sync.SyncScheduler
import javax.inject.Inject

@HiltAndroidApp
class NatsuApplication :
    Application(),
    Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var syncScheduler: SyncScheduler

    override fun onCreate() {
        super.onCreate()
        syncScheduler.scheduleOnAppStart()
        syncScheduler.schedulePeriodic()
    }

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory)
                .build()
}
