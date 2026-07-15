package io.mikoshift.natsu.ui.auth

import android.util.Patterns
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
 * Backs the registration screen. Performs client-side validation before ever hitting the
 * network, then delegates to [AuthRepository.register] and maps the resulting [AuthError]
 * (if any) onto [RegisterUiState]'s per-field error slots.
 */
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, nameError = null, generalError = null) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, generalError = null) }
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

        val nameError = if (state.name.isBlank()) "Name is required" else null
        val emailError = if (!Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            "Enter a valid email address"
        } else {
            null
        }
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

        if (nameError != null || emailError != null || passwordError != null || passwordConfirmationError != null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    nameError = nameError,
                    emailError = emailError,
                    passwordError = passwordError,
                    passwordConfirmationError = passwordConfirmationError,
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                nameError = null,
                emailError = null,
                passwordError = null,
                passwordConfirmationError = null,
                generalError = null,
            )
        }

        viewModelScope.launch {
            val result = authRepository.register(
                name = state.name,
                email = state.email,
                password = state.password,
                passwordConfirmation = state.passwordConfirmation,
            )

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, isRegistered = true) }
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
                        nameError = fieldErrors["name"]?.joinToString(", "),
                        emailError = fieldErrors["email"]?.joinToString(", "),
                        passwordError = fieldErrors["password"]?.joinToString(", "),
                        passwordConfirmationError = fieldErrors["password_confirmation"]?.joinToString(", "),
                        generalError = leftover.takeIf { it.isNotEmpty() }?.joinToString(", "),
                    )
                }
            }
            is AuthError.Unauthorized -> {
                _uiState.update {
                    it.copy(isLoading = false, generalError = "Session expired, please try again")
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
        val KNOWN_FIELD_KEYS = setOf("name", "email", "password", "password_confirmation")
    }
}
