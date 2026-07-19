package io.mikoshift.natsu.core.domain.usecase

import io.mikoshift.natsu.core.common.analytics.AnalyticsTracker
import io.mikoshift.natsu.core.domain.repository.DocumentRepository
import io.mikoshift.natsu.core.model.Document
import javax.inject.Inject

class ImportDocumentUseCase
@Inject
constructor(
    private val documentRepository: DocumentRepository,
    private val analyticsTracker: AnalyticsTracker,
) {
    suspend operator fun invoke(contentUri: String): Result<Document> =
        documentRepository.import(contentUri).also { result ->
            if (result.isSuccess) {
                analyticsTracker.track("import_started")
            }
        }
}
