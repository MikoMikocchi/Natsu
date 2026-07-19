package io.mikoshift.natsu.ui.auth

import android.content.Context
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.mikoshift.natsu.core.domain.usecase.ForgotPasswordUseCase
import io.mikoshift.natsu.core.model.AuthError
import io.mikoshift.natsu.feature.auth.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val forgotPassword: ForgotPasswordUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, generalError = null) }
    }

    fun submit() {
        val state = _uiState.value
        val emailError =
            if (!Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
                context.getString(R.string.error_invalid_email)
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
            forgotPassword(state.email).fold(
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
