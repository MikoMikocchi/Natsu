package io.mikoshift.natsudroid.core.domain.usecase

import io.mikoshift.natsudroid.core.domain.repository.DocumentPackageRepository
import io.mikoshift.natsudroid.core.model.content.DocumentPackage
import javax.inject.Inject

class OpenDocumentPackageUseCase
@Inject
constructor(
    private val documentPackageRepository: DocumentPackageRepository,
) {
    suspend operator fun invoke(documentId: String): Result<DocumentPackage> =
        documentPackageRepository.openPackage(documentId)
}
