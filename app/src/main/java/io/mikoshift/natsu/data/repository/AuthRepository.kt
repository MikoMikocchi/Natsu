package io.mikoshift.natsu.data.repository

import io.mikoshift.natsu.data.local.AuthSession
import io.mikoshift.natsu.data.local.TokenStore
import io.mikoshift.natsu.data.remote.AuthApi
import io.mikoshift.natsu.data.remote.NetworkFactory
import io.mikoshift.natsu.data.remote.dto.ApiErrorResponse
import io.mikoshift.natsu.data.remote.dto.AuthResponse
import io.mikoshift.natsu.data.remote.dto.LoginRequest
import io.mikoshift.natsu.data.remote.dto.RegisterRequest
import io.mikoshift.natsu.data.remote.dto.UserResponse
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import retrofit2.Response

/**
 * Repository fronting the backend's `/v1/auth` endpoints.
 *
 * Takes two [AuthApi] instances: [unauthenticatedApi] for endpoints that don't require a
 * token (register/login/refresh/forgot-password/reset-password), and [authenticatedApi] for
 * endpoints that do (logout/getUser/deleteAccount/changePassword) — the latter's underlying
 * OkHttpClient auto-attaches the Bearer token via `AuthInterceptor`.
 *
 * Every network-calling function returns a `kotlin.Result<T>` whose failure is always an
 * [AuthError] (which itself extends [RuntimeException], so `result.exceptionOrNull() as?
 * AuthError` recovers it directly without needing to unwrap a separate wrapper exception).
 */
class AuthRepository(
    private val unauthenticatedApi: AuthApi,
    private val authenticatedApi: AuthApi,
    private val tokenStore: TokenStore,
) {

    /** Whether a session is currently persisted. */
    val isLoggedIn: Flow<Boolean> = tokenStore.sessionFlow.map { it != null }

    /** Re-exports [TokenStore.sessionFlow] directly; state lives in [TokenStore], not here. */
    val currentSession: StateFlow<AuthSession?> = tokenStore.sessionFlow

    suspend fun register(
        name: String,
        email: String,
        password: String,
        passwordConfirmation: String,
    ): Result<AuthResponse> = runCatching {
        unauthenticatedApi.register(
            RegisterRequest(
                name = name,
                email = email,
                password = password,
                passwordConfirmation = passwordConfirmation,
            ),
        )
    }.fold(
        onSuccess = { response -> handleAuthResponse(response) },
        onFailure = { throwable -> Result.failure(throwable.toAuthFailure()) },
    )

    suspend fun login(email: String, password: String): Result<AuthResponse> = runCatching {
        unauthenticatedApi.login(
            LoginRequest(
                email = email,
                password = password,
            ),
        )
    }.fold(
        onSuccess = { response -> handleAuthResponse(response) },
        onFailure = { throwable -> Result.failure(throwable.toAuthFailure()) },
    )

    /**
     * Logs the user out. The local session is *always* cleared afterward, regardless of
     * whether the network call succeeds, fails with an auth error, or fails outright — the
     * whole point of logout is to remove local credentials. [Result.success] is returned for
     * a genuine 2xx response *or* a 401 (session was already invalid server-side, which is
     * still an acceptable logout outcome); [Result.failure] is returned only for network-level
     * failures or unexpected server errors, even though local state has already been cleared.
     */
    suspend fun logout(): Result<Unit> {
        val authHeader = tokenStore.sessionFlow.value?.accessToken?.let { "Bearer $it" } ?: ""
        val result = runCatching {
            authenticatedApi.logout(authHeader)
        }.fold(
            onSuccess = { response ->
                if (response.isSuccessful || response.code() == 401) {
                    Result.success(Unit)
                } else {
                    Result.failure(mapErrorResponse(response))
                }
            },
            onFailure = { throwable -> Result.failure(throwable.toAuthFailure()) },
        )
        tokenStore.clearSession()
        return result
    }

    suspend fun getUser(): Result<UserResponse> = runCatching {
        val authHeader = tokenStore.sessionFlow.value?.accessToken?.let { "Bearer $it" } ?: ""
        authenticatedApi.getUser(authHeader)
    }.fold(
        onSuccess = { response ->
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body.user)
            } else {
                Result.failure(mapErrorResponse(response))
            }
        },
        onFailure = { throwable -> Result.failure(throwable.toAuthFailure()) },
    )

    private suspend fun handleAuthResponse(
        response: Response<AuthResponse>,
    ): Result<AuthResponse> {
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            tokenStore.saveSession(body)
            Result.success(body)
        } else {
            Result.failure(mapErrorResponse(response))
        }
    }

    private fun Throwable.toAuthFailure(): AuthError =
        if (this is IOException) AuthError.NetworkFailure else AuthError.Unknown(message)

    private fun <T> mapErrorResponse(response: Response<T>): AuthError {
        val errorBody = try {
            response.errorBody()?.string()
        } catch (_: IOException) {
            null
        }
        if (errorBody != null) {
            try {
                val parsed = NetworkFactory.json.decodeFromString(
                    ApiErrorResponse.serializer(),
                    errorBody,
                )
                return AuthError.ValidationError(parsed.errors)
            } catch (_: SerializationException) {
                // fall through to status-code based mapping below
            } catch (_: IllegalArgumentException) {
                // fall through to status-code based mapping below
            }
        }
        return if (response.code() == 401) {
            AuthError.Unauthorized
        } else {
            AuthError.Unknown(response.message())
        }
    }
}
