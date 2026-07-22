package io.mikoshift.natsudroid.data.repository

import io.mikoshift.natsudroid.core.common.di.OAuthClientId
import io.mikoshift.natsudroid.core.domain.repository.AuthRepository
import io.mikoshift.natsudroid.core.model.AuthError
import io.mikoshift.natsudroid.core.model.AuthSession
import io.mikoshift.natsudroid.core.model.DeviceSession
import io.mikoshift.natsudroid.core.model.User
import io.mikoshift.natsudroid.data.local.TokenStore
import io.mikoshift.natsudroid.data.mapper.toDomain
import io.mikoshift.natsudroid.data.mapper.toSession
import io.mikoshift.natsudroid.data.remote.AuthApi
import io.mikoshift.natsudroid.data.remote.NetworkFactory
import io.mikoshift.natsudroid.data.remote.OAuthApi
import io.mikoshift.natsudroid.data.remote.UserInfoApi
import io.mikoshift.natsudroid.data.remote.dto.ApiErrorResponse
import io.mikoshift.natsudroid.data.remote.dto.ChangePasswordRequest
import io.mikoshift.natsudroid.data.remote.dto.DeleteAccountRequest
import io.mikoshift.natsudroid.data.remote.dto.ForgotPasswordRequest
import io.mikoshift.natsudroid.data.remote.dto.LoginRequest
import io.mikoshift.natsudroid.data.remote.dto.MessageResponse
import io.mikoshift.natsudroid.data.remote.dto.RegisterRequest
import io.mikoshift.natsudroid.data.remote.dto.ResetPasswordRequest
import io.mikoshift.natsudroid.data.remote.dto.TokenResponse
import io.mikoshift.natsudroid.di.AuthenticatedAuthApi
import io.mikoshift.natsudroid.di.UnauthenticatedAuthApi
import io.mikoshift.natsudroid.di.UnauthenticatedOAuthApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl
@Inject
constructor(
    @UnauthenticatedAuthApi private val unauthenticatedApi: AuthApi,
    @AuthenticatedAuthApi private val authenticatedApi: AuthApi,
    @UnauthenticatedOAuthApi private val oauthApi: OAuthApi,
    private val userInfoApi: UserInfoApi,
    private val tokenStore: TokenStore,
    private val networkFactory: NetworkFactory,
    @OAuthClientId private val clientId: String,
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
        onSuccess = { response ->
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(mapErrorResponse(response))
            }
        },
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
        onSuccess = { response -> handleLoginResponse(response).map { } },
        onFailure = { throwable -> Result.failure(throwable.toAuthFailure()) },
    )

    override suspend fun logout(): Result<Unit> {
        val refreshToken = tokenStore.getRefreshTokenBlocking()
        if (refreshToken != null) {
            runCatching {
                oauthApi.revoke(clientId = clientId, token = refreshToken)
            }
        }
        tokenStore.clearSession()
        return Result.success(Unit)
    }

    override suspend fun getUser(): Result<User> = runCatching {
        userInfoApi.getUserInfo()
    }.fold(
        onSuccess = { response ->
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body.toDomain())
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

    override suspend fun resetPassword(token: String, password: String, passwordConfirmation: String): Result<String> =
        runCatching {
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
        authenticatedApi.changePassword(
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
        val result =
            runCatching {
                authenticatedApi.deleteAccount(DeleteAccountRequest(password = password))
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
        authenticatedApi.getSessions()
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

    override suspend fun revokeSession(id: String, isCurrentSession: Boolean): Result<Unit> {
        val result =
            runCatching {
                authenticatedApi.revokeSession(id)
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

    private suspend fun handleLoginResponse(response: Response<TokenResponse>): Result<AuthSession> {
        val tokenBody = response.body()
        if (!response.isSuccessful || tokenBody == null) {
            return Result.failure(mapErrorResponse(response))
        }

        val userInfoResponse = userInfoApi.getUserInfo("Bearer ${tokenBody.accessToken}")
        val userInfoBody = userInfoResponse.body()
        if (!userInfoResponse.isSuccessful || userInfoBody == null) {
            return Result.failure(mapErrorResponse(userInfoResponse))
        }

        val session = tokenBody.toSession(userInfoBody)
        tokenStore.saveSession(session)
        return Result.success(session)
    }

    private fun mapMessageResponse(response: Response<MessageResponse>): Result<String> {
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            Result.success(body.message)
        } else {
            Result.failure(mapErrorResponse(response))
        }
    }

    private fun Throwable.toAuthFailure(): AuthError =
        if (this is IOException) AuthError.NetworkFailure else AuthError.Unknown(message)

    private fun <T> mapErrorResponse(response: Response<T>): AuthError {
        val errorBody =
            try {
                response.errorBody()?.string()
            } catch (_: IOException) {
                null
            }
        if (errorBody != null) {
            try {
                val parsed =
                    networkFactory.json.decodeFromString(
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
