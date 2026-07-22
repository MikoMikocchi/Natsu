package io.mikoshift.natsudroid.core.domain.usecase

import io.mikoshift.natsudroid.core.domain.repository.AuthRepository
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
