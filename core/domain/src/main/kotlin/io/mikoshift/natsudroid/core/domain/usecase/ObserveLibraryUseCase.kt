package io.mikoshift.natsudroid.core.domain.usecase

import io.mikoshift.natsudroid.core.domain.repository.DocumentRepository
import io.mikoshift.natsudroid.core.model.Document
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveLibraryUseCase
@Inject
constructor(private val documentRepository: DocumentRepository) {
    operator fun invoke(): Flow<List<Document>> = documentRepository.observeLibrary()
}
