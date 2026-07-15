package io.mikoshift.natsu.core.domain.usecase

import io.mikoshift.natsu.core.domain.repository.DocumentRepository
import io.mikoshift.natsu.core.model.Document
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveLibraryUseCase @Inject constructor(
    private val documentRepository: DocumentRepository,
) {
    operator fun invoke(): Flow<List<Document>> = documentRepository.observeLibrary()
}
