package io.mikoshift.natsu.core.domain.repository

import io.mikoshift.natsu.core.model.SyncState
import kotlinx.coroutines.flow.StateFlow

interface SyncStatusRepository {
    val syncState: StateFlow<SyncState>

    fun setSyncing()

    fun setIdle()

    fun setFailed(message: String?)
}
