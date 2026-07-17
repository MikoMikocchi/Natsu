package io.mikoshift.natsu.data.remote

import io.mikoshift.natsu.data.remote.dto.DocumentMetadataIndexResponse
import io.mikoshift.natsu.data.remote.dto.DocumentMetadataShowResponse
import io.mikoshift.natsu.data.remote.dto.DocumentMetadataSyncRequest
import io.mikoshift.natsu.data.remote.dto.DocumentSearchResponse
import io.mikoshift.natsu.data.remote.dto.ReadingProgressIndexResponse
import io.mikoshift.natsu.data.remote.dto.ReadingProgressSyncRequest
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/**
 * Retrofit definition of the backend's document endpoints.
 *
 * Metadata and reading progress are synced on separate delta streams.
 */
interface DocumentApi {

    @GET("documents")
    suspend fun indexMetadata(
        @Query("since") since: Long,
        @Query("limit") limit: Int? = null,
    ): Response<DocumentMetadataIndexResponse>

    @POST("documents/sync")
    suspend fun syncMetadata(
        @Body request: DocumentMetadataSyncRequest,
    ): Response<DocumentMetadataIndexResponse>

    @GET("reading-progress")
    suspend fun indexProgress(
        @Query("since") since: Long,
        @Query("limit") limit: Int? = null,
    ): Response<ReadingProgressIndexResponse>

    @POST("reading-progress/sync")
    suspend fun syncProgress(
        @Body request: ReadingProgressSyncRequest,
    ): Response<ReadingProgressIndexResponse>

    @GET("documents/search")
    suspend fun search(@Query("q") query: String): Response<DocumentSearchResponse>

    @GET("documents/{id}")
    suspend fun show(@Path("id") id: String): Response<DocumentMetadataShowResponse>

    @Multipart
    @POST("documents/import")
    suspend fun importDocument(@Part file: MultipartBody.Part): Response<DocumentMetadataShowResponse>

    @Streaming
    @GET("documents/{id}/package")
    suspend fun downloadPackage(@Path("id") id: String): Response<ResponseBody>
}
