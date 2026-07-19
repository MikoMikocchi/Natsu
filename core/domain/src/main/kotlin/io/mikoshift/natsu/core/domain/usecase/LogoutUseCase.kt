package io.mikoshift.natsu.core.domain.usecase

import io.mikoshift.natsu.core.domain.repository.AuthRepository
import javax.inject.Inject

class LogoutUseCase
@Inject
constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(): Result<Unit> = authRepository.logout()
}
