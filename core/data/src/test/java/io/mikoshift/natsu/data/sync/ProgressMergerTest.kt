package io.mikoshift.natsu.data.sync

import io.mikoshift.natsu.data.local.db.ReadingProgressEntity
import io.mikoshift.natsu.data.remote.dto.ReadingProgressResponse
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressMergerTest {

    @Test
    fun merge_whenLocalMissing_returnsServerEntity() {
        val server = sampleServer(updatedAtMs = 100, charOffset = 42)

        val merged = ProgressMerger.merge(server, local = null, hasPendingOutbox = false)

        assertEquals(42, merged.lastReadCharOffset)
        assertEquals(100, merged.updatedAtMs)
    }

    @Test
    fun merge_whenServerNewer_usesServerProgress() {
        val local = sampleLocal(updatedAtMs = 100, charOffset = 10)
        val server = sampleServer(updatedAtMs = 200, charOffset = 99)

        val merged = ProgressMerger.merge(server, local, hasPendingOutbox = true)

        assertEquals(99, merged.lastReadCharOffset)
        assertEquals(200, merged.updatedAtMs)
    }

    @Test
    fun merge_whenPendingOutboxAndServerNotNewer_keepsLocalProgress() {
        val local = sampleLocal(updatedAtMs = 300, charOffset = 50)
        val server = sampleServer(updatedAtMs = 200, charOffset = 10)

        val merged = ProgressMerger.merge(server, local, hasPendingOutbox = true)

        assertEquals(50, merged.lastReadCharOffset)
        assertEquals(300, merged.updatedAtMs)
    }

    @Test
    fun merge_whenNoPendingOutboxAndServerNotNewer_usesServerSnapshot() {
        val local = sampleLocal(updatedAtMs = 200, charOffset = 10)
        val server = sampleServer(updatedAtMs = 200, charOffset = 10)

        val merged = ProgressMerger.merge(server, local, hasPendingOutbox = false)

        assertEquals(10, merged.lastReadCharOffset)
    }

    private fun sampleServer(updatedAtMs: Long, charOffset: Int) = ReadingProgressResponse(
        documentId = "doc-1",
        lastReadCharOffset = charOffset,
        updatedAtMs = updatedAtMs,
    )

    private fun sampleLocal(updatedAtMs: Long, charOffset: Int) = ReadingProgressEntity(
        documentId = "doc-1",
        lastReadCharOffset = charOffset,
        updatedAtMs = updatedAtMs,
    )
}
