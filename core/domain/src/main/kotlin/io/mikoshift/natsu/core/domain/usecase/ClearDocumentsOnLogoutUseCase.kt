package io.mikoshift.natsu.core.domain.usecase

import io.mikoshift.natsu.core.domain.repository.DocumentRepository
import javax.inject.Inject

class ClearDocumentsOnLogoutUseCase @Inject constructor(
    private val documentRepository: DocumentRepository,
) {
    suspend operator fun invoke() {
        documentRepository.clearOnLogout()
    }
}
