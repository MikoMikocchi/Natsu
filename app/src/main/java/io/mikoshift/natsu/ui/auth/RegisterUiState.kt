package io.mikoshift.natsu.ui.auth

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val passwordConfirmation: String = "",
    val isLoading: Boolean = false,
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val passwordConfirmationError: String? = null,
    val generalError: String? = null,
    val isRegistered: Boolean = false,
)
