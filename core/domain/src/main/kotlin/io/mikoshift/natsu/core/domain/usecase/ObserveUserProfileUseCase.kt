package io.mikoshift.natsu.core.domain.usecase

import io.mikoshift.natsu.core.domain.repository.AuthRepository
import io.mikoshift.natsu.core.model.User
import javax.inject.Inject

class ObserveUserProfileUseCase
@Inject
constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(): Result<User> = authRepository.getUser()
}
