package io.mikoshift.natsu.ui.auth

import android.content.Context
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.mikoshift.natsu.core.domain.usecase.RegisterUseCase
import io.mikoshift.natsu.core.model.AuthError
import io.mikoshift.natsu.feature.auth.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val register: RegisterUseCase,
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

        val nameError =
            if (state.name.isBlank()) {
                context.getString(R.string.error_name_required)
            } else {
                null
            }
        val emailError =
            if (!Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
                context.getString(R.string.error_invalid_email)
            } else {
                null
            }
        val passwordError =
            if (state.password.length < 8) {
                context.getString(R.string.error_password_min_length)
            } else {
                null
            }
        val passwordConfirmationError =
            if (state.passwordConfirmation != state.password) {
                context.getString(R.string.error_passwords_do_not_match)
            } else {
                null
            }

        if (listOf(nameError, emailError, passwordError, passwordConfirmationError).any { it != null }) {
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
            register(
                name = state.name,
                email = state.email,
                password = state.password,
                passwordConfirmation = state.passwordConfirmation,
            ).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, registrationSucceeded = true) }
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
                    it.copy(
                        isLoading = false,
                        generalError = context.getString(R.string.error_session_expired),
                    )
                }
            }
            is AuthError.NetworkFailure -> {
                _uiState.update {
                    it.copy(isLoading = false, generalError = context.getString(R.string.error_network))
                }
            }
            is AuthError.Unknown -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        generalError = error.errorMessage ?: context.getString(R.string.error_generic),
                    )
                }
            }
            null -> {
                _uiState.update {
                    it.copy(isLoading = false, generalError = context.getString(R.string.error_generic))
                }
            }
        }
    }

    private companion object {
        val KNOWN_FIELD_KEYS = setOf("name", "email", "password", "password_confirmation")
    }
}
