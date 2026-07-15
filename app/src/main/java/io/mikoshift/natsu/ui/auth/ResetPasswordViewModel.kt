package io.mikoshift.natsu.ui.auth

import androidx.lifecycle.SavedStateHandle
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

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ResetPasswordUiState(token = savedStateHandle.get<String>("token").orEmpty()),
    )
    val uiState: StateFlow<ResetPasswordUiState> = _uiState.asStateFlow()

    fun onTokenChange(value: String) {
        _uiState.update { it.copy(token = value, tokenError = null, generalError = null) }
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

        val tokenError = if (state.token.isBlank()) "Token is required" else null
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

        if (tokenError != null || passwordError != null || passwordConfirmationError != null) {
            _uiState.update {
                it.copy(
                    tokenError = tokenError,
                    passwordError = passwordError,
                    passwordConfirmationError = passwordConfirmationError,
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                tokenError = null,
                passwordError = null,
                passwordConfirmationError = null,
                generalError = null,
            )
        }

        viewModelScope.launch {
            authRepository.resetPassword(
                token = state.token,
                password = state.password,
                passwordConfirmation = state.passwordConfirmation,
            ).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
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
                        tokenError = fieldErrors["token"]?.joinToString(", "),
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
