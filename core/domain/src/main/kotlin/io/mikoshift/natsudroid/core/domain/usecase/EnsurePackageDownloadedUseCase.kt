package io.mikoshift.natsudroid.core.domain.usecase

import io.mikoshift.natsudroid.core.domain.repository.DocumentRepository
import javax.inject.Inject

class EnsurePackageDownloadedUseCase
@Inject
constructor(private val documentRepository: DocumentRepository) {
    suspend operator fun invoke(documentId: String): Result<Unit> =
        documentRepository.ensurePackageDownloaded(documentId)
}
