package io.mikoshift.natsu.core.testing.fixture

import io.mikoshift.natsu.core.model.Document
import io.mikoshift.natsu.core.model.DocumentMetadata
import io.mikoshift.natsu.core.model.DocumentStatus
import io.mikoshift.natsu.core.model.SourceFormat

object DocumentFixtures {
    fun metadata(
        id: String = "doc-1",
        title: String = "Sample Document",
        status: DocumentStatus = DocumentStatus.READY,
        sourceFormat: SourceFormat = SourceFormat.EPUB,
        updatedAtMs: Long = 1_000L,
    ) = DocumentMetadata(
        id = id,
        title = title,
        sourceFormat = sourceFormat,
        status = status,
        updatedAtMs = updatedAtMs,
    )

    fun document(
        id: String = "doc-1",
        title: String = "Sample Document",
        status: DocumentStatus = DocumentStatus.READY,
        sourceFormat: SourceFormat = SourceFormat.EPUB,
        updatedAtMs: Long = 1_000L,
    ) = Document(metadata = metadata(id, title, status, sourceFormat, updatedAtMs))
}
