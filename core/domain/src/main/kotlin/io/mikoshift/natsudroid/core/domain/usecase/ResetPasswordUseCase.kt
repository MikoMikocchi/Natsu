package io.mikoshift.natsudroid.core.domain.usecase

import io.mikoshift.natsudroid.core.domain.repository.AuthRepository
import javax.inject.Inject

class ResetPasswordUseCase
@Inject
constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(token: String, password: String, passwordConfirmation: String): Result<String> =
        authRepository.resetPassword(
            token = token,
            password = password,
            passwordConfirmation = passwordConfirmation,
        )
}
