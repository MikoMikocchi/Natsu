package io.mikoshift.natsudroid.core.domain.usecase

import io.mikoshift.natsudroid.core.domain.repository.AuthRepository
import javax.inject.Inject

class DeleteAccountUseCase
@Inject
constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(password: String): Result<Unit> = authRepository.deleteAccount(password)
}
