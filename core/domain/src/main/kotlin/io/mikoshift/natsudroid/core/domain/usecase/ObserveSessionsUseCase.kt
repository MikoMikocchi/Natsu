package io.mikoshift.natsudroid.core.domain.usecase

import io.mikoshift.natsudroid.core.domain.repository.AuthRepository
import io.mikoshift.natsudroid.core.model.DeviceSession
import javax.inject.Inject

class ObserveSessionsUseCase
@Inject
constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(): Result<List<DeviceSession>> = authRepository.getSessions()
}
