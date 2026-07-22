package io.mikoshift.natsudroid.core.domain.repository

import io.mikoshift.natsudroid.core.model.SyncState
import kotlinx.coroutines.flow.StateFlow

interface SyncStatusRepository {
    val syncState: StateFlow<SyncState>

    fun setSyncing()

    fun setIdle()

    fun setFailed(message: String?)
}
