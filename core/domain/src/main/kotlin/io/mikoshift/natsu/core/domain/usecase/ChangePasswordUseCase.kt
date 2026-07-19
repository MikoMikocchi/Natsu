package io.mikoshift.natsu.core.domain.usecase

import io.mikoshift.natsu.core.domain.repository.AuthRepository
import javax.inject.Inject

class ChangePasswordUseCase
@Inject
constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(
        currentPassword: String,
        password: String,
        passwordConfirmation: String,
    ): Result<String> = authRepository.changePassword(
        currentPassword = currentPassword,
        password = password,
        passwordConfirmation = passwordConfirmation,
    )
}
