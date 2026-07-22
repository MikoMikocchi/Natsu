package io.mikoshift.natsu.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.mikoshift.natsu.core.domain.usecase.LoginUseCase
import io.mikoshift.natsu.core.model.AuthError
import io.mikoshift.natsu.feature.auth.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

open class LoginViewModel(private val context: Context, private val login: LoginUseCase) : ViewModel() {
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

        val emailError =
            if (state.email.isBlank()) {
                context.getString(R.string.error_email_required)
            } else {
                null
            }
        val passwordError =
            if (state.password.isBlank()) {
                context.getString(R.string.error_password_required)
            } else {
                null
            }

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
            login(email = state.email, password = state.password).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
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
                    it.copy(
                        isLoading = false,
                        generalError = context.getString(R.string.error_invalid_credentials),
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
        val KNOWN_FIELD_KEYS = setOf("email", "password")
    }
}
