package io.mikoshift.natsu.core.domain.usecase

import io.mikoshift.natsu.core.domain.repository.DocumentRepository
import io.mikoshift.natsu.core.model.DocumentSearchResult
import javax.inject.Inject

class SearchDocumentsUseCase @Inject constructor(
    private val documentRepository: DocumentRepository,
) {
    suspend operator fun invoke(query: String): Result<List<DocumentSearchResult>> =
        documentRepository.search(query)
}
