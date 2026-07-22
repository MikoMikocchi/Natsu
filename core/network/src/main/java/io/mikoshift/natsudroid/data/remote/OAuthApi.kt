package io.mikoshift.natsudroid.data.remote

import io.mikoshift.natsudroid.data.remote.dto.TokenResponse
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface OAuthApi {
    @FormUrlEncoded
    @POST("oauth2/token")
    suspend fun refresh(
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("client_id") clientId: String,
        @Field("refresh_token") refreshToken: String,
    ): Response<TokenResponse>

    @FormUrlEncoded
    @POST("oauth2/revoke")
    suspend fun revoke(
        @Field("client_id") clientId: String,
        @Field("token") token: String,
        @Field("token_type_hint") tokenTypeHint: String = "refresh_token",
    ): Response<Unit>
}
