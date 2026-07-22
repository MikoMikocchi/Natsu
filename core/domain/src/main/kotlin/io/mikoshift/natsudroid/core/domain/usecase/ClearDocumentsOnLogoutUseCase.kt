package io.mikoshift.natsudroid.core.domain.usecase

import io.mikoshift.natsudroid.core.domain.repository.DocumentRepository
import javax.inject.Inject

class ClearDocumentsOnLogoutUseCase
@Inject
constructor(private val documentRepository: DocumentRepository) {
    suspend operator fun invoke() {
        documentRepository.clearOnLogout()
    }
}
