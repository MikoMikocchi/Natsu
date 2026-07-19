package io.mikoshift.natsu.data.repository

import io.mikoshift.natsu.core.domain.repository.SyncStatusRepository
import io.mikoshift.natsu.core.model.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncStatusRepositoryImpl
@Inject
constructor() : SyncStatusRepository {
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
