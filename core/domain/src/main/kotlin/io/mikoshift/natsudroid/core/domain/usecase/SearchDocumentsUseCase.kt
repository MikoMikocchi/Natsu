package io.mikoshift.natsudroid.core.domain.usecase

import io.mikoshift.natsudroid.core.domain.repository.DocumentRepository
import io.mikoshift.natsudroid.core.model.DocumentSearchResult
import javax.inject.Inject

class SearchDocumentsUseCase
@Inject
constructor(private val documentRepository: DocumentRepository) {
    suspend operator fun invoke(query: String): Result<List<DocumentSearchResult>> = documentRepository.search(query)
}
