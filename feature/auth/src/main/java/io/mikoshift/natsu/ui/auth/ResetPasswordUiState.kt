package io.mikoshift.natsu.ui.auth

data class ResetPasswordUiState(
    val token: String = "",
    val password: String = "",
    val passwordConfirmation: String = "",
    val tokenError: String? = null,
    val passwordError: String? = null,
    val passwordConfirmationError: String? = null,
    val generalError: String? = null,
    val isLoading: Boolean = false,
)

sealed interface ResetPasswordEffect {
    data object NavigateToLogin : ResetPasswordEffect
}
