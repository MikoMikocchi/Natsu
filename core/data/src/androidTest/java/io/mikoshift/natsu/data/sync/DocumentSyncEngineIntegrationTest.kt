package io.mikoshift.natsu.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mikoshift.natsu.core.model.DocumentStatus
import io.mikoshift.natsu.core.model.SourceFormat
import io.mikoshift.natsu.data.local.PackageFileStore
import io.mikoshift.natsu.data.local.SyncCursorStore
import io.mikoshift.natsu.data.local.SyncOutboxStore
import io.mikoshift.natsu.data.local.db.DocumentEntity
import io.mikoshift.natsu.data.local.db.NatsuDatabase
import io.mikoshift.natsu.data.local.db.ReadingProgressEntity
import io.mikoshift.natsu.data.remote.FakeDocumentApi
import io.mikoshift.natsu.data.remote.dto.DocumentMetadataResponse
import io.mikoshift.natsu.data.remote.dto.DocumentStatus as DocumentStatusDto
import io.mikoshift.natsu.data.remote.dto.ReadingProgressResponse
import io.mikoshift.natsu.data.remote.dto.SourceFormat as SourceFormatDto
import java.io.IOException
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DocumentSyncEngineIntegrationTest {

    private lateinit var context: Context
    private lateinit var database: NatsuDatabase
    private lateinit var fakeApi: FakeDocumentApi
    private lateinit var syncCursorStore: SyncCursorStore
    private lateinit var syncOutboxStore: SyncOutboxStore
    private lateinit var packageFileStore: PackageFileStore
    private lateinit var engine: DocumentSyncEngine

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(
            context,
            NatsuDatabase::class.java,
        ).allowMainThreadQueries().build()

        fakeApi = FakeDocumentApi()
        syncCursorStore = SyncCursorStore(database.syncStateDao())
        syncOutboxStore = SyncOutboxStore(database.syncOutboxDao())
        packageFileStore = PackageFileStore(context)

        engine = DocumentSyncEngine(
            documentApi = fakeApi.asDocumentApi(),
            documentDao = database.documentDao(),
            readingProgressDao = database.readingProgressDao(),
            documentCacheDao = database.documentCacheDao(),
            syncOutboxDao = database.syncOutboxDao(),
            syncOutboxStore = syncOutboxStore,
            syncCursorStore = syncCursorStore,
            packageFileStore = packageFileStore,
        )
    }

    @After
    fun tearDown() {
        packageFileStore.deleteAll()
        database.close()
    }

    @Test
    fun sync_fullCycle_pullsMetadataAndProgress_pushesOutbox_downloadsPackage_commitsCursors() = runTest {
        fakeApi.serverTimeMs = 5_000L
        fakeApi.putDocument(
            serverDocument(
                id = "doc-remote",
                title = "Remote Book",
                updatedAtMs = 2_000L,
                packageSha256 = "sha-new",
            ),
        )
        fakeApi.putProgress(
            ReadingProgressResponse(
                documentId = "doc-remote",
                lastReadCharOffset = 100,
                updatedAtMs = 3_000L,
            ),
        )
        fakeApi.putPackage("doc-remote", byteArrayOf(1, 2, 3, 4))

        database.documentDao().upsert(
            DocumentEntity(
                id = "doc-local",
                title = "Local Edit",
                sourceFormat = SourceFormat.EPUB,
                status = DocumentStatus.READY,
                updatedAtMs = 4_000L,
            ),
        )
        syncOutboxStore.enqueueMetadata("doc-local", nowMs = 10L)

        val result = engine.sync()

        assertTrue(result.isSuccess)
        assertEquals("Remote Book", database.documentDao().getById("doc-remote")?.title)
        assertEquals(100, database.readingProgressDao().getByDocumentId("doc-remote")?.lastReadCharOffset)
        assertEquals("Local Edit", fakeApi.getDocument("doc-local")?.title)
        assertEquals(1, fakeApi.syncMetadataCallCount)
        assertEquals(1, fakeApi.downloadPackageCallCount)

        val cache = database.documentCacheDao().getByDocumentId("doc-remote")
        assertNotNull(cache)
        assertEquals("sha-new", cache?.cachedPackageSha256)
        assertNotNull(cache?.localPackagePath)
        assertTrue(packageFileStore.getPath("doc-remote") != null)

        assertEquals(0, database.syncOutboxDao().getPending().size)
        assertEquals(5_000L, syncCursorStore.getMetadataSinceMs())
        assertEquals(5_000L, syncCursorStore.getProgressSinceMs())
    }

    @Test
    fun sync_pullMetadataWithPendingOutbox_keepsLocalTitleWhileApplyingPackageFields() = runTest {
        fakeApi.serverTimeMs = 2_000L
        fakeApi.putDocument(
            serverDocument(
                id = "doc-1",
                title = "Server Title",
                updatedAtMs = 500L,
                packageSha256 = "sha-from-server",
            ),
        )

        database.documentDao().upsert(
            DocumentEntity(
                id = "doc-1",
                title = "Local Title",
                sourceFormat = SourceFormat.EPUB,
                status = DocumentStatus.READY,
                updatedAtMs = 1_000L,
            ),
        )
        syncOutboxStore.enqueueMetadata("doc-1", nowMs = 1L)

        val result = engine.sync()

        assertTrue(result.isSuccess)
        val local = database.documentDao().getById("doc-1")
        assertEquals("Local Title", local?.title)
        assertEquals("sha-from-server", local?.packageSha256)
        assertTrue(syncOutboxStore.hasPendingMetadata("doc-1"))
    }

    @Test
    fun sync_pullDeletedDocument_cleansProgressCacheAndPackage() = runTest {
        fakeApi.serverTimeMs = 3_000L
        fakeApi.putDocument(
            serverDocument(
                id = "doc-deleted",
                title = "Gone",
                updatedAtMs = 2_000L,
                deleted = true,
            ),
        )

        database.documentDao().upsert(
            DocumentEntity(
                id = "doc-deleted",
                title = "Old",
                sourceFormat = SourceFormat.EPUB,
                status = DocumentStatus.READY,
                updatedAtMs = 1_000L,
            ),
        )
        database.readingProgressDao().upsert(
            ReadingProgressEntity(
                documentId = "doc-deleted",
                lastReadCharOffset = 10,
                updatedAtMs = 1_000L,
            ),
        )
        database.documentCacheDao().upsert(
            io.mikoshift.natsu.data.local.db.DocumentCacheEntity(
                documentId = "doc-deleted",
                localPackagePath = "/tmp/old.zip",
                cachedPackageSha256 = "old-sha",
            ),
        )
        syncOutboxStore.enqueueProgress("doc-deleted", nowMs = 1L)
        packageFileStore.save(
            "doc-deleted",
            byteArrayOf(9, 9, 9).toResponseBody("application/zip".toMediaType()),
        )

        val result = engine.sync()

        assertTrue(result.isSuccess)
        assertTrue(database.documentDao().getById("doc-deleted")?.deleted == true)
        assertNull(database.readingProgressDao().getByDocumentId("doc-deleted"))
        assertNull(database.documentCacheDao().getByDocumentId("doc-deleted"))
        assertFalse(syncOutboxStore.hasPendingProgress("doc-deleted"))
        assertNull(packageFileStore.getPath("doc-deleted"))
    }

    @Test
    fun sync_pushFailure_doesNotCommitCursors() = runTest {
        fakeApi.serverTimeMs = 6_000L
        fakeApi.putDocument(
            serverDocument(
                id = "doc-remote",
                title = "Remote",
                updatedAtMs = 2_000L,
            ),
        )
        database.documentDao().upsert(
            DocumentEntity(
                id = "doc-local",
                title = "Pending Push",
                sourceFormat = SourceFormat.EPUB,
                status = DocumentStatus.READY,
                updatedAtMs = 4_000L,
            ),
        )
        syncOutboxStore.enqueueMetadata("doc-local", nowMs = 1L)
        fakeApi.syncMetadataFailure = IOException("network down")

        val result = engine.sync()

        assertTrue(result.isFailure)
        assertEquals("Remote", database.documentDao().getById("doc-remote")?.title)
        assertEquals(0L, syncCursorStore.getMetadataSinceMs())
        assertEquals(0L, syncCursorStore.getProgressSinceMs())
        assertEquals(1, database.syncOutboxDao().getPending().size)
    }

    @Test
    fun sync_secondRun_usesCommittedCursorForIncrementalPull() = runTest {
        fakeApi.serverTimeMs = 2_000L
        fakeApi.putDocument(
            serverDocument(
                id = "doc-old",
                title = "Old",
                updatedAtMs = 1_000L,
            ),
        )

        val firstResult = engine.sync()
        assertTrue(firstResult.isSuccess)
        assertEquals(2_000L, syncCursorStore.getMetadataSinceMs())

        fakeApi.serverTimeMs = 4_000L
        fakeApi.putDocument(
            serverDocument(
                id = "doc-new",
                title = "New",
                updatedAtMs = 3_000L,
            ),
        )

        val secondResult = engine.sync()

        assertTrue(secondResult.isSuccess)
        assertEquals(listOf(2_000L), fakeApi.metadataPullSinceValues.drop(1))
        assertNotNull(database.documentDao().getById("doc-new"))
        assertEquals(4_000L, syncCursorStore.getMetadataSinceMs())
    }

    @Test
    fun sync_localProgressPush_reachesServerAndClearsOutbox() = runTest {
        fakeApi.serverTimeMs = 2_000L
        database.documentDao().upsert(
            DocumentEntity(
                id = "doc-1",
                title = "Book",
                sourceFormat = SourceFormat.EPUB,
                status = DocumentStatus.READY,
                updatedAtMs = 1_000L,
            ),
        )
        database.readingProgressDao().upsert(
            ReadingProgressEntity(
                documentId = "doc-1",
                lastReadCharOffset = 250,
                updatedAtMs = 1_500L,
            ),
        )
        syncOutboxStore.enqueueProgress("doc-1", nowMs = 1L)

        val result = engine.sync()

        assertTrue(result.isSuccess)
        assertEquals(250, fakeApi.getProgress("doc-1")?.lastReadCharOffset)
        assertFalse(syncOutboxStore.hasPendingProgress("doc-1"))
        assertEquals(1, fakeApi.syncProgressCallCount)
    }

    private fun serverDocument(
        id: String,
        title: String,
        updatedAtMs: Long,
        packageSha256: String? = null,
        deleted: Boolean = false,
    ) = DocumentMetadataResponse(
        id = id,
        title = title,
        sourceFormat = SourceFormatDto.EPUB,
        status = DocumentStatusDto.READY,
        updatedAtMs = updatedAtMs,
        packageSha256 = packageSha256,
        deleted = deleted,
    )
}
