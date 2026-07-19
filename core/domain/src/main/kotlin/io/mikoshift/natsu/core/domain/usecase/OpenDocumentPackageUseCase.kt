package io.mikoshift.natsu.core.domain.usecase

import io.mikoshift.natsu.core.domain.repository.DocumentPackageRepository
import io.mikoshift.natsu.core.model.content.DocumentPackage
import javax.inject.Inject

class OpenDocumentPackageUseCase
@Inject
constructor(
    private val documentPackageRepository: DocumentPackageRepository,
) {
    suspend operator fun invoke(documentId: String): Result<DocumentPackage> =
        documentPackageRepository.openPackage(documentId)
}
