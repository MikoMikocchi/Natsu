package io.mikoshift.natsudroid.data.sync

import io.mikoshift.natsudroid.core.model.DocumentError
import io.mikoshift.natsudroid.core.model.DocumentStatus
import io.mikoshift.natsudroid.core.model.SourceFormat
import io.mikoshift.natsudroid.data.local.PackageFileStore
import io.mikoshift.natsudroid.data.local.SyncCursorStore
import io.mikoshift.natsudroid.data.local.SyncOutboxStore
import io.mikoshift.natsudroid.data.local.db.DocumentCacheDao
import io.mikoshift.natsudroid.data.local.db.DocumentDao
import io.mikoshift.natsudroid.data.local.db.DocumentEntity
import io.mikoshift.natsudroid.data.local.db.ReadingProgressDao
import io.mikoshift.natsudroid.data.local.db.SyncEntityType
import io.mikoshift.natsudroid.data.local.db.SyncOutboxDao
import io.mikoshift.natsudroid.data.local.db.SyncOutboxEntity
import io.mikoshift.natsudroid.data.local.db.SyncOutboxStatus
import io.mikoshift.natsudroid.data.remote.DocumentApi
import io.mikoshift.natsudroid.data.remote.dto.DocumentIndexResponse
import io.mikoshift.natsudroid.data.remote.dto.DocumentResponse
import io.mikoshift.natsudroid.data.remote.dto.DocumentSyncRequest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import io.mikoshift.natsudroid.data.remote.dto.DocumentStatus as DocumentStatusDto
import io.mikoshift.natsudroid.data.remote.dto.SourceFormat as SourceFormatDto

class DocumentSyncEngineTest {
    private lateinit var documentApi: DocumentApi
    private lateinit var documentDao: DocumentDao
    private lateinit var readingProgressDao: ReadingProgressDao
    private lateinit var documentCacheDao: DocumentCacheDao
    private lateinit var syncOutboxDao: SyncOutboxDao
    private lateinit var syncOutboxStore: SyncOutboxStore
    private lateinit var syncCursorStore: SyncCursorStore
    private lateinit var packageFileStore: PackageFileStore
    private lateinit var packageDownloadService: PackageDownloadService
    private lateinit var engine: DocumentSyncEngine

    @Before
    fun setUp() {
        documentApi = mockk()
        documentDao = mockk(relaxed = true)
        readingProgressDao = mockk(relaxed = true)
        documentCacheDao = mockk(relaxed = true)
        syncOutboxDao = mockk(relaxed = true)
        syncOutboxStore = mockk(relaxed = true)
        syncCursorStore = mockk(relaxed = true)
        packageFileStore = mockk(relaxed = true)
        packageDownloadService = mockk(relaxed = true)

        engine =
            DocumentSyncEngine(
                documentApi = documentApi,
                documentDao = documentDao,
                readingProgressDao = readingProgressDao,
                documentCacheDao = documentCacheDao,
                syncOutboxDao = syncOutboxDao,
                syncOutboxStore = syncOutboxStore,
                syncCursorStore = syncCursorStore,
                packageFileStore = packageFileStore,
                packageDownloadService = packageDownloadService,
            )

        stubEmptyPull()
        stubEmptyPackageDownload()
        coEvery { syncOutboxDao.resetInProgressToPending() } returns Unit
        coEvery { syncOutboxDao.getPending() } returns emptyList()
        coEvery { readingProgressDao.getByDocumentId(any()) } returns null
    }

    @Test
    fun sync_success_commitsPullCursorAfterPush() = runTest {
        coEvery { syncCursorStore.getDocumentsSinceMs() } returns 100L
        coEvery { documentApi.indexDocuments(since = 100L) } returns
            Response.success(
                DocumentIndexResponse(
                    documents =
                    listOf(
                        sampleServerDocument(id = "doc-1", updatedAtMs = 500L),
                    ),
                    serverTimeMs = 600L,
                ),
            )
        coEvery { documentDao.getById("doc-1") } returns null

        val result = engine.sync()

        assertTrue(result.isSuccess)
        coVerify { syncCursorStore.setDocumentsSinceMs(600L) }
    }

    @Test
    fun sync_pushFailure_doesNotCommitPullCursor() = runTest {
        coEvery { syncCursorStore.getDocumentsSinceMs() } returns 0L
        coEvery { documentApi.indexDocuments(since = 0L) } returns
            Response.success(
                DocumentIndexResponse(
                    documents = listOf(sampleServerDocument(updatedAtMs = 500L)),
                    serverTimeMs = 600L,
                ),
            )
        coEvery { syncOutboxDao.getPending() } returns
            listOf(
                metadataOutboxEntry(entityId = "doc-local"),
            )
        coEvery { documentDao.getById("doc-local") } returns sampleLocalDocument(id = "doc-local")
        coEvery { documentApi.syncDocuments(any(), any()) } throws IOException("network down")

        val result = engine.sync()

        assertTrue(result.isFailure)
        assertEquals(DocumentError.NetworkFailure, result.exceptionOrNull())
        coVerify(exactly = 0) { syncCursorStore.setDocumentsSinceMs(any()) }
    }

    @Test
    fun pushOutbox_resetsInProgressBeforeReadingPending() = runTest {
        coEvery { syncOutboxDao.getPending() } returns emptyList()

        engine.sync()

        coVerifyOrder {
            syncOutboxDao.resetInProgressToPending()
            syncOutboxDao.getPending()
        }
    }

    @Test
    fun pushDocuments_success_clearsOutboxEntries() = runTest {
        val entry = metadataOutboxEntry(entityId = "doc-1")
        coEvery { syncOutboxDao.getPending() } returns listOf(entry)
        coEvery { documentDao.getById("doc-1") } returns sampleLocalDocument(id = "doc-1")
        stubSuccessfulDocumentSync()

        val result = engine.sync()

        assertTrue(result.isSuccess)
        coVerify { syncOutboxStore.clearEntity(SyncEntityType.METADATA, "doc-1") }
        coVerify(exactly = 0) {
            syncOutboxDao.updateStatus(
                id = entry.id,
                status = SyncOutboxStatus.FAILED,
                attempts = any(),
                lastError = any(),
            )
        }
    }

    @Test
    fun pushDocuments_orphanOutboxEntry_isRemovedWithoutApiCall() = runTest {
        val entry = metadataOutboxEntry(entityId = "missing-doc")
        coEvery { syncOutboxDao.getPending() } returns listOf(entry)
        coEvery { documentDao.getById("missing-doc") } returns null

        val result = engine.sync()

        assertTrue(result.isSuccess)
        coVerify { syncOutboxStore.clearEntity(SyncEntityType.METADATA, "missing-doc") }
        coVerify(exactly = 0) { documentApi.syncDocuments(any(), any()) }
    }

    private fun stubEmptyPull() {
        coEvery { syncCursorStore.getDocumentsSinceMs() } returns 0L
        coEvery { documentApi.indexDocuments(any()) } returns
            Response.success(
                DocumentIndexResponse(
                    documents = emptyList(),
                    serverTimeMs = 1_000L,
                ),
            )
        coEvery { syncOutboxStore.hasPendingMetadata(any()) } returns false
        coEvery { syncOutboxStore.hasPendingProgress(any()) } returns false
    }

    private fun stubEmptyPackageDownload() {
        coEvery { packageDownloadService.downloadMissingPackages() } returns Unit
    }

    @Test
    fun pushDocuments_passesStableIdempotencyKeys() = runTest {
        val entry = metadataOutboxEntry(entityId = "doc-1")
        coEvery { syncOutboxDao.getPending() } returns listOf(entry)
        coEvery { documentDao.getById("doc-1") } returns sampleLocalDocument(id = "doc-1")
        val batchKeySlot = slot<String>()
        coEvery { documentApi.syncDocuments(capture(batchKeySlot), any()) } coAnswers {
            successfulDocumentResponse(secondArg())
        }

        val result = engine.sync()

        assertTrue(result.isSuccess)
        assertEquals("key-doc-1", batchKeySlot.captured)
    }

    private fun stubSuccessfulDocumentSync() {
        coEvery { documentApi.syncDocuments(any(), any()) } coAnswers {
            successfulDocumentResponse(secondArg())
        }
    }

    private fun successfulDocumentResponse(request: DocumentSyncRequest): Response<DocumentIndexResponse> =
        Response.success(
            DocumentIndexResponse(
                documents =
                request.documents.map { item ->
                    DocumentResponse(
                        id = item.id,
                        title = item.title ?: "Title",
                        sourceFormat = item.sourceFormat,
                        status = DocumentStatusDto.READY,
                        importedAt = item.importedAt,
                        charCount = item.charCount,
                        lastReadCharOffset = item.lastReadCharOffset,
                        lastReadSectionId = item.lastReadSectionId,
                        lastReadBlockIndex = item.lastReadBlockIndex,
                        lastReadBlockCharOffset = item.lastReadBlockCharOffset,
                        updatedAtMs = item.updatedAtMs,
                        deleted = item.deleted,
                    )
                },
                serverTimeMs = 2_000L,
            ),
        )

    private fun metadataOutboxEntry(
        entityId: String,
        attempts: Int = 0,
        status: SyncOutboxStatus = SyncOutboxStatus.PENDING,
    ) = SyncOutboxEntity(
        id = "METADATA:$entityId",
        entityType = SyncEntityType.METADATA,
        entityId = entityId,
        createdAtMs = 1L,
        idempotencyKey = "key-$entityId",
        status = status,
        attempts = attempts,
    )

    private fun sampleLocalDocument(id: String) = DocumentEntity(
        id = id,
        title = "Local title",
        sourceFormat = SourceFormat.EPUB,
        status = DocumentStatus.READY,
        updatedAtMs = 1_000L,
    )

    private fun sampleServerDocument(id: String = "doc-1", updatedAtMs: Long) = DocumentResponse(
        id = id,
        title = "Server title",
        sourceFormat = SourceFormatDto.EPUB,
        status = DocumentStatusDto.READY,
        updatedAtMs = updatedAtMs,
        deleted = false,
    )
}
