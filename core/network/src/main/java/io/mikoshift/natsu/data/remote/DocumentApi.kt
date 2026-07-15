package io.mikoshift.natsu.data.remote

import io.mikoshift.natsu.data.remote.dto.DocumentIndexResponse
import io.mikoshift.natsu.data.remote.dto.DocumentSearchResponse
import io.mikoshift.natsu.data.remote.dto.DocumentShowResponse
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

/**
 * Retrofit definition of the backend's `/v1/documents` endpoints.
 *
 * Uses the authenticated OkHttpClient; [AuthInterceptor] attaches the Bearer token.
 */
interface DocumentApi {

    @GET("documents")
    suspend fun index(
        @Query("since") since: Long,
        @Query("limit") limit: Int? = null,
    ): Response<DocumentIndexResponse>

    @GET("documents/search")
    suspend fun search(@Query("q") query: String): Response<DocumentSearchResponse>

    @GET("documents/{id}")
    suspend fun show(@Path("id") id: String): Response<DocumentShowResponse>

    @POST("documents/sync")
    suspend fun sync(@Body request: DocumentSyncRequest): Response<DocumentIndexResponse>

    @Multipart
    @POST("documents/import")
    suspend fun importDocument(@Part file: MultipartBody.Part): Response<DocumentShowResponse>

    @Streaming
    @GET("documents/{id}/package")
    suspend fun downloadPackage(@Path("id") id: String): Response<ResponseBody>
}
