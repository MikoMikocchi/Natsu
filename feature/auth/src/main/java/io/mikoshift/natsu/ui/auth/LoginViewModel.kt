package io.mikoshift.natsu.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.mikoshift.natsu.core.domain.repository.AuthRepository
import io.mikoshift.natsu.core.model.AuthError
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Backs the login screen. Performs client-side validation before ever hitting the network,
 * then delegates to [AuthRepository.login] and maps the resulting [AuthError] (if any) onto
 * [LoginUiState]'s error slots. Invalid-credential errors from the backend land under the
 * `"base"` key, which is treated as [LoginUiState.generalError].
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, generalError = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null, generalError = null) }
    }

    fun submit() {
        val state = _uiState.value

        val emailError = if (state.email.isBlank()) "Email is required" else null
        val passwordError = if (state.password.isBlank()) "Password is required" else null

        if (emailError != null || passwordError != null) {
            _uiState.update {
                it.copy(isLoading = false, emailError = emailError, passwordError = passwordError)
            }
            return
        }

        _uiState.update {
            it.copy(isLoading = true, emailError = null, passwordError = null, generalError = null)
        }

        viewModelScope.launch {
            val result = authRepository.login(email = state.email, password = state.password)

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
                },
                onFailure = { throwable ->
                    applyError(throwable as? AuthError)
                },
            )
        }
    }

    private fun applyError(error: AuthError?) {
        when (error) {
            is AuthError.ValidationError -> {
                val fieldErrors = error.fieldErrors
                val leftover = mutableListOf<String>()
                fieldErrors.forEach { (key, messages) ->
                    if (key !in KNOWN_FIELD_KEYS) {
                        leftover += messages
                    }
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        emailError = fieldErrors["email"]?.joinToString(", "),
                        passwordError = fieldErrors["password"]?.joinToString(", "),
                        generalError = leftover.takeIf { it.isNotEmpty() }?.joinToString(", "),
                    )
                }
            }
            is AuthError.Unauthorized -> {
                _uiState.update {
                    it.copy(isLoading = false, generalError = "Invalid email or password")
                }
            }
            is AuthError.NetworkFailure -> {
                _uiState.update {
                    it.copy(isLoading = false, generalError = "Network error, please try again")
                }
            }
            is AuthError.Unknown -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        generalError = error.errorMessage ?: "Something went wrong, please try again",
                    )
                }
            }
            null -> {
                _uiState.update {
                    it.copy(isLoading = false, generalError = "Something went wrong, please try again")
                }
            }
        }
    }

    private companion object {
        val KNOWN_FIELD_KEYS = setOf("email", "password")
    }
}
