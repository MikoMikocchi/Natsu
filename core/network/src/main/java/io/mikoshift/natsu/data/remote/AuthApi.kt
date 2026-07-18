package io.mikoshift.natsu.data.remote

import io.mikoshift.natsu.data.remote.dto.ChangePasswordRequest
import io.mikoshift.natsu.data.remote.dto.DeleteAccountRequest
import io.mikoshift.natsu.data.remote.dto.DeviceSessionResponse
import io.mikoshift.natsu.data.remote.dto.ForgotPasswordRequest
import io.mikoshift.natsu.data.remote.dto.LoginRequest
import io.mikoshift.natsu.data.remote.dto.MessageResponse
import io.mikoshift.natsu.data.remote.dto.RegisterRequest
import io.mikoshift.natsu.data.remote.dto.RegisterResponse
import io.mikoshift.natsu.data.remote.dto.ResetPasswordRequest
import io.mikoshift.natsu.data.remote.dto.TokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit definition of the backend's `/v1/auth` endpoints.
 *
 * All functions return [Response] rather than the raw body so callers can inspect HTTP
 * status codes and raw error bodies (parsed as `ApiErrorResponse`) instead of Retrofit
 * throwing on non-2xx responses.
 */
interface AuthApi {

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<TokenResponse>

    @DELETE("auth/account")
    suspend fun deleteAccount(@Body request: DeleteAccountRequest): Response<Unit>

    @PATCH("auth/password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<MessageResponse>

    @POST("auth/password/forgot")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<MessageResponse>

    @POST("auth/password/reset")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<MessageResponse>

    @GET("auth/sessions")
    suspend fun getSessions(): Response<List<DeviceSessionResponse>>

    @DELETE("auth/sessions/{id}")
    suspend fun revokeSession(@Path("id") id: String): Response<Unit>
}
