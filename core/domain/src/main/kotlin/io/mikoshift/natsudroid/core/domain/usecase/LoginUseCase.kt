package io.mikoshift.natsudroid.core.domain.usecase

import io.mikoshift.natsudroid.core.common.analytics.AnalyticsTracker
import io.mikoshift.natsudroid.core.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase
@Inject
constructor(
    private val authRepository: AuthRepository,
    private val analyticsTracker: AnalyticsTracker,
) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> =
        authRepository.login(email = email, password = password).also { result ->
            if (result.isSuccess) {
                analyticsTracker.track("login_success")
            }
        }
}
