package io.mikoshift.natsudroid.core.testing.fixture

import io.mikoshift.natsudroid.core.model.Document
import io.mikoshift.natsudroid.core.model.DocumentMetadata
import io.mikoshift.natsudroid.core.model.DocumentSearchMatch
import io.mikoshift.natsudroid.core.model.DocumentSearchResult
import io.mikoshift.natsudroid.core.model.DocumentStatus
import io.mikoshift.natsudroid.core.model.SourceFormat

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

    fun searchResult(
        id: String = "doc-1",
        title: String = "Sample Document",
        snippet: String = "...matching text...",
    ) = DocumentSearchResult(
        id = id,
        title = title,
        matches = listOf(DocumentSearchMatch(charOffset = 0, snippet = snippet)),
    )
}
