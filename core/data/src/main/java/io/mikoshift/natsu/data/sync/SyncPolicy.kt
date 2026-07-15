package io.mikoshift.natsu.data.sync

import androidx.work.Constraints
import androidx.work.NetworkType

object SyncPolicy {
    fun immediateConstraints(): Constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun periodicConstraints(): Constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.UNMETERED)
        .build()
}
