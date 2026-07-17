package io.mikoshift.natsu.ui.profile

import android.content.Context
import io.mikoshift.natsu.core.domain.usecase.ChangePasswordUseCase
import io.mikoshift.natsu.core.model.AuthError
import io.mikoshift.natsu.core.testing.repository.FakeAuthRepository
import io.mikoshift.natsu.feature.profile.R
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
class ChangePasswordViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var viewModel: ChangePasswordViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        every { context.getString(R.string.error_current_password_required) } returns "Current password is required"
        every { context.getString(R.string.error_password_min_length) } returns "Password must be at least 8 characters"
        every { context.getString(R.string.error_passwords_do_not_match) } returns "Passwords do not match"
        every { context.getString(R.string.error_network) } returns "Network error"
        every { context.getString(R.string.error_generic) } returns "Something went wrong"

        authRepository = FakeAuthRepository()
        viewModel = ChangePasswordViewModel(
            context = context,
            changePassword = ChangePasswordUseCase(authRepository),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun submit_withBlankCurrentPassword_setsValidationErrors() = runTest {
        viewModel.onPasswordChange("newpassword")
        viewModel.onPasswordConfirmationChange("newpassword")

        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Current password is required", state.currentPasswordError)
        assertNull(state.passwordError)
        assertNull(state.passwordConfirmationError)
        assertFalse(state.isLoading)
    }

    @Test
    fun submit_withShortPassword_setsPasswordError() = runTest {
        viewModel.onCurrentPasswordChange("oldpassword")
        viewModel.onPasswordChange("short")
        viewModel.onPasswordConfirmationChange("short")

        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Password must be at least 8 characters", state.passwordError)
        assertFalse(state.isLoading)
    }

    @Test
    fun submit_withMismatchedConfirmation_setsConfirmationError() = runTest {
        viewModel.onCurrentPasswordChange("oldpassword")
        viewModel.onPasswordChange("newpassword")
        viewModel.onPasswordConfirmationChange("different")

        viewModel.submit()
        advanceUntilIdle()

        assertEquals("Passwords do not match", viewModel.uiState.value.passwordConfirmationError)
    }

    @Test
    fun submit_onSuccess_clearsFieldsAndSetsMessage() = runTest {
        authRepository.changePasswordResult = Result.success("Password updated")
        viewModel.onCurrentPasswordChange("oldpassword")
        viewModel.onPasswordChange("newpassword")
        viewModel.onPasswordConfirmationChange("newpassword")

        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Password updated", state.successMessage)
        assertEquals("", state.currentPassword)
        assertEquals("", state.password)
        assertEquals("", state.passwordConfirmation)
    }

    @Test
    fun submit_onValidationError_mapsFieldErrors() = runTest {
        authRepository.changePasswordResult = Result.failure(
            AuthError.ValidationError(
                mapOf(
                    "current_password" to listOf("Incorrect password"),
                    "password" to listOf("Too weak"),
                ),
            ),
        )
        viewModel.onCurrentPasswordChange("wrong")
        viewModel.onPasswordChange("newpassword")
        viewModel.onPasswordConfirmationChange("newpassword")

        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Incorrect password", state.currentPasswordError)
        assertEquals("Too weak", state.passwordError)
    }

    @Test
    fun submit_onNetworkFailure_setsGeneralError() = runTest {
        authRepository.changePasswordResult = Result.failure(AuthError.NetworkFailure)
        viewModel.onCurrentPasswordChange("oldpassword")
        viewModel.onPasswordChange("newpassword")
        viewModel.onPasswordConfirmationChange("newpassword")

        viewModel.submit()
        advanceUntilIdle()

        assertEquals("Network error", viewModel.uiState.value.generalError)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
