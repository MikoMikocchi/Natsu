package io.mikoshift.natsudroid.core.domain.usecase

import io.mikoshift.natsudroid.core.common.analytics.AnalyticsTracker
import io.mikoshift.natsudroid.core.domain.repository.DocumentRepository
import io.mikoshift.natsudroid.core.model.Document
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
