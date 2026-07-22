package io.mikoshift.natsudroid.core.domain.usecase

import io.mikoshift.natsudroid.core.domain.repository.SyncStatusRepository
import io.mikoshift.natsudroid.core.model.SyncState
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ObserveSyncStatusUseCase
@Inject
constructor(private val syncStatusRepository: SyncStatusRepository) {
    operator fun invoke(): StateFlow<SyncState> = syncStatusRepository.syncState
}
