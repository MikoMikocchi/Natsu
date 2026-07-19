package io.mikoshift.natsu.data.remote

import io.mikoshift.natsu.data.remote.dto.DictionaryIndexResponse
import io.mikoshift.natsu.data.remote.dto.DictionaryLookupResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface DictionaryApi {
    @GET("dictionaries")
    suspend fun index(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 50,
    ): Response<DictionaryIndexResponse>

    @PATCH("dictionaries/{id}/toggle")
    suspend fun toggle(@Path("id") id: String): Response<Unit>

    @GET("dictionary/lookup")
    suspend fun lookup(@Query("q") query: String): Response<DictionaryLookupResponse>
}
