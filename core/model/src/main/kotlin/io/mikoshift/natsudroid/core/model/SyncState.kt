package io.mikoshift.natsudroid.core.model

sealed interface SyncState {
    data object Idle : SyncState

    data object Syncing : SyncState

    data class Failed(val message: String? = null) : SyncState
}
