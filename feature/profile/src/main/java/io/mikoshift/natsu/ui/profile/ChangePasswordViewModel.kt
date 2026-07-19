package io.mikoshift.natsu.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.mikoshift.natsu.core.domain.usecase.ChangePasswordUseCase
import io.mikoshift.natsu.core.model.AuthError
import io.mikoshift.natsu.feature.profile.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChangePasswordViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val changePassword: ChangePasswordUseCase,
) : ViewModel() {
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

        val currentPasswordError =
            if (state.currentPassword.isBlank()) {
                context.getString(R.string.error_current_password_required)
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
            changePassword(
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
                    it.copy(isLoading = false, generalError = context.getString(R.string.error_network))
                }
            }
            else -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        generalError =
                        (error as? AuthError.Unknown)?.errorMessage
                            ?: context.getString(R.string.error_generic),
                    )
                }
            }
        }
    }
}
