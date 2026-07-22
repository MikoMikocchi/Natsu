package io.mikoshift.natsudroid.data.remote

import io.mikoshift.natsudroid.data.remote.dto.ReaderSettingShowResponse
import io.mikoshift.natsudroid.data.remote.dto.ReaderSettingUpdateRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

interface ReaderSettingApi {
    @GET("settings/reader")
    suspend fun show(): Response<ReaderSettingShowResponse>

    @PATCH("settings/reader")
    suspend fun update(@Body request: ReaderSettingUpdateRequest): Response<ReaderSettingShowResponse>
}
