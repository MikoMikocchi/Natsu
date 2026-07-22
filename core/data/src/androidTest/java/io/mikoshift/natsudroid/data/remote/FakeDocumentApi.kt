package io.mikoshift.natsudroid.data.remote

import io.mikoshift.natsudroid.data.remote.dto.DocumentIndexResponse
import io.mikoshift.natsudroid.data.remote.dto.DocumentResponse
import io.mikoshift.natsudroid.data.remote.dto.DocumentSearchResponse
import io.mikoshift.natsudroid.data.remote.dto.DocumentShowResponse
import io.mikoshift.natsudroid.data.remote.dto.DocumentStatus
import io.mikoshift.natsudroid.data.remote.dto.DocumentSyncRequest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import java.security.MessageDigest

class FakeDocumentApi {
    var serverTimeMs: Long = 1_000L

    private val documents = linkedMapOf<String, DocumentResponse>()
    private val packages = mutableMapOf<String, ByteArray>()
    private val idempotencyCache = mutableMapOf<String, CachedIdempotentResponse>()

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
        override suspend fun indexDocuments(since: Long, limit: Int?): Response<DocumentIndexResponse> {
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
            idempotencyKey: String,
            request: DocumentSyncRequest,
        ): Response<DocumentIndexResponse> {
            syncDocumentsCallCount++
            syncDocumentsFailure?.let { throw it }

            val requestHash = hashRequest(request)
            idempotencyCache[idempotencyKey]?.let { cached ->
                require(cached.requestHash == requestHash) {
                    "Idempotency key was already used with a different request body"
                }
                return Response.success(cached.response)
            }

            val syncedDocuments =
                request.documents.map { item ->
                    val existing = documents[item.id]
                    val merged =
                        if (existing == null || item.updatedAtMs >= existing.updatedAtMs) {
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

            val response =
                DocumentIndexResponse(
                    documents = syncedDocuments,
                    serverTimeMs = serverTimeMs,
                )
            idempotencyCache[idempotencyKey] = CachedIdempotentResponse(requestHash, response)
            return Response.success(response)
        }

        override suspend fun search(query: String): Response<DocumentSearchResponse> =
            error("Not used in sync integration tests")

        override suspend fun show(id: String): Response<DocumentShowResponse> =
            error("Not used in sync integration tests")

        override suspend fun importDocument(file: MultipartBody.Part): Response<DocumentShowResponse> =
            error("Not used in sync integration tests")

        override suspend fun downloadPackage(id: String): Response<ResponseBody> {
            downloadPackageCallCount++
            val content =
                packages[id]
                    ?: return Response.error(404, "missing".toResponseBody("text/plain".toMediaType()))
            return Response.success(content.toResponseBody("application/zip".toMediaType()))
        }
    }

    private data class CachedIdempotentResponse(val requestHash: String, val response: DocumentIndexResponse)

    private fun hashRequest(request: DocumentSyncRequest): String {
        val canonical =
            request.documents.joinToString("|") { item ->
                listOf(
                    item.id,
                    item.idempotencyKey,
                    item.title,
                    item.sourceFormat.name,
                    item.importedAt,
                    item.charCount,
                    item.lastReadCharOffset,
                    item.lastReadSectionId,
                    item.lastReadBlockIndex,
                    item.lastReadBlockCharOffset,
                    item.updatedAtMs,
                    item.deleted,
                ).joinToString(",")
            }
        val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray())
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }
}
