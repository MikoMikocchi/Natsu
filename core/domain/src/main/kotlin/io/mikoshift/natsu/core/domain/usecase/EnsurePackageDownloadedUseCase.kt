package io.mikoshift.natsu.core.domain.usecase

import io.mikoshift.natsu.core.domain.repository.DocumentRepository
import javax.inject.Inject

class EnsurePackageDownloadedUseCase
@Inject
constructor(private val documentRepository: DocumentRepository) {
    suspend operator fun invoke(documentId: String): Result<Unit> =
        documentRepository.ensurePackageDownloaded(documentId)
}
