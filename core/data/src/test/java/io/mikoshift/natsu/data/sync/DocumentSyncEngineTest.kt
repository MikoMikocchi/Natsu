package io.mikoshift.natsu.data.sync

import io.mikoshift.natsu.core.model.DocumentError
import io.mikoshift.natsu.core.model.DocumentStatus
import io.mikoshift.natsu.core.model.SourceFormat
import io.mikoshift.natsu.data.local.PackageFileStore
import io.mikoshift.natsu.data.local.SyncCursorStore
import io.mikoshift.natsu.data.local.SyncOutboxStore
import io.mikoshift.natsu.data.local.db.DocumentCacheDao
import io.mikoshift.natsu.data.local.db.DocumentDao
import io.mikoshift.natsu.data.local.db.DocumentEntity
import io.mikoshift.natsu.data.local.db.ReadingProgressDao
import io.mikoshift.natsu.data.local.db.SyncEntityType
import io.mikoshift.natsu.data.local.db.SyncOutboxDao
import io.mikoshift.natsu.data.local.db.SyncOutboxEntity
import io.mikoshift.natsu.data.local.db.SyncOutboxStatus
import io.mikoshift.natsu.data.remote.DocumentApi
import io.mikoshift.natsu.data.remote.dto.DocumentMetadataIndexResponse
import io.mikoshift.natsu.data.remote.dto.DocumentMetadataResponse
import io.mikoshift.natsu.data.remote.dto.DocumentMetadataSyncRequest
import io.mikoshift.natsu.data.remote.dto.DocumentStatus as DocumentStatusDto
import io.mikoshift.natsu.data.remote.dto.ReadingProgressIndexResponse
import io.mikoshift.natsu.data.remote.dto.SourceFormat as SourceFormatDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

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

        engine = DocumentSyncEngine(
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
    }

    @Test
    fun sync_success_commitsPullCursorsAfterPush() = runTest {
        coEvery { syncCursorStore.getMetadataSinceMs() } returns 100L
        coEvery { syncCursorStore.getProgressSinceMs() } returns 200L
        coEvery { documentApi.indexMetadata(since = 100L) } returns Response.success(
            DocumentMetadataIndexResponse(
                documents = listOf(
                    sampleServerDocument(id = "doc-1", updatedAtMs = 500L),
                ),
                serverTimeMs = 600L,
            ),
        )
        coEvery { documentApi.indexProgress(since = 200L) } returns Response.success(
            ReadingProgressIndexResponse(
                progress = emptyList(),
                serverTimeMs = 900L,
            ),
        )
        coEvery { documentDao.getById("doc-1") } returns null

        val result = engine.sync()

        assertTrue(result.isSuccess)
        coVerify { syncCursorStore.setMetadataSinceMs(600L) }
        coVerify { syncCursorStore.setProgressSinceMs(900L) }
    }

    @Test
    fun sync_pushFailure_doesNotCommitPullCursors() = runTest {
        coEvery { syncCursorStore.getMetadataSinceMs() } returns 0L
        coEvery { documentApi.indexMetadata(since = 0L) } returns Response.success(
            DocumentMetadataIndexResponse(
                documents = listOf(sampleServerDocument(updatedAtMs = 500L)),
                serverTimeMs = 600L,
            ),
        )
        coEvery { syncOutboxDao.getPending() } returns listOf(
            metadataOutboxEntry(entityId = "doc-local"),
        )
        coEvery { documentDao.getById("doc-local") } returns sampleLocalDocument(id = "doc-local")
        coEvery { documentApi.syncMetadata(any()) } throws IOException("network down")

        val result = engine.sync()

        assertTrue(result.isFailure)
        assertEquals(DocumentError.NetworkFailure, result.exceptionOrNull())
        coVerify(exactly = 0) { syncCursorStore.setMetadataSinceMs(any()) }
        coVerify(exactly = 0) { syncCursorStore.setProgressSinceMs(any()) }
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
    fun pushMetadata_success_clearsOutboxEntries() = runTest {
        val entry = metadataOutboxEntry(entityId = "doc-1")
        coEvery { syncOutboxDao.getPending() } returns listOf(entry)
        coEvery { documentDao.getById("doc-1") } returns sampleLocalDocument(id = "doc-1")
        stubSuccessfulMetadataSync()

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
    fun pushMetadata_batchFailure_marksEntriesFailedForRetry() = runTest {
        val entry = metadataOutboxEntry(entityId = "doc-1", attempts = 2)
        coEvery { syncOutboxDao.getPending() } returns listOf(entry)
        coEvery { documentDao.getById("doc-1") } returns sampleLocalDocument(id = "doc-1")
        coEvery { documentApi.syncMetadata(any()) } throws IOException("network down")

        val result = engine.sync()

        assertTrue(result.isFailure)
        coVerify {
            syncOutboxDao.updateStatus(
                id = entry.id,
                status = SyncOutboxStatus.IN_PROGRESS,
                attempts = 3,
                lastError = null,
            )
        }
        coVerify {
            syncOutboxDao.updateStatus(
                id = entry.id,
                status = SyncOutboxStatus.FAILED,
                attempts = 3,
                lastError = "network down",
            )
        }
    }

    @Test
    fun pushMetadata_partialBatchSuccess_leavesLaterBatchForRetry() = runTest {
        val entries = (1..101).map { index ->
            metadataOutboxEntry(entityId = "doc-$index")
        }
        coEvery { syncOutboxDao.getPending() } returns entries
        entries.forEach { entry ->
            coEvery { documentDao.getById(entry.entityId) } returns sampleLocalDocument(id = entry.entityId)
        }

        var syncCalls = 0
        coEvery { documentApi.syncMetadata(any()) } coAnswers {
            syncCalls++
            if (syncCalls == 1) {
                successfulMetadataResponse(firstArg())
            } else {
                throw IOException("second batch failed")
            }
        }

        val result = engine.sync()

        assertTrue(result.isFailure)
        coVerify(exactly = 2) { documentApi.syncMetadata(any()) }
        coVerify(atLeast = 1) { syncOutboxStore.clearEntity(SyncEntityType.METADATA, "doc-1") }
        coVerify(atLeast = 1) { syncOutboxStore.clearEntity(SyncEntityType.METADATA, "doc-100") }
        coVerify(exactly = 0) { syncOutboxStore.clearEntity(SyncEntityType.METADATA, "doc-101") }
        coVerify {
            syncOutboxDao.updateStatus(
                id = "METADATA:doc-101",
                status = SyncOutboxStatus.FAILED,
                attempts = 1,
                lastError = "second batch failed",
            )
        }
    }

    @Test
    fun pushMetadata_orphanOutboxEntry_isRemovedWithoutApiCall() = runTest {
        val entry = metadataOutboxEntry(entityId = "missing-doc")
        coEvery { syncOutboxDao.getPending() } returns listOf(entry)
        coEvery { documentDao.getById("missing-doc") } returns null

        val result = engine.sync()

        assertTrue(result.isSuccess)
        coVerify { syncOutboxStore.clearEntity(SyncEntityType.METADATA, "missing-doc") }
        coVerify(exactly = 0) { documentApi.syncMetadata(any()) }
    }

    @Test
    fun pushMetadata_failedEntry_isRetriedOnNextSync() = runTest {
        val entry = metadataOutboxEntry(
            entityId = "doc-1",
            attempts = 1,
            status = SyncOutboxStatus.FAILED,
        )
        coEvery { syncOutboxDao.getPending() } returnsMany listOf(
            listOf(entry),
            emptyList(),
        )
        coEvery { documentDao.getById("doc-1") } returns sampleLocalDocument(id = "doc-1")
        stubSuccessfulMetadataSync()

        val firstResult = engine.sync()
        val secondResult = engine.sync()

        assertTrue(firstResult.isSuccess)
        assertTrue(secondResult.isSuccess)
        coVerify(exactly = 1) { documentApi.syncMetadata(any()) }
        coVerify { syncOutboxStore.clearEntity(SyncEntityType.METADATA, "doc-1") }
    }

    private fun stubEmptyPull() {
        coEvery { syncCursorStore.getMetadataSinceMs() } returns 0L
        coEvery { syncCursorStore.getProgressSinceMs() } returns 0L
        coEvery { documentApi.indexMetadata(any()) } returns Response.success(
            DocumentMetadataIndexResponse(
                documents = emptyList(),
                serverTimeMs = 1_000L,
            ),
        )
        coEvery { documentApi.indexProgress(any()) } returns Response.success(
            ReadingProgressIndexResponse(
                progress = emptyList(),
                serverTimeMs = 1_000L,
            ),
        )
        coEvery { syncOutboxStore.hasPendingMetadata(any()) } returns false
        coEvery { syncOutboxStore.hasPendingProgress(any()) } returns false
    }

    private fun stubEmptyPackageDownload() {
        coEvery { packageDownloadService.downloadMissingPackages() } returns Unit
    }

    private fun stubSuccessfulMetadataSync() {
        coEvery { documentApi.syncMetadata(any()) } coAnswers {
            successfulMetadataResponse(firstArg())
        }
    }

    private fun successfulMetadataResponse(
        request: DocumentMetadataSyncRequest,
    ): Response<DocumentMetadataIndexResponse> = Response.success(
        DocumentMetadataIndexResponse(
            documents = request.documents.map { item ->
                DocumentMetadataResponse(
                    id = item.id,
                    title = item.title ?: "Title",
                    sourceFormat = item.sourceFormat,
                    status = DocumentStatusDto.READY,
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

    private fun sampleServerDocument(
        id: String = "doc-1",
        updatedAtMs: Long,
    ) = DocumentMetadataResponse(
        id = id,
        title = "Server title",
        sourceFormat = SourceFormatDto.EPUB,
        status = DocumentStatusDto.READY,
        updatedAtMs = updatedAtMs,
    )
}
