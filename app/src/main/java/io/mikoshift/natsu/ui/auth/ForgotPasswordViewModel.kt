package io.mikoshift.natsu.ui.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.mikoshift.natsu.data.repository.AuthError
import io.mikoshift.natsu.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ForgotPasswordViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, generalError = null) }
    }

    fun submit() {
        val state = _uiState.value
        val emailError = if (!Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            "Enter a valid email address"
        } else {
            null
        }

        if (emailError != null) {
            _uiState.update { it.copy(emailError = emailError) }
            return
        }

        _uiState.update {
            it.copy(isLoading = true, emailError = null, generalError = null, successMessage = null)
        }

        viewModelScope.launch {
            authRepository.forgotPassword(state.email).fold(
                onSuccess = { message ->
                    _uiState.update {
                        it.copy(isLoading = false, successMessage = message)
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
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        emailError = error.fieldErrors["email"]?.joinToString(", "),
                        generalError = error.fieldErrors["base"]?.joinToString(", "),
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
