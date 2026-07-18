package io.mikoshift.natsu.data.remote

import io.mikoshift.natsu.data.remote.dto.DocumentIndexResponse
import io.mikoshift.natsu.data.remote.dto.DocumentResponse
import io.mikoshift.natsu.data.remote.dto.DocumentShowResponse
import io.mikoshift.natsu.data.remote.dto.DocumentSearchResponse
import io.mikoshift.natsu.data.remote.dto.DocumentStatus
import io.mikoshift.natsu.data.remote.dto.DocumentSyncRequest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

class FakeDocumentApi {

    var serverTimeMs: Long = 1_000L

    private val documents = linkedMapOf<String, DocumentResponse>()
    private val packages = mutableMapOf<String, ByteArray>()

    var syncDocumentsCallCount: Int = 0
        private set
    var downloadPackageCallCount: Int = 0
        private set

    var syncDocumentsFailure: Exception? = null

    val documentsPullSinceValues = mutableListOf<Long>()

    fun putDocument(document: DocumentResponse) {
        documents[document.id] = document
    }

    fun putPackage(documentId: String, content: ByteArray) {
        packages[documentId] = content
    }

    fun getDocument(id: String): DocumentResponse? = documents[id]

    fun asDocumentApi(): DocumentApi = object : DocumentApi {
        override suspend fun indexDocuments(
            since: Long,
            limit: Int?,
        ): Response<DocumentIndexResponse> {
            documentsPullSinceValues.add(since)
            val delta = documents.values.filter { it.updatedAtMs > since }
            return Response.success(
                DocumentIndexResponse(
                    documents = delta,
                    serverTimeMs = serverTimeMs,
                ),
            )
        }

        override suspend fun syncDocuments(
            request: DocumentSyncRequest,
        ): Response<DocumentIndexResponse> {
            syncDocumentsCallCount++
            syncDocumentsFailure?.let { throw it }

            val syncedDocuments = request.documents.map { item ->
                val existing = documents[item.id]
                val merged = if (existing == null || item.updatedAtMs >= existing.updatedAtMs) {
                    DocumentResponse(
                        id = item.id,
                        title = item.title ?: existing?.title ?: "Untitled",
                        sourceFormat = item.sourceFormat,
                        status = existing?.status ?: DocumentStatus.READY,
                        importError = existing?.importError,
                        importedAt = item.importedAt,
                        charCount = item.charCount,
                        lastReadCharOffset = item.lastReadCharOffset,
                        lastReadSectionId = item.lastReadSectionId,
                        lastReadBlockIndex = item.lastReadBlockIndex,
                        lastReadBlockCharOffset = item.lastReadBlockCharOffset,
                        updatedAtMs = item.updatedAtMs,
                        packageSizeBytes = existing?.packageSizeBytes ?: 0,
                        packageUpdatedAtMs = existing?.packageUpdatedAtMs ?: 0,
                        packageSha256 = existing?.packageSha256,
                        deleted = item.deleted,
                    )
                } else {
                    existing
                }
                documents[item.id] = merged
                merged
            }

            return Response.success(
                DocumentIndexResponse(
                    documents = syncedDocuments,
                    serverTimeMs = serverTimeMs,
                ),
            )
        }

        override suspend fun search(query: String): Response<DocumentSearchResponse> =
            error("Not used in sync integration tests")

        override suspend fun show(id: String): Response<DocumentShowResponse> =
            error("Not used in sync integration tests")

        override suspend fun importDocument(file: MultipartBody.Part): Response<DocumentShowResponse> =
            error("Not used in sync integration tests")

        override suspend fun downloadPackage(id: String): Response<ResponseBody> {
            downloadPackageCallCount++
            val content = packages[id]
                ?: return Response.error(404, "missing".toResponseBody("text/plain".toMediaType()))
            return Response.success(content.toResponseBody("application/zip".toMediaType()))
        }
    }
}
