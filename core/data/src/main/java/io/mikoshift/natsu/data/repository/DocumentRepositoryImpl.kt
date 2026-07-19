package io.mikoshift.natsu.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import io.mikoshift.natsu.core.domain.repository.DocumentRepository
import io.mikoshift.natsu.core.model.Document
import io.mikoshift.natsu.core.model.DocumentError
import io.mikoshift.natsu.core.model.DocumentSearchResult
import io.mikoshift.natsu.core.model.content.ReadingPosition
import io.mikoshift.natsu.data.local.PackageFileStore
import io.mikoshift.natsu.data.local.SyncCursorStore
import io.mikoshift.natsu.data.local.SyncOutboxStore
import io.mikoshift.natsu.data.local.db.DocumentCacheDao
import io.mikoshift.natsu.data.local.db.DocumentDao
import io.mikoshift.natsu.data.local.db.ReadingProgressDao
import io.mikoshift.natsu.data.local.db.ReadingProgressEntity
import io.mikoshift.natsu.data.local.db.SyncOutboxDao
import io.mikoshift.natsu.data.mapper.toDomain
import io.mikoshift.natsu.data.mapper.toEntities
import io.mikoshift.natsu.data.pkg.PackageAssetStore
import io.mikoshift.natsu.data.remote.DocumentApi
import io.mikoshift.natsu.data.remote.NetworkFactory
import io.mikoshift.natsu.data.remote.dto.ApiErrorResponse
import io.mikoshift.natsu.data.remote.dto.DocumentStatus
import io.mikoshift.natsu.data.sync.DocumentSyncEngine
import io.mikoshift.natsu.data.sync.PackageDownloadService
import io.mikoshift.natsu.data.sync.SyncScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val documentApi: DocumentApi,
    private val documentDao: DocumentDao,
    private val readingProgressDao: ReadingProgressDao,
    private val documentCacheDao: DocumentCacheDao,
    private val syncOutboxDao: SyncOutboxDao,
    private val syncCursorStore: SyncCursorStore,
    private val syncOutboxStore: SyncOutboxStore,
    private val packageFileStore: PackageFileStore,
    private val packageAssetStore: PackageAssetStore,
    private val packageDownloadService: PackageDownloadService,
    private val syncEngine: DocumentSyncEngine,
    private val syncScheduler: SyncScheduler,
    private val networkFactory: NetworkFactory,
) : DocumentRepository {

    override fun observeLibrary(): Flow<List<Document>> =
        documentDao.observeLibrary().map { documents -> documents.map { it.toDomain() } }

    override fun observeDocument(id: String): Flow<Document?> =
        documentDao.observeById(id).map { relations -> relations?.toDomain() }

    override suspend fun sync(): Result<Unit> = syncEngine.sync()

    override suspend fun search(query: String): Result<List<DocumentSearchResult>> = runCatching {
        documentApi.search(query.trim())
    }.fold(
        onSuccess = { response ->
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body.results.map { it.toDomain() })
            } else {
                Result.failure(mapErrorResponse(response))
            }
        },
        onFailure = { throwable -> Result.failure(throwable.toDocumentFailure()) },
    )

    override suspend fun import(contentUri: String): Result<Document> = runCatching {
        val uri = Uri.parse(contentUri)
        val (bytes, fileName) = readUriContent(uri)
        val part = MultipartBody.Part.createFormData(
            "file",
            fileName,
            bytes.toRequestBody(guessMediaType(fileName)),
        )
        documentApi.importDocument(part)
    }.fold(
        onSuccess = { response ->
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val metadata = body.document
                val (documentEntity, progressEntity) = metadata.toEntities()
                documentDao.upsert(documentEntity)
                readingProgressDao.upsert(progressEntity)
                pollImportStatus(metadata.id)
            } else {
                Result.failure(mapErrorResponse(response))
            }
        },
        onFailure = { throwable -> Result.failure(throwable.toDocumentFailure()) },
    )

    override suspend fun markDeleted(id: String): Result<Unit> {
        val local = documentDao.getById(id) ?: return Result.success(Unit)
        val now = System.currentTimeMillis()
        documentDao.upsert(
            local.copy(
                deleted = true,
                updatedAtMs = now,
            ),
        )
        syncOutboxStore.enqueueMetadata(id, now)
        packageFileStore.delete(id)
        packageAssetStore.deleteDocumentAssets(id)
        documentCacheDao.deleteByDocumentId(id)
        return syncEngine.sync()
    }

    override suspend fun ensurePackageDownloaded(id: String): Result<Unit> = packageDownloadService.download(id)

    override suspend fun updateReadingProgress(id: String, position: ReadingPosition): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        readingProgressDao.upsert(
            ReadingProgressEntity(
                documentId = id,
                lastReadCharOffset = position.globalCharOffset,
                lastReadSectionId = position.sectionId,
                lastReadBlockIndex = position.blockIndex,
                lastReadBlockCharOffset = position.blockCharOffset,
                updatedAtMs = now,
                clientUpdatedAtMs = now,
            ),
        )
        syncOutboxStore.enqueueProgress(id, now)
        syncScheduler.scheduleImmediateSync()
    }

    override suspend fun clearOnLogout() {
        documentDao.deleteAll()
        readingProgressDao.deleteAll()
        documentCacheDao.deleteAll()
        syncOutboxDao.deleteAll()
        packageFileStore.deleteAll()
        packageAssetStore.deleteAll()
        syncCursorStore.clear()
    }

    private suspend fun pollImportStatus(documentId: String): Result<Document> {
        repeat(IMPORT_POLL_MAX_ATTEMPTS) {
            delay(IMPORT_POLL_INTERVAL_MS)
            val response = documentApi.show(documentId)
            val body = response.body()
            if (!response.isSuccessful || body == null) {
                return Result.failure(mapErrorResponse(response))
            }
            val metadata = body.document
            val (documentEntity, progressEntity) = metadata.toEntities()
            documentDao.upsert(documentEntity)
            readingProgressDao.upsert(progressEntity)

            when (metadata.status) {
                DocumentStatus.READY -> {
                    syncEngine.sync()
                    val withRelations = documentDao.getWithRelationsById(documentId)
                    return Result.success(
                        withRelations?.toDomain()
                            ?: metadata.toDomain(),
                    )
                }

                DocumentStatus.FAILED -> {
                    return Result.failure(DocumentError.ImportFailed(metadata.importError))
                }

                DocumentStatus.PENDING -> Unit
            }
        }
        return Result.failure(DocumentError.ImportFailed("Import timed out"))
    }

    private fun readUriContent(uri: Uri): Pair<ByteArray, String> {
        val resolver = context.contentResolver
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IOException("Cannot read file")
        val fileName = resolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                cursor.getString(nameIndex)
            } else {
                null
            }
        } ?: "import.bin"
        return bytes to fileName
    }

    private fun guessMediaType(fileName: String) = when {
        fileName.endsWith(".epub", ignoreCase = true) -> "application/epub+zip"

        fileName.endsWith(".md", ignoreCase = true) ||
            fileName.endsWith(".markdown", ignoreCase = true) -> "text/markdown"

        fileName.endsWith(".txt", ignoreCase = true) -> "text/plain"

        else -> "application/octet-stream"
    }.toMediaTypeOrNull()

    private fun Throwable.toDocumentFailure(): DocumentError = if (this is DocumentError) {
        this
    } else if (this is IOException) {
        DocumentError.NetworkFailure
    } else {
        DocumentError.Unknown(message)
    }

    private fun <T> mapErrorResponse(response: Response<T>): DocumentError {
        val errorBody = try {
            response.errorBody()?.string()
        } catch (_: IOException) {
            null
        }
        if (errorBody != null) {
            try {
                val parsed = networkFactory.json.decodeFromString(
                    ApiErrorResponse.serializer(),
                    errorBody,
                )
                return DocumentError.ValidationError(parsed.errors)
            } catch (_: SerializationException) {
                // fall through
            } catch (_: IllegalArgumentException) {
                // fall through
            }
        }
        return if (response.code() == 401) {
            DocumentError.Unauthorized
        } else {
            DocumentError.Unknown(response.message())
        }
    }

    private companion object {
        const val IMPORT_POLL_INTERVAL_MS = 2_000L
        const val IMPORT_POLL_MAX_ATTEMPTS = 30
    }
}
