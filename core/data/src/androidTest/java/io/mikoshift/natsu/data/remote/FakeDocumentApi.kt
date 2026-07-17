package io.mikoshift.natsu.data.remote

import io.mikoshift.natsu.data.remote.dto.DocumentMetadataIndexResponse
import io.mikoshift.natsu.data.remote.dto.DocumentMetadataResponse
import io.mikoshift.natsu.data.remote.dto.DocumentMetadataShowResponse
import io.mikoshift.natsu.data.remote.dto.DocumentMetadataSyncRequest
import io.mikoshift.natsu.data.remote.dto.DocumentSearchResponse
import io.mikoshift.natsu.data.remote.dto.DocumentStatus
import io.mikoshift.natsu.data.remote.dto.ReadingProgressIndexResponse
import io.mikoshift.natsu.data.remote.dto.ReadingProgressResponse
import io.mikoshift.natsu.data.remote.dto.ReadingProgressSyncRequest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

/**
 * In-memory [DocumentApi] for integration tests of [io.mikoshift.natsu.data.sync.DocumentSyncEngine].
 */
class FakeDocumentApi {

    var serverTimeMs: Long = 1_000L

    private val documents = linkedMapOf<String, DocumentMetadataResponse>()
    private val progressByDocumentId = linkedMapOf<String, ReadingProgressResponse>()
    private val packages = mutableMapOf<String, ByteArray>()

    var syncMetadataCallCount: Int = 0
        private set
    var syncProgressCallCount: Int = 0
        private set
    var downloadPackageCallCount: Int = 0
        private set

    var syncMetadataFailure: Exception? = null
    var syncProgressFailure: Exception? = null

    val metadataPullSinceValues = mutableListOf<Long>()
    val progressPullSinceValues = mutableListOf<Long>()

    fun putDocument(document: DocumentMetadataResponse) {
        documents[document.id] = document
    }

    fun putProgress(progress: ReadingProgressResponse) {
        progressByDocumentId[progress.documentId] = progress
    }

    fun putPackage(documentId: String, content: ByteArray) {
        packages[documentId] = content
    }

    fun getDocument(id: String): DocumentMetadataResponse? = documents[id]

    fun getProgress(documentId: String): ReadingProgressResponse? = progressByDocumentId[documentId]

    fun asDocumentApi(): DocumentApi = object : DocumentApi {
        override suspend fun indexMetadata(
            since: Long,
            limit: Int?,
        ): Response<DocumentMetadataIndexResponse> {
            metadataPullSinceValues.add(since)
            val delta = documents.values.filter { it.updatedAtMs > since }
            return Response.success(
                DocumentMetadataIndexResponse(
                    documents = delta,
                    serverTimeMs = serverTimeMs,
                ),
            )
        }

        override suspend fun syncMetadata(
            request: DocumentMetadataSyncRequest,
        ): Response<DocumentMetadataIndexResponse> {
            syncMetadataCallCount++
            syncMetadataFailure?.let { throw it }

            val syncedDocuments = request.documents.map { item ->
                val existing = documents[item.id]
                val merged = if (existing == null || item.updatedAtMs >= existing.updatedAtMs) {
                    DocumentMetadataResponse(
                        id = item.id,
                        title = item.title ?: existing?.title ?: "Untitled",
                        sourceFormat = item.sourceFormat,
                        status = existing?.status ?: DocumentStatus.READY,
                        importError = existing?.importError,
                        importedAt = item.importedAt,
                        charCount = item.charCount,
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
                DocumentMetadataIndexResponse(
                    documents = syncedDocuments,
                    serverTimeMs = serverTimeMs,
                ),
            )
        }

        override suspend fun indexProgress(
            since: Long,
            limit: Int?,
        ): Response<ReadingProgressIndexResponse> {
            progressPullSinceValues.add(since)
            val delta = progressByDocumentId.values.filter { it.updatedAtMs > since }
            return Response.success(
                ReadingProgressIndexResponse(
                    progress = delta,
                    serverTimeMs = serverTimeMs,
                ),
            )
        }

        override suspend fun syncProgress(
            request: ReadingProgressSyncRequest,
        ): Response<ReadingProgressIndexResponse> {
            syncProgressCallCount++
            syncProgressFailure?.let { throw it }

            val syncedProgress = request.progress.map { item ->
                val existing = progressByDocumentId[item.documentId]
                val merged = if (existing == null || item.updatedAtMs >= existing.updatedAtMs) {
                    ReadingProgressResponse(
                        documentId = item.documentId,
                        lastReadCharOffset = item.lastReadCharOffset,
                        lastReadSectionId = item.lastReadSectionId,
                        lastReadBlockIndex = item.lastReadBlockIndex,
                        lastReadBlockCharOffset = item.lastReadBlockCharOffset,
                        updatedAtMs = item.updatedAtMs,
                        clientUpdatedAtMs = item.clientUpdatedAtMs,
                    )
                } else {
                    existing
                }
                progressByDocumentId[item.documentId] = merged
                merged
            }

            return Response.success(
                ReadingProgressIndexResponse(
                    progress = syncedProgress,
                    serverTimeMs = serverTimeMs,
                ),
            )
        }

        override suspend fun search(query: String): Response<DocumentSearchResponse> =
            error("Not used in sync integration tests")

        override suspend fun show(id: String): Response<DocumentMetadataShowResponse> =
            error("Not used in sync integration tests")

        override suspend fun importDocument(file: MultipartBody.Part): Response<DocumentMetadataShowResponse> =
            error("Not used in sync integration tests")

        override suspend fun downloadPackage(id: String): Response<ResponseBody> {
            downloadPackageCallCount++
            val content = packages[id]
                ?: return Response.error(404, "missing".toResponseBody("text/plain".toMediaType()))
            return Response.success(content.toResponseBody("application/zip".toMediaType()))
        }
    }
}
