package io.mikoshift.natsudroid.core.domain.usecase

import io.mikoshift.natsudroid.core.domain.repository.DocumentRepository
import io.mikoshift.natsudroid.core.model.content.ReadingPosition
import javax.inject.Inject

class UpdateReadingProgressUseCase
@Inject
constructor(private val documentRepository: DocumentRepository) {
    suspend operator fun invoke(documentId: String, position: ReadingPosition): Result<Unit> =
        documentRepository.updateReadingProgress(documentId, position)
}
