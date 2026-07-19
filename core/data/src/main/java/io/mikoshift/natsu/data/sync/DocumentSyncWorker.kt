package io.mikoshift.natsu.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.mikoshift.natsu.core.domain.usecase.SyncDocumentsUseCase
import io.mikoshift.natsu.core.model.DocumentError

@HiltWorker
class DocumentSyncWorker
@AssistedInject
constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncDocuments: SyncDocumentsUseCase,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = syncDocuments().fold(
        onSuccess = { Result.success() },
        onFailure = { error ->
            when (error) {
                is DocumentError.Unauthorized -> Result.failure()
                else -> Result.retry()
            }
        },
    )
}
