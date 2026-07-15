package io.mikoshift.natsu.ui.auth

import android.content.Context
import io.mikoshift.natsu.core.domain.usecase.LoginUseCase
import io.mikoshift.natsu.core.model.AuthError
import io.mikoshift.natsu.core.testing.analytics.FakeAnalyticsTracker
import io.mikoshift.natsu.core.testing.repository.FakeAuthRepository
import io.mikoshift.natsu.feature.auth.R
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        every { context.getString(R.string.error_email_required) } returns "Email is required"
        every { context.getString(R.string.error_password_required) } returns "Password is required"
        every { context.getString(R.string.error_invalid_credentials) } returns "Invalid email or password"
        every { context.getString(R.string.error_network) } returns "Network error, please try again"
        every { context.getString(R.string.error_generic) } returns "Something went wrong, please try again"

        authRepository = FakeAuthRepository()
        viewModel = LoginViewModel(
            context = context,
            login = LoginUseCase(authRepository, FakeAnalyticsTracker()),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun submit_withBlankEmail_setsEmailError() = runTest {
        viewModel.onPasswordChange("password")

        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Email is required", state.emailError)
        assertNull(state.passwordError)
        assertFalse(state.isLoading)
    }

    @Test
    fun submit_withValidCredentials_succeeds() = runTest {
        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("password")

        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.generalError)
        assertEquals(listOf("test@example.com" to "password"), authRepository.loginCalls)
    }

    @Test
    fun submit_withUnauthorizedError_setsGeneralError() = runTest {
        authRepository.loginResult = Result.failure(AuthError.Unauthorized)
        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("password")

        viewModel.submit()
        advanceUntilIdle()

        assertEquals("Invalid email or password", viewModel.uiState.value.generalError)
    }
}
