package io.mikoshift.natsu.data.sync

import io.mikoshift.natsu.data.local.db.DocumentEntity
import io.mikoshift.natsu.data.remote.dto.DocumentResponse
import io.mikoshift.natsu.data.remote.dto.DocumentStatus
import io.mikoshift.natsu.data.remote.dto.SourceFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentMergerTest {

    @Test
    fun merge_whenLocalMissing_returnsServerEntity() {
        val server = sampleServer(updatedAtMs = 100)

        val merged = DocumentMerger.merge(server, local = null)

        assertEquals("doc-1", merged.id)
        assertEquals("Title", merged.title)
        assertFalse(merged.isDirty)
    }

    @Test
    fun merge_whenServerNewer_replacesLocalAndClearsDirty() {
        val local = sampleLocal(updatedAtMs = 100, isDirty = true, localPackagePath = "/old/path")
        val server = sampleServer(updatedAtMs = 200, title = "Server title")

        val merged = DocumentMerger.merge(server, local)

        assertEquals("Server title", merged.title)
        assertEquals(200, merged.updatedAtMs)
        assertFalse(merged.isDirty)
        assertNull(merged.localPackagePath)
    }

    @Test
    fun merge_whenServerNewer_keepsCachedPackageWhenShaMatches() {
        val local = sampleLocal(
            updatedAtMs = 100,
            packageSha256 = "abc",
            cachedPackageSha256 = "abc",
            localPackagePath = "/packages/doc-1.zip",
        )
        val server = sampleServer(updatedAtMs = 200, packageSha256 = "abc")

        val merged = DocumentMerger.merge(server, local)

        assertEquals("/packages/doc-1.zip", merged.localPackagePath)
        assertEquals("abc", merged.cachedPackageSha256)
    }

    @Test
    fun merge_whenLocalDirtyAndServerNotNewer_preservesDirtyFlag() {
        val local = sampleLocal(
            updatedAtMs = 300,
            isDirty = true,
            title = "Local title",
            charCount = 50,
        )
        val server = sampleServer(
            updatedAtMs = 200,
            charCount = 0,
            status = DocumentStatus.READY,
        )

        val merged = DocumentMerger.merge(server, local)

        assertTrue(merged.isDirty)
        assertEquals("Local title", merged.title)
        assertEquals(50, merged.charCount)
        assertEquals(DocumentStatus.READY, merged.status)
    }

    @Test
    fun merge_whenLocalCleanAndServerNotNewer_usesServerSnapshot() {
        val local = sampleLocal(updatedAtMs = 200, isDirty = false, title = "Local")
        val server = sampleServer(updatedAtMs = 200, title = "Server")

        val merged = DocumentMerger.merge(server, local)

        assertEquals("Server", merged.title)
        assertFalse(merged.isDirty)
    }

    private fun sampleServer(
        updatedAtMs: Long,
        title: String = "Title",
        packageSha256: String? = null,
        charCount: Int = 10,
        status: DocumentStatus = DocumentStatus.PENDING,
    ) = DocumentResponse(
        id = "doc-1",
        title = title,
        sourceFormat = SourceFormat.EPUB,
        status = status,
        updatedAtMs = updatedAtMs,
        charCount = charCount,
        packageSha256 = packageSha256,
    )

    private fun sampleLocal(
        updatedAtMs: Long,
        isDirty: Boolean = false,
        title: String = "Title",
        charCount: Int = 10,
        packageSha256: String? = null,
        cachedPackageSha256: String? = null,
        localPackagePath: String? = null,
    ) = DocumentEntity(
        id = "doc-1",
        title = title,
        sourceFormat = SourceFormat.EPUB,
        status = DocumentStatus.PENDING,
        charCount = charCount,
        updatedAtMs = updatedAtMs,
        isDirty = isDirty,
        packageSha256 = packageSha256,
        cachedPackageSha256 = cachedPackageSha256,
        localPackagePath = localPackagePath,
    )
}
