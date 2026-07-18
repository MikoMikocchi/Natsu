package io.mikoshift.natsu.data.remote

import io.mikoshift.natsu.data.remote.dto.DocumentIndexResponse
import io.mikoshift.natsu.data.remote.dto.DocumentShowResponse
import io.mikoshift.natsu.data.remote.dto.DocumentSearchResponse
import io.mikoshift.natsu.data.remote.dto.DocumentSyncRequest
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

interface DocumentApi {

    @GET("documents")
    suspend fun indexDocuments(
        @Query("since") since: Long,
        @Query("limit") limit: Int? = null,
    ): Response<DocumentIndexResponse>

    @POST("documents/sync")
    suspend fun syncDocuments(
        @Body request: DocumentSyncRequest,
    ): Response<DocumentIndexResponse>

    @GET("documents/search")
    suspend fun search(@Query("q") query: String): Response<DocumentSearchResponse>

    @GET("documents/{id}")
    suspend fun show(@Path("id") id: String): Response<DocumentShowResponse>

    @Multipart
    @POST("documents/import")
    suspend fun importDocument(@Part file: MultipartBody.Part): Response<DocumentShowResponse>

    @Streaming
    @GET("documents/{id}/package")
    suspend fun downloadPackage(@Path("id") id: String): Response<ResponseBody>
}
