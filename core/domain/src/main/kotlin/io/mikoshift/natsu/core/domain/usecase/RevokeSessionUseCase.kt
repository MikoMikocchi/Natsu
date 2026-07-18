package io.mikoshift.natsu.core.domain.usecase

import io.mikoshift.natsu.core.domain.repository.AuthRepository
import javax.inject.Inject

class RevokeSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(sessionId: String, isCurrentSession: Boolean): Result<Unit> =
        authRepository.revokeSession(id = sessionId, isCurrentSession = isCurrentSession)
}
