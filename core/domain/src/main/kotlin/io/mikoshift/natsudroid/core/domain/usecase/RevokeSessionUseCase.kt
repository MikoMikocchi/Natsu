package io.mikoshift.natsudroid.core.domain.usecase

import io.mikoshift.natsudroid.core.domain.repository.AuthRepository
import javax.inject.Inject

class RevokeSessionUseCase
@Inject
constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(sessionId: String, isCurrentSession: Boolean): Result<Unit> =
        authRepository.revokeSession(id = sessionId, isCurrentSession = isCurrentSession)
}
