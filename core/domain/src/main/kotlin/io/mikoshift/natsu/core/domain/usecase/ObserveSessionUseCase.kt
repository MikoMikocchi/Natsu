package io.mikoshift.natsu.core.domain.usecase

import io.mikoshift.natsu.core.domain.repository.AuthRepository
import io.mikoshift.natsu.core.model.AuthSession
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ObserveSessionUseCase
@Inject
constructor(private val authRepository: AuthRepository) {
    operator fun invoke(): StateFlow<AuthSession?> = authRepository.currentSession
}
