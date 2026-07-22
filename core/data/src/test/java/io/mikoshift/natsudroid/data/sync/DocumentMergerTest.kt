package io.mikoshift.natsudroid.data.sync

import io.mikoshift.natsudroid.core.model.SourceFormat
import io.mikoshift.natsudroid.data.local.db.DocumentEntity
import io.mikoshift.natsudroid.data.local.db.ReadingProgressEntity
import io.mikoshift.natsudroid.data.remote.dto.DocumentResponse
import io.mikoshift.natsudroid.data.remote.dto.DocumentStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import io.mikoshift.natsudroid.data.remote.dto.SourceFormat as SourceFormatDto

class DocumentMergerTest {
    @Test
    fun merge_whenServerIsNewer_appliesFullSnapshot() {
        val server = sampleServer(updatedAtMs = 200L, title = "Server title")
        val local = sampleLocal(updatedAtMs = 100L, title = "Local title")

        val (document, progress) =
            DocumentMerger.merge(
                server = server,
                localDocument = local,
                localProgress = sampleProgress(updatedAtMs = 100L),
                hasPendingMetadata = false,
                hasPendingProgress = false,
            )

        assertEquals("Server title", document.title)
        assertEquals(200L, progress.updatedAtMs)
    }

    @Test
    fun merge_whenPendingMetadata_keepsLocalTitleButTakesServerPackageFields() {
        val server =
            sampleServer(
                updatedAtMs = 100L,
                title = "Server title",
                packageSha256 = "abc",
                charCount = 42,
            )
        val local = sampleLocal(updatedAtMs = 200L, title = "Local title")

        val (document, _) =
            DocumentMerger.merge(
                server = server,
                localDocument = local,
                localProgress = sampleProgress(updatedAtMs = 50L),
                hasPendingMetadata = true,
                hasPendingProgress = false,
            )

        assertEquals("Local title", document.title)
        assertEquals("abc", document.packageSha256)
        assertEquals(42, document.charCount)
    }

    @Test
    fun merge_whenPendingProgress_keepsLocalProgress() {
        val server = sampleServer(updatedAtMs = 100L, lastReadCharOffset = 10)
        val localProgress = sampleProgress(updatedAtMs = 200L, lastReadCharOffset = 99)

        val (_, progress) =
            DocumentMerger.merge(
                server = server,
                localDocument = sampleLocal(updatedAtMs = 50L, title = "Local title"),
                localProgress = localProgress,
                hasPendingMetadata = false,
                hasPendingProgress = true,
            )

        assertEquals(99, progress.lastReadCharOffset)
    }

    private fun sampleServer(
        updatedAtMs: Long,
        title: String = "Title",
        packageSha256: String? = null,
        charCount: Int = 0,
        lastReadCharOffset: Int = 0,
    ) = DocumentResponse(
        id = DOC_ID,
        title = title,
        sourceFormat = SourceFormatDto.EPUB,
        status = DocumentStatus.READY,
        charCount = charCount,
        lastReadCharOffset = lastReadCharOffset,
        updatedAtMs = updatedAtMs,
        packageSha256 = packageSha256,
        deleted = false,
    )

    private fun sampleLocal(updatedAtMs: Long, title: String) = DocumentEntity(
        id = DOC_ID,
        title = title,
        sourceFormat = SourceFormat.EPUB,
        status = io.mikoshift.natsudroid.core.model.DocumentStatus.READY,
        importedAt = 0,
        charCount = 0,
        updatedAtMs = updatedAtMs,
        deleted = false,
    )

    private fun sampleProgress(updatedAtMs: Long, lastReadCharOffset: Int = 0) = ReadingProgressEntity(
        documentId = DOC_ID,
        lastReadCharOffset = lastReadCharOffset,
        lastReadBlockIndex = 0,
        lastReadBlockCharOffset = 0,
        updatedAtMs = updatedAtMs,
        clientUpdatedAtMs = updatedAtMs,
    )

    private companion object {
        const val DOC_ID = "doc-1"
    }
}
