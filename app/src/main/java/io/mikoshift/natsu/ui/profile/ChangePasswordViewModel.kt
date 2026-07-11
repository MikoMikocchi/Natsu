package io.mikoshift.natsu.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.mikoshift.natsu.data.repository.AuthError
import io.mikoshift.natsu.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChangePasswordViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    fun onCurrentPasswordChange(value: String) {
        _uiState.update { it.copy(currentPassword = value, currentPasswordError = null, generalError = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null, generalError = null) }
    }

    fun onPasswordConfirmationChange(value: String) {
        _uiState.update {
            it.copy(passwordConfirmation = value, passwordConfirmationError = null, generalError = null)
        }
    }

    fun submit() {
        val state = _uiState.value

        val currentPasswordError = if (state.currentPassword.isBlank()) "Current password is required" else null
        val passwordError = if (state.password.length < 8) {
            "Password must be at least 8 characters"
        } else {
            null
        }
        val passwordConfirmationError = if (state.passwordConfirmation != state.password) {
            "Passwords do not match"
        } else {
            null
        }

        if (currentPasswordError != null || passwordError != null || passwordConfirmationError != null) {
            _uiState.update {
                it.copy(
                    currentPasswordError = currentPasswordError,
                    passwordError = passwordError,
                    passwordConfirmationError = passwordConfirmationError,
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                currentPasswordError = null,
                passwordError = null,
                passwordConfirmationError = null,
                generalError = null,
                successMessage = null,
            )
        }

        viewModelScope.launch {
            authRepository.changePassword(
                currentPassword = state.currentPassword,
                password = state.password,
                passwordConfirmation = state.passwordConfirmation,
            ).fold(
                onSuccess = { message ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = message,
                            currentPassword = "",
                            password = "",
                            passwordConfirmation = "",
                        )
                    }
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
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentPasswordError = fieldErrors["current_password"]?.joinToString(", "),
                        passwordError = fieldErrors["password"]?.joinToString(", "),
                        passwordConfirmationError = fieldErrors["password_confirmation"]?.joinToString(", "),
                        generalError = fieldErrors["base"]?.joinToString(", "),
                    )
                }
            }
            is AuthError.NetworkFailure -> {
                _uiState.update {
                    it.copy(isLoading = false, generalError = "Network error, please try again")
                }
            }
            else -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        generalError = (error as? AuthError.Unknown)?.errorMessage
                            ?: "Something went wrong, please try again",
                    )
                }
            }
        }
    }
}
