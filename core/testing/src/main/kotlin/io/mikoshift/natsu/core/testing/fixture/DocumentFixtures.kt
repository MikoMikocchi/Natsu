package io.mikoshift.natsu.core.testing.fixture

import io.mikoshift.natsu.core.model.Document
import io.mikoshift.natsu.core.model.DocumentStatus
import io.mikoshift.natsu.core.model.SourceFormat

object DocumentFixtures {
    fun document(
        id: String = "doc-1",
        title: String = "Sample Document",
        status: DocumentStatus = DocumentStatus.READY,
        sourceFormat: SourceFormat = SourceFormat.EPUB,
        updatedAtMs: Long = 1_000L,
    ) = Document(
        id = id,
        title = title,
        sourceFormat = sourceFormat,
        status = status,
        updatedAtMs = updatedAtMs,
    )
}
