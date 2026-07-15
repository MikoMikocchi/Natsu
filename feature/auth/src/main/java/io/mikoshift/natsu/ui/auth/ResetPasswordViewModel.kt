package io.mikoshift.natsu.ui.auth

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.mikoshift.natsu.core.domain.usecase.ResetPasswordUseCase
import io.mikoshift.natsu.core.model.AuthError
import io.mikoshift.natsu.feature.auth.R
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val resetPassword: ResetPasswordUseCase,
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

        val tokenError = if (state.token.isBlank()) {
            context.getString(R.string.error_token_required)
        } else {
            null
        }
        val passwordError = if (state.password.length < 8) {
            context.getString(R.string.error_password_min_length)
        } else {
            null
        }
        val passwordConfirmationError = if (state.passwordConfirmation != state.password) {
            context.getString(R.string.error_passwords_do_not_match)
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
            resetPassword(
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
                    it.copy(isLoading = false, generalError = context.getString(R.string.error_network))
                }
            }
            else -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        generalError = (error as? AuthError.Unknown)?.errorMessage
                            ?: context.getString(R.string.error_generic),
                    )
                }
            }
        }
    }
}
