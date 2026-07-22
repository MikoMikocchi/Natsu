package io.mikoshift.natsudroid.core.domain.usecase

import io.mikoshift.natsudroid.core.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterUseCase
@Inject
constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(
        name: String,
        email: String,
        password: String,
        passwordConfirmation: String,
    ): Result<Unit> = authRepository.register(
        name = name,
        email = email,
        password = password,
        passwordConfirmation = passwordConfirmation,
    )
}
