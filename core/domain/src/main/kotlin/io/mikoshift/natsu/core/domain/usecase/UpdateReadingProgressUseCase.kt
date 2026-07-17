package io.mikoshift.natsu.core.domain.usecase

import io.mikoshift.natsu.core.domain.repository.DocumentRepository
import io.mikoshift.natsu.core.model.content.ReadingPosition
import javax.inject.Inject

class UpdateReadingProgressUseCase @Inject constructor(
    private val documentRepository: DocumentRepository,
) {
    suspend operator fun invoke(documentId: String, position: ReadingPosition): Result<Unit> =
        documentRepository.updateReadingProgress(documentId, position)
}
