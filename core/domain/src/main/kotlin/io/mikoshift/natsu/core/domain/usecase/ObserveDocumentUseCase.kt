package io.mikoshift.natsu.core.domain.usecase

import io.mikoshift.natsu.core.domain.repository.DocumentRepository
import io.mikoshift.natsu.core.model.Document
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveDocumentUseCase
@Inject
constructor(private val documentRepository: DocumentRepository) {
    operator fun invoke(id: String): Flow<Document?> = documentRepository.observeDocument(id)
}
