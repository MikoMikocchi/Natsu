package io.mikoshift.natsu.data.remote

import io.mikoshift.natsu.data.remote.dto.AuthResponse
import io.mikoshift.natsu.data.remote.dto.ChangePasswordRequest
import io.mikoshift.natsu.data.remote.dto.DeleteAccountRequest
import io.mikoshift.natsu.data.remote.dto.ForgotPasswordRequest
import io.mikoshift.natsu.data.remote.dto.LoginRequest
import io.mikoshift.natsu.data.remote.dto.MessageResponse
import io.mikoshift.natsu.data.remote.dto.RefreshRequest
import io.mikoshift.natsu.data.remote.dto.RegisterRequest
import io.mikoshift.natsu.data.remote.dto.ResetPasswordRequest
import io.mikoshift.natsu.data.remote.dto.UserShowResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST

/**
 * Retrofit definition of the backend's `/v1/auth` endpoints.
 *
 * All functions return [Response] rather than the raw body so callers can inspect HTTP
 * status codes and raw error bodies (parsed as `ApiErrorResponse`) instead of Retrofit
 * throwing on non-2xx responses.
 *
 * Endpoints that require authentication take the `Authorization` header explicitly as a
 * parameter (e.g. pass `"Bearer <token>"`) for now. A later subtask will replace this with
 * an automatic interceptor on an authenticated Retrofit/OkHttp instance.
 */
interface AuthApi {

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/logout")
    suspend fun logout(@Header("Authorization") authHeader: String): Response<Unit>

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): Response<AuthResponse>

    @GET("auth/user")
    suspend fun getUser(@Header("Authorization") authHeader: String): Response<UserShowResponse>

    @DELETE("auth/account")
    suspend fun deleteAccount(
        @Header("Authorization") authHeader: String,
        @Body request: DeleteAccountRequest,
    ): Response<Unit>

    @PATCH("auth/password")
    suspend fun changePassword(
        @Header("Authorization") authHeader: String,
        @Body request: ChangePasswordRequest,
    ): Response<MessageResponse>

    @POST("auth/password/forgot")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<MessageResponse>

    @POST("auth/password/reset")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<MessageResponse>
}
