package io.mikoshift.natsudroid.core.domain.usecase

import io.mikoshift.natsudroid.core.common.analytics.AnalyticsTracker
import io.mikoshift.natsudroid.core.domain.repository.AuthRepository
import io.mikoshift.natsudroid.core.domain.repository.DocumentRepository
import io.mikoshift.natsudroid.core.domain.repository.SyncStatusRepository
import io.mikoshift.natsudroid.core.model.DocumentError
import javax.inject.Inject

class SyncDocumentsUseCase
@Inject
constructor(
    private val authRepository: AuthRepository,
    private val documentRepository: DocumentRepository,
    private val syncStatusRepository: SyncStatusRepository,
    private val analyticsTracker: AnalyticsTracker,
) {
    suspend operator fun invoke(): Result<Unit> {
        if (authRepository.currentSession.value == null) {
            return Result.success(Unit)
        }

        syncStatusRepository.setSyncing()
        return documentRepository.sync().also { result ->
            result.fold(
                onSuccess = { syncStatusRepository.setIdle() },
                onFailure = { error ->
                    syncStatusRepository.setFailed(error.toUserMessage())
                    analyticsTracker.track("sync_failed")
                },
            )
        }
    }

    private fun Throwable.toUserMessage(): String? = when (this) {
        is DocumentError.NetworkFailure -> "Network error, please try again"
        is DocumentError.Unauthorized -> "Session expired, please sign in again"
        is DocumentError.ValidationError -> fieldErrors.values.flatten().joinToString(", ")
        is DocumentError.ImportFailed -> reason ?: "Import failed"
        is DocumentError.Unknown -> errorMessage
        else -> message
    }
}
