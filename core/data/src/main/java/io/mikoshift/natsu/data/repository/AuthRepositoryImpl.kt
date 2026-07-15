package io.mikoshift.natsu.data.repository

import io.mikoshift.natsu.core.domain.repository.AuthRepository
import io.mikoshift.natsu.core.model.AuthError
import io.mikoshift.natsu.core.model.AuthSession
import io.mikoshift.natsu.core.model.DeviceSession
import io.mikoshift.natsu.core.model.User
import io.mikoshift.natsu.data.local.TokenStore
import io.mikoshift.natsu.data.mapper.toDomain
import io.mikoshift.natsu.data.mapper.toSession
import io.mikoshift.natsu.data.remote.AuthApi
import io.mikoshift.natsu.data.remote.NetworkFactory
import io.mikoshift.natsu.data.remote.dto.ApiErrorResponse
import io.mikoshift.natsu.data.remote.dto.AuthResponse
import io.mikoshift.natsu.data.remote.dto.ChangePasswordRequest
import io.mikoshift.natsu.data.remote.dto.DeleteAccountRequest
import io.mikoshift.natsu.data.remote.dto.ForgotPasswordRequest
import io.mikoshift.natsu.data.remote.dto.LoginRequest
import io.mikoshift.natsu.data.remote.dto.MessageResponse
import io.mikoshift.natsu.data.remote.dto.RegisterRequest
import io.mikoshift.natsu.data.remote.dto.ResetPasswordRequest
import io.mikoshift.natsu.di.AuthenticatedAuthApi
import io.mikoshift.natsu.di.UnauthenticatedAuthApi
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import retrofit2.Response

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @UnauthenticatedAuthApi private val unauthenticatedApi: AuthApi,
    @AuthenticatedAuthApi private val authenticatedApi: AuthApi,
    private val tokenStore: TokenStore,
    private val networkFactory: NetworkFactory,
) : AuthRepository {

    override val isLoggedIn: Flow<Boolean> = tokenStore.sessionFlow.map { it != null }

    override val currentSession: StateFlow<AuthSession?> = tokenStore.sessionFlow

    override suspend fun register(
        name: String,
        email: String,
        password: String,
        passwordConfirmation: String,
    ): Result<Unit> = runCatching {
        unauthenticatedApi.register(
            RegisterRequest(
                name = name,
                email = email,
                password = password,
                passwordConfirmation = passwordConfirmation,
            ),
        )
    }.fold(
        onSuccess = { response -> handleAuthResponse(response).map { } },
        onFailure = { throwable -> Result.failure(throwable.toAuthFailure()) },
    )

    override suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        unauthenticatedApi.login(
            LoginRequest(
                email = email,
                password = password,
            ),
        )
    }.fold(
        onSuccess = { response -> handleAuthResponse(response).map { } },
        onFailure = { throwable -> Result.failure(throwable.toAuthFailure()) },
    )

    override suspend fun logout(): Result<Unit> {
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

    override suspend fun getUser(): Result<User> = runCatching {
        val authHeader = tokenStore.sessionFlow.value?.accessToken?.let { "Bearer $it" } ?: ""
        authenticatedApi.getUser(authHeader)
    }.fold(
        onSuccess = { response ->
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body.user.toDomain())
            } else {
                Result.failure(mapErrorResponse(response))
            }
        },
        onFailure = { throwable -> Result.failure(throwable.toAuthFailure()) },
    )

    override suspend fun forgotPassword(email: String): Result<String> = runCatching {
        unauthenticatedApi.forgotPassword(ForgotPasswordRequest(email = email))
    }.fold(
        onSuccess = { response -> mapMessageResponse(response) },
        onFailure = { throwable -> Result.failure(throwable.toAuthFailure()) },
    )

    override suspend fun resetPassword(
        token: String,
        password: String,
        passwordConfirmation: String,
    ): Result<String> = runCatching {
        unauthenticatedApi.resetPassword(
            ResetPasswordRequest(
                token = token,
                password = password,
                passwordConfirmation = passwordConfirmation,
            ),
        )
    }.fold(
        onSuccess = { response -> mapMessageResponse(response) },
        onFailure = { throwable -> Result.failure(throwable.toAuthFailure()) },
    )

    override suspend fun changePassword(
        currentPassword: String,
        password: String,
        passwordConfirmation: String,
    ): Result<String> = runCatching {
        val authHeader = tokenStore.sessionFlow.value?.accessToken?.let { "Bearer $it" } ?: ""
        authenticatedApi.changePassword(
            authHeader,
            ChangePasswordRequest(
                currentPassword = currentPassword,
                password = password,
                passwordConfirmation = passwordConfirmation,
            ),
        )
    }.fold(
        onSuccess = { response -> mapMessageResponse(response) },
        onFailure = { throwable -> Result.failure(throwable.toAuthFailure()) },
    )

    override suspend fun deleteAccount(password: String): Result<Unit> {
        val authHeader = tokenStore.sessionFlow.value?.accessToken?.let { "Bearer $it" } ?: ""
        val result = runCatching {
            authenticatedApi.deleteAccount(authHeader, DeleteAccountRequest(password = password))
        }.fold(
            onSuccess = { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(mapErrorResponse(response))
                }
            },
            onFailure = { throwable -> Result.failure(throwable.toAuthFailure()) },
        )
        if (result.isSuccess) {
            tokenStore.clearSession()
        }
        return result
    }

    override suspend fun getSessions(): Result<List<DeviceSession>> = runCatching {
        val authHeader = tokenStore.sessionFlow.value?.accessToken?.let { "Bearer $it" } ?: ""
        authenticatedApi.getSessions(authHeader)
    }.fold(
        onSuccess = { response ->
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body.map { it.toDomain() })
            } else {
                Result.failure(mapErrorResponse(response))
            }
        },
        onFailure = { throwable -> Result.failure(throwable.toAuthFailure()) },
    )

    override suspend fun revokeSession(id: Long, isCurrentSession: Boolean): Result<Unit> {
        val authHeader = tokenStore.sessionFlow.value?.accessToken?.let { "Bearer $it" } ?: ""
        val result = runCatching {
            authenticatedApi.revokeSession(authHeader, id)
        }.fold(
            onSuccess = { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(mapErrorResponse(response))
                }
            },
            onFailure = { throwable -> Result.failure(throwable.toAuthFailure()) },
        )
        if (result.isSuccess && isCurrentSession) {
            tokenStore.clearSession()
        }
        return result
    }

    private fun mapMessageResponse(response: Response<MessageResponse>): Result<String> {
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            Result.success(body.message)
        } else {
            Result.failure(mapErrorResponse(response))
        }
    }

    private suspend fun handleAuthResponse(
        response: Response<AuthResponse>,
    ): Result<AuthResponse> {
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            tokenStore.saveSession(body.toSession())
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
                val parsed = networkFactory.json.decodeFromString(
                    ApiErrorResponse.serializer(),
                    errorBody,
                )
                return AuthError.ValidationError(parsed.errors)
            } catch (_: SerializationException) {
                // fall through
            } catch (_: IllegalArgumentException) {
                // fall through
            }
        }
        return if (response.code() == 401) {
            AuthError.Unauthorized
        } else {
            AuthError.Unknown(response.message())
        }
    }
}
