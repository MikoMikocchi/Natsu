package io.mikoshift.natsu.core.domain.usecase

import io.mikoshift.natsu.core.domain.repository.SyncStatusRepository
import io.mikoshift.natsu.core.model.SyncState
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ObserveSyncStatusUseCase
@Inject
constructor(private val syncStatusRepository: SyncStatusRepository) {
    operator fun invoke(): StateFlow<SyncState> = syncStatusRepository.syncState
}
