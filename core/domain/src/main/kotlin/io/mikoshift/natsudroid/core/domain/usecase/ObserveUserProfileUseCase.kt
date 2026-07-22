package io.mikoshift.natsudroid.core.domain.usecase

import io.mikoshift.natsudroid.core.domain.repository.AuthRepository
import io.mikoshift.natsudroid.core.model.User
import javax.inject.Inject

class ObserveUserProfileUseCase
@Inject
constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(): Result<User> = authRepository.getUser()
}
