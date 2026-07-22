package io.mikoshift.natsudroid.core.domain.usecase

import io.mikoshift.natsudroid.core.domain.repository.AuthRepository
import javax.inject.Inject

class ForgotPasswordUseCase
@Inject
constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String): Result<String> = authRepository.forgotPassword(email)
}
