package io.mikoshift.natsu.core.testing.repository

import io.mikoshift.natsu.core.domain.repository.AuthRepository
import io.mikoshift.natsu.core.model.AuthSession
import io.mikoshift.natsu.core.model.DeviceSession
import io.mikoshift.natsu.core.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class FakeAuthRepository(
    session: AuthSession? = null,
) : AuthRepository {
    private val _currentSession = MutableStateFlow(session)
    override val currentSession: StateFlow<AuthSession?> = _currentSession.asStateFlow()
    override val isLoggedIn: Flow<Boolean> = currentSession.map { it != null }

    var loginResult: Result<Unit> = Result.success(Unit)
    var getUserResult: Result<User> = Result.failure(UnsupportedOperationException())
    var getSessionsResult: Result<List<DeviceSession>> = Result.failure(UnsupportedOperationException())
    var changePasswordResult: Result<String> = Result.failure(UnsupportedOperationException())
    var deleteAccountResult: Result<Unit> = Result.success(Unit)
    var revokeSessionResult: Result<Unit> = Result.success(Unit)

    var loginCalls: List<Pair<String, String>> = emptyList()
        private set
    var deleteAccountCalls: List<String> = emptyList()
        private set
    var revokeSessionCalls: List<Pair<Long, Boolean>> = emptyList()
        private set

    fun setSession(session: AuthSession?) {
        _currentSession.value = session
    }

    override suspend fun register(
        name: String,
        email: String,
        password: String,
        passwordConfirmation: String,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun login(email: String, password: String): Result<Unit> {
        loginCalls = loginCalls + (email to password)
        return loginResult
    }

    override suspend fun logout(): Result<Unit> {
        _currentSession.value = null
        return Result.success(Unit)
    }

    override suspend fun getUser(): Result<User> = getUserResult

    override suspend fun forgotPassword(email: String): Result<String> =
        Result.failure(UnsupportedOperationException())

    override suspend fun resetPassword(
        token: String,
        password: String,
        passwordConfirmation: String,
    ): Result<String> = Result.failure(UnsupportedOperationException())

    override suspend fun changePassword(
        currentPassword: String,
        password: String,
        passwordConfirmation: String,
    ): Result<String> = changePasswordResult

    override suspend fun deleteAccount(password: String): Result<Unit> {
        deleteAccountCalls = deleteAccountCalls + password
        return deleteAccountResult
    }

    override suspend fun getSessions(): Result<List<DeviceSession>> = getSessionsResult

    override suspend fun revokeSession(id: Long, isCurrentSession: Boolean): Result<Unit> {
        revokeSessionCalls = revokeSessionCalls + (id to isCurrentSession)
        return revokeSessionResult
    }
}
