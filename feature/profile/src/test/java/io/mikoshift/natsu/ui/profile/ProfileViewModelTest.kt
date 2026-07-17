package io.mikoshift.natsu.ui.profile

import android.content.Context
import app.cash.turbine.test
import io.mikoshift.natsu.core.domain.usecase.DeleteAccountUseCase
import io.mikoshift.natsu.core.domain.usecase.LogoutUseCase
import io.mikoshift.natsu.core.domain.usecase.ObserveSessionsUseCase
import io.mikoshift.natsu.core.domain.usecase.ObserveUserProfileUseCase
import io.mikoshift.natsu.core.domain.usecase.RevokeSessionUseCase
import io.mikoshift.natsu.core.model.AuthError
import io.mikoshift.natsu.core.testing.fixture.AuthFixtures
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        every { context.getString(R.string.error_password_required) } returns "Password is required"
        every { context.getString(R.string.error_network) } returns "Network error"
        every { context.getString(R.string.error_generic) } returns "Something went wrong"

        authRepository = FakeAuthRepository(
            session = AuthFixtures.session(),
        ).apply {
            getUserResult = Result.success(AuthFixtures.user(name = "Alice"))
            getSessionsResult = Result.success(
                listOf(
                    AuthFixtures.deviceSession(id = 1L, name = "Phone", current = true),
                    AuthFixtures.deviceSession(id = 2L, name = "Tablet", current = false),
                ),
            )
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun refresh_loadsUserAndSessions() = runTest {
        createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoadingUser)
        assertFalse(state.isLoadingSessions)
        assertEquals("Alice", state.user?.displayName)
        assertEquals(2, state.sessions.size)
        assertEquals("Phone", state.sessions.first().deviceName)
    }

    @Test
    fun deleteAccount_withBlankPassword_setsValidationError() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.deleteAccount()
        advanceUntilIdle()

        assertEquals("Password is required", viewModel.uiState.value.deletePasswordError)
        assertFalse(viewModel.uiState.value.isDeletingAccount)
        assertTrue(authRepository.deleteAccountCalls.isEmpty())
    }

    @Test
    fun deleteAccount_onSuccess_closesDialog() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.showDeleteDialog()
        viewModel.onDeletePasswordChange("secret")
        viewModel.deleteAccount()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isDeletingAccount)
        assertFalse(state.showDeleteDialog)
        assertEquals(listOf("secret"), authRepository.deleteAccountCalls)
    }

    @Test
    fun deleteAccount_onValidationError_setsFieldError() = runTest {
        authRepository.deleteAccountResult = Result.failure(
            AuthError.ValidationError(mapOf("password" to listOf("Incorrect password"))),
        )
        createViewModel()
        advanceUntilIdle()

        viewModel.onDeletePasswordChange("wrong")
        viewModel.deleteAccount()
        advanceUntilIdle()

        assertEquals("Incorrect password", viewModel.uiState.value.deletePasswordError)
        assertFalse(viewModel.uiState.value.isDeletingAccount)
    }

    @Test
    fun deleteAccount_onNetworkFailure_emitsEffect() = runTest {
        authRepository.deleteAccountResult = Result.failure(AuthError.NetworkFailure)
        createViewModel()
        advanceUntilIdle()

        viewModel.onDeletePasswordChange("secret")

        viewModel.effects.test {
            viewModel.deleteAccount()
            advanceUntilIdle()

            assertEquals(
                ProfileEffect.ShowMessage("Network error"),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(viewModel.uiState.value.isDeletingAccount)
    }

    @Test
    fun logout_setsLoadingState() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.logout()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoggingOut)
        assertNull(authRepository.currentSession.value)
    }

    @Test
    fun revokeSession_onSuccess_reloadsSessionsForNonCurrent() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.revokeSession(sessionId = 2L, isCurrent = false)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.revokingSessionId)
        assertEquals(listOf(2L to false), authRepository.revokeSessionCalls)
    }

    @Test
    fun revokeSession_onFailure_setsGeneralError() = runTest {
        authRepository.revokeSessionResult = Result.failure(
            AuthError.Unknown("Could not revoke session"),
        )
        createViewModel()
        advanceUntilIdle()

        viewModel.revokeSession(sessionId = 2L, isCurrent = false)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.revokingSessionId)
        assertEquals("Could not revoke session", viewModel.uiState.value.generalError)
    }

    @Test
    fun deleteDialog_showAndDismiss_resetsState() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.showDeleteDialog()
        viewModel.onDeletePasswordChange("temp")
        viewModel.dismissDeleteDialog()

        val state = viewModel.uiState.value
        assertFalse(state.showDeleteDialog)
        assertEquals("", state.deletePassword)
        assertNull(state.deletePasswordError)
    }

    private fun createViewModel() {
        viewModel = ProfileViewModel(
            context = context,
            observeUserProfile = ObserveUserProfileUseCase(authRepository),
            observeSessions = ObserveSessionsUseCase(authRepository),
            logoutUseCase = LogoutUseCase(authRepository),
            deleteAccount = DeleteAccountUseCase(authRepository),
            revokeSession = RevokeSessionUseCase(authRepository),
        )
    }
}
