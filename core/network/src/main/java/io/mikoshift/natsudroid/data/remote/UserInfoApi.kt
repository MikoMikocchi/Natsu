package io.mikoshift.natsudroid.data.remote

import io.mikoshift.natsudroid.data.remote.dto.UserInfoResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface UserInfoApi {
    @GET("userinfo")
    suspend fun getUserInfo(): Response<UserInfoResponse>

    @GET("userinfo")
    suspend fun getUserInfo(@Header("Authorization") authorization: String): Response<UserInfoResponse>
}
