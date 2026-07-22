package io.mikoshift.natsudroid.core.testing.repository

import io.mikoshift.natsudroid.core.domain.repository.SyncStatusRepository
import io.mikoshift.natsudroid.core.model.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeSyncStatusRepository : SyncStatusRepository {
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    override fun setSyncing() {
        _syncState.value = SyncState.Syncing
    }

    override fun setIdle() {
        _syncState.value = SyncState.Idle
    }

    override fun setFailed(message: String?) {
        _syncState.value = SyncState.Failed(message)
    }
}
