package io.mikoshift.natsu.core.domain.usecase

import io.mikoshift.natsu.core.domain.repository.SyncStatusRepository
import io.mikoshift.natsu.core.model.SyncState
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

class ObserveSyncStatusUseCase @Inject constructor(
    private val syncStatusRepository: SyncStatusRepository,
) {
    operator fun invoke(): StateFlow<SyncState> = syncStatusRepository.syncState
}
