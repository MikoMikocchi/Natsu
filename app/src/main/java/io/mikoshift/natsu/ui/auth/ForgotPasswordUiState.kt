package io.mikoshift.natsu.ui.auth

data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: String? = null,
    val generalError: String? = null,
    val successMessage: String? = null,
    val isLoading: Boolean = false,
)
