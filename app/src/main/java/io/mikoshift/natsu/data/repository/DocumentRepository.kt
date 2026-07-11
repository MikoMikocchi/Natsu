package io.mikoshift.natsu.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import io.mikoshift.natsu.data.local.PackageFileStore
import io.mikoshift.natsu.data.local.SyncCursorStore
import io.mikoshift.natsu.data.local.db.DocumentDao
import io.mikoshift.natsu.data.local.db.DocumentEntity
import io.mikoshift.natsu.data.local.db.toEntity
import io.mikoshift.natsu.data.remote.DocumentApi
import io.mikoshift.natsu.data.remote.NetworkFactory
import io.mikoshift.natsu.data.remote.dto.ApiErrorResponse
import io.mikoshift.natsu.data.remote.dto.DocumentResponse
import io.mikoshift.natsu.data.remote.dto.DocumentSearchResponse
import io.mikoshift.natsu.data.remote.dto.DocumentStatus
import io.mikoshift.natsu.data.sync.DocumentSyncEngine
import java.io.IOException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response

class DocumentRepository(
    private val context: Context,
    private val documentApi: DocumentApi,
    private val documentDao: DocumentDao,
    private val syncCursorStore: SyncCursorStore,
    private val packageFileStore: PackageFileStore,
    private val syncEngine: DocumentSyncEngine,
) {

    fun observeLibrary(): Flow<List<DocumentEntity>> = documentDao.observeLibrary()

    suspend fun sync(): Result<Unit> = syncEngine.sync()

    suspend fun search(query: String): Result<DocumentSearchResponse> = runCatching {
        documentApi.search(query.trim())
    }.fold(
        onSuccess = { response ->
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(mapErrorResponse(response))
            }
        },
        onFailure = { throwable -> Result.failure(throwable.toDocumentFailure()) },
    )

    suspend fun import(uri: Uri): Result<DocumentResponse> = runCatching {
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
                val document = body.document
                documentDao.upsert(document.toEntity())
                pollImportStatus(document.id)
            } else {
                Result.failure(mapErrorResponse(response))
            }
        },
        onFailure = { throwable -> Result.failure(throwable.toDocumentFailure()) },
    )

    suspend fun markDeleted(id: String): Result<Unit> {
        val local = documentDao.getById(id) ?: return Result.success(Unit)
        val now = System.currentTimeMillis()
        documentDao.upsert(
            local.copy(
                deleted = true,
                isDirty = true,
                updatedAtMs = now,
            ),
        )
        packageFileStore.delete(id)
        return syncEngine.sync()
    }

    suspend fun clearOnLogout() {
        documentDao.deleteAll()
        packageFileStore.deleteAll()
        syncCursorStore.clear()
    }

    private suspend fun pollImportStatus(documentId: String): Result<DocumentResponse> {
        repeat(IMPORT_POLL_MAX_ATTEMPTS) {
            delay(IMPORT_POLL_INTERVAL_MS)
            val response = documentApi.show(documentId)
            val body = response.body()
            if (!response.isSuccessful || body == null) {
                return Result.failure(mapErrorResponse(response))
            }
            val document = body.document
            documentDao.upsert(document.toEntity())

            when (document.status) {
                DocumentStatus.READY -> {
                    syncEngine.sync()
                    return Result.success(document)
                }
                DocumentStatus.FAILED -> {
                    return Result.failure(DocumentError.ImportFailed(document.importError))
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

    private fun guessMediaType(fileName: String) =
        when {
            fileName.endsWith(".epub", ignoreCase = true) -> "application/epub+zip"
            fileName.endsWith(".md", ignoreCase = true) ||
                fileName.endsWith(".markdown", ignoreCase = true) -> "text/markdown"
            fileName.endsWith(".txt", ignoreCase = true) -> "text/plain"
            else -> "application/octet-stream"
        }.toMediaTypeOrNull()

    private fun Throwable.toDocumentFailure(): DocumentError =
        if (this is DocumentError) this
        else if (this is IOException) DocumentError.NetworkFailure
        else DocumentError.Unknown(message)

    private fun <T> mapErrorResponse(response: Response<T>): DocumentError {
        val errorBody = try {
            response.errorBody()?.string()
        } catch (_: IOException) {
            null
        }
        if (errorBody != null) {
            try {
                val parsed = NetworkFactory.json.decodeFromString(
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
