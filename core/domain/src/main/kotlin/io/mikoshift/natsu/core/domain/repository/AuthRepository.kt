package io.mikoshift.natsu.core.domain.repository

import io.mikoshift.natsu.core.model.AuthSession
import io.mikoshift.natsu.core.model.DeviceSession
import io.mikoshift.natsu.core.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val isLoggedIn: Flow<Boolean>
    val currentSession: StateFlow<AuthSession?>

    suspend fun register(name: String, email: String, password: String, passwordConfirmation: String): Result<Unit>

    suspend fun login(email: String, password: String): Result<Unit>

    suspend fun logout(): Result<Unit>

    suspend fun getUser(): Result<User>

    suspend fun forgotPassword(email: String): Result<String>

    suspend fun resetPassword(token: String, password: String, passwordConfirmation: String): Result<String>

    suspend fun changePassword(currentPassword: String, password: String, passwordConfirmation: String): Result<String>

    suspend fun deleteAccount(password: String): Result<Unit>

    suspend fun getSessions(): Result<List<DeviceSession>>

    suspend fun revokeSession(id: String, isCurrentSession: Boolean): Result<Unit>
}
