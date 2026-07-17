package io.mikoshift.natsu.data.sync

import io.mikoshift.natsu.core.model.DocumentStatus
import io.mikoshift.natsu.core.model.SourceFormat
import io.mikoshift.natsu.data.local.db.DocumentEntity
import io.mikoshift.natsu.data.remote.dto.DocumentMetadataResponse
import io.mikoshift.natsu.data.remote.dto.DocumentStatus as DocumentStatusDto
import io.mikoshift.natsu.data.remote.dto.SourceFormat as SourceFormatDto
import org.junit.Assert.assertEquals
import org.junit.Test

class MetadataMergerTest {

    @Test
    fun merge_whenLocalMissing_returnsServerEntity() {
        val server = sampleServer(updatedAtMs = 100)

        val merged = MetadataMerger.merge(server, local = null, hasPendingOutbox = false)

        assertEquals("doc-1", merged.id)
        assertEquals("Title", merged.title)
    }

    @Test
    fun merge_whenServerNewer_replacesLocal() {
        val local = sampleLocal(updatedAtMs = 100, title = "Local title")
        val server = sampleServer(updatedAtMs = 200, title = "Server title")

        val merged = MetadataMerger.merge(server, local, hasPendingOutbox = true)

        assertEquals("Server title", merged.title)
        assertEquals(200, merged.updatedAtMs)
    }

    @Test
    fun merge_whenPendingOutboxAndServerNotNewer_preservesLocalMetadata() {
        val local = sampleLocal(
            updatedAtMs = 300,
            title = "Local title",
            charCount = 50,
        )
        val server = sampleServer(
            updatedAtMs = 200,
            charCount = 0,
            status = DocumentStatusDto.READY,
        )

        val merged = MetadataMerger.merge(server, local, hasPendingOutbox = true)

        assertEquals("Local title", merged.title)
        assertEquals(50, merged.charCount)
        assertEquals(DocumentStatus.READY, merged.status)
    }

    @Test
    fun merge_whenNoPendingOutboxAndServerNotNewer_usesServerSnapshot() {
        val local = sampleLocal(updatedAtMs = 200, title = "Local")
        val server = sampleServer(updatedAtMs = 200, title = "Server")

        val merged = MetadataMerger.merge(server, local, hasPendingOutbox = false)

        assertEquals("Server", merged.title)
    }

    private fun sampleServer(
        updatedAtMs: Long,
        title: String = "Title",
        charCount: Int = 10,
        status: DocumentStatusDto = DocumentStatusDto.PENDING,
    ) = DocumentMetadataResponse(
        id = "doc-1",
        title = title,
        sourceFormat = SourceFormatDto.EPUB,
        status = status,
        updatedAtMs = updatedAtMs,
        charCount = charCount,
    )

    private fun sampleLocal(
        updatedAtMs: Long,
        title: String = "Title",
        charCount: Int = 10,
    ) = DocumentEntity(
        id = "doc-1",
        title = title,
        sourceFormat = SourceFormat.EPUB,
        status = DocumentStatus.PENDING,
        charCount = charCount,
        updatedAtMs = updatedAtMs,
    )
}
